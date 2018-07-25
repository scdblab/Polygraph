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
var items;
// Configuration for the Timeline
var options = {
	orientation : {
		axis : 'none'
	},
	height : "400px",
	width : "700px"
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
		pic.parentNode.removeChild(pic);
		pic = document.getElementById("layerPic");
		pic.parentNode.removeChild(pic);
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

function fillIntervals(json) {
	try {
		var parsedJSON = JSON.parse(json);
		if (parsedJSON.items.length > 0) {
			for (var i = 0; i < parsedJSON.items.length; i++) {
				if (!exist(items_data, parsedJSON.items[i])) {
					var p = document.createElement("p");
					p.setAttribute("style", "margin-bottom: 0px; font-size: large;");
					var span = document.createElement("span");
					var className = "";
					switch (parsedJSON.items[i].className) {
					case "Stale":
						className = "glyphicon glyphicon-alert";
						break;
					case "Read":
						className = "glyphicon glyphicon-check";
						break;
					case "Write":
						className = "glyphicon glyphicon-edit";
						break;
					}
					span.setAttribute("class", className);
					p.appendChild(span);
					p.appendChild(document.createTextNode(" " + parsedJSON.items[i].id));
					itemsArr[itemsArr.length] = {
						id : itemsArr.length,
						title:"",
						content : p,
						start : new Date(parsedJSON.items[i].start),
						end : new Date(parsedJSON.items[i].end),
						className : parsedJSON.items[i].className
					};
					items_data[items_data.length] = parsedJSON.items[i];
				}
			}
			items = new vis.DataSet(itemsArr);
			container = document.getElementById('visualization');
			if (container.children.length != 0)
				container.removeChild(container.lastChild);
			timeline = new vis.Timeline(container, items, options);
			timeline.on('select', function(properties) {
				logEvent(properties.items[0]);
			});
			document.getElementById("getMore").disabled = false;
			var pic = document.getElementById("loadingPic");
			pic.parentNode.removeChild(pic);
			pic = document.getElementById("layerPic");
			pic.parentNode.removeChild(pic);
		} else {
			getMore(false, false);
		}
	} catch (e) {
		alert("ERROR in fillIntervals.\n" + e.message);
	}
}

function exist(items_data, item) {
	for (var i = 0; i < items_data.length; i++) {
		if (item.id === items_data[i].id)
			return true;
	}
	return false;
}

function logEvent(id) {// expected
	var index = id;
	var mainTR = document.getElementById("logsInfoTableTr");
	var mainTD = document.createElement("td");
	var table = document.createElement("table");
	table.setAttribute("id", "item_" + index);
	table.setAttribute("class", items_data[index].className + " logTable");
	var tr = document.createElement("tr");
	var th = document.createElement("th");
	var span = document.createElement("span");
	if (items_data[index].className === "Stale") {
		span.setAttribute("class", "glyphicon glyphicon-alert");
		th.setAttribute("colspan", "4");
	} else if (items_data[index].className === "Read") {
		span.setAttribute("class", "glyphicon glyphicon-check");
		th.setAttribute("colspan", "2");
	} else if (items_data[index].className === "Write") {
		span.setAttribute("class", "glyphicon glyphicon-edit");	
		th.setAttribute("colspan", "2");	
	}
	th.setAttribute("class", "logHeader");
	th.appendChild(span);
	th.appendChild(document.createTextNode(" " + items_data[index].id));
	tr.appendChild(th);
	th = document.createElement("th");
	th.setAttribute("class", "logHeader");
	th.setAttribute("width", "31px");
	var DeleteElement = createButton("deleteTable_" + index, "\u2716", "delete", null, "deleteItemTable(" + index + ")");
	th.appendChild(DeleteElement);
	tr.appendChild(th);
	table.appendChild(tr);
	for (var i = 0; i < items_data[index].entities.length; i++) {
		tr = document.createElement("tr");
		var td = document.createElement("td");
		td.setAttribute("colspan", "3");
		td.setAttribute("class", "eName");
		td.appendChild(document.createTextNode(items_data[index].entities[i].name));
		tr.appendChild(td);
		if (items_data[index].className === "Stale") {
			td = document.createElement("td");
			td.setAttribute("colspan", "2");
			td.setAttribute("class", "expected");
			td.appendChild(document.createTextNode("Expected"));
			tr.appendChild(td);
		}
		table.appendChild(tr);
		for (var j = 0; j < items_data[index].entities[i].properties.length; j++) {
			tr = document.createElement("tr");
			td = document.createElement("td");
			td.setAttribute("class", "pName");
			td.appendChild(document.createTextNode(items_data[index].entities[i].properties[j].name));
			tr.appendChild(td);
			td = document.createElement("td");
			td.setAttribute("class", "props");
			td.appendChild(document.createTextNode(items_data[index].entities[i].properties[j].value));
			tr.appendChild(td);
			td = document.createElement("td");
			td.setAttribute("class", "props");
			td.appendChild(document.createTextNode(items_data[index].entities[i].properties[j].type));
			tr.appendChild(td);
			if (items_data[index].className === "Stale") {
				td = document.createElement("td");
				td.setAttribute("colspan", "2");
				td.setAttribute("class", "props");
				td.appendChild(document.createTextNode(items_data[index].entities[i].properties[j].expected));
				tr.appendChild(td);
			}
			table.appendChild(tr);
		}
		//div.appendChild(document.createElement("br"));
		mainTD.appendChild(table);
		//mainTR.appendChild(mainTD);
		mainTR.insertBefore(mainTD, mainTR.firstChild);
	}
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