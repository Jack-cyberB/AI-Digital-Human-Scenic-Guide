package com.jingqu.visitor.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.jingqu.visitor.ui.components.MapJsBridge
import com.jingqu.visitor.ui.components.CategoryToggleBar
import com.jingqu.visitor.data.model.PoiDetailData
import com.jingqu.visitor.data.model.PoiCategory

val ROUTE_COLORS_LIST = listOf(
    0xFF1890FFL, 0xFF52C41AL, 0xFF722ED1L, 0xFFF5222DL, 0xFFFAAD14L,
    0xFFEB2F96L, 0xFF13C2C2L, 0xFFFA541CL, 0xFF2F54EBL, 0xFFA0D911L
)

private data class RoutePt(val keyword: String, val city: String)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapRouteScreen(viewModel: MainViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val routeJson = uiState.routeDataJson
    val mode = uiState.routeMode ?: "city"
    val isScenic = mode == "scenic"

    var webView by remember { mutableStateOf<WebView?>(null) }
    var routePoints by remember { mutableStateOf<List<List<RoutePt>>>(emptyList()) }
    var activeDay by remember { mutableIntStateOf(0) }
    var activePoint by remember { mutableIntStateOf(0) }
    var isMapReady by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val latestRouteJson = remember { mutableStateOf<String?>(null) }

    // POI detail state
    var selectedPoiData by remember { mutableStateOf<PoiDetailData?>(null) }
    var aiDescription by remember { mutableStateOf<String?>(null) }
    var aiReviews by remember { mutableStateOf<List<com.jingqu.visitor.data.model.ReviewCard>>(emptyList()) }
    var isLoadingAiEnrich by remember { mutableStateOf(false) }
    var showFullDetail by remember { mutableStateOf(false) }

    // Category toggle state (Phase 4)
    var activeCategories by remember { mutableStateOf<Set<PoiCategory>>(emptySet()) }

    LaunchedEffect(Unit) {
        MainViewModel.routeCache?.let { json ->
            if (routeJson == null) {
                latestRouteJson.value = json
                routePoints = parseRoutes(json)
            }
        }
    }

    LaunchedEffect(routeJson) {
        if (routeJson != null) {
            latestRouteJson.value = routeJson
            routePoints = parseRoutes(routeJson)
            if (isMapReady && webView != null) {
                webView?.evaluateJavascript("console.log('renderRoute from LaunchedEffect');renderRoute(" + routeJson.toJsLiteral() + ")", null)
            }
            coroutineScope.launch { sheetState.bottomSheetState.partialExpand() }
        }
    }

    // Wrap everything in a Box so PlaceDetailScreen can overlay above BottomSheetScaffold
    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = sheetState,
            sheetPeekHeight = if (routePoints.isNotEmpty()) 48.dp else 0.dp,
            sheetContent = {
                if (routePoints.isNotEmpty() && activeDay < routePoints.size) {
                    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8F9FA)).padding(8.dp)) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                            Box(modifier = Modifier.width(32.dp).height(4.dp).clip(MaterialTheme.shapes.small).background(Color(0xFFCCCCCC)))
                        }

                        val label = if (isScenic) "路线" else "第"
                        val suffix = if (isScenic) "" else "天"
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            routePoints.forEachIndexed { idx, _ ->
                                val c = Color(ROUTE_COLORS_LIST[idx % ROUTE_COLORS_LIST.size])
                                val sel = activeDay == idx
                                Surface(onClick = { activeDay = idx }, shape = MaterialTheme.shapes.medium,
                                    color = if (sel) c.copy(alpha = 0.15f) else Color(0xFFF0F0F0),
                                    contentColor = if (sel) c else Color(0xFF666666)) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(c))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("$label${idx + 1}$suffix", fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        val group = routePoints[activeDay]
                        group.forEachIndexed { idx, pt ->
                            Row(modifier = Modifier.fillMaxWidth().clickable {
                                activePoint = idx
                                // Focus map + get rich data from JS cache, then show detail
                                webView?.evaluateJavascript("focusPoint($activeDay,$idx)", null)
                                val dayIdx = activeDay; val pointIdx = idx
                                val fallbackPoi = PoiDetailData(name = pt.keyword, city = pt.city)
                                webView?.evaluateJavascript("JSON.stringify(poiDataCache['${dayIdx}_$pointIdx']||{})") { json ->
                                    var poi = fallbackPoi
                                    try {
                                        // evaluateJavascript double-encodes: unwrap outer JSON string first
                                        val innerJson = com.google.gson.Gson().fromJson(json, String::class.java)
                                        val detail = com.google.gson.Gson().fromJson(innerJson, PoiDetailData::class.java)
                                        if (detail != null && detail.name.isNotBlank()) poi = detail
                                    } catch (_: Exception) {}
                                    // Now everything uses the same rich data as marker click
                                    selectedPoiData = poi
                                    aiDescription = null; aiReviews = emptyList()
                                    isLoadingAiEnrich = true; showFullDetail = true
                                    viewModel.loadAiEnrichForPoi(poi) { desc, reviews ->
                                        aiDescription = desc; aiReviews = reviews
                                        isLoadingAiEnrich = false
                                    }
                                }
                            }.padding(horizontal = 12.dp, vertical = 6.dp).background(if (activePoint == idx) Color(0xFFE6F7FF) else Color.Transparent),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("${idx + 1}", color = Color(ROUTE_COLORS_LIST[activeDay % ROUTE_COLORS_LIST.size]), fontSize = 14.sp, modifier = Modifier.width(24.dp))
                                Column(modifier = Modifier.weight(1f)) { Text(pt.keyword, style = MaterialTheme.typography.bodyMedium); Text(pt.city, style = MaterialTheme.typography.labelSmall, color = Color(0xFF999999)) }
                                Icon(Icons.Default.ChevronRight, null, tint = Color(0xFFCCCCCC))
                            }
                        }
                    }
                }
            },
            content = {
                Box(modifier = Modifier.fillMaxSize().padding(it)) {
                    AndroidView(factory = { ctx ->
                        WebView(ctx).apply {
                            webView = this
                            settings.javaScriptEnabled = true; settings.domStorageEnabled = true
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                            val b = MapJsBridge(
                                onRouteMarkerClick = { poi ->
                                    Log.d("WV", "Marker clicked: ${poi.name}")
                                    selectedPoiData = poi; aiDescription = null; aiReviews = emptyList()
                                    isLoadingAiEnrich = true; showFullDetail = true
                                    viewModel.loadAiEnrichForPoi(poi) { desc, reviews ->
                                        aiDescription = desc; aiReviews = reviews
                                        isLoadingAiEnrich = false
                                    }
                                },
                                onCategoryMarkerClick = { poi ->
                                    Log.d("WV", "Category marker: ${poi.name}")
                                    selectedPoiData = poi; aiDescription = null; aiReviews = emptyList()
                                    isLoadingAiEnrich = true; showFullDetail = true
                                    viewModel.loadAiEnrichForPoi(poi) { desc, reviews ->
                                        aiDescription = desc; aiReviews = reviews
                                        isLoadingAiEnrich = false
                                    }
                                },
                                onMapCenterChanged = { _, _ -> }
                            )
                            addJavascriptInterface(b, "android")

                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(msg: android.webkit.ConsoleMessage?): Boolean {
                                    Log.d("WV", "JS:" + (msg?.message() ?: "")); return true
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(v: WebView?, url: String?) {
                                    isMapReady = true
                                    latestRouteJson.value?.let { json ->
                                        Log.d("WV", "onPageFinished: rendering route")
                                        v?.evaluateJavascript("console.log('onPageFinished renderRoute');renderRoute(" + json.toJsLiteral() + ")", null)
                                    }
                                }
                            }
                            loadDataWithBaseURL("https://webapi.amap.com/", MAP_HTML, "text/html", "UTF-8", null)
                            postDelayed({
                                evaluateJavascript("document.getElementById('map').style.height='" + height + "px';if(window.map){window.map.setFitView();window.map.setZoom(12)}", null)
                                val json = latestRouteJson.value
                                if (json != null) {
                                    postDelayed({ evaluateJavascript("renderRoute(" + json.toJsLiteral() + ")", null) }, 1500)
                                }
                            }, 1500)
                        }
                    }, modifier = Modifier.fillMaxSize())

                    // Category toggle bar
                    CategoryToggleBar(
                        activeCategories = activeCategories,
                        onToggle = { cat ->
                            val newSet = if (cat in activeCategories) activeCategories - cat else activeCategories + cat
                            activeCategories = newSet
                            val colorHex = String.format("#%06X", cat.color and 0xFFFFFF)
                            if (cat in newSet) {
                                webView?.evaluateJavascript(
                                    "searchNearbyPois(39.905,116.397,5000,'${cat.amapType}','$colorHex','${cat.emoji}')",
                                    null
                                )
                            } else {
                                webView?.evaluateJavascript(
                                    "var k='cm_${cat.amapType}';if(window[k]){window[k].forEach(function(m){m.setMap(null)});delete window[k]}",
                                    null
                                )
                            }
                        },
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                    )

                    // Floating toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp).padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.92f), shadowElevation = 4.dp) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(4.dp)) {
                                IconButton(onClick = { webView?.evaluateJavascript("fitToRoutes()", null) }) {
                                    Icon(Icons.Default.FitScreen, "适应路线", tint = Color(0xFF1890FF))
                                }
                                IconButton(onClick = {
                                    val json = routeJson ?: latestRouteJson.value
                                    json?.let { webView?.evaluateJavascript("renderRoute(" + it.toJsLiteral() + ")", null) }
                                }) {
                                    Icon(Icons.Default.Refresh, "重新生成", tint = Color(0xFF1890FF))
                                }
                            }
                        }
                    }
                }
            }
        )

        // PlaceDetailScreen overlay - outside BottomSheetScaffold to avoid nesting
        if (showFullDetail && selectedPoiData != null) {
            PlaceDetailScreen(
                poi = selectedPoiData!!,
                aiDescription = aiDescription,
                aiReviews = aiReviews,
                isLoadingAi = isLoadingAiEnrich,
                onBack = { showFullDetail = false },
                onNavigate = {
                    val p = selectedPoiData!!
                    if (p.lat != 0.0 && p.lng != 0.0) {
                        val uri = "androidamap://navi?sourceApplication=jingqu&lat=${p.lat}&lon=${p.lng}&dev=0"
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uri))
                        try { webView?.context?.startActivity(intent) } catch (e: Exception) {
                            val webUri = "https://uri.amap.com/navigation?to=${p.lng},${p.lat},${p.name}"
                            val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(webUri))
                            webView?.context?.startActivity(webIntent)
                        }
                    }
                },
                onLoadAiEnrich = {
                    isLoadingAiEnrich = true
                    viewModel.loadAiEnrichForPoi(selectedPoiData!!) { desc, reviews ->
                        aiDescription = desc; aiReviews = reviews
                        isLoadingAiEnrich = false
                    }
                }
            )
        }
    }
}

private fun parseRoutes(json: String): List<List<RoutePt>> {
    try { val root = JSONObject(json); val routes = root.getJSONArray("dailyRoutes"); val r = mutableListOf<List<RoutePt>>()
        for (i in 0 until routes.length()) { val day = routes.getJSONObject(i).getJSONArray("points"); val pts = mutableListOf<RoutePt>()
            for (j in 0 until day.length()) { val p = day.getJSONObject(j); pts.add(RoutePt(p.getString("keyword"), p.getString("city"))) }; r.add(pts) }; return r
    } catch (e: Exception) { return emptyList() }
}

private fun String.toJsLiteral(): String = "'" + replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n") + "'"

val MAP_HTML = """
<!DOCTYPE html>
<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>*{margin:0;padding:0} html,body{width:100vw;height:100vh;background:#fff}
#map{width:100%;height:100vh}</style></head><body>
<div id="map"></div><div id="log" style="position:absolute;bottom:0;left:0;color:#333;font-size:10px;background:rgba(255,255,255,0.9);padding:2px;z-index:999"></div>
<script>
document.getElementById('log').innerText='Loading...';console.log('SCRIPT_START');
window._AMapSecurityConfig={securityJsCode:'364432508d71f2b0fb200f22611a9b91'};
var s=document.createElement('script');
s.src='https://webapi.amap.com/maps?v=2.0&key=4f92d1e69d842f517f46af6b62ef0e80&plugin=AMap.Driving,AMap.Walking,AMap.PlaceSearch';
s.onload=function(){
console.log('AMAP_LOADED');
var div=document.getElementById('map');
console.log('MapDiv:'+div.offsetWidth+'x'+div.offsetHeight);
setTimeout(function(){
var map=new AMap.Map('map',{zoom:12,center:[116.397,39.905],resizeEnable:true});
console.log('MAP_CREATED');var c=document.querySelector('#map canvas');if(c)console.log('Canvas:'+c.width+'x'+c.height);
setTimeout(function(){map.setFitView();map.setZoom(10);},1000);
AMap.plugin(['AMap.Driving','AMap.Walking'],function(){
window.driving=new AMap.Driving({map:map,hideMarkers:true,autoFitView:false});
window.walking=new AMap.Walking({map:map,hideMarkers:true,autoFitView:false});
});
window.map=map;
map.on('moveend',function(){var c=map.getCenter();if(c)try{window.android.onMapCenterChanged(JSON.stringify({lat:c.lat,lng:c.lng}));}catch(e){}});
document.getElementById('log').innerText='Map OK';
},300);
};
document.head.appendChild(s);
var allMarkers=[];
var poiDataCache={};
function haversine(lat1,lng1,lat2,lng2){var R=6371000,dLat=(lat2-lat1)*Math.PI/180,dLng=(lng2-lng1)*Math.PI/180;var a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(lat1*Math.PI/180)*Math.cos(lat2*Math.PI/180)*Math.sin(dLng/2)*Math.sin(dLng/2);return R*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a));}
function fitToRoutes(){if(window.map&&allMarkers.length>0)window.map.setFitView(allMarkers)}
function focusPoint(d,i){if(allMarkers[d]&&allMarkers[d][i]){window.map.setCenter(allMarkers[d][i].getPosition());window.map.setZoom(17)}}
function focusOnPoi(lat,lng){window.map.setCenter([lng,lat]);window.map.setZoom(16);}
function renderRoute(json){
console.log('RR:start');
var d=JSON.parse(json),routes=d.dailyRoutes||[],colors=['#1890FF','#52C41A','#722ED1','#F5222D','#FAAD14'];
var isScenic=true;window.map.clearMap();allMarkers=[];
(async function(){
for(var di=0;di<routes.length;di++){var pts=routes[di].points,color=colors[di%5],coords=[],dayMarkers=[];
for(var i=0;i<pts.length;i++){var pt=pts[i];
// Step 1: geocode to get location + POI id (no extensions for accurate results)
var geoResult=await new Promise(function(r){new AMap.PlaceSearch({city:pt.city,pageSize:1}).search(pt.keyword,function(s,re){
if(s=='complete'&&re&&re.poiList&&re.poiList.pois&&re.poiList.pois.length>0)r(re.poiList.pois[0]);else r(null)});});
let ptKeyword=pt.keyword, ptCity=pt.city; let loc=geoResult?geoResult.location:null;
let poiId=geoResult?geoResult.id:null;
if(loc){coords.push(loc);
let mk=new AMap.Marker({position:loc,title:pt.keyword,label:{content:'<div style="background:'+color+';color:#fff;padding:2px 6px;border-radius:10px;font-size:11px">'+(i+1)+'</div>',offset:new AMap.Pixel(0,-30)}});
mk.setMap(window.map);dayMarkers.push(mk);
// Cache key + basic data immediately (LET for per-iteration closure)
let cacheKey=di+'_'+i;
poiDataCache[cacheKey]={name:ptKeyword,city:ptCity,lat:loc.lat,lng:loc.lng,address:'',category:'',categoryTag:'',rating:null,openingHours:'',cost:'',phone:'',photos:[]};
// Step 2: get rich detail by POI id (correct POI, not search guess)
if(poiId){new AMap.PlaceSearch({extensions:'all'}).getDetails(poiId,function(sd,red){
if(sd=='complete'&&red&&red.poiList&&red.poiList.pois&&red.poiList.pois.length>0){
let detail=red.poiList.pois[0];
poiDataCache[cacheKey]={
name:detail.name||ptKeyword,
address:detail.paddress||detail.address||'',
city:ptCity,
lat:loc.lat,lng:loc.lng,
category:detail.type?detail.type.split(';')[0]:'',
categoryTag:detail.type||'',
rating:detail.biz_ext&&detail.biz_ext.rating?detail.biz_ext.rating:null,
openingHours:detail.deep_info?detail.deep_info.opentime||'':'',
cost:detail.biz_ext&&detail.biz_ext.cost?detail.biz_ext.cost:'',
phone:detail.tel||'',
photos:detail.photos?detail.photos.map(function(p){return p.url}):[]
};
}});}
// Click handler
mk.on('click',function(){let d=poiDataCache[cacheKey];console.log('MK_CLICK:'+d.name);try{window.android.onRouteMarkerClick(JSON.stringify(d));}catch(e){console.log('Bridge err:'+e);}});
}
if(i<pts.length-1)await new Promise(function(r){setTimeout(r,600)});}
console.log('RR:day'+di+' geocoded '+coords.length+'/'+pts.length);
if(isScenic&&coords.length>=2){var sumLng=0,sumLat=0;
for(var k=0;k<coords.length;k++){sumLng+=coords[k].lng;sumLat+=coords[k].lat;}
var cLng=sumLng/coords.length,cLat=sumLat/coords.length;
var goodCoords=[],goodMarkers=[];
for(var k=0;k<coords.length;k++){var dist=haversine(cLat,cLng,coords[k].lat,coords[k].lng);
if(dist<3000){goodCoords.push(coords[k]);goodMarkers.push(dayMarkers[k]);}
else{console.log('RR:outlier removed dist='+Math.round(dist)+'m');dayMarkers[k].setMap(null);}}
if(goodCoords.length<coords.length){console.log('RR:day'+di+' filtered '+(coords.length-goodCoords.length)+' outliers, '+goodCoords.length+' remain');coords=goodCoords;dayMarkers=goodMarkers;}}
for(var m=0;m<dayMarkers.length;m++){allMarkers.push(dayMarkers[m]);}
if(coords.length>=2){
if(isScenic&&window.walking){for(var j=0;j<coords.length-1;j++){
await new Promise(function(r){window.walking.search(coords[j],coords[j+1],function(s,res){if(s=='complete'&&res.routes&&res.routes[0])res.routes[0].steps.forEach(function(st){if(st.path){var pl=new AMap.Polyline({path:st.path,strokeColor:color,strokeWeight:5,strokeOpacity:0.7,strokeStyle:di===0?'solid':'dashed'});pl.setMap(window.map)}});r()})});
if(j<coords.length-2)await new Promise(function(r){setTimeout(r,300)});}
console.log('RR:walk route done day'+di);
}else{await new Promise(function(r){window.driving.search(coords[0],coords[coords.length-1],
coords.length>2?{waypoints:coords.slice(1,-1).map(function(c){return[c.lng,c.lat]})}:{},
function(s,res){if(s=='complete'&&res.routes&&res.routes[0])res.routes[0].steps.forEach(function(st){if(st.path){
var pl=new AMap.Polyline({path:st.path,strokeColor:color,strokeWeight:6,strokeStyle:di===0?'solid':'dashed'});pl.setMap(window.map)}});r()})});
console.log('RR:drive route done day'+di);}}
if(di<routes.length-1)await new Promise(function(r){setTimeout(r,2000)});}
console.log('RR:ALL DONE markers='+allMarkers.length);if(allMarkers.length>0)window.map.setFitView(allMarkers);
var c=window.map.getCenter();if(c)try{window.android.onMapCenterChanged(JSON.stringify({lat:c.lat,lng:c.lng}));}catch(e){}
})();
}
// Category nearby POI search
function searchNearbyPois(lat,lng,radius,typeFilter,color,emoji){
var key='cm_'+typeFilter;
if(window[key]){window[key].forEach(function(m){m.setMap(null)});}
window[key]=[];
new AMap.PlaceSearch({type:typeFilter,pageSize:30,extensions:'all'}).searchNearBy('',[lng,lat],radius,function(s,re){
if(s=='complete'&&re&&re.poiList&&re.poiList.pois){
re.poiList.pois.forEach(function(poi){
var content='<div style="width:32px;height:32px;background:'+color+';border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:14px;box-shadow:0 2px 4px rgba(0,0,0,0.3);border:2px solid #fff">'+emoji+'</div>';
let mk=new AMap.Marker({position:poi.location,content:content,offset:new AMap.Pixel(-16,-16),zIndex:90});
mk.on('click',function(){
let detail={name:poi.name,address:poi.paddress||poi.address||'',lat:poi.location.lat,lng:poi.location.lng,category:poi.type?poi.type.split(';')[0]:'',categoryTag:poi.type||'',rating:poi.biz_ext&&poi.biz_ext.rating?poi.biz_ext.rating:null,openingHours:poi.deep_info?poi.deep_info.opentime||'':'',phone:poi.tel||'',photos:poi.photos?poi.photos.map(function(p){return p.url}):[]};
try{window.android.onCategoryMarkerClick(JSON.stringify(detail));}catch(e){}
});
mk.setMap(window.map);window[key].push(mk);
});
console.log('Nearby: '+window[key].length+' markers for type='+typeFilter);
}
});
}
</script></body></html>
""".trimIndent()
