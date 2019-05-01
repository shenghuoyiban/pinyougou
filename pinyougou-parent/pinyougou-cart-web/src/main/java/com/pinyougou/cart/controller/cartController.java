package com.pinyougou.cart.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.pojoroup.Cart;

import entity.Result;

@RestController
@RequestMapping("/cart")
public class cartController {
	@Reference(timeout = 60000)
	private CartService cartService;
	@Autowired
	private HttpServletRequest request;
	@Autowired
	private HttpServletResponse response;
	
	
	//从cookie中提取购物车列表
	@RequestMapping("/findCartList")
	public List<Cart> findCartList(){
		//获取当前登录人
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		System.out.println("当前登录人: "+username);
		//未登录 从cookie中取出购物车列表
		String cartListString = util.CookieUtil.getCookieValue(request, "cartList","UTF-8");
		List<Cart> cartList = JSON.parseArray(cartListString,Cart.class);
		if(cartListString==null||cartListString.equals("")) {
			cartListString="[]";
		}
		if(username.equals("anonymousUser")) {
			
			
			return cartList;
		}else {
			//已登录 根据用户名取出购物车列表
			List<Cart> listFromRedis = cartService.findCartListFromRedis(username);
			//cookie中有数据我才合并
			if(cartList.size()>0) {
				//用户登陆之后我需要合并购物车
				List<Cart> mergeCartList = cartService.mergeCartList(cartList, listFromRedis);
				//再次存入redis中
				cartService.saveCartListToRedis(username, mergeCartList);
				//避免重复合并清除cookie中的购物车
				util.CookieUtil.deleteCookie(request, response, "cartList");
				//返回合并的购物车
				return mergeCartList;
			}
		
			return listFromRedis;
		}
			
	}
	
	//操作购物车列表
	@RequestMapping("/addGoodsToCartList")
	public Result addGoodsToCatrList(Long itemId,Integer num) {
		
		//获取当前登录人
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		System.out.println("当前登录人: "+username);
		
		
		try {
			//提取购物车列表
			List<Cart> cartList = findCartList();
			//调用服务层操作购物车
			List<Cart> list = cartService.addGoodsToCartLis(cartList, itemId, num);
			if(username.equals("anonymousUser")) {
				//没登录
				String cartListString = JSON.toJSONString(list);
				//存回cookie
				util.CookieUtil.setCookie(request, response, "cartList", cartListString, 3600 * 24, "UTF-8");
			}else {
				//登录了存入redis中
				cartService.saveCartListToRedis(username, cartList);
			}

			return new Result(true, "存入cookie中了");
		} catch (Exception e) {
			return new Result(false, "存入cookie失败");
		}
		
	}
}
