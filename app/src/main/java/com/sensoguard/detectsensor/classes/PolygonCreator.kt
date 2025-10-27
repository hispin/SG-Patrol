package com.sensoguard.detectsensor.classes

import android.graphics.Color
import com.google.android.gms.maps.model.LatLng
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolygonAnnotationManager
import com.mapbox.maps.viewannotation.ViewAnnotationManager

class PolygonCreator {


    //annotations (markers)
    private var viewAnnotationManager: ViewAnnotationManager? = null//mapView?.viewAnnotationManager
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var annotationApi: AnnotationPlugin? = null
    //////////////

    companion object {
        val instance = PolygonCreator()
    }

    fun drawRectangle(
        mapView: MapView,
        myTopRightP: Point,
        myBottomLeftP: Point
    ) {

        val topLeft = LatLng(myBottomLeftP.latitude(), myTopRightP.longitude())
        val bottomRight = LatLng(myTopRightP.latitude(), myBottomLeftP.longitude())


        // Create an instance of the Annotation API and get the polygon manager.
        val annotationApi = mapView.annotations
        val mPolygonAnnotationManager = annotationApi.createPolygonAnnotationManager()

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
        mPolygonAnnotationManager.create(polygonAnnotationOptions)
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
}