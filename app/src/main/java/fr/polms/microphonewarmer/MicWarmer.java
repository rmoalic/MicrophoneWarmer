package fr.polms.microphonewarmer;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

public class MicWarmer extends Service {
    private static final String TAG = "MicWarmer";
    private static final int bufferSize = 4096;
    private AudioRecord audio_record;
    private volatile boolean isRecording = false;
    private Thread recordingThread;
    public static final String ACTION_STATE_CHANGED = "fr.polms.microphonewarmer.STATE_CHANGED";
    public static final String EXTRA_IS_RUNNING = "is_running";
    public static boolean isRunning;

    public void start_recording_service() {
        if (isRecording && recordingThread.isAlive()) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "missing audio record permission");
            stopSelf();
            return;
        }
        this.audio_record = new AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
                    .setPrivacySensitive(false)
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(8000)
                            .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .build())
                    .setBufferSizeInBytes(bufferSize)
                    .build();
        if (this.audio_record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "audio_record initialization failed");
            stopSelf();
            return;
        }

        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        AudioDeviceInfo[] inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);

        AudioDeviceInfo preferredInputDevice = null;
        for (AudioDeviceInfo device : inputDevices) {
            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                preferredInputDevice = device;
                break;
            }
        }
        this.audio_record.setPreferredDevice(preferredInputDevice);
        this.audio_record.startRecording();
        this.isRecording = true;

        this.recordingThread = new Thread(this::writeAudioData, "AudioRecorderThread");
        this.recordingThread.start();

        Toast.makeText(this, R.string.toast_micwarmer_service_started, Toast.LENGTH_SHORT).show();
    }

    private void stop_recording() {
        if (this.audio_record != null && audio_record.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            this.audio_record.stop();
        }
        this.isRecording = false;
    }
    private void writeAudioData()  {
        var audioBuffer = new byte[bufferSize];
        sendStateBroadcast(true);

        try {
            while (isRecording) {
                int bytesRead = this.audio_record.read(audioBuffer, 0, bufferSize);
                if (bytesRead > 0) {
                    Log.v(TAG, "Read " + bytesRead + " bytes of audio data.");
                } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION || bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Error reading audio data: " + bytesRead);
                    break;
                }
                Thread.sleep(10000);
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "thread interrupted");
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        stop_recording();
        if (this.audio_record != null) {
            this.audio_record.release();
            this.audio_record = null;
        }
        sendStateBroadcast(false);
        MicWarmer.isRunning = false;
    }

    private void sendStateBroadcast(boolean running) {
        Intent intent = new Intent(ACTION_STATE_CHANGED);
        intent.putExtra(EXTRA_IS_RUNNING, running);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        if (this.recordingThread != null) {
            this.recordingThread.interrupt();
        }
        cleanup();
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MicWarmer.isRunning = true;
        this.startForeground();
    }

    private void startForeground() {
        Notification notification = new NotificationCompat.Builder(this, "MicWarmerNotification")
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
        ServiceCompat.startForeground(this, 100, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        this.start_recording_service();
    }


    @Override
    public IBinder onBind(Intent intent) {
       return null;
    }


}