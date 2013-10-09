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
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import ru.synthet.synthpass.synthkeyboard.KeyboardData;
import ru.synthet.synthpass.synthkeyboard.KeyboardDataBuilder;

//import android.os.StrictMode;
//import org.openintents.sensorsimulator.hardware.Sensor;
//import org.openintents.sensorsimulator.hardware.SensorEvent;
//import org.openintents.sensorsimulator.hardware.SensorEventListener;
//import org.openintents.sensorsimulator.hardware.SensorManagerSimulator;

//AndroidManifest.xml: <uses-permission android:name="android.permission.INTERNET"/>

public class PassShakeActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SensorEventListener mEventListenerAccelerometer;
    private SensorEventListener mEventListenerMagneticField;
    private TextView textStatus;
    private TextView entropyCount;
    private ProgressBar progressBar;
    private Button genButton;
    private final PassGenerator passGenerator = new PassGenerator();
    private int currentBytes;
    private String collectedEntropy = "";
    /* Here we store the current values of acceleration, one for each axis */
    private float[] xAccel = new float[2];
    private float[] yAccel = new float[2];
    private float[] zAccel = new float[2];
    /* And here the previous ones */
    private float[] xPreviousAccel = new float[2];
    private float[] yPreviousAccel = new float[2];
    private float[] zPreviousAccel = new float[2];
    private boolean[] firstUpdateAccel = {true, true};
    private final float[] shakeThreshold = {0.2f, 5.0f};
    private boolean passwordReady = false;

    private SensorManager mSensorManager;
    //private SensorManagerSimulator mSensorManager;

    private void initializeApp() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy);
        //mSensorManager = SensorManagerSimulator.getSystemService(this, SENSOR_SERVICE);
        //mSensorManager.connectSimulator();

        initListeners();
        // Preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        // инициализация элементов
        genButton    = (Button) findViewById(R.id.button);
        progressBar  = (ProgressBar) findViewById(R.id.progressBar);
        textStatus   = (TextView) findViewById(R.id.textStatus);
        entropyCount = (TextView) findViewById(R.id.entropyCount);
        // обработчик кнопки genButton
        View.OnClickListener genTapListener = new View.OnClickListener() {
            public void onClick(View v) {
                refreshData();
                updateView();
            }
        };
        genButton.setOnClickListener(genTapListener);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeWindow();
        initializeApp();

        if (savedInstanceState != null) {
            collectedEntropy = savedInstanceState.getString("collectedEntropy");
            currentBytes     = savedInstanceState.getInt("currentBytes", 0);
            updateView();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        getPrefs(prefs);
    }

    private void getPrefs(SharedPreferences prefs) {
        PassGenerator.PassRules.requireSpecialSymbols   = prefs.getBoolean("cbReqSpecial", true);
        PassGenerator.PassRules.requireDigits           = prefs.getBoolean("cbReqDigits", true);
        PassGenerator.PassRules.generatedPasswordLength = prefs.getInt("count", 12);
        PassGenerator.PassRules.requireUppercaseLetters = prefs.getBoolean("cbUpperCase", true);
        PassGenerator.PassRules.requireLowercaseLetters = prefs.getBoolean("cbLowerCase", true);
        updateView();
    }

    private void updateView() {
        if (collectedEntropy.length() == 0)
            textStatus.setText(R.string.shake_phone);
        else
            textStatus.setText(collectedEntropy);
        progressBar.setMax(PassGenerator.PassRules.generatedPasswordLength);
        progressBar.setProgress(currentBytes);
        entropyCount.setText("");
        // testing lines
        //progressBar.setProgress(12);
        //PassGenActivity.showDecoratedPassword("o1L*0)liOx@I", textStatus);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("collectedEntropy", collectedEntropy);
        outState.putInt("currentBytes", currentBytes);
    }

    private void initializeWindow() {
        setContentView(R.layout.pass_shake);
    }

    private void onSensorChange(SensorEvent se, int type) {
        if (!passwordReady) {
            updateAccelParameters(se.values[0], se.values[1], se.values[2], type);
            if ((isAccelerationChanged(type)) && (!firstUpdateAccel[type]))
                executeShakeAction();
        }
    }

    private void initListeners() {
        mEventListenerAccelerometer = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent se) {
                onSensorChange(se, 0);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        mEventListenerMagneticField = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent se) {
                onSensorChange(se, 1);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
    }

    private static String removeDuplicates(String s) {
        StringBuilder noDupes = new StringBuilder();
        int strLen = s.length();
        for (int i = 0; i < strLen-1; i++) {
            if (s.charAt(i) != s.charAt(i+1))
                noDupes.append(s.charAt(i));
        }
        if (strLen != 0) {
            noDupes.append(s.charAt(strLen-1));
            return noDupes.toString();
        } else {
            return s;
        }
    }

    private void executeShakeAction() {
        if (currentBytes < PassGenerator.PassRules.generatedPasswordLength) {
            float deltaAX = Math.abs(xPreviousAccel[0] - xAccel[0]);
            float deltaAY = Math.abs(yPreviousAccel[0] - yAccel[0]);
            float deltaAZ = Math.abs(zPreviousAccel[0] - zAccel[0]);
            float deltaMX = Math.abs(xPreviousAccel[1] - xAccel[1]);
            float deltaMY = Math.abs(yPreviousAccel[1] - yAccel[1]);
            float deltaMZ = Math.abs(zPreviousAccel[1] - zAccel[1]);
            collectedEntropy += passGenerator.getShakedString(new Float[]{
                    deltaAX, deltaAY, deltaAZ, deltaMX, deltaMY, deltaMZ
            });
            collectedEntropy = removeDuplicates(collectedEntropy);
            currentBytes = collectedEntropy.length();
            progressBar.setProgress(currentBytes);
            textStatus.setText(collectedEntropy);
            if (PassGenerator.PassRules.generatedPasswordLength == currentBytes) {
                showPassword();
            }
        } else {
            if (PassGenerator.PassRules.generatedPasswordLength < currentBytes) {
                collectedEntropy = collectedEntropy.substring(0,PassGenerator.PassRules.generatedPasswordLength);
            }
            showPassword();
        }
        PassGenActivity.showDecoratedPassword(collectedEntropy, textStatus);
    }

    private void showPassword() {
        passwordReady = true;
        KeyboardData.entryName = getString(R.string.random_entry);
        KeyboardDataBuilder kbdataBuilder = new KeyboardDataBuilder();
        kbdataBuilder.addPair(getString(R.string.last_password), collectedEntropy);
        kbdataBuilder.commit();
        genButton.setVisibility(View.VISIBLE);
        Intent intent = getIntent();
        String id = intent.getStringExtra("SYNTHPASS_APPTASK");
        if (id != null) {
            if (id.equals("GENERATE")) {
                finish();
            }
        }
    }

    private boolean isAccelerationChanged(int type) {
        float deltaX = Math.abs(xPreviousAccel[type] - xAccel[type]);
        float deltaY = Math.abs(yPreviousAccel[type] - yAccel[type]);
        float deltaZ = Math.abs(zPreviousAccel[type] - zAccel[type]);
        return (deltaX > shakeThreshold[type]) & (deltaZ > shakeThreshold[type]) & (deltaY > shakeThreshold[type]);
    }

    private void updateAccelParameters(float xNewAccel, float yNewAccel,float zNewAccel, int type) {
        if (firstUpdateAccel[type]) {
            xPreviousAccel[type] = xNewAccel;
            yPreviousAccel[type] = yNewAccel;
            zPreviousAccel[type] = zNewAccel;
            firstUpdateAccel[type] = false;
            genButton.setVisibility(View.INVISIBLE);
        } else {
            xPreviousAccel[type] = xAccel[type];
            yPreviousAccel[type] = yAccel[type];
            zPreviousAccel[type] = zAccel[type];
        }
        xAccel[type] = xNewAccel;
        yAccel[type] = yNewAccel;
        zAccel[type] = zNewAccel;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startListeners();
        getPrefs(PreferenceManager.getDefaultSharedPreferences(this));
    }

    @Override
    protected void onPause() {
        stopListeners();
        refreshData();
        super.onPause();
    }

    @Override
    protected void onStop() {
        stopListeners();
        super.onStop();
    }

    private void startListeners() {
        mSensorManager.registerListener(mEventListenerAccelerometer,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mEventListenerMagneticField,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void stopListeners() {
        mSensorManager.unregisterListener(mEventListenerAccelerometer);
        mSensorManager.unregisterListener(mEventListenerMagneticField);
    }

    private void refreshData() {
        if (passwordReady) {
            collectedEntropy = "";
            currentBytes = 0;
            passwordReady = false;
        }
        firstUpdateAccel[0] = true;
        firstUpdateAccel[1] = true;
    }

}