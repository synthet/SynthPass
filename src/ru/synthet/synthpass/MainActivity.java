package ru.synthet.synthpass;
/*
 * Copyright 2013 Vladimir Synthet
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
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.TabHost;
import android.widget.TextView;

public class MainActivity extends TabActivity {

    private final String TAG = getClass().getName();
    private TabHost tabHost;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeWindow();
        initializeApp();
    }

    private void initializeApp() {
        // init Tabs
        tabHost = getTabHost();
        tabHost.setup();

        TabHost.TabSpec tabSpec;
        Intent intent = getIntent();
        String id = intent.getStringExtra("SYNTHPASS_APPTASK");
        if (id == null) id = "NORMAL";
        // PassGen
        tabSpec = tabHost.newTabSpec("tag1");
        tabSpec.setIndicator(getResources().getString(R.string.generator));
        intent = new Intent(this, PassGenActivity.class);
        intent.putExtra("SYNTHPASS_APPTASK", id);
        tabSpec.setContent(intent);
        tabHost.addTab(tabSpec);
        // PassShake
        tabSpec = tabHost.newTabSpec("tag2");
        tabSpec.setIndicator(getResources().getString(R.string.random));
        intent = new Intent(this, PassShakeActivity.class);
        intent.putExtra("SYNTHPASS_APPTASK", id);
        tabSpec.setContent(intent);
        tabHost.addTab(tabSpec);
        // Preferences
        tabSpec = tabHost.newTabSpec("tag3");
        tabSpec.setIndicator(getResources().getString(R.string.prefs));
        tabSpec.setContent(new Intent(this, Preferences.class));
        tabHost.addTab(tabSpec);
        // add
        tabHost.setCurrentTab(0);
        //tabHost.getTabWidget().getLayoutParams().height = 50;
        //tabHost.getTabWidget().getChildAt(0).getLayoutParams().height = 50;
        //tabHost.getTabWidget().getChildAt(1).getLayoutParams().height = 50;
        //tabHost.getTabWidget().getChildAt(2).getLayoutParams().height = 50;

    }

    private void initializeWindow() {
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.pass_main);

    }

    // Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        switch (item.getItemId())
        {
            case R.id.menu_about:
                showAbout();
                return true;
            case R.id.menu_settings:
                tabHost.setCurrentTab(2);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void showAbout() {
        View messageView = getLayoutInflater().inflate(R.layout.about, null, false);

        TextView textView = (TextView) messageView.findViewById(R.id.about_credits);
        int defaultColor = textView.getTextColors().getDefaultColor();
        textView.setTextColor(defaultColor);
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "Error", ex);
        }
        String version = null;
        if (pInfo != null) {
            version = pInfo.versionName;
        }
        textView = (TextView) messageView.findViewById(R.id.about_desc);
        textView.setText(getString(R.string.app_name) + " " + version);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(R.string.app_name);
        builder.setView(messageView);
        builder.create();
        builder.show();
    }
}