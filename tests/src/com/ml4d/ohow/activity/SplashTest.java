package com.ml4d.ohow.activity;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.ml4d.ohow.activity.SplashTest \
 * com.ml4d.ohow.tests/android.test.InstrumentationTestRunner
 */
public class SplashTest extends ActivityInstrumentationTestCase2<SplashActivity> {

    public SplashTest() {
        super("com.ml4d.ohow", SplashActivity.class);
    }

}
