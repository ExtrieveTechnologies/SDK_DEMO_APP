package com.extrieve.demo;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.extrieve.quickcapture.sdk.DeviceGuard;

import java.util.HashMap;
import java.util.Map;

public class DeviceGuardActivity extends AppCompatActivity {
    // Containers
    private LinearLayout iconRootLinear, iconHALinear, iconDebugLinear, iconVpnLinear, iconLocLinear;
    // ImageView
    private static ImageView iconRoot, iconHA, iconDebug, iconVpn, iconLoc, wholeStatus;
    // TextViews
    private TextView txtRootDesc, txtRootPolicyError, txtRootStatus, txtHADesc, txtHAPolicyError, txtHAStatus, txtDebugDesc, txtDebugPolicyError, txtDebugStatus, txtVpnDesc, txtVpnPolicyError, txtVpnStatus, txtLocDesc, txtLocPolicyError, txtLocStatus, txtDeviceGuardTitle, txtDeviceGuardSubTitle, realTimeDenote;
    // Switch
    private SwitchCompat switchDeviceGuard, switchScreenProtect;
    private Button deviceGuardCheck;
    private LinearLayout layoutStatus;
    private static FrameLayout frameStatus;
    private ObjectAnimator rotateAnimator;
    private SharedPreferences prefs;
    private final Map<String, Object> currentReport = new HashMap<>();
    private boolean checkCompleted = true;
    DeviceGuard guard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_guard);

        Window window = getWindow();
        window.setStatusBarColor(getResources().getColor(R.color.lightBtnColor));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // ================= Views =================
        iconRootLinear = findViewById(R.id.iconRootLinear);
        iconHALinear = findViewById(R.id.iconHALinear);
        iconDebugLinear = findViewById(R.id.iconDebugLinear);
        iconVpnLinear = findViewById(R.id.iconVpnLinear);
        iconLocLinear = findViewById(R.id.iconLocLinear);
        // ================= ImageViews =================
        iconRoot = findViewById(R.id.iconRoot);
        iconHA = findViewById(R.id.iconHA);
        iconDebug = findViewById(R.id.iconDebug);
        iconVpn = findViewById(R.id.iconVpn);
        iconLoc = findViewById(R.id.iconLoc);
        // ================= TextViews =================
        txtRootDesc = findViewById(R.id.txtRootDesc);
        txtRootPolicyError = findViewById(R.id.txtRootPolicyError);
        txtRootStatus = findViewById(R.id.txtRootStatus);

        txtHADesc = findViewById(R.id.txtHADesc);
        txtHAPolicyError = findViewById(R.id.txtHAPolicyError);
        txtHAStatus = findViewById(R.id.txtHAStatus);

        txtDebugDesc = findViewById(R.id.txtDebugDesc);
        txtDebugPolicyError = findViewById(R.id.txtDebugPolicyError);
        txtDebugStatus = findViewById(R.id.txtDebugStatus);

        txtVpnDesc = findViewById(R.id.txtVpnDesc);
        txtVpnPolicyError = findViewById(R.id.txtVpnPolicyError);
        txtVpnStatus = findViewById(R.id.txtVpnStatus);

        txtLocDesc = findViewById(R.id.txtLocDesc);
        txtLocPolicyError = findViewById(R.id.txtLocPolicyError);
        txtLocStatus = findViewById(R.id.txtLocStatus);
        // Switches
        switchDeviceGuard = findViewById(R.id.switchDeviceGuard);
        switchScreenProtect = findViewById(R.id.switchScreenProtect);
        layoutStatus = findViewById(R.id.layoutStatus);
        frameStatus = findViewById(R.id.frameStatus);
        wholeStatus = findViewById(R.id.wholeStatus);
        deviceGuardCheck = findViewById(R.id.device_guard_check);
        txtDeviceGuardTitle = findViewById(R.id.device_guard_title);
        txtDeviceGuardSubTitle = findViewById(R.id.device_guard_subtitle);
        realTimeDenote = findViewById(R.id.realTimeDenote);

        // Shared pref reading
        guard = DeviceGuard.getInstance(DeviceGuardActivity.this);
        prefs = getSharedPreferences("security_prefs", MODE_PRIVATE);
        boolean deviceGuardEnabled = prefs.getBoolean("device_guard", false);
        boolean screenProtectEnabled = prefs.getBoolean("screen_protect", false);
        switchDeviceGuard.setChecked(deviceGuardEnabled);
        switchScreenProtect.setChecked(screenProtectEnabled);
        if (screenProtectEnabled) enableScreenProtection();
        switchDeviceGuard.setOnCheckedChangeListener((v, enabled) -> {
            prefs.edit().putBoolean("device_guard", enabled).apply();
            if (enabled) {
                realTimeDenote.setVisibility(View.VISIBLE);
                runDeviceGuard();
            } else {
                layoutStatus.setVisibility(View.GONE);
                realTimeDenote.setVisibility(View.INVISIBLE);
                txtDeviceGuardTitle.setText("Verify Integrity");
                txtDeviceGuardSubTitle.setText("Check against security policy");
                frameStatus.setActivated(false);
                wholeStatus.setImageResource(R.drawable.ic_reload_data);
                wholeStatus.setColorFilter(ContextCompat.getColor(wholeStatus.getContext(), R.color.darkFontColor), PorterDuff.Mode.SRC_IN);
                txtDeviceGuardTitle.setTextColor(ContextCompat.getColor(txtDeviceGuardTitle.getContext(), R.color.darkFontColor));
            }
        });
        switchScreenProtect.setOnCheckedChangeListener((v, enabled) -> {
            prefs.edit().putBoolean("screen_protect", enabled).apply();
            if (enabled) enableScreenProtection();
            else disableScreenProtection();
        });
        deviceGuardCheck.setOnClickListener(v -> {
            runDeviceGuard();
        });

        if (deviceGuardEnabled) {
            realTimeDenote.setVisibility(View.VISIBLE);
            runDeviceGuard();
        } else {
            layoutStatus.setVisibility(View.GONE);
            realTimeDenote.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        guard.stopMonitoring();
    }

    private void runDeviceGuard() {
        if (!checkCompleted) return;
        checkCompleted = false;
        frameStatus.setActivated(false);
        wholeStatus.setImageResource(R.drawable.ic_reload_data);
        wholeStatus.setColorFilter(ContextCompat.getColor(wholeStatus.getContext(), R.color.darkFontColor), PorterDuff.Mode.SRC_IN);
        deviceGuardCheck.setText("Verifying...");
        deviceGuardCheck.setBackgroundResource(R.drawable.bg_light_gray);
        txtDeviceGuardTitle.setText("Verifying Integrity...");
        txtDeviceGuardTitle.setTextColor(ContextCompat.getColor(txtDeviceGuardTitle.getContext(), R.color.darkFontColor));
        startRefreshAnimation();

        guard.getSecurityReport(report -> {
            currentReport.clear();
            currentReport.putAll(report);
            runOnUiThread(() -> {
                renderSecurityStatus(report);
                startRealtimeMonitoring();
            });
        });
    }

    private void startRealtimeMonitoring() {
        guard.startMonitoring((threatType, message) -> {
            runOnUiThread(() -> handleRealtimeThreat(threatType, message));
        });
    }

    private void handleRealtimeThreat(String threatType, String message) {
        String mappedKey = null;
        switch (threatType) {
            case "ROOT_DETECTED":
                mappedKey = "ROOT";
                break;

            case "EMULATOR_DETECTED":
                mappedKey = "HARDWARE";
                break;

            case "ADB_ENABLED":
                mappedKey = "DEBUG";
                break;

            case "VPN_CONNECTED":
                mappedKey = "NETWORK";
                break;

            case "LOCATION_SPOOFED":
                mappedKey = "LOCATION";
                currentReport.put("locationMessage", message);
                break;

            default:
                return;
        }
        // Only update if not already true (prevents unnecessary UI redraw)
        if (!Boolean.TRUE.equals(currentReport.get(mappedKey))) {
            currentReport.put(mappedKey, true);
            renderSecurityStatus(currentReport);
        }
    }

    private void renderSecurityStatus(Map<String, Object> report) {
        int violation = 0;
        layoutStatus.setVisibility(View.GONE);
        if (report == null || report.isEmpty()) return;

        for (Map.Entry<String, Object> entry : report.entrySet()) {
            String type = entry.getKey();
            boolean condition = false;
            if (entry.getValue() instanceof Boolean) {
                condition = (Boolean) entry.getValue();
                if (type.equalsIgnoreCase("ISDEVOPTIONSENABLED")) continue;
                if (condition) violation++;
            }

            switch (type.toUpperCase()) {
                case "ISROOTED":
                    UpdatePolicyUI("ROOT", condition, txtRootDesc, txtRootPolicyError, iconRootLinear, txtRootStatus, iconRoot);
                    break;

                case "ISEMULATOR":
                    UpdatePolicyUI("HARDWARE", condition, txtHADesc, txtHAPolicyError, iconHALinear, txtHAStatus, iconHA);
                    break;

                case "ISADBENABLED":
                    UpdatePolicyUI("DEBUG", condition, txtDebugDesc, txtDebugPolicyError, iconDebugLinear, txtDebugStatus, iconDebug);
                    break;

                case "ISVPNACTIVE":
                    UpdatePolicyUI("NETWORK", condition, txtVpnDesc, txtVpnPolicyError, iconVpnLinear, txtVpnStatus, iconVpn);
                    break;

                case "ISLOCATIONSPOOFED":
                    UpdatePolicyUI("LOCATION", condition, txtLocDesc, txtLocPolicyError, iconLocLinear, txtLocStatus, iconLoc);
                    break;
            }
        }

        int finalViolation = violation;
        wholeStatus.postDelayed(() -> {
            stopRefreshAnimation();
            if (finalViolation > 0) {
                txtDeviceGuardTitle.setText("Risk Detected");
                txtDeviceGuardSubTitle.setText("Device environment does not meet security policy");
                txtDeviceGuardTitle.setTextColor(ContextCompat.getColor(txtDeviceGuardTitle.getContext(), R.color.darkRedColor));
                frameStatus.setActivated(true);
                wholeStatus.setImageResource(R.drawable.unselect);
                wholeStatus.setColorFilter(ContextCompat.getColor(wholeStatus.getContext(), R.color.darkRedColor), PorterDuff.Mode.SRC_IN);
            } else {
                txtDeviceGuardTitle.setText("No Risk Found");
                txtDeviceGuardSubTitle.setText("Your device environment meets all security policies");
                txtDeviceGuardTitle.setTextColor(ContextCompat.getColor(txtDeviceGuardTitle.getContext(), R.color.darkBtnColor));
                frameStatus.setActivated(false);
                wholeStatus.setImageResource(R.drawable.secure);
                wholeStatus.setColorFilter(ContextCompat.getColor(wholeStatus.getContext(), R.color.darkBtnColor), PorterDuff.Mode.SRC_IN);
            }
            deviceGuardCheck.setText("Verify Environment");
            ScrollView scrollView = findViewById(R.id.bodyScroll);
            scrollView.post(() -> {
                scrollView.smoothScrollTo(0, scrollView.getChildAt(0).getBottom());
            });
            layoutStatus.setVisibility(View.VISIBLE);
            deviceGuardCheck.setBackgroundResource(R.drawable.bg_top);
            for (int i = 0; i < layoutStatus.getChildCount(); i++) {
                View child = layoutStatus.getChildAt(i);
                child.setVisibility(View.INVISIBLE);
                child.setTranslationY(30f);
                int delay = i * 200;

                child.postDelayed(() -> {
                    child.setVisibility(View.VISIBLE);
                    child.animate().translationY(0f).alpha(1f).setDuration(500).setInterpolator(new DecelerateInterpolator()) // smooth end
                            .start();
                }, delay);
            }
            checkCompleted = true;
        }, 3000);
    }

    public static void UpdatePolicyUI(String type, boolean condition, TextView msgView, TextView policyView, LinearLayout linearIcon, TextView txtStatus, ImageView iconView) {
        String msg = "";
        String policyMsg = "";

        switch (type) {
            case "ROOT":
                msg = condition ? "Root access detected on this device. This may compromise app security and data protection." : "Device security verified. No root access detected.";
                policyMsg = condition ? "Policy Violation: Device integrity compromised." : "";
                break;

            case "HARDWARE":
                msg = condition ? "Device identity could not be verified. The device may be modified or running unofficial software." : "Device identity verified. Security keys match certified hardware.";
                policyMsg = condition ? "Policy Violation: Hardware trust validation failed." : "";
                break;

            case "DEBUG":
                msg = condition ? "USB or wireless debugging is enabled. This increases the risk of unauthorized data access." : "Debugging interfaces are disabled. Device is secure from external debugging access.";
                policyMsg = condition ? "Policy Violation: Debug access detected." : "";
                break;

            case "NETWORK":
                msg = condition ? "Potential network manipulation detected. Traffic may be intercepted or redirected." : "Secure network connection established. No suspicious traffic detected.";
                policyMsg = condition ? "Policy Violation: Network integrity compromised." : "";
                break;

            case "LOCATION":
                msg = condition ? "Location data may be spoofed or manipulated. Accuracy cannot be guaranteed." : "Location authenticity verified. GNSS signals are trusted.";
                policyMsg = condition ? "Policy Violation: Location authenticity failed." : "";
                break;
        }

        msgView.setText(msg);
        boolean isFail = !policyMsg.isEmpty();
        if (isFail) {
            policyView.setText(policyMsg);
            policyView.setVisibility(View.VISIBLE);
            txtStatus.setText("☒ Fail");
            txtStatus.setTextColor(ContextCompat.getColor(txtStatus.getContext(), R.color.darkRedColor));
            iconView.setColorFilter(ContextCompat.getColor(iconView.getContext(), R.color.darkRedColor), PorterDuff.Mode.SRC_IN);
        } else {
            policyView.setVisibility(View.GONE);
            txtStatus.setText("☑ Pass");
            txtStatus.setTextColor(ContextCompat.getColor(txtStatus.getContext(), R.color.darkBtnColor));
            iconView.setColorFilter(ContextCompat.getColor(iconView.getContext(), R.color.darkBtnColor), PorterDuff.Mode.SRC_IN);
        }

        txtStatus.setTypeface(null, Typeface.BOLD);
        GradientDrawable bg1 = new GradientDrawable();
        bg1.setShape(GradientDrawable.RECTANGLE);
        bg1.setCornerRadius(12f);
        bg1.setStroke(1, isFail ? Color.parseColor("#E95757") : Color.parseColor("#50802D"));
        bg1.setColor(isFail ? Color.parseColor("#FAE6E7") : Color.parseColor("#DCFCE7"));
        txtStatus.setBackground(bg1);
        txtStatus.setPadding(30, 20, 30, 20);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(12f);
        bg.setColor(isFail ? Color.parseColor("#FAE6E7") : Color.parseColor("#DCFCE7"));
        linearIcon.setBackground(bg);
    }

    private void startRefreshAnimation() {
        if (rotateAnimator == null) {
            rotateAnimator = ObjectAnimator.ofFloat(wholeStatus, View.ROTATION, 0f, 360f);
            rotateAnimator.setDuration(750);
            rotateAnimator.setRepeatCount(ValueAnimator.INFINITE);
            rotateAnimator.setInterpolator(new LinearInterpolator());
        }
        rotateAnimator.start();
    }

    private void stopRefreshAnimation() {
        if (rotateAnimator != null) {
            rotateAnimator.cancel();
        }
        wholeStatus.setRotation(0f);
    }

    private void enableScreenProtection() {
        guard.enableScreenProtection(this);
    }

    private void disableScreenProtection() {
        guard.disableScreenProtection(this);
    }
}