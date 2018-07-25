var container = document.getElementById('visualization');
timeFormat = 'ss';
// Create a DataSet (allows two way data-binding)
items = new vis.DataSet();
// Configuration for the Timeline
var options = {
		 
	dataAxis:{ left:{
		title:{text:''}, range:{min:0}, 
		format:function (value) {
			  return value;
		}
	}
		
	},
	height : "450px",
	width : "700px",

	zoomMin:1,
	//showMinorLabels:false,
	showMajorLabels:false,
	zoomMax:5*60*1000,
	//horizontalScroll:true,
	//zoomable:false,
	

	format:{
		  minorLabels: {
			    millisecond:'mm:ss.SS',
			    second:     'mm:ss.SS',
			    minute:     'mm:ss.SS',
			    hour:       'mm:ss.SS',
			    weekday:    'mm:ss.SS',
			    day:        'mm:ss.SS',
			    //week:       '',
			    month:      'mm:ss.SS',
			    year:       'mm:ss.SS'
			  },
			  majorLabels: {
			    millisecond:'mm:ss.SS',
			    second:     'mm:ss.SS',
			    minute:     'mm:ss.SS',
			    hour:       'mm:ss.SS',
			    weekday:    'mm:ss.SS',
			    day:        'mm:ss.SS',
			   // week:       'MMMM YYYY',
			    month:      'mm:ss.SS',
			    year:       'mm:ss.SS'
			  }
			}
};


// Create a Timeline
var timeline=null;

var groupData = {
		style:'stroke:#3989a6;fill:#3989a6;',
        id: 0,
        content: "Group Name",
        options: {
            drawPoints: {
                styles: 'stroke:#3989a6;fill:#3989a6;' ,// square, circle
                style:'square',
            }
        }
    };

    var groups = null;


	

	function buildChart(chartData){
		items.clear();
		if (groups==null){
			groups=new vis.DataSet();
	    	groups.add(groupData);

		}
    	container = document.getElementById('visualization');
    	var res = chartData.split(";");
    	var last= res[res.length-1];
    	var tokens=last.split(':');
    	allReads= Number(tokens[0]);
    	valid=allReads - Number(tokens[1]);
    	var x=(0)+(8*60*60*1000);
		var time=moment(x,"x");
		var yval=Number((valid/allReads))*100;
		
		
//		alert ("ffirst="+time+","+yval);
		items.add({x:time,y:yval, group:0, title:'cc'});
//    	alert("AllReads="+allReads+", valids="+valid);
    	for (var i=0; i<res.length-1;i++){
    		tokens=res[i].split(':');
    		var x=Number(tokens[1])+(8*60*60*1000);
    		var time=moment(x,"x");
    		allReads=allReads - Number(tokens[2]);
    		if (allReads==0)
    			break;
    		var myvalid= Number(tokens[2])-Number(tokens[3]);
    		valid= valid-myvalid;
    		yval=(valid/allReads)*100;
//    		alert("xxx="+time+" ,y="+yval+", valid="+valid+" allR="+allReads);
    		items.add({x:time,y:yval, group:0, title:'cc'});
    	}
    	if (timeline==null)
    	timeline = new vis.Graph2d(container, items, groups,options);
    	timeline.fit();
    	var keys = Object.keys(items);
    	var len = keys.length;
    	
    	if (res.length>1)
    	document.getElementById("fdiv").style.visibility="inherit";
    	

    }
interval=null;
function getData() {
	if (interval==null)
	loading();
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			if (xhttp.responseText.trim() != "null")
				fill(xhttp.responseText.trim());
			if (interval!=null)
				clearInterval(interval);
				interval=setInterval(getData,20000);
		} else {
			// document.getElementById("output").innerHTML = "Failed. readyState
			// = " + xhttp.readyState + "... status = " + xhttp.status;
		}
	};
	xhttp.open("POST", "vGetStats2.jsp", true);
	xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	xhttp.send();
	
}

function fill(data) {
	if (data.startsWith('ERROR')) {
		alert(data);
	} else {
		var obj = JSON.parse(data);
		var table = document.getElementById("ERTable");
		while (table.childElementCount > 0) {
			table.removeChild(table.lastChild);
		}
		updateStats(obj.stats);
		updateFreshness(obj.stats);
		for (var i = 0; i < obj.stales.entities.length; i++) {
			var tr = document.createElement("TR");
			var tdName = document.createElement("TD");
			tdName.setAttribute("width", "126px");
			var tdCount = document.createElement("TD");
			tdCount.setAttribute("width", "100px");
			tdCount.setAttribute("style", "text-align:center;");
			var link = document.createElement("A");
			// link.setAttribute("target", "_blank");
			link.setAttribute("href", "vTimeline2.jsp?key=" + obj.stales.entities[i].name + "&type=e");
			link.appendChild(document.createTextNode(obj.stales.entities[i].name));
			tdName.appendChild(link);
			tdCount.appendChild(document.createTextNode(obj.stales.entities[i].num));
			tr.appendChild(tdName);
			tr.appendChild(tdCount);
			table.appendChild(tr);
		}
		table = document.getElementById("TTable");
		while (table.childElementCount > 0) {
			table.removeChild(table.lastChild);
		}
		for (var i = 0; i < obj.stales.transactions.length; i++) {
			var tr = document.createElement("TR");
			var tdName = document.createElement("TD");
			tdName.setAttribute("width", "126px");
			var tdCount = document.createElement("TD");
			tdCount.setAttribute("width", "100px");
			tdCount.setAttribute("style", "text-align:center;");
			var link = document.createElement("A");
			// link.setAttribute("target", "_blank");
			link.setAttribute("href", "vTimeline2.jsp?key=" + obj.stales.transactions[i].name + "&type=t");
			link.appendChild(document.createTextNode(obj.stales.transactions[i].name));
			tdName.appendChild(link);
			tdCount.appendChild(document.createTextNode(obj.stales.transactions[i].num));
			tr.appendChild(tdName);
			tr.appendChild(tdCount);
			table.appendChild(tr);
		}
	}
	if (interval==null)
	doneLoading();
}

function updateFreshness(obj){
	var text;
	for(var i = 0; i < obj.length-1; i++){
		text=obj[i].bucket;

	}
	if (text!=null){
//	alert("building..");
//		document.getElementById("flabel").style.visibility="inherit";
//		document.getElementById("tlabel").style.visibility="inherit";
		buildChart(text);
	}
}

function updateStats(obj) {
	mainTR = document.getElementById("stats");
	while (mainTR.childElementCount > 0) {
		mainTR.removeChild(mainTR.lastChild);
	}
	for(var i = 0; i < obj.length; i++){
		var mainTD = document.createElement("TD");
		//mainTD.setAttribute("style", "padding: 0px");
		var table = document.createElement("table");
		table.setAttribute("class", "evenOdd");
		table.setAttribute("style", "border-collapse: collapse;");
		var tr = document.createElement("TR");
		var td = document.createElement("TD");
		td.appendChild(document.createTextNode(obj[i].cID));
		td.setAttribute("style", "padding: 1px 3px;");
		tr.appendChild(td);
		table.appendChild(tr);
		tr = document.createElement("TR");
		td = document.createElement("TD");
		td.appendChild(document.createTextNode(obj[i].reads));
		td.setAttribute("style", "padding: 1px 3px;");
		tr.appendChild(td);
		table.appendChild(tr);
		tr = document.createElement("TR");
		td = document.createElement("TD");
		td.appendChild(document.createTextNode(obj[i].writes));
		td.setAttribute("style", "padding: 1px 3px;");
		tr.appendChild(td);
		table.appendChild(tr);
		tr = document.createElement("TR");
		td = document.createElement("TD");
		td.appendChild(document.createTextNode(obj[i].stales));
		td.setAttribute("style", "padding: 1px 3px;");
		tr.appendChild(td);
		table.appendChild(tr);
		tr = document.createElement("TR");
		td = document.createElement("TD");
		td.appendChild(document.createTextNode(obj[i].avgSS));
		td.setAttribute("style", "padding: 1px 3px;");
		tr.appendChild(td);
		table.appendChild(tr);
		tr = document.createElement("TR");
		td = document.createElement("TD");
		td.appendChild(document.createTextNode(obj[i].maxSS));
		td.setAttribute("style", "padding: 1px 3px;");
		tr.appendChild(td);
		table.appendChild(tr);
		tr = document.createElement("TR");
		td = document.createElement("TD");
		td.appendChild(document.createTextNode(obj[i].avgMem));
		td.setAttribute("style", "padding: 1px 3px;");
		tr.appendChild(td);
		table.appendChild(tr);
		tr = document.createElement("TR");
		td = document.createElement("TD");
		td.appendChild(document.createTextNode(obj[i].maxMem));
		td.setAttribute("style", "padding: 1px 3px;");
		tr.appendChild(td);
		table.appendChild(tr);
		tr = document.createElement("TR");
		td = document.createElement("TD");
		td.appendChild(document.createTextNode(obj[i].pDis));
		td.setAttribute("style", "padding: 1px 3px;");
		tr.appendChild(td);
		table.appendChild(tr);
		tr = document.createElement("TR");
		td = document.createElement("TD");
		td.appendChild(document.createTextNode(obj[i].fDis));
		td.setAttribute("style", "padding: 1px 3px;");
		tr.appendChild(td);
		table.appendChild(tr);
		tr = document.createElement("TR");
		td = document.createElement("TD");
		td.appendChild(document.createTextNode(obj[i].duration));
		td.setAttribute("style", "padding: 1px 3px;");
		tr.appendChild(td);
		table.appendChild(tr);
		mainTD.appendChild(table);
		mainTR.appendChild(mainTD);
	}
}

function loading(){
	var eTable = document.getElementById("eTable");
	var tTable = document.getElementById("tTable");
	var eTable_loadPic = document.createElement("img");
	var tTable_loadPic = document.createElement("img");
	var eTable_layerPic = document.createElement("img");
	var tTable_layerPic = document.createElement("img");
	var body = document.getElementById("body");
	eTable_loadPic.setAttribute("id", "eloadingPic");
	eTable_loadPic.setAttribute("src", "img/ajax-loader.gif");
	tTable_loadPic.setAttribute("id", "tloadingPic");
	tTable_loadPic.setAttribute("src", "img/ajax-loader.gif");
	var eNode = eTable;
	var eLeft = 0, eTop = 0;
	while (eNode.tagName !== "BODY") {
		eLeft += eNode.offsetLeft;
		eTop += eNode.offsetTop;
		eNode = eNode.offsetParent;
	}
	var ePicleft = eLeft + eTable.offsetWidth / 2 - 110;
	var ePictop = eTop + eTable.offsetHeight / 2 - 10;
	eTable_loadPic.setAttribute("style", "position:absolute;left:" + ePicleft + "px;top:" + ePictop + "px;z-index:2");
	eTable_layerPic.setAttribute("id", "elayerPic");
	eTable_layerPic.setAttribute("src", "img/layer.png");
	eTable_layerPic.setAttribute("width", eTable.offsetWidth + "px");
	eTable_layerPic.setAttribute("height", eTable.offsetHeight + "px");
	eTable_layerPic.setAttribute("style", "position:absolute;left:" + eLeft + "px;top:" + eTop + "px;z-index:1");
	body.appendChild(eTable_loadPic);
	body.appendChild(eTable_layerPic);
	
	var tNode = tTable;
	var tLeft = 0, tTop = 0;
	while (tNode.tagName !== "BODY") {
		tLeft += tNode.offsetLeft;
		tTop += tNode.offsetTop;
		tNode = tNode.offsetParent;
	}
	var tPicleft = tLeft + tTable.offsetWidth / 2 - 110;
	var tPictop = tTop + tTable.offsetHeight / 2 - 10;
	tTable_loadPic.setAttribute("style", "position:absolute;left:" + tPicleft + "px;top:" + tPictop + "px;z-index:2");
	tTable_layerPic.setAttribute("id", "tlayerPic");
	tTable_layerPic.setAttribute("src", "img/layer.png");
	tTable_layerPic.setAttribute("width", tTable.offsetWidth + "px");
	tTable_layerPic.setAttribute("height", tTable.offsetHeight + "px");
	tTable_layerPic.setAttribute("style", "position:absolute;left:" + tLeft + "px;top:" + tTop + "px;z-index:1");
	body.appendChild(tTable_loadPic);
	body.appendChild(tTable_layerPic);
}

function doneLoading(){
	var pic = document.getElementById("eloadingPic");
	pic.parentNode.removeChild(pic);
	pic = document.getElementById("elayerPic");
	pic.parentNode.removeChild(pic);
	pic = document.getElementById("tloadingPic");
	pic.parentNode.removeChild(pic);
	pic = document.getElementById("tlayerPic");
	pic.parentNode.removeChild(pic);
}