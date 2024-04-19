package com.wrlus.module.sandbox.hook;

import android.os.UserHandle;
import android.util.Log;

import com.wrlus.xposed.framework.HookInterface;
import com.wrlus.xposed.framework.MethodHook;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookSysServer implements HookInterface {
    private static final String TAG = "HookSysServer";
    @Override
    public void onHookPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (loadPackageParam.packageName.equals("android")) {
            MethodHook hooker = new MethodHook.Builder(
                    "com.android.server.policy.PermissionPolicyService$Internal", loadPackageParam.classLoader)
                    .setMethodName("shouldForceShowNotificationPermissionRequest")
                    .addParameter(String.class)
                    .addParameter(UserHandle.class)
                    .setCallback(new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Log.i(TAG, "Previous result: " + param.getResult());
                            param.setResult(true);
                        }
                    }).build();
            MethodHook.normalInstall(hooker);
        }
    }
}
