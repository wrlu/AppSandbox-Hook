package com.wrlus.module.sandbox.hook;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.wrlus.xposed.framework.HookInterface;
import com.wrlus.xposed.framework.MethodHook;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookIntent implements HookInterface {
    private static final String TAG_ACTIVITY = "HookActivity";
    private static final String TAG_SERVICE = "HookService";
    @Override
    public void onHookPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam.packageName.equals("android")) {
            MethodHook startActivityHooker = new MethodHook.Builder(
                    "com.android.server.wm.ActivityStarter", loadPackageParam.classLoader)
                    .setMethodName("executeRequest")
                    .addParameter("com.android.server.wm.ActivityStarter$Request")
                    .setCallback(new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object request = param.args[0];
                            Class<?> requestClass = XposedHelpers.findClassIfExists(
                                    "com.android.server.wm.ActivityStarter$Request",
                                    loadPackageParam.classLoader);
                            if (requestClass == null) {
                                Log.e(TAG_ACTIVITY, "requestClass == null");
                                return;
                            }
                            Field intentField = XposedHelpers.findFieldIfExists(requestClass,
                                    "intent");
                            if (intentField == null) {
                                Log.e(TAG_ACTIVITY, "intentField == null");
                                return;
                            }
                            Intent intent = (Intent) intentField.get(request);
                            try {
                                logIntent(intent, "startActivity", TAG_ACTIVITY);
                            } catch (Exception e) {
                                Log.e(TAG_ACTIVITY, "Cannot log Intent.", e);
                            }
                        }
                    }).build();
            MethodHook startServiceHooker = new MethodHook.Builder(
                    "com.android.server.am.ActiveServices", loadPackageParam.classLoader)
                    .setMethodName("startServiceInnerLocked")
                    .addParameter("com.android.server.am.ActiveServices$ServiceMap")
                    .addParameter(Intent.class)
                    .addParameter("com.android.server.am.ServiceRecord")
                    .addParameter(boolean.class) //callerFg
                    .addParameter(boolean.class) //addToStarting
                    .setCallback(new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Intent service = (Intent) param.args[1];
                            try {
                                logIntent(service, "startService", TAG_SERVICE);
                            } catch (Exception e) {
                                Log.e(TAG_SERVICE, "Cannot log Intent.", e);
                            }
                        }
                    }).build();
            MethodHook bindServiceHooker = new MethodHook.Builder(
                    "com.android.server.am.ActiveServices", loadPackageParam.classLoader)
                    .setMethodName("bindServiceLocked")
                    .addParameter("android.app.IApplicationThread") //caller
                    .addParameter(IBinder.class) //token
                    .addParameter(Intent.class) //service
                    .addParameter(String.class) //resolveType
                    .addParameter("android.app.IServiceConnection") //connection
                    .addParameter(int.class) //flags
                    .addParameter(String.class) //instanceName
                    .addParameter(String.class) //callingPackage
                    .addParameter(int.class) //userId
                    .setCallback(new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Intent service = (Intent) param.args[2];
                            try {
                                logIntent(service, "bindService", TAG_SERVICE);
                            } catch (Exception e) {
                                Log.e(TAG_SERVICE, "Cannot log Intent.", e);
                            }
                        }
                    }).build();
            MethodHook.normalInstall(startActivityHooker, startServiceHooker, bindServiceHooker);
        }
    }

    public void logIntent(Intent intent, String operation, String tag) {
        Log.i(tag, operation);
        if (intent != null) {
            Log.i(tag, intent.toString());
            if (intent.getData() != null && !intent.getDataString().isEmpty()) {
                Log.i(tag, intent.getDataString());
            }
            Bundle extras = intent.getExtras();
            logBundle(extras, true, tag);
        } else {
            Log.e(tag, "intent == null");
        }
    }

    public void logBundle(Bundle bundle, boolean isExtra, String tag) {
        if (bundle != null) {
            String prefix = isExtra ? "extra" : "bundle";
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value != null) {
                    // Another bundle object
                    Class<?> clazz = value.getClass();
                    if (clazz.getName().equals(Bundle.class.getName())) {
                        logBundle((Bundle) value, false, tag);
                    } else if (clazz.isArray()){
                        Class<?> arrayCompType = clazz.getComponentType();
                        if (arrayCompType != null && !arrayCompType.isPrimitive()) {
                            List<Object> valueList = Arrays.asList((Object[]) value);
                            Log.i(tag, "[" + prefix + "] " +
                                    key + " `" + clazz.getName() + "`: " +
                                    logValue(valueList));
                        } else {
                            Log.i(tag, "[" + prefix + "] " +
                                    key + " `" + clazz.getName() + "`: " +
                                    logValue(value));
                        }

                    } else {
                        Log.i(tag, "[" + prefix + "] " +
                                key + " `" + clazz.getName() + "`: " +
                                logValue(value));
                    }
                } else {
                    Log.i(tag, "[" + prefix + "] " +
                            key + ": null");
                }
            }
        }
    }

    public String logValue(Object value) {
        return value.toString();
    }
}
