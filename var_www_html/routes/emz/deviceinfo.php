<html>
<br></br>
<div id="divimg">
<?php

require_once("dbconf.php");
$sql = 'SELECT d.loc_id, 
	d.devicenum, 
	d.filllevel, 
	d.lastfl,
	l.latitude,
	l.longitude,
	CONCAT_WS(", ",
		TRIM(CONCAT_WS(" ", COALESCE(adr.street, ""), COALESCE(adr.house_nr, ""))), 
		TRIM(CONCAT_WS(" ",COALESCE(ci.plz, ""), COALESCE(ci.name, "")))) 
		as address, 
	DATE_FORMAT(d.timestamp, "%d.%m.%Y %H:%i") as timestamp, 
	DATE_FORMAT(d.lastfltimestamp, "%d.%m.%Y %H:%i") as lastfltimestamp 
FROM bin as d
	INNER JOIN distances.locations as l ON d.loc_id = l.loc_id
	INNER JOIN distances.location_type as t ON l.location_type_id=t.id
	INNER JOIN distances.address as adr ON adr.id=l.address_id
	INNER JOIN distances.city as ci ON ci.id = adr.city_id 
WHERE t.fraction_id="' . $fraction_id .'" 
	AND t.organization_id="' .$organization_id . '" 
	AND t.region="' . $region  . '"';

$sql2= 'SELECT * FROM devices.bin';
?>

<script>
leafletOSRMServicePath = <?php echo $leaflet_osrm_service_path; ?>;
array = new Array(<?php 

$db_erg = mysqli_query( $db_link, $sql) or die("Ungültige Abfrage: " . mysqli_error($db_link));

while ($line = mysqli_fetch_array( $db_erg ))
{
	echo '"'.$line["filllevel"] . '",';	
}

?>);


</script> 

<div id="binOverviewMap"></div>
<script type="text/javascript">

var binOverviewMap = L.map('binOverviewMap', {attributionControl: false}).setView([35.853537,10.599213], 14);
L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
    attribution: 'Map data &copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors, <a href="https://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="https://www.mapbox.com/">Mapbox</a>',
    maxZoom: 18,
    id: 'mapbox.streets',
    accessToken: 'pk.eyJ1IjoiaG9maXNpOTIiLCJhIjoiY2p2Z29lampmMDk2NTN5cDdyeGk3YmpueCJ9.z75NeyMUQWmeuxf2D0Gflg'
}).addTo(binOverviewMap);
// timeout in actual version of leaflet needed to show map in full area
setTimeout(function(){ binOverviewMap.invalidateSize()}, 50);

<?php
$db_erg = mysqli_query( $db_link, $sql ) or die("Ungültige Abfrage: " . mysqli_error($db_link));

while ($line = mysqli_fetch_array( $db_erg )){
?>
	var markerIcon = L.divIcon({html:'<div id="binMapOverviewMarkerIcon<?php echo $line["loc_id"]  ?>"><center><b> <?php echo $line["loc_id"] ?> </b></center></div>',iconSize: [24,18]});
	var marker = L.marker([<?php echo $line["latitude"] . "," . $line["longitude"] ?>], {icon: markerIcon}).addTo(binOverviewMap);
	binLocationInfoMap[<?php echo $line["loc_id"]?>]=<?php echo $line["latitude"]?> + "," + <?php echo $line["longitude"] ?>;
<?php	
 } 
 
?>
</script>

</div>
<br></br>
<br></br>
<br></br>
<br></br>
<br></br>
<br></br>
<br></br>
<br></br>
<br></br>

<div id="resultingRoute">
</div>


<h3> Behälterzustände </h3>
<div>
<form action="#" method="post">
<!--  onclick='itv = setInterval(prog, 50);' -->
<button type="button" onclick='requestRoute("Hello")'/>Route berechnen</button>
<button type="button" onclick="set();setOptionbg()">Sensor Daten laden</button>
<button type="button" onclick="reset();setOptionbg()">Reset</button>
<span style="margin-left:40"> </span>
<?php
//echo '<a target="_blank" rel="noopener noreferrer" href="/' . $project_path . 'vadim/maps/rgbmap.html">Letzte berechnete Route ansehen</a>'
?>


<div id="tables" style="margin-left: 20">

<?php
ini_set( "error_reporting", E_ALL );
ini_set( "display_errors", true );
?>
<table border="1" border-spacing="10">
<tr>
<td>ID</td>
<td>GeräteNr</td>
<td>Zustand</td>
<td>Füllstand</td>
<td>Uhrzeit</td>
<td> </td>
<td>Addresse</td>
</tr>
<?php

$db_erg = mysqli_query( $db_link, $sql ) or die("Ungültige Abfrage: " . mysqli_error($db_link));
while ($zeile = mysqli_fetch_array( $db_erg ))
{
echo '<tr>';
echo '<td> '. $zeile["loc_id"] . '</td>';
echo '<td> '. $zeile["devicenum"] . '</td>';
if($zeile["loc_id"] == $startpoint_loc_id) echo '<td> Start</td>';
else echo '<td>' . '<select onclick="setOptionbg()" name="sel' . $zeile["loc_id"] . '" id="sel' . $zeile["loc_id"] . '">' . 
	  '<option class="deviceFullStatus" value="' . ($zeile["loc_id"]-1) . '">Voll</option>' .
	 '<option class="deviceEmptyStatus" value = "">Leer</option>' .
'</select></td>';
echo '<td ALIGN="RIGHT"> '. $zeile["filllevel"] . '%</td>';
echo "<td> ". $zeile["timestamp"] . '</td>';
echo '<td> </td>';

  echo '<td> '. $zeile["address"] . '</td>';
  echo '</tr>';
}
?>
</table>
</form>

<?php
/*
if(isset($_POST["submit"])){
$dump = "[";
for ($i = 15; $i > 1; $i--) {
	if($_POST["sel".$i] != "")
	{
	$dump .= $_POST["sel".$i] . ",";
	}
}

$dump .= $startpoint_loc_id-1 . "]";
echo '<script> type="text/javascript"> requestRoute("This could be the route"); </script>';
//exec("java -jar vadim/target/dynamic-acs-1.0-jar-with-dependencies.jar --acs --tries 1 --time 5 --localsearch 0 --quiet -p 2 ".$dump, $output);
}
 */
?>
<div id="pwidget" >
<div id="progressbar">
<div id="indicator"></div>
</div>
</div>

</div>

</html>
