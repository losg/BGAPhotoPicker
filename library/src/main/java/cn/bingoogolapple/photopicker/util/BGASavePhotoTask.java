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
import java.lang.ref.SoftReference;
import java.sql.NClob;

import cn.bingoogolapple.photopicker.R;

/**
 * 作者:王浩 邮件:bingoogolapple@gmail.com
 * 创建时间:16/6/25 下午6:49
 * 描述:
 */
public class BGASavePhotoTask extends BGAAsyncTask<Void, String> {
    private Context               mContext;
    private SoftReference<Bitmap> mBitmap;
    private File                  mNewFile;

    public BGASavePhotoTask(Callback<String> callback, Context context, File newFile) {
        super(callback);
        mContext = context.getApplicationContext();
        mNewFile = newFile;
    }

    public void setBitmapAndPerform(Bitmap bitmap) {
        mBitmap = new SoftReference<>(bitmap);

        if (Build.VERSION.SDK_INT >= 11) {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            execute();
        }
    }

    @Override
    protected String doInBackground(Void... params) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mNewFile);
            mBitmap.get().compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            BGAPhotoPickerUtil.showSafe(mContext.getString(R.string.bga_pp_save_img_success_folder, mNewFile.getParentFile().getAbsolutePath()));
            return mNewFile.getAbsolutePath();
        } catch (Exception e) {
            BGAPhotoPickerUtil.showSafe(R.string.bga_pp_save_img_failure);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    BGAPhotoPickerUtil.showSafe(R.string.bga_pp_save_img_failure);
                }
            }
            recycleBitmap();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String savePath) {
        super.onPostExecute(savePath);
        if (savePath == null) {
            return;
        }
        broadCastPicChange(savePath);
    }

    private void broadCastPicChange(String savePath) {
        File file = new File(savePath);
        if (Build.VERSION.SDK_INT < 19) {
            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(file)));
            return;
        }
        if (Build.VERSION.SDK_INT >= 29) {
           MediaStore.Images.Media.insertImage(mContext.getContentResolver(), file.getAbsolutePath(), file.getName(), null);
           return;
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



    private static void autoScanFile(Context context,String fileName,String filePath){
        try {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = MediaStore.Files.getContentUri("external");

            ContentValues values = new ContentValues();
            values.put(MediaStore.Files.FileColumns.DATA, filePath);
            values.put(MediaStore.Files.FileColumns.TITLE, fileName);
            values.put(MediaStore.Files.FileColumns.MIME_TYPE, "image/jpeg");
            uri = resolver.insert(uri, values);
            MediaScannerConnection.scanFile(context, new String[]{filePath}, null, null);
        }catch (Exception e) {

        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        recycleBitmap();
    }

    private void recycleBitmap() {
        if (mBitmap != null && mBitmap.get() != null && !mBitmap.get().isRecycled()) {
            mBitmap.get().recycle();
            mBitmap = null;
        }
    }

}
