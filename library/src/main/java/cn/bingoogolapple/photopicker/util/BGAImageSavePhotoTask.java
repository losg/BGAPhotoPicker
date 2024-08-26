/**
 * Copyright 2016 bingoogolapple
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.bingoogolapple.photopicker.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import cn.bingoogolapple.photopicker.R;

/**
 * 作者:王浩 邮件:bingoogolapple@gmail.com
 * 创建时间:16/6/25 下午6:49
 * 描述:
 */
public class BGAImageSavePhotoTask extends BGAAsyncTask<Void, String> {
    private Context mContext;
    private File    mNewFile;
    private String  mUrl;

    public BGAImageSavePhotoTask(Callback<String> callback, Context context, File newFile) {
        super(callback);
        mContext = context.getApplicationContext();
        mNewFile = newFile;
    }

    public void startDownLoadUrl(String url) {
        mUrl = url;
        if (Build.VERSION.SDK_INT >= 11) {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            execute();
        }
    }


    @Override
    protected String doInBackground(Void... params) {

        try {
            URL url = new URL(mUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            if (urlConnection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) urlConnection).setSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
            }
            int code = urlConnection.getResponseCode();
            if (code != 200) {
                return null;
            }
            InputStream inputStream = urlConnection.getInputStream();
            mNewFile.getParentFile().exists();
            FileOutputStream fileOutputStream = new FileOutputStream(mNewFile);
            byte[] read = new byte[2 * 1024];
            int readLen;
            while ((readLen = inputStream.read(read)) > -1) {
                fileOutputStream.write(read,0,readLen);
            }
            fileOutputStream.close();
            return mNewFile.getAbsolutePath();
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    protected void onPostExecute(String savePath) {
        super.onPostExecute(savePath);
        if (savePath == null && mCallback.get() != null) {
            BGAPhotoPickerUtil.showSafe(R.string.bga_pp_save_img_failure);
            return;
        }
        if (mCallback.get() != null)
            BGAPhotoPickerUtil.showSafe(mContext.getString(R.string.bga_pp_save_img_success_folder, mNewFile.getParentFile().getAbsolutePath()));
        broadCastPicChange(savePath);
    }

    private void broadCastPicChange(String savePath) {
        File file = new File(savePath);
        if (Build.VERSION.SDK_INT < 19) {
            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(file)));
            return;
        }
        if (Build.VERSION.SDK_INT >= 29) {
            try {
                MediaStore.Images.Media.insertImage(mContext.getContentResolver(), file.getAbsolutePath(), file.getName(), null);
                return;
            } catch (Exception e) {
            }
        }
        autoScanFile(mContext, file.getName(), file.getAbsolutePath());
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = null;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            uri = FileProvider.getUriForFile(mContext, mContext.getPackageName() + ".fileprovider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        intent.setData(uri);
        mContext.sendBroadcast(intent);
    }


    private static void autoScanFile(Context context, String fileName, String filePath) {
        try {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = MediaStore.Files.getContentUri("external");

            ContentValues values = new ContentValues();
            values.put(MediaStore.Files.FileColumns.DATA, filePath);
            values.put(MediaStore.Files.FileColumns.TITLE, fileName);
            values.put(MediaStore.Files.FileColumns.MIME_TYPE, "image/jpeg");
            uri = resolver.insert(uri, values);
            MediaScannerConnection.scanFile(context, new String[]{filePath}, null, null);
        } catch (Exception e) {

        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }
}
