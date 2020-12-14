package com.sensoguard.detectsensor.activities;

import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolDragListener;
import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;
import com.sensoguard.detectsensor.R;
import com.sensoguard.detectsensor.global.SysMethodsSharedPrefKt;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

//import com.mapbox.mapboxandroiddemo.R;
//import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolDragListener;
//import com.mapbox.mapboxsdk.plugins.annotation.Symbol;
//import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager;
//import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions;

//import timber.log.Timber;
public class DownloadOfflineTilesActivity extends AppCompatActivity implements MapboxMap.OnMapClickListener {

    // JSON encoding/decoding
    public static final String JSON_CHARSET = "UTF-8";
    public static final String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";
    public static final String ID_ICON_LOCATION = "location";
    private static final String TAG = "SimpleOfflineMap";
    private static final LatLng locationOne = new LatLng(32.173001, 34.842284);
    private static final LatLng locationTwo = new LatLng(32.067477, 34.801851);
    PolygonOptions boundsArea;
    int sum = 0;
    private boolean isEndNotified;
    private ProgressBar progressBar;
    private MapView mapView;
    private OfflineManager offlineManager;
    private AppCompatTextView tvResults;
    private MapboxMap myMapboxMap;
    private Style myStyle;
    private AppCompatButton btnDownload;
    private AppCompatButton btnDelete;
    private LatLng myTopRight;
    private LatLng myBottomLeft;
    private Iterator<LatLng> polyRegions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

// Mapbox access token is configured here. This needs to be called either in your application
// object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

// This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_dowmload_offline_tiles);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        tvResults = findViewById(R.id.tvResults);

        btnDownload = findViewById(R.id.btnDownload);
        btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downLoadOfflineMap();
            }
        });

        btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteTile();
            }
        });

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull final MapboxMap mapboxMap) {

                myMapboxMap = mapboxMap;

                mapboxMap.setStyle(Style.SATELLITE_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {

                        myStyle = style;
                        mapboxMap.animateCamera(
                                CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                                        .zoom(3)
                                        .build()), 2000);


                        int viewportWidth = mapView.getWidth();
                        int viewportHeight = mapView.getHeight();
                        myTopRight = myMapboxMap.getProjection().fromScreenLocation(new PointF(viewportWidth - 200, 200));
                        myBottomLeft = myMapboxMap.getProjection().fromScreenLocation(new PointF(200, viewportHeight - 200));


                        addMarkerIconsToMap(style);
                        drawRectangle();
                    }
                });
            }
        });
    }

    //draw rectangle for selecting offline region
    private void drawRectangle() {
        if (boundsArea != null && boundsArea.getPolygon() != null) {
            myMapboxMap.removePolygon(boundsArea.getPolygon());
        }

        LatLng topLeft = new LatLng(myBottomLeft.getLatitude(), myTopRight.getLongitude());
        LatLng bottomRight = new LatLng(myTopRight.getLatitude(), myBottomLeft.getLongitude());


        boundsArea = new PolygonOptions()
                .add(topLeft)
                .add(myTopRight)
                .add(bottomRight)
                .add(myBottomLeft);
        boundsArea.alpha(0.25f);
        boundsArea.fillColor(Color.RED);
        myMapboxMap.addPolygon(boundsArea);

        //update for downloading
        polyRegions = boundsArea.getPoints().iterator();

    }
//    private void getTotalOfflineTiles() {
//        // Set up the OfflineManager
//        offlineManager = OfflineManager.getInstance(DownloadOfflineTilesActivity.this);
//
//        if (offlineManager != null) {
//            offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
//
//                @Override
//                public void onList(OfflineRegion[] offlineRegions) {
//
//
//                    for (int i = 0; i < offlineRegions.length; i++) {
//                        offlineRegions[i].getStatus(new OfflineRegion.OfflineRegionStatusCallback() {
//                            @Override
//                            public void onStatus(OfflineRegionStatus status) {
//                                sum+=status.getCompletedTileSize();
//                                btnTotalTiles.setText("total tiles downloaded is "+sum);
//                            }
//
//                            @Override
//                            public void onError(String error) {
//
//                            }
//                        });
//                    }
//                }
//
//                @Override
//                public void onError(String error) {
//
//                }
//            });
//        }
//
//    }

    // show the last offline region
//    private void showOfflineMap() {
//
//        // Set up the OfflineManager
//        offlineManager = OfflineManager.getInstance(DownloadOfflineTilesActivity.this);
//
//        if (offlineManager != null) {
//            offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
//                @Override
//                public void onList(OfflineRegion[] offlineRegions) {
//
//                    Log.d(TAG,offlineRegions.length+"");
//
//
//                    if(offlineRegions.length<1){
//                        return;
//                    }
//
//                    tvResults.setText(offlineRegions.length+"");
//
//                    // Get the region bounds and zoom and move the camera.
//                    LatLngBounds bounds = ((OfflineTilePyramidRegionDefinition)
//                            offlineRegions[0].getDefinition()).getBounds();
//                    double regionZoom = ((OfflineTilePyramidRegionDefinition)
//                            offlineRegions[0].getDefinition()).getMinZoom();
//
//
//
////                    LatLng latLng1=bounds.getNorthWest();
////                    LatLng latLng2=bounds.getNorthEast();
//
//// Create new camera position
//                    CameraPosition cameraPosition = new CameraPosition.Builder()
//                            .target(bounds.getCenter())
//                            .zoom(regionZoom)
//                            .build();
//
//
//                    // Move camera to new position
//                    myMapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//
//                }
//
//                @Override
//                public void onError(String error) {
//                     Log.d("","");
//                }
//
//            });
//                                              }
//
//    }

    //downLoad regine
    private void downLoadOfflineMap() {

        tvResults.setText("");

        int minZoom = 10;
        int maxZoom = 15;

        // Set up the OfflineManager
        offlineManager = OfflineManager.getInstance(DownloadOfflineTilesActivity.this);
        offlineManager.setOfflineMapboxTileCountLimit(10000);

        ArrayList<LatLng> locations = new ArrayList<>();
        while (polyRegions != null && polyRegions.hasNext()) {
            locations.add(polyRegions.next());
        }

        if (locations.size() < 4) {
            Toast.makeText(this, getResources().getString(R.string.no_selected_location), Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a bounding box for the offline region
        LatLngBounds latLngBounds = new LatLngBounds.Builder()
                .include(locations.get(0))
                .include(locations.get(1))
                .include(locations.get(2))
                .include(locations.get(3))
                .build();

        // Define the offline region
        OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                myStyle.getUri(),
                latLngBounds,
                minZoom,
                maxZoom,
                DownloadOfflineTilesActivity.this.getResources().getDisplayMetrics().density);

        // Set the metadata
        byte[] metadata;
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_FIELD_REGION_NAME, "Erzelia");
            String json = jsonObject.toString();
            metadata = json.getBytes(JSON_CHARSET);
        } catch (Exception exception) {
            Log.e(TAG, "Failed to encode metadata:" + exception.getMessage());
            metadata = null;
        }

        // Create the region asynchronously
        if (metadata != null) {
            offlineManager.createOfflineRegion(
                    definition,
                    metadata,
                    new OfflineManager.CreateOfflineRegionCallback() {
                        @Override
                        public void onCreate(OfflineRegion offlineRegion) {

                            offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);

                            // Display the download progress bar
                            progressBar = findViewById(R.id.progress_bar);
                            startProgress();

                            // Monitor the download progress using setObserver
                            offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {
                                @Override
                                public void onStatusChanged(OfflineRegionStatus status) {

                                    // Calculate the download percentage and update the progress bar
                                    double percentage = status.getRequiredResourceCount() >= 0
                                            ? (100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) :
                                            0.0;

                                    if (status.isComplete()) {

                                        long total = SysMethodsSharedPrefKt.getLongInPreference
                                                (DownloadOfflineTilesActivity.this, "TOTAL_TILES", 0);
                                        if (total != status.getCompletedTileCount()) {
                                            total += status.getCompletedTileCount();
                                        }
                                        //tvResults.setText(total+"");
                                        SysMethodsSharedPrefKt.setLongInPreference(DownloadOfflineTilesActivity.this, "TOTAL_TILES", total);

                                        // Download complete
                                        //Log.d(TAG,"download has benn completed");
                                        Log.d(TAG, "download success size of tiles:" + status.getCompletedTileCount());
                                        endProgress(getString(R.string.simple_offline_end_progress_success));
                                        tvResults.setText("download success size of tiles:" + status.getCompletedTileCount() + " total tiles =" + total);
                                    } else if (status.isRequiredResourceCountPrecise()) {
                                        // Switch to determinate state
                                        setPercentage((int) Math.round(percentage));
                                    }
                                }

                                @Override
                                public void onError(OfflineRegionError error) {
                                    // If an error occurs, print to logcat
                                    Log.e(TAG, "onError reason:" + error.getReason());
                                    endProgress("onError reason:" + error.getReason());
                                    tvResults.setText("onError reason:" + error.getReason());

                                }

                                @Override
                                public void mapboxTileCountLimitExceeded(long limit) {
                                    // Notify if offline region exceeds maximum tile count
                                    Log.e(TAG, "Mapbox tile count limit exceeded:" + limit);
                                    tvResults.setText("Mapbox tile count limit exceeded:" + limit);

                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error:" + error);
                            tvResults.setText("Error:" + error);
                        }
                    });
        }

    }

    private void deleteTile() {

        SysMethodsSharedPrefKt.setLongInPreference(DownloadOfflineTilesActivity.this, "TOTAL_TILES", 0);
        // Set up the OfflineManager
        offlineManager = OfflineManager.getInstance(DownloadOfflineTilesActivity.this);
        if (offlineManager != null) {
            offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {
                @Override
                public void onList(OfflineRegion[] offlineRegions) {


                    if (offlineRegions.length < 1) {
                        Toast.makeText(DownloadOfflineTilesActivity.this, "no Region", Toast.LENGTH_LONG).show();
                        return;
                    }

                    offlineRegions[offlineRegions.length - 1].delete(new OfflineRegion.OfflineRegionDeleteCallback() {
                        @Override
                        public void onDelete() {
                            // Once the region is deleted, remove the
                            // progressBar and display a toast
                            //progressBar.setVisibility(View.INVISIBLE);
                            //progressBar.setIndeterminate(false);
                            Toast.makeText(DownloadOfflineTilesActivity.this, "Region deleted", Toast.LENGTH_SHORT).show();

                        }

                        @Override
                        public void onError(String error) {
                            //progressBar.setVisibility(View.INVISIBLE);
                            //progressBar.setIndeterminate(false);
                            Log.e(TAG, "Error: " + error);
                            Toast.makeText(DownloadOfflineTilesActivity.this, "Error", Toast.LENGTH_LONG).show();
                        }
                    });


                }

                @Override
                public void onError(String error) {
                    Toast.makeText(DownloadOfflineTilesActivity.this, "Error", Toast.LENGTH_LONG).show();
                    Log.d("", "");
                }
            });
        }


    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    // Progress bar methods
    private void startProgress() {

// Start and show the progress bar
        isEndNotified = false;
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void setPercentage(final int percentage) {
        progressBar.setIndeterminate(false);
        progressBar.setProgress(percentage);
    }

    private void endProgress(final String message) {
        // Don't notify more than once
        if (isEndNotified) {
            return;
        }

        // Stop and hide the progress bar
        isEndNotified = true;
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.GONE);

        // Show a toast
        Toast.makeText(DownloadOfflineTilesActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private void addMarkerIconsToMap(@NonNull Style loadedMapStyle) {

        loadedMapStyle.addImage("icon-id", BitmapUtils.getBitmapFromDrawable(
                ResourcesCompat.getDrawable(getResources(), R.drawable.mapbox_marker_icon_default, null)));


        loadedMapStyle.addSource(new GeoJsonSource("source-id"));

        SymbolManager symbolManager1 = new SymbolManager(mapView, myMapboxMap, loadedMapStyle);

        symbolManager1.setIconAllowOverlap(true);
        symbolManager1.setIconIgnorePlacement(true);

        // Add symbol at top right
        symbolManager1.create(new SymbolOptions()
                .withLatLng(myTopRight)//new LatLng(new LatLng(32.173001, 34.842284)))//32.941484, 35.795603)))
                .withIconImage("icon-id")
                .withDraggable(true)
                .withIconSize(2.0f));
        symbolManager1.addDragListener(new OnSymbolDragListener() {
            @Override
            public void onAnnotationDragStarted(Symbol annotation) {
                Log.d(TAG, "topright onAnnotationDragStarted");
            }

            @Override
            public void onAnnotationDrag(Symbol annotation) {
                Log.d(TAG, "topright onAnnotationDrag");
            }

            @Override
            public void onAnnotationDragFinished(Symbol annotation) {
                Log.d(TAG, "topright onAnnotationDragFinished");
                myTopRight = annotation.getLatLng();
                drawRectangle();
            }
        });


        SymbolManager symbolManager2 = new SymbolManager(mapView, myMapboxMap, loadedMapStyle);

        symbolManager2.setIconAllowOverlap(true);
        symbolManager2.setIconIgnorePlacement(true);
        // Add symbol at bottom left
        symbolManager2.create(new SymbolOptions()
                .withLatLng(myBottomLeft)//new LatLng(new LatLng(32.173001, 34.842284)))//32.941484, 35.795603)))
                .withIconImage("icon-id")
                .withDraggable(true)
                .withIconSize(2.0f));
        symbolManager2.addDragListener(new OnSymbolDragListener() {
            @Override
            public void onAnnotationDragStarted(Symbol annotation) {
                Log.d(TAG, "bottomLeft onAnnotationDragStarted");
            }

            @Override
            public void onAnnotationDrag(Symbol annotation) {
                Log.d(TAG, "bottomLeft onAnnotationDrag");
            }

            @Override
            public void onAnnotationDragFinished(Symbol annotation) {
                Log.d(TAG, "bottomLeft onAnnotationDragFinished");
                myBottomLeft = annotation.getLatLng();
                drawRectangle();
            }
        });

    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        return false;
    }

}