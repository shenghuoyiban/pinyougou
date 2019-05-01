package com.pinyougou.content.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.content.service.ContentService;
import com.pinyougou.mapper.TbContentMapper;
import com.pinyougou.pojo.TbContent;
import com.pinyougou.pojo.TbContentExample;
import com.pinyougou.pojo.TbContentExample.Criteria;

import entity.PageResult;

/**
 * 服务实现层
 * 
 * @author Administrator
 *
 */
@Service
public class ContentServiceImpl implements ContentService {

	@Autowired
	private TbContentMapper contentMapper;
	// 注入redis操作对象
	@Autowired
	private RedisTemplate redisTemplate;
	

	/**
	 * 查询全部
	 */
	@Override
	public List<TbContent> findAll() {
		return contentMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		Page<TbContent> page = (Page<TbContent>) contentMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbContent content) {
		// 先把redis数据全删了 页面有执行findByCategoryId方法会重新在存进去
		// 那组广告要添加 我就清那个组 然后在findByCategoryId在重新存进redis中
		redisTemplate.boundHashOps("content").delete(content.getCategoryId());
		contentMapper.insert(content);
	}

	/**
	 * 修改
	 */
	@Override
	public void update(TbContent content) {
		// 有可能categoryId不一样啦所以要先查一下

		Long categoryId = contentMapper.selectByPrimaryKey(content.getId()).getCategoryId();

		// 先删除之前的categoryid的redis数据
		redisTemplate.boundHashOps("content").delete(categoryId);

		// 判断之前categoryId跟跟新后一样不
		contentMapper.updateByPrimaryKey(content);
		if (categoryId.longValue() != content.getCategoryId().longValue()) {
			// 不一样 清除之后跟新categoryid之后的数据
			redisTemplate.boundHashOps("content").delete(content.getCategoryId());
		}
	}

	/**
	 * 根据ID获取实体
	 * 
	 * @param id
	 * @return
	 */
	@Override
	public TbContent findOne(Long id) {
		return contentMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for (Long id : ids) {
		     //先查詢 categoryid在刪除
			redisTemplate.boundHashOps("content").delete(contentMapper.selectByPrimaryKey(id).getCategoryId());
			contentMapper.deleteByPrimaryKey(id);
		}
	}

	@Override
	public PageResult findPage(TbContent content, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);

		TbContentExample example = new TbContentExample();
		Criteria criteria = example.createCriteria();

		if (content != null) {
			if (content.getTitle() != null && content.getTitle().length() > 0) {
				criteria.andTitleLike("%" + content.getTitle() + "%");
			}
			if (content.getUrl() != null && content.getUrl().length() > 0) {
				criteria.andUrlLike("%" + content.getUrl() + "%");
			}
			if (content.getPic() != null && content.getPic().length() > 0) {
				criteria.andPicLike("%" + content.getPic() + "%");
			}
			if (content.getStatus() != null && content.getStatus().length() > 0) {
				criteria.andStatusLike("%" + content.getStatus() + "%");
			}

		}

		Page<TbContent> page = (Page<TbContent>) contentMapper.selectByExample(example);
		return new PageResult(page.getTotal(), page.getResult());
	}

	// 根据广告分类id查询广告列表
	@Override
	public List<TbContent> findByCategoryId(Long categoryId) {
		// 去数据库查询之前 先去redis缓存中看看 key是categoryId value是个content集合
		List<TbContent> list = (List<TbContent>) redisTemplate.boundHashOps("content").get(categoryId);
		if (list == null) {
			
			System.out.println("我从数据库中查询数据");
			// redis中查询不到数据 在去数据库查
			TbContentExample contentExample = new TbContentExample();
			Criteria criteria = contentExample.createCriteria();
			criteria.andCategoryIdEqualTo(categoryId);
			criteria.andStatusEqualTo("1");// 开启状态
			contentExample.setOrderByClause("sort_order");// 根据数据库字段排序
			list = contentMapper.selectByExample(contentExample);
			redisTemplate.boundHashOps("content").put(categoryId, list);


		} else {
			System.out.println("我从redis中查询数据");

		}
		return list;

	}

}
