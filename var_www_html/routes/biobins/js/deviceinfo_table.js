var array;
var binOverviewMap;
var routesLayer = null;
var leafletOSRMServicePath;
var binLocationInfoMap = {}; // Map with: key:locId values as string: latitude,longitude
var actualRouteControlLayer;

var startLocationId  = -4; // startpoint of route, editable in parameters-page later on (identical to displayed id in table - 1)
var maxSecondsPerTry = 2; // time for calculating best route for acp-algorithm(ant colony optimization), editable in parameters-page
var numOfTries = 2; // number of tries for calculating best route in aco (ant colony optimization), editable in parameters

function displayContent(contentId){
	if(contentId != null){
		// hide all pages
		document.querySelector('[id^="contentDeviceOverview"]').style.display='none';	
		document.querySelector('[id^="contentAddDevices"]').style.display='none';
		
		// display chosen page
		document.getElementById(contentId).style.display='inline';

	}
}

function setOptionbg() {
	var binMapOverviewClass;
	var id;
	var nr;
	document.querySelectorAll('[id^="sel"]').forEach(function(entry){
		id = entry.id;
		nr = id.replace("sel", "");
                
		binMapOverviewClass = "binMapOverviewMarkerIcon" + nr;
                binOverviewMap.setView([49.019389, 12.095421],12.2);

		if(!document.getElementById(id).value == "")
                {
                        document.getElementById(id).style.backgroundColor = "rgba(255, 0, 0,0.9)";
                        document.getElementById(binMapOverviewClass).style.backgroundColor = "rgba(255,0,0,0.9)";
                }
                else {
                document.getElementById(id).style.backgroundColor = "rgba(0, 255, 0,0.9)";
                document.getElementById(binMapOverviewClass).style.backgroundColor = "rgba(0,255,0,0.9)";
                }
	
	});
}

function reset() {
	document.querySelector('[id^="sel"]').id;
}

function set() {
	var id;
        var nr;
	var i = 0;
        document.querySelectorAll('[id^="sel"]').forEach(function(entry){
                id = entry.id;
                nr = id.replace("sel", "");
		if(array[i] == "25" || array[i]=="-1"){
			document.getElementById(id).options[1].selected = true;
		}
		else{
			document.getElementById(id).options[0].selected = true;
		}
		i=i+1;
	});

}



var server_url="http://im-kininvie.oth-regensburg.de";
var server_port="8888";

function requestRoute(){
	
        if(routesLayer != null) binOverviewMap.removeLayer(routesLayer);
	var fullBinsLocIds = getFullBinsFromTableData();
	var startLocation="";
	if(startLocationId >= 0) startLocation = "&startLocationId=" + startLocationId;
	var url='http://im-kininvie.oth-regensburg.de:8888/single-depot/aco?locationsToVisit='+ fullBinsLocIds + startLocation;

	waitForMsg(url);
}

function waitForMsg(url){
	    $.ajax({
        type:"Get",
        url:url,
	crossDomain: true,
	    dataType: 'text',
	    async:true,
	    data: {
		// TODO: configurable parameters
		maxSecondsPerTry: maxSecondsPerTry,
		tries: numOfTries,
		acs: true
	    },
	headers: {
	"Access-Control-Allow-Origin": "*"
       },
       cache:false,
       success: resultsReceived,
	error: errorRequestingData
        });

}

function resultsReceived(data){
	// parse entry from route-calculation and prove if it is valid
	
	var tourInfo = JSON.parse(data);
	if(!tourInfo['done'] || tourInfo['cancelled']){
		document.getElementById("resultingRoute").innerHTML = 'Die Routenberechnung hat nicht funktioniert...';
	}
	else if (tourInfo['tourLength < 0'] || tourInfo['route'] == "") 
		document.getElementById("resultingRoute").innerHTML = 'Die Routenberechnung hat kein Ergebnis geliefert...';
	else {
		displayRoute(tourInfo['tourLength'], tourInfo['route'].replace('[', '').replace(']', ''));	
	}
}

function errorRequestingData(requestObject, textStatus, errorThrown){
	console.warn("Error receiving results..." + textStatus + ":" + errorThrown);
}

function displayRoute(tourLength, routes){
        if(actualRouteControlLayer != null) actualRouteControlLayer.removeFrom(binOverviewMap);
	var routesArray = routes.split(",");
	routes = "";
	for(var i = 0; i < routesArray.length; i++) {
		if(i != 0) routes = routes + ",";
		routes = routes + (parseInt(routesArray[i])+1);
	}
	document.getElementById("resultingRoute").innerHTML = 'Resultierende Route:    Tour Länge: ' + tourLength + 'm, Reihenfolge der StandortIDs: ' + routes;
	// split route-string to locid-array
	document.getElementById("resultingRoute").innerHeight='50px';
        var routeLocIds = null;
        var latitudes, longitudes;
        if(!Array.isArray(routes)) routeLocIds = routes.split(",");
	
	// join latitude,longitude-information, needed as string without spaces in form latLng=XX.XXXXXX,XX.XXXXXX
	var locId, latLngString, latLng;
	var waypoints = new Array();
	for(var i = 0; i < routeLocIds.length; i++){
		locId = routeLocIds[i];
		latLngString = binLocationInfoMap[locId];
		latLng = latLngString.split(",");
		waypoints.push(L.latLng(latLng[0], latLng[1]));
	}

// create Layer
var control = L.Routing.control({
	waypoints: waypoints,
	lineOptions: { styles: [{color: 'green', opacity: 1, weight: 2}] },
	// insert information
	router: new L.Routing.OSRMv1({
        	serviceUrl: leafletOSRMServicePath
    	})

});
	control.addTo(binOverviewMap);
	// add layer to map & display
	control.hide();
	actualRouteControlLayer=control;
}

function getFullBinsFromTableData(){
	var route = "";
        document.querySelectorAll("[id^=sel]").forEach(function(entry){
		if(entry.value != ""){	
			if(route.length > 0) route = route + ",";
			route = route + entry.value;
		}
	});
	return route;
}


