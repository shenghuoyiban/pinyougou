package com.pinyougou.cart.service;

import java.util.List;

import com.pinyougou.pojoroup.Cart;

public interface CartService {
  
	
	//添加购物车列表
	public List<Cart>  addGoodsToCartLis(List<Cart> list,Long itemId,Integer num);
	
	//将购物车列表存入redis
	public List<Cart> findCartListFromRedis(String username);
	
	//从取出redis购物车列表
	public void saveCartListToRedis(String username,List<Cart> cartList);
	//合并购物车
	public List<Cart> mergeCartList(List<Cart> cartList1,List<Cart> cartList2);
}
