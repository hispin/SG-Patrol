package com.sensoguard.detectsensor.classes;

import android.content.Context;
import android.content.res.AssetManager;

import androidx.appcompat.widget.AppCompatImageButton;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CustomMapTileProvider implements TileProvider {
    private static final int TILE_WIDTH = 256;
    private static final int TILE_HEIGHT = 256;
    private static final int BUFFER_SIZE = 16 * 1024;
    AppCompatImageButton ivMap;
    Context context;
    private AssetManager mAssets = null;

    public CustomMapTileProvider(AppCompatImageButton ivMap, Context context) {
        this.ivMap = ivMap;
        this.context = context;
    }

    public CustomMapTileProvider(AssetManager assets) {
        mAssets = assets;
    }

    @Override
    public Tile getTile(int x, int y, int zoom) {
        String path = "http://mt1.google.com/vt/lyrs=y&x=" + x + "&y=" + y + "&z=" + zoom;
        Glide.with(context).load(path).into(ivMap);

        //byte[] image = readTileImage(x, y, zoom);
        //return image == null ? null : new Tile(TILE_WIDTH, TILE_HEIGHT, image);
        return null;
    }

    private byte[] readTileImage(int x, int y, int zoom) {
        InputStream in = null;
        ByteArrayOutputStream buffer = null;

        try {
            in = mAssets.open(getTileFilename(x, y, zoom));
            buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[BUFFER_SIZE];

            while ((nRead = in.read(data, 0, BUFFER_SIZE)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();

            return buffer.toByteArray();
        } catch (IOException | OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        } finally {
            if (in != null) try {
                in.close();
            } catch (Exception ignored) {
            }
            if (buffer != null) try {
                buffer.close();
            } catch (Exception ignored) {
            }
        }
    }

    private String getTileFilename(int x, int y, int zoom) {
        return "map/" + zoom + '/' + x + '/' + y + ".png";
    }
}
