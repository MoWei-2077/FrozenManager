package io.github.jark006.freezeit.hook;import java.lang.reflect.Constructor;import java.lang.reflect.Field;import java.lang.reflect.Method;import java.util.Arrays;import de.robv.android.xposed.XC_MethodHook;import de.robv.android.xposed.XposedBridge;import de.robv.android.xposed.XposedHelpers;import io.github.jark006.freezeit.Utils;public class XpUtils {    public final static boolean DEBUG_WAKEUP_LOCK = false;    public final static boolean DEBUG_BROADCAST = false;    public final static boolean DEBUG_ALARM = false;    public final static boolean DEBUG_ANR = false;    public final static boolean DEBUG_PENDING_UID = false;    public final static boolean PowerKeeperHook = false;    static final int maxLogLength = 16000; // 16K 非KiB    public static StringBuilder xpLogContent = new StringBuilder(maxLogLength); // StringBuffer?    public static void log(final String TAG, final String content) {        // XposedBridge.log(TAG + content);        if (xpLogContent.length() + TAG.length() + content.length() + 20 > maxLogLength)            xpLogContent.setLength(0);        var timeStamp = System.currentTimeMillis() / 1000 + 8 * 3600; //UTC+8        var hour = (timeStamp / 3600) % 24;        var min = (timeStamp % 3600) / 60;        var sec = timeStamp % 60;        if(hour < 10) xpLogContent.append('0');        xpLogContent.append(hour).append(':');        if(min < 10) xpLogContent.append('0');        xpLogContent.append(min).append(':');        if(sec < 10) xpLogContent.append('0');        xpLogContent.append(sec).append(' ');        xpLogContent.append(TAG).append(": ").append(content).append('\n');    }    public static boolean hookMethod(String TAG, ClassLoader classLoader, XC_MethodHook callback,                                     String className, String methodName, Object... parameterTypes) {        Class<?> clazz = XposedHelpers.findClassIfExists(className, classLoader);        if (clazz == null) {            log(TAG, "⚠️Cannot hookMethod: " + methodName + ", cannot find " + className);            return false;        }        Method method = XposedHelpers.findMethodExactIfExists(clazz, methodName, parameterTypes);        if (method == null) {            log(TAG, "⚠️Cannot hookMethod: " + methodName);            return false;        }        XposedBridge.hookMethod(method, callback);        log(TAG, "Success hookMethod: " + methodName);        return true;    }    public static void hookConstructor(String TAG, ClassLoader classLoader, XC_MethodHook callback,                                       String className, Object... parameterTypes) {        Class<?> clazz = XposedHelpers.findClassIfExists(className, classLoader);        if (clazz == null) {            log(TAG, "⚠️Cannot hookConstructor, cannot find " + className);            return;        }        Constructor<?> constructor = XposedHelpers.findConstructorExact(clazz, parameterTypes);        if (constructor == null) {            log(TAG, "⚠️Cannot hookConstructor: " + className);            return;        }        XposedBridge.hookMethod(constructor, callback);        log(TAG, "Success hookConstructor: " + className);    }    public static int getInt(final Object obj, final String fieldName) {        try {            Field field = obj.getClass().getDeclaredField(fieldName);            field.setAccessible(true);            return field.getInt(obj);        } catch (Exception e) {            log("Freezeit[getInt]", "获取失败 " + obj.getClass().getName() + "#" + fieldName + ": " + e);            return -1;        }    }    public static boolean getBoolean(final Object obj, final String fieldName) {        try {            Field field = obj.getClass().getDeclaredField(fieldName);            field.setAccessible(true);            return field.getBoolean(obj);        } catch (Exception e) {            log("Freezeit[getBoolean]", "获取失败 " + obj.getClass().getName() + "#" + fieldName + ": " + e);            return false;        }    }    public static String getString(final Object obj, final String fieldName) {        try {            Field field = obj.getClass().getDeclaredField(fieldName);            field.setAccessible(true);            return (String) field.get(obj);        } catch (Exception e) {            log("Freezeit[getString]", "获取失败 " + obj.getClass().getName() + "#" + fieldName + ": " + e);            return "null";        }    }    // 少量元素(0-10)时，clear,add,contain 性能均优于 HashSet, TreeSet    public static class VectorSet {        int size = 0, maxSize;        int[] vector;        public VectorSet(int maxSize) {            this.maxSize = maxSize;            vector = new int[maxSize];        }        public int size() {            return size;        }        public boolean isEmpty() {            return size == 0;        }        public void clear() {            size = 0;        }        public void add(final int n) {            for (int i = 0; i < size; i++) {                if (vector[i] == n) return;            }            if (size < maxSize)                vector[size++] = n;        }        public void erase(final int n) {            for (int i = 0; i < size; i++) {                if (vector[i] == n) {                    vector[i] = vector[--size];                    return;                }            }        }        // 顺序查找        public boolean contains(final int n) {            if (n < 10000) return false;            for (int i = 0; i < size; i++) {                if (vector[i] == n)                    return true;            }            return false;        }        public void toBytes(byte[] bytes, int byteOffset) {            if (size > 0)                Utils.Int2Byte(vector, 0, size, bytes, byteOffset);        }    }    // 造轮子：常见UID位于 10000 ~ 14000    // 在 APP UID 范围, 性能均优于HashSet    public static class BucketSet {        final int uidMin = 10000;        final int uidMax = 14000;// 默认最多4千个应用        int size = 0;        boolean[] bucket = new boolean[uidMax - uidMin];        public BucketSet() {            clear();        }        public int size() {            return size;        }        public boolean isEmpty() {            return size == 0;        }        public void clear() {            size = 0;            Arrays.fill(bucket, false);        }        public void add(final int n) {            if (n < uidMin || uidMax <= n)                return;            if (!bucket[n - uidMin]) {                bucket[n - uidMin] = true;                size++;            }        }        public void erase(final int n) {            if (n < uidMin || uidMax <= n)                return;            if (bucket[n - uidMin]) {                bucket[n - uidMin] = false;                size--;            }        }        public boolean contains(final int n) {            if (n < uidMin || uidMax <= n)                return false;            return bucket[n - uidMin];        }    }}