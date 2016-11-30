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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class DirActivity extends ListActivity {
    public static String RET_PARAM = "ret_param";
    public static String IN_PARAM = "in_param";
    public static String FILE_NAME_COVER = "Info/Image/Cover.jpg";
    public static String FILE_NAME_BOOK = "Book";
    public static String FILE_NAME_ICON = ".icon";
    public static String[] FILE_NAME_ICON_EXT = {"", ".png", ".jpg", ".gif"};
    public static String FILE_UPDIR_LABEL = "..";
    public static String FILE_ROOTDIR_LABEL = "/";
    public static String FILE_LAST_LABEL = "[LAST]";
    
    private Bitmap houseBitmap;
    private Bitmap lastBitmap;

    static class MyArrayAdapter extends ArrayAdapter<DirInfo> {
        private int itemLayout;

        public MyArrayAdapter(Context context, int resource, final List<DirInfo> objects) {
            super(context, resource, objects);
            this.itemLayout = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView = inflater.inflate(itemLayout, parent, false);
            TextView textView = (TextView) rowView.findViewById(R.id.label);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.logo);
            textView.setText(getListItem(position).getLabel());
            
            if (getListItem(position).getBitmap() != null) {
                imageView.setImageBitmap(getListItem(position).getBitmap());
            }
            return rowView;
        }
        
        private DirInfo getListItem(int position) {
            return getItem(position);
        }
    }    
    
    private final List<DirInfo> datalist = new ArrayList<DirInfo>();
    private File rootDir;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        houseBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.house);
        lastBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.recent);
        lastOpened = new ArrayList<>();

        File externalStore = Environment.getExternalStorageDirectory();
        Bundle extra = getIntent().getExtras();
        if (extra != null){
            String inParam = extra.getString(IN_PARAM);
            if (inParam != null) {
            	File fl = new File(inParam);
            	if (fl.exists() && fl.canRead()) {
            		externalStore = fl;
            	}
            }
        }
        
        rootDir = readRootDir();
        setDataList(externalStore, rootDir);
        //android.R.layout.simple_list_item_1
        final ArrayAdapter<DirInfo> adapter = new MyArrayAdapter(this, R.layout.list_img_item, datalist);
        setListAdapter(adapter);
    }
    
    private File readRootDir() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        return new File(prefs.getString(Util.key(this, R.string.const_pref_media_dir), "/"));
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        DirInfo selectedValue = (DirInfo)getListAdapter().getItem(position);
        if (selectedValue.getFile() == null) {
        	setDataListLastBook(currentDir, rootDir);
            ((MyArrayAdapter)getListAdapter()).notifyDataSetChanged();
        } else
        if (selectedValue.getBookDir() != null && !isNavigatedDir(selectedValue)) {
    		if (selectedValue.getBookDir().exists()) {
	        	Uri ret = null;
	        	if (selectedValue.getBookInfo() != null) {
	        		String filePath = selectedValue.getBookInfo().getPath();
	        		if (filePath != null) {
	        			ret = Uri.fromFile(new File(filePath));
	        		}
	        	} else {
	    			ret = Uri.fromFile(selectedValue.getBookDir());
	        	}
	        	
	        	if (ret != null) {
		            Intent intent = this.getIntent();
		            intent.setData(ret);
		            intent.putExtra(RET_PARAM, ret);
		            setResult(RESULT_OK, intent);
		            finish();
	        	}
    		}
        } else {
            setDataList(selectedValue.getFile(), rootDir);
            ((MyArrayAdapter)getListAdapter()).notifyDataSetChanged();
        }
    }
    
    private boolean isNavigatedDir(DirInfo selectedValue) {
        return FILE_UPDIR_LABEL.equals(selectedValue.getLabel()) || FILE_ROOTDIR_LABEL.equals(selectedValue.getLabel());
    }
    
    private Comparator<File> fileComparator = new FileNameComparator();
    
    private File currentDir;
    private void setDataList(File fdir, File rootDir) {
    	currentDir = fdir;
        datalist.clear();
        if (fdir != null && fdir.exists() && fdir.isDirectory() && fdir.canRead()) {
            File parent = fdir.getParentFile();
            DirInfo fdirInfo = null;
            
            if (parent != null) {
                fdirInfo = new DirInfo(parent);
            }
            if (fdirInfo != null && fdirInfo.getBookDir() != null) {
                setDataList(parent.getParentFile(), rootDir);
            } else {
                if (rootDir != null && rootDir.isDirectory() && rootDir.canRead()) {
                    datalist.add(new DirInfo(rootDir, FILE_ROOTDIR_LABEL).setBitmap(houseBitmap));
                }
                if (parent != null && parent.canRead()) {
                    datalist.add(new DirInfo(null, FILE_LAST_LABEL).setBitmap(lastBitmap));
                }
                if (parent != null && parent.canRead()) {
                    datalist.add(new DirInfo(parent, FILE_UPDIR_LABEL));
                }
                
                File[] fileList = fdir.listFiles();
                if (fileList != null) {
	                Arrays.sort(fileList, fileComparator);
	                for(File fl : fileList) {
	                    if (fl.isDirectory()) {
	                        datalist.add(new DirInfo(fl));
	                    }
	                }
                }
            }
        }        
    }
    
    private ArrayList<DirInfo> lastOpened;
    private void setDataListLastBook(File fdir, File rootDir) {
    	Collection<BookInfo> books = getMp3Player().getAppSettings().getLastBooks().values();
        datalist.clear();
        if (rootDir != null && rootDir.isDirectory() && rootDir.canRead()) {
            datalist.add(new DirInfo(rootDir, FILE_ROOTDIR_LABEL).setBitmap(houseBitmap));
        }
        if (fdir != null && fdir.canRead() && fdir.isDirectory()) {
            datalist.add(new DirInfo(fdir, FILE_UPDIR_LABEL));
        }
        if (lastOpened.size() == 0) {
            for(BookInfo bi : books) {
            	lastOpened.add(0, bi.getDirInfo());
            }
        	
        }
        datalist.addAll(lastOpened);
    }
    private Mp3Player getMp3Player() {
        return AppService.mp3Player;
    }
}
