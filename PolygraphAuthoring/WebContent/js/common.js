function showHide(currentDiv, button) {
	if (currentDiv.style.display !== 'none') {
		currentDiv.style.display = 'none';
		button.innerHTML = '+';
	} else {
		currentDiv.style.display = 'block';
		button.innerHTML = '−';
	}
}

function createInput(inputId, inputName, inputType, inputClass, inputStyle, events, eventsValues, inputValue) {
	if (events.length !== eventsValues.length)
		return null;
	var input = document.createElement("input");
	if (inputId != null)
		input.setAttribute("id", inputId);
	if (inputName != null)
		input.setAttribute("name", inputName);
	input.setAttribute("type", inputType);
	if (inputStyle != null)
		input.setAttribute("style", inputStyle);
	if (inputValue != null)
		input.setAttribute("value", inputValue);
	for (var i = 0; i < events.length; i++) {
		input.setAttribute(events[i], eventsValues[i]);
	}
	return input;
}

function createButton(buttonId, buttonString, buttonClass, buttonStyle, onClick) {
	var ButtonElement = document.createElement("button");
	if (buttonClass != null)
		ButtonElement.setAttribute("class", buttonClass);
	if (buttonId != null)
		ButtonElement.setAttribute("id", buttonId);
	ButtonElement.setAttribute("onclick", onClick);
	if (buttonStyle != null)
		ButtonElement.setAttribute("style", buttonStyle);
	ButtonElement.appendChild(document.createTextNode(buttonString));
	return ButtonElement;
}

function createSelect(selectId, arrNames, arrValues, selectClass, selectStyle, onChange, compareValue) {
	if (arrNames.length !== arrValues.length)
		return null;
	var Select = document.createElement("select");
	if (selectId != null)
		Select.setAttribute("id", selectId);
	if (selectClass != null)
		Select.setAttribute("class", selectClass);
	if (selectStyle != null)
		Select.setAttribute("style", selectStyle);
	if (onChange != null)
		Select.setAttribute("onchange", onChange);
	
	for (var i = 0; i < arrNames.length; i++) {
		var op = document.createElement("option");
		op.setAttribute("value", arrValues[i]);
		op.appendChild(document.createTextNode(arrNames[i]));
		Select.appendChild(op);
		if (compareValue != null){
			if (compareValue == arrValues[i]) {
				Select.selectedIndex = i;
			}
		}
	}
	return Select;
}

function changeSelectedIndex(select, value){
	var opts = select.options;
    for(var opt, j = 0; opt = opts[j]; j++) {
        if(opt.value == value) {
            select.selectedIndex = j;
            break;
        }
    }
}

function createTable(tableId, tableClass, tableStyle, tableRows){
	var table = document.createElement("table");
	if (tableId != null)
		table.setAttribute("id", tableId);
	if (tableClass != null)
		table.setAttribute("class", tableClass);
	if (tableStyle != null)
		table.setAttribute("style", tableStyle);
	
	for (var i = 0; i < tableRows.length; i++) {
		table.appendChild(tableRows[i]);
	}
	return table;
}

function inputFocus(i){
	if(i.value==i.defaultValue){ i.value=""; i.style.color="#000"; }
}
function inputBlur(i){
	if(i.value===""){ i.value=i.defaultValue; i.style.color="#888"; }
}

function fadeout_wait(div){
	setTimeout(function(){fadeOut(div)}, 3000);
}

function changeBColor(div){
	setTimeout(function(){doChangeBColor(div)}, 1000);
}



function doChangeBColor(div){
	var ele = document.getElementById(div);
	var c=ele.style.backgroundColor;
	var l= parseInt(c.split(',')[1]);
	
	l=l+26;
	if(l<255){
		ele.style.backgroundColor='rgb(255,'+l+','+l+')';
		setTimeout(function(){doChangeBColor(div)}, 100);
	} else {
		ele.style.backgroundColor='rgb(255,255,255)';
	}
}


function fadeOut(div){
	var ele = document.getElementById(div);
	if(ele.style.opacity === "")
		ele.style.opacity = 1;
	var opacity = ele.style.opacity
	opacity -= 0.1;
	if(opacity < 0){
		opacity = 0;
	}
	ele.style.opacity = opacity;
	if(opacity != 0){
		setTimeout(function(){fadeOut(div)}, 100);
	} else {
		ele.innerHTML = "";
		ele.style.opacity = 1;
		// ele.style.filter = 'alpha(opacity=100)'; // IE fallback
	}
}



