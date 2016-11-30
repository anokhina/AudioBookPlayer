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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.json.JSONArray;
import org.json.JSONException;

import ru.org.sevn.audiobookplayer.MediaPlayerProxy.CantPlayException;
import ru.org.sevn.audiobookplayer.MediaPlayerProxy.ChangeStateEvent;
import ru.org.sevn.audiobookplayer.MediaPlayerProxy.ChangeStateListener;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;
//import android.support.v4.app.NotificationCompat;
import android.net.Uri;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class AppService extends Service implements ChangeStateListener, PropertyChangeListener, OnAudioFocusChangeListener {
    
    private static final String URI_BASE = AppService.class.getName() + ".";
    
    public static final String ACTION_PAUSE = "ru.org.sevn.simplemp3play.AppService.ACTION_PAUSE";
    public static final String ACTION_PLAY_PAUSE = "ru.org.sevn.simplemp3play.AppService.ACTION_PLAY_PAUSE";
    public static final String ACTION_PLAY = "ru.org.sevn.simplemp3play.AppService.ACTION_PLAY";
    public static final String ACTION_STOP = "ru.org.sevn.simplemp3play.AppService.ACTION_STOP";
    public static final String ACTION_PREV = "ru.org.sevn.simplemp3play.AppService.ACTION_PREV";
    public static final String ACTION_NEXT = "ru.org.sevn.simplemp3play.AppService.ACTION_NEXT";
    public static final String ACTION_BWARD = "ru.org.sevn.simplemp3play.AppService.ACTION_BWARD";
    public static final String ACTION_FWARD = "ru.org.sevn.simplemp3play.AppService.ACTION_FWARD";
    public static final String ACTION_FIRST = "ru.org.sevn.simplemp3play.AppService.ACTION_FIRST";
    public static final String ACTION_SEEK = "ru.org.sevn.simplemp3play.AppService.ACTION_SEEK";
    public static final String ACTION_TRACK = "ru.org.sevn.simplemp3play.AppService.ACTION_TRACK";
    public static final String ACTION_EXIT = "ru.org.sevn.simplemp3play.AppService.ACTION_EXIT";
    public static final String ACTION_INIT = "ru.org.sevn.simplemp3play.AppService.ACTION_INIT";
    
    public static final String ACTION_SET_FILE = "ru.org.sevn.simplemp3play.AppService.SET_FILE";
    
    
    public static final String ACTION_TRACK_UP = URI_BASE + "ACTION_TRACK_UP";
    public static final String ACTION_TRACK_DOWN = URI_BASE + "ACTION_TRACK_DOWN";
    
    public static final String PARAM_URI = URI_BASE + "param_uri";
    public static final String PARAM_SEEK = URI_BASE + "param_seek";
    
    static final Mp3Player mp3Player = new Mp3Player();
    
    private AudioManager audioManager;
    private Bitmap defaultIcon;
    
    //TODO depends on screen density
    private static final int ICON_SIZE = 96;
    
    static MainActivity mainActivity;
    
	private class MusicIntentReceiver extends BroadcastReceiver {
	    @Override public void onReceive(Context context, Intent intent) {
	        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
	            int state = intent.getIntExtra("state", -1);
	            if (state == 0 || state == 1) {
	            	getMp3Player().pausePlaying();
	            }
	        }
	    }
	}
	
	private MusicIntentReceiver musicIntentReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private Mp3PlayerSettings initSettings;
    
    @Override
    public void onCreate() {
        defaultIcon = BitmapFactory.decodeResource(getResources(), R.drawable.picture);
        defaultIcon = Util.getScaledImage(defaultIcon, ICON_SIZE, ICON_SIZE, Util.getImageFactor(getResources()));
        
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        super.onCreate();
        mp3Player.initialize();
        restoreSettings();
        mp3Player.getMediaPlayer().addChangeStateListener(this);
        mp3Player.getAppSettings().addPropertyChangeListener(this);
        
    	TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
    	if(mgr != null) {
    	    mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    	}
    	
    	musicIntentReceiver = new MusicIntentReceiver();
        registerReceiver(musicIntentReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));	
    	
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if(mgr != null) {
            mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        
    	saveLastOpened(preferenceEditor);
        releaseMediaPlayer();
        audioManager.abandonAudioFocus(this);
        if (preferenceEditor != null) {
	        preferenceEditor.commit();
	        preferenceEditor = null;
        }
        stopForeground(true);
    }
    
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch(intent.getAction()) {
        	case ACTION_INIT:
        		mp3Player.restore(initSettings);
        		initSettings = null;
        		break;
            case ACTION_SEEK:
                if (mp3Player.getMediaPlayer() != null) {
                    int i = intent.getExtras().getInt(PARAM_SEEK);
                    mp3Player.getMediaPlayer().seekTo(i);
                }
                break;
            case ACTION_TRACK:
                int i = intent.getExtras().getInt(PARAM_SEEK);
                mp3Player.playTrack(i);
                break;
            case ACTION_SET_FILE:
                Uri uri = (Uri)intent.getExtras().get(PARAM_URI);
                mp3Player.setFile(uri);
                break;
            default: 
                doAction(intent.getAction());
        }
        
        startForeground(1, 
                makeNotify(
                        getApplicationContext(), 
                        mp3Player.getTitleEncoded(), 
                        Util.getShortPlayingMessage(mp3Player.getMediaPlayer(), audioManager)
                        ).build());
        return START_NOT_STICKY;
    }
    
    private void releaseMediaPlayer() {
    	if (musicIntentReceiver != null) {
    		unregisterReceiver(musicIntentReceiver);
    		musicIntentReceiver = null;
    	}
        if (mp3Player.getMediaPlayer() != null) {
            mp3Player.getMediaPlayer().removePropertyChangeListener(this);
        }
        mp3Player.getAppSettings().removePropertyChangeListener(this);
        
        mp3Player.releaseMediaPlayer();
    }

    @Override
    public void onChangeStateEvent(ChangeStateEvent evt) {
//    	System.err.println("+++++++++++++++++++"+evt.getState().name());
        switch(evt.getState()) {
        case CHANGE_DS:
        case PAUSE:
        case START:
        case SEEK:
        	saveLastOpened(this.preferenceEditor);
        case PLAYING:
            if (mp3Player.getMediaPlayer() != null) {
                // TODO set to 0 at start playing
                mp3Player.getAppSettings().setSeek(mp3Player.getMediaPlayer().getCurrentPosition());
            }
            String title = mp3Player.getTitleEncoded();
            String playingMsg = Util.getShortPlayingMessage(mp3Player.getMediaPlayer(), audioManager);
            notify(getApplicationContext(), title, playingMsg);
            break;
        case ERROR:
            CantPlayException e = (CantPlayException)evt.getExtra();
            notify(getApplicationContext(), "ERROR", e.getMessage());
        case RESET:
        case RELEASE:
        case STOP: //pause seek(0)
        }
        if (mainActivity != null) {
            mainActivity.onChangeStateEvent(evt);
        }
    }
    private void notify(Context ctx, String title, String text) {
        int mId = 1;
        NotificationManager mNotificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = makeNotify(ctx, title, text);
        mNotificationManager.notify(mId, mBuilder.build());
    }

	private NotificationCompat.Builder makeNotify(Context ctx, String title, String text){
	    //Notification notification = new Notification(R.drawable.ic_launcher, null, System.currentTimeMillis());
    	NotificationCompat.Builder mBuilder = makeNotifyBuilder(ctx, title, text);
        		
	    RemoteViews notificationView = new RemoteViews(getPackageName(), R.layout.notification);
	    notificationView.setTextViewText(R.id.tvTitle, title);
	    notificationView.setTextViewText(R.id.textView1, "" + makeNotifyNumber() + " " + text);
	    notificationView.setImageViewBitmap(R.id.imageView1, getLargeNotificationIcon());
    	mBuilder.setContent(notificationView);
        mBuilder.setContentIntent(makePendingNotificationIntent(makeNotificationIntent()));
	
//	    notification.contentView = notificationView;
//	    notification.contentIntent = makePendingNotificationIntent(makeNotificationIntent());
//	    notification.flags |= Notification.FLAG_NO_CLEAR;
	
	    notificationView.setOnClickPendingIntent(R.id.btn_prev, makeActionPendingIntent(ACTION_PREV));
	    notificationView.setOnClickPendingIntent(R.id.btn_play, makeActionPendingIntent(ACTION_PLAY_PAUSE));
	    notificationView.setOnClickPendingIntent(R.id.btn_next, makeActionPendingIntent(ACTION_NEXT));
	    notificationView.setOnClickPendingIntent(R.id.btn_exit, makeActionPendingIntent(ACTION_EXIT));
	
	    return mBuilder;
	}
	private Intent makeNotificationIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
	}
	private PendingIntent makePendingNotificationIntent(Intent intent) {
        int requestCode = (int)System.currentTimeMillis();
        requestCode = 3;
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
	}
	private Bitmap getLargeNotificationIcon() {
        if (mp3Player.getAppSettings().getBitmapImage() != null) {
            return mp3Player.getAppSettings().getBitmapLargeImage(ICON_SIZE, ICON_SIZE, Util.getImageFactor(getResources()));
        } else {
            //icon = makeBitmap(33);
        	return defaultIcon;
        }
	}
    private NotificationCompat.Builder makeNotifyBuilder(Context ctx, String title, String text) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ctx)
                .setSmallIcon(R.drawable.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                //.setLights(Color.RED, 3000, 3000);
                //.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });
//                    .setSound(Uri.parse(PreferenceManager.getDefaultSharedPreferences(ctx).getString(
//                            ConstUtil.get(ctx, R.string.const_pref_charge_ringtone), "default ringtone")))
                .setContentTitle(title)
                .setContentText(text);
//                    .setContent(contentView);
        return mBuilder;
    }
    public int makeNotifyNumber() {
        if (audioManager != null) {
            return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
    	return 0;
    }
    private NotificationCompat.Builder makeNotify1(Context ctx, String title, String text) {
    	
    	NotificationCompat.Builder mBuilder = makeNotifyBuilder(ctx, title, text);

        mBuilder.setLargeIcon(getLargeNotificationIcon());
        mBuilder.setNumber(makeNotifyNumber());
        
        mBuilder.setAutoCancel(true);
    
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        /*
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(ctx);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(intent);
        */
        
        mBuilder.setContentIntent(makePendingNotificationIntent(makeNotificationIntent()));
        
        boolean isPlaying = false;
        if (getMp3Player() != null && getMp3Player().getMediaPlayer() != null) {
            isPlaying = getMp3Player().getMediaPlayer().isPlaying();
        }
        
        addAction(mBuilder, android.R.drawable.ic_media_previous, "", ACTION_PREV);
        if (isPlaying) {
            addAction(mBuilder, android.R.drawable.ic_media_pause, "", ACTION_PLAY_PAUSE);
        } else {
            addAction(mBuilder, android.R.drawable.ic_media_play, "", ACTION_PLAY_PAUSE);
        }
        addAction(mBuilder, android.R.drawable.ic_media_next, "", ACTION_NEXT);
        addAction(mBuilder, android.R.drawable.ic_delete, "", ACTION_EXIT);
        return mBuilder;
    }
    
    private void addAction(NotificationCompat.Builder mBuilder, int drawableId, String name, String actionid) {
        mBuilder.addAction(drawableId, name, makeActionPendingIntent(actionid));
    }
    
    private PendingIntent makeActionPendingIntent(String actionid) {
        Intent intent = new Intent(this, AppService.class);
        intent.setAction(actionid);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    	return pendingIntent;
    }
    
    private Mp3Player getMp3Player() {
        return mp3Player;
    }
    
    private void doAction(String actionName) {
        if (getMp3Player().getMediaPlayer() != null) {
            switch (actionName) {
                case ACTION_PAUSE:
                    getMp3Player().pausePlaying();
                case ACTION_PLAY:
                    getMp3Player().resumePlaying();
                case ACTION_PLAY_PAUSE:
                    boolean isPlaying = false;
                    isPlaying = getMp3Player().getMediaPlayer().isPlaying();
                    if (!isPlaying) {
                        getMp3Player().resumePlaying();
                    } else {
                        getMp3Player().pausePlaying();
                    }
                    break;
                case ACTION_STOP:
                    getMp3Player().stopPlaying();
                    break;
                case ACTION_PREV:
                    getMp3Player().playPrev();
                    break;
                case ACTION_NEXT:
                    getMp3Player().playNext();
                    break;
                case ACTION_BWARD:
                    if (getMp3Player().getMediaPlayer() != null) {
                        getMp3Player().getMediaPlayer().seekFromCurrent(-3000);
                    }
                    break;
                case ACTION_FWARD:
                    if (getMp3Player().getMediaPlayer() != null) {
                        getMp3Player().getMediaPlayer().seekFromCurrent(3000);
                    }
                    break;
                case ACTION_FIRST:
                    getMp3Player().startPlayingFirst();
                    break;
                case ACTION_EXIT:
                    getMp3Player().pausePlaying();
                    if (mainActivity != null) {
                        mainActivity.finish();
                        mainActivity = null;
                    }
                    releaseMediaPlayer();

                    stopSelf();
                    break;
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        saveSettings(event.getPropertyName());
    }

    private static final String[] props = new String[] {"charsetName", "playingDirPath", "playingName", "title", "loop", "seek"};
    
    private Editor preferenceEditor;
    
    private void saveSettings(String prop) {
        if (getMp3Player() != null && getMp3Player().getAppSettings() != null) {
            if (preferenceEditor == null) {
                SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
            	preferenceEditor = prefs.edit();
            }
            if (prop != null) {
                saveSetting(preferenceEditor, prop);
            } else {
                for(String p : props) {
                    saveSetting(preferenceEditor, p);
                }
            }
            //preferenceEditor.apply();
        }
    }
    private void restoreSettings() {
        if (getMp3Player() != null && getMp3Player().getAppSettings() != null) {
            SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
            getMp3Player().getAppSettings().setCharsetName(prefs.getString(key(R.string.const_pref_encoding), "UTF-8"));
            getMp3Player().getAppSettings().setPlayingDirPath(prefs.getString(key(R.string.const_pref_playing_dir), null));
            getMp3Player().getAppSettings().setPlayingName(prefs.getString(key(R.string.const_pref_playing_name), null));
            getMp3Player().getAppSettings().setTitle(prefs.getString(key(R.string.const_pref_title), null));
            getMp3Player().getAppSettings().setLoop(prefs.getBoolean(key(R.string.const_pref_loop), false));
            getMp3Player().getAppSettings().setSeek(prefs.getInt(key(R.string.const_pref_seek), 0));
            restoreLastOpened();            
            initSettings = mp3Player.getAppSettings().getCopy();
//            System.err.println("*************>"+initSettings.getLastBooks().size()+":"+getMp3Player().getAppSettings().getLastBooks().size());
        }        
    }
    private void saveSetting(Editor ed, String prop) {
        switch(prop) {
        case "charsetName":
            ed.putString(key(R.string.const_pref_encoding), getMp3Player().getAppSettings().getCharsetName());
            break;
        case "playingDirPath":
            ed.putString(key(R.string.const_pref_playing_dir), getMp3Player().getAppSettings().getPlayingDirPath());
            break;
        case "playingName":
            ed.putString(key(R.string.const_pref_playing_name), getMp3Player().getAppSettings().getPlayingName());
            break;
        case "title":
            ed.putString(key(R.string.const_pref_title), getMp3Player().getAppSettings().getTitle());
            break;
        case "loop":
            ed.putBoolean(key(R.string.const_pref_loop), getMp3Player().getAppSettings().isLoop());
            break;
        case "seek":
            ed.putInt(key(R.string.const_pref_seek), getMp3Player().getAppSettings().getSeek());
            break;
        }
        
    }
    
    private void saveLastOpened(Editor ed) {
    	// TODO Optimization - save separate
    	if (ed != null && getMp3Player() != null && getMp3Player().getAppSettings() != null) {
	    	JSONArray arr = new JSONArray();
	    	for (BookInfo bi : getMp3Player().getAppSettings().getLastBooks().values()) {
	    		try {
					bi.putIn(arr);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    	ed.putString(key(R.string.const_pref_last_opened), arr.toString());
//        	System.err.println("SSSSSSSSSSSSSSSSSSSSSSSS>"+arr.toString());
	    	
    	}
    	ed.apply();
    }
    private void restoreLastOpened() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREF_NAME, MODE_PRIVATE);
        try {
        	JSONArray arr = new JSONArray(prefs.getString(key(R.string.const_pref_last_opened), ""));
//        	System.err.println("RRRRRRRRRRRRRRRRRRRRRRRRR>"+prefs.getString(key(R.string.const_pref_last_opened), ""));
			getMp3Player().getAppSettings().getLastBooks().clear();
        	
        	for(int i = 0; i < arr.length(); i++) {
        		BookInfo bi = BookInfo.makeBookInfo(arr.getJSONObject(i));
        		if (bi != null) {
                	getMp3Player().getAppSettings().getLastBooks().put(bi.getDirInfo().getBookDir().getAbsolutePath(), bi);
        		}
        	}
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
        }
    }
    
    private String key(int id) {
        return getResources().getString(id);
    }

	@Override
	public void onAudioFocusChange(int focusChange) {
	    if(focusChange<=0) { //LOSS
            getMp3Player().pausePlaying();
	    } else {
            //getMp3Player().resumePlaying();
	    }
    }
	
	
	PhoneStateListener phoneStateListener = new PhoneStateListener() {
	    @Override
	    public void onCallStateChanged(int state, String incomingNumber) {
	        if (state == TelephonyManager.CALL_STATE_RINGING) {
	        	getMp3Player().pausePlaying();
//	        } else if(state == TelephonyManager.CALL_STATE_IDLE) {
//	            //Not in call: Play music
//	        } else if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
//	            //A call is dialing, active or on hold
	        }
	        super.onCallStateChanged(state, incomingNumber);
	    }
	};
	
}
