package edu.sfsu.csc780.chathub.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import edu.sfsu.csc780.chathub.R;

public class ChatHeadService extends Service {
    private View mChatHeadView;
    private WindowManager mWindowManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // entry point of service
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();

        mChatHeadView = LayoutInflater.from(this).inflate(R.layout.chat_head, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mChatHeadView, params);

        ImageView chatHeadImage = mChatHeadView.findViewById(R.id.chat_head_image);
        chatHeadImage.setOnTouchListener(new View.OnTouchListener() {

            private int initialX;
            private int initialY;
            private int touchX;
            private int touchY;
            private int lastAction;


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    initialX = params.x;
                    initialY = params.y;

                    touchX = (int) event.getRawX();
                    touchY = (int) event.getRawY();

                    lastAction = event.getAction();
                    return  true;
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (lastAction == MotionEvent.ACTION_DOWN) {
                        Button button = new Button(ChatHeadService.this);
                        button.setText("Close");

                        RelativeLayout layout = mChatHeadView.findViewById(R.id.chat_head_layout);
                        layout.addView(button);

                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                stopSelf();
                            }
                        });
                    }
                    lastAction = event.getAction();
                    return true;
                }

                if (event.getAction() == MotionEvent.ACTION_MOVE ) {
                    params.x = initialX + (int)(event.getRawX() - touchX);
                    params.y = initialY + (int)(event.getRawY() - touchY);

                    mWindowManager.updateViewLayout(mChatHeadView, params);
                    lastAction = event.getAction();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mChatHeadView != null) {
            mWindowManager.removeView(mChatHeadView);
        }
    }
}
