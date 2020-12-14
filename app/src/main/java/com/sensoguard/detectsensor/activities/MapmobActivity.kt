package com.sensoguard.detectsensor.activities

import android.os.Bundle
import android.util.Log
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.offline.*
import com.sensoguard.detectsensor.R


class MapmobActivity : AppCompatActivity() {
    private var isEndNotified = false
    private var progressBar: ProgressBar? = null
    private var mapView: MapView? = null
    private var offlineManager: OfflineManager? = null
    private var btnDownloadMaps: AppCompatButton? = null

    private var myMapboxMap: MapboxMap? = null
    private var myOfflineRegion: OfflineRegion? = null
    val TAG = "MapmobFragment"

    // JSON encoding/decoding
    val JSON_CHARSET = "UTF-8"
    val JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_mapmob)

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)


        mapView?.getMapAsync { mapboxMap ->

            Log.d("", "")

            mapboxMap.setStyle(Style.SATELLITE_STREETS) {

                //myMapboxMap = mapboxMap
                //downLoadMaps(mapboxMap)

                // Map is set up and the style has loaded. Now you can add data or make other map adjustments


            }
        }
    }

    private fun downLoadMaps(mapboxMap: MapboxMap) {
        // Set up the OfflineManager
        val offlineManager = OfflineManager.getInstance(this)

// Create a bounding box for the offline region
        val latLngBounds = LatLngBounds.Builder()
            .include(LatLng(32.9683656, 35.480819)) // Northeast ,35.480819
            .include(LatLng(31.7216641, 35.1875794)) // Southwest 31.7216641,35.1875794
            .build()

// Define the offline region
        val definition = OfflineTilePyramidRegionDefinition(
            mapboxMap.style?.uri,    //.style?.uri,
            latLngBounds,
            10.0,
            20.0, this.resources.displayMetrics.density
        )


        // Implementation that uses JSON to store Yosemite National Park as the offline region name.
        var metadata: ByteArray?
        try {
//            val jsonObject = JSONObject()
//            jsonObject.put(JSON_FIELD_REGION_NAME, "Yosemite National Park")
//            val json = jsonObject.toString()
//            metadata = json.toByteArray(charset(JSON_CHARSET))

            val charset = Charsets.UTF_8
            metadata = "israel".toByteArray(charset)

        } catch (exception: Exception) {
            Log.e(TAG, "Failed to encode metadata: " + exception.message)
            metadata = null
        }

//var thread=Thread {
        // Create the region asynchronously
        if (metadata != null) {
            offlineManager.createOfflineRegion(definition, metadata,
                object : OfflineManager.CreateOfflineRegionCallback {
                    override fun onCreate(offlineRegion: OfflineRegion) {
                        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)

                        // Monitor the download progress using setObserver
                        offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                            override fun onStatusChanged(status: OfflineRegionStatus) {

                                // Calculate the download percentage
                                var percentage = if (status.requiredResourceCount >= 0)
                                    100.0 * status.completedResourceCount / status.requiredResourceCount else 0.0

                                Log.d(TAG, percentage.toString())

                                if (status.isComplete) {
                                    // Download complete
                                    Log.d(TAG, "Region downloaded successfully.")
                                } else if (status.isRequiredResourceCountPrecise) {
                                    Log.d(TAG, percentage.toString())
                                }
                            }

                            override fun onError(error: OfflineRegionError) {
                                // If an error occurs, print to logcat
                                Log.e(TAG, "onError reason: " + error.reason)
                                Log.e(TAG, "onError message: " + error.message)
                            }

                            override fun mapboxTileCountLimitExceeded(limit: Long) {
                                // Notify if offline region exceeds maximum tile count
                                Log.e(TAG, "Mapbox tile count limit exceeded: $limit")
                            }
                        })
                    }

                    override fun onError(error: String) {
                        Log.e(TAG, "Error: $error")
                    }
                })
        }

    }

}