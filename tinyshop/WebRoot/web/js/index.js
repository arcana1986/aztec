

function viewContent(contentId){
	window.location.href=ctx_path+"/contentView?contentId="+contentId; 
}

function viewList(contentCategoryId){
	window.location.href=ctx_path+"/contentListView?contentCategoryId="+contentCategoryId; 
}

function viewHome()
{
	window.location.href=ctx_path+"/"; 
}

function viewShop()
{
	window.location.href=ctx_path+"/shopView"; 
}

function viewHomePage()
{
    window.location.href=ctx_path+"/index"; 
}

function doPage(pageNum,pageIndex,pageCount){
	if(pageNum>pageCount){
		pageNum=pageCount==0?1:pageCount;
	}else if(pageNum<1){
		pageNum=1;
	}
	if(pageIndex!=pageNum){
		$("#pageIndex").val(pageNum);
		query();
	}
}

function pad(num) {  
	var a = num.toString();
	var s = a.indexOf(".");
	if(s==-1)
	{
		a = a+".00";
	}
    return a;  
}  
