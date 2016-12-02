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
import java.util.EmptyStackException;
import java.util.List;
import java.util.Stack;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.KeyEvent;
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
    private Stack<String> folderNames;
    private Stack<Integer> folderPos;
    private boolean buttonInList;

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
        folderNames = new Stack<>();
        folderPos = new Stack<>();

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
        
        currentDir = externalStore;
        rootDir = readRootDir();
        //android.R.layout.simple_list_item_1
        final ArrayAdapter<DirInfo> adapter = new MyArrayAdapter(this, R.layout.list_img_item, datalist);
        setListAdapter(adapter);
        setContentView(R.layout.activity_dir);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
        setDataList(currentDir, rootDir);
    }    
    
    private File readRootDir() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        return new File(prefs.getString(Util.key(this, R.string.const_pref_media_dir), "/"));
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	selectDir((DirInfo)getListAdapter().getItem(position));
    }
    
    private void selectDir(DirInfo selectedValue) {
        if (selectedValue.getFile() == null) {
        	saveListScroll();
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
        	saveListScroll();
        	updateDataList(selectedValue.getFile(), rootDir);
        }
    }
    
    private void saveListScroll() {
		this.folderPos.push(getListView().getLastVisiblePosition());
	}
    private int popListScroll() {
    	Integer ret = null;
    	try {
    		ret = this.folderPos.pop();
    	} catch (EmptyStackException e) {}
    	if (ret != null) {
    		return ret;
    	}
    	return 0;
    }

	private boolean isNavigatedDir(DirInfo selectedValue) {
        return FILE_UPDIR_LABEL.equals(selectedValue.getLabel()) || FILE_ROOTDIR_LABEL.equals(selectedValue.getLabel());
    }
    
    private Comparator<File> fileComparator = new FileNameComparator();
    
    private File currentDir;
    
    private void updateDataList(File fdir, File rootDir) {
        setDataList(fdir, rootDir);
        ((MyArrayAdapter)getListAdapter()).notifyDataSetChanged();
    }
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
            	
            	pushDir(fdir, false);
            	
            	if (buttonInList) {
	                if (rootDir != null && rootDir.isDirectory() && rootDir.canRead()) {
	                    datalist.add(new DirInfo(rootDir, FILE_ROOTDIR_LABEL).setBitmap(houseBitmap));
	                }
	                if (parent != null && parent.canRead()) {
	                    datalist.add(new DirInfo(null, FILE_LAST_LABEL).setBitmap(lastBitmap));
	                }
	                if (parent != null && parent.canRead()) {
	                    datalist.add(new DirInfo(parent, FILE_UPDIR_LABEL));
	                }
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
    
    private void pushDir(File fdir, boolean force) {
    	String ldir = null;
    	try {
    		ldir = folderNames.peek();
    	} catch (EmptyStackException e) {}
    	if (ldir != null && !force && ldir.equals(fdir.getAbsolutePath()) ) {} else {
    		folderNames.push(fdir.getAbsolutePath());
    	}
    }
    
    private ArrayList<DirInfo> lastOpened;
    private void setDataListLastBook(File fdir, File rootDir) {
    	pushDir(fdir, true);

    	Collection<BookInfo> books = getMp3Player().getAppSettings().getLastBooks().values();
        datalist.clear();
        
    	if (buttonInList) {
	        if (rootDir != null && rootDir.isDirectory() && rootDir.canRead()) {
	            datalist.add(new DirInfo(rootDir, FILE_ROOTDIR_LABEL).setBitmap(houseBitmap));
	        }
	        if (fdir != null && fdir.canRead() && fdir.isDirectory()) {
	            datalist.add(new DirInfo(fdir, FILE_UPDIR_LABEL));
	        }
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
    
    public static final String SAVED_STATE_KEY_ROOT_DIR = "RootDir";
    public static final String SAVED_STATE_KEY_CUR_DIR = "CurDir";
    public static final String SAVED_STATE_KEY_STACK_DIR = "StackDir";
    public static final String SAVED_STATE_KEY_SCROLL = "ScrollDirPos";
    
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(SAVED_STATE_KEY_ROOT_DIR, rootDir.getAbsolutePath());
        outState.putString(SAVED_STATE_KEY_CUR_DIR, currentDir.getAbsolutePath());
        outState.putStringArrayList(SAVED_STATE_KEY_STACK_DIR, new ArrayList<>(this.folderNames));
    	outState.putIntegerArrayList(SAVED_STATE_KEY_SCROLL, new ArrayList<>(this.folderPos));
        super.onSaveInstanceState(outState);
    }
    protected void onRestoreInstanceState(Bundle state) {
    	String pathRoot = state.getString(SAVED_STATE_KEY_ROOT_DIR);
    	String pathCur = state.getString(SAVED_STATE_KEY_CUR_DIR);
    	ArrayList<String> fldrNames = state.getStringArrayList(SAVED_STATE_KEY_STACK_DIR);
    	ArrayList<Integer> fldrPos = state.getIntegerArrayList(SAVED_STATE_KEY_SCROLL);
    	if (pathRoot != null) {
    		rootDir = new File(pathRoot);
    	}
    	if (pathCur != null) {
    		currentDir = new File(pathCur);
    	}
    	if (fldrNames != null) {
    		folderNames.clear();
    		folderNames.addAll(fldrNames);
    	}
    	if (fldrPos != null) {
    		folderPos.clear();
    		folderPos.addAll(fldrPos);
    	}
    	super.onRestoreInstanceState(state);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
        	if(pressBack()) {
        		return true;
        	}
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private boolean pressBack() {
    	String ldir = null;
    	File fldir = null;
    	int vpos = 0;
    	try {
    		folderNames.pop();
    		while (fldir == null && folderNames.size() > 0) {
    			ldir = folderNames.pop();
    			vpos = popListScroll();
    			if(ldir != null) {
    				fldir = new File(ldir);
    				if (!fldir.canRead()) {
    					fldir = null;
    				}
    			}
    		}
    	} catch (EmptyStackException e) {}
    	if (fldir != null) {
    		updateDataList(fldir, rootDir);
    		getListView().setSelection(vpos);
    		return true;
    	}
    	return false;
    }
    
    public void onClickGoHome(View view) {
    	switch (view.getId()) {
		case R.id.btnCancel:
	        setResult(RESULT_CANCELED, this.getIntent());
	        finish();
			break;
		case R.id.btnBack:
        	if (!pressBack()) {
    	        setResult(RESULT_CANCELED, this.getIntent());
    	        finish();
        	}
			break;
		case R.id.btnPrev:
			File parent = null;
			if (currentDir != null) {
				parent = currentDir.getParentFile();
			}
			if (parent != null) {
				selectDir(new DirInfo(parent, FILE_UPDIR_LABEL));
			} else {
	        	if (!pressBack()) {
	    	        setResult(RESULT_CANCELED, this.getIntent());
	    	        finish();
	        	}
			}
			break;
		case R.id.btnLast:
			selectDir(new DirInfo(null, FILE_LAST_LABEL));
			break;
		case R.id.btnHome:
			selectDir(new DirInfo(rootDir, FILE_ROOTDIR_LABEL));
			break;
		default:
			break;
		}
    }
    
    //TODO stack max size
}
