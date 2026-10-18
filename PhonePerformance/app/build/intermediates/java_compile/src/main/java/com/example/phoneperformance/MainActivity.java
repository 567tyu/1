package com.example.phoneperformance;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.phoneperformance.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ScheduledExecutorService scheduler;
    private boolean isMonitoring = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
        registerBatteryReceiver();
    }

    private void setupUI() {
        binding.btnStartStop.setOnClickListener(v -> toggleMonitoring());
        binding.btnRefresh.setOnClickListener(v -> refreshAllStats());
        updateStartStopButton();
    }

    private void toggleMonitoring() {
        isMonitoring = !isMonitoring;
        updateStartStopButton();
        
        if (isMonitoring) {
            startMonitoring();
            Toast.makeText(this, "开始监控", Toast.LENGTH_SHORT).show();
        } else {
            stopMonitoring();
            Toast.makeText(this, "已停止监控", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStartStopButton() {
        if (isMonitoring) {
            binding.btnStartStop.setText(R.string.stop_monitoring);
            binding.btnStartStop.setBackgroundTintList(getColorStateList(R.color.red_500));
        } else {
            binding.btnStartStop.setText(R.string.start_monitoring);
            binding.btnStartStop.setBackgroundTintList(getColorStateList(R.color.primary_500));
        }
    }

    private void startMonitoring() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            runOnUiThread(() -> {
                updateCpuStats();
                updateMemoryStats();
                updateBatteryStats();
                updateStorageStats();
                updateTemperatureStats();
                updateProcessCount();
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void stopMonitoring() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    private void refreshAllStats() {
        updateCpuStats();
        updateMemoryStats();
        updateBatteryStats();
        updateStorageStats();
        updateTemperatureStats();
        updateProcessCount();
    }

    private void updateCpuStats() {
        try {
            double[] cpuPercentages = getCpuUsagePercentages();
            binding.tvCpuUsage.setText(String.format("CPU: %.1f%%", cpuPercentages[0]));
            binding.progressCpu.setProgress((int) cpuPercentages[0]);
            
            int cardColor = getCardColor(cpuPercentages[0]);
            binding.cardCpu.setCardBackgroundColor(getColor(cardColor));
        } catch (Exception e) {
            binding.tvCpuUsage.setText("CPU: N/A");
        }
    }

    private void updateMemoryStats() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            android.os.Debug.MemoryInfo[] memoryInfo = 
                am.getProcessMemoryInfo(new int[]{android.os.Process.myPid()});
            
            long totalPss = memoryInfo[0].getTotalPss() / 1024;
            binding.tvMemoryUsage.setText(String.format("内存: %.1f MB", totalPss));
            binding.progressMemory.setProgress(Math.min(100, (int)(totalPss / 10)));
        } catch (Exception e) {
            binding.tvMemoryUsage.setText("内存: N/A");
        }
    }

    private void updateBatteryStats() {
        try {
            Intent batteryIntent = registerReceiver(null, 
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(
                    android.os.BatteryManager.EXTRA_LEVEL, 0);
                int scale = batteryIntent.getIntExtra(
                    android.os.BatteryManager.EXTRA_SCALE, 1);
                int status = batteryIntent.getIntExtra(
                    android.os.BatteryManager.EXTRA_STATUS, -1);
                
                boolean isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING 
                    || status == android.os.BatteryManager.BATTERY_STATUS_FULL;
                
                String statusText = isCharging ? "充电中" : "放电中";
                binding.tvBatteryUsage.setText(
                    String.format("电池: %d%% (%s)", level, statusText));
                binding.progressBattery.setProgress(level);
                
                int cardColor = level < 20 ? R.color.red_500 : 
                               level < 50 ? R.color.orange_500 : R.color.primary_container;
                binding.cardBattery.setCardBackgroundColor(getColor(cardColor));
            }
        } catch (Exception e) {
            binding.tvBatteryUsage.setText("电池: N/A");
        }
    }

    private void updateStorageStats() {
        try {
            android.os.StatFs statFs = new android.os.StatFs(getFilesDir().getPath());
            long totalBytes = statFs.getTotalBytes();
            long availableBytes = statFs.getAvailableBytes();
            long usedBytes = totalBytes - availableBytes;
            
            String totalStr = formatFileSize(totalBytes);
            String usedStr = formatFileSize(usedBytes);
            String availableStr = formatFileSize(availableBytes);
            
            binding.tvStorageUsage.setText(String.format("存储: %s / %s", usedStr, totalStr));
            binding.tvStorageAvailable.setText("可用: " + availableStr);
            
            int progress = (int) ((usedBytes * 100) / totalBytes);
            binding.progressStorage.setProgress(progress);
        } catch (Exception e) {
            binding.tvStorageUsage.setText("存储: N/A");
        }
    }

    private void updateTemperatureStats() {
        try {
            float temperature = getAverageTemperature();
            binding.tvTemperature.setText(String.format("温度: %.1f°C", temperature));
            binding.progressTemperature.setProgress((int) Math.min(100, (temperature / 50) * 100));
            
            int cardColor = temperature > 40 ? R.color.red_500 : 
                           temperature > 30 ? R.color.orange_500 : R.color.primary_container;
            binding.cardTemperature.setCardBackgroundColor(getColor(cardColor));
        } catch (Exception e) {
            binding.tvTemperature.setText("温度: N/A");
        }
    }

    private void updateProcessCount() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> processes = 
                am.getRunningAppProcesses();
            int count = processes != null ? processes.size() : 0;
            binding.tvProcessCount.setText("进程数: " + count);
        } catch (Exception e) {
            binding.tvProcessCount.setText("进程数: N/A");
        }
    }

    private double[] getCpuUsagePercentages() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"), 8 * 1024);
            String line = reader.readLine();
            reader.close();
            
            if (line == null || !line.startsWith("cpu ")) {
                return new double[]{0, 0, 0, 0};
            }
            
            String[] parts = line.split("\\s+");
            long user = Long.parseLong(parts[1]);
            long nice = Long.parseLong(parts[2]);
            long system = Long.parseLong(parts[3]);
            long idle = Long.parseLong(parts[4]);
            
            long total = user + nice + system + idle;
            long totalIdle = idle;
            
            return new double[]{
                (total > 0) ? (100.0 * (total - totalIdle) / total) : 0,
                (total > 0) ? (100.0 * user / total) : 0,
                (total > 0) ? (100.0 * system / total) : 0,
                (total > 0) ? (100.0 * nice / total) : 0
            };
        } catch (IOException e) {
            return new double[]{0, 0, 0, 0};
        }
    }

    private float getAverageTemperature() {
        try {
            float sum = 0;
            int count = 0;
            
            for (int i = 0; i <= 10; i++) {
                try {
                    java.io.File tempFile = new java.io.File(
                        "/sys/class/thermal/thermal_zone" + i + "/temp");
                    if (tempFile.exists()) {
                        BufferedReader reader = new BufferedReader(new FileReader(tempFile));
                        String line = reader.readLine();
                        reader.close();
                        
                        if (line != null) {
                            float temp = Float.parseFloat(line) / 1000;
                            if (temp > 0 && temp < 100) {
                                sum += temp;
                                count++;
                            }
                        }
                    }
                } catch (Exception e) {
                    // 跳过这个传感器
                }
            }
            
            return count > 0 ? sum / count : 25.0f;
        } catch (Exception e) {
            return 25.0f;
        }
    }

    private int getCardColor(double cpuUsage) {
        if (cpuUsage > 80) {
            return R.color.red_500;
        } else if (cpuUsage > 50) {
            return R.color.orange_500;
        }
        return R.color.primary_container;
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        return NumberFormat.getInstance().format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private void registerBatteryReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    int percentage = (level * 100) / scale;
                    binding.tvBatteryUsage.setText(String.format("电池: %d%%", percentage));
                    binding.progressBattery.setProgress(percentage);
                }
            }
        }, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllStats();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMonitoring();
        binding = null;
    }
}
