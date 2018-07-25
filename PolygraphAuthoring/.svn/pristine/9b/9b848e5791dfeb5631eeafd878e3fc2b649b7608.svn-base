function saveAppName(){
	var appName = document.getElementById("appName");
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			//document.getElementById("buttonResult").innerHTML = "Saved.";
			//fadeout_wait("buttonResult");
			var barItem = document.getElementById("barAppName");
			barItem.innerHTML = appName.value;
		}
	};
	xhttp.open("POST", "3updateAppName.jsp", true);
	xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	xhttp.send("name=" + appName.value + "");
}

function getCode(){
	var appName = document.getElementById("appName");
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (xhttp.readyState == 4 && xhttp.status == 200) {
			downloadFile(xhttp.responseText);
		}
	};
	xhttp.open("POST", "3getCode.jsp", true);
	xhttp.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
	xhttp.send("name=" + appName.value + "");	
}

function downloadFile(text) {
	var element = document.createElement('a');
	element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
	element.setAttribute('download', 'code.txt');

	element.style.display = 'none';
	document.body.appendChild(element);

	element.click();
	document.body.removeChild(element);
}