package com.pinyougou.manager.controller;

import java.util.Arrays;
import java.util.List;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.pinyougou.pojo.TbGoods;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojoroup.Goods;

import com.pinyougou.sellergoods.service.GoodsService;

import entity.PageResult;
import entity.Result;

/**
 * controller
 * 
 * @author Administrator
 *
 */
@RestController
@RequestMapping("/goods")
public class GoodsController {

	@Reference
	private GoodsService goodsService;

	// 引入search工程
	/*
	 * @Reference(timeout=100000) private ItemSearchService ItemSearchService;
	 */
	/**
	 * 返回全部列表
	 * 
	 * @return
	 */
	@RequestMapping("/findAll")
	public List<TbGoods> findAll() {
		return goodsService.findAll();
	}

	/**
	 * 返回全部列表
	 * 
	 * @return
	 */
	@RequestMapping("/findPage")
	public PageResult findPage(int page, int rows) {
		return goodsService.findPage(page, rows);
	}

	/**
	 * 增加
	 * 
	 * @param goods
	 * @return
	 */

	/**
	 * 修改
	 * 
	 * @param goods
	 * @return
	 */
	@RequestMapping("/update")
	public Result update(@RequestBody Goods goods) {
		try {
			goodsService.update(goods);
			return new Result(true, "修改成功");
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "修改失败");
		}
	}

	/**
	 * 获取实体
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping("/findOne")
	public Goods findOne(Long id) {
		return goodsService.findOne(id);
	}

	/**
	 * 批量删除
	 * 
	 * @param ids
	 * @return
	 */
	// 同步删除solr索引库
	@Autowired
	private Destination queueSolrDeleteDestination;

	@RequestMapping("/delete")
	public Result delete(final Long[] ids) {
		try {
			goodsService.delete(ids);
			/*
			 * //闪现solr索引库 ItemSearchService.deleteByGoodsIds(Arrays.asList(ids));
			 */
			// MQ点对点删除同步solr库
			jmsTemplate.send(queueSolrDeleteDestination, new MessageCreator() {

				@Override
				public Message createMessage(Session session) throws JMSException {
					// 直接发送消息对象Long数组
					return session.createObjectMessage(ids);
				}
			});
			return new Result(true, "删除成功");
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "删除失败");
		}
	}

	/**
	 * 查询+分页
	 * 
	 * @param brand
	 * @param page
	 * @param rows
	 * @return
	 */
	@RequestMapping("/search")
	public PageResult search(@RequestBody TbGoods goods, int page, int rows) {
		return goodsService.findPage(goods, page, rows);
	}

	/**
	 * 更新状态
	 * 
	 * @param ids
	 * @param status
	 * 
	 * 
	 */

	// 注入jms操作avtiveMQ
	@Autowired
	private JmsTemplate jmsTemplate;
	// 点对点消息队列发送者
	@Autowired
	private Destination queueSolrDestination;

	//发布订阅模式
	@Autowired
	private Destination topicPageDestination;

	@RequestMapping("/updateStatus")
	public Result updateStatus(Long[] ids, String status) {
		try {
			goodsService.updateStatus(ids, status);
			// 通过审核
			if ("1".equals(status)) {
				// 得到要插入的sku列表
				List<TbItem> findItemListByGoodsIdListAndStatus = goodsService.findItemListByGoodsIdListAndStatus(ids,
						status);
				Long id = findItemListByGoodsIdListAndStatus.get(0).getId();
				System.out.println(id);
				// 批量插入solr索引库
				// ItemSearchService.ImportList(findItemListByGoodsIdListAndStatus);
				// 将集合转成字符串在传过去 用文本类型传过去
				final String jsonString = JSON.toJSONString(findItemListByGoodsIdListAndStatus);

				// 点对点消息发送
				jmsTemplate.send(queueSolrDestination, new MessageCreator() {

					@Override
					public Message createMessage(Session session) throws JMSException {

						return session.createTextMessage(jsonString);
					}
				});
			}

			// 静态页生成
			
			for(final Long goodsId:ids){
				
				/* ItemPageService.genTtemHtml(goodsId); */
				//发布订阅方式
				jmsTemplate.send(topicPageDestination, new MessageCreator() {
					
					@Override
					public Message createMessage(Session session) throws JMSException {
						//字符串类型发送文本
						return session.createTextMessage(goodsId+"");
					}
				});
			}
			 

			return new Result(true, "已通过审核");
		} catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "未能通过审核");
		}
	}
	// 远程调用
	/*
	 * @Reference(timeout = 50000) private ItemPageService ItemPageService;
	 */

	// 通过审核才能生成商品详细页面
	@RequestMapping("/genHtml")
	public void genHtml(Long goodsId) {
		/* ItemPageService.genTtemHtml(goodsId); */
	}

}
