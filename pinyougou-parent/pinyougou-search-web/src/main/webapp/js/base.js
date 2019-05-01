// 定义模块:
var app = angular.module("pinyougou",[]);

/*$sce 服务写成过滤器*/ 
app.filter('trustHtml',['$sce',function($sce){ 
    return function(data){ //要过滤的内容
    	//放回过滤的内容
        return $sce.trustAsHtml(data); 
    } 
}]); 