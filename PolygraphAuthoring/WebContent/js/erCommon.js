var canvas;
var context;
var Entities = [];
var Relationships = [];
var Connectors = [];
var eventType = "";

var drag = false;
var dragObj = undefined;
var dragOffsetLeft = 0;
var dragOffsetTop = 0;
var entityPropertyCount = 0;

var newShape = false;
var newShapeType = undefined;
var movement = false;
var editElementIndex = -1;
var editElementType = undefined;

var Entity_MAX_WIDTH = 100;
var Entity_MAX_HEIGHT = 50;

var Relationship_MAX_WIDTH = 100;
var Relationship_MAX_HEIGHT = 80;

function init(events) {
	eventType = events;
	canvas = document.getElementById("myCanvas");
	context = canvas.getContext("2d");
	context.lineWidth = 2;
	context.font = "10pt Impact, Charcoal, sans-serif";
	var txtHeight = parseInt(context.font);

	if (events === "edit") {
		canvas.addEventListener('mousemove', function(evt) {
			if (drag === true) {
				var mousePos = getMousePos(canvas, evt);
				changeLocation(dragObj, mousePos.x + dragOffsetLeft, mousePos.y + dragOffsetTop, true);
				updateDraw();
			}
		}, false);

		canvas.addEventListener('click', function(evt) {
			if (newShape) {
				var tdE = document.getElementById("EntityButtonTD");
				var tdR = document.getElementById("RelationshipButtonTD");
				tdE.innerHTML = "<button onClick=\"newElement('Entity')\"><img src=\"img/entity.png\"/></button>";
				tdR.innerHTML = "<button onClick=\"newElement('Relationship')\"><img src=\"img/relationship.png\"/></button>";
				var mousePos = getMousePos(canvas, evt);
				switch (newShapeType) {
				case "Entity": {
					var id = Entities.length;
					Entities[id] = {
						type : "Entity",
						// id : id,
						name : "Entity",
						center : {
							x : mousePos.x,
							y : mousePos.y
						},
						properties : [],
						left : mousePos.x - (Entity_MAX_WIDTH / 2),
						top : mousePos.y - (Entity_MAX_HEIGHT / 2),
						width : Entity_MAX_WIDTH,
						height : Entity_MAX_HEIGHT,
						fill : false
					};
					if (editElementIndex !== -1) {
						switch (editElementType) {
						case "Entity":
							Entities[editElementIndex].fill = false;
							break;
						case "Relationship":
							Relationships[editElementIndex].fill = false;
							break;
						}
					}
					dragObj = Entities[id];
					editElementIndex = id;
					editElementType = "Entity";
					dragObj.fill = true;
					entityDetails(Entities[id]);
					break;
				}
				case "Relationship": {
					var id = Relationships.length;
					Relationships[id] = {
						type : "Relationship",
						// id : id,
						name : "Relationship",
						center : {
							x : mousePos.x,
							y : mousePos.y
						},
						properties : [],
						source : -1,
						sCardinality : "one",
						sConnector : -1,
						destination : -1,
						dCardinality : "one",
						dConnector : -1,
						left : mousePos.x - (Relationship_MAX_WIDTH / 2),
						top : mousePos.y - (Relationship_MAX_HEIGHT / 2),
						width : Relationship_MAX_WIDTH,
						height : Relationship_MAX_HEIGHT,
						fill : false
					};
					if (editElementIndex !== -1) {
						switch (editElementType) {
						case "Entity":
							Entities[editElementIndex].fill = false;
							break;
						case "Relationship":
							Relationships[editElementIndex].fill = false;
							break;
						}
					}
					dragObj = Relationships[id];
					editElementIndex = id;
					editElementType = "Relationship";
					relationshipDetails(Relationships[id]);
					dragObj.fill = true;
					break;
				}
				}
				newShape = false;
				newShapeType = undefined;
				updateDraw();
			}
		}, false);

		canvas.onmousedown = function(evt) {
			if (!newShape) {
				var mousePos = getMousePos(canvas, evt);
				for (var i = 0; i < Entities.length; i++) {
					if (isItInRect(Entities[i], {
						x : mousePos.x,
						y : mousePos.y
					})) {
						if (editElementIndex !== -1) {
							switch (editElementType) {
							case "Entity":
								Entities[editElementIndex].fill = false;
								break;
							case "Relationship":
								Relationships[editElementIndex].fill = false;
								break;
							}
						}
						dragObj = Entities[i];
						dragObj.fill = true;
						editElementIndex = i;
						editElementType = "Entity";
						entityDetails(dragObj);
					}
				}

				for (var i = 0; i < Relationships.length; i++) {
					if (isItInDiamond(Relationships[i], {
						x : mousePos.x,
						y : mousePos.y
					})) {
						if (editElementIndex !== -1) {
							switch (editElementType) {
							case "Entity":
								Entities[editElementIndex].fill = false;
								break;
							case "Relationship":
								Relationships[editElementIndex].fill = false;
								break;
							}
						}
						dragObj = Relationships[i];
						dragObj.fill = true;
						editElementIndex = i;
						editElementType = "Relationship";
						relationshipDetails(dragObj);
					}
				}
				updateDraw();

				if (dragObj !== undefined) {
					drag = true;
					dragOffsetLeft = dragObj.left - mousePos.x;
					dragOffsetTop = dragObj.top - mousePos.y;
				}
			}
		};

		canvas.onmouseup = function(evt) {
			drag = false;
			dragObj = undefined;
			dragOffsetLeft = 0;
			dragOffsetTop = 0;
		};
	} else if (events === "click") {
		canvas.addEventListener('click', function(evt) {
			var mousePos = getMousePos(canvas, evt);
			for (var i = 0; i < Entities.length; i++) {
				if (isItInRect(Entities[i], {
					x : mousePos.x,
					y : mousePos.y
				})) {
					Entities[i].fill = !Entities[i].fill;
					if (Entities[i].fill) {
						addEntity(i, "Entity");
					} else {
						removeEntity(i, "Entity");
					}
				}
			}

			for (var i = 0; i < Relationships.length; i++) {
				if (isItInDiamond(Relationships[i], {
					x : mousePos.x,
					y : mousePos.y
				})) {
					Relationships[i].fill = !Relationships[i].fill;
					if (Relationships[i].fill) {
						addEntity(i, "Relationship");
					} else {
						removeEntity(i, "Relationship");
					}
				}
			}
			updateDraw();
		}, false);
	}
}

function cleanAll() {
	context.clearRect(0, 0, canvas.width, canvas.height);
}

function updateDraw() {
	cleanAll();
	for (var i = 0; i < Connectors.length; i++) {
		reDrawConnector(Connectors[i]);
	}
	for (var i = 0; i < Entities.length; i++) {
		reDraw(Entities[i]);
	}
	for (var i = 0; i < Relationships.length; i++) {
		reDraw(Relationships[i]);
	}
}

function reDraw(obj) {
	switch (obj.type) {
	case "Entity":
		context.beginPath();
		context.rect(obj.left, obj.top, Entity_MAX_WIDTH, Entity_MAX_HEIGHT);
		if (obj.fill) {
			context.fillStyle = "rgb(102, 153, 204)";
		} else {
			context.fillStyle = "rgb(255,255,255)";
		}
		context.fill();
		context.fillStyle = "rgb(0,0,0)";
		context.stroke();
		writeName(obj.name, obj.center.x, obj.center.y);
		break;
	case "Attribute":
		break;
	case "Relationship":
		context.beginPath();
		context.moveTo(obj.left + (Relationship_MAX_WIDTH / 2), obj.top);
		context.lineTo(obj.left, obj.top + (Relationship_MAX_HEIGHT / 2));
		context.lineTo(obj.left + (Relationship_MAX_WIDTH / 2), obj.top + (Relationship_MAX_HEIGHT));
		context.lineTo(obj.left + Relationship_MAX_WIDTH, obj.top + (Relationship_MAX_HEIGHT / 2));
		context.lineTo(obj.left + (Relationship_MAX_WIDTH / 2), obj.top);
		if (obj.fill) {
			context.fillStyle = "rgb(102, 153, 204)";
		} else {
			context.fillStyle = "rgb(255,255,255)";
		}
		context.fill();
		context.fillStyle = "rgb(0,0,0)";
		context.stroke();
		writeName(obj.name, obj.center.x, obj.center.y);
		break;
	}
}

function reDrawConnector(connector) {
	if(connector.source == -1 || connector.destination == -1)
		return;
	var source = Relationships[connector.source];
	var destination = Entities[connector.destination];

	context.beginPath();
	context.moveTo(connector.sX, connector.sY);
	context.lineTo(connector.dX, connector.dY);
	context.strokeStyle = "rgb(255,255,255)";
	context.lineWidth = 4;
	context.stroke();

	var startPoint = getFromPoint(source, destination);
	var endPoint = getFromPoint(destination, source);

	connector.sX = startPoint.x;
	connector.sY = startPoint.y;
	connector.dX = endPoint.x;
	connector.dY = endPoint.y;

	context.beginPath();
	context.moveTo(startPoint.x, startPoint.y);
	context.lineTo(endPoint.x, endPoint.y);
	context.strokeStyle = "rgb(0,0,0)";
	context.lineWidth = 2;
	context.stroke();
}

function writeName(name, x, y) {
	var text = name;
	var textMetrics = context.measureText(text); // TextMetrics object
	context.fillText(text, x - (textMetrics.width / 2), y + 5);
}

function getFromPoint(source, destination) {
	var numX = Math.abs(source.center.x - destination.center.x);
	var numY = Math.abs(source.center.y - destination.center.y);
	if (numX > numY) {
		if (source.center.x > destination.center.x) {
			switch (source.type) {
			case "Entity":
				return {
					x : source.left,
					y : source.top + (Entity_MAX_HEIGHT / 2)
				};
			case "Relationship":
				return {
					x : source.left,
					y : source.top + (Relationship_MAX_HEIGHT / 2)
				};
			}
		} else if (source.center.x < destination.center.x) {
			switch (source.type) {
			case "Entity":
				return {
					x : source.left + Entity_MAX_WIDTH,
					y : source.top + (Entity_MAX_HEIGHT / 2)
				};
			case "Relationship":
				return {
					x : source.left + Relationship_MAX_WIDTH,
					y : source.top + (Relationship_MAX_HEIGHT / 2)
				};
			}
		} else {
			switch (source.type) {
			case "Entity":
				return {
					x : source.left,
					y : source.top + (Entity_MAX_HEIGHT / 2)
				};
			case "Relationship":
				return {
					x : source.left,
					y : source.top + (Relationship_MAX_HEIGHT / 2)
				};
			}
		}
	} else {
		if (source.center.y > destination.center.y) {
			switch (source.type) {
			case "Entity":
				return {
					x : source.left + (Entity_MAX_WIDTH / 2),
					y : source.top
				};
			case "Relationship":
				return {
					x : source.left + (Relationship_MAX_WIDTH / 2),
					y : source.top
				};
			}
		} else if (source.center.y < destination.center.y) {
			switch (source.type) {
			case "Entity":
				return {
					x : source.left + (Entity_MAX_WIDTH / 2),
					y : source.top + Entity_MAX_HEIGHT
				};
			case "Relationship":
				return {
					x : source.left + (Relationship_MAX_WIDTH / 2),
					y : source.top + Relationship_MAX_HEIGHT
				};
			}
		} else {
			switch (source.type) {
			case "Entity":
				return {
					x : source.left + (Entity_MAX_WIDTH / 2),
					y : source.top
				};
			case "Relationship":
				return {
					x : source.left + (Relationship_MAX_WIDTH / 2),
					y : source.top
				};
			}
		}
	}
}

function changeLocation(obj, x, y, clear) {

	switch (obj.type) {
	case "Entity":
		if (clear) {
			context.clearRect(dragObj.left - 2, dragObj.top - 2, Entity_MAX_WIDTH + 4, Entity_MAX_HEIGHT + 4);
		}
		obj.center.x = x + (Entity_MAX_WIDTH / 2);
		obj.center.y = y + (Entity_MAX_HEIGHT / 2);
		obj.left = x;
		obj.top = y;
		break;
	case "Relationship":
		if (clear) {
			context.clearRect(dragObj.left - 2, dragObj.top - 2, Relationship_MAX_WIDTH + 4, Relationship_MAX_HEIGHT + 4);
		}
		obj.center.x = x + (Relationship_MAX_WIDTH / 2);
		obj.center.y = y + (Relationship_MAX_HEIGHT / 2);
		obj.left = x;
		obj.top = y;
		break;
	}
}

function isItInRect(rect, point) {
	if (point.x < rect.left || point.x > (rect.left + rect.width)) {
		return false;
	}
	if (point.y < rect.top || point.y > (rect.top + rect.height)) {
		return false;
	}
	return true;
}

function isItInDiamond(diamond, point){
	var dx = Math.abs(point.x - diamond.center.x);
	var dy = Math.abs(point.y - diamond.center.y);
	if (dx / diamond.width + dy / diamond.height <= 0.5)
	  return true;
	else
	  return false;
}

function getMousePos(canvas, evt) {
	var rect = canvas.getBoundingClientRect();
	return {
		x : evt.clientX - rect.left,
		y : evt.clientY - rect.top
	};
}

// =====================================================================================================
// ============================== Saving and Loading
// ===================================================
// =====================================================================================================

function loadER(textEdit) {
	if (textEdit) {
		document.getElementById("buttonResult").innerHTML = "";
	}
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			if (textEdit) {
				document.getElementById("buttonResult").innerHTML = "Loaded.";// <br>"+xhttp.responseText;
				fadeout_wait("buttonResult");
			}
			if (xhttp.responseText.trim() != "null")
				fillER(xhttp.responseText);
		}
	};
	xhttp.open("POST", "3loadER.jsp", true);
	xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	xhttp.send("");
}

function saveER() {
	
	document.getElementById("buttonResult").innerHTML = "";
	var j = {
		"Entities" : Entities,
		"Relationships" : Relationships,
		"Connectors" : Connectors
	};
	var jsonCode = JSON.stringify(j);
	jsonCode = jsonCode.replace(",\"fill\":true", ",\"fill\":false");
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			document.getElementById("buttonResult").innerHTML = "Saved.";
			fadeout_wait("buttonResult");
		}
	};
	xhttp.open("POST", "4updateER.jsp", true);
	xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	xhttp.send("data=" + encodeURIComponent(jsonCode) + "");
}

function importER() {
	var element = document.createElement('input');
	element.setAttribute('type', 'file');
	element.setAttribute('accept', 'text/plain');
	element.setAttribute('onchange', 'openER(event)');

	element.style.display = 'none';
	document.body.appendChild(element);

	element.click();
	document.body.removeChild(element);
}

function openER(event) {
	var input = event.target;
	var reader = new FileReader();
	reader.onload = function() {
		var text = reader.result;
		fillER(text);
	};
	reader.readAsText(input.files[0]);
}

function exportER(name) {
	var j = {
		"Entities" : Entities,
		"Relationships" : Relationships,
		"Connectors" : Connectors
	};
	var jsonCode = JSON.stringify(j);
	jsonCode = jsonCode.replace(",\"fill\":true", ",\"fill\":false");
	var element = document.createElement('a');
	element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(jsonCode));
	element.setAttribute('download', name + '.txt');

	element.style.display = 'none';
	document.body.appendChild(element);

	element.click();
	document.body.removeChild(element);
}

function fillER(jsonText) {
	jsonText = jsonText.replace(",\"fill\":true", ",\"fill\":false");
	var obj = null;
	try {
		obj = JSON.parse(jsonText);
	} catch (e) {
		return;
	}
	Entities = obj.Entities;
	Relationships = obj.Relationships;
	Connectors = obj.Connectors;
	updateDraw();
	if (eventType === "edit") {
		document.getElementById("details").innerHTML = "";
	}
}