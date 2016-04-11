package name.caiyao.microreader.ui.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;

import com.bugtags.library.Bugtags;
import com.jaeger.library.StatusBarUtil;
import com.umeng.analytics.MobclickAgent;

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

    public void setToolBar(Toolbar toolbar, boolean isChangeToolbar, boolean isChangeStatusBar, DrawerLayout drawerLayout) {
        int vibrantColor = getSharedPreferences(SharePreferenceUtil.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE).getInt(SharePreferenceUtil.VIBRANT, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Config.isChangeNavColor(this))
                getWindow().setNavigationBarColor(vibrantColor);
            else
                getWindow().setNavigationBarColor(Color.BLACK);
        }
        if (isChangeToolbar)
            toolbar.setBackgroundColor(vibrantColor);
        if (isChangeStatusBar) {
            if (Config.isImmersiveMode(this))
                StatusBarUtil.setColorNoTranslucent(this, vibrantColor);
            else
                StatusBarUtil.setColor(this, vibrantColor);
        }
        if (drawerLayout != null) {
            if (Config.isImmersiveMode(this))
                StatusBarUtil.setColorNoTranslucentForDrawerLayout(this, drawerLayout, vibrantColor);
            else
                StatusBarUtil.setColorForDrawerLayout(this, drawerLayout, vibrantColor);
        }
    }
}
