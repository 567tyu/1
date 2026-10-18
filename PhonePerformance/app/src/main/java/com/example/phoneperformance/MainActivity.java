package com.example.phoneperformance;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity {

    private TextView tvCpuUsage;
    private TextView tvMemoryUsage;
    private TextView tvBatteryUsage;
    private TextView tvStorageUsage;
    private TextView tvTemperature;
    private TextView tvProcessCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCpuUsage = findViewById(R.id.tvCpuUsage);
        tvMemoryUsage = findViewById(R.id.tvMemoryUsage);
        tvBatteryUsage = findViewById(R.id.tvBatteryUsage);
        tvStorageUsage = findViewById(R.id.tvStorageUsage);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvProcessCount = findViewById(R.id.tvProcessCount);

        refreshAllStats();
    }

    public void onRefresh(View view) {
        refreshAllStats();
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
            double cpuUsage = getCpuUsage();
            tvCpuUsage.setText(String.format("CPU: %.1f%%", cpuUsage));
        } catch (Exception e) {
            tvCpuUsage.setText("CPU: N/A");
        }
    }

    private void updateMemoryStats() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            android.os.Debug.MemoryInfo[] memoryInfo = 
                am.getProcessMemoryInfo(new int[]{android.os.Process.myPid()});
            long totalPss = memoryInfo[0].getTotalPss() / 1024;
            tvMemoryUsage.setText(String.format("内存: %.1f MB", totalPss));
        } catch (Exception e) {
            tvMemoryUsage.setText("内存: N/A");
        }
    }

    private void updateBatteryStats() {
        try {
            android.content.Intent batteryIntent = registerReceiver(null, 
                new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0);
                int status = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING 
                    || status == android.os.BatteryManager.BATTERY_STATUS_FULL;
                String statusText = isCharging ? "充电中" : "放电中";
                tvBatteryUsage.setText(String.format("电池: %d%% (%s)", level, statusText));
            }
        } catch (Exception e) {
            tvBatteryUsage.setText("电池: N/A");
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
            tvStorageUsage.setText(String.format("存储: %s / %s", usedStr, totalStr));
        } catch (Exception e) {
            tvStorageUsage.setText("存储: N/A");
        }
    }

    private void updateTemperatureStats() {
        try {
            float temperature = getAverageTemperature();
            tvTemperature.setText(String.format("温度: %.1f°C", temperature));
        } catch (Exception e) {
            tvTemperature.setText("温度: N/A");
        }
    }

    private void updateProcessCount() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            int count = am.getRunningAppProcesses() != null ? 
                am.getRunningAppProcesses().size() : 0;
            tvProcessCount.setText("进程数: " + count);
        } catch (Exception e) {
            tvProcessCount.setText("进程数: N/A");
        }
    }

    private double getCpuUsage() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/stat"), 8 * 1024);
            String line = reader.readLine();
            reader.close();
            
            if (line == null || !line.startsWith("cpu ")) {
                return 0;
            }
            
            String[] parts = line.split("\\s+");
            long user = Long.parseLong(parts[1]);
            long nice = Long.parseLong(parts[2]);
            long system = Long.parseLong(parts[3]);
            long idle = Long.parseLong(parts[4]);
            
            long total = user + nice + system + idle;
            return (total > 0) ? (100.0 * (total - idle) / total) : 0;
        } catch (IOException e) {
            return 0;
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

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        return NumberFormat.getInstance().format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
