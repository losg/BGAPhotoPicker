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

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

/**
 * 作者:王浩 邮件:bingoogolapple@gmail.com
 * 创建时间:16/6/25 下午6:30
 * 描述:
 */
public abstract class BGAAsyncTask<Params, Result> extends AsyncTask<Params, Void, Result> {
    protected WeakReference<Callback<Result>> mCallback;

    public BGAAsyncTask(Callback<Result> callback) {
        mCallback = new WeakReference<>(callback);
    }

    public void cancelTask() {
        if (getStatus() != Status.FINISHED) {
            cancel(true);
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        if (mCallback.get() != null) {
            mCallback.get().onPostExecute(result);
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if (mCallback.get() != null) {
            mCallback.get().onTaskCancelled();
        }
    }

    public interface Callback<Result> {
        /**
         * 当结果返回的时候执行
         *
         * @param result 返回的结果
         */
        void onPostExecute(Result result);

        /**
         * 当请求被取消的时候执行
         */
        void onTaskCancelled();
    }
}
