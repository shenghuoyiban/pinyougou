app.controller('searchController',function($scope,searchService,$location){
	
	//商品分类
	$scope.searchMap={'keywords':'','category':'','brand':'','spec':{},'price':'','pageNo':1,'pageSize':40,'sort':'','sortField':''};//搜索对象
	//搜索
	$scope.search=function(){
		//转换为数字
		$scope.searchMap.pageNo= parseInt($scope.searchMap.pageNo);
		searchService.search($scope.searchMap).success(
			function(response){
				$scope.resultMap=response;
				
				//构建分页栏
				buildPageLabel();
			}
		);		
	}
	//面包屑
	$scope.addSearchItem=function(key,value){
		//用户点击品牌和商品分类
		if(key=='category'||key=='brand'||key=='price'){
			//点击的具体名称给value
			$scope.searchMap[key]=value;
		}else{
			//客户点击的是规格
			$scope.searchMap.spec[key]=value;
		}
		//刷新
		$scope.search();
	}
	//清空面包屑
	$scope.removeSearchItem=function(key){
		//用户点击品牌和商品分类
		if(key=='category'||key=='brand'||key=='price'){
			//清空
			$scope.searchMap[key]="";
		}else{
			//清空
		   delete $scope.searchMap.spec[key];
		}
		//刷新
		$scope.search();
	}
	
	
	//构建分页栏	
	buildPageLabel=function(){
		//构建分页栏
		$scope.pageLabel=[];
		var firstPage=1;//开始页码
		var lastPage=$scope.resultMap.totalPages;//截止页码
		$scope.firstDot=true;//前面有点
		$scope.lastDot=true;//后边有点
		
		if($scope.resultMap.totalPages>5){  
			//如果页码数量大于5
			
			if($scope.searchMap.pageNo<=3){
				//如果当前页码小于等于3 ，显示前5页
				lastPage=5;
				//前面没点
				$scope.firstDot=false;
				
			}else if( $scope.searchMap.pageNo>= $scope.resultMap.totalPages-2 ){//显示后5页
				firstPage=$scope.resultMap.totalPages-4;	
				$scope.lastDot=false;//后边没点
			}else{  
				//显示以当前页为中心的5页
				firstPage=$scope.searchMap.pageNo-2;
				lastPage=$scope.searchMap.pageNo+2;
			}			
		}else{
			$scope.firstDot=false;//前面无点
			$scope.lastDot=false;//后边无点
		}
		
		
		//构建页码
		for(var i=firstPage;i<=lastPage;i++){
			$scope.pageLabel.push(i);
		}
	}
	
		//分页查询
	$scope.queryByPage=function(pageNo){
		if(pageNo<1 || pageNo>$scope.resultMap.totalPages){
			return ;
		}		
		$scope.searchMap.pageNo=pageNo;
		$scope.search();//查询
	}
	
	//排序查询
	$scope.sortSearch=function(sortField,sort){
		$scope.searchMap.sortField=sortField;
		$scope.searchMap.sort=sort;
		
		$scope.search();//查询
	}
	
	
	
		//判断当前页是否为第一页
	$scope.isTopPage=function(){
		if($scope.searchMap.pageNo==1){
			return true;
		}else{
			return false;
		}		
	}
	
	//判断当前页是否为最后一页
	$scope.isEndPage=function(){
		if($scope.searchMap.pageNo==$scope.resultMap.totalPages){
			return true;
		}else{
			return false;
		}	
	}
	
	//判断是否是品牌关键字
    $scope.keywordsIsBrand=function(){
    	//循环品牌列表
    	for(var i =0;i<$scope.resultMap.brandList.length;i++){
    		//indexof判断集合中有没有这个值  brandList就是规格表中brandIds格式：[{"id":1,"text":"联想"},{"id":3,"text":"三星"},{"id":2,"text":"华为"},{"id":5,"text":"OPPO"},{"id":4,"text":"小米"},{"id":9,"text":"苹果"},{"id":8,"text":"魅族"},{"id":6,"text":"360"},{"id":10,"text":"VIVO"},{"id":11,"text":"诺基亚"},{"id":12,"text":"锤子"}]
    		if($scope.searchMap.keywords.indexOf($scope.resultMap.brandList[i].text)>=0){
    			
    			//包含
    			return true;
    		}
    	}
    	//不包含
    	return false;
    }
    
    
    //获取主页跳转关键字
    
    $scope.loadkeywords=function(){
    	$scope.searchMap.keywords=$location.search()['keywords'];
    	$scope.search();//查询
    }
});