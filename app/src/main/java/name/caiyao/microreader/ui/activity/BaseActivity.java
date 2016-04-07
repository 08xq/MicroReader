package name.caiyao.microreader.ui.activity;

import android.content.Context;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.bugtags.library.Bugtags;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.umeng.analytics.MobclickAgent;

import name.caiyao.microreader.R;
import name.caiyao.microreader.config.Config;
import name.caiyao.microreader.utils.SharePreferenceUtil;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        Bugtags.onResume(this);
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Bugtags.onPause(this);
        MobclickAgent.onPause(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Bugtags.onDispatchTouchEvent(this, event);
        return super.dispatchTouchEvent(event);
    }

    public void setToolBar(Toolbar toolbar, boolean isChangeStatusBar, boolean isChangeToolbar,boolean marginTop) {
        Config.vibrantColor = getSharedPreferences(SharePreferenceUtil.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE).getInt(SharePreferenceUtil.VIBRANT, ContextCompat.getColor(this, R.color.colorPrimary));
        if (Config.vibrantColor == 0){
            Config.vibrantColor = ContextCompat.getColor(this, R.color.colorPrimary);
        }
        if (isChangeToolbar)
            toolbar.setBackgroundColor(Config.vibrantColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            if (isChangeStatusBar){
                tintManager.setStatusBarTintEnabled(true);
            }
            if (marginTop && Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT){
                SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
                getWindow().getDecorView().getRootView().setPadding(0, toolbar.getHeight(), config.getPixelInsetRight(), 0);
            }
            tintManager.setNavigationBarTintEnabled(true);
            tintManager.setTintColor(Config.vibrantColor);
        }
    }
}
