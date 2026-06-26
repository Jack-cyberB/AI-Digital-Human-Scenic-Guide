package com.jingqu.visitor.ui.components

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jingqu.visitor.data.model.PoiDetailData

/**
 * JavaScript ↔ Kotlin bridge for the Amap WebView.
 * JS calls window.android.onRouteMarkerClick(json) etc.
 */
class MapJsBridge(
    private val onRouteMarkerClick: (PoiDetailData) -> Unit,
    private val onCategoryMarkerClick: (PoiDetailData) -> Unit,
    private val onMapCenterChanged: (lat: Double, lng: Double) -> Unit
) {
    private val gson = Gson()

    @android.webkit.JavascriptInterface
    fun onRouteMarkerClick(poiJson: String) {
        try {
            val poi = gson.fromJson(poiJson, PoiDetailData::class.java)
            Log.d(TAG, "onRouteMarkerClick: ${poi.name}")
            onRouteMarkerClick(poi)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse route marker JSON: ${e.message}")
        }
    }

    @android.webkit.JavascriptInterface
    fun onCategoryMarkerClick(poiJson: String) {
        try {
            val poi = gson.fromJson(poiJson, PoiDetailData::class.java)
            Log.d(TAG, "onCategoryMarkerClick: ${poi.name}")
            onCategoryMarkerClick(poi)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse category marker JSON: ${e.message}")
        }
    }

    @android.webkit.JavascriptInterface
    fun onMapCenterChanged(centerJson: String) {
        try {
            val map = gson.fromJson<Map<String, Double>>(
                centerJson,
                object : TypeToken<Map<String, Double>>() {}.type
            )
            val lat = map["lat"] ?: return
            val lng = map["lng"] ?: return
            onMapCenterChanged(lat, lng)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse map center: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "MapJsBridge"
    }
}
