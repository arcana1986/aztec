package com.sosee.sys.base.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import com.jfinal.aop.Before;
import com.jfinal.kit.JsonKit;
import com.jfinal.kit.StringKit;
import com.sosee.sys.base.interceptor.LoginInterceptor;
import com.sosee.sys.base.pojo.BaseCode;
import com.sosee.sys.base.pojo.BaseDict;
import com.sosee.sys.log.util.LoggerUtil;
import com.sosee.sys.tree.TreeNode;
import com.sosee.sys.base.validator.BaseCodeValidator;

/**
 * @author :outworld
 * @date :2012-12-6 下午5:39:51
 * @Copyright:2012 outworld Studio Inc. All rights reserved.
 * @function:
 */
@Before(LoginInterceptor.class)
public class BaseCodeController extends BaseController {
	/**
	 * 默认打开时做两件事： 1、构建目录树 2、先查询出第一个字典数据
	 */
	public void index() {
		// 把树json放入缓存
		
		this.createToken("baseCodeToken",1800);
		setAttr("treeJson", createTree());
		render("/WEB-INF/sys/user/baseCodeList.html");
	}

	/**
	 * 构建树 通过t_basedict中的typeName分大类，然后根据分类创建二级字典数，最后根据第一条数据来查询出第一个字典的数据
	 * 
	 * @return
	 */
	public String createTree() {
		// 先找出大分类
		List<BaseDict> baseDictParent = getModel(BaseDict.class).find(
				"select typeName from t_basedict group by typeName");

		List<BaseDict> baseDictChildren = new ArrayList<BaseDict>();

		List<TreeNode> treeNodeParent = new ArrayList<TreeNode>();

		if (baseDictParent != null && baseDictParent.size() > 0) {
			int s = 0;
			// 外循环找出大分类,作为一级树节点
			for (BaseDict bDict : baseDictParent) {
				String typeName = bDict.get("typeName");
				TreeNode pNode = new TreeNode();
				pNode.setId(typeName);// 用名称作为id
				pNode.setpId("-1");
				pNode.setName(typeName);
				pNode.setOpen(s++ > 0 ? false : true);
				pNode.setParent(true);
				pNode.setClick("");
				pNode.setTarget("_self");
				pNode.setUrl("#");
				pNode.setChildren(null);// 先把子节点默认为null
				// 根据分类名称找出对应的子树
				baseDictChildren = getModel(BaseDict.class).find(
						"select * from t_basedict where typeName='" + typeName
								+ "'");
				List<TreeNode> treeNodechild = new ArrayList<TreeNode>();

				if (baseDictChildren != null && baseDictChildren.size() > 0) {
					for (BaseDict child : baseDictChildren) {
						if (s == 1) {
							// 默认把第一个子节点的分类保存下来
							setAttr("baseDict", child);
							// 把第一个字典放入缓存
							setAttr("baseCodeList",
									getModel(BaseCode.class)
											.find("select * from t_basecode where categoryId='"
													+ child.getStr("categoryId")
													+ "' order by orderNum"));
						}
						TreeNode cNode = new TreeNode();
						cNode.setpId(child.getStr("categoryId"));
						cNode.setId(child.getStr("id"));
						cNode.setpId(typeName);
						cNode.setName(child.getStr("categoryName"));
						cNode.setUrl("#");
						cNode.setOpen(false);
						cNode.setParent(false);
						cNode.setClick("javascript:queryList('"
								+ this.getAttrForStr("ctx_path")
								+ "/baseCode/queryList/"
								+ child.getStr("categoryId") + "')");
						cNode.setTarget("_self");
						cNode.setChildren(null);
						treeNodechild.add(cNode);
					}
				}
				pNode.setChildren(treeNodechild);
				treeNodeParent.add(pNode);
			}
		}
		String jsonString = JsonKit.listToJson(treeNodeParent, 4);
		return jsonString;
	}

	
	/**
	 * 如果首次查询得到第一个字典的数据，否则查询当时的字典数据,此为公共方法
	 * 
	 * @return
	 */
	private List<BaseCode> queryListObject() {
		String categoryId = this.getPara();// 得到点击父节点分类标识
		if (categoryId == null || categoryId.equals("")) {
			BaseCode baseCode = getModel(BaseCode.class);
			categoryId = baseCode.getStr("categoryId");
		}
		String sqlParam = "";
		if (StringKit.notBlank(categoryId)) {
			sqlParam += " and categoryId ='" + categoryId.trim() + "'";
			// 从字典分类找对分类对象放入缓存中,只能存在一条分类标识，多个分类标识又进行了分组归类
			setAttr("baseDict",
					getModel(BaseDict.class).find(
							"select * from t_basedict where categoryId='"
									+ categoryId + "'"));
		}
		return getModel(BaseCode.class).find(
				"select * from t_basecode where 1=1 " + sqlParam
						+ " order by orderNum");
	}

	/**
	 * 用于异步调用时生成查询出的字典列表
	 */
	public void queryList() {
		List<BaseCode> baseCodes = queryListObject();

		if (baseCodes != null && baseCodes.size() > 0) {
			renderJson("{\"baseCodeToken\":\""+createNewToken()+"\",\"data\":"+JsonKit.listToJson(queryListObject(), 2)+"}");
		} else {
			this.renderJson("{\"baseCodeToken\":\""+createNewToken()+"\",\"errorinfo\":\"没有数据!\"}");
		}
	}

	@Before(BaseCodeValidator.class)
	public void save() {
		try {
			BaseCode baseCode = getModel(BaseCode.class);
			boolean bInfo = false;
			
			if (baseCode.getStr("id") != null
					&& !baseCode.getStr("id").trim().equals("")) {
				// 编辑
				this.setAttr("type", "edit");
				bInfo = baseCode.update();
			} else {
				// 添加
				this.setAttr("type", "add");
				
				baseCode.set("id", UUID.randomUUID().toString());
				bInfo = baseCode.save();
			}
			if (bInfo) {
				LoggerUtil.business.info("基础字典_成功_"+baseCode.getStr("name")+"维护");
				this.setAttr("msg", "数据保存成功!");
			} else {
				this.setAttr("errorinfo", "数据保存失败!");
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.keepModel(BaseCode.class);
			this.setAttr("errorinfo", "数据保存失败!");
		}
		queryList();
	}

	private String createNewToken(){
	  this.createToken("baseCodeToken",1800);	
	  return this.getAttrForStr("baseCodeToken");
	}
	
	public void edit() {
		try {
			String id = this.getPara();
			if (StringKit.notBlank(id)) {
				BaseCode baseCode = this.getModel(BaseCode.class).findById(id);
				if (baseCode != null && !baseCode.equals("")) {
					setAttr("baseDict",
							getModel(BaseDict.class).find(
									"select * from t_basedict where categoryId='"
											+ baseCode.getStr("categoryId")
											+ "'"));
					this.renderJson("{\"result\":1,\"baseCode\":"
							+ baseCode.toJson() + ",\"msg\":\"\"}");
				} else {
					this.renderJson("{\"result\":0,\"msg\":\"查询出错!\"}");
				}
			} else {
				this.renderJson("{\"result\":0,\"msg\":\"查询出错!\"}");
			}
		} catch (Exception e) {
			this.renderJson("{\"result\":0,\"msg\":\"查询出错!\"}");
		}
	}

	/**
	 * 删除一条记录
	 */
	public void del() {
		String id = getPara();
		BaseCode baseCode = getModel(BaseCode.class).findById(id);
		try {
			 boolean bInfo = baseCode.deleteById(baseCode.getStr("id"));
			 if (bInfo) {
				this.setAttr("msg", "数据删除成功!");
				LoggerUtil.business.info("基础字典_成功_"+baseCode.getStr("name")+"删除");
			} else {
				this.setAttr("errorinfo", "数据删除失败!");
			}
	 	  } catch (Exception e) {
			this.setAttr("errorinfo", "数据删除失败!");
		}
		setAttr("baseDict",
				getModel(BaseDict.class).find(
						"select * from t_basedict where categoryId='"
								+ baseCode.getStr("categoryId") + "'"));
		List<BaseCode> baseCodes=getModel(BaseCode.class).find("select * from t_basecode where categoryId='"+baseCode.getStr("categoryId")+"' order by orderNum");
		if(baseCodes!=null && baseCodes.size()>0){
			renderJson(JsonKit.listToJson(baseCodes, 2));
		}else{
			this.renderJson("{\"result\":0,\"msg\":\"没有数据!\"}");
		}
		
	}

   }
