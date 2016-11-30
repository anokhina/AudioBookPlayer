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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class DirInfo {
    private File file;
    private String label;
    private Bitmap bitmap;
    private File bookDir;
    
    private BookInfo bookInfo;
    
    public DirInfo(File f) {
        this(f, f.getName());
    }
    public DirInfo(File f, String l) {
        this.file = f;
        label = l;
        if (file != null && file.isDirectory()) {
        	for(String ext : DirActivity.FILE_NAME_ICON_EXT) {
	            File imgfile = new File(file, DirActivity.FILE_NAME_ICON+ext);
	            if (imgfile.exists() && imgfile.canRead()) {
	                bitmap = BitmapFactory.decodeFile(imgfile.getAbsolutePath());
	                break;
	            }
        	}            
            File cover = new File(file, DirActivity.FILE_NAME_COVER);
            File bDir = new File(file, DirActivity.FILE_NAME_BOOK);
            if (cover.exists() && cover.canRead()) {
                bitmap = BitmapFactory.decodeFile(cover.getAbsolutePath());
                if (bDir.exists() && bDir.isDirectory() && bDir.canRead()) {
                    bookDir = bDir;
                }
            }
        } 
    }
    public String toString() {
        return label;
    }
    public File getFile() {
        return file;
    }
    public String getLabel() {
        return label;
    }
    public Bitmap getBitmap() {
        return bitmap;
    }
    public File getBookDir() {
        return bookDir;
    }
	public DirInfo setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
		return this;
	}
	BookInfo getBookInfo() {
		return bookInfo;
	}
	void setBookInfo(BookInfo bookInfo) {
		this.bookInfo = bookInfo;
	}
    
}