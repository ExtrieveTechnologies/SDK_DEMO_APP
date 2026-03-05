package com.extrieve.demo;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.extrieve.quickcapture.sdk.DeviceInfo;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class DigiPinActivity extends AppCompatActivity {
    private ImageView btnRefresh;
    // ===== MODE 1 : CURRENT LOCATION =====
    private Button btnGetDigiPin, btnGetLocation, btnGetPostalCode, btnOpenMap;
    private TextView txtDigiPin, txtLat, txtLng, txtAccuracy, txtPostalCode, txtTime, moveToCustom;
    private ImageButton btnCopyDigiPin, btnCopyLat, btnCopyLng, btnCopyPostal;
    private String main_longitude = "", main_latitude = "";
    // ===== MODE 2 : LAT / LNG INPUT =====
    private EditText edtLat, edtLng;
    private Button btnGenerateLatLng;
    private TextView txtLatLngDigiPin;
    private ImageButton btnCopyLatLngDigiPin;
    private LinearLayout linearLatLngDigiPin;
    private long loaderStartTime1, loaderStartTime2, loaderStartTime3, loaderStartTime4;
    private static final long MIN_LOADER_TIME = 1000; // 2 sec
    DeviceInfo deviceInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digipin);

        Window window = getWindow();
        window.setStatusBarColor(getResources().getColor(R.color.lightBtnColor));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        initViews();
        initLoader();
        deviceInfo = new DeviceInfo(this);
        btnGetDigiPin.setOnClickListener(v -> fetchDigiPin(1));
        btnGetLocation.setOnClickListener(v -> fetchDigiPin(2));
        btnGetPostalCode.setOnClickListener(v -> fetchDigiPin(3));
        btnGenerateLatLng.setOnClickListener(v -> fetchDigiPinOnly());
        btnOpenMap.setOnClickListener(v -> {
            String uri = "https://www.google.com/maps?q=" + main_latitude + "," + main_longitude;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // If Google Maps app not installed, open in browser
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
            }
        });
        btnRefresh.setOnClickListener(v -> refreshOrResetUI());
        moveToCustom.setOnClickListener(v -> {
            ScrollView scrollView = findViewById(R.id.bodyScrollView);
            scrollView.post(() -> {
                scrollView.smoothScrollTo(0, scrollView.getChildAt(0).getBottom());
            });
        });
    }

    private void refreshOrResetUI() {
        RotateAnimation rotateAnimation = new RotateAnimation(0F, 360F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(1000);
        rotateAnimation.setRepeatCount(1);
        btnRefresh.startAnimation(rotateAnimation);
        edtLat.setText("");
        edtLng.setText("");
        recreate();
    }

    public boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return false;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void initViews() {
        btnRefresh = findViewById(R.id.refresh);
        // Mode 1
        btnGetDigiPin = findViewById(R.id.btnGetDigiPin);
        txtDigiPin = findViewById(R.id.txtDigiPin);
        btnCopyDigiPin = findViewById(R.id.btnCopyDigiPin);

        btnGetLocation = findViewById(R.id.btnGetLocation);
        txtLat = findViewById(R.id.txtLat);
        btnCopyLat = findViewById(R.id.btnCopyLat);
        txtLng = findViewById(R.id.txtLng);
        btnCopyLng = findViewById(R.id.btnCopyLng);
        txtAccuracy = findViewById(R.id.txtAccuracy);

        btnGetPostalCode = findViewById(R.id.btnGetPostalCode);
        txtPostalCode = findViewById(R.id.txtPostalCode);
        btnCopyPostal = findViewById(R.id.btnCopyPostal);

        txtTime = findViewById(R.id.txtTime);
        btnOpenMap = findViewById(R.id.btnOpenMap);
        btnOpenMap.setVisibility(View.GONE);
        moveToCustom = findViewById(R.id.moveToCustom);
        // Mode 2
        edtLat = findViewById(R.id.edtLat);
        edtLng = findViewById(R.id.edtLng);
        btnGenerateLatLng = findViewById(R.id.btnGenerateLatLng);
        txtLatLngDigiPin = findViewById(R.id.txtLatLngDigiPin);
        btnCopyLatLngDigiPin = findViewById(R.id.btnCopyLatLngDigiPin);
        linearLatLngDigiPin = findViewById(R.id.linearLatLngDigiPin);
    }

    private void fetchDigiPin(int type) {
        btnOpenMap.setVisibility(View.INVISIBLE);
        if (deviceInfo.hasLocationPermissions()) {
            if (isLocationEnabled()) {
                if (type == 1) showLoader(1, "Generating DigiPin");
                if (type == 2) showLoader(2, "Fetching Location");
                if (type == 3) showLoader(3, "Fetching PinCode");
                deviceInfo.getCurrentLocation(new DeviceInfo.LocationCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        hideLoader(type, () -> {
                            if (type == 1) {
                                setField(txtDigiPin, btnCopyDigiPin, data.get("digi_pin"));
                            }
                            if (type == 2) {
                                main_latitude = main_longitude = "";
                                double accuracy = Double.parseDouble(data.get("accuracy").toString());
                                String formattedAccuracy = String.format(Locale.US, "%.2f", accuracy);
                                setField(txtAccuracy, null, "± " + formattedAccuracy + "m");

                                DecimalFormat df = new DecimalFormat("#.######");
                                df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));

                                double lat = Double.parseDouble(data.get("latitude").toString());
                                main_latitude = df.format(lat);
                                setField(txtLat, btnCopyLat, main_latitude);

                                double lan = Double.parseDouble(data.get("longitude").toString());
                                main_longitude = df.format(lan);
                                setField(txtLng, btnCopyLng, main_longitude);

                                long timestamp = (long) data.get("time");
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                sdf.setTimeZone(TimeZone.getDefault());
                                String formattedTime = sdf.format(new Date(timestamp));
                                setField(txtTime, null, formattedTime);
                            }
                            if (type == 3) {
                                setField(txtPostalCode, btnCopyPostal, data.get("postal_code").equals("Error") ? "---" : data.get("postal_code"));
                            }
                            if (!main_latitude.isEmpty() && !main_longitude.isEmpty())
                                btnOpenMap.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        hideLoader(type, () -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(DigiPinActivity.this);
                            builder.setMessage(error);
                            builder.setTitle("Failed To Generate");
                            builder.setCancelable(true);
                            builder.setPositiveButton("Ok", (dialog, which) -> {
                                dialog.dismiss();
                            });
                            AlertDialog alertDialog1 = builder.create();
                            alertDialog1.show();
                        });
                    }
                });
            } else {
                Toast.makeText(this, "Location Service Is Not Enabled", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Location Access not granted by user", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchDigiPinOnly() {
        String latStr = edtLat.getText().toString().trim();
        String lngStr = edtLng.getText().toString().trim();
        if (latStr.isEmpty() || lngStr.isEmpty()) {
            Toast.makeText(this, "Please enter both Latitude & Longitude", Toast.LENGTH_SHORT).show();
            return;
        }
        double MIN_LAT = 2.5, MAX_LAT = 38.5, MIN_LON = 63.5, MAX_LON = 99.5;
        double lat, lng;
        try {
            lat = Double.parseDouble(latStr);
            lng = Double.parseDouble(lngStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid latitude or longitude format", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lat < MIN_LAT || lat > MAX_LAT) {
            Toast.makeText(this, "Latitude must be within (" + MIN_LAT + " to " + MAX_LAT + ")", Toast.LENGTH_SHORT).show();
            return;
        }
        if (lng < MIN_LON || lng > MAX_LON) {
            Toast.makeText(this, "Longitude must be within (" + MIN_LON + " to " + MAX_LON + ")", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoader(4, "Generating DigiPin");
        hideLoader(4, () -> {
            String myPin = DeviceInfo.generateDigiPin(lat, lng);
            linearLatLngDigiPin.setVisibility(View.VISIBLE);
            setField(txtLatLngDigiPin, btnCopyLatLngDigiPin, myPin);
            ScrollView scrollView = findViewById(R.id.bodyScrollView);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            edtLat.clearFocus();
            edtLng.clearFocus();
        });
    }

    private void setField(TextView textView, @Nullable ImageButton copyBtn, Object value) {
        if (value != null && !value.toString().isEmpty()) {
            textView.setText(value.toString());
            if (copyBtn != null && copyBtn instanceof ImageButton) {
                copyBtn.setVisibility(View.VISIBLE);
                copyBtn.setOnClickListener(v -> copyToClipboard(textView.getText().toString()));
            }
        } else {
            textView.setText("");
            if (copyBtn != null && copyBtn instanceof ImageButton) {
                copyBtn.setVisibility(View.GONE);
            }
        }
    }

    private void copyToClipboard(String value) {
        if (value.equals("---")) return;
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("Copied", value));
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
    }

    private void initLoader() {
        // ===== LOADER =====
        FrameLayout loaderOverlay1 = findViewById(R.id.loaderDigiPin);
        FrameLayout loaderOverlay2 = findViewById(R.id.loaderLocation);
        FrameLayout loaderOverlay3 = findViewById(R.id.loaderPostal);
        FrameLayout loaderOverlay4 = findViewById(R.id.loaderDigiPinCustom);

        loaderOverlay1.setAlpha(0f);
        loaderOverlay2.setAlpha(0f);
        loaderOverlay3.setAlpha(0f);
        loaderOverlay4.setAlpha(0f);
    }

    private void showLoader(int sectionNumber, String message) {
        FrameLayout loaderOverlay;
        View ripple1, ripple2, centerDot;
        TextView txtMessage;

        // Find views based on section
        switch (sectionNumber) {
            case 1:
                loaderOverlay = findViewById(R.id.loaderDigiPin);
                ripple1 = loaderOverlay.findViewById(R.id.ripple1);
                ripple2 = loaderOverlay.findViewById(R.id.ripple2);
                centerDot = loaderOverlay.findViewById(R.id.centerDot);
                txtMessage = loaderOverlay.findViewById(R.id.txtLoaderMessage);
                loaderStartTime1 = System.currentTimeMillis();
                break;
            case 2:
                loaderOverlay = findViewById(R.id.loaderLocation);
                ripple1 = loaderOverlay.findViewById(R.id.ripple1);
                ripple2 = loaderOverlay.findViewById(R.id.ripple2);
                centerDot = loaderOverlay.findViewById(R.id.centerDot);
                txtMessage = loaderOverlay.findViewById(R.id.txtLoaderMessage);
                loaderStartTime2 = System.currentTimeMillis();
                break;
            case 3:
                loaderOverlay = findViewById(R.id.loaderPostal);
                ripple1 = loaderOverlay.findViewById(R.id.ripple1);
                ripple2 = loaderOverlay.findViewById(R.id.ripple2);
                centerDot = loaderOverlay.findViewById(R.id.centerDot);
                txtMessage = loaderOverlay.findViewById(R.id.txtLoaderMessage);
                loaderStartTime3 = System.currentTimeMillis();
                break;
            case 4:
                loaderOverlay = findViewById(R.id.loaderDigiPinCustom);
                ripple1 = loaderOverlay.findViewById(R.id.ripple1);
                ripple2 = loaderOverlay.findViewById(R.id.ripple2);
                centerDot = loaderOverlay.findViewById(R.id.centerDot);
                txtMessage = loaderOverlay.findViewById(R.id.txtLoaderMessage);
                loaderStartTime4 = System.currentTimeMillis();
                break;
            default:
                return;
        }

        txtMessage.setText(message);
        loaderOverlay.setVisibility(View.VISIBLE);
        loaderOverlay.setAlpha(0f);
        loaderOverlay.animate().alpha(1f).setDuration(250).start();

        Animation rippleAnim1 = AnimationUtils.loadAnimation(this, R.anim.ripple_scale);
        ripple1.startAnimation(rippleAnim1);

        Animation rippleAnim2 = AnimationUtils.loadAnimation(this, R.anim.ripple_scale);
        rippleAnim2.setStartOffset(400);
        ripple2.startAnimation(rippleAnim2);

        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        centerDot.startAnimation(pulse);
    }

    private void hideLoader(int sectionNumber, @Nullable Runnable onHidden) {
        FrameLayout loaderOverlay;
        View ripple1, ripple2, centerDot;
        long loaderStartTime;

        // Find views and start time
        switch (sectionNumber) {
            case 1:
                loaderOverlay = findViewById(R.id.loaderDigiPin);
                ripple1 = loaderOverlay.findViewById(R.id.ripple1);
                ripple2 = loaderOverlay.findViewById(R.id.ripple2);
                centerDot = loaderOverlay.findViewById(R.id.centerDot);
                loaderStartTime = loaderStartTime1;
                break;
            case 2:
                loaderOverlay = findViewById(R.id.loaderLocation);
                ripple1 = loaderOverlay.findViewById(R.id.ripple1);
                ripple2 = loaderOverlay.findViewById(R.id.ripple2);
                centerDot = loaderOverlay.findViewById(R.id.centerDot);
                loaderStartTime = loaderStartTime2;
                break;
            case 3:
                loaderOverlay = findViewById(R.id.loaderPostal);
                ripple1 = loaderOverlay.findViewById(R.id.ripple1);
                ripple2 = loaderOverlay.findViewById(R.id.ripple2);
                centerDot = loaderOverlay.findViewById(R.id.centerDot);
                loaderStartTime = loaderStartTime3;
                break;
            case 4:
                loaderOverlay = findViewById(R.id.loaderDigiPinCustom);
                ripple1 = loaderOverlay.findViewById(R.id.ripple1);
                ripple2 = loaderOverlay.findViewById(R.id.ripple2);
                centerDot = loaderOverlay.findViewById(R.id.centerDot);
                loaderStartTime = loaderStartTime4;
                break;

            default:
                return;
        }

        long elapsed = System.currentTimeMillis() - loaderStartTime;
        long delay = Math.max(0, MIN_LOADER_TIME - elapsed);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ripple1.clearAnimation();
            ripple2.clearAnimation();
            centerDot.clearAnimation();

            loaderOverlay.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                loaderOverlay.setVisibility(View.GONE);
                loaderOverlay.setAlpha(1f);
                if (onHidden != null) onHidden.run();
            }).start();
        }, delay);
    }
}