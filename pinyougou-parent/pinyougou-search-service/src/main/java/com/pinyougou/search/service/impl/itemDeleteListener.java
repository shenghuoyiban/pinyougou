package com.pinyougou.search.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pinyougou.search.service.ItemSearchService;
//删除solr索引库同步数据
@Component
public class itemDeleteListener implements MessageListener {
	   
	@Autowired
	private ItemSearchService ItemSearchService;
     //点对点消息消费者
	@Override
	public void onMessage(Message message) {
	  //转换类型
		ObjectMessage tetxMessage= (ObjectMessage)message;
		
		try {
			//得到消息实体ids
			Long[] ids = (Long [])tetxMessage.getObject();
			System.out.println("监听到消息"+ids);
			//数组转化成集合
			ItemSearchService.deleteByGoodsIds(Arrays.asList(ids));
			System.out.println("删除");
		} catch (JMSException e) {
			
			e.printStackTrace();
		}
	}

}
