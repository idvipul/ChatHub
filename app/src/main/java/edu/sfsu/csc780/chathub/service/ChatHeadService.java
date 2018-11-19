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
import android.widget.ImageView;

import edu.sfsu.csc780.chathub.R;
import edu.sfsu.csc780.chathub.ui.MainActivity;

public class ChatHeadService extends Service {
    private View mChatHeadView;
    private WindowManager mWindowManager;
    private View mChatHeadLayout;
    private WindowManager.LayoutParams mParams;

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

        mParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        // chat head initial position
        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        mParams.x = 10;
        mParams.y = 150;

        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mChatHeadView, mParams);

        mChatHeadLayout = mChatHeadView.findViewById(R.id.chat_head_layout);

        ImageView chatHeadImage = (ImageView) mChatHeadView.findViewById(R.id.chat_head_image);

        chatHeadImage.setOnTouchListener(new View.OnTouchListener() {

            int initialX, initialY;
            float touchX, touchY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // initial positions
                        initialX = mParams.x;
                        initialY = mParams.y;

                        // touch locations
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        mParams.x = (int) (initialX + (event.getRawX() - touchX));
                        mParams.y = (int) (initialY + (event.getRawY() - touchY));

                        // update layout to the new location
                        mWindowManager.updateViewLayout(mChatHeadView, mParams);
                        return true;

                    case MotionEvent.ACTION_UP:
                        int xClick = (int) (event.getRawX() - touchX);
                        int yClick = (int) (event.getRawY() - touchY);

                        if (xClick < 10 && yClick < 10) {
                            if (mChatHeadLayout.getVisibility() == View.VISIBLE || mChatHeadLayout == null) {
                                // start chat hub app - on click event
                                startChatApp(view);
                            }
                        }

                        return true;
                }
                return false;
            }

        });

        // close chathead
        ImageView closeButton = (ImageView) mChatHeadView.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSelf();
            }
        });
    }

    public void startChatApp(View view) {
        Intent intent = new Intent(ChatHeadService.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatHeadView != null)
            mWindowManager.removeView(mChatHeadView);
    }
}