package com.wrlus.xposed.framework;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class MethodHook {
    private static final String TAG = "MethodHook";
    public static final String CONSTRUCTOR = "$new";
    protected final String className;
    protected final ClassLoader classLoader;
    protected final String methodName;
    protected Object[] parameterTypes;
    protected XC_MethodHook callback;
    private XC_MethodHook.Unhook hookTarget;

    public static class Status {
        public static final int SUCCESS = 0;
        public static final int FAILED_CLASS_NOT_FOUND = 1;
        public static final int FAILED_NO_SUCH_METHOD = 2;
    }
    protected MethodHook(Builder builder) {
        this.className = builder.className;
        this.classLoader = builder.classLoader;
        this.methodName = builder.methodName;
        this.parameterTypes = builder.parameterTypes.toArray();
        this.callback = builder.callback;
    }

    public static class Builder {
        private final String className;
        private final ClassLoader classLoader;
        private String methodName;
        private final List<Object> parameterTypes;
        private XC_MethodHook callback;
        public Builder(String className, ClassLoader classLoader) {
            this.className = className;
            this.classLoader = classLoader;
            parameterTypes = new ArrayList<>();
        }

        public Builder(Class<?> clazz) {
            this.className = clazz.getCanonicalName();
            this.classLoader = ClassLoader.getSystemClassLoader();
            parameterTypes = new ArrayList<>();
        }

        public Builder setMethodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public synchronized Builder addParameter(Object parameterType) {
            parameterTypes.add(parameterType);
            return this;
        }

        public synchronized Builder addParameters(Object[] parameterTypes) {
            this.parameterTypes.addAll(Arrays.asList(parameterTypes));
            return this;
        }

        public Builder setCallback(XC_MethodHook callback) {
            this.callback = callback;
            return this;
        }

        public MethodHook build() {
            return new MethodHook(this);
        }
    }

    public void install() {
        if (hasInstall()) {
            Log.w(TAG, "Method hook [className = " + className +
                    ", methodName = " + methodName + "] already exists," +
                    "try to unhook the old one.");
            uninstall();
        }
        List<Object> parameterTypesAndCallback =
                new ArrayList<>(Arrays.asList(parameterTypes));
        parameterTypesAndCallback.add(callback);
        if (CONSTRUCTOR.equals(methodName)) {
            hookTarget = XposedHelpers.findAndHookConstructor(className, classLoader,
                    parameterTypesAndCallback.toArray());
        } else {
            hookTarget = XposedHelpers.findAndHookMethod(className, classLoader,
                    methodName, parameterTypesAndCallback.toArray());
        }
        Log.d(TAG, "Install" + this);
    }

    public static void normalInstall(MethodHook... methodHooks) {
        if (methodHooks == null) {
            return;
        }
        for (MethodHook methodHook : methodHooks) {
            normalInstall(methodHook);
        }
    }

    public static void normalInstall(MethodHook methodHook) {
        if (methodHook == null) {
            return;
        }
        try {
            methodHook.install();
        } catch (XposedHelpers.ClassNotFoundError | NoSuchMethodError e) {
            Log.e(TAG, "Failed to install " + methodHook);
        }
    }

    public static void superInstall(String className, ClassLoader classLoader,
                                    MethodHook... methodHooks) {
        throw new UnsupportedOperationException("Unsupported in AppSandbox-Hook!");
    }

    public void uninstall() {
        if (hookTarget != null) {
            hookTarget.unhook();
            Log.d(TAG, "Uninstall" + this);
        }
    }

    public boolean hasInstall() {
        return hookTarget != null;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getParameterTypes() {
        return parameterTypes;
    }

    public XC_MethodHook getCallback() {
        return callback;
    }

    @Override
    public String toString() {
        return "MethodHook{" +
                "className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
