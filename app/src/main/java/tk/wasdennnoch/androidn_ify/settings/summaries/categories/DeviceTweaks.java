package tk.wasdennnoch.androidn_ify.settings.summaries.categories;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.BatteryStats;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.Settings;
import android.text.format.Formatter;

import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.util.MemInfoReader;

import java.io.File;
import java.text.NumberFormat;
import java.util.List;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.utils.StringUtils;

public class DeviceTweaks {

    public static void hookDisplayTile(Object tile, Context context) {
        int brightnessMode = Settings.System.getInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        String status = brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL ? StringUtils.getInstance().getString(R.string.off) : StringUtils.getInstance().getString(R.string.on);
        String summary = StringUtils.getInstance().getString(R.string.display_summary, status);
        XposedHelpers.setObjectField(tile, "summary", summary);
    }

    public static void hookNotificationTile(Object tile, Context context) {
        hookApplicationTile(tile, context);
    }

    public static void hookSoundTile(Object tile, Context context) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_RING);
        int maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_RING);
        XposedHelpers.setObjectField(tile, "summary", StringUtils.getInstance().getString(R.string.sound_summary, formatPercentage(currentVolume, maxVolume)));
    }

    public static void hookApplicationTile(Object tile, Context context) {
        final PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(0);
        XposedHelpers.setObjectField(tile, "summary", StringUtils.getInstance().getString(R.string.apps_summary, packages.size()));
    }

    public static void hookStorageTile(Object tile, Context context) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        long totalBlocks = stat.getBlockCountLong();

        long available = availableBlocks * blockSize;
        long total = totalBlocks * blockSize;
        long used = total - available;

        String summary = StringUtils.getInstance().getString(R.string.storage_summary, Formatter.formatFileSize(context, used), Formatter.formatFileSize(context, total));
        XposedHelpers.setObjectField(tile, "summary", summary);
    }

    public static void hookBatteryTile(Object tile, Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        //noinspection ConstantConditions
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        //summary = String.valueOf((int) (((float)level / (float)scale) * 100.0f)) + "% - ";
        //summary = String.valueOf((level * 100) / scale) + "%";
        String summary = formatPercentage(level, scale);
        switch (plugged) {
            case 0:
                BatteryStatsHelper batteryStatsHelper = new BatteryStatsHelper(context, false);
                batteryStatsHelper.create(new Bundle());
                BatteryStats batteryStats = batteryStatsHelper.getStats();
                long remaining = batteryStats.computeBatteryTimeRemaining(SystemClock.elapsedRealtime()); // TODO Fix remaining time
                if (remaining > 0) {
                    summary += StringUtils.getInstance().getString(R.string.battery_approx);
                    Class<?> Formatter = XposedHelpers.findClass("android.text.format.Formatter", null);
                    summary += XposedHelpers.callStaticMethod(Formatter, "formatShortElapsedTime", context, remaining / 1000);
                    summary += StringUtils.getInstance().getString(R.string.battery_left);
                }
                break;
            case BatteryManager.BATTERY_PLUGGED_AC:
                summary += StringUtils.getInstance().getString(R.string.battery_charging_ac);
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                summary += StringUtils.getInstance().getString(R.string.battery_charging_usb);
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                summary += StringUtils.getInstance().getString(R.string.battery_charging_wireless);
                break;
        }
        XposedHelpers.setObjectField(tile, "summary", summary);
    }

    public static void hookMemoryTile(Object tile, Context context) {

        // Thanks to GravityBox for this snippet!
        ActivityManager mAm = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        MemInfoReader mMemInfoReader = new MemInfoReader();

        mAm.getMemoryInfo(memInfo);
        long secServerMem = 0;//XposedHelpers.getLongField(memInfo, "secondaryServerThreshold");
        mMemInfoReader.readMemInfo();
        long availMem = mMemInfoReader.getFreeSize() + mMemInfoReader.getCachedSize() - secServerMem;
        long totalMem = mMemInfoReader.getTotalSize();

        String summary = StringUtils.getInstance().getString(R.string.memory_summary, Formatter.formatShortFileSize(context, totalMem - availMem), Formatter.formatShortFileSize(context, totalMem));
        XposedHelpers.setObjectField(tile, "summary", summary);

    }

    public static void hookUserTile(Object tile, Context context) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        XposedHelpers.setObjectField(tile, "summary", StringUtils.getInstance().getString(R.string.user_summary, userManager.getUserName()));
    }


    private static String formatPercentage(long amount, long total) {
        return formatPercentage(((double) amount) / total);
    }

    private static String formatPercentage(double percentage) {
        return NumberFormat.getPercentInstance().format(percentage);
    }

}
