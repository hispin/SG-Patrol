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
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolygonOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
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


class DownloadOfflineTilesActivity1 : ParentActivity() {


    // JSON encoding/decoding

    private var mPolygonAnnotationManager: PolygonAnnotationManager? = null
    val JSON_CHARSET: String = "UTF-8"

    val JSON_FIELD_REGION_NAME: String = "FIELD_REGION_NAME"

    val ID_ICON_LOCATION: String = "location"

    val TAG: String = "SimpleOfflineMap"
    val locationOne: LatLng = LatLng(32.173001, 34.842284)
    val locationTwo: LatLng = LatLng(32.067477, 34.801851)
    var boundsArea: PolygonOptions? = null
    var sum: Int = 0
    private val isEndNotified = false
    private val progressBar: ProgressBar? = null
    private var mapView: MapView? = null
    private val offlineManager: OfflineManager? = null
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline_download_tiles1)
        mapView = findViewById(R.id.mapView)


        //mapView.onCreate(savedInstanceState);
        tvResults = findViewById(R.id.tvResults)

        btnDownload = findViewById(R.id.btnDownload)
        btnDownload?.setOnClickListener(View.OnClickListener {
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
        val polygonAnnotationOptions: PolygonAnnotationOptions = PolygonAnnotationOptions()
            .withPoints(points) // Style the polygon that will be added to the map.
            .withFillColor(Color.RED)
            .withFillOpacity(0.4)

        // Add the resulting polygon to the map.
        mPolygonAnnotationManager?.create(polygonAnnotationOptions)
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
    private fun addMarkerIconsToMap() {//@NonNull  loadedMapStyle:Style) {


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

//        loadedMapStyle.addImage("icon-id", BitmapUtils.getBitmapFromDrawable(
//            ResourcesCompat.getDrawable(getResources(), R.drawable.mapbox_marker_icon_default, null)));
//
//
//        loadedMapStyle.addSource(new GeoJsonSource("source-id"));
//
//        SymbolManager symbolManager1 = new SymbolManager(mapView, myMapboxMap, loadedMapStyle);
//
//        symbolManager1.setIconAllowOverlap(true);
//        symbolManager1.setIconIgnorePlacement(true);
//
//        // Add symbol at top right
//        symbolManager1.create(new SymbolOptions()
//            .withLatLng(myTopRight)//new LatLng(new LatLng(32.173001, 34.842284)))//32.941484, 35.795603)))
//            .withIconImage("icon-id")
//            .withDraggable(true)
//            .withIconSize(1.5f));
//        symbolManager1.addDragListener(new OnSymbolDragListener() {
//            @Override
//            public void onAnnotationDragStarted(Symbol annotation) {
//                Log.d(TAG, "topright onAnnotationDragStarted");
//            }
//
//            @Override
//            public void onAnnotationDrag(Symbol annotation) {
//                Log.d(TAG, "topright onAnnotationDrag");
//            }
//
//            @Override
//            public void onAnnotationDragFinished(Symbol annotation) {
//                Log.d(TAG, "topright onAnnotationDragFinished");
//                myTopRight = annotation.getLatLng();
//                drawRectangle();
//            }
//        });
//
//
//        SymbolManager symbolManager2 = new SymbolManager(mapView, myMapboxMap, loadedMapStyle);
//
//        symbolManager2.setIconAllowOverlap(true);
//        symbolManager2.setIconIgnorePlacement(true);
//        // Add symbol at bottom left
//        symbolManager2.create(new SymbolOptions()
//            .withLatLng(myBottomLeft)//new LatLng(new LatLng(32.173001, 34.842284)))//32.941484, 35.795603)))
//            .withIconImage("icon-id")
//            .withDraggable(true)
//            .withIconSize(1.5f));
//        symbolManager2.addDragListener(new OnSymbolDragListener() {
//            @Override
//            public void onAnnotationDragStarted(Symbol annotation) {
//                Log.d(TAG, "bottomLeft onAnnotationDragStarted");
//            }
//
//            @Override
//            public void onAnnotationDrag(Symbol annotation) {
//                Log.d(TAG, "bottomLeft onAnnotationDrag");
//            }
//
//            @Override
//            public void onAnnotationDragFinished(Symbol annotation) {
//                Log.d(TAG, "bottomLeft onAnnotationDragFinished");
//                myBottomLeft = annotation.getLatLng();
//                drawRectangle();
//            }
//        });

    }


}