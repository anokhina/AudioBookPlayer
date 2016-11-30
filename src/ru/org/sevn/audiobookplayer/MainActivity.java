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

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;
import ru.org.sevn.audiobookplayer.MediaPlayerProxy.CantPlayException;
import ru.org.sevn.audiobookplayer.MediaPlayerProxy.ChangeStateEvent;
import ru.org.sevn.audiobookplayer.MediaPlayerProxy.ChangeStateListener;
import ru.org.sevn.audiobookplayer.MediaPlayerProxy.STATE;

//http://www.mpgedit.org/mpgedit/mpeg_format/mpeghdr.htm
// see https://github.com/yohpapa/simplemusicplayer/blob/master/src/com/yohpapa/research/simplemusicplayer/PlaybackService.java

public class MainActivity extends Activity implements ChangeStateListener {
    
    public static final String PREF_NAME = "MainActivity";

    private AudioManager audioManager;
    
    private ToggleButton chbLoop;
    
    @Override
    protected void onDestroy() {
        AppService.mainActivity = null;
        super.onDestroy();
    }
    
    private Mp3Player getMp3Player() {
        return AppService.mp3Player;
    }
    
    public static final int CHOOSE_FILE_TO_SAVE = 1;
    public static final int CHOOSE_DIR_TO_OPEN = 1;
    public static final String SAVED_STATE_KEY = "ExtraData";
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri = null;
        if (requestCode == CHOOSE_FILE_TO_SAVE) {
            if (resultCode == RESULT_OK) {
                uri = data.getData();
            }
        } else if (requestCode == CHOOSE_DIR_TO_OPEN) {
            if (resultCode == RESULT_OK) {
                Object retParam = data.getExtras().get(DirActivity.RET_PARAM);
                if (retParam instanceof Uri) {
                    uri = (Uri)retParam;
                } else {
                	// todo restore
                }
            }
        }
        if (uri != null) {
            startService(new Intent(this, AppService.class).setAction(AppService.ACTION_SET_FILE).putExtra(AppService.PARAM_URI, uri));
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    
    
    public Activity getActivity() {
        return this;
    }
    
    public void onClickOpen(View view) {
        //Util.chooseFile2selectDir2save(getActivity(), CHOOSE_FILE_TO_SAVE);
        Intent i = new Intent(this, DirActivity.class);
        if (getMp3Player() != null && getMp3Player().getAppSettings() != null && 
                getMp3Player().getAppSettings().getPlayingDirPath() != null) {
            
            i.putExtra(DirActivity.IN_PARAM, getMp3Player().getAppSettings().getPlayingDirPath());
        }
        
        startActivityForResult(i, CHOOSE_DIR_TO_OPEN);
    }
    
    public void onClick(View view) {
        if (getMp3Player().getMediaPlayer() != null) {
            switch (view.getId()) {
//                case R.id.actvTitle:
//                    nextTitleEncoding();
//                    break;
                case R.id.btnStartSD:
                    startService(new Intent(this, AppService.class).setAction(AppService.ACTION_PLAY_PAUSE));
                    break;
                case R.id.btnStop:
                    startService(new Intent(this, AppService.class).setAction(AppService.ACTION_STOP));
                    break;
                case R.id.btnPrev:
                    startService(new Intent(this, AppService.class).setAction(AppService.ACTION_PREV));
                    break;
                case R.id.btnNext:
                    startService(new Intent(this, AppService.class).setAction(AppService.ACTION_NEXT));
                    break;
                case R.id.btnBackward:
                    startService(new Intent(this, AppService.class).setAction(AppService.ACTION_BWARD));
                    break;
                case R.id.btnForward:
                    startService(new Intent(this, AppService.class).setAction(AppService.ACTION_FWARD));
                    break;
                case R.id.btnFirstRecord:
                    startService(new Intent(this, AppService.class).setAction(AppService.ACTION_FIRST));
                    break;
            }
        }
    }
    
    public static final String ENC_CP = "windows-1251";
    public static final String ENC_koi = "koi8-r";
    public static final String ENC_UTF = "UTF-8";
    
    private void showMediaInfo() {
        showLoopState();
        showTrackBar();
        showTrackSBar();
        showTitle();
        showImage();
        showInfo();
    }
    private void showLoopState() {
        if (chbLoop != null && getMp3Player() != null && getMp3Player().getAppSettings() != null) {
            //    T F
            // T  F T
            // F  T F
            boolean checked = chbLoop.isChecked();
            boolean checkedApp = getMp3Player().getAppSettings().isLoop();
            if (checked^checkedApp) {
                chbLoop.setChecked(checkedApp);
            }
        }
    }
    private void showTrackSBar() {
        if (pbarFiles != null && getMp3Player() != null) {
            int m = getMp3Player().getFilesLength();
            if (m > 0) { m--; }
            if (m != pbarFiles.getMax()) {
                pbarFiles.setMax(m);
            }
            pbarFiles.setProgress(getMp3Player().getPlayingFileIdx());
        }
    }
    private void showTrackBar() {
        if (pbar != null && getMp3Player() != null && getMp3Player().getMediaPlayer() != null) {
            int m = getMp3Player().getMediaPlayer().getDuration();
            if (m != pbar.getMax()) {
                pbar.setMax(m);
            }
            pbar.setProgress(getMp3Player().getMediaPlayer().getCurrentPosition());
        }
    }
    private void showImage() {
        showImage(getMp3Player().getAppSettings().getBitmapImage());
    }
    private void showImage(Bitmap bitmap) {
        ImageView imageView = (ImageView)findViewById(R.id.imageView1);
        if (imageView != null) {
            if(bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.picture);
            }                        
        }        
    }
    
    private void nextTitleEncoding() {
        String enc = ENC_UTF;
        switch(getMp3Player().getAppSettings().getCharsetName()) {
            case ENC_UTF:
                enc = ENC_CP;
                break;
            case ENC_CP:
                enc = ENC_koi;
                break;
            case ENC_koi:
                enc = ENC_UTF;
                break;
        }
        setTitleEncoding(enc);
    }
    private void setTitleEncoding(String enc) {
        getMp3Player().getAppSettings().setCharsetName(enc);
    }
    
    private void showTitle() {
        TextView tv = (TextView)findViewById(R.id.tvTitle);
        if (tv != null) {
            String title = getMp3Player().getTitleEncoded();
            if (title == null) { title = ""; }
            tv.setText(getMp3Player().getAppSettings().getCharsetName()+": "+title);
        }
    }
    
    synchronized void showInfo() {
        TextView tv = (TextView)findViewById(R.id.textView1);
        if (getMp3Player().getMediaPlayer() != null && tv != null) {
            tv.setText(Util.getPlayingMessage(getMp3Player().getMediaPlayer(), audioManager));
        }
    }
    
    private boolean working = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        boolean useFragment = false;
        if (useFragment) {
	        setContentView(R.layout.activity_main);
	
	        if (savedInstanceState == null) {
	            getFragmentManager().beginTransaction()
	                    .add(R.id.container, new PlaceholderFragment()).commit();
	        }
        } else {
	        setContentView(R.layout.fragment_main);
        }
        
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        AppService.mainActivity = this;
        
        //on restore
        startService(new Intent(this, AppService.class).setAction(AppService.ACTION_INIT));
    }
    
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(SAVED_STATE_KEY, true);
    }
    
    AutoCompleteTextView actv;
    SeekBar pbar;
    SeekBar pbarFiles;
    
    private void initWidgets() {
        if (pbar == null) {
            pbar = (SeekBar) findViewById(R.id.pbarSound);
            if (pbar != null) {
	            OnSeekBarChangeListener l = new OnSeekBarChangeListener() {
	                int progressValue = -1;
	                @Override
	                public void onStopTrackingTouch(SeekBar seekBar) {
	                    if (progressValue >=0) {
	                        //startService(new Intent(getActivity(), AppService.class).setAction(AppService.ACTION_SEEK).putExtra(AppService.PARAM_SEEK, progressValue));
	                        progressValue = -1;
	                    }
	                }
	                
	                @Override
	                public void onStartTrackingTouch(SeekBar seekBar) {
	                    // TODO Auto-generated method stub
	                    
	                }
	                
	                @Override
	                public void onProgressChanged(SeekBar seekBar, int progress,
	                        boolean fromUser) {
	                    if (fromUser) {
	                        progressValue = progress;
	                        startService(new Intent(getActivity(), AppService.class).setAction(AppService.ACTION_SEEK).putExtra(AppService.PARAM_SEEK, progressValue));
	                    }
	                }
	            };
            	pbar.setOnSeekBarChangeListener(l);
            }
        }
        if (pbarFiles == null) {
            pbarFiles = (SeekBar) findViewById(R.id.pbarFiles);
            if (pbarFiles != null) {
	            OnSeekBarChangeListener l = new OnSeekBarChangeListener() {
	                int progressValue = -1;
	                
	                @Override
	                public void onStopTrackingTouch(SeekBar seekBar) {
	                    if (progressValue >=0) {
	                        startService(new Intent(getActivity(), AppService.class).setAction(AppService.ACTION_TRACK).putExtra(AppService.PARAM_SEEK, progressValue));
	                        progressValue = -1;
	                    }
	                }
	                
	                @Override
	                public void onStartTrackingTouch(SeekBar seekBar) {
	                    // TODO Auto-generated method stub
	                    
	                }
	                
	                @Override
	                public void onProgressChanged(SeekBar seekBar, int progress,
	                        boolean fromUser) {
	                    if (fromUser) {
	                        progressValue = progress;
	                    }
	                }
	            };
	            pbarFiles.setOnSeekBarChangeListener(l);
            }
        }
        if (chbLoop == null) {
            chbLoop = (ToggleButton) findViewById(R.id.chbLoop);
            if (chbLoop != null) {
                if (getMp3Player() != null && getMp3Player().getAppSettings() != null) {
                    chbLoop.setChecked(getMp3Player().getAppSettings().isLoop());
                }
                chbLoop.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        getMp3Player().getAppSettings().setLoop(isChecked);
                    }
                });
            }
        } else {
            if (getMp3Player() != null && getMp3Player().getAppSettings() != null) {
                chbLoop.setChecked(getMp3Player().getAppSettings().isLoop());
            }
        }
        
        if (actv == null) {
            ArrayList<String> languages = new ArrayList<>(Charset.availableCharsets().keySet());
            Collections.sort(languages);
            actv = (AutoCompleteTextView)findViewById(R.id.actvTitle);
            if (actv != null) {
                if (getMp3Player() != null && getMp3Player().getAppSettings() != null) {
                    actv.setText(getMp3Player().getAppSettings().getCharsetName());
                }
                actv.setAdapter(new ModifiedArrayAdapter(this, android.R.layout.simple_list_item_1, languages));
                //actv.setVerticalScrollBarEnabled(false);
                actv.setThreshold(0);
                actv.setOnItemClickListener(new OnItemClickListener() {
    
                    @Override
                    public void onItemClick(AdapterView<?> parent, View arg1, int pos, long id) {
                        String enc = (String) parent.getItemAtPosition(pos);
                        setTitleEncoding(enc);
                    }
                });            
            }
        } else {
            if (getMp3Player() != null && getMp3Player().getAppSettings() != null) {
                ModifiedArrayAdapter adapter = (ModifiedArrayAdapter)actv.getAdapter();
                actv.setAdapter(null);
                actv.setText(getMp3Player().getAppSettings().getCharsetName());
                actv.setAdapter(adapter);
            }
        }
        
    }

    
    @Override
    protected void onResume() {
        working = true;
        super.onResume();
        initWidgets();
        showMediaInfo();
        updateHandler = new UpdateHandler(this);
        AppService.mainActivity = this;
    }

    @Override
    protected void onPause() {
        working = false;
        super.onPause();
    	AppService.mainActivity = null;
        if (updateHandler != null) {
        	updateHandler.removeCallbacksAndMessages(null);
        	updateHandler = null;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container,
                    false);
            
            return rootView;
        }
    }

    @Override
    public void onChangeStateEvent(ChangeStateEvent evt) {
    	Message msg = new Message();
    	Bundle data = new Bundle();
    	if (evt.getExtra() instanceof CantPlayException) {
            CantPlayException e = (CantPlayException)evt.getExtra();
            if(e != null) {
            	data.putString("msg", e.getMessage());
            }
    	}
    	data.putString("state", evt.getState().name());
    	msg.setData(data);
    	if (updateHandler != null) {
    		updateHandler.sendMessage(msg);
    	}
    }
    
    private UpdateHandler updateHandler;
    
    private static class UpdateHandler extends Handler {
        private final WeakReference<MainActivity> handledObject;
        
        public UpdateHandler(MainActivity p) {
        	handledObject = new WeakReference<MainActivity>(p);
        }
        
        public void handleMessage(android.os.Message msg) {
        	String stateStr = msg.getData().getString("state");
        	STATE state = null;
        	if (stateStr != null) {
        		state = STATE.valueOf(stateStr); 
        	}
        	MainActivity ho = handledObject.get();
	        if (state != null && ho != null && ho.working) {
				switch(state) {
				case CHANGE_DS:
				case START:
				case PREPARED:
				    ho.showMediaInfo();
				    break;
				case SEEK:
				case PAUSE:
				case STOP:
				case PLAYING:
				case COMPLETED:
				    ho.showTrackBar();
				    ho.showInfo();
				    break;
				case ERROR:
				    Util.toast("ERROR:" + msg.getData().getString("msg"), ho);
				    break;
				case RESET:
				case RELEASE:
				}
	        }
        }
    }
    
}
