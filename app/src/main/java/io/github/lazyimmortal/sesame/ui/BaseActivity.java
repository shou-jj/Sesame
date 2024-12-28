package io.github.lazyimmortal.sesame.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import io.github.lazyimmortal.sesame.R;
import io.github.lazyimmortal.sesame.data.ViewAppInfo;
import io.github.lazyimmortal.sesame.util.LanguageUtil;

public class BaseActivity extends AppCompatActivity {

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewAppInfo.init(getApplicationContext());
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        toolbar = findViewById(R.id.x_toolbar);
        toolbar.setTitle(getBaseTitle());
        toolbar.setSubtitle(getBaseSubtitle());
        setSupportActionBar(toolbar);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageUtil.setLocal(newBase));
    }

    public String getBaseTitle() {
        return ViewAppInfo.getAppTitle();
    }

    public String getBaseSubtitle() {
        return null;
    }

    public void setBaseTitle(String title) {
        toolbar.setTitle(title);
    }

    public void setBaseSubtitle(String subTitle) {
        toolbar.setSubtitle(subTitle);
    }

    public void setBaseTitleTextColor(int color) {
        toolbar.setTitleTextColor(color);
    }

    public void setBaseSubtitleTextColor(int color) {
        toolbar.setSubtitleTextColor(color);
    }

}
