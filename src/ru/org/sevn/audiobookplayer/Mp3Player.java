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
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Comparator;

import ru.org.sevn.audiobookplayer.MediaPlayerProxy.ChangeStateEvent;
import ru.org.sevn.audiobookplayer.MediaPlayerProxy.ChangeStateListener;
import ru.org.sevn.audiobookplayer.MediaPlayerProxy.STATE;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.text.TextUtils;

public class Mp3Player implements ChangeStateListener {
    
    //https://developer.android.com/reference/android/media/MediaPlayer.html
    private MediaPlayerProxy mediaPlayer;
    private Mp3PlayerSettings appSettings = new Mp3PlayerSettings();
    private File[] files;
    private int playingFileIdx = -1;

    private Comparator<File> fileComparator = new FileNameComparator();
    
    private FilenameFilter filenameFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            File nfl = new File(dir, filename);
            if (nfl.isDirectory()) {
            	return (nfl.canRead());
            }
            if (filename.matches("(?i).*\\.mp3")) {
                if (!nfl.isDirectory()) {
                    return (nfl.canRead());
                }
            }
            return false;
        }
    };
    public boolean isInitialized() {
        return (mediaPlayer != null);
    }

    public synchronized void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.removePropertyChangeListener(this);
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    
    public synchronized void resetMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.reset();
        }
    }
    
    public synchronized void restore(Mp3PlayerSettings appSet) {
        if (appSet != null && appSet.getPlayingDirPath() != null) {
            final int seek = appSet.getSeek();
            restore(new File(appSet.getPlayingDirPath()), appSet.getPlayingName(), new Runnable() {
                public void run() {
                    mediaPlayer.seekTo(seek);
                    toRunOnComplete = completePlayRun;
                }
            });
        }
    }
    
    public static class ComplexRunnable implements Runnable {
    	private final Runnable[] runnables;

		@Override
		public void run() {
			for(Runnable r : runnables) {
				if (r != null) {
					r.run();
				}
			}
		}
		
		public ComplexRunnable(Runnable[] runnables) {
			this.runnables = runnables;
		}
    }

    private Runnable noPlayRun = new Runnable() {
        
        @Override
        public void run() {
            toRunOnComplete = completePlayRun;
        }
    };
    
    private void restore(File parentDir, String name, Runnable runnable) {
    	
//    	System.err.println("--------------"+parentDir+":"+name+":"+runnable);
        File[] fileList = BookInfo.getFileList(parentDir, filenameFilter);
        Arrays.sort(fileList, fileComparator);
        int i = 0;
        if (name != null) {
        	i = -1;
	        for (File fl : fileList) {
	            i++;
	            if (fl.getAbsolutePath().endsWith(name)) {
	                break;
	            }
	        }
        }
        if (fileList.length > 0) {
        	if (i>=0 && i < fileList.length) {
            	getAppSettings().lastBook(fileList[i]);
        	} else {
            	getAppSettings().lastBook(null);
        	}
        }
        
        BookInfo bi = getAppSettings().getLastBook();
        final int seek;
        if ( bi != null) {
        	seek = bi.getSeek();
        } else {
        	seek = 0;
        }
        ComplexRunnable seekAndRun = new ComplexRunnable(new Runnable[]{
        		new Runnable() {
        			public void run() {
        				mediaPlayer.seekTo(seek);
        			}
        		},
        		runnable});
        		
        startPlaying(fileList, parentDir.getAbsolutePath(), i, seekAndRun);
    }
    
    public synchronized void setFile(Uri uri) {
        if (uri == null) return;
        File selectedFile = new File(uri.getPath());
        File parentDir = selectedFile;
        String file2play = null;
        if (!selectedFile.isDirectory()) {
            parentDir = BookInfo.findBookDirFile(selectedFile.getAbsolutePath());
        	file2play = BookInfo.getRelativeFileName(parentDir, selectedFile);
        }
        restore(parentDir, file2play, autoPlayRun);
    }
    
    private void startPlaying(File[] fileList, String playingDir, int idx, Runnable aPlay) {
        stopPlaying();
        files = fileList;
        playingFileIdx = idx - 1;
        appSettings.setPlayingName(null);
        appSettings.setPlayingDirPath(playingDir);
        startPlaying(aPlay);
    }
    
    public synchronized void stopPlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }
    
    public int getFilesLength() {
        if (files != null) {
            return files.length;
        }
        return 0;
    }
    
    public synchronized boolean startPlaying() {
        return startPlaying(autoPlayRun);
    }
    private boolean startPlaying(Runnable aPlay) {
        if (getFilesLength() > 0) {
            return playNext(aPlay);
        }
        return false;        
    }
    
    private Runnable getAutoPlayRun() {
        if (getMediaPlayer()!=null && !getMediaPlayer().isPlaying()) {
            return noPlayRun;
        }
        return autoPlayRun;
    }
    public synchronized boolean playNext() {
        return playNext(getAutoPlayRun());
    }
    protected boolean playNext(Runnable aPlay) {
        playingFileIdx++; 
        if (playingFileIdx >= getFilesLength()) {
            if (appSettings.isLoop()) {
                playingFileIdx = 0;
            } else {
                playingFileIdx = getFilesLength();
            }
        }
        return playCurrent(aPlay);
    }
    
    public synchronized boolean playPrev() {
        return playPrev(getAutoPlayRun());
    }
    protected boolean playPrev(Runnable aPlay) {
        playingFileIdx--; 
        if (playingFileIdx < 0) {
            if (appSettings.isLoop()) {
                playingFileIdx = getFilesLength() - 1;
            } else {
                playingFileIdx = -1;
            }
        }
        return playCurrent(aPlay);
    }
    
    public synchronized boolean playTrack(int idx) {
        return playTrack(idx, getAutoPlayRun());
    }
    protected synchronized boolean playTrack(int idx, Runnable aPlay) {
        playingFileIdx = idx - 1;
        return playNext(aPlay);
    }
    
    public synchronized boolean playCurrent() {
        return playCurrent(getAutoPlayRun());
    }
    protected boolean playCurrent(Runnable aPlay) {
        toRunOnComplete = null;
        toRunOnPrepare = null;
        appSettings.setPlayingName(null);
        if (playingFileIdx >=0 && playingFileIdx < getFilesLength()) {
            return play(files[playingFileIdx], aPlay);
        }
        return false;
    }
    
    public synchronized void initialize() {
        if (mediaPlayer == null) {
            mediaPlayer = createMediaPlayer();
            mediaPlayer.addChangeStateListener(this);
        }
    }
    
    protected MediaPlayerProxy createMediaPlayer() {
        MediaPlayer player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        /*
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnSeekCompleteListener(this);
        player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(audioOutputChangedEventReceiver, filter);
        */
        return new AndrMediaPlayer(player);
    }
    
    private Runnable toRunOnPrepare;
    private Runnable toRunOnComplete;
    
    private Runnable autoPlayRun = new Runnable() {
        public void run() {
            resumePlaying();
            toRunOnComplete = completePlayRun;
        }
    };
    private Runnable completePlayRun = new Runnable() {
        public void run() {
            playNext(autoPlayRun);
        }
    };
    
    private boolean play(File selectedFile, final Runnable aPlay) {
        resetMediaPlayer();
        initialize();
        if (selectedFile != null && selectedFile.exists() && selectedFile.canRead()) {
            try {
	            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
	            mmr.setDataSource(selectedFile.getAbsolutePath());
	            
	            getAppSettings().setTitle(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
	            getAppSettings().setImage(mmr.getEmbeddedPicture());
	            
	            appSettings.setPlayingName(BookInfo.getRelativeFileName(getAppSettings().getPlayingDirPath(), selectedFile));
            
                toRunOnPrepare = aPlay;
                mediaPlayer.prepare(selectedFile.getAbsolutePath());
                return true;
            } catch (Exception e) {
            }
        }
        return false;
    }
    
    public synchronized boolean resumePlaying() {
        if (mediaPlayer != null) {
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                return true;
            }
        }
        return false;
    }
    
    public synchronized boolean pausePlaying() {
        if (mediaPlayer != null) {
        	if (mediaPlayer.isPlaying()) {
        		mediaPlayer.pause();
        	}
            return true;
        }
        return false;
    }
    
    public void setTitleEncoding(String enc) {
        appSettings.setCharsetName(enc);
    }
    
    public String getTitleEncoded() {
        return getTitleEncoded(appSettings.getTitle(), appSettings.getCharsetName());
    }
    
    private String getTitleEncoded(String msg, String encoding) {
        if (msg != null) {
            String encW = "";
            try {
                for (char ch : msg.toCharArray() ) {
                    if (ch < 256) {
                        byte bt[] = new byte[1];
                        bt[0] = (byte)ch;
                        encW += new String(bt, encoding);
                    } else {
                        encW += ch;
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return encW;
        }
        return msg;
    }

    public MediaPlayerProxy getMediaPlayer() {
        return mediaPlayer;
    }

    public synchronized void startPlayingFirst() {
        startPlaying(files, appSettings.getPlayingDirPath(), 0, autoPlayRun);
    }
    
    public Mp3PlayerSettings getAppSettings() {
        return appSettings;
    }

    public int getPlayingFileIdx() {
        return playingFileIdx;
    }

    @Override
    public void onChangeStateEvent(ChangeStateEvent evt) {
        Runnable toRun = null;
        if (evt.getState() == STATE.COMPLETED && mediaPlayer != null) {
                toRun = toRunOnComplete;
                toRunOnComplete = null;
        } else
        if (evt.getState() == STATE.PREPARED && mediaPlayer != null) {
            toRun = toRunOnPrepare;
            toRunOnPrepare = null;
        }
        if (toRun != null) {
            toRun.run();
        }
    }

//    private final BroadcastReceiver audioOutputChangedEventReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if(intent == null)
//                return;
//
//            String action = intent.getAction();
//            if(TextUtils.isEmpty(action))
//                return;
//
//            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
//                onReceiveActionAudioBecomingNoisy(intent);
//            } else if(Intent.ACTION_HEADSET_PLUG.equals(action)) {
//                onReceiveActionHeadsetPlug(intent);
//            } else if(
//                BluetoothDevice.ACTION_ACL_CONNECTED.equals(action) ||
//                BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
//                onReceiveActionAclConnection(intent);
//            }
//        }
//
//        private void onReceiveActionAudioBecomingNoisy(Intent intent) {
//            pauseTrack();
//        }
//
//        private void onReceiveActionHeadsetPlug(Intent intent) {
//            int state = intent.getIntExtra("state", -1);
//            if(state == -1) {
//                Log.d(TAG, "Unknown headset plug event parameter.");
//                return;
//            }
//
//            if(state == 0) {    // Disconnected
//                showNotification(getCurrentAudioPathOtherThanWired());
//            } else {            // Connected
//                showNotification(AUDIO_PATH_WIRED);
//            }
//        }
//
//        private void onReceiveActionAclConnection(Intent intent) {
//            Log.d(TAG, "onReceiveActionAclConnection action: " + intent.getAction());
//
//            String action = intent.getAction();
//            if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
//                showNotification(AUDIO_PATH_A2DP);
//            } else {
//                showNotification(getCurrentAudioPathOtherThanA2dp());
//            }
//        }
//    };
}
