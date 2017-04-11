package com.example.peteratt.bluetoothtester;

import android.app.Application;
import android.content.Context;

/**
 * Holds the reference to the application object.
 *
 * Created by peteratt on 4/11/17.
 */
public class BluetoothTesterApplication extends Application {

    private static Context appContext;

    public static Context getAppContext() {
        return appContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }
}
