package com.pinyougou.page.service.impl;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pinyougou.page.service.ItemPageService;
//发布订阅方式
@Component
public class pageListener implements MessageListener {
	//注入商品生存页服务
	@Autowired
	private ItemPageService ItemPageService;
     //发布订阅方式监听类 生产商品详细页
	@Override
	public void onMessage(Message message) {
		//转化文本消息
		TextMessage textMessage=(TextMessage)message;
		
		
		try {
			//得到消息实体
			String text = textMessage.getText();
			System.out.println("监听到消息");
			//生成
			ItemPageService.genTtemHtml(Long.parseLong(text));
			System.out.println("生成");
		} catch (JMSException e) {
			
			e.printStackTrace();
		}
	}

}
