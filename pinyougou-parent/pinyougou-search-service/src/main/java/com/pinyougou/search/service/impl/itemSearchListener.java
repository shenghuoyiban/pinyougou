package com.pinyougou.search.service.impl;

import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;
//点对点监听类
@Component
public class itemSearchListener implements MessageListener {
    //注入搜索服务
	@Autowired
    private ItemSearchService itemSearchService;
	//监听到消息 插入sorl索引库
	@Override
	public void onMessage(Message message) {
		System.out.println("监听到消息");
		//多态转化下text类型
		TextMessage textMessage=(TextMessage)message;
		try {
			//得到点对点文本消息 json字符串
			String text = textMessage.getText();
			//json字符串转为为集合
			List<TbItem> itemList = JSON.parseArray(text,TbItem.class);
			//得到list插入solr索引库
			itemSearchService.ImportList(itemList);
			System.out.println("导入");
		} catch (JMSException e) {
			
			e.printStackTrace();
		}
	}

}
