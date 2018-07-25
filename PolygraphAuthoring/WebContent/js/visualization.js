var getMoreCount = -1;
var itemsArr = [];
var items_data = [];
var numOfLogsBeforeLast = 1000;
var selectedIndex = -1;
var currentKey;
var type;
var rO;
var wO;
var appName;

// DOM element where the Timeline will be attached
var container = document.getElementById('visualization');
// Create a DataSet (allows two way data-binding)
items = new vis.DataSet();
// Configuration for the Timeline
var options = {
		  showTooltips: true,
	editable:false,
	height : "450px",
	width : "700px",


	//showMinorLabels:false,
	showMajorLabels:false,
//	zoomMax:1000 * 2,
	//horizontalScroll:true,
	//zoomable:false,
	  tooltip: {
	      followMouse: true,
	      overflowMethod:'cap',
	    },

	format:{
		  minorLabels: {
			    millisecond:'ss.SSS',
			    second:     'HH:mm:ss',
			    minute:     'HH:mm:ss',
			    hour:       'HH:mm:ss',
			    weekday:    'HH:mm:ss',
			    day:        'HH:mm:ss',
			    //week:       '',
			    month:      'HH:mm:ss',
			    year:       'HH:mm:ss'
			  },
			  majorLabels: {
			    millisecond:'HH:mm:ss',
			    second:     'HH:mm:ss',
			    minute:     'HH:mm:ss',
			    hour:       'HH:mm:ss',
			    weekday:    'HH:mm:ss',
			    day:        'HH:mm:ss',
			   // week:       'MMMM YYYY',
			    month:      'HH:mm:ss',
			    year:       'HH:mm:ss'
			  }
			}
};
// Create a Timeline
var timeline;

/**
 * Move the timeline a given percentage to left or right
 * 
 * @param {Number}
 *            percentage For example 0.1 (left) or -0.1 (right)
 */
function move(percentage) {
	var range = timeline.getWindow();
	var interval = range.end - range.start;

	timeline.setWindow({
		start : range.start.valueOf() - interval * percentage,
		end : range.end.valueOf() - interval * percentage
	});
}

/**
 * Zoom the timeline a given percentage in or out
 * 
 * @param {Number}
 *            percentage For example 0.1 (zoom out) or -0.1 (zoom in)
 */
function zoom(percentage) {
	var range = timeline.getWindow();
	var interval = range.end - range.start;

	timeline.setWindow({
		start : range.start.valueOf() - interval * percentage,
		end : range.end.valueOf() + interval * percentage
	});
}

function getMore(pix, restart) {
	if (restart) {
		getMoreCount = -1;
		itemsArr = [];
		items_data = [];
	}
	getMoreCount++;
	if (weCanGetMore()) {
		if (pix) {
			document.getElementById("getMore").disabled = true;
			var visualization = document.getElementById("visualization");
			var loadPic = document.createElement("img");
			var layerPic = document.createElement("img");
			var body = document.getElementById("body");
			loadPic.setAttribute("id", "loadingPic");
			loadPic.setAttribute("src", "img/ajax-loader.gif");
			var node = visualization;
			var left = 0, top = 0;
			while (node.tagName !== "BODY") {
				left += node.offsetLeft;
				top += node.offsetTop;
				node = node.offsetParent;
			}
			var picleft = left + visualization.offsetWidth / 2 - 110;
			var pictop = top + visualization.offsetHeight / 2 - 10;
			loadPic.setAttribute("style", "position:absolute;left:" + picleft + "px;top:" + pictop + "px;z-index:2");
			layerPic.setAttribute("id", "layerPic");
			layerPic.setAttribute("src", "img/layer.png");
			layerPic.setAttribute("width", visualization.offsetWidth + "px");
			layerPic.setAttribute("height", visualization.offsetHeight + "px");
			layerPic.setAttribute("style", "position:absolute;left:" + left + "px;top:" + top + "px;z-index:1");
			body.appendChild(loadPic);
			body.appendChild(layerPic);
		}

		var xhttp = new XMLHttpRequest();
		xhttp.onreadystatechange = function() {
			if (xhttp.readyState == 4 && xhttp.status == 200) {
				if (xhttp.responseText.trim() != "null")
					fillIntervals(xhttp.responseText);
			}
		};
		xhttp.open("POST", "vGetMore2.jsp", true);
		xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
		xhttp.send("rO=" + rO + "&wO=" + wO + "&count=" + getMoreCount + "&appName=" + appName + "&key=" + currentKey + "&index=" + selectedIndex + "&type=" + type);
	} else {
		alert("No more logs to get.");
		document.getElementById("getMore").disabled = false;
		var pic = document.getElementById("loadingPic");
		if (pic!=null){
		pic.parentNode.removeChild(pic);
		pic = document.getElementById("layerPic");
		pic.parentNode.removeChild(pic);
		}
	}
}

function weCanGetMore() {
	var b = false;
	if (rO - (getMoreCount * numOfLogsBeforeLast) >= 0) {
		b = true;
	}
	if (wO - (getMoreCount * numOfLogsBeforeLast) >= 0) {
		b = true;
	}
	return b;
}
function getMoreLogs(first,tcount){
//	alert("get More is called");
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			if (xhttp.responseText.trim() != "null")
				fillIntervals(xhttp.responseText,first);
		}
	};
	xhttp.open("POST", "vGetMore2.jsp", true);
	xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	xhttp.send("first="+first +"&tcount="+tcount+"");
} 
function fillIntervals(json,first) {
	//items = new vis.DataSet();
	if (first==true){
		items = new vis.DataSet();
	}
	
	try {
		var parsedJSON = JSON.parse(json);
		if (parsedJSON.items.length > 0) {
			for (var i = 0; i < parsedJSON.items.length; i++) {
				if (!exist(items_data, parsedJSON.items[i])) {

					var p = document.createElement("label");
					p.setAttribute("style", "margin-bottom: 0px; font-size: small;vertical-align: bottom;align-content:left;");
					p.setAttribute("align", "center");
					//var span = document.createElement("span");
					var className = "";
					switch (parsedJSON.items[i].className) {
					case "Stale":
						className = "";
						break;
					case "Read":
						className = "";
						break;
					case "Write":
						className = "";
						break;
					}
				//	span.setAttribute("class", className);
					//p.appendChild(span);
					p.innerHTML=parsedJSON.items[i].id;
					//p.appendChild(document.createTextNode(" " + parsedJSON.items[i].id));
					
					var title1= getTitle(parsedJSON.items[i]);
					
					
					
					itemsArr[itemsArr.length] = {
						id : itemsArr.length,
						content : p,
						title:title1,
						type:'range',
						start : new Date(parsedJSON.items[i].start),
						end : new Date(parsedJSON.items[i].end),
						className : parsedJSON.items[i].className
					};
					items_data[items_data.length] = parsedJSON.items[i];
					var item1= itemsArr[itemsArr.length-1];
					items.add(item1);
					
				}
			}
		//	items = new vis.DataSet(itemsArr);
			if (first==true){
			container = document.getElementById('visualization');
			if (container.children.length != 0)
				container.removeChild(container.lastChild);
			timeline = new vis.Timeline(container, items, options);
			
			
			timeline.on('itemover', function(properties) {
				//alert('over');
				logEvent(properties.item);
			});
//			timeline.on('select', function(properties) {
//				//alert('over');
//				logEvent(properties.items[0]);
//			});
			timeline.on('itemout', function(properties) {
				//alert('over');
				logEventItemout(properties.item);
			});
			}
		
		}
//		} else {
//			getMore(false, false);
//		}
	} catch (e) {
		alert("ERROR in fillIntervals.\n" + e.message);
	}
}
function numberWithCommas(x) {
	var n= parseFloat(x);
	
	if (n===0){
		return n;
	}
	if (n<1){
		return n.toFixed(1);
	}
    return x.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}
function getTitle(data){
	var observedInt;
	var expected="";
	 for (var i = 0; i < data.entities.length; i++) {
		 for (var j = 0; j < data.entities[i].properties.length; j++) {
			 var t=data.entities[i].properties[j].type;
			 if (t ==='R'){
				 var observed=data.entities[i].properties[j].value;
				 observedInt=parseInt(observed);
				// observedInt=numberWithCommas(observedInt);
				 var e=data.entities[i].properties[j].expected;
				 e= e.substring(1,e.length-1);
				 e=e.split(",");
				 for (var k=0; k<e.length;k++){
					// e[k]=numberWithCommas(e[k]);
					 if (k==e.length-1){
						 expected=expected+' $'+e[k].trim()+"";
					 }
					 else if (k==e.length-2){
						 expected=expected+' $'+e[k].trim()+", or ";
					 }
					 else if (k==0){
						 expected=expected+ '$'+e[k].trim()+",";
					 }
					 else{
					 expected=expected+ ' $'+e[k].trim()+",";
					 }
				 }
				 
				 
			 }
		 }
		 }
	
	 if (data.className === "Stale"){
	return '<table class="stats" style="font-size:14px;"> <tr> <td>Acct Balance</td> <td></td> </tr>\
	 <tr><td style="padding-left:10px;">Observed:</td><td style="color:red;"> $'+observedInt+'</td> </tr>  \
	 <tr><td style="padding-left:10px;">Expected:</td><td> '+expected+'</td> </tr>  \
	 <tr><td>Xact amount:</td><td>-100</td> </tr>  \
	 </table>';
	 }
	 else{
		 return '<table class="stats" style="font-size:14px;"> <tr> <td>Acct Balance</td> <td></td> </tr>\
		 <tr><td style="padding-left:10px;">Observed:</td><td> $'+observedInt+'</td> </tr>  \
		  \
		 <tr><td>Xact amount:</td><td>-100</td> </tr>  \
		 </table>';
		 
	 }
}
function exist(items_data, item) {
	for (var i = 0; i < items_data.length; i++) {
		if (item.id === items_data[i].id)
			return true;
	}
	return false;
}

function logEventItemout(id) {
	timeline.removeCustomTime(id+'-t1');
	timeline.removeCustomTime(id+'-t2');
	
}

function logEvent(id) {// expected
//	alert("Fired:"+id);
	var index = id;
	timeline.addCustomTime(items_data[index].start,id+'-t1');
	 var observedInt;
	 timeline.setCustomTimeTitle(function(time){
	      return moment(items_data[index].start).format( 'HH:MM:ss.SS');
	  }, id+"-t1");
	timeline.addCustomTime(items_data[index].end,id+'-t2');
	 timeline.setCustomTimeTitle(function(time){
	      return moment(items_data[index].end).format( 'HH:MM:ss.SS');
	  }, id+"-t2");
	 
	

	
//	var mainTR = document.getElementById("logsInfoTableTr");
//	var mainTD = document.createElement("td");
//	var table = document.createElement("table");
//	table.setAttribute("id", "item_" + index);
//	table.setAttribute("class", items_data[index].className + " logTable");
//	var tr = document.createElement("tr");
//	var th = document.createElement("th");
//	var span = document.createElement("span");
//	if (items_data[index].className === "Stale") {
//		span.setAttribute("class", "glyphicon glyphicon-alert");
//		th.setAttribute("colspan", "4");
//	} else if (items_data[index].className === "Read") {
//		span.setAttribute("class", "glyphicon glyphicon-check");
//		th.setAttribute("colspan", "2");
//	} else if (items_data[index].className === "Write") {
//		span.setAttribute("class", "glyphicon glyphicon-edit");	
//		th.setAttribute("colspan", "2");	
//	}
//	th.setAttribute("class", "logHeader");
//	th.appendChild(span);
//	th.appendChild(document.createTextNode(" " + items_data[index].id));
//	tr.appendChild(th);
//	th = document.createElement("th");
//	th.setAttribute("class", "logHeader");
//	th.setAttribute("width", "31px");
//	var DeleteElement = createButton("deleteTable_" + index, "\u2716", "delete", null, "deleteItemTable(" + index + ")");
//	th.appendChild(DeleteElement);
//	tr.appendChild(th);
//	table.appendChild(tr);
//	for (var i = 0; i < items_data[index].entities.length; i++) {
//		tr = document.createElement("tr");
//		var td = document.createElement("td");
//		td.setAttribute("colspan", "3");
//		td.setAttribute("class", "eName");
//		td.appendChild(document.createTextNode(items_data[index].entities[i].name));
//		tr.appendChild(td);
//		if (items_data[index].className === "Stale") {
//			td = document.createElement("td");
//			td.setAttribute("colspan", "2");
//			td.setAttribute("class", "expected");
//			td.appendChild(document.createTextNode("Expected"));
//			tr.appendChild(td);
//		}
//		table.appendChild(tr);
//		for (var j = 0; j < items_data[index].entities[i].properties.length; j++) {
//			tr = document.createElement("tr");
//			td = document.createElement("td");
//			td.setAttribute("class", "pName");
//			td.appendChild(document.createTextNode(items_data[index].entities[i].properties[j].name));
//			tr.appendChild(td);
//			td = document.createElement("td");
//			td.setAttribute("class", "props");
//			td.appendChild(document.createTextNode(items_data[index].entities[i].properties[j].value));
//			tr.appendChild(td);
//			td = document.createElement("td");
//			td.setAttribute("class", "props");
//			td.appendChild(document.createTextNode(items_data[index].entities[i].properties[j].type));
//			tr.appendChild(td);
//			if (items_data[index].className === "Stale") {
//				td = document.createElement("td");
//				td.setAttribute("colspan", "2");
//				td.setAttribute("class", "props");
//				td.appendChild(document.createTextNode(items_data[index].entities[i].properties[j].expected));
//				tr.appendChild(td);
//			}
//			table.appendChild(tr);
//		}
//		//div.appendChild(document.createElement("br"));
//		mainTD.appendChild(table);
//		//mainTR.appendChild(mainTD);
//		mainTR.insertBefore(mainTD, mainTR.firstChild);
//	}
}

function deleteItemTable(index) {
	var table = document.getElementById("item_" + index);
	var td = table.parentNode;
	td.parentNode.removeChild(td);
}

function selectedNewIndex(rOInput, wOInput, appNameInput, pix, restart, key, index, typeInput) {
	if (selectedIndex != -1) {
		var td = document.getElementById("td_" + selectedIndex);
		td.setAttribute("class", "");
	}
	rO = rOInput;
	wO = wOInput;
	selectedIndex = index;
	appName = appNameInput;
	currentKey = key;
	type = typeInput;
	var td = document.getElementById("td_" + index);
	td.setAttribute("class", "selected");
	getMore(pix, restart);
}