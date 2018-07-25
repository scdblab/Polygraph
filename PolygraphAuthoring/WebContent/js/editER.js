//===================================
//========= Element =================
//===================================

function newElement(type) {
	newShape = true;
	newShapeType = type;
	movement = false;
	switch (type) {
	case "Entity":
		var tdE = document.getElementById("EntityButtonTD");
		var tdR = document.getElementById("RelationshipButtonTD");
		tdR.innerHTML = "<button onClick=\"newElement('Relationship')\"><img src=\"img/relationship.png\"/></button>";
		tdE.innerHTML = "<div style=\"background-color: #abaeb1; padding: 1px 6px; border: #828282 solid 1.6px; vertical-align: middle; text-align: center;\"><img src=\"img/entity.png\"/></div>";
		break;
	case "Relationship":
		var tdE = document.getElementById("EntityButtonTD");
		var tdR = document.getElementById("RelationshipButtonTD");
		tdE.innerHTML = "<button onClick=\"newElement('Entity')\"><img src=\"img/entity.png\"/></button>";
		tdR.innerHTML = "<div style=\"background-color: #abaeb1; padding: 1px 6px; border: #828282 solid 1.6px; vertical-align: middle; text-align: center;\"><img src=\"img/relationship.png\"/></div>";
		break;
	}
}

function nameUpdated() {
	var eleName = document.getElementById("eleName");
	switch (editElementType) {
	case "Entity":
		Entities[editElementIndex].name = eleName.value;
		break;
	case "Relationship":
		Relationships[editElementIndex].name = eleName.value;
		break;
	}
	updateDraw();
}

function cleanAll() {
	context.clearRect(0, 0, canvas.width, canvas.height);
}

// ==================================
// ========= Entity =================
// ==================================

function entityDetails(obj) {
	var p = document.createElement("p");
	p.setAttribute("style", "margin-bottom: 0px; margin-top: 0px; text-align: left; font-weight: bold; font-family: sans-serif;");
	p.appendChild(document.createTextNode("Name "));
	p.appendChild(createInput("eleName", null, "text", null, "width: 280px;", [ "onkeyup" ], [ "nameUpdated()" ], obj.name));
	p.appendChild(createButton("e_del", "\u2716", "delete", null, "deleteEntity()"));
	var div = document.getElementById("details");
	while (div.firstChild) {
		div.removeChild(div.firstChild);
	}
	div.appendChild(p);
	div.appendChild(document.createElement("br"));
	div.appendChild(createButton(null, " + | Add Property", "add", null, "addProperty()"));
	div.appendChild(document.createElement("br"));
	var pDiv = document.createElement("div");
	pDiv.setAttribute("id", "Properties");
	pDiv.setAttribute("style", "height: 455px; font-size: 12px; overflow: scroll;");
	div.appendChild(pDiv);
	fillProperties(obj);
}

function deleteEntity() {
	document.getElementById("details").innerHTML = "";
	for (var i = editElementIndex; i < Entities.length - 1; i++) {
		Entities[i] = Entities[i + 1];
		// Entities[i].id = i;
	}
	for (var i = 0; i < Relationships.length; i++) {
		if (Relationships[i].source === editElementIndex) {
			Relationships[i].source = -1;
			Relationships[i].sCardinality = "one";
			Relationships[i].sConnector = -1;
		}
		if (Relationships[i].destination === editElementIndex) {
			Relationships[i].destination = -1;
			Relationships[i].dCardinality = "one";
			Relationships[i].dConnector = -1;
		}
		if (Relationships[i].source > editElementIndex) {
			Relationships[i].source--;
		}
		if (Relationships[i].destination > editElementIndex) {
			Relationships[i].destination--;
		}
	}
	for (var i = 0; i < Connectors.length; i++) {
		if (Connectors[i].destination === editElementIndex) {
			Connectors[i].destination = -1;
			Connectors[i].sX = -1;
			Connectors[i].sY = -1;
			Connectors[i].dX = -1;
			Connectors[i].dY = -1;			
		} else if (Connectors[i].destination > editElementIndex) {
			Connectors[i].destination--;
		}
	}
	Entities.length--;
	editElementIndex = -1;
	editElementType = undefined;
	cleanAll();
	updateDraw();
}

// ==================================
// ========= Relationship ===========
// ==================================

function relationshipDetails(obj) {
	var p = document.createElement("p");
	p.setAttribute("style", "margin-bottom: 0px; margin-top: 0px; text-align: left; font-weight: bold; font-family: sans-serif;");
	p.appendChild(document.createTextNode("Name "));
	p.appendChild(createInput("eleName", null, "text", null, "width: 280px;", [ "onkeyup" ], [ "nameUpdated()" ], obj.name));
	p.appendChild(createButton("e_del", "\u2716", "delete", null, "deleteRelationship()"));

	var div = document.getElementById("details");
	while (div.firstChild) {
		div.removeChild(div.firstChild);
	}
	div.appendChild(p);
	div.appendChild(document.createElement("br"));

	for (var i = 1; i <= 2; i++) {
		var rDiv = document.createElement("div");
		rDiv.setAttribute("style", "margin: 5px; background-color: #efefef; padding: 3px;");
		rDiv.appendChild(document.createTextNode("Entity " + i + ": "));
		var names = [ "" ];
		var ids = [ -1 ];
		for (var j = 0; j < Entities.length; j++) {
			ids[ids.length] = j;
			names[names.length] = Entities[j].name;
		}
		if (i == 1) {
			rDiv.appendChild(createSelect("source", names, ids, null, "width: 150px;", "updateRelEntity('source')", obj.source));
			rDiv.appendChild(document.createElement("br"));
			rDiv.appendChild(document.createTextNode("Cardinality: "));
			rDiv.appendChild(createInput("sCardinalityOne", "sCardinality", "radio", null, null, [ "onchange" ], [ "updateCardinality('sCardinality', 'one')" ], "one"));
			rDiv.appendChild(document.createTextNode("One"));
			rDiv.appendChild(createInput("sCardinalityMany", "sCardinality", "radio", null, null, [ "onchange" ], [ "updateCardinality('sCardinality', 'many')" ], "many"));
			rDiv.appendChild(document.createTextNode("Many"));
		} else {
			rDiv.appendChild(createSelect("destination", names, ids, null, "width: 150px;", "updateRelEntity('destination')", obj.destination));
			rDiv.appendChild(document.createElement("br"));
			rDiv.appendChild(document.createTextNode("Cardinality: "));
			rDiv.appendChild(createInput("dCardinalityOne", "dCardinality", "radio", null, null, [ "onchange" ], [ "updateCardinality('dCardinality', 'one')" ], "one"));
			rDiv.appendChild(document.createTextNode("One"));
			rDiv.appendChild(createInput("dCardinalityMany", "dCardinality", "radio", null, null, [ "onchange" ], [ "updateCardinality('dCardinality', 'many')" ], "many"));
			rDiv.appendChild(document.createTextNode("Many"));
		}

		div.appendChild(rDiv);

	}
	switch (obj.sCardinality) {
	case "one":
		var radiobtn = document.getElementById("sCardinalityOne");
		radiobtn.checked = true;
		break;
	case "many":
		var radiobtn = document.getElementById("sCardinalityMany");
		radiobtn.checked = true;
		break;
	}

	switch (obj.dCardinality) {
	case "one":
		var radiobtn = document.getElementById("dCardinalityOne");
		radiobtn.checked = true;
		break;
	case "many":
		var radiobtn = document.getElementById("dCardinalityMany");
		radiobtn.checked = true;
		break;
	}

	div.appendChild(createButton(null, " + | Add Property", "add", null, "addProperty()"));
	div.appendChild(document.createElement("br"));
	var pDiv = document.createElement("div");
	pDiv.setAttribute("id", "Properties");
	pDiv.setAttribute("style", "height: 338px; font-size: 12px; overflow: scroll;");
	div.appendChild(pDiv);
	fillProperties(obj);
}

function updateCardinality(entity, cardinality) {
	switch (entity) {
	case "sCardinality":
		Relationships[editElementIndex].sCardinality = cardinality;
		break;
	case "dCardinality":
		Relationships[editElementIndex].dCardinality = cardinality;
		break;
	}
}

function updateRelEntity(updated) {
	switch (updated) {
	case "source":
		var source = document.getElementById("source");
		Relationships[editElementIndex].source = parseInt(source.value);
		if (Relationships[editElementIndex].sConnector === -1) {
			newConnector("source");
		} else {
			updateConnector("source");
		}
		break;
	case "destination":
		var destination = document.getElementById("destination");
		Relationships[editElementIndex].destination = parseInt(destination.value);
		if (Relationships[editElementIndex].dConnector === -1) {
			newConnector("destination");
		} else {
			updateConnector("destination");
		}
		break;
	}
	updateDraw();
}

function updateConnector(type) {
	switch (type) {
	case "source":
		context.beginPath();
		context.moveTo(Connectors[Relationships[editElementIndex].sConnector].sX, Connectors[Relationships[editElementIndex].sConnector].sY);
		context.lineTo(Connectors[Relationships[editElementIndex].sConnector].dX, Connectors[Relationships[editElementIndex].sConnector].dY);
		context.strokeStyle = "rgb(255,255,255)";
		context.lineWidth = 4;
		context.stroke();
		context.strokeStyle = "rgb(0,0,0)";
		context.lineWidth = 2;

		if (Relationships[editElementIndex].source != -1) {
			var startPoint = getFromPoint(Relationships[editElementIndex], Entities[Relationships[editElementIndex].source]);
			var endPoint = getFromPoint(Entities[Relationships[editElementIndex].source], Relationships[editElementIndex]);
			Connectors[Relationships[editElementIndex].sConnector].destination = Relationships[editElementIndex].source;
			Connectors[Relationships[editElementIndex].sConnector].sX = startPoint.x;
			Connectors[Relationships[editElementIndex].sConnector].sY = startPoint.y;
			Connectors[Relationships[editElementIndex].sConnector].dX = endPoint.x;
			Connectors[Relationships[editElementIndex].sConnector].dY = endPoint.y;
		} else {
			Connectors[Relationships[editElementIndex].sConnector].destination = Relationships[editElementIndex].source;
			Connectors[Relationships[editElementIndex].sConnector].sX = -1;
			Connectors[Relationships[editElementIndex].sConnector].sY = -1;
			Connectors[Relationships[editElementIndex].sConnector].dX = -1;
			Connectors[Relationships[editElementIndex].sConnector].dY = -1;
		}
		break;
	case "destination":
		context.beginPath();
		context.moveTo(Connectors[Relationships[editElementIndex].dConnector].sX, Connectors[Relationships[editElementIndex].dConnector].sY);
		context.lineTo(Connectors[Relationships[editElementIndex].dConnector].dX, Connectors[Relationships[editElementIndex].dConnector].dY);
		context.strokeStyle = "rgb(255,255,255)";
		context.lineWidth = 4;
		context.stroke();
		context.strokeStyle = "rgb(0,0,0)";
		context.lineWidth = 2;

		if (Relationships[editElementIndex].destination != -1) {
			var startPoint = getFromPoint(Relationships[editElementIndex], Entities[Relationships[editElementIndex].destination]);
			var endPoint = getFromPoint(Entities[Relationships[editElementIndex].destination], Relationships[editElementIndex]);
			Connectors[Relationships[editElementIndex].dConnector].destination = Relationships[editElementIndex].destination;
			Connectors[Relationships[editElementIndex].dConnector].sX = startPoint.x;
			Connectors[Relationships[editElementIndex].dConnector].sY = startPoint.y;
			Connectors[Relationships[editElementIndex].dConnector].dX = endPoint.x;
			Connectors[Relationships[editElementIndex].dConnector].dY = endPoint.y;
		} else {
			Connectors[Relationships[editElementIndex].dConnector].destination = Relationships[editElementIndex].destination;
			Connectors[Relationships[editElementIndex].dConnector].sX = -1;
			Connectors[Relationships[editElementIndex].dConnector].sY = -1;
			Connectors[Relationships[editElementIndex].dConnector].dX = -1;
			Connectors[Relationships[editElementIndex].dConnector].dY = -1;
		}
		break;
	}
}

function newConnector(type) {
	var index = Connectors.length;
	switch (type) {
	case "source":
		Relationships[editElementIndex].sConnector = index;
		var startPoint = getFromPoint(Relationships[editElementIndex], Entities[Relationships[editElementIndex].source]);
		var endPoint = getFromPoint(Entities[Relationships[editElementIndex].source], Relationships[editElementIndex]);
		Connectors[index] = {
			"source" : editElementIndex,
			"destination" : Relationships[editElementIndex].source,
			"sX" : startPoint.x,
			"sY" : startPoint.y,
			"dX" : endPoint.x,
			"dY" : endPoint.y,
			"id" : index
		};
		break;
	case "destination":
		Relationships[editElementIndex].dConnector = index;
		var startPoint = getFromPoint(Relationships[editElementIndex], Entities[Relationships[editElementIndex].destination]);
		var endPoint = getFromPoint(Entities[Relationships[editElementIndex].destination], Relationships[editElementIndex]);
		Connectors[index] = {
			"source" : editElementIndex,
			"destination" : Relationships[editElementIndex].destination,
			"sX" : startPoint.x,
			"sY" : startPoint.y,
			"dX" : endPoint.x,
			"dY" : endPoint.y,
			"id" : index
		};
		break;
	}
}

function deleteRelationship() {
	document.getElementById("details").innerHTML = "";
	for (var i = 0; i < Connectors.length; i++) {
		if (Connectors[i].source === editElementIndex) {
			for (var j = i; j < Connectors.length - 1; j++) {
				Connectors[j] = Connectors[j + 1];
				// Connectors[j].id = j;
				if (Relationships[Connectors[j].source].sConnector === j + 1) {
					Relationships[Connectors[j].source].sConnector = j;
				}
				if (Relationships[Connectors[j].source].dConnector === j + 1) {
					Relationships[Connectors[j].source].dConnector = j;
				}
			}
			i--;
			Connectors.length--;
		} else if (Connectors[i].source > editElementIndex) {
			Connectors[i].source--;
		}
	}
	for (var i = editElementIndex; i < Relationships.length - 1; i++) {
		Relationships[i] = Relationships[i + 1];
		// Relationships[i].id = i;
	}
	Relationships.length--;
	editElementIndex = -1;
	editElementType = undefined;
	cleanAll();
	updateDraw();
}

// ==================================
// ========= Property ===============
// ==================================

function addProperty() {
	switch (editElementType) {
	case "Entity":
		var id = Entities[editElementIndex].properties.length;
		Entities[editElementIndex].properties[id] = {
			// id : id,
			name : "",
			type : "String",
			pk : false
		};
		propertyLine(id, Entities[editElementIndex].properties[id]);
		break;
	case "Relationship":
		var id = Relationships[editElementIndex].properties.length;
		Relationships[editElementIndex].properties[id] = {
			// id : id,
			name : "",
			type : "String",
			pk : false
		};
		propertyLine(id, Relationships[editElementIndex].properties[id]);
		break;
	}
}

function fillProperties(obj) {
	for (var i = 0; i < obj.properties.length; i++) {
		propertyLine(i, obj.properties[i]);
	}
}

function propertyLine(index, prop) {
	var div = document.createElement("div");
	div.setAttribute("style", "margin: 5px; background-color: #efefef; padding: 3px;");
	div.setAttribute("id", "p_" + index + "");

	var table = document.createElement("table");
	table.setAttribute("style", "width: 100%;");
	var tr = document.createElement("tr");

	var td_inputName = document.createElement("td");
	var inputName = createInput("p_name_" + index, null, "text", null, "width: 150px;", [ "onkeyup" ], [ "updatePropertyName(" + index + ")" ], prop.name);
	td_inputName.appendChild(inputName);
	tr.appendChild(td_inputName);

	var td_select = document.createElement("td");
	var arrNames = [ "String", "Integer", "Double", "Date" ];
	var select = createSelect("p_type_" + index, arrNames, arrNames, null, null, "updatePropertyType(" + index + ")", prop.type);
	td_select.appendChild(select);
	tr.appendChild(td_select);

	var td_inputCB = document.createElement("td");
	var inputCB = createInput("p_pk_" + index, null, "checkbox", null, null, [ "onclick" ], [ "updatePropertyPK(" + index + ")" ], null);
	inputCB.checked = prop.pk;
	td_inputCB.appendChild(inputCB);
	td_inputCB.appendChild(document.createTextNode("PK"));
	tr.appendChild(td_inputCB);

	var td_deleteButton = document.createElement("td");
	var deleteButton = createButton("p_del_" + index, "\u2716", "delete", "text-align: right;", "deleteProperty(" + index + ")");
	td_deleteButton.appendChild(deleteButton);
	tr.appendChild(td_deleteButton);

	table.appendChild(tr);
	div.appendChild(table);

	var PropertiesDiv = document.getElementById("Properties");
	PropertiesDiv.appendChild(div);
}

function deleteProperty(id) {
	var PropertiesDiv = document.getElementById("Properties");
	var tbdDiv = document.getElementById("p_" + id + "");
	PropertiesDiv.removeChild(tbdDiv);
	var length = -1;
	switch (editElementType) {
	case "Entity":
		length = Entities[editElementIndex].properties.length - 1;
		break;
	case "Relationship":
		length = Relationships[editElementIndex].properties.length - 1;
		break;
	}
	for (var i = id; i < length; i++) {
		switch (editElementType) {
		case "Entity":
			Entities[editElementIndex].properties[i] = Entities[editElementIndex].properties[i + 1];
			break;
		case "Relationship":
			Relationships[editElementIndex].properties[i] = Relationships[editElementIndex].properties[i + 1];
			break;
		}

		var div = document.getElementById("p_" + (i + 1) + "");
		div.setAttribute("id", "p_" + i + "");

		var inputName = document.getElementById("p_name_" + (i + 1) + "");
		inputName.setAttribute("id", "p_name_" + i + "");
		inputName.setAttribute("onkeyup", "updatePropertyName(" + i + ")");

		var select = document.getElementById("p_type_" + (i + 1) + "");
		select.setAttribute("id", "p_type_" + i + "");
		select.setAttribute("onchange", "updatePropertyType(" + i + ")");

		var inputCB = document.getElementById("p_pk_" + (i + 1) + "");
		inputCB.setAttribute("id", "p_pk_" + i + "");
		inputCB.setAttribute("onclick", "updatePropertyPK(" + i + ")");

		var deleteButton = document.getElementById("p_del_" + (i + 1) + "");
		deleteButton.setAttribute("id", "p_del_" + i + "");
		deleteButton.setAttribute("onClick", "deleteProperty(" + i + ")");
	}
	switch (editElementType) {
	case "Entity":
		Entities[editElementIndex].properties.length--;
		break;
	case "Relationship":
		Relationships[editElementIndex].properties.length--;
		break;
	}
}

function updatePropertyName(id) {
	var eleName = document.getElementById("p_name_" + id + "");
	switch (editElementType) {
	case "Entity":
		Entities[editElementIndex].properties[id].name = eleName.value;
		break;
	case "Relationship":
		Relationships[editElementIndex].properties[id].name = eleName.value;
		break;
	}
}

function updatePropertyType(id) {
	var eleName = document.getElementById("p_type_" + id + "");
	switch (editElementType) {
	case "Entity":
		Entities[editElementIndex].properties[id].type = eleName.value;
		break;
	case "Relationship":
		Relationships[editElementIndex].properties[id].type = eleName.value;
		break;
	}
}

function updatePropertyPK(id) {
	var eleName = document.getElementById("p_pk_" + id + "");
	switch (editElementType) {
	case "Entity":
		Entities[editElementIndex].properties[id].pk = eleName.checked;
		break;
	case "Relationship":
		Relationships[editElementIndex].properties[id].pk = eleName.checked;
		break;
	}
}
