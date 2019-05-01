package com.pinyougou.search.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.FilterQuery;
import org.springframework.data.solr.core.query.GroupOptions;
import org.springframework.data.solr.core.query.HighlightOptions;
import org.springframework.data.solr.core.query.HighlightQuery;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleFilterQuery;
import org.springframework.data.solr.core.query.SimpleHighlightQuery;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.GroupEntry;
import org.springframework.data.solr.core.query.result.GroupPage;
import org.springframework.data.solr.core.query.result.GroupResult;
import org.springframework.data.solr.core.query.result.HighlightEntry;
import org.springframework.data.solr.core.query.result.HighlightEntry.Highlight;
import org.springframework.data.solr.core.query.result.HighlightPage;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.search.service.ItemSearchService;

@Service
public class ItemSearchServiceimpl implements ItemSearchService {
	// 注入操控solr索引库操作类
	@Autowired
	private SolrTemplate solrTemplate;
	// 注入操控redis库操作类
	@Autowired
	private RedisTemplate redisTemplate;

	@Override
	public Map search(Map searchMap) {
	
		/*
		 * Query query = new SimpleQuery("*:*"); //复制域名称 keywords取出来的是多个对象
		 * Criteria criteria = new
		 * Criteria("item_keywords").is(searchMap.get("keywords"));
		 * 
		 * query.addCriteria(criteria); ScoredPage<TbItem> page =
		 * solrTemplate.queryForPage(query , TbItem.class);
		 * //page.getContent就是当页数据 map.put("rows",page.getContent());
		 * //把查出的数据存入map 放回给客户
		 */
		
		// 创建个容器
		Map map = new HashMap<>();
		
		// 关键字空格处理 
		   String keywords = (String) searchMap.get("keywords");
		   System.out.println(keywords);
		  searchMap.put("keywords", keywords.replaceAll(" ", ""));
		 
		// 顯示高亮
		// 1. 查询列表
		map.putAll(searchList(searchMap));

		// 2.分组查询 商品分类列表
		List<String> categoryList = searchCategory(searchMap);
		map.put("categoryList", categoryList);

		// 3.查询品牌和规格列表
		String category = (String) searchMap.get("category");
		if (!category.equals("")) {
			map.putAll(searchBrandAndSpecList(category));
		} else {
			if (categoryList.size() > 0) {
				map.putAll(searchBrandAndSpecList(categoryList.get(0)));
			}
		}

		return map;
	}

	// 显示高亮 查询列表
	private Map searchList(Map searchMap) {
		Map map = new HashMap<>();
		// 关键字显示高亮效果 就是想搜索字段叫上<em>标签 你要加的高亮显示的域
		HighlightQuery query = new SimpleHighlightQuery();
		// *********************** 1.1 关键字查询列表 *********************
		// 设置3大条件
		// 在标题列上设置高亮
		HighlightOptions highilghOptions = new HighlightOptions().addField("item_title");
		// 设置前缀
		highilghOptions.setSimplePrefix("<em style='color:red'>");
		// 设置后缀
		highilghOptions.setSimplePostfix("</em>");
		// 设置高亮选项方法
		query.setHighlightOptions(highilghOptions);

		// 复制域名称 keywords取出来的是多个对象
		Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));
		// 创建查询条件
		query.addCriteria(criteria);

		// 1.2按商品分类过滤
		// 过滤条件查询
		if (!("".equals(searchMap.get("category")))) {
			// 若果客户选择了商品筛选
			FilterQuery filterQuery = new SimpleFilterQuery();
			Criteria filterCriteria = new Criteria("item_category").is(searchMap.get("category"));
			filterQuery.addCriteria(filterCriteria);
			query.addFilterQuery(filterQuery);
		}

		// 1.3按品牌分类过滤
		// 过滤条件查询
		if (!("".equals(searchMap.get("brand")))) {
			// 若果客户选择了品牌筛选
			FilterQuery filterQuery = new SimpleFilterQuery();
			Criteria filterCriteria = new Criteria("item_brand").is(searchMap.get("brand"));
			filterQuery.addCriteria(filterCriteria);
			query.addFilterQuery(filterQuery);
		}

		// 1.4按規格分类过滤
		// 过滤条件查询
		if (searchMap.get("spec") != null) {
			Map<String, String> specMap = (Map) searchMap.get("spec");
			for (String key : specMap.keySet()) {
				Criteria filterCriteria = new Criteria("item_spec_" + key).is(specMap.get(key));
				FilterQuery filterQuery = new SimpleFilterQuery(filterCriteria);
				query.addFilterQuery(filterQuery);
			}
		}

		// 1.5按价格过滤
		if (!"".equals(searchMap.get("price"))) {
			String[] price = ((String) searchMap.get("price")).split("-");
			if (!price[0].equals("0")) {
				// 如果最低价格不等于0
				FilterQuery filterQuery = new SimpleFilterQuery();
				Criteria filterCriteria = new Criteria("item_price").greaterThanEqual(price[0]);
				filterQuery.addCriteria(filterCriteria);
				query.addFilterQuery(filterQuery);
			}
			if (!price[1].equals("*")) { // 如果最高价格不等于*
				FilterQuery filterQuery = new SimpleFilterQuery();
				Criteria filterCriteria = new Criteria("item_price").lessThanEqual(price[1]);
				filterQuery.addCriteria(filterCriteria);
				query.addFilterQuery(filterQuery);
			}
		}

		/**
		 * 根据关键字搜索列表
		 * 
		 * @param keywords
		 * @return
		 */

		// 1.6 分页查询
		// 前端传过来的
		Integer pageNo = (Integer) searchMap.get("pageNo");// 就是第几页
		if (pageNo == null) {
			// 默认第一页
			pageNo = 1;
		}
		Integer pageSize = (Integer) searchMap.get("pageSize");// 每页记录数
		if (pageSize == null) {
			pageSize = 20;// 默认 20
		}
		// 从第几条记录查询就是起始索引
		query.setOffset((pageNo - 1) * pageSize);
		query.setRows(pageSize);
		// 高亮显示处理
		// 高亮页对象
		HighlightPage<TbItem> page = solrTemplate.queryForHighlightPage(query, TbItem.class);
		/*
		 * //获取页面实体数据 List<TbItem> content = page.getContent();
		 */
		// 高亮入口
		List<HighlightEntry<TbItem>> entityList = page.getHighlighted();
		for (HighlightEntry<TbItem> entity : entityList) {
			// 获取高亮列表 可能有多个域加有高亮
			List<Highlight> highlights = entity.getHighlights();
			// 就是你可能不止就三星要高亮有可能多个 如 苹果 等
			/*
			 * for(Highlight h : highlights){ //得到高亮字段 因为就复制域的存在有多个高亮结果 所以用集合装
			 * List<String> snipplets = h.getSnipplets();
			 * System.out.println(snipplets); }
			 */

			if (highlights.size() > 0 && highlights.get(0).getSnipplets().size() > 0) {
				// 获取页面实体数据 与page.getContent();操作的是同一个对象
				TbItem tbItem = entity.getEntity();
				// 把数据设置成高亮存回去
				tbItem.setTitle(highlights.get(0).getSnipplets().get(0));
			}

		}
		// 1.7排序
		// 接受排序字段 升序ASC 降序DESC
		String sortValue = (String) searchMap.get("sort");
		// 排序字段
		String sortField = (String) searchMap.get("sortField");
		System.out.println(sortField+","+sortValue);
		// 传来为空那就不排序
		if (!sortValue.equals("") && sortValue != null) {
			
			// 为升序
			if (sortValue.equals("ASC")) {
				Sort sort = new Sort(Sort.Direction.ASC, "item_"+sortField);
				System.out.println("升序"+sortValue+"  "+sortField);
				query.addSort(sort);
			}
			// 为降序
			if (sortValue.equals("DESC")) {
				
				Sort sort = new Sort(Sort.Direction.DESC, "item_"+sortField);
				System.out.println("降序"+sortValue+"  "+sortField);
				query.addSort(sort);
			}
		}

		// 放回数据
		map.put("rows", page.getContent());
		// 返回总页数
		map.put("totalPages", page.getTotalPages());
		// 返回总记录数
		map.put("total", page.getTotalElements());
		return map;
	}

	// 获取规格分类的商品名称分组
	private List<String> searchCategory(Map searchMap) {
		// 构建容器
		List list = new ArrayList<>();

		Query query = new SimpleQuery("*:*"); // 复制域名称 keywords取出来的是多个对象
		// 创建条件查询
		Criteria criteria = new Criteria("item_keywords").is(searchMap.get("keywords"));

		// 设置分组选项 根据什么来分组
		GroupOptions groupOptions = new GroupOptions().addGroupByField("item_category");

		query.setGroupOptions(groupOptions);
		// 获取分组页对象
		GroupPage<TbItem> page = solrTemplate.queryForGroupPage(query, TbItem.class);

		// 获取分组入口集合 应该不止一个分组条件addGroupByField("item_category");后面还可以追加
		GroupResult<TbItem> groupResult = page.getGroupResult("item_category");

		// 获取获取真正的分组页对象
		Page<GroupEntry<TbItem>> groupEntries = groupResult.getGroupEntries();
		for (GroupEntry<TbItem> entries : groupEntries) {
			// 获取每个分组字段存入容器中
			list.add(entries.getGroupValue());
		}

		return list;
	}

	// 根据商品分类来显示规格列表和品牌
	private Map searchBrandAndSpecList(String categoryName) {
		Map map = new HashMap();
		// 通过商品名称得到模板ID
		Long typeId = (Long) redisTemplate.boundHashOps("itemCat").get(categoryName);

		if (typeId != null) {
			// 通过模板id得到商品分类列表
			List brandList = (List) redisTemplate.boundHashOps("brandList").get(typeId);
			map.put("brandList", brandList);
			// 通过模板id得到规格分类列表
			List specList = (List) redisTemplate.boundHashOps("specList").get(typeId);
			map.put("specList", specList);
		}

		return map;

	}
   //跟新solr的所有库数据 就是跟新后台运营商通过审核的商品
	@Override
	public void ImportList(List list) {
		
		//插入
		solrTemplate.saveBeans(list);
		//提交
		solrTemplate.commit();
	}
	/**
	 * 删除同步数据库
	 * 
	 * @param keywords
	 * @return
	 */
	@Override
	public void deleteByGoodsIds(List goodsIdList) {
		  System.out.println("删除商品 ID"+goodsIdList); 
		  Query query=new SimpleQuery("*:*");   
		  Criteria criteria=new Criteria("item_goodsid").in(goodsIdList); 

		  query.addCriteria(criteria); 
		  solrTemplate.delete(query); 
		  solrTemplate.commit(); 
		
	}

	

}
