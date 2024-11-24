package io.github.jark006.freezeit.hook;import static io.github.jark006.freezeit.hook.XpUtils.log;import de.robv.android.xposed.IXposedHookLoadPackage;import de.robv.android.xposed.XC_MethodReplacement;import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;import io.github.jark006.freezeit.BuildConfig;import io.github.jark006.freezeit.hook.android.ANRHelperHooks;import io.github.jark006.freezeit.hook.android.ANRErrorStateHook;import io.github.jark006.freezeit.hook.android.AlarmHook;import io.github.jark006.freezeit.hook.android.ANRHook;import io.github.jark006.freezeit.hook.android.BroadCastHook;import io.github.jark006.freezeit.hook.android.FreezeitService;import io.github.jark006.freezeit.hook.android.WakeLockHook;import io.github.jark006.freezeit.hook.app.PowerKeeper;public class Hook implements IXposedHookLoadPackage {    @Override    public void handleLoadPackage(LoadPackageParam lpParam) {        switch (lpParam.packageName) {            case Enum.Package.self:                XpUtils.hookMethod("Freezeit[manager]:", lpParam.classLoader,                        XC_MethodReplacement.returnConstant(true),                        Enum.Class.self, Enum.Method.isXposedActive);                return;            case Enum.Package.android:                hookAndroid(lpParam.classLoader);                return;            case Enum.Package.powerkeeper:                HookPowerKeeper(lpParam.classLoader);                return;            default:        }    }    public void hookAndroid(ClassLoader classLoader) {        log("Freezeit[Xposed]", BuildConfig.VERSION_NAME);        Config config = new Config();        new FreezeitService(config, classLoader);        new AlarmHook(config, classLoader);        new ANRHook(classLoader, config);        new ANRHelperHooks(classLoader, config);        new ANRErrorStateHook(classLoader, config);        new BroadCastHook(classLoader, config);        new WakeLockHook(config, classLoader); //FreezeitService 的 handleWakeLock 暂时不用    }    public void HookPowerKeeper(ClassLoader classLoader) {        if (XpUtils.PowerKeeperHook)        PowerKeeper.Hook(classLoader);    }}