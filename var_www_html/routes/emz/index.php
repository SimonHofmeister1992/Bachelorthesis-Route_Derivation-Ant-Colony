<html>
<head>
<meta http-equiv="Content-Type" content="text/html;charset=UTF-8">
<link rel="stylesheet" type="text/css" href="style.css">

<!-- JQuery-->
 <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.4.0/jquery.min.js"></script>

<!-- include bootstraps for better designs -->
<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">

<!-- include leaflet for maps -->
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.5.1/dist/leaflet.css"
   integrity="sha512-xwE/Az9zrjBIphAcBb3F6JVqxf46+CDLwfLMHloNu6KEQCAWi6HcDUbeOfBIptF7tcCzusKFjFw2yuvEpDL9wQ=="
   crossorigin=""/>
<!-- route-machine of leaflet -->
<!-- 
<link rel="stylesheet" href="https://unpkg.com/leaflet-routing-machine@latest/dist/leaflet-routing-machine.css" />
-->
<link rel="stylesheet" href="http://im-kininvie.oth-regensburg.de/leaflet-routing-machine-3.2.12/dist/leaflet-routing-machine.css" />

 <!-- Leaflet-script: Make sure you put this AFTER Leaflet's CSS -->
 <script src="https://unpkg.com/leaflet@1.5.1/dist/leaflet.js"
   integrity="sha512-GffPMF3RvMeYyc1LWMHtK8EbPv0iNZ8/oTtHPx9/cc2ILxQ+u905qIwdpULaqDkyBKgOaB57QTMg7ztg8Jm2Og=="
   crossorigin=""></script>
<!-- routing-machine script for leaflet -->
<!--
<script src="https://unpkg.com/leaflet-routing-machine@latest/dist/leaflet-routing-machine.js"></script>
-->
<script src="http://im-kininvie.oth-regensburg.de/leaflet-routing-machine-3.2.12/dist/leaflet-routing-machine.js"></script>

<?php
if($_GET["section"] == "deviceinfo" || $_GET["section"] == null){
	echo '<script type="text/javascript" charset="utf-8" src="js/deviceinfo_table.js"></script>';
}
?>
</head>
<?php
if($_GET["section"] == "deviceinfo" || $_GET["section"] == null){
	echo '<body onload="set();setOptionbg()">';
}
else{
	echo '<body>';	
}
include 'organization_theme.php';
include 'requests_config.php';
$headerfooter_background_color=lightgrey;
echo '<div id="header" style="background-color:' . $headerfooter_background_color . '">';
?>
<div id ="logo"> 
<img src="img/logo.png" alt="logo" width="100%" height="100%">
</div>
<div id="head">
<h1 style='float:left'>EMZ</h1>
</div>

<br></br>

<!-- navigation menu  -->

<div class="topnav"> 
  
<a href="index.php" style="margin-left:-230" > Übersicht </a>
  <span style="margin-left:10"> </span>
  <a href="index.php?section=adddevices"> Standorte </a>
  <span style="margin-left:10"> </span>
  <a href="index.php?section=editparameters"> Parameter </a>
  <span style="margin-left:10"> </span>
  <a href="index.php?section=licenses"> Licenses </a>


<br></br>
</div>

<!-- included content. chosen - by example - by navigation menu  -->

<div id="content">
</div>

<?php

$section = isset($_GET["section"]) ? trim(strtolower($_GET["section"])) : "deviceinfo";
	
	$allowedPages = array(
		""           => "deviceinfo.php",
		"deviceinfo" => "deviceinfo.php",
		"adddevices" => "adddevices.php",
		"editparameters" => "editparameters.php",
		"licenses" => "licenses.php"
	);
	include(isset($allowedPages[$section]) ? $allowedPages[$section] : $allowedPages["deviceinfo"] );


?>

<?php
echo '<div id="footer" style="background-color:' . $headerfooter_background_color . '">';
?>
<a target='_blank' rel='noopener noreferrer' href='https://www.oth-regensburg.de/impressum.html'>Impressum</a>
<a target='_blank' rel='noopener noreferrer' href='https://www.oth-regensburg.de/datenschutz.html'>Datenschutz</a>
</div>


</body>
</html>
