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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class MediaPlayerProxy<MEDIAPLAYER, DATASOURCE> {
    
    public static class CantPlayException extends Exception {
        public CantPlayException(String msg, Throwable tr) {
            super(msg, tr);
        }
        
    }

    public enum STATE {
        CHANGE_DS, START, SEEK, PAUSE, RESET, RELEASE, STOP, PREPARED, COMPLETED, PLAYING, ERROR 
    }
    
    public interface ChangeStateListener {
        void onChangeStateEvent(ChangeStateEvent evt);
    }
    
    private transient List<ChangeStateListener> listeners = new CopyOnWriteArrayList<ChangeStateListener>();
    
    public void addChangeStateListener(ChangeStateListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            listeners.add(listener);
        }
    }
    public void removePropertyChangeListener(ChangeStateListener listener) {
        listeners.remove(listener);
    }
    
 
    public static class ChangeStateEvent {
        private MediaPlayerProxy mediaPlayer;
        private STATE state;
        private Object extra;
        public ChangeStateEvent(MediaPlayerProxy mediaPlayer, STATE state, Object extra) {
            this.mediaPlayer = mediaPlayer;
            this.state = state;
            this.extra = extra;
        }
        public MediaPlayerProxy getMediaPlayer() {
            return mediaPlayer;
        }
        public void setMediaPlayer(MediaPlayerProxy mediaPlayer) {
            this.mediaPlayer = mediaPlayer;
        }
        public STATE getState() {
            return state;
        }
        public void setState(STATE state) {
            this.state = state;
        }
        public Object getExtra() {
            return extra;
        }
        public void setExtra(Object extra) {
            this.extra = extra;
        }
    }
    
    protected void fireEvent(STATE state) {
        fireEvent(state, null);
    }
    protected void fireEvent(STATE state, Object extra) {
        ChangeStateEvent evt = new ChangeStateEvent(this, state, extra);
        for (ChangeStateListener l : listeners) {
            l.onChangeStateEvent(evt);
        }
    }
    
    private MEDIAPLAYER mediaPlayer;
    
    public MediaPlayerProxy(MEDIAPLAYER mp) {
        mediaPlayer = mp;
    }

    public MEDIAPLAYER getMediaPlayer() {
        return mediaPlayer;
    }

    public void setMediaPlayer(MEDIAPLAYER mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public void release() {
        fireEvent(STATE.RELEASE);
    }
    
    public void reset() {
        fireEvent(STATE.RESET);
    }

    public void pause() {
        fireEvent(STATE.PAUSE);
    }

    public void seekTo(int i) {
        fireEvent(STATE.SEEK, i);
    }

    public abstract void seekFromCurrent(int i);

    public abstract boolean isPlaying();

    public void start() {
        fireEvent(STATE.START);
    }

    private DATASOURCE dataSource;
    public void prepare(DATASOURCE dataSource) {
        //DONT FIRE PREPARED!
        this.dataSource = dataSource;
        fireEvent(STATE.CHANGE_DS);
    }
    public void stop() {
        pause();
        seekTo(0);
        fireEvent(STATE.STOP);
    }
    public abstract int getCurrentPosition();
    public abstract int getDuration();
    
    public DATASOURCE getDataSource() {
        return dataSource;
    }
}
