package com.wrlus.xposed.util;

import android.util.Log;

public class Debug {
    public static void printStackTrace(String tag) {
        try {
            throw new NullPointerException("get stack");
        } catch (NullPointerException e) {
            Log.v(tag, "[StackTrace] ", e);
        }
    }
}
