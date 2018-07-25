var PROPERTY_PK = "PropertyPK_";
var PROPERTY = "Property_";
var PROPERTY_VARIABLE_PK = "PropertyVariablePK_";
var PROPERTY_VARIABLE = "PropertyVariable_";
var TRANSACTION_ENTITY = "TE_";
var TRANSACTION_ENTITY_TYPE = "TransactionEntityType_";
var TRANSACTION_ENTITY_PROPERTY_TYPE = "TransactionEntityTypeProperty_";
var PROPERTY_TR = "PropertyTR_";

var TRANSACTION_ENTITY_TYPE_READ = "READ";
var TRANSACTION_ENTITY_TYPE_INSERT = "INSERT";
var TRANSACTION_ENTITY_TYPE_UPDATE = "UPDATE";
var TRANSACTION_ENTITY_TYPE_DELETE = "DELETE";
var TRANSACTION_ENTITY_TYPE_RW = "READ&WRITE";

var TRANSACTION_ENTITY_PROPERTY_TYPE_READ = "READ";
var TRANSACTION_ENTITY_PROPERTY_TYPE_NVU = "NEW VALUE";
var TRANSACTION_ENTITY_PROPERTY_TYPE_INCREMENT = "INCREMENT";
var TRANSACTION_ENTITY_PROPERTY_TYPE_DECREMENT = "DECREMENT";

var PROPERTY_TYPE_READ = "R";
var PROPERTY_TYPE_NVU = "N";
var PROPERTY_TYPE_INCREMENT = "I";
var PROPERTY_TYPE_DECREMENT = "Q";

// =========================================================================================================
// ================ Transaction Page =======================================================================
// =========================================================================================================
var SelectedEntitis = [];

function addEntity(index, ElementType){
	var id = SelectedEntitis.length;
	SelectedEntitis[id] = {
			eid: index,
			elementType: ElementType,
			sets: []
	};
	createEntityTransactionTable(id);
}

function removeEntity(index, ElementType){
	for(var i = 0; i < SelectedEntitis.length; i++){
		if(SelectedEntitis[i].eid === index && SelectedEntitis[i].elementType === ElementType){
			deleteTransactionEntity(i);
			break;
		}
	}	
}

function cleanTransactions(){
	var div = document.getElementById("er_sets");
	while(div.firstChild){
		div.removeChild(div.firstChild);
	}
}

function deleteTransactionEntity(index){
	switch(SelectedEntitis[index].elementType){
	case "Entity":
		Entities[SelectedEntitis[index].eid].fill = false;
		break;
	case "Relationship":
		Relationships[SelectedEntitis[index].eid].fill = false;
		break;
	}
	updateDraw();
    var table = document.getElementById("ER_" + index);
    table.parentNode.removeChild(table);
    for(var id = index; id < SelectedEntitis.length-1; id++){
    	
    	SelectedEntitis[id] = SelectedEntitis[id+1];
    	
        var table = document.getElementById("ER_" + (id+1));
        table.setAttribute("id", "ER_" + id);
    	
        var showHide = document.getElementById("showHide_" + (id+1));
        showHide.setAttribute("id", "showHide_" + id);
        showHide.setAttribute("onclick", "showHide("+TRANSACTION_ENTITY+id+", this)");
    	
        var deleteTable = document.getElementById("deleteTable_" + (id+1));
        deleteTable.setAttribute("id", "deleteTable_" + id);
        deleteTable.setAttribute("onclick", "deleteTransactionEntity("+id+")");
    	
        var deleteSet = document.getElementById("deleteSet_" + (id+1));
        deleteSet.setAttribute("id", "deleteSet_" + id);
        deleteSet.setAttribute("onclick", "decrementNumOfSets("+id+")");
    	
        var addSet = document.getElementById("addSet_" + (id+1));
        addSet.setAttribute("id", "addSet_" + id);
        addSet.setAttribute("onclick", "incrementNumOfSets("+id+")");
    	
        var lengthDiv = document.getElementById("ER_LENGTH_" + (id+1));
        lengthDiv.setAttribute("id", "ER_LENGTH_" + id);
    	
        var div = document.getElementById(TRANSACTION_ENTITY + (id+1));
        div.setAttribute("id", TRANSACTION_ENTITY + id);
        
        for(var setId = 0; setId < SelectedEntitis[id].sets.length; setId++){
        	var setTable = document.getElementById(TRANSACTION_ENTITY + (id+1) + "_" + setId);
        	setTable.setAttribute("id", TRANSACTION_ENTITY + id + "_" + setId);
            
    		var eType = document.getElementById(TRANSACTION_ENTITY_TYPE + (id+1) + "_" + setId);
    		eType.setAttribute("id", TRANSACTION_ENTITY_TYPE + id + "_" + setId);
    		eType.setAttribute("onchange", "changeTransactionEntityType("+id+","+setId+")");
        	
        	for(var i_pks = 0; i_pks < SelectedEntitis[id].sets[setId].pks.length; i_pks++){
        		var pName = document.getElementById(PROPERTY_PK+(id+1)+"_"+setId+"_"+ i_pks);
        		pName.setAttribute("id", PROPERTY_PK+id+"_"+setId+"_"+i_pks);
        		
        		var pVar = document.getElementById(PROPERTY_VARIABLE_PK+(id+1)+"_"+setId+"_"+ i_pks);
	    		pVar.setAttribute("id", PROPERTY_VARIABLE_PK+id+"_"+setId+"_"+i_pks);
	    		pVar.setAttribute("onblur", "updatePKVariable(this, "+id+", "+setId+", "+i_pks+")");
        	}
        	
    		var bAddP = document.getElementById("AddP_"+(id+1)+"_"+setId);
    		if(bAddP != null){
    			bAddP.setAttribute("id", "AddP_"+id+"_"+setId);
    			bAddP.setAttribute("onclick", "AddTransactionEntityProperty(this, "+id+", "+setId+")");
    		}
    		
        	for(var i = 0; i < SelectedEntitis[id].sets[setId].properties.length; i++){
        		var pTR = document.getElementById(PROPERTY_TR + (id+1) + "_" + setId + "_" + i);
	    		pTR.setAttribute("id", PROPERTY_TR + id + "_" + setId + "_" + i);
	
	    		var pName = document.getElementById(PROPERTY+(id+1)+"_"+setId+"_"+ i);
	    		pName.setAttribute("id", PROPERTY+id+"_"+setId+"_"+i);
	    		pName.setAttribute("onchange", "changeTransactionEntityPropertyName(this,"+id+","+setId+","+i+")");
	
	    		var pVar = document.getElementById(PROPERTY_VARIABLE+(id+1)+"_"+setId+"_"+ i);
	    		pVar.setAttribute("id", PROPERTY_VARIABLE+id+"_"+setId+"_"+i);
	    		pVar.setAttribute("onblur", "updatePropVariable(this, "+id+", "+setId+", "+i+")");
	
	    		var pType = document.getElementById(TRANSACTION_ENTITY_PROPERTY_TYPE + (id+1) + "_" + setId + "_" + i);
	    		pType.setAttribute("id", TRANSACTION_ENTITY_PROPERTY_TYPE + id + "_" + setId + "_" + i);
	    		pType.setAttribute("onchange", "changePropertyType(this, "+id+","+setId+","+i+")");
	
	    		var pDel = document.getElementById(PROPERTY_TR + "_del_" + (id+1) + "_" + setId + "_" + i);
	    		pDel.setAttribute("id", PROPERTY_TR + "_del_" + id + "_" + setId + "_" + i);
	    		pDel.setAttribute("onclick", "deleteTransactionEntityProperty("+id+","+setId+","+i+")");
        	}
        }
    }
    SelectedEntitis.length--;	
}

function createEntityTransactionTable(id){
	var div = document.getElementById("er_sets");
	var table = document.createElement("table");
	table.setAttribute("id", "ER_"+id);
	table.setAttribute("style", "width: 95%; box-shadow: 5px 5px 10px gray; margin: auto; margin-bottom: 8px; border: black solid 1px; background-color: seashell;");

	table.appendChild(createEntityTransactionTableHeader(id));
	table.appendChild(createEntityTransactionTableBody(id));
	div.appendChild(table);
}

function createEntityTransactionTableHeader(id){
	var tr = document.createElement("TR");
	
	var td_ShowHide = document.createElement("TD");
	td_ShowHide.setAttribute("style", "width: 35px;");
	var ButtonElement = createButton("showHide_"+id, "−", "showHide", null, "showHide("+TRANSACTION_ENTITY+id+", this)"); 
	td_ShowHide.appendChild(ButtonElement);	
	
	var td_Name = document.createElement("TD");
	td_Name.setAttribute("style", "text-align: center;");
	switch(SelectedEntitis[id].elementType){
	case "Entity":
		td_Name.appendChild(document.createTextNode(Entities[SelectedEntitis[id].eid].name));
		break;
	case "Relationship":
		td_Name.appendChild(document.createTextNode(Relationships[SelectedEntitis[id].eid].name));
		break;
	}

	var td_Delete = document.createElement("TD");
	td_Delete.setAttribute("style", "width: 35px;");
	var DeleteElement = createButton("deleteTable_"+id, "\u2716", "delete", null, "deleteTransactionEntity("+id+")");
	td_Delete.appendChild(DeleteElement);
	
	tr.appendChild(td_ShowHide);
	tr.appendChild(td_Name);
	tr.appendChild(td_Delete);
	
	return tr;
}

function createEntityTransactionTableNumOfSets(id){
	var table = document.createElement("table");
	table.setAttribute("style", "margin: auto;");
	var tr = document.createElement("TR");
	
	var td = document.createElement("TD");
	td.setAttribute("style", "text-align: center;");
	
	var table_inc_dec = document.createElement("table");
	table_inc_dec.setAttribute("style", "margin: auto;box-shadow: 5px 5px 10px gray; margin-bottom: 2px; border: black solid 1px;");
	var tr_inc_dec = document.createElement("TR");
	table_inc_dec.appendChild(tr_inc_dec);
	
	var td_inc_dec = document.createElement("TD");
	var DecButton = createButton("deleteSet_"+id, "−", "delete", "padding: 2px 10px 1px 10px;", "decrementNumOfSets("+id+")");
	td_inc_dec.appendChild(DecButton);
	tr_inc_dec.appendChild(td_inc_dec);

	td_inc_dec = document.createElement("TD");
	td_inc_dec.setAttribute("style", "width: 100px;");
	var div_text = document.createElement("div");
	div_text.setAttribute("id", "ER_LENGTH_"+id);
	div_text.appendChild(document.createTextNode(SelectedEntitis[id].sets.length));
	td_inc_dec.appendChild(div_text);
	tr_inc_dec.appendChild(td_inc_dec);
	
	td_inc_dec = document.createElement("TD");
	var IncButton = createButton("addSet_"+id, "+", "add", "padding: 2px 10px 1px 10px;", "incrementNumOfSets("+id+")");
	td_inc_dec.appendChild(IncButton);
	tr_inc_dec.appendChild(td_inc_dec);
	
	td.appendChild(table_inc_dec);
	tr.appendChild(td);
	table.appendChild(tr);
	
	return table;
}

function createEntityTransactionTableBody(id){
	var tr = document.createElement("TR");
	
	var td = document.createElement("TD");
	td.setAttribute("style", "text-align: center;");
	td.setAttribute("colspan", "3");
	
	var div = document.createElement("div");
	div.setAttribute("id", TRANSACTION_ENTITY+id);
	div.setAttribute("syle", "display: block;");
	div.appendChild(createEntityTransactionTableNumOfSets(id));
	td.appendChild(div);
	
	tr.appendChild(td);
	
	return tr;	
}

function incrementNumOfSets(id){
	var setId = SelectedEntitis[id].sets.length;
	SelectedEntitis[id].sets[setId] = {
			type: TRANSACTION_ENTITY_TYPE_READ,
			pks: [],
			properties: [],
			single: true,
			stop: ""
	};
	var div = document.getElementById(TRANSACTION_ENTITY+id);
	div.appendChild(newERSetTable(id, setId, false));
	var div_text = document.getElementById("ER_LENGTH_"+id);
	div_text.innerHTML = SelectedEntitis[id].sets.length;
}

function decrementNumOfSets(id){
	SelectedEntitis[id].sets.length--;
	var setId = SelectedEntitis[id].sets.length;
	var table = document.getElementById(TRANSACTION_ENTITY+id+"_"+setId);
	table.parentNode.removeChild(table);
	var div_text = document.getElementById("ER_LENGTH_"+id);
	div_text.innerHTML = SelectedEntitis[id].sets.length;
}

function newERSetTable(id, setId, Loading){
	var table = document.createElement("table");
	table.setAttribute("id", TRANSACTION_ENTITY+id+"_"+setId);
	table.setAttribute("style", "width: 95%; box-shadow: 5px 5px 10px gray; margin: auto; margin-bottom: 8px; border: black solid 1px; background-color: #bff3ff;");
	table.appendChild(createEntityTransactionTableTypeRow(id, setId));
	table.appendChild(createEntityTransactionTableSingleCollectionRow(id, setId, Loading));
	table.appendChild(createEntityTransactionTablePKRow(id, setId, Loading));
	 switch(SelectedEntitis[id].elementType){
		 case "Entity":
			 if(Entities[SelectedEntitis[id].eid].properties.length > 0)
				 table.appendChild(createEntityTransactionTableAddPropertyButtonRow(id, setId));
			 break;
		 case "Relationship":
			 if(Relationships[SelectedEntitis[id].eid].properties.length > 0)
				 table.appendChild(createEntityTransactionTableAddPropertyButtonRow(id, setId));
			 break;
	 }
	return table;
}

function createEntityTransactionTableTypeRow(id, setId){
	var tr = document.createElement("TR");
	
	var td = document.createElement("TD");
	td.setAttribute("style", "text-align: center;");
	td.setAttribute("colspan", "3");
	td.appendChild(document.createTextNode("Type"));
	var TranEntitType = createTransactionEntityType(id, setId);
	td.appendChild(TranEntitType);
	tr.appendChild(td);
	
	return tr;
}

function createTransactionEntityType(id, setId){
	var arr = [TRANSACTION_ENTITY_TYPE_READ, TRANSACTION_ENTITY_TYPE_INSERT, TRANSACTION_ENTITY_TYPE_UPDATE, TRANSACTION_ENTITY_TYPE_RW, TRANSACTION_ENTITY_TYPE_DELETE];
	return createSelect(TRANSACTION_ENTITY_TYPE+id+"_"+setId, arr, arr, null, null, "changeTransactionEntityType("+id+", "+setId+")", SelectedEntitis[id].sets[setId].type);
}

function changeTransactionEntityType(id, setId){
	var select = document.getElementById(TRANSACTION_ENTITY_TYPE+id+"_"+setId);
	var oldType = SelectedEntitis[id].sets[setId].type;
	SelectedEntitis[id].sets[setId].type = select.value;
	deleteAllProperties(id, setId);
	var table = document.getElementById("TE_"+id+"_"+setId);
	if(select.value === TRANSACTION_ENTITY_TYPE_DELETE && oldType !== TRANSACTION_ENTITY_TYPE_DELETE){
		//remove "add" bytton
		table.removeChild(table.lastChild);
	} else if (oldType === TRANSACTION_ENTITY_TYPE_DELETE && select.value !== TRANSACTION_ENTITY_TYPE_DELETE){
		//TODO: add "add" button
		table.appendChild(createEntityTransactionTableAddPropertyButtonRow(id, setId));
	}
}

function createEntityTransactionTableSingleCollectionRow(id, setId, Loading){
	var tr = document.createElement("TR");
	var td = document.createElement("TD");
	var rb_single = createInput("single"+id+"_"+setId, "single"+id+"_"+setId, "radio", null, null, [ "onchange" ], [ "updateSingle(this.parentNode, "+id+", "+setId+", true)" ], true);
	td.appendChild(rb_single);
	td.appendChild(document.createTextNode("Single"));
	var rb_collection = createInput("collection"+id+"_"+setId, "single"+id+"_"+setId, "radio", null, null, [ "onchange" ], [ "updateSingle(this.parentNode, "+id+", "+setId+", false)" ], false);
	td.appendChild(rb_collection);
	td.appendChild(document.createTextNode("Collection"));
	if(! Loading){
		rb_single.checked = true;
	} else {
		if(SelectedEntitis[id].sets[setId].single){
			rb_single.checked = true;
		} else {
			rb_collection.checked = true;
			td.appendChild(createCollectionDiv(id, setId, SelectedEntitis[id].sets[setId].stop));
		}
	}
	tr.appendChild(td);	
	return tr;
}

function updateSingle(td , id, setId, bool){
	SelectedEntitis[id].sets[setId].single = bool;
	if(bool){
		var div = document.getElementById("collectionDiv"+id+"_"+setId);
		td.removeChild(div);
	} else {
		td.appendChild(createCollectionDiv(id, setId, SelectedEntitis[id].sets[setId].stop));
	}
}

function createCollectionDiv(id, setId, stopCon){
	var div = document.createElement("div");
	var bold = document.createElement("b");
	bold.innerHTML = ";";
	div.setAttribute("id", "collectionDiv"+id+"_"+setId);
	div.appendChild(document.createTextNode("Use "));
	div.appendChild(bold);
	div.appendChild(document.createTextNode(" to indecate the iterator variable"));
	div.appendChild(document.createElement("br"));
	div.appendChild(document.createTextNode("Stop condition"));
	div.appendChild(createInput("StopCondition"+id+"_"+setId, null, "text", null, "width: 120px;", ["onblur"], ["updateStopCondition(this, "+id+", "+setId+")"], stopCon));
	return div;
}

function updateStopCondition(textBox, id, setId){
	SelectedEntitis[id].sets[setId].stop = textBox.value;
}

function createEntityTransactionTablePKRow(id, setId, Loading){
	var tr = document.createElement("TR");
	var td = document.createElement("TD");
	td.appendChild(document.createTextNode("Primary Key (Required)"));
	td.appendChild(document.createElement("br"));
	var element = null;
	switch (SelectedEntitis[id].elementType){
	case "Entity":
		element = Entities[SelectedEntitis[id].eid];
		break;
	case "Relationship":
		element = Relationships[SelectedEntitis[id].eid];
		break;
	}
	var loadPK = 0;
	for(var i = 0; i < element.properties.length; i++){
		if(element.properties[i].pk){
			var pkId = SelectedEntitis[id].sets[setId].pks.length;
			if(! Loading){
				SelectedEntitis[id].sets[setId].pks[pkId] = {
						variable: undefined,
						pid: i
				};
			} else {
				pkId = loadPK;
				loadPK++;
			}
			var TextBoxPName = createInput(PROPERTY_PK+id+"_"+setId+"_"+pkId, null, "text", null, "width: 80px;", [], [], element.properties[i].name);
			TextBoxPName.readOnly = true;
			td.appendChild(TextBoxPName);
			
			var TextBoxElement = createInput(PROPERTY_VARIABLE_PK+id+"_"+setId+"_"+pkId, null, "text", null, "color:#888; width: 120px", ["onfocus", "onblur"], ["inputFocus(this)", "updatePKVariable(this, "+id+", "+setId+", "+pkId+")"], "Variable Name");
			td.appendChild(TextBoxElement);
			td.appendChild(document.createElement("br"));		
		}
	}
	tr.appendChild(td);
	return tr;
}

function updatePKVariable(tb, id, setId, pkId){
	SelectedEntitis[id].sets[setId].pks[pkId].variable = tb.value;
	inputBlur(tb);
}

function createEntityTransactionTableAddPropertyButtonRow(id, setId){
	var tr = document.createElement("TR");
	var td = document.createElement("TD");
	var button = createButton("AddP_"+id+"_"+setId, " + │ Add Property", "add", null, "AddTransactionEntityProperty(this, "+id+", "+setId+")");
	td.appendChild(button);
	tr.appendChild(td);
	return tr;
}

function AddTransactionEntityProperty(addButton, id, setId){
	var pId = SelectedEntitis[id].sets[setId].properties.length;
	var length = SelectedEntitis[id].sets[setId].pks.length;
	var totalP = length + pId;
	if(SelectedEntitis[id].sets[setId].type !== TRANSACTION_ENTITY_TYPE_RW){
		switch(SelectedEntitis[id].elementType){
			case "Entity": if(totalP >= Entities[SelectedEntitis[id].eid].properties.length){
				alert("No more Properties.");
				return;
			}
			break;
			case "Relationship": if(totalP >= Relationships[SelectedEntitis[id].eid].properties.length){
				alert("No more Properties.");
				return;
			}
			break;
		}		
	}
	SelectedEntitis[id].sets[setId].properties[pId] = {
			variable: undefined,
			type: "",
			pid: -1
	};
	var table = addButton.parentNode.parentNode.parentNode;	
	table.appendChild(createTransactionEntityPropertiesTR(id, setId, pId, false));		
}

function createTransactionEntityPropertiesTR(id, setId, pId, loading){
	var tr = document.createElement("TR");
	tr.setAttribute("id", PROPERTY_TR + id + "_" + setId + "_" + pId);
	var td = document.createElement("TD");
	var element = null;
	switch (SelectedEntitis[id].elementType){
	case "Entity":
		element = Entities[SelectedEntitis[id].eid];
		break;
	case "Relationship":
		element = Relationships[SelectedEntitis[id].eid];
		break;
	}
	var names = [];
	var ids = [];
	for(var i = 0; i < element.properties.length; i++){
		if(!element.properties[i].pk){
			names[names.length] = element.properties[i].name;
			ids[ids.length] = i;		
		}
	}
	if(! loading){
		SelectedEntitis[id].sets[setId].properties[pId].pid = ids[0];
		SelectedEntitis[id].sets[setId].properties[pId].type = "";
	}
	
	var table = document.createElement("table");
	table.setAttribute("style", "border-collapse: collapse;");
	var innerTTR = document.createElement("tr");
	var innerTTD = document.createElement("td");
	innerTTD.appendChild(createSelect(PROPERTY+id+"_"+setId+"_"+pId, names, ids, null, "margin: 0px;", "changeTransactionEntityPropertyName(this,"+id+","+setId+","+pId+")", parseInt(SelectedEntitis[id].sets[setId].properties[pId].pid)));
	innerTTR.appendChild(innerTTD);
	
	innerTTD = document.createElement("td");
	innerTTD.appendChild(createInput(PROPERTY_VARIABLE+id+"_"+setId+"_"+pId, null, "text", null, "color:#888; width: 100px; margin: 0px;", ["onfocus", "onblur"], ["inputFocus(this)", "updatePropVariable(this, "+id+", "+setId+", "+pId+")"], "Variable Name"));
	innerTTR.appendChild(innerTTD);

	innerTTD = document.createElement("td");
	innerTTD.appendChild(PropertyTypeElement(id, setId, pId, false, null));
	innerTTR.appendChild(innerTTD);

	innerTTD = document.createElement("td");
	innerTTD.appendChild(createButton(PROPERTY_TR + "_del_" + id + "_" + setId + "_" + pId, "\u2716", "delete", null, "deleteTransactionEntityProperty("+id+","+setId+","+pId+")"));
	innerTTR.appendChild(innerTTD);

	table.appendChild(innerTTR);
	td.appendChild(table);
	tr.appendChild(td);
	return tr;
}

function PropertyTypeElement(id, setId, pId) {
    switch (SelectedEntitis[id].sets[setId].type) {
    
        case TRANSACTION_ENTITY_TYPE_READ:
        	var names = [TRANSACTION_ENTITY_PROPERTY_TYPE_READ];
        	var values= [PROPERTY_TYPE_READ];
        	if(SelectedEntitis[id].sets[setId].properties[pId].type === "")
        		SelectedEntitis[id].sets[setId].properties[pId].type = PROPERTY_TYPE_READ;
        	return createSelect(TRANSACTION_ENTITY_PROPERTY_TYPE + id + "_" + setId + "_" + pId, names, values, null, "width: 108px;margin: 0px;", null, SelectedEntitis[id].sets[setId].properties[pId].type);
        	
        case TRANSACTION_ENTITY_TYPE_INSERT:
        	var names = [TRANSACTION_ENTITY_PROPERTY_TYPE_NVU];
        	var values= [PROPERTY_TYPE_NVU];
        	if(SelectedEntitis[id].sets[setId].properties[pId].type === "")
        		SelectedEntitis[id].sets[setId].properties[pId].type = PROPERTY_TYPE_NVU;
        	return createSelect(TRANSACTION_ENTITY_PROPERTY_TYPE + id + "_" + setId + "_" + pId, names, values, null, "width: 108px;margin: 0px;", null, SelectedEntitis[id].sets[setId].properties[pId].type);
        	
        case TRANSACTION_ENTITY_TYPE_UPDATE:
            var names = [TRANSACTION_ENTITY_PROPERTY_TYPE_NVU, TRANSACTION_ENTITY_PROPERTY_TYPE_INCREMENT, TRANSACTION_ENTITY_PROPERTY_TYPE_DECREMENT];
            var values = [PROPERTY_TYPE_NVU, PROPERTY_TYPE_INCREMENT, PROPERTY_TYPE_DECREMENT];
        	if(SelectedEntitis[id].sets[setId].properties[pId].type === "")
        		SelectedEntitis[id].sets[setId].properties[pId].type = PROPERTY_TYPE_NVU;
        	return createSelect(TRANSACTION_ENTITY_PROPERTY_TYPE + id + "_" + setId + "_" + pId, names, values, null, "width: 108px;margin: 0px;", "changePropertyType(this, "+id+","+setId+","+pId+")", SelectedEntitis[id].sets[setId].properties[pId].type);

        case TRANSACTION_ENTITY_TYPE_RW:
            var names = [TRANSACTION_ENTITY_PROPERTY_TYPE_READ, TRANSACTION_ENTITY_PROPERTY_TYPE_NVU, TRANSACTION_ENTITY_PROPERTY_TYPE_INCREMENT, TRANSACTION_ENTITY_PROPERTY_TYPE_DECREMENT];
            var values = [PROPERTY_TYPE_READ, PROPERTY_TYPE_NVU, PROPERTY_TYPE_INCREMENT, PROPERTY_TYPE_DECREMENT];
        	if(SelectedEntitis[id].sets[setId].properties[pId].type === "")
        		SelectedEntitis[id].sets[setId].properties[pId].type = PROPERTY_TYPE_READ;
        	return createSelect(TRANSACTION_ENTITY_PROPERTY_TYPE + id + "_" + setId + "_" + pId, names, values, null, "width: 108px;margin: 0px;", "changePropertyType(this, "+id+","+setId+","+pId+")", SelectedEntitis[id].sets[setId].properties[pId].type);
        	
    }
}

function deleteTransactionEntityProperty(id, setId, pId){
    var tr = document.getElementById(PROPERTY_TR + id + "_" + setId + "_" + pId);
    tr.parentNode.removeChild(tr);
    for(var i = pId; i < SelectedEntitis[id].sets[setId].properties.length-1; i++){
    	SelectedEntitis[id].sets[setId].properties[i] = SelectedEntitis[id].sets[setId].properties[i+1];
    	
		var pTR = document.getElementById(PROPERTY_TR + id + "_" + setId + "_" + (i+1));
		pTR.setAttribute("id", PROPERTY_TR + id + "_" + setId + "_" + i);

		var pName = document.getElementById(PROPERTY+id+"_"+setId+"_"+ (i+1));
		pName.setAttribute("id", PROPERTY+id+"_"+setId+"_"+i);
		pName.setAttribute("onchange", "changeTransactionEntityPropertyName(this,"+id+","+setId+","+i+")");

		var pVar = document.getElementById(PROPERTY_VARIABLE+id+"_"+setId+"_"+ (i+1));
		pVar.setAttribute("id", PROPERTY_VARIABLE+id+"_"+setId+"_"+i);
		pVar.setAttribute("onblur", "updatePropVariable(this, "+id+", "+setId+", "+i+")");

		var pType = document.getElementById(TRANSACTION_ENTITY_PROPERTY_TYPE + id + "_" + setId + "_" + (i+1));
		pType.setAttribute("id", TRANSACTION_ENTITY_PROPERTY_TYPE + id + "_" + setId + "_" + i);
		pType.setAttribute("onchange", "changePropertyType(this, "+id+","+setId+","+i+")");

		var pDel = document.getElementById(PROPERTY_TR + "_del_" + id + "_" + setId + "_" + (i+1));
		pDel.setAttribute("id", PROPERTY_TR + "_del_" + id + "_" + setId + "_" + i);
		pDel.setAttribute("onclick", "deleteTransactionEntityProperty("+id+","+setId+","+i+")");
    }
    SelectedEntitis[id].sets[setId].properties.length--;
}

function deleteAllProperties(id, setId){
    while(SelectedEntitis[id].sets[setId].properties.length !== 0){
    	deleteTransactionEntityProperty(id, setId, 0);
    }
}

function changeTransactionEntityPropertyName(select,id,setId,pId){
	SelectedEntitis[id].sets[setId].properties[pId].pid = select.value;
}

function updatePropVariable(tb,id,setId,pId){
	SelectedEntitis[id].sets[setId].properties[pId].variable = tb.value;
}

function changePropertyType(select,id,setId,pId){
	SelectedEntitis[id].sets[setId].properties[pId].type = select.value;
}

//=====================================================================
// ======================= Transaction Store ==========================
//=====================================================================

function loadTrans(tid, textEdit) {
	if(textEdit){
	document.getElementById("buttonResult").innerHTML = "";
	}
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			if(textEdit){
			document.getElementById("buttonResult").innerHTML = "Loaded.";// <br>"+xhttp.responseText;
			fadeout_wait("buttonResult");
			}
			if(xhttp.responseText.trim() != "null")
				fillTrans(xhttp.responseText);
		}
	};
	xhttp.open("POST", "5loadTrans.jsp", true);
	xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	xhttp.send("tid="+tid+"");
}

function saveTrans(tid){
	document.getElementById("buttonResult").innerHTML = "";
	var tName = document.getElementById("transName");
	var j = {
			"Name": tName.value,
			"Elements": SelectedEntitis
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
	xhttp.open("POST", "5updateTrans.jsp", true);
	xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	xhttp.send("tid="+tid+"&name="+tName.value+"&data="+encodeURIComponent(jsonCode)+"");
}

function importTrans(){
	var element = document.createElement('input');
	element.setAttribute('type', 'file');
	element.setAttribute('accept', 'text/plain');
	element.setAttribute('onchange', 'openTrans(event)');

	element.style.display = 'none';
	document.body.appendChild(element);

	element.click();
	document.body.removeChild(element);	
}

function openTrans(event) {
	var input = event.target;
	var reader = new FileReader();
	reader.onload = function() {
		var text = reader.result;
		fillTrans(text);
	};
	reader.readAsText(input.files[0]);
}

function exportTrans(name){
	var tName = document.getElementById("transName");
	var j = {
			"Name": tName.value,
			"Elements": SelectedEntitis
	};
	var jsonCode = JSON.stringify(j);
	var element = document.createElement('a');
	element.setAttribute('href', 'data:text/plain;charset=utf-8,'
			+ encodeURIComponent(jsonCode));
	element.setAttribute('download', name+'_'+tName.value+'.txt');

	element.style.display = 'none';
	document.body.appendChild(element);

	element.click();
	document.body.removeChild(element);
}

function fillTrans(jsonText){
//	jsonText = jsonText.replace(",\"fill\":true", ",\"fill\":false"); 
	try{
		var parsedJSON = JSON.parse(jsonText);
		SelectedEntitis = parsedJSON.Elements;
	} catch (e) {
		return;
	}
	cleanTransactions();
	for(var id = 0; id < SelectedEntitis.length; id++){
		switch(SelectedEntitis[id].elementType){
		case "Entity": Entities[SelectedEntitis[id].eid].fill = true;break;
		case "Relationship": Relationships[SelectedEntitis[id].eid].fill = true;break;
		}
		updateDraw();
		createEntityTransactionTable(id);
		var div_text = document.getElementById("ER_LENGTH_"+id);
		div_text.innerHTML = SelectedEntitis[id].sets.length;
		
		for(var setId = 0; setId < SelectedEntitis[id].sets.length; setId++){

			var div = document.getElementById(TRANSACTION_ENTITY+id);
			div.appendChild(newERSetTable(id, setId, true));
			
			var setType = document.getElementById(TRANSACTION_ENTITY_TYPE + id + "_" + setId);
    		changeSelectedIndex(setType, SelectedEntitis[id].sets[setId].type);

			for(var i = 0; i < SelectedEntitis[id].sets[setId].pks.length; i++){
				var tb = document.getElementById(PROPERTY_VARIABLE_PK+id+"_"+setId+"_"+i);
				tb.value = SelectedEntitis[id].sets[setId].pks[i].variable;
				tb.style.color="#000";
			}

			var table = document.getElementById(TRANSACTION_ENTITY+id+"_"+setId);
			for(var i = 0; i < SelectedEntitis[id].sets[setId].properties.length; i++){
				table.appendChild(createTransactionEntityPropertiesTR(id, setId, i, true));
				
				var tb = document.getElementById(PROPERTY_VARIABLE + id + "_" + setId + "_" + i);
				tb.value = SelectedEntitis[id].sets[setId].properties[i].variable;
				tb.style.color="#000";
			}
		}
	}
}