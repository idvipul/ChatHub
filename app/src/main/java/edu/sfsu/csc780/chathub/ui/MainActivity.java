/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.sfsu.csc780.chathub.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;
import edu.sfsu.csc780.chathub.R;
import edu.sfsu.csc780.chathub.model.ChatMessage;
import edu.sfsu.csc780.chathub.service.ChatHeadService;

import hani.momanii.supernova_emoji_library.Helper.EmojiconGridView;
import hani.momanii.supernova_emoji_library.Helper.EmojiconsPopup;
import hani.momanii.supernova_emoji_library.emoji.Emojicon;

import static edu.sfsu.csc780.chathub.ui.ImageUtil.savePhotoImage;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener,
        MessageUtil.MessageLoadListener {

    private static final String TAG = "MainActivity";
    public static final String MESSAGES_CHILD = "messages";
    private static final int REQUEST_INVITE = 1;
    public static final int MSG_LENGTH_LIMIT = 100;
    public static final String ANONYMOUS = "anonymous";
    private String mUsername;
    private String mPhotoUrl;
    private SharedPreferences mSharedPreferences;
    private GoogleApiClient mGoogleApiClient;

    private FloatingActionButton mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private ImageButton mLocationButton;
    private ImageButton mCameraButton;
    private ImageButton mMicrophoneButton;

    // Firebase instance variables
    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private FirebaseRecyclerAdapter<ChatMessage, MessageUtil.MessageViewHolder> mFirebaseAdapter;
    private ImageButton mImageButton;
    private RelativeLayout mRelativeLayout;

    // FCM
    public DatabaseReference ref;
    public static boolean isPaused;
    private Handler mHandler;
    final Handler handler = new Handler();
    private ProgressDialog mProgressDialog;

    private static final int REQUEST_PICK_IMAGE = 1;
    private static final int REQUEST_CAPTURE_IMAGE = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 3;
    private static final int REQUEST_MICROPHONE_PERMISSION = 4;
    private static final int REQUEST_CHANGE_WALLPAPER = 5;
    private static final int REQUEST_CHATHEAD_PERMISSION = 6;
    private double MAX_LINEAR_DIMENSION = 500.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            setTheme(R.style.AppThemeNightMode);
        } else {
            setTheme(R.style.AppThemeDayMode);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRelativeLayout = (RelativeLayout) findViewById(R.id.setWallpaper);

        // fcm
        mHandler = new Handler();
        ref = FirebaseDatabase.getInstance().getReference();
        ref.child(MESSAGES_CHILD).limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(final DataSnapshot snapshot, String s) {

                if (isPaused) {
                    mHandler.postAtTime(new Runnable() {
                        @Override
                        public void run() {
                            String text = (String) snapshot.child("text").getValue();
                            showNotification(text);
                        }
                    }, 3000);
                }

            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot snapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // get wallpaper path
//        String wallpaperPath = mSharedPreferences.getString("wallpaperPath", null);
//
//        if (wallpaperPath != null) {
//            Uri uri = Uri.parse(wallpaperPath);
//            // get bitmap of the wallpaper
//            Bitmap bitmap = ImageUtil.getBitmapForUri(this, uri);
//            changeWallpaper(bitmap);
//        }

        // Set default username is anonymous.
        mUsername = ANONYMOUS;
        //Initialize Auth
        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        if (mUser == null) {
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mUser.getDisplayName();
            if (mUser.getPhotoUrl() != null) {
                mPhotoUrl = mUser.getPhotoUrl().toString();
            }
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MSG_LENGTH_LIMIT)});
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = (FloatingActionButton) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Send messages on click.
                ChatMessage chatMessage = new
                        ChatMessage(mMessageEditText.getText().toString(),
                        mUsername,
                        mPhotoUrl);
                MessageUtil.send(chatMessage);
                mMessageEditText.setText("");
                int lastPosition = mMessageRecyclerView.getAdapter().getItemCount();
                mMessageRecyclerView.scrollToPosition(lastPosition);
            }
        });

        mFirebaseAdapter = MessageUtil.getFirebaseAdapter(this,
                this,  /* MessageLoadListener */
                mLinearLayoutManager,
                mMessageRecyclerView);

        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        mImageButton = (ImageButton) findViewById(R.id.shareImageButton);
        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage(REQUEST_PICK_IMAGE);
            }

        });

        mLocationButton = (ImageButton) findViewById(R.id.locationButton);
        mLocationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                loadMap();
            }
        });

        // camera -- referred YouTube tutorials
        mCameraButton = (ImageButton) findViewById(R.id.cameraButton);
        mCameraButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                captureImage();
            }
        });

        // microphone -- referred YouTube tutorials
        mMicrophoneButton = (ImageButton) findViewById(R.id.microphoneButton);
        mMicrophoneButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                getSpeechInput();
            }
        });

        // push notification
        // retrieve the intent that was used to launch this activity.
        Intent intent = getIntent();

        //get the string parameter called 'param' - set in showNotification()
        String param = getMessagetext(intent);

        if (param != null) {

            // send the message to the mobile app
            ChatMessage chatMessage = new
                    ChatMessage(param,
                    mUsername,
                    mPhotoUrl);
            MessageUtil.send(chatMessage);
            mMessageEditText.setText("");
        }

        // emoticons - referred an online tutorial
        ListView listView = (ListView) findViewById(R.id.listView);
        final ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(this, R.layout.listview_row_layout);
        listView.setAdapter(mAdapter);

        View rootView = getWindow().getDecorView().getRootView();
        final ImageView emojiButton = (ImageView) findViewById(R.id.emoticonButton);

        final EmojiconsPopup popup = new EmojiconsPopup(rootView, this, true);
        popup.setSizeForSoftKeyboard();

        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                changeEmojiKeyboardIcon(emojiButton, R.drawable.smiley);
            }
        });

        // dismiss emoji popup
        popup.setOnSoftKeyboardOpenCloseListener(new EmojiconsPopup.OnSoftKeyboardOpenCloseListener() {
            @Override
            public void onKeyboardOpen(int keyBoardHeight) {
            }

            @Override
            public void onKeyboardClose() {
                if(popup.isShowing())
                    popup.dismiss();
            }
        });

        // on emoji clicked
        popup.setOnEmojiconClickedListener(new EmojiconGridView.OnEmojiconClickedListener() {
            @Override
            public void onEmojiconClicked(Emojicon emojicon) {
                if (mMessageEditText == null || emojicon == null) {
                    return;
                }

                int start = mMessageEditText.getSelectionStart();
                int end = mMessageEditText.getSelectionEnd();
                if (start < 0) {
                    mMessageEditText.append(emojicon.getEmoji());
                } else {
                    mMessageEditText.getText().replace(Math.min(start, end),
                            Math.max(start, end), emojicon.getEmoji(), 0,
                            emojicon.getEmoji().length());
                }
            }
        });

        // on backspace clicked
        popup.setOnEmojiconBackspaceClickedListener(new EmojiconsPopup.OnEmojiconBackspaceClickedListener() {
            @Override
            public void onEmojiconBackspaceClicked(View v) {
                KeyEvent event = new KeyEvent(
                        0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
                mMessageEditText.dispatchKeyEvent(event);
            }
        });

        // To toggle between text keyboard and emoji keyboard keyboard(Popup)
        emojiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If popup is not showing => emoji keyboard is not visible, we need to show it
                if(!popup.isShowing()){
                    //If keyboard is visible, simply show the emoji popup
                    if(popup.isKeyBoardOpen()){
                        popup.showAtBottom();
                        changeEmojiKeyboardIcon(emojiButton, R.drawable.ic_action_keyboard);
                    }
                    //else, open the text keyboard first and immediately after that show the emoji popup
                    else{
                        mMessageEditText.setFocusableInTouchMode(true);
                        mMessageEditText.requestFocus();
                        popup.showAtBottomPending();
                        final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(mMessageEditText, InputMethodManager.SHOW_IMPLICIT);
                        changeEmojiKeyboardIcon(emojiButton, R.drawable.ic_action_keyboard);
                    }
                }
                //If popup is showing, simply dismiss it to show the undelying text keyboard
                else{
                    popup.dismiss();
                }
            }
        });

        initChatHead();
    } // -- onCreate ends

    private void initChatHead() {
        // enable user permission to start chathead service -- referred Youtube tutorial
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + this.getPackageName()));
            startActivityForResult(intent, REQUEST_CHATHEAD_PERMISSION);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in.
        // TODO: Add code to check if user is signed in.
    }

    @Override
    public void onPause() {
        super.onPause();
        isPaused = true;
        if (isPaused) {
            Log.d("is paused is ", "true");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isPaused = false;
        if (!isPaused) {
            Log.d("is paused is ", "false");
        }
        LocationUtils.startLocationUpdates(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        startService(new Intent(MainActivity.this, ChatHeadService.class));
        super.onBackPressed();

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                mAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mUsername = ANONYMOUS;
                startActivity(new Intent(this, SignInActivity.class));
                return true;
            case R.id.day_night_mode:
                changeMode();
                return true;
            case R.id.change_wallpaper:
                pickImage(REQUEST_CHANGE_WALLPAPER);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void loadMap() {
        mProgressBar.setVisibility(ProgressBar.VISIBLE);
        mLocationButton.setEnabled(false);

        Loader<Bitmap> loader = getSupportLoaderManager().initLoader(0, null,
                new LoaderManager.LoaderCallbacks<Bitmap>() {

                    @Override
                    public Loader<Bitmap> onCreateLoader(final int id, final Bundle args) {
                        return new MapLoader(MainActivity.this);
                    }

                    @Override
                    public void onLoadFinished(final Loader<Bitmap> loader, final Bitmap result) {
                        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                        mLocationButton.setEnabled(true);

                        if (result == null) return;
                        // Resize if too big for messaging
                        Bitmap resizedBitmap = ImageUtil.scaleImage(result);
                        Uri uri = null;
                        if (result != resizedBitmap) {
                            uri = savePhotoImage(MainActivity.this, resizedBitmap);
                        } else {
                            uri = savePhotoImage(MainActivity.this, result);
                        }
                        createImageMessage(uri);
                    }

                    @Override
                    public void onLoaderReset(final Loader<Bitmap> loader) {
                    }
                });
        loader.forceLoad();
    }

    private void pickImage(int requestCode) {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file browser
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        // Filter to only show results that can be "opened"
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // Filter to show only images, using the image MIME data type.
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    // camera
    private void captureImage() {
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 2);
            startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
        } else {
            String[] permissionRequested = {Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(this, permissionRequested, REQUEST_CAMERA_PERMISSION);
        }
    }

    // microphone
    private void getSpeechInput() {

        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)) {
            // prompt the user for speech and send it through a speech recognizer
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            if (intent.resolveActivity(getPackageManager()) != null) {
                // result returned via activity results in the form of intent
                startActivityForResult(intent, REQUEST_MICROPHONE_PERMISSION);
            } else {
                Toast.makeText(this, "Your Device does not support Speech Input", Toast.LENGTH_SHORT).show();
            }
        } else {
            String[] permissionRequested = {Manifest.permission.RECORD_AUDIO};
            ActivityCompat.requestPermissions(this, permissionRequested, REQUEST_MICROPHONE_PERMISSION);
        }
    }

    // day/night mode -- referred YouTube tutorials
    private void changeMode() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO || AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_AUTO || AppCompatDelegate.getDefaultNightMode() == -1) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Toast.makeText(this, "Changed to Night Mode", Toast.LENGTH_SHORT).show();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(this, "Changed to Day Mode", Toast.LENGTH_SHORT).show();
        }
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    // emoticon
    private void changeEmojiKeyboardIcon(ImageView iconToBeChanged, int drawableResourceId){
        iconToBeChanged.setImageResource(drawableResourceId);
    }

    // reply from notification
    private String getMessagetext(Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            return (String) remoteInput.getCharSequence("message");
        }
        return null;
    }

    // referred a tutorial online
    private void showNotification(String param) {
        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(param);
        String replyLabel = getResources().getString(R.string.reply);

        // This PendingIntent will be fired when the user taps on the notification in the status bar.

        RemoteInput remoteInput = new RemoteInput.Builder("message")
                .setLabel(replyLabel)
                .build();

        Intent replyIntent = new Intent(this, MainActivity.class);
        PendingIntent replyPendingIntent = PendingIntent.getActivity(this, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        replyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        NotificationCompat.Action action1 = new NotificationCompat.Action.Builder(R.drawable.ic_reply_back_48px,
                getString(R.string.reply), replyPendingIntent)
                .addRemoteInput(remoteInput).build();

        List<NotificationCompat.Action> actions = new ArrayList<>();
        actions.add(action1);

        Notification notification = new NotificationCompat.Builder(this)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.mipmap.ic_launcher).setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_SOUND).setContentTitle("New Message Received!")
                .setContentText(param)
                .setStyle(bigStyle).build();

        replyIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.contentIntent = replyPendingIntent;

        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(this);
        mNotificationManager.notify(1, notification);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoadComplete() {
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        boolean isGranted = (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED);

        if (isGranted) {
            switch (requestCode) {
                case LocationUtils.REQUEST_CODE:
                    LocationUtils.startLocationUpdates(this);
                    break;
                case REQUEST_CAMERA_PERMISSION:
                    captureImage();
                    break;
                case REQUEST_MICROPHONE_PERMISSION:
                    getSpeechInput();
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: request=" + requestCode + ", result=" + resultCode);

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            // Process selected image here
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            if (data != null) {

                Uri uri = data.getData();
                Log.i(TAG, "Uri: " + uri.toString());
                // Resize if too big for messaging
                Bitmap bitmap = ImageUtil.getBitmapForUri(this, uri);
                Bitmap resizedBitmap = ImageUtil.scaleImage(bitmap);
                if (bitmap != resizedBitmap) {
                    uri = savePhotoImage(this, resizedBitmap);
                }
                createImageMessage(uri);

            } else {
                Log.e(TAG, "Cannot get image for uploading");
            }
        }

        // camera
        if (requestCode == REQUEST_CAPTURE_IMAGE && resultCode == Activity.RESULT_OK) {
            // process captured image
            if (data != null) {
                Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                Uri uri = savePhotoImage(this, bitmap);
                createImageMessage(uri);
            } else {
                Log.e(TAG, "Cannot capture an image");
            }
        }

        // microphone
        if (requestCode == REQUEST_MICROPHONE_PERMISSION && resultCode == Activity.RESULT_OK) {

            if (data != null) {
                // get audio & store it in ArrayList of Strings
                ArrayList<String> audio = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                ChatMessage text = new ChatMessage(audio.get(0), mUsername, mPhotoUrl);

                // send audio as a text message
                MessageUtil.send(text);
            } else {
                Log.e(TAG, "Cannot get an audio");
            }
        }

        // change wallpaper
        if (requestCode == REQUEST_CHANGE_WALLPAPER && resultCode == Activity.RESULT_OK) {

            if (data != null) {
                Uri uri = data.getData();
                Bitmap bitmap = ImageUtil.getBitmapForUri(this, uri);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("wallpaperPath", uri.toString());
                editor.apply();
                changeWallpaper(bitmap);
            }
        }

        // chathead
        if (requestCode == REQUEST_CHATHEAD_PERMISSION && resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Overlay permission not granted. Closing the application", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    // change wallpaper
    private void changeWallpaper(Bitmap bitmap) {
        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
        mRelativeLayout.setBackground(drawable);
    }

    private void createImageMessage(Uri uri) {
        if (uri == null) Log.e(TAG, "Could not create image message with null uri");
        final StorageReference imageReference = MessageUtil.getImageStorageReference(mUser, uri);
        UploadTask uploadTask = imageReference.putFile(uri);
        // Register observers to listen for when task is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e(TAG, "Failed to upload image message");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                ChatMessage chatMessage = new
                        ChatMessage(mMessageEditText.getText().toString(),
                        mUsername,
                        mPhotoUrl, imageReference.toString());
                MessageUtil.send(chatMessage);
                mMessageEditText.setText("");
            }
        });
    }
}