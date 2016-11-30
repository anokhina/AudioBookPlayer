/*
 * Copyright 2016 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.org.sevn.audiobookplayer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.widget.Toast;

public class Util {
    public static void toast(String text, Context context) {
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();       
    }
    
    public static void chooseFile2selectDir2save(Activity activity, int result_ID) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //i.setType("image/*|application/pdf|audio/*");
        //i.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"image/*", "application/*|text/*"});
        intent.setType("file/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            // TODO string
            activity.startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), result_ID);
        } catch (android.content.ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public static String getFilename(Uri uri, Context context) { 
        String fileName = null;
        String scheme = uri.getScheme();
        if (scheme.equals("file")) {
            fileName = uri.getLastPathSegment();
        }
        else if (scheme.equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
              if (cursor != null && cursor.moveToFirst()) {
                  fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
              }
            } finally {
              cursor.close();
            }           
        }
        return fileName;
    }

    public static String getShortPlayingMessage(MediaPlayerProxy mediaPLayer, AudioManager audioManager) {
        return getPlayingMessage(mediaPLayer, audioManager, !false);
    }
    
    public static String getPlayingMessage(MediaPlayerProxy mediaPLayer, AudioManager audioManager) {
        return getPlayingMessage(mediaPLayer, audioManager, false);
    }
    
    public static String getPlayingMessage(MediaPlayerProxy mediaPLayer, AudioManager audioManager, boolean isshort) {
        // TODO string
    	
        String msg = "";
        if (mediaPLayer != null) {
            if (!isshort && mediaPLayer.isPlaying()) {
                msg += "PLAYING ";
            }
            msg += ("Time: " + getTimeString(mediaPLayer.getCurrentPosition()) + " / "
                    + getTimeString(mediaPLayer.getDuration()) +  " ");
        }
        if (!isshort && audioManager != null) {
            msg += ("Volume: " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        }
        if (!isshort) {
        	msg += ("" + mediaPLayer.getDataSource());
        }
        return msg;
    }
    
    private static String getTimeString(int ms) {
        int s = ms / 1000; 
        int h = s / 60 / 60;
        int m = (s - h * 60 * 60) / 60 ;
        s -= (h * 60 * 60 + m * 60);
        return "" + getTimeUnitString(h) + ":" + getTimeUnitString(m) + ":" + getTimeUnitString(s);
    }
    
    private static String getTimeUnitString(int ms) {
        if (ms < 10) {
            return "0" + ms;
        }
        return "" + ms;
    }
    
    public static float getImageFactor(Resources r){
        DisplayMetrics metrics = r.getDisplayMetrics();
        float multiplier=metrics.density;
        return multiplier;
    }
    
    public static Bitmap getScaledImage(Bitmap image, int w, int h, float multiplier){
        int newW = (int)(w*multiplier);
        int newH = (int)(h*multiplier);
        if (image.getWidth() > newW || image.getHeight() > newH) {
            return Bitmap.createScaledBitmap(image, newW, newH, false);
        }
        return image;
    }
    public static String key(Context ctx, int id) {
        return ctx.getResources().getString(id);
    }
    
    public static byte[] fromZip(File fl, String name) throws IOException {
        byte[] ret = null;
        FileInputStream fis = new FileInputStream(fl); 
        ZipInputStream zis = new ZipInputStream(fis); 
        try {
	        ZipEntry ze = null;
	        while ((ze = zis.getNextEntry()) != null) { 
	        	if (!ze.isDirectory() && ze.getName().endsWith(name)) {
	        		//BitmapFactory.decodeStream(zis)
	                byte[] buffer = new byte[2048];
	                ArrayList<byte[]> lst = new ArrayList<byte[]>(); //TODO
	                int len;
	                int dataLen = 0;
	                while ((len = zis.read(buffer)) > 0) {
	                	dataLen += len;
	                    lst.add(buffer);
	                    if (len < buffer.length) {
	                    	ret = new byte[dataLen];
	                    	int retI = 0;
	                    	for (byte[] buf : lst) {
	                    		for (int j = 0; j < buf.length && retI < ret.length; j++, retI++ ) {
	                    			ret[retI] = buf[j];
	                    		}
	                    	}
	                    	break;
	                    }
	                    buffer = new byte[2048];
	                }        		
	        	}
	        }
        } finally {
			zis.close();
		}
        return ret;
    }
    
    public static String printExceptionStackTrace(Throwable tr) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        
        tr.printStackTrace(ps);
        return new String(baos.toByteArray(), Charset.forName("UTF-8"));
    }
    
    public static Bitmap makeBitmap(final int number) {
        Bitmap bitmap = Bitmap.createBitmap(32, 16, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        paint.setColor(0xFF808080); // gray
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(12);
        new Canvas(bitmap).drawText(""+number, 14, 14, paint);        
        return bitmap;
    }
    
}
