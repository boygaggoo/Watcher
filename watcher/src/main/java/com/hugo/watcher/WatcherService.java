package com.hugo.watcher;

import android.app.ActivityManager;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hugo.watcher.config.AppBackground;
import com.hugo.watcher.config.WatcherConfig;
import com.hugo.watcher.config.WatcherListener;
import com.hugo.watcher.monitor.FpsMonitor;
import com.hugo.watcher.monitor.MemoryMonitor;
import java.text.DecimalFormat;

public class WatcherService extends Service {

    private final DecimalFormat mFpsFormat = new DecimalFormat("#.0' fps'");
    private final DecimalFormat mMemoryFormat = new DecimalFormat("#.00' MB'");

    private WindowManager mWindowManager;

    private View mStageView;
    private TextView mTvFps;
    private TextView mTvMemory;

    private FpsMonitor mFpsMonitor;
    private MemoryMonitor mMemoryMonitor;
    private Handler mHandler;

    private boolean mHasInitialized = false;

    private WatcherConfig mWatcherConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mHandler = new Handler(getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mWatcherConfig = intent.getParcelableExtra(WatcherConfig.CONFIG_KEY);

        if (mWatcherConfig != null && !mHasInitialized) {
            ((Application) getApplicationContext()).registerActivityLifecycleCallbacks(AppBackground.init(getApplication()));
            initView();
            if (mWatcherConfig.enableFps) {
                initFps();
            }
            if (mWatcherConfig.enableMemory) {
                initMemory();
            }
            mHasInitialized = true;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void initView() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        layoutParams.format = PixelFormat.TRANSLUCENT;
        layoutParams.gravity = mWatcherConfig.seat;

        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout parent = new LinearLayout(this);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setBackgroundColor(Color.parseColor("#60ffffff"));
        mStageView = inflater.inflate(R.layout.watcher_stage, parent);

        mWindowManager.addView(mStageView, layoutParams);
    }

    private void initFps() {
        mTvFps = (TextView) mStageView.findViewById(R.id.tv_fps);
        mFpsMonitor = new FpsMonitor();
        mFpsMonitor.setListener(new WatcherListener() {
            @Override
            public void post(double value) {
                if (mTvFps != null) {
                    mTvFps.setText(mFpsFormat.format(value));
                }
            }
        });
        mFpsMonitor.start();
    }

    private void initMemory() {
        mTvMemory = (TextView) mStageView.findViewById(R.id.tv_memory);
        mMemoryMonitor = new MemoryMonitor((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE), getPackageName());
        mMemoryMonitor.setListener(new WatcherListener() {
            @Override
            public void post(final double value) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mTvMemory != null) {
                            mTvMemory.setText(mMemoryFormat.format(value));
                        }
                    }
                });
            }
        });
        mMemoryMonitor.start();
    }

    private void stop() {
        if (mWatcherConfig.enableFps) {
            mFpsMonitor.stop();
        }
        if (mWatcherConfig.enableMemory) {
            mMemoryMonitor.stop();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mHasInitialized) {
            mWindowManager.removeView(mStageView);
            stop();
        }
    }
}
