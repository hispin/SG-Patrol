package com.sensoguard.detectsensor.activities

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolygonOptions
import com.mapbox.bindgen.Value
import com.mapbox.common.Cancelable
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.MapboxMapsOptions.tileStore
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.maps.plugin.annotation.Annotation
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.OnPointAnnotationDragListener
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.sensoguard.detectsensor.R
import com.sensoguard.detectsensor.global.CURRENT_LATITUDE_PREF
import com.sensoguard.detectsensor.global.CURRENT_LONGTUDE_PREF
import com.sensoguard.detectsensor.global.getStringInPreference
import kotlin.let as let1


class DownloadOfflineTilesActivity : ParentActivity() {


    // JSON encoding/decoding

    private var polygonAnnotationOptions: PolygonAnnotationOptions? = null
    private var mPolygonAnnotationManager: PolygonAnnotationManager? = null
    val JSON_CHARSET: String = "UTF-8"

    val JSON_FIELD_REGION_NAME: String = "FIELD_REGION_NAME"

    val ID_ICON_LOCATION: String = "location"

    val TAG: String = "DownloadOfflineTilesActivity"
    val locationOne: LatLng = LatLng(32.173001, 34.842284)
    val locationTwo: LatLng = LatLng(32.067477, 34.801851)
    var boundsArea: PolygonOptions? = null
    var sum: Int = 0
    private var isEndNotified = true
    private var progressBar: ProgressBar? = null
    private var mapView: MapView? = null

    //private var offlineManager: OfflineManager? = null
    private var tvResults: AppCompatTextView? = null
    private var myMapboxMap: MapboxMap? = null
    private var myStyle: Style? = null
    private var btnDownload: AppCompatButton? = null
    private var btnDelete: AppCompatButton? = null
    private val myTopRight: LatLng? = null
    private var myTopRightP: Point? = null
    private val myBottomLeft: LatLng? = null
    private var myBottomLeftP: Point? = null
    private val polyRegions: Iterator<LatLng>? = null


    private var myLocate: LatLng? = null
    private var locationManager: LocationManager? = null

    //annotations (markers)
    private var viewAnnotationManager: ViewAnnotationManager? =
        null //mapView?.viewAnnotationManager
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var annotationApi: AnnotationPlugin? = null
    private var pointAnnotation: PointAnnotation? = null
    private var pointTopRight: PointAnnotation? = null
    private var pointBottomLeft: PointAnnotation? = null
    private val cancelables = mutableListOf<Cancelable>()
    private val offlineManager: OfflineManager = OfflineManager()


    private val TILE_REGION_METADATA = "my-offline-region"

    //private lateinit var binding: ActivityOfflineBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_download_tiles)
        mapView = findViewById(R.id.mapView)


        progressBar = findViewById(R.id.progress_bar)

        //mapView.onCreate(savedInstanceState);
        tvResults = findViewById(R.id.tvResults)

        btnDownload = findViewById(R.id.btnDownload)
        btnDownload?.setOnClickListener(View.OnClickListener {
            downloadOfflineRegion()
            //haggay downLoadOfflineMap();
        })

        btnDelete = findViewById(R.id.btnDelete)
        btnDelete?.setOnClickListener(View.OnClickListener {
            //haggay deleteTile();
        })

        myMapboxMap = mapView?.mapboxMap
        myMapboxMap!!.loadStyle(Style.SATELLITE_STREETS)
        myStyle = myMapboxMap!!.style


        //go to last location
        val location: Location? = initFindLocation()


        //set last location if exist
        if (location != null) {
            myLocate =
                LatLng(location.latitude, location.longitude)
        }

        showLocation(location)

        mapView?.post(Runnable {
            initializeAnnotation(mapView)
            var viewportWidth = mapView?.width
            val viewportHeight = mapView?.height
            viewportWidth = viewportWidth?.plus(500)

            if (viewportWidth != null && viewportHeight != null) {

                val pixel1 =
                    ScreenCoordinate(
                        ((viewportWidth / 2)).toDouble(),
                        (viewportHeight / 4).toDouble()
                    )
                myTopRightP = myMapboxMap!!.coordinateForPixel(pixel1)
                val pixel2 =
                    ScreenCoordinate(
                        ((viewportWidth) / 4).toDouble(),
                        (viewportHeight / 2).toDouble()
                    )
                myBottomLeftP = myMapboxMap!!.coordinateForPixel(pixel2)


                //var polygonCreateor = new PolygonCreator();
                if (mapView != null && myTopRightP != null && myBottomLeftP != null) {
                    drawRectangle(mapView!!, myTopRightP!!, myBottomLeftP!!)
                    addMarkerIconsToMap()
                }
                //instance.drawRectangle(mapView!!, myTopRightP!!, myBottomLeftP!!)
                //PolygonCreator..drawRectangle(mapView, myTopRightP, myBottomLeftP);
                // end haggay
            }
        })

    }

    /**
     * initialize annotation for markers
     */
    fun initializeAnnotation(mapView: MapView?) {
        viewAnnotationManager = mapView?.viewAnnotationManager
        // Create an instance of the Annotation API and get the PointAnnotationManager.
        annotationApi = mapView?.annotations
        pointAnnotationManager = annotationApi?.createPointAnnotationManager()
    }

    //get last location
    private fun initFindLocation(): Location? {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager


        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            return locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }

        return null
    }

    // move the camera to ic_mark location
    private fun showLocation(location: Location?) {
        if (location != null) {
            setMyLocate(
                LatLng(
                    location.latitude,
                    location.longitude
                )
            )
        } else {
            myLocate = getLastLocationLocally()

            if (myLocate == null) {
                //set default location (london)
                myLocate = LatLng(51.509865, -0.118092)
                //set default location (london) if there is no last location
                setMyLocate(LatLng(51.509865, -0.118092))
            }
        }
        //add marker at the focus of the map
        if (myLocate != null) {
            //load the camera
            if (myLocate != null
            ) {
                val cameraPosition = CameraOptions.Builder()
                    .zoom(15.0)
                    .center(
                        Point.fromLngLat(
                            myLocate!!.longitude,
                            myLocate!!.latitude
                        )
                    ) //Point.fromLngLat(myLocate?.latitude!!, myLocate?.longitude!!))
                    .build()

                myMapboxMap!!.setCamera(cameraPosition)

                //howMarkers()
            }
        }
    }

    /**
     * draw rectangle on map
     */
    fun drawRectangle(
        mapView: MapView,
        myTopRightP: Point,
        myBottomLeftP: Point
    ) {

        val topLeft = LatLng(myBottomLeftP.latitude(), myTopRightP.longitude())
        val bottomRight = LatLng(myTopRightP.latitude(), myBottomLeftP.longitude())


        // Create an instance of the Annotation API and get the polygon manager.
        val annotationApi = mapView.annotations

        if (mPolygonAnnotationManager == null) {
            mPolygonAnnotationManager = annotationApi.createPolygonAnnotationManager()
        } else {
            mPolygonAnnotationManager?.deleteAll()
        }


        // Define a list of geographic coordinates to be connected.
        val points = listOf(
            listOf(
                Point.fromLngLat(topLeft.longitude, topLeft.latitude),
                Point.fromLngLat(myTopRightP.longitude(), myTopRightP.latitude()),
                Point.fromLngLat(bottomRight.longitude, bottomRight.latitude),
                Point.fromLngLat(myBottomLeftP.longitude(), myBottomLeftP.latitude())
            )
        )

        // Set options for the resulting fill layer.
        polygonAnnotationOptions = PolygonAnnotationOptions()
            .withPoints(points) // Style the polygon that will be added to the map.
            .withFillColor(Color.RED)
            .withFillOpacity(0.4)

        if (polygonAnnotationOptions != null) {
            // Add the resulting polygon to the map.
            mPolygonAnnotationManager?.create(polygonAnnotationOptions!!)
        }
    }


    private fun setMyLocate(myLocate: LatLng) {
        this.myLocate = myLocate
    }

    //get last location from shared preference
    private fun getLastLocationLocally(): LatLng? {
        val latitude = getStringInPreference(this, CURRENT_LATITUDE_PREF, "-1")
        val longtude = getStringInPreference(this, CURRENT_LONGTUDE_PREF, "-1")
        var lat = 0.0
        var lon = 0.0

        if (latitude != "-1" && longtude != "-1") {
            try {
                lat = latitude!!.toDouble()
                lon = longtude!!.toDouble()
                return LatLng(lat, lon)
            } catch (ex: NumberFormatException) {
            }
        }
        return null
    }


    /**
     * Add marker to polygon to enable dragging
     */
    private fun addMarkerIconsToMap() {


        if (pointAnnotation == null && myTopRightP?.longitude() != null && myTopRightP?.longitude() != null) {


            // Set options for the resulting symbol layer.
            val pointAnnotationOptions1: PointAnnotationOptions = PointAnnotationOptions()
                // Define a geographic coordinate.
                .withPoint(Point.fromLngLat(myTopRightP?.longitude()!!, myTopRightP?.latitude()!!))
                // Specify the bitmap you assigned to the point annotation
                // The bitmap will be added to map style automatically.
                .withIconImage(
                    BitmapFactory.decodeResource(
                        resources, R.drawable.ic_sensor_alarm
                    )
                )
                //.withIconImage("icon-id")
                .withDraggable(true)
                .withIconSize(1.5)


            //pointAnnotationOptions.ad
            // Add the resulting pointAnnotation to the map.
            pointTopRight = pointAnnotationManager?.create(pointAnnotationOptions1)

            pointAnnotation?.id


            val pointAnnotationOptions2: PointAnnotationOptions = PointAnnotationOptions()
                .withPoint(
                    Point.fromLngLat(
                        myBottomLeftP?.longitude()!!,
                        myBottomLeftP?.latitude()!!
                    )
                )
                // Specify the bitmap you assigned to the point annotation
                // The bitmap will be added to map style automatically.
                .withIconImage(
                    BitmapFactory.decodeResource(
                        resources, R.drawable.ic_sensor_alarm
                    )
                )
                //.withIconImage("icon-id")
                .withDraggable(true)
                .withIconSize(1.5)


            // Add the resulting pointAnnotation to the map.
            pointBottomLeft = pointAnnotationManager?.create(pointAnnotationOptions2)



            pointAnnotationManager!!.addDragListener(object : OnPointAnnotationDragListener {

                override fun onAnnotationDrag(annotation: Annotation<*>) {
                }

                override fun onAnnotationDragFinished(annotation: Annotation<*>) {

                    if (pointTopRight?.id?.equals(annotation.id) == true
                        && pointTopRight?.geometry?.longitude() != null
                        && pointTopRight?.geometry?.latitude() != null
                    ) {

                        myTopRightP = Point.fromLngLat(
                            pointTopRight?.geometry?.longitude()!!,
                            pointTopRight?.geometry?.latitude()!!
                        )
                        Log.d(
                            "idIcon",
                            "iconOne lat:" + pointTopRight?.geometry?.latitude() + " long:" + pointTopRight?.geometry?.longitude()
                        )
                    } else if (pointBottomLeft?.id?.equals(annotation.id) == true) {
                        myBottomLeftP = Point.fromLngLat(
                            pointBottomLeft?.geometry?.longitude()!!,
                            pointBottomLeft?.geometry?.latitude()!!
                        )
                        Log.d("idIcon", "iconTwo")
                    }
                    drawRectangle(mapView!!, myTopRightP!!, myBottomLeftP!!)
                    //Log.d("idIcon",annotation.id)
                    //Log.d("idIcon",annotation.getType().value..lat().)

                }

                override fun onAnnotationDragStarted(annotation: Annotation<*>) {
                }
            })

        }
    }

    val ZOOM = 12.0
    val TOKYO = Point.fromLngLat(139.769305, 35.682027)
    val TILE_REGION_ID = "myTileRegion"
    val STYLE_PACK_STANDARD_SATELLITE_METADATA = "my-standard-satellite-style-pack"
    val STYLE_PACK_STANDARD_METADATA = "my-standard-style-pack"


    /**
     * Download offline region
     */
    fun downloadOfflineRegion() {
        // 1. Create style package with loadStylePack() call.

        // A style pack (a Style offline package) contains the loaded style and its resources: loaded
        // sources, fonts, sprites. Style packs are identified with their style URI.

        // Style packs are stored in the disk cache database, but their resources are not subject to
        // the data eviction algorithm and are not considered when calculating the disk cache size.
        offlineManager.loadStylePack(
            Style.STANDARD_SATELLITE,
            // Build Style pack load options
            StylePackLoadOptions.Builder()
                .glyphsRasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
                .metadata(Value(STYLE_PACK_STANDARD_SATELLITE_METADATA))
                .build(),
            { progress ->

            },
            { expected ->
                expected.value?.let1 { stylePack ->
                    // Style pack download finishes successfully
                    Log.d(TAG, "StylePack downloaded: $stylePack")
                }
                expected.error?.let1 {
                    // Handle error occurred during the style pack download.
                    Log.d(TAG, "StylePackError: $it")
                }
            }
        ).let1 {
            cancelables.add(

                it
            )
        }

        // Download standard style pack
        offlineManager.loadStylePack(
            Style.STANDARD,
            // Build Style pack load options
            StylePackLoadOptions.Builder()
                .glyphsRasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
                .metadata(Value(STYLE_PACK_STANDARD_METADATA))
                .build(),
            { progress ->
                // Update the download progress to UI
                Log.d(TAG, "StylePackStandardLoadProgress: $progress")
            },
            { expected ->
                expected.value?.let1 { stylePack ->
                    // Style pack download finishes successfully
                }
                expected.error?.let1 {
                    // Handle error occurred during the style pack download.
                    Log.d(TAG, "StylePackError: $it")
                }
            }
        ).let1 {
            cancelables.add(
                it
            )
        }

        // The OfflineManager is responsible for creating tileset descriptors for the given style and zoom range.
        val tilesetDescriptors = listOf(
            offlineManager.createTilesetDescriptor(
                TilesetDescriptorOptions.Builder()
                    .styleURI(Style.SATELLITE_STREETS)
                    .pixelRatio(resources.displayMetrics.density)
                    .minZoom(1)
                    .maxZoom(15)
                    .build()
            ),
            offlineManager.createTilesetDescriptor(
                TilesetDescriptorOptions.Builder()
                    .styleURI(Style.STANDARD)
                    .pixelRatio(resources.displayMetrics.density)
                    .minZoom(0)
                    .maxZoom(15)
                    .build()
            )
        )

        // Use the the default TileStore to load this region. You can create custom TileStores that are
        // unique for a particular file path, i.e. there is only ever one TileStore per unique path.

        // Note that the TileStore path must be the same with the TileStore used when initialise the MapView.
        tileStore?.let1 {
            cancelables.add(
                it.loadTileRegion(
                    TILE_REGION_ID,
                    TileRegionLoadOptions.Builder()
                        .geometry(polygonAnnotationOptions?.getGeometry())
                        .descriptors(tilesetDescriptors)
                        .metadata(Value(TILE_REGION_METADATA))
                        .acceptExpired(true)
                        .networkRestriction(NetworkRestriction.NONE)
                        .build(),
                    { progress ->
                        Log.d(TAG, "TileRegionLoadProgress: $progress")

                        runOnUiThread(kotlinx.coroutines.Runnable {
                            tvResults?.text = ""
                            if (isEndNotified) {
                                isEndNotified = false
                                //startProgress()
                                tvResults?.text = "start download"
                            } else {
                                val msg =
                                    progress.completedResourceCount.toString() + "/" + progress.requiredResourceCount.toString()
                                tvResults?.text = msg
                                //Log.d(TAG+"1","process:"+msg)
                            }
                        })

                    }
                ) { expected ->
                    // Tile pack download finishes successfully
                    expected.value?.let1 { region ->
                        Log.d(TAG, "TileRegion downloaded: $region")

                        runOnUiThread(kotlinx.coroutines.Runnable {
                            tvResults?.text = ""
                            tvResults?.text =
                                getString(R.string.tile_download_complete) + region.completedResourceCount
                            //endProgress(getString(R.string.simple_offline_end_progress_success))
                        })

                    }
                    expected.error?.let1 {
                        // Handle error occurred during the tile region download.
                        //logErrorMessage("TileRegionError: $it")
                        Log.d(TAG, "TileRegionError: $it")
                        tvResults?.text = ""
                        tvResults?.text = "TileRegionError: $it"
                    }
                }
            )
        }

    }


    private fun startProgress() {
        tvResults!!.text = ""
        // Start and show the progress bar
        isEndNotified = false
        progressBar!!.isIndeterminate = true
        progressBar!!.visibility = View.VISIBLE
    }

    private fun setPercentage(percentage: Long) {
        progressBar!!.isIndeterminate = false
        progressBar!!.progress = percentage.toInt()
    }

    private fun endProgress(message: String) {
        // Don't notify more than once
        if (isEndNotified) {
            return
        }

        // Stop and hide the progress bar
        isEndNotified = true
        progressBar!!.isIndeterminate = false
        progressBar!!.visibility = View.GONE

        // Show a toast
        Toast.makeText(this@DownloadOfflineTilesActivity, message, Toast.LENGTH_LONG).show()
    } //

}