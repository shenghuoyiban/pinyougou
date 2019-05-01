package com.pinyougou.search.service;

import java.util.List;
import java.util.Map;

public interface  ItemSearchService {
      
	//规格搜索
	public Map search(Map searchMap);
	
	
	//批量导入
	public void ImportList(List list);
	public void deleteByGoodsIds(List goodsIdList); 
}
