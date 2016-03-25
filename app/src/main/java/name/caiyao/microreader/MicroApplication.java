package name.caiyao.microreader;

import android.app.Application;
import android.content.Context;

import com.apkfuns.logutils.LogLevel;
import com.apkfuns.logutils.LogUtils;
import com.bugtags.library.Bugtags;
import com.bugtags.library.BugtagsOptions;
import com.umeng.analytics.MobclickAgent;

/**
 * Created by 蔡小木 on 2016/3/9 0009.
 */
public class MicroApplication extends Application {

    public static MicroApplication microApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.getLogConfig()
                .configAllowLog(true)
                .configTagPrefix("MicroReader-")
                .configShowBorders(true)
                .configLevel(LogLevel.TYPE_VERBOSE);

        BugtagsOptions options = new BugtagsOptions.Builder().
                trackingLocation(true).//是否获取位置
                trackingCrashLog(true).//是否收集crash
                trackingConsoleLog(true).//是否收集console log
                trackingUserSteps(true).//是否收集用户操作步骤
                build();
        Bugtags.start("9c1b1a3234ceeb5b9c531177a93b65ec", this, Bugtags.BTGInvocationEventNone, options);
        MobclickAgent.setCatchUncaughtExceptions(false);
        microApplication = this;
    }

    public static Context getContext(){
        return microApplication;
    }
}
