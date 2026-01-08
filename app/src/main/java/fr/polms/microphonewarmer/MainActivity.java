package fr.polms.microphonewarmer;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import fr.polms.microphonewarmer.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1005;
    private ActivityMainBinding binding;
    private BroadcastReceiver serviceReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Apparently, not asking for the post notification permission allow the use of foreground service with no notification visible
        createNotificationChannel();

        updateButtons(MicWarmer.isRunning);

        serviceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (MicWarmer.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                    boolean isRunning = intent.getBooleanExtra(MicWarmer.EXTRA_IS_RUNNING, false);
                    updateButtons(isRunning);
                }
            }
        };

        binding.startButton.setOnClickListener((l) -> {
            if (hasMicPermission()) {
                startWarmerService();
            } else {
                requestMicPermission(); // will start the service in onRequestPermissionsResult
            }
        });

        binding.stopButton.setOnClickListener((l) -> stopWarmerService());
    }

    private void updateButtons(boolean isRunning) {
        binding.stopButton.setEnabled(isRunning);
        binding.startButton.setEnabled(! isRunning);
        if (isRunning) {
            binding.serviceStatus.setText(R.string.service_started);
        } else {
            binding.serviceStatus.setText(R.string.service_stopped);
        }
    }

    private void startWarmerService() {
        Intent intent = new Intent(this, MicWarmer.class);
        startForegroundService(intent);
    }

    private void stopWarmerService() {
        Intent intent = new Intent(this, MicWarmer.class);
        stopService(intent);
    }

    private boolean hasMicPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                "MicWarmerNotification",
                "Basic",
                NotificationManager.IMPORTANCE_HIGH
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 startWarmerService();
            } else {
                Toast.makeText(this, R.string.toast_permission_denied_cannot_warm_mic, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(this, serviceReceiver, new IntentFilter(MicWarmer.ACTION_STATE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(serviceReceiver);
    }
}