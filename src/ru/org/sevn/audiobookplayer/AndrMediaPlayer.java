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

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;

public class AndrMediaPlayer extends MediaPlayerProxy<MediaPlayer, String> implements OnCompletionListener, OnPreparedListener {

    private boolean prepared = false;
    
    private static class UpdateHandler extends Handler {
        final long delayMillis = 1000;
        private boolean posted = false;
        private final WeakReference<AndrMediaPlayer> handledObject;
        
        public UpdateHandler(AndrMediaPlayer p) {
        	handledObject = new WeakReference<AndrMediaPlayer>(p);
        }
        
        final Runnable task = new Runnable(){

            @Override
            public void run() {
                posted = false;
                AndrMediaPlayer ho = handledObject.get();
                
                if (ho.isPrepared() && ho.isPlaying()) {
                    ho.fireEvent(STATE.PLAYING);
                    postDelayed();
                }
            }
        };
        
        public void postDelayed() {
            if (!posted) {
                posted = postDelayed(task, delayMillis);
            }
        }
    }
    
    private UpdateHandler updateHandler = new UpdateHandler(this);
    

    public AndrMediaPlayer(MediaPlayer mp) {
        super(mp);
        getMediaPlayer().setOnCompletionListener(this);
        getMediaPlayer().setOnPreparedListener(this);
    }

    @Override
    public void release() {
    	prepared = false;
        getMediaPlayer().setOnPreparedListener(null);
        getMediaPlayer().setOnCompletionListener(null);
        getMediaPlayer().release();
        if(updateHandler != null) {
        	updateHandler.removeCallbacksAndMessages(null);
        	updateHandler = null;
        }
        
        super.release();
    }

    @Override
    public void reset() {
        getMediaPlayer().reset();
        super.reset();
    }

    @Override
    public void pause() {
        if (isPrepared() && getMediaPlayer().isPlaying()) {
            getMediaPlayer().pause();
            super.pause();
        }
    }

    @Override
    public void seekTo(int i) {
        if (isPrepared()) {
            getMediaPlayer().seekTo(i);
            super.seekTo(i);
        }
    }

    @Override
    public boolean isPlaying() {
        return getMediaPlayer().isPlaying();
    }

    @Override
    public void start() {
        if (isPrepared()) {
            getMediaPlayer().start();
            super.start();
            updateHandler.postDelayed();
        }
    }

    @Override
    public void prepare(String dataSource) {
        prepared = false;
        
        MediaPlayer mediaPlayer = getMediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        
        try {
            mediaPlayer.setDataSource(dataSource);
            mediaPlayer.prepareAsync();
            super.prepare(dataSource);
        } catch (Exception e) {
            fireEvent(STATE.ERROR, new CantPlayException(dataSource, e));
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mp != null && mp.getDuration() >= 0 && mp.getCurrentPosition()+1000 >= mp.getDuration()) {
            fireEvent(STATE.COMPLETED);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        prepared = true;
        fireEvent(STATE.PREPARED);
    }

    @Override
    public void seekFromCurrent(int i) {
        if (isPrepared() && getMediaPlayer() != null) {
            seekTo(getMediaPlayer().getCurrentPosition() + i);
        }
    }

    @Override
    public int getCurrentPosition() {
        if (isPrepared() && getMediaPlayer() != null) {
            return getMediaPlayer().getCurrentPosition();
        }
        return 0;
    }

    @Override
    public int getDuration() {
        if (isPrepared() && getMediaPlayer() != null) {
            return getMediaPlayer().getDuration();
        }
        return 0;
    }

    public boolean isPrepared() {
        return prepared;
    }

}
