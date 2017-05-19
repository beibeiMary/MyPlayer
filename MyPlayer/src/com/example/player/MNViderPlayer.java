package com.example.player;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import com.example.myplayer.R;
import com.example.tools.LightnessControl;
import com.example.tools.PlayerUtils;
import com.example.tools.ProgressWheel;


/**
 * Created by maning on 16/6/14.
 * ������
 */
public class MNViderPlayer extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener,
        SurfaceHolder.Callback, GestureDetector.OnGestureListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener {

    private static final String TAG = "MNViderPlayer";
    private Context context;
    private Activity activity;

    static final Handler myHandler = new Handler(Looper.getMainLooper()) {
    };

    // SurfaceView�Ĵ����ȽϺ�ʱ��Ҫע��
    private SurfaceHolder surfaceHolder;
    private MediaPlayer mediaPlayer;

    //��ַ
    private String videoPath;
    private String videoTitle;
    private int video_position = 0;

    //�ؼ���λ����Ϣ
    private float mediaPlayerX;
    private float mediaPlayerY;

    // ��ʱ��
    private Timer timer_video_time;
    private TimerTask task_video_timer;
    private Timer timer_controller;
    private TimerTask task_controller;

    //�Ƿ��Ǻ���
    private boolean isFullscreen = false;
    private boolean isLockScreen = false;
    private boolean isPrepare = false;
    private boolean isNeedBatteryListen = true;
    private boolean isNeedNetChangeListen = true;
    private boolean isFirstPlay = false;

    //�ؼ�
    private RelativeLayout mn_rl_bottom_menu;
    private SurfaceView mn_palyer_surfaceView;
    private ImageView mn_iv_play_pause;
    private ImageView mn_iv_fullScreen;
    private TextView mn_tv_time;
    private SeekBar mn_seekBar;
    private ImageView mn_iv_back;
    private TextView mn_tv_title;
    private TextView mn_tv_system_time;
    private RelativeLayout mn_rl_top_menu;
    private RelativeLayout mn_player_rl_progress;
    private ImageView mn_player_iv_lock;
    private LinearLayout mn_player_ll_error;
    private LinearLayout mn_player_ll_net;
    private ProgressWheel mn_player_progressBar;
    private ImageView mn_iv_battery;
    private ImageView mn_player_iv_play_center;

    public MNViderPlayer(Context context) {
        this(context, null);
    }

    public MNViderPlayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MNViderPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        activity = (Activity) this.context;
        //�Զ����������
        initAttrs(context, attrs);
        //����
        init();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        //��ȡ�Զ�������
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MNViderPlayer);
        //�����õ��Զ�������
        for (int i = 0; i < typedArray.getIndexCount(); i++) {
            int index = typedArray.getIndex(i);
            if (index == R.styleable.MNViderPlayer_mnFirstNeedPlay) {
                isFirstPlay = typedArray.getBoolean(R.styleable.MNViderPlayer_mnFirstNeedPlay, false);
            }
        }
        //����
        typedArray.recycle();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int screenWidth = PlayerUtils.getScreenWidth(activity);
        int screenHeight = PlayerUtils.getScreenHeight(activity);
        ViewGroup.LayoutParams layoutParams = getLayoutParams();

        //newConfig.orientation��õ�ǰ��Ļ״̬�Ǻ����������
        //Configuration.ORIENTATION_PORTRAIT ��ʾ����
        //Configuration.ORIENTATION_LANDSCAPE ��ʾ����
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            //������Ƶ�Ĵ�С16��9
            layoutParams.width = screenWidth;
            layoutParams.height = screenWidth * 9 / 16;

            setX(mediaPlayerX);
            setY(mediaPlayerY);
        }
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            layoutParams.width = screenWidth;
            layoutParams.height = screenHeight;

            setX(0);
            setY(0);
        }
        setLayoutParams(layoutParams);
    }

    //��ʼ��
    private void init() {
        View inflate = View.inflate(context, R.layout.mn_player_view, this);
        mn_rl_bottom_menu = (RelativeLayout) inflate.findViewById(R.id.mn_rl_bottom_menu);
        mn_palyer_surfaceView = (SurfaceView) inflate.findViewById(R.id.mn_palyer_surfaceView);
        mn_iv_play_pause = (ImageView) inflate.findViewById(R.id.mn_iv_play_pause);
        mn_iv_fullScreen = (ImageView) inflate.findViewById(R.id.mn_iv_fullScreen);
        mn_tv_time = (TextView) inflate.findViewById(R.id.mn_tv_time);
        mn_tv_system_time = (TextView) inflate.findViewById(R.id.mn_tv_system_time);
        mn_seekBar = (SeekBar) inflate.findViewById(R.id.mn_seekBar);
        mn_iv_back = (ImageView) inflate.findViewById(R.id.mn_iv_back);
        mn_tv_title = (TextView) inflate.findViewById(R.id.mn_tv_title);
        mn_rl_top_menu = (RelativeLayout) inflate.findViewById(R.id.mn_rl_top_menu);
        mn_player_rl_progress = (RelativeLayout) inflate.findViewById(R.id.mn_player_rl_progress);
        mn_player_iv_lock = (ImageView) inflate.findViewById(R.id.mn_player_iv_lock);
        mn_player_ll_error = (LinearLayout) inflate.findViewById(R.id.mn_player_ll_error);
        mn_player_ll_net = (LinearLayout) inflate.findViewById(R.id.mn_player_ll_net);
        mn_player_progressBar = (ProgressWheel) inflate.findViewById(R.id.mn_player_progressBar);
        mn_iv_battery = (ImageView) inflate.findViewById(R.id.mn_iv_battery);
        mn_player_iv_play_center = (ImageView) inflate.findViewById(R.id.mn_player_iv_play_center);

        mn_seekBar.setOnSeekBarChangeListener(this);
        mn_iv_play_pause.setOnClickListener(this);
        mn_iv_fullScreen.setOnClickListener(this);
        mn_iv_back.setOnClickListener(this);
        mn_player_iv_lock.setOnClickListener(this);
        mn_player_ll_error.setOnClickListener(this);
        mn_player_ll_net.setOnClickListener(this);
        mn_player_iv_play_center.setOnClickListener(this);

        //��ʼ��
        initViews();

        if (!isFirstPlay) {
            mn_player_iv_play_center.setVisibility(View.VISIBLE);
            mn_player_progressBar.setVisibility(View.GONE);
        }

        //��ʼ��SurfaceView
        initSurfaceView();

        //��ʼ������
        initGesture();

        //�洢�ؼ���λ����Ϣ
        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mediaPlayerX = getX();
                mediaPlayerY = getY();
                Log.i(TAG, "�ؼ���λ��---X��" + mediaPlayerX + "��Y��" + mediaPlayerY);
            }
        }, 1000);
    }

    private void initViews() {
        mn_tv_system_time.setText(PlayerUtils.getCurrentHHmmTime());
        mn_rl_bottom_menu.setVisibility(View.GONE);
        mn_rl_top_menu.setVisibility(View.GONE);
        mn_player_iv_lock.setVisibility(View.GONE);
        initLock();
        mn_player_rl_progress.setVisibility(View.VISIBLE);
        mn_player_progressBar.setVisibility(View.VISIBLE);
        mn_player_ll_error.setVisibility(View.GONE);
        mn_player_ll_net.setVisibility(View.GONE);
        mn_player_iv_play_center.setVisibility(View.GONE);
        initTopMenu();
    }

    private void initLock() {
        if (isFullscreen) {
            mn_player_iv_lock.setVisibility(View.VISIBLE);
        } else {
            mn_player_iv_lock.setVisibility(View.GONE);
        }
    }

    private void initSurfaceView() {
        Log.i(TAG, "initSurfaceView");
        // �õ�SurfaceView���������ŵ����ݾ�����ʾ�������������
        surfaceHolder = mn_palyer_surfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        // SurfaceView��һ���ص�����
        surfaceHolder.addCallback(this);
    }

    private void initTopMenu() {
        mn_tv_title.setText(videoTitle);
        if (isFullscreen) {
            mn_rl_top_menu.setVisibility(View.VISIBLE);
        } else {
            mn_rl_top_menu.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.mn_iv_play_pause) {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    mn_iv_play_pause.setImageResource(R.drawable.mn_player_play);
                } else {
                    mediaPlayer.start();
                    mn_iv_play_pause.setImageResource(R.drawable.mn_player_pause);
                }
            }
        } else if (i == R.id.mn_iv_fullScreen) {
            if (isFullscreen) {
                setProtrait();
            } else {
                setLandscape();
            }
        } else if (i == R.id.mn_iv_back) {
            setProtrait();
        } else if (i == R.id.mn_player_iv_lock) {
            if (isFullscreen) {
                if (isLockScreen) {
                    unLockScreen();
                    initBottomMenuState();
                } else {
                    lockScreen();
                    destroyControllerTask(true);
                }
            }
        } else if (i == R.id.mn_player_ll_error || i == R.id.mn_player_ll_net || i == R.id.mn_player_iv_play_center) {
            playVideo(videoPath, videoTitle, 0);
        }
    }

    //--------------------------------------------------------------------------------------
    // ######## ���View�Ĳ��� ########
    //--------------------------------------------------------------------------------------

    private void unLockScreen() {
        isLockScreen = false;
        mn_player_iv_lock.setImageResource(R.drawable.mn_player_landscape_screen_lock_open);
    }

    private void lockScreen() {
        isLockScreen = true;
        mn_player_iv_lock.setImageResource(R.drawable.mn_player_landscape_screen_lock_close);
    }

    //����˵�����ʾ������
    private void initBottomMenuState() {
        mn_tv_system_time.setText(PlayerUtils.getCurrentHHmmTime());
        if (mn_rl_bottom_menu.getVisibility() == View.GONE) {
            initControllerTask();
            mn_rl_bottom_menu.setVisibility(View.VISIBLE);
            if (isFullscreen) {
                mn_rl_top_menu.setVisibility(View.VISIBLE);
                mn_player_iv_lock.setVisibility(View.VISIBLE);
            }
        } else {
            destroyControllerTask(true);
        }
    }

    private void dismissControllerMenu() {
        if (isFullscreen && !isLockScreen) {
            mn_player_iv_lock.setVisibility(View.GONE);
        }
        mn_rl_top_menu.setVisibility(View.GONE);
        mn_rl_bottom_menu.setVisibility(View.GONE);
    }

    private void showErrorView() {
        mn_player_iv_play_center.setVisibility(View.GONE);
        mn_player_ll_net.setVisibility(View.GONE);
        mn_player_progressBar.setVisibility(View.GONE);
        mn_player_ll_error.setVisibility(View.VISIBLE);
    }

    private void showNoNetView() {
        mn_player_iv_play_center.setVisibility(View.GONE);
        mn_player_ll_net.setVisibility(View.VISIBLE);
        mn_player_progressBar.setVisibility(View.GONE);
        mn_player_ll_error.setVisibility(View.GONE);
    }

    private void setLandscape() {
        isFullscreen = true;
        //���ú���
        ((Activity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (mn_rl_bottom_menu.getVisibility() == View.VISIBLE) {
            mn_rl_top_menu.setVisibility(View.VISIBLE);
        }
        initLock();
    }

    private void setProtrait() {
        isFullscreen = false;
        //���ú���
        ((Activity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        mn_rl_top_menu.setVisibility(View.GONE);
        unLockScreen();
        initLock();
    }

    //--------------------------------------------------------------------------------------
    // ######## ��ʱ����ز��� ########
    //--------------------------------------------------------------------------------------

    private void initTimeTask() {
        timer_video_time = new Timer();
        task_video_timer = new TimerTask() {
            @Override
            public void run() {
                myHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mediaPlayer == null) {
                            return;
                        }
                        //����ʱ��
                        mn_tv_time.setText(String.valueOf(PlayerUtils.converLongTimeToStr(mediaPlayer.getCurrentPosition()) + " / " + PlayerUtils.converLongTimeToStr(mediaPlayer.getDuration())));
                        //������
                        int progress = mediaPlayer.getCurrentPosition();
                        mn_seekBar.setProgress(progress);
                    }
                });
            }
        };
        timer_video_time.schedule(task_video_timer, 0, 1000);
    }

    private void destroyTimeTask() {
        if (timer_video_time != null && task_video_timer != null) {
            timer_video_time.cancel();
            task_video_timer.cancel();
            timer_video_time = null;
            task_video_timer = null;
        }
    }

    private void initControllerTask() {
        // ���ü�ʱ��,��������Ӱ�غ���ʾ
        timer_controller = new Timer();
        task_controller = new TimerTask() {
            @Override
            public void run() {
                destroyControllerTask(false);
            }
        };
        timer_controller.schedule(task_controller, 5000);
        initTimeTask();
    }

    private void destroyControllerTask(boolean isMainThread) {
        if (isMainThread) {
            dismissControllerMenu();
        } else {
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    dismissControllerMenu();
                }
            });
        }
        if (timer_controller != null && task_controller != null) {
            timer_controller.cancel();
            task_controller.cancel();
            timer_controller = null;
            task_controller = null;
        }
        destroyTimeTask();
    }

    //--------------------------------------------------------------------------------------
    // ######## �ӿڷ���ʵ�� ########
    //--------------------------------------------------------------------------------------
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            int maxCanSeekTo = seekBar.getMax() - 5 * 1000;
            if (seekBar.getProgress() < maxCanSeekTo) {
                mediaPlayer.seekTo(seekBar.getProgress());
            } else {
                //�����ϵ����
                mediaPlayer.seekTo(maxCanSeekTo);
            }
        }
    }

    //����
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated");
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDisplay(holder); // ��ӵ�������
        //������ɵļ���
        mediaPlayer.setOnCompletionListener(this);
        // �첽׼����һ������������׼�����˾͵�������ķ���
        mediaPlayer.setOnPreparedListener(this);
        //���Ŵ���ļ���
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        //��һ�γ�ʼ���費��Ҫ��������
        if (isFirstPlay) {
            //�жϵ�ǰ��û�����磨���ŵ���������Ƶ��
            if (!PlayerUtils.isNetworkConnected(context) && videoPath.startsWith("http")) {
                Toast.makeText(context, context.getString(R.string.mnPlayerNoNetHint), Toast.LENGTH_SHORT).show();
                showNoNetView();
            } else {
                //�ֻ����������
                if (PlayerUtils.isMobileConnected(context)) {
                    Toast.makeText(context, context.getString(R.string.mnPlayerMobileNetHint), Toast.LENGTH_SHORT).show();
                }
                //��Ӳ���·��
                try {
                    mediaPlayer.setDataSource(videoPath);
                    // ׼����ʼ,�첽׼�����Զ������߳���
                    mediaPlayer.prepareAsync();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        isFirstPlay = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //���沥��λ��
        if (mediaPlayer != null) {
            video_position = mediaPlayer.getCurrentPosition();
        }
        destroyControllerTask(true);
        pauseVideo();
        Log.i(TAG, "surfaceDestroyed---video_position��" + video_position);
    }

    //MediaPlayer
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mn_iv_play_pause.setImageResource(R.drawable.mn_player_play);
        destroyControllerTask(true);
        video_position = 0;
        if (onCompletionListener != null) {
            onCompletionListener.onCompletion(mediaPlayer);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.i(TAG, "��������onBufferingUpdate: " + percent);
        if (percent >= 0 && percent <= 100) {
            int secondProgress = mp.getDuration() * percent / 100;
            mn_seekBar.setSecondaryProgress(secondProgress);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.i(TAG, "��������error:" + what);
        if (what != -38) {  //������󲻹�
            showErrorView();
        }
        return true;
    }

    @Override
    public void onPrepared(final MediaPlayer mediaPlayer) {
        mediaPlayer.start(); // ��ʼ����
        isPrepare = true;
        if (video_position > 0) {
            Log.i(TAG, "onPrepared---video_position:" + video_position);
            mediaPlayer.seekTo(video_position);
            video_position = 0;
        }
        // �ѵõ����ܳ��Ⱥͽ�������ƥ��
        mn_seekBar.setMax(mediaPlayer.getDuration());
        mn_iv_play_pause.setImageResource(R.drawable.mn_player_pause);
        mn_tv_time.setText(String.valueOf(PlayerUtils.converLongTimeToStr(mediaPlayer.getCurrentPosition()) + "/" + PlayerUtils.converLongTimeToStr(mediaPlayer.getDuration())));
        //��ʱ�����������һ����Ƶ�Ļ�������
        myHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initBottomMenuState();
                mn_player_rl_progress.setVisibility(View.GONE);
            }
        }, 500);
    }

    //--------------------------------------------------------------------------------------
    // ######## ������� ########
    //--------------------------------------------------------------------------------------
    private RelativeLayout gesture_volume_layout;// �������Ʋ���
    private TextView geture_tv_volume_percentage;// �����ٷֱ�
    private ImageView gesture_iv_player_volume;// ����ͼ��
    private RelativeLayout gesture_light_layout;// ���Ȳ���
    private TextView geture_tv_light_percentage;// ���Ȱٷֱ�
    private RelativeLayout gesture_progress_layout;// ����ͼ��
    private TextView geture_tv_progress_time;// ����ʱ�����
    private ImageView gesture_iv_progress;// �������˱�־
    private GestureDetector gestureDetector;
    private AudioManager audiomanager;
    private int maxVolume, currentVolume;
    private static final float STEP_PROGRESS = 2f;// �趨���Ȼ���ʱ�Ĳ���������ÿ�λ������ı䣬���¸ı����
    private static final float STEP_VOLUME = 2f;// Э����������ʱ�Ĳ���������ÿ�λ������ı䣬���¸ı����
    private static final float STEP_LIGHT = 2f;// Э�����Ȼ���ʱ�Ĳ���������ÿ�λ������ı䣬���¸ı����
    private int GESTURE_FLAG = 0;// 1,���ڽ��ȣ�2����������
    private static final int GESTURE_MODIFY_PROGRESS = 1;
    private static final int GESTURE_MODIFY_VOLUME = 2;
    private static final int GESTURE_MODIFY_BRIGHTNESS = 3;

    private void initGesture() {
        gesture_volume_layout = (RelativeLayout) findViewById(R.id.mn_gesture_volume_layout);
        geture_tv_volume_percentage = (TextView) findViewById(R.id.mn_gesture_tv_volume_percentage);
        gesture_iv_player_volume = (ImageView) findViewById(R.id.mn_gesture_iv_player_volume);

        gesture_progress_layout = (RelativeLayout) findViewById(R.id.mn_gesture_progress_layout);
        geture_tv_progress_time = (TextView) findViewById(R.id.mn_gesture_tv_progress_time);
        gesture_iv_progress = (ImageView) findViewById(R.id.mn_gesture_iv_progress);

        //���ȵĲ���
        gesture_light_layout = (RelativeLayout) findViewById(R.id.mn_gesture_light_layout);
        geture_tv_light_percentage = (TextView) findViewById(R.id.mn_geture_tv_light_percentage);

        gesture_volume_layout.setVisibility(View.GONE);
        gesture_progress_layout.setVisibility(View.GONE);
        gesture_light_layout.setVisibility(View.GONE);

        gestureDetector = new GestureDetector(getContext(), this);
        setLongClickable(true);
        gestureDetector.setIsLongpressEnabled(true);
        audiomanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); // ��ȡϵͳ�������
        currentVolume = audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC); // ��ȡ��ǰֵ
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (!isPrepare || isLockScreen) {
            return false;
        }
        initBottomMenuState();
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

        if (!isPrepare || isLockScreen) {
            return false;
        }

        int FLAG = 0;

        // ����ľ���仯����������ȣ�����ı仯�����������
        if (Math.abs(distanceX) >= Math.abs(distanceY)) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                FLAG = GESTURE_MODIFY_PROGRESS;
            }
        } else {
            int intX = (int) e1.getX();
            int screenWidth = PlayerUtils.getScreenWidth((Activity) context);
            if (intX > screenWidth / 2) {
                FLAG = GESTURE_MODIFY_VOLUME;
            } else {
                //���������
                FLAG = GESTURE_MODIFY_BRIGHTNESS;
            }
        }

        if (GESTURE_FLAG != 0 && GESTURE_FLAG != FLAG) {
            return false;
        }

        GESTURE_FLAG = FLAG;

        if (FLAG == GESTURE_MODIFY_PROGRESS) {
            //��ʾ�Ǻ��򻬶�,������ӿ��
            // distanceX=lastScrollPositionX-currentScrollPositionX�����Ϊ��ʱ�ǿ��
            gesture_volume_layout.setVisibility(View.GONE);
            gesture_light_layout.setVisibility(View.GONE);
            gesture_progress_layout.setVisibility(View.VISIBLE);
            try {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    if (Math.abs(distanceX) > Math.abs(distanceY)) {// �����ƶ����������ƶ�
                        if (distanceX >= PlayerUtils.dip2px(context, STEP_PROGRESS)) {// ���ˣ��ò������Ƹı��ٶȣ���΢��
                            gesture_iv_progress
                                    .setImageResource(R.drawable.mn_player_backward);
                            if (mediaPlayer.getCurrentPosition() > 3 * 1000) {// ����Ϊ��
                                int cpos = mediaPlayer.getCurrentPosition();
                                mediaPlayer.seekTo(cpos - 3000);
                                mn_seekBar.setProgress(mediaPlayer.getCurrentPosition());
                            } else {
                                //ʲô������
                                mediaPlayer.seekTo(3000);
                            }
                        } else if (distanceX <= -PlayerUtils.dip2px(context, STEP_PROGRESS)) {// ���
                            gesture_iv_progress
                                    .setImageResource(R.drawable.mn_player_forward);
                            if (mediaPlayer.getCurrentPosition() < mediaPlayer.getDuration() - 5 * 1000) {// ���ⳬ����ʱ��
                                int cpos = mediaPlayer.getCurrentPosition();
                                mediaPlayer.seekTo(cpos + 3000);
                                // �ѵ�ǰλ�ø�ֵ��������
                                mn_seekBar.setProgress(mediaPlayer.getCurrentPosition());
                            }
                        }
                    }
                    String timeStr = PlayerUtils.converLongTimeToStr(mediaPlayer.getCurrentPosition()) + " / "
                            + PlayerUtils.converLongTimeToStr(mediaPlayer.getDuration());
                    geture_tv_progress_time.setText(timeStr);

                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        // ���ÿ�δ�����Ļ���һ��scroll�ǵ�����������֮���scroll�¼��������������ڣ�ֱ���뿪��Ļִ����һ�β���
        else if (FLAG == GESTURE_MODIFY_VOLUME) {
            //�ұ�������
            gesture_volume_layout.setVisibility(View.VISIBLE);
            gesture_light_layout.setVisibility(View.GONE);
            gesture_progress_layout.setVisibility(View.GONE);
            currentVolume = audiomanager
                    .getStreamVolume(AudioManager.STREAM_MUSIC); // ��ȡ��ǰֵ
            if (Math.abs(distanceY) > Math.abs(distanceX)) {// �����ƶ����ں����ƶ�
                if (currentVolume == 0) {// �������趨�������е�ͼƬ
                    gesture_iv_player_volume.setImageResource(R.drawable.mn_player_volume_close);
                }
                if (distanceY >= PlayerUtils.dip2px(context, STEP_VOLUME)) {// ��������,ע�����ʱ��������ϵ,�������Ͻ���ԭ�㣬���������ϻ���ʱdistanceYΪ��
                    if (currentVolume < maxVolume) {// Ϊ������ڹ��죬distanceYӦ����һ���趨ֵ
                        currentVolume++;
                    }
                    gesture_iv_player_volume.setImageResource(R.drawable.mn_player_volume_open);
                } else if (distanceY <= -PlayerUtils.dip2px(context, STEP_VOLUME)) {// ������С
                    if (currentVolume > 0) {
                        currentVolume--;
                        if (currentVolume == 0) {// �������趨�������е�ͼƬ
                            gesture_iv_player_volume.setImageResource(R.drawable.mn_player_volume_close);
                        }
                    }
                }
                int percentage = (currentVolume * 100) / maxVolume;
                geture_tv_volume_percentage.setText(String.valueOf(percentage + "%"));
                audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
            }
        }
        //��������
        else if (FLAG == GESTURE_MODIFY_BRIGHTNESS) {
            gesture_volume_layout.setVisibility(View.GONE);
            gesture_light_layout.setVisibility(View.VISIBLE);
            gesture_progress_layout.setVisibility(View.GONE);
            currentVolume = audiomanager
                    .getStreamVolume(AudioManager.STREAM_MUSIC); // ��ȡ��ǰֵ
            if (Math.abs(distanceY) > Math.abs(distanceX)) {// �����ƶ����ں����ƶ�
                // ���ȵ���,ע�����ʱ��������ϵ,�������Ͻ���ԭ�㣬���������ϻ���ʱdistanceYΪ��
                int mLight = LightnessControl.GetLightness((Activity) context);
                if (mLight >= 0 && mLight <= 255) {
                    if (distanceY >= PlayerUtils.dip2px(context, STEP_LIGHT)) {
                        if (mLight > 245) {
                            LightnessControl.SetLightness((Activity) context, 255);
                        } else {
                            LightnessControl.SetLightness((Activity) context, mLight + 10);
                        }
                    } else if (distanceY <= -PlayerUtils.dip2px(context, STEP_LIGHT)) {// ���ȵ�С
                        if (mLight < 10) {
                            LightnessControl.SetLightness((Activity) context, 0);
                        } else {
                            LightnessControl.SetLightness((Activity) context, mLight - 10);
                        }
                    }
                } else if (mLight < 0) {
                    LightnessControl.SetLightness((Activity) context, 0);
                } else {
                    LightnessControl.SetLightness((Activity) context, 255);
                }
                //��ȡ��ǰ����
                int currentLight = LightnessControl.GetLightness((Activity) context);
                int percentage = (currentLight * 100) / 255;
                geture_tv_light_percentage.setText(String.valueOf(percentage + "%"));
            }
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // ���������singleTapUp��û���������up�ķ���
        if (event.getAction() == MotionEvent.ACTION_UP) {
            GESTURE_FLAG = 0;// ��ָ�뿪��Ļ�����õ�����������ȵı�־
            gesture_volume_layout.setVisibility(View.GONE);
            gesture_progress_layout.setVisibility(View.GONE);
            gesture_light_layout.setVisibility(View.GONE);
        }
        return gestureDetector.onTouchEvent(event);
    }

    //--------------------------------------------------------------------------------------
    // ######## �����ṩ�ķ��� ########
    //--------------------------------------------------------------------------------------

    /**
     * ������Ƶ��Ϣ
     *
     * @param url   ��Ƶ��ַ
     * @param title ��Ƶ����
     */
    public void setDataSource(String url, String title) {
        //��ֵ
        videoPath = url;
        videoTitle = title;
    }

    /**
     * ������Ƶ
     *
     * @param url   ��Ƶ��ַ
     * @param title ��Ƶ����
     */
    public void playVideo(String url, String title) {
        playVideo(url, title, video_position);
    }

    /**
     * ������Ƶ��֧���ϴβ���λ�ã�
     * �Լ���¼��һ�β��ŵ�λ�ã�Ȼ�󴫵�position�����Ϳ�����
     *
     * @param url      ��Ƶ��ַ
     * @param title    ��Ƶ����
     * @param position ��Ƶ��ת��λ��
     */
    public void playVideo(String url, String title, int position) {
        //��ַ�пմ���
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(context, context.getString(R.string.mnPlayerUrlEmptyHint), Toast.LENGTH_SHORT).show();
            return;
        }
        //����ControllerView
        destroyControllerTask(true);

        //��ֵ
        videoPath = url;
        videoTitle = title;
        video_position = position;
        isPrepare = false;

        //�жϵ�ǰ��û�����磨���ŵ���������Ƶ��
        if (!PlayerUtils.isNetworkConnected(context) && url.startsWith("http")) {
            Toast.makeText(context, context.getString(R.string.mnPlayerNoNetHint), Toast.LENGTH_SHORT).show();
            showNoNetView();
            return;
        }
        //�ֻ����������
        if (PlayerUtils.isMobileConnected(context)) {
            Toast.makeText(context, context.getString(R.string.mnPlayerMobileNetHint), Toast.LENGTH_SHORT).show();
        }

        //����MediaPlayer
        resetMediaPlayer();

        //��ʼ��View
        initViews();
        //�жϹ㲥���
        if (isNeedBatteryListen) {
            registerBatteryReceiver();
        } else {
            unRegisterBatteryReceiver();
            mn_iv_battery.setVisibility(View.GONE);
        }
        //��������Ĺ㲥
        if (isNeedNetChangeListen) {
            registerNetReceiver();
        } else {
            unregisterNetReceiver();
        }
    }

    private void resetMediaPlayer() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    mediaPlayer.stop();
                }
                //����mediaPlayer
                mediaPlayer.reset();
                //��Ӳ���·��
                mediaPlayer.setDataSource(videoPath);
                // ׼����ʼ,�첽׼�����Զ������߳���
                mediaPlayer.prepareAsync();
            } else {
                Toast.makeText(context, "��������ʼ��ʧ��", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ������Ƶ
     */
    public void startVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            mn_iv_play_pause.setImageResource(R.drawable.mn_player_pause);
        }
    }

    /**
     * ��ͣ��Ƶ
     */
    public void pauseVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mn_iv_play_pause.setImageResource(R.drawable.mn_player_play);
            video_position = mediaPlayer.getCurrentPosition();
        }
    }

    /**
     * ����
     */
    public void setOrientationPortrait() {
        setProtrait();
    }

    /**
     * ����
     */
    public void setOrientationLandscape() {
        setLandscape();
    }

    /**
     * �����Ƿ���Ҫ��������
     */
    public void setIsNeedBatteryListen(boolean isNeedBatteryListen) {
        this.isNeedBatteryListen = isNeedBatteryListen;
    }

    /**
     * �����Ƿ���Ҫ����仯����
     */
    public void setIsNeedNetChangeListen(boolean isNeedNetChangeListen) {
        this.isNeedNetChangeListen = isNeedNetChangeListen;
    }

    /**
     * �ж��ǲ���ȫ��״̬
     *
     * @return
     */
    public boolean isFullScreen() {
        return isFullscreen;
    }

    /**
     * ��ȡ��ǰ���ŵ�λ��
     */
    public int getVideoCurrentPosition() {
        int position = 0;
        if (mediaPlayer != null) {
            position = mediaPlayer.getCurrentPosition();
        }
        return position;
    }

    /**
     * ��ȡ��Ƶ�ܳ���
     */
    public int getVideoTotalDuration() {
        int position = 0;
        if (mediaPlayer != null) {
            position = mediaPlayer.getDuration();
        }
        return position;
    }

    /**
     * ��ȡ������
     */
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    /**
     * ������Դ
     */
    public void destroyVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();// �ͷ���Դ
            mediaPlayer = null;
        }
        surfaceHolder = null;
        mn_palyer_surfaceView = null;
        video_position = 0;
        unRegisterBatteryReceiver();
        unregisterNetReceiver();
        removeAllListener();
        destroyTimeTask();
        myHandler.removeCallbacksAndMessages(null);
    }


    //--------------------------------------------------------------------------------------
    // ######## �㲥��� ########
    //--------------------------------------------------------------------------------------

    /**
     * �����㲥������
     */
    class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //�ж����Ƿ���Ϊ�����仯��Broadcast Action
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                //��ȡ��ǰ����
                int level = intent.getIntExtra("level", 0);
                //�������̶ܿ�
                int scale = intent.getIntExtra("scale", 100);

                int battery = (level * 100) / scale;

                //����ת�ɰٷֱ�
                Log.i(TAG, "��ص���Ϊ" + battery + "%");

                mn_iv_battery.setVisibility(View.VISIBLE);
                if (battery > 0 && battery < 20) {
                    mn_iv_battery.setImageResource(R.drawable.mn_player_battery_01);
                } else if (battery >= 20 && battery < 40) {
                    mn_iv_battery.setImageResource(R.drawable.mn_player_battery_02);
                } else if (battery >= 40 && battery < 65) {
                    mn_iv_battery.setImageResource(R.drawable.mn_player_battery_03);
                } else if (battery >= 65 && battery < 90) {
                    mn_iv_battery.setImageResource(R.drawable.mn_player_battery_04);
                } else if (battery >= 90 && battery <= 100) {
                    mn_iv_battery.setImageResource(R.drawable.mn_player_battery_05);
                } else {
                    mn_iv_battery.setVisibility(View.GONE);
                }


            }
        }
    }

    private BatteryReceiver batteryReceiver;

    private void registerBatteryReceiver() {
        if (batteryReceiver == null) {
            //ע��㲥������
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            //�����㲥�����߶���
            batteryReceiver = new BatteryReceiver();
            //ע��receiver
            context.registerReceiver(batteryReceiver, intentFilter);
        }
    }

    private void unRegisterBatteryReceiver() {
        if (batteryReceiver != null) {
            context.unregisterReceiver(batteryReceiver);
        }
    }

    //-------------------------����仯����
    public class NetChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (onNetChangeListener == null || !isNeedNetChangeListen) {
                return;
            }
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isAvailable()) {
                if (netInfo.getType() == ConnectivityManager.TYPE_WIFI) { //WiFi����
                    onNetChangeListener.onWifi(mediaPlayer);
                } else if (netInfo.getType() == ConnectivityManager.TYPE_MOBILE) {   //3g����
                    onNetChangeListener.onMobile(mediaPlayer);
                } else {    //����
                    Log.i(TAG, "��������");
                }
            } else {
                onNetChangeListener.onNoAvailable(mediaPlayer);
            }
        }
    }

    private NetChangeReceiver netChangeReceiver;

    private void registerNetReceiver() {
        if (netChangeReceiver == null) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            netChangeReceiver = new NetChangeReceiver();
            context.registerReceiver(netChangeReceiver, filter);
        }
    }

    private void unregisterNetReceiver() {
        if (netChangeReceiver != null) {
            context.unregisterReceiver(netChangeReceiver);
        }
    }


    //--------------------------------------------------------------------------------------
    // ######## �Զ���ص� ########
    //--------------------------------------------------------------------------------------

    private void removeAllListener() {
        if (onNetChangeListener != null) {
            onNetChangeListener = null;
        }
        if (onPlayerCreatedListener != null) {
            onPlayerCreatedListener = null;
        }
    }


    //��������ص�
    private OnNetChangeListener onNetChangeListener;

    public void setOnNetChangeListener(OnNetChangeListener onNetChangeListener) {
        this.onNetChangeListener = onNetChangeListener;
    }

    public interface OnNetChangeListener {
        //wifi
        void onWifi(MediaPlayer mediaPlayer);

        //�ֻ�
        void onMobile(MediaPlayer mediaPlayer);

        //������
        void onNoAvailable(MediaPlayer mediaPlayer);
    }

    //SurfaceView��ʼ����ɻص�
    private OnPlayerCreatedListener onPlayerCreatedListener;

    public void setOnPlayerCreatedListener(OnPlayerCreatedListener onPlayerCreatedListener) {
        this.onPlayerCreatedListener = onPlayerCreatedListener;
    }

    public interface OnPlayerCreatedListener {
        //������
        void onPlayerCreated(String url, String title);
    }

    //-----------------------������ص�
    private OnCompletionListener onCompletionListener;

    public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
        this.onCompletionListener = onCompletionListener;
    }

    public interface OnCompletionListener {
        void onCompletion(MediaPlayer mediaPlayer);
    }

}
