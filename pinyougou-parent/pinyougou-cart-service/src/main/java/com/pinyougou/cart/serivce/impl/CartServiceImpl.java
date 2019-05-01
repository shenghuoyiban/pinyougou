package com.pinyougou.cart.serivce.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojoroup.Cart;

@Service
public class CartServiceImpl implements CartService {
	@Autowired
	private TbItemMapper tbItemMapper;

	@Override
	public List<Cart> addGoodsToCartLis(List<Cart> CartList, Long itemId, Integer num) {
		// 1.根据item表id查询商品对象信息
		TbItem item = tbItemMapper.selectByPrimaryKey(itemId);
		if (item == null) {
			throw new RuntimeException("商品不存在");
		}
		if (item.getStatus().equals("1")) {
			throw new RuntimeException("商品状态不合法");
		}
		// 2.根据item表id查询商家id根据购物车分组
		String sellerId = item.getSellerId();
		// 从购物车列表查询商家id
		Cart cart = searchCartBysellerId(CartList, sellerId);
		// 3.根据商家id查询购物车对象有没有
		if (cart != null) {
			// 3.2如果购物车列表存在商家的购物车
			List<TbOrderItem> orderItemList = cart.getOrderItemList();
			TbOrderItem tbOrderItem = searchOrderItemByItemId(orderItemList, itemId);
			// 但是还要判断商品是否存在该购物车明细列表中存在
			if (tbOrderItem == null) {
				// 不在明细列表中,创建新的明细列表对象
				tbOrderItem = createOrderItem(item, num);
				// 添加明细列表中
				orderItemList.add(tbOrderItem);

			} else {
				// 3.55如果购物车对象和明细列表对象都存在，添加数量 更改金额
				// 更改数量
				tbOrderItem.setNum(tbOrderItem.getNum() + num);
				// 更改金额
				tbOrderItem.setTotalFee(new BigDecimal(tbOrderItem.getPrice().doubleValue() * num.byteValue()));
				// 当你要添加的明细商品的数量为0的时候直接移除
				if (tbOrderItem.getNum() <= 0) {
					// 移除明细
					cart.getOrderItemList().remove(tbOrderItem);
				}
				// 当购物车明细列表没有值时候移除购物车
				if (cart.getOrderItemList().size() == 0) {
					// 移除购物车
					CartList.remove(cart);
				}

			}

		} else {
			// 3.1如果购物车列表不存在商家的购物车
			// 3.11创建一个新的购物车对象
			cart = new Cart();
			cart.setSellerId(sellerId);
			// 得到商品名字
			cart.setSellerName(item.getSeller());
			List<TbOrderItem> orderItemList = new ArrayList<TbOrderItem>();
			TbOrderItem tbOrderItem = createOrderItem(item, num);
			// 添加新的购物车明细对象
			orderItemList.add(tbOrderItem);
			// 将新的明细列表添加到购物车中
			cart.setOrderItemList(orderItemList);
			// 将新的购物车添加到购物车列表中
			CartList.add(cart);
		}

		// 3.33判断购物车明细列表有没有重复

		return CartList;
	}

	// 查询购物车明细列表
	private TbOrderItem searchOrderItemByItemId(List<TbOrderItem> orderItemList, Long itemId) {
		for (TbOrderItem orderItem : orderItemList) {
			if (orderItem.getItemId().longValue() == itemId.longValue()) {
				return orderItem;
			}

		}
		return null;
	}

	// 创建购物车明细对象
	private TbOrderItem createOrderItem(TbItem item, Integer num) {
		TbOrderItem tbOrderItem = new TbOrderItem();
		tbOrderItem.setItemId(item.getId());
		tbOrderItem.setNum(num);
		tbOrderItem.setPicPath(item.getImage());
		tbOrderItem.setPrice(item.getPrice());
		tbOrderItem.setTitle(item.getTitle());
		tbOrderItem.setGoodsId(item.getGoodsId());
		tbOrderItem.setTotalFee(new BigDecimal(item.getPrice().doubleValue() * num));
		return tbOrderItem;
	}

	// 从购物车列表查询商家id
	private Cart searchCartBysellerId(List<Cart> CartList, String sellerId) {
		for (Cart cart : CartList) {
			if (cart.getSellerId().equals(sellerId)) {
				return cart;
			}
		}
		return null;
	}

	@Autowired
	private RedisTemplate redisTemplate;

	@Override
	public List<Cart> findCartListFromRedis(String username) {
		// 根据用户名取出购物车列表
		List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(username);
		if (cartList == null) {
			cartList = new ArrayList<>();
		}
		return cartList;
	}

	@Override
	public void saveCartListToRedis(String username, List<Cart> cartList) {
		System.out.println("想redis中存入购物车" + username);
		redisTemplate.boundHashOps("cartList").put(username, cartList);

	}
    //合并购物车
	@Override
	public List<Cart> mergeCartList(List<Cart> cartList1, List<Cart> cartList2) {
		System.out.println("合并购物车");
		for (Cart cart : cartList2) {
			for (TbOrderItem orderItem : cart.getOrderItemList()) {
				cartList1 = addGoodsToCartLis(cartList1, orderItem.getItemId(), orderItem.getNum());
			}
		}

		return cartList1;
	}

}
