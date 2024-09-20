package com.wrlus.module.sandbox.hook;

import android.app.ActivityOptions;
import android.content.ClipData;
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
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object request = param.args[0];
                            try {
                                Class<?> requestClass = Class.forName("com.android.server.wm.ActivityStarter$Request",
                                        false, loadPackageParam.classLoader);
                                Field intentField = requestClass.getDeclaredField("intent");
                                intentField.setAccessible(true);
                                Intent intent = (Intent) intentField.get(request);
                                try {
                                    logIntent(intent, "startActivity", TAG_ACTIVITY);
                                } catch (Exception e) {
                                    Log.e(TAG_ACTIVITY, "Cannot log Intent.", e);
                                }

                                Field activityOptionsField = requestClass.getDeclaredField("activityOptions");
                                activityOptionsField.setAccessible(true);
                                Class<?> safeActivityOptionsClass = Class.forName("com.android.server.wm.SafeActivityOptions",
                                        false, loadPackageParam.classLoader);
                                Field originalOptionsField = safeActivityOptionsClass.getDeclaredField("mOriginalOptions");
                                originalOptionsField.setAccessible(true);
                                ActivityOptions options = (ActivityOptions)
                                        originalOptionsField.get(activityOptionsField.get(request));
                                try {
                                    logBundle(options.toBundle(), "ActivityOptions", TAG_ACTIVITY);
                                } catch (Exception e) {
                                    Log.e(TAG_ACTIVITY, "Cannot log Intent.", e);
                                }
                            } catch (ReflectiveOperationException e) {
                                Log.e(TAG_ACTIVITY, "ReflectiveOperationException", e);
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

    public static void logIntent(Intent intent, String operation, String tag) {
        Log.i(tag, operation);
        if (intent != null) {
            Log.i(tag, "[Intent] " + intent);
            if (intent.getData() != null && !intent.getDataString().isEmpty()) {
                Log.i(tag, "[Data] " + intent.getDataString());
            }
            if (intent.getClipData() != null) {
                ClipData clipData = intent.getClipData();
                for (int i = 0; i < clipData.getItemCount(); ++i) {
                    ClipData.Item item = clipData.getItemAt(i);
                    Log.i(tag, "[ClipData] " + item.getText());
                }
            }
            Bundle extras = intent.getExtras();
            logBundle(extras, "Extras", tag);
        } else {
            Log.e(tag, "intent == null");
        }
    }

    public static void logBundle(Bundle bundle, String operation, String tag) {
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                if (value != null) {
                    // Another bundle object
                    Class<?> clazz = value.getClass();
                    if (clazz.getName().equals(Bundle.class.getName())) {
                        Log.i(tag, "[" + operation + "] " +
                                key + " `" + clazz.getName() + "`: ");
                        logBundle((Bundle) value, "Bundle", tag);
                    } else if (clazz.isArray()){
                        Class<?> arrayCompType = clazz.getComponentType();
                        if (arrayCompType != null && !arrayCompType.isPrimitive()) {
                            List<Object> valueList = Arrays.asList((Object[]) value);
                            Log.i(tag, "[" + operation + "] " +
                                    key + " `" + clazz.getName() + "`: " +
                                    logValue(valueList));
                        } else {
                            Log.i(tag, "[" + operation + "] " +
                                    key + " `" + clazz.getName() + "`: " +
                                    logValue(value));
                        }
                    } else {
                        Log.i(tag, "[" + operation + "] " +
                                key + " `" + clazz.getName() + "`: " +
                                logValue(value));
                    }
                } else {
                    Log.i(tag, "[" + operation + "] " +
                            key + ": null");
                }
            }
        }
    }

    public static String logValue(Object value) {
        return value.toString();
    }
}
