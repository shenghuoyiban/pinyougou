package com.pinyougou.solrutil;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbItemExample;
import com.pinyougou.pojo.TbItemExample.Criteria;

@Component
public class SolrUtil {

	@Autowired
	private TbItemMapper itemMapper;
	
	@Autowired
	private SolrTemplate solrTemplate;
	
	public void importItemData(){
		
		TbItemExample example=new TbItemExample();
		Criteria criteria = example.createCriteria();
		criteria.andStatusEqualTo("1");//审核通过的才导入的
		List<TbItem> itemList = itemMapper.selectByExample(example);
		TbItem item1 = new TbItem();
		item1.setBarcode("22");
		item1.setBrand("22");
		item1.setCartThumbnail("22");
		item1.setCategory("22");
		item1.setCategoryid(22L);
		item1.setCreateTime(new Date());
		item1.setGoodsId(222L);
		item1.setIsDefault("2");
		item1.setNum(22);
		item1.setSeller("yijia");
		item1.setSellerId("yijia22");
		item1.setStatus("1");
		item1.setTitle("好2的东西");
		BigDecimal price =new BigDecimal(12.01);
		item1.setPrice(price);
		item1.setUpdateTime(new Date());
		itemMapper.insert(item1);
		
		System.out.println("---商品列表---");
		for(TbItem item:itemList){
			System.out.println(item.getId()+" "+ item.getTitle()+ " "+item.getPrice());	
			Map specMap = JSON.parseObject(item.getSpec(), Map.class);//从数据库中提取规格json字符串转换为map
			item.setSpecMap(specMap);
		}
		
		solrTemplate.saveBeans(itemList);
		solrTemplate.commit();
		
		System.out.println("---结束---");
	}
	
	public static void main(String[] args) {
		
		ApplicationContext context=new ClassPathXmlApplicationContext("classpath*:spring/applicationContext*.xml");
		SolrUtil solrUtil=  (SolrUtil) context.getBean("solrUtil");
		solrUtil.deleteAll(); 
		
		
	}
	
	public void deleteAll(){
		Query query = new SimpleQuery("*:*");
		solrTemplate.delete(query);
		solrTemplate.commit();
		
	}
	
	
}
