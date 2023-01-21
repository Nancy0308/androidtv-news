package io.github.anenasa.news;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SettingsActivity extends AppCompatActivity {

    String defaultFormat;
    String defaultVolume;
    final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        defaultFormat = getIntent().getExtras().getString("defaultFormat");
        defaultVolume = getIntent().getExtras().getString("defaultVolume");
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, new SettingsFragment())
                .commit();
    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("defaultFormat", defaultFormat);
        returnIntent.putExtra("defaultVolume", defaultVolume);
        setResult(Activity.RESULT_OK, returnIntent);
        super.onBackPressed();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SettingsActivity activity;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            activity = (SettingsActivity)getActivity();
            setPreferencesFromResource(R.xml.activity_settings, rootKey);

            Preference config = findPreference("config");
            assert config != null;
            config.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/*");
                startActivityForResult(intent, 0);
                return true;
            });

            EditTextPreference prefConfigurl = findPreference("configurl");
            assert prefConfigurl != null;
            prefConfigurl.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!newValue.toString().isEmpty()) {
                    try {
                        File file = new File(requireActivity().getExternalFilesDir(null), "config.txt");
                        // Without deleting first, when config.txt is already created with adb push,
                        // writing will fail with "java.io.FileNotFoundException" "open failed: EACCES (Permission denied)"
                        file.delete();
                        FileOutputStream stream = new FileOutputStream(file);
                        stream.write(("{\"channelList\": [{\"list\": \"" + newValue + "\"}]}").getBytes());
                    } catch (IOException e) {
                        Log.e(activity.TAG, Log.getStackTraceString(e));
                        preference.setSummary(e.toString());
                    }
                }
                return true;
            });

            EditTextPreference prefFormat = findPreference("format");
            assert prefFormat != null;
            prefFormat.setSummary(activity.defaultFormat);
            prefFormat.setText(activity.defaultFormat);
            prefFormat.setOnPreferenceChangeListener((preference, newValue) -> {
                if(newValue.toString().isEmpty()){
                    activity.defaultFormat = "best";
                    preference.setSummary("best");
                }
                else {
                    activity.defaultFormat = newValue.toString();
                    preference.setSummary(newValue.toString());
                }
                return true;
            });

            EditTextPreference prefVolume = findPreference("volume");
            assert prefVolume != null;
            prefVolume.setSummary(activity.defaultVolume);
            prefVolume.setText(activity.defaultVolume);
            prefVolume.setOnPreferenceChangeListener((preference, newValue) -> {
                if(newValue.toString().isEmpty()){
                    activity.defaultVolume = "1.0";
                    preference.setSummary("1.0");
                }
                else {
                    activity.defaultVolume = newValue.toString();
                    preference.setSummary(newValue.toString());
                }
                return true;
            });

            Preference update = findPreference("update");
            assert update != null;
            update.setOnPreferenceClickListener(preference -> {
                update.setSummary("正在檢查更新");
                new Thread(() -> {
                    try {
                        URL url = new URL("https://raw.githubusercontent.com/anenasa/androidtv-news/main/VERSION");
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestMethod("GET");
                        urlConnection.setReadTimeout(10000);
                        urlConnection.setConnectTimeout(15000);
                        urlConnection.setDoOutput(true);
                        urlConnection.connect();
                        InputStream inputStream = url.openStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        String version_new = reader.readLine();
                        if(!version_new.equals(BuildConfig.VERSION_NAME.split("-")[0])){
                            activity.runOnUiThread(() -> update.setSummary("有新版本"));
                            String link = "https://github.com/anenasa/androidtv-news/releases/tag/v" + version_new;
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                            startActivity(intent);
                        }
                        else{
                            activity.runOnUiThread(() -> update.setSummary("已經是最新版本"));
                        }
                    } catch (IOException e) {
                        activity.runOnUiThread(() -> update.setSummary("更新時發生錯誤"));
                        Log.e(activity.TAG, Log.getStackTraceString(e));
                    }
                }).start();
                return true;
            });

            Preference about = findPreference("about");
            assert about != null;
            about.setOnPreferenceClickListener(preference -> {
                InputStream streamGPL3 = getResources().openRawResource(R.raw.gpl3);
                InputStream streamApache2 = getResources().openRawResource(R.raw.apache2);
                BufferedReader readerGPL3 = new BufferedReader(new InputStreamReader(streamGPL3));
                BufferedReader readerApache2 = new BufferedReader(new InputStreamReader(streamApache2));
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(String.format("新聞直播 %s 版", BuildConfig.VERSION_NAME)).append('\n')
                        .append("專案頁面：https://github.com/anenasa/androidtv-news").append('\n')
                        .append("授權：GNU General Public License v3.0").append('\n')
                        .append("函式庫：").append('\n')
                        .append("youtubedl-android - GNU General Public License v3.0").append('\n')
                        .append("ExoPlayer - Apache License 2.0").append('\n')
                        .append("OkHttp - Apache License 2.0").append('\n')
                        .append('\n');
                try{
                    for (String line; (line = readerGPL3.readLine()) != null; ) {
                        stringBuilder.append(line).append('\n');
                    }
                    for (String line; (line = readerApache2.readLine()) != null; ) {
                        stringBuilder.append(line).append('\n');
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(preference.getContext())
                        .setTitle("關於")
                        .setMessage(stringBuilder.toString())
                        .setPositiveButton("確定", (dialog, id) -> dialog.dismiss());
                        AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return true;
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == Activity.RESULT_OK && resultData != null) {
            Uri uri = resultData.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                File outputFile = new File(getExternalFilesDir(null), "config.txt");
                OutputStream outputStream = new FileOutputStream(outputFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }
    }
}
