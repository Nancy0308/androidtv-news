package io.github.anenasa.news;

import android.content.Context;
import android.os.StrictMode;
import android.util.Log;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class YtDlp {
    private final String TAG = "YtDlp";
    private PyObject extractor;

    /**
     * Initialize yt-dlp
     * @param context Context
     * @throws PyException yt-dlp failed to load
     */
    YtDlp(Context context) throws PyException {
        if(!YtDlp.isDownloaded(context)) {
            YtDlp.download(context);
        }
        try {
            Python py = Python.getInstance();
            String path = context.getExternalFilesDir(null).toString();
            py.getModule("sys").get("path").callAttr("append", path);
            PyObject m = py.getModule("extract");
            extractor = m.callAttr("Extractor");
        } catch (PyException e){
            Log.e(TAG, "yt-dlp 載入失敗");
            new File(context.getExternalFilesDir(null), "yt-dlp").delete();
            throw e;
        }
    }

    /**
     * Check if yt-dlp is downloaded
     * @param context Context
     * @return true if yt-dlp is downloaded, false otherwise
     */
    public static boolean isDownloaded(Context context){
        File file = new File(context.getExternalFilesDir(null), "yt-dlp");
        return file.exists();
    }

    /**
     * Download yt-dlp binary to external storage
     * @param context Context
     * @return true if download is successful, false otherwise
     */
    public static boolean download(Context context){
        try {
            File file = new File(context.getExternalFilesDir(null), "yt-dlp.part");
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            URL url = new URL("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            FileOutputStream fileOutput = new FileOutputStream(file);
            InputStream inputStream = urlConnection.getInputStream();

            byte[] buffer = new byte[1024];
            int bufferLength = 0;

            while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
                fileOutput.write(buffer, 0, bufferLength);
            }
            fileOutput.close();
            file.renameTo(new File(context.getExternalFilesDir(null), "yt-dlp"));
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Extract video url
     * @param url url to extract
     * @param option python dictionary of options for yt-dlp
     * @return extracted video url
     * @throws PyException extraction failed
     * @see <a href="https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/YoutubeDL.py#L184">list of options</a>
     */
    public String extract(String url, PyObject option) throws PyException {
        return extractor.callAttr("extract", url, option).toString();
    }
}
