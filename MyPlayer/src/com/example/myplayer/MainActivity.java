package com.example.myplayer;

import com.example.player.MNViderPlayer;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String TAG = "MNViderPlayer";

    private final String url1 = "http://svideo.spriteapp.com/video/2016/0703/7b5bc740-4134-11e6-ac2b-d4ae5296039d_wpd.mp4";
    private final String url2 = "http://bvideo.spriteapp.cn/video/2016/0704/577a4c29e1f14_wpd.mp4";
    //�����ַ�Ǵ����
    private final String url3 = "http://weibo.com/p/23044451f0e5c4b762b9e1aa49c3091eea4d94";
    //����rtspЭ��ĵ�ַ
    private final String url4 = "rtsp://192.168.1.101:554/18.mp4";
    //����rtmpЭ��ĵ�ַ
    private final String url5 = "rtmp://10.10.153.37/oflaDemo/guichuideng.mp4";
    private MNViderPlayer mnViderPlayer;
    private PowerManager powerManager;
    private WakeLock wakeLock;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        powerManager = (PowerManager) this.getSystemService(Service.POWER_SERVICE);
//        wakeLock = this.powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");  
//        wakeLock.setReferenceCounted(false);//�Ƿ���Ҫ������������
        mnViderPlayer = (MNViderPlayer) findViewById(R.id.mn_videoplayer);
        
        initPlayer();

    }
    
    private void initPlayer() {
        //��ʼ����ز���(�������Playǰ��)
        mnViderPlayer.setIsNeedBatteryListen(true);
        mnViderPlayer.setIsNeedNetChangeListen(true);
        //��һ�ν�������������
        mnViderPlayer.setDataSource(url2, "����2");

        //������ɼ���
        mnViderPlayer.setOnCompletionListener(new MNViderPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                Log.i(TAG, "�������----");
            }
        });

        //�������
        mnViderPlayer.setOnNetChangeListener(new MNViderPlayer.OnNetChangeListener() {
            @Override
            public void onWifi(MediaPlayer mediaPlayer) {
            }

            @Override
            public void onMobile(MediaPlayer mediaPlayer) {
                Toast.makeText(MainActivity.this, "��ע��,��ǰ����״̬�л�Ϊ3G/4G����", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNoAvailable(MediaPlayer mediaPlayer) {
                Toast.makeText(MainActivity.this, "��ǰ���粻����,�����������", Toast.LENGTH_LONG).show();
            }
        });

    }

    public void btn01(View view) {
        mnViderPlayer.playVideo(url1, "����1");
    }

    public void btn02(View view) {
        //position��ʾ��Ҫ��ת����λ��
        mnViderPlayer.playVideo(url2, "����2", 1000);
    }

    public void btn03(View view) {
        mnViderPlayer.playVideo(url3, "����3");
//        wakeLock.release();
    }
    
    public void btn04(View view) {
        mnViderPlayer.playVideo(url4, "����3");
    }
    
    public void btn05(View view) {
        mnViderPlayer.playVideo(url5, "����3");
    }

    @Override
    public void onBackPressed() {
        if (mnViderPlayer.isFullScreen()) {
            mnViderPlayer.setOrientationPortrait();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mnViderPlayer.pauseVideo();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        wakeLock.acquire();
        Log.i("zst", "1111111111");
    }

    @Override
    protected void onDestroy() {
        //һ��Ҫ�ǵ�����View
        if(mnViderPlayer != null){
            mnViderPlayer.destroyVideo();
            mnViderPlayer = null;
        }
        super.onDestroy();
    }
}
