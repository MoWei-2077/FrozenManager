package io.github.jark006.freezeit.hook.android;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import io.github.jark006.freezeit.base.AbstractMethodHook;
import io.github.jark006.freezeit.hook.Config;
import io.github.jark006.freezeit.hook.Enum;
import io.github.jark006.freezeit.hook.XpUtils;

public class ANRHelperHooks {
    public final Integer findIndex(Class<?>[] parameterTypes, String clazz) {
        for (int i = 0; i < parameterTypes.length; i++)
            if (clazz.equals(parameterTypes[i].getName()))
                return i;
        return null;
    }

    public ANRHelperHooks(ClassLoader classLoader, Config config) {
        try {
            Class<?> AnrHelperClass = XposedHelpers.findClassIfExists("com.android.server.am.AnrHelper", classLoader);

            if (AnrHelperClass == null)
                return;

            int success = 0;
            for (Method method : AnrHelperClass.getDeclaredMethods()) {
                boolean isAppNotResponding = method.getName().equals("appNotResponding");
                boolean isDeferAppNotResponding = method.getName().equals("deferAppNotResponding"); // MIUI / HyperOS 的方法体
                if ((isAppNotResponding || isDeferAppNotResponding) && method.getReturnType().equals(void.class)) { // 只处理void为返回类型的方法体
                    Integer processRecordIndex = findIndex(method.getParameterTypes(), "com.android.server.am.ProcessRecord");
                    if (processRecordIndex == null) { // 没找到进程记录就找Anr记录
                        Integer anrRecordIndex = findIndex(method.getParameterTypes(), "com.android.server.am.AnrHelper$AnrRecord");
                        if (anrRecordIndex != null) {
                            XposedBridge.hookMethod(method, new AbstractMethodHook() {
                                @Override
                                protected void beforeMethod(MethodHookParam param) {
                                    Object anrRecord = param.args[anrRecordIndex];
                                    if (anrRecord == null)
                                        return;
                                    Object app = XposedHelpers.getObjectField(anrRecord, "mApp");
                                    if (app == null)
                                        return;
                                    final int uid = config.getProcessRecordUid(app);// processRecord
                                    if (!config.managedApp.contains(uid))
                                        return;
                                    param.setResult(null);
                                    if (XpUtils.DEBUG_ANR)
                                        XpUtils.log("Frozen[AnrHook]:", "跳过 ANR:" + XpUtils.getString(app, Enum.Field.processName));
                                }
                            });
                            success++;
                        }
                    } else {
                        XposedBridge.hookMethod(method, new AbstractMethodHook() {
                            @Override
                            protected void beforeMethod(MethodHookParam param) {
                                Object record = param.args[processRecordIndex];
                                if (record == null)
                                    return;
                                final int uid = config.getProcessRecordUid(record);// processRecord
                                if (!config.managedApp.contains(uid))
                                    return;
                                param.setResult(null);
                                if (XpUtils.DEBUG_ANR)
                                    XpUtils.log("Frozen[AnrHook]:", "跳过 ANR:" + XpUtils.getString(record, Enum.Field.processName));
                            }
                        });
                        success++;
                    }
                }
            }

            if (success > 0)
                XpUtils.log("Frozen[Hook]:", "拦截应用无响应");
        } catch (Throwable throwable) {
            XpUtils.log("Frozen[Hook]:", throwable.getMessage());
        }
    }
}
