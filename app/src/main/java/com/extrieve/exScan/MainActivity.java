package com.extrieve.exScan;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.print.PrintHelper;

import com.extrieve.quickcapture.sdk.CameraHelper;
import com.extrieve.quickcapture.sdk.Config;
import com.extrieve.quickcapture.sdk.ImgException;
import com.extrieve.quickcapture.sdk.ImgHelper;

import com.extrieve.splicer.aisdk.AiDocument;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE_PERMISSIONS = 1001;
    public final int REQUEST_CODE_FILE_RETURN = 1002;
    private final int RESULT_CODE_PICK_1IMAGE = 1003;
    private final int RESULT_CODE_PICK_IMAGE = 1004;
    private static final int PICK_IMAGE = 1;
    private static final int MASK_IMAGE = 2;
    private String IMAGE_ID_FOR_DELETE;
    private String IMAGE_URL_FOR_DELETE;
    private String IMAGE_URL_FOR_MASK;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private final String[] REQUIRED_PERMISSIONS_ABOVE13 = new String[]{"android.permission.CAMERA", "android.permission.READ_MEDIA_IMAGES"};
    private boolean permissionStatus = false;
    private int angle = 0, angleRight = 0, angleLeft = 0;
    ImageView imageViewLogo;
    ZoomPanImageView zoomPanImageView;
    String setCameraOption, setCaptureMode, setLayoutOption, setQualityOption, setResizeModeOption, setResizeModeValues, setDPIOption, setCrpFilterOption, setReviewOption, setColorOption, setacToggleOption, setSensitivityOption, setMaxPages;
    String setProfileOption, setCropOption, setFlashOption, setToggleOption, setSoundOption, setShowCountOption;
    ArrayList<String> FileCollection;
    ArrayList<String> FinalOutputCollection;
    LinearLayout startLinear, backLinear, multiSelectLinear, optionLinear, captureLinear, galleryLayout, ImageviewLinear;
    ScrollView recyclerLinear;
    Button showSingleBtn, showMoreBtn, resetImageBtn, deleteAllBtn, showBottomDialog, unselectAll;
    ImageView deleteSelected, showSetting;
    CheckBox selectAllImages;
    TextView selectedCount;
    Button shareBtn, maskBtn, printBtn, rotateLeftBtn, rotateRightBtn, infoBtn, textractBtn;
    private String resData = null;
    private String APIURL = "https://textract.extrieve.in/api/";
    ArrayList<Uri> uri = new ArrayList<>();
    List<ImageView> selectedImages;
    Boolean isSelectMode = false, alertInfoOnResponse=false;
    int currentPos = -1;
    ImageView currentImageView = null;
    Uri imgUri;
    Bitmap bitmap;
    String bmp = null;
    File folderPath;
    String type = null;
    int useCaseType = 0;
    boolean isPressesOnce = false;
    boolean enableDisableAddImagesToGallery; //this is for add to gallery enable disable
    boolean enableDisableOutputDir; //this is for selecting output directory folder
    private ActivityResultLauncher<Intent> attachedActivityResultLauncher, captureActivityResultLauncher, aadhaarActivityResultLauncher;
    LinearLayout getPictureButton, goToFaceMatchBtn, compressForGalleryBtn, createQRButton, getQRButton, buildOutPutBtn, doAadhaarMaskBtn, doKycDocAnalysis, doGeneralDocAnalysis, doShowUseCase;
    ImgHelper ImageHelper;
    CameraHelper CameraHelper;

    //DEV_HELP : License For Both QuickCapture & SplicerAi SDK
    String licStringAadhaar = "eJxazXCBkQENMEIxMvCa/+Xkp7BnDrxgHgtYng/KBgF+KJsJSHKA2cIMyfm5eqkVJUWZqWWpQEZwcmIeJ1SZS2puPg+YzckQXJCTmZxa5JjJDBYQABMCS+47g2gDIC0CFi9BNa8YoksvMbM4JRvdB+QDJjS+AG6/iCIpQAamniEuzEA/hgSFugIAAAD//wMApuksPQ==";
    String licenseStringQuickCapture = "eJxazXCBkQENMEIxMvDuLDr2KeyZAy+YxwKW54OyQYAfymYCkhxgtjBDcn6uXmpFSVFmalkqkBGcnJjHCVWWkpqbzwNm8zAElmYmZzsnFpSUFqUyg8UEwITAkvvOINoASIuAxUtQjSwEaUyGaNQrTslG9weZgAWNj9Vuz9zE9FT/gmJHTyrZih2AQwJ7UIoiKUAG+hG+bsxAT4QEhbpau7vA2QAAAAD//wMAihc59w==";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CheckAndPromptPermissions();
        SharedPreferences prefs = getSharedPreferences("LIC_SETTING", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("LIC_SPLICER_DATA", licStringAadhaar);
        editor.putString("LIC_QC_DATA", licenseStringQuickCapture);
        editor.apply();

        try {
            Config.License.Activate(this, licenseStringQuickCapture);
            ImageHelper = new ImgHelper(this);
            com.extrieve.splicer.aisdk.Config.License.Activate(this, licStringAadhaar);
        } catch (Exception e) {
            e.printStackTrace();
        }
        CameraHelper = new CameraHelper();

        selectedImages = new ArrayList<>();
        recyclerLinear = findViewById(R.id.recyclerLinear);
        galleryLayout = findViewById(R.id.galleryLayout);
        ImageviewLinear = findViewById(R.id.Imageview_frame);
        startLinear = findViewById(R.id.startLinear);
        backLinear = findViewById(R.id.backLinear);
        multiSelectLinear = findViewById(R.id.multiSelectLinear);
        optionLinear = findViewById(R.id.optionLinear);
        captureLinear = findViewById(R.id.captureLinear);
        //added for control linear
        getPictureButton = findViewById(R.id.getPictureButton);
        goToFaceMatchBtn = findViewById(R.id.goToFaceMatchBtn);
        compressForGalleryBtn = findViewById(R.id.loadFromGalleryBtn);
        createQRButton = findViewById(R.id.createQRButton);
        getQRButton = findViewById(R.id.getQRButton);
        showSetting = findViewById(R.id.showSetting);
        buildOutPutBtn = findViewById(R.id.buildOutPutBtn);
        doAadhaarMaskBtn = findViewById(R.id.doAadhaarMaskBtn);
        doKycDocAnalysis = findViewById(R.id.doKycDocAnalysis);
        doGeneralDocAnalysis = findViewById(R.id.doGeneralDocAnalysis);
        doShowUseCase = findViewById(R.id.showUseCase);
        //added for textView scroll automatically
        TextView filename = findViewById(R.id.filename);
        filename.setSelected(true);
        // Show status bar
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //capture & back linear btn here
        zoomPanImageView = findViewById(R.id.zoom_pan_image_view);
        imageViewLogo = findViewById(R.id.displayImageView);
        showSingleBtn = findViewById(R.id.showSingle);
        showMoreBtn = findViewById(R.id.showMore);
        resetImageBtn = findViewById(R.id.resetImage);
        //For Custom/Multi image selection
        unselectAll = findViewById(R.id.unselectAll);
        selectAllImages = findViewById(R.id.selectAllImages);
        deleteSelected = findViewById(R.id.deleteSelected);
        selectedCount = findViewById(R.id.selectedCount);
        //beginning of app open
        imageViewLogo.setVisibility(View.VISIBLE);
        zoomPanImageView.setVisibility(View.GONE);
        recyclerLinear.setVisibility(View.GONE);
        backLinear.setVisibility(View.GONE);
        selectedCount.setVisibility(View.GONE);
        deleteSelected.setVisibility(View.GONE);
        multiSelectLinear.setVisibility(View.GONE);
        startLinear.setVisibility(View.VISIBLE);
        //option & linear btn here
        shareBtn = findViewById(R.id.shareBtn);
        maskBtn = findViewById(R.id.maskBtn);
        printBtn = findViewById(R.id.printBtn);
        rotateLeftBtn = findViewById(R.id.rotateLeftBtn);
        rotateRightBtn = findViewById(R.id.rotateRightBtn);
        deleteAllBtn = findViewById(R.id.deleteBtn);
        infoBtn = findViewById(R.id.infoBtn);
        textractBtn = findViewById(R.id.textractBtn);
        optionLinearHideShowCaptureLinear();

        showBottomDialog = findViewById(R.id.showOptionDialog);
        showBottomDialog.setOnClickListener(v -> showBottomSheetDialog());

        //control linear click events
        getPictureButton.setOnClickListener(v -> AlertForCaptureImage());
        compressForGalleryBtn.setOnClickListener(v -> checkCaptureType("galleryOne", null, true));
        goToFaceMatchBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, FaceIdentifyAndMatcher.class);
            intent.putExtra("LIC_STRING", licenseStringQuickCapture);
            startActivity(intent);
        });
        createQRButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, QrBarGenerator.class);
            startActivity(intent);
        });
        getQRButton.setOnClickListener(v -> {
            if (!MainActivity.this.allPermissionsGranted()) {
                Toast.makeText(MainActivity.this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                OpenQRCaptureActivity();
            } catch (Exception ex) {
                Toast.makeText(this, "Exception Occurred -" + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        showSetting.setOnClickListener(v -> showPopupDialog());
        buildOutPutBtn.setOnClickListener(v -> {
            if (isSelectMode) {
                int selectedSize = selectedImages.size();
                if (selectedSize == 0) {
                    showToast("Please Select Image", Gravity.CENTER);
                    return;
                }
                AlertForBuild();
                imageViewLogo.setVisibility(View.GONE);
                zoomPanImageView.setVisibility(View.GONE);
                recyclerLinear.setVisibility(View.VISIBLE);
                startLinearShowHideBackLinear();
                optionLinearHideShowCaptureLinear();
            } else {
                int size = uri.size();
                if (size == 0) {
                    showToast("Please Capture Image", Gravity.CENTER);
                    return;
                }
                AlertForBuild();
            }
        });
        doAadhaarMaskBtn.setOnClickListener(v -> {
            type = "Mask";
            Intent gallery = new Intent();
            gallery.setType("image/*");
            gallery.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(gallery, "Select Image"), PICK_IMAGE);
        });
        doKycDocAnalysis.setOnClickListener(v -> {
            useCaseType = 0;
            type = "KycDocument";
            Intent gallery = new Intent();
            gallery.setType("image/*");
            gallery.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(gallery, "Select Image"), PICK_IMAGE);
        });
        doGeneralDocAnalysis.setOnClickListener(v -> {
            useCaseType = 0;
            type = "GeneralDocument";
            Intent gallery = new Intent();
            gallery.setType("image/*");
            gallery.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(gallery, "Select Image"), PICK_IMAGE);
        });
        doShowUseCase.setOnClickListener(v -> {
            showUseCasePopup();
        });
//        delete.setOnClickListener(v -> {
//            int uSize = uri.size();
//            if (uSize == 0) {
//                showToast("Please Capture Image", Gravity.CENTER);
//                return;
//            }
//            AlertDialog alertDialog = getAlertDialog();
//            alertDialog.show();
//        });

        SharedPreferences txtGet = getSharedPreferences("result", Context.MODE_PRIVATE);
        txtGet.edit().clear().apply();

        deleteSelected.setOnClickListener(v -> {
            //KARTHIK: delete selected: add in same method
            AlertForDelete(false);
        });
        selectAllImages.setOnClickListener(v -> {
            //show gridview & hide imageview & clear the selected items
            setAllSelected();
            imageViewLogo.setVisibility(View.GONE);
            zoomPanImageView.setVisibility(View.GONE);
            recyclerLinear.setVisibility(View.VISIBLE);
            //startLinearShowHideBackLinear();
            optionLinearHideShowCaptureLinear();
            updateCountToUI();
        });
        unselectAll.setOnClickListener(v -> {
            selectAllImages.setChecked(false);
            setAllSelected();
            isSelectMode = false;
            multiSelectLinear.setVisibility(View.GONE);
            startLinear.setVisibility(View.VISIBLE);
        });

        showSingleBtn.setOnClickListener(v -> {
            //show gridview & hide imageview
            imageViewLogo.setVisibility(View.GONE);
            zoomPanImageView.setVisibility(View.GONE);
            recyclerLinear.setVisibility(View.VISIBLE);
            startLinearShowHideBackLinear();
            optionLinearHideShowCaptureLinear();
        });
        showMoreBtn.setOnClickListener(v -> {
            //show gridview & hide imageview
            imageViewLogo.setVisibility(View.GONE);
            zoomPanImageView.setVisibility(View.GONE);
            recyclerLinear.setVisibility(View.VISIBLE);
            startLinearShowHideBackLinear();
            optionLinearHideShowCaptureLinear();
        });
        resetImageBtn.setOnClickListener(v -> {
            //reload the same image
            showSingleBtn.performClick();
            if (currentImageView != null && currentPos >= 0) {
                toggleImageSelection(currentImageView, currentPos);
            }
            zoomPanImageView.setRotation(0);
        });
        rotateLeftBtn.setOnClickListener(view -> {
            //rotateToLeft();
            rotateDummy("left");
        });
        rotateRightBtn.setOnClickListener(view -> {
            //rotateToRight();
            rotateDummy("right");
        });
        printBtn.setOnClickListener(v -> printCurrentImage());
        deleteAllBtn.setOnClickListener(v -> AlertForDelete(false));
        infoBtn.setOnClickListener(v -> AlertForInfo());

        maskBtn.setOnClickListener(view -> {
            //moving to masking activity in library
            String path = MainActivity.this.BuildStoragePath() + "/" + IMAGE_URL_FOR_DELETE;
            Intent MaskIntent = null;
            try {
                MaskIntent = new Intent(this, Class.forName("com.extrieve.splicer.aisdk.AadhaarMask"));
                MaskIntent.putExtra("BitmapImagePath", path);
                startActivity(MaskIntent);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
        shareBtn.setOnClickListener(v -> shareDummy());
        textractBtn.setOnClickListener(v -> {
            //showDialogAndProcessExtract();
        });

        //LOAD SETTINGS DATA
        SharedPreferences sh = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
        boolean userChanges = Boolean.parseBoolean(sh.getString("userChanges", "false"));
        if (!userChanges) {
            defaultInputForConfig();
        } else {
            //for setting up default camera configuration
            try {
                SetCameraConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //LOAD EXISTING IMAGES IF AVAILABLE
        try {
            if (!getIntent().getBooleanExtra("EXIT", false)) {
                setSelectedOnCreate(true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //for activity launcher
        captureActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> handleCaptureActivityResult(result));
        attachedActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> handleAttachedActivityResult(result));
        aadhaarActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> handleAadhaarMaskActivityResult(result));

        //for close app
        // API < 33 → OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleCustomBackPressed();
            }
        });
        // API 33+ → OnBackInvokedCallback (predictive back support)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    this::handleCustomBackPressed
            );
        }

        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
            ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo runningProInfo : procInfos) {
                if (runningProInfo.processName.equals("com.extrieve.exScan")) {
                    activityManager.killBackgroundProcesses(runningProInfo.processName);
                }
            }
        }
    }

    @NonNull
    private AlertDialog getAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Are you sure you want to delete all images?");
        builder.setTitle("Delete Warning!");
        builder.setCancelable(true);
        builder.setNegativeButton("Delete", (dialog, which) -> DeleteIfExit(true, true, "null"));
        builder.setPositiveButton("Cancel", (dialog, which) -> dialog.cancel());
        return builder.create();
    }

    private int getCount() {
        return uri.size();
    }

    private int selectedItemCount() {
        return selectedImages.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(selectedImages.size());
        for (int i = 0; i < selectedImages.size(); i++) {
            ImageView img = selectedImages.get(i);
            for (int j = 0; j < uri.size(); j++) {
                if (img.getId() == j) {
                    items.add(j);
                }
            }
        }
        return items;
    }

    private void addThumbnails() {
        galleryLayout.removeAllViews();
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int desiredColumnWidth = screenWidth / 3;
        int imagesPerRow = 3;
        int rowCount = (int) Math.ceil((double) uri.size() / imagesPerRow);

        for (int i = 0; i < rowCount; i++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            galleryLayout.addView(rowLayout);

            int startIndex = i * imagesPerRow;
            int endIndex = Math.min(startIndex + imagesPerRow, uri.size());

            for (int j = startIndex; j < endIndex; j++) {
                final int jl = j;
                ImageView imageView = new ImageView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(desiredColumnWidth - 10, desiredColumnWidth - 10);
                lp.setMargins(5, 5, 5, 5);
                imageView.setLayoutParams(lp);
                Uri imageUri = uri.get(jl);
                String imagePath = new File(Objects.requireNonNull(imageUri.getPath())).getAbsolutePath();

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imagePath, options);
                // Use safe target dimensions for thumbnails
                options.inSampleSize = calculateInSampleSize(options, 300, 300);
                options.inJustDecodeBounds = false;

                Bitmap thumbnailBitmap = BitmapFactory.decodeFile(imagePath, options);
                if (thumbnailBitmap != null) {
                    imageView.setImageBitmap(thumbnailBitmap);
                } else {
                    Log.e("BitmapError", "Failed to decode thumbnail bitmap");
                }

                imageView.setId(jl);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setOnClickListener(v -> {
                    currentImageView = imageView;
                    toggleImageSelection(imageView, jl);
                });
                imageView.setOnLongClickListener(v -> {
                    imageViewLogo.setVisibility(View.GONE);
                    zoomPanImageView.setVisibility(View.GONE);
                    recyclerLinear.setVisibility(View.VISIBLE);
                    startLinearShowHideBackLinear();
                    optionLinearHideShowCaptureLinear();
                    isSelectMode = true;
                    updateCountToUI();
                    multiSelectLinear.setVisibility(View.VISIBLE);
                    startLinear.setVisibility(View.GONE);
                    toggleImageSelection(imageView, jl);
                    return true;
                });
                rowLayout.addView(imageView);
            }
        }
    }

    private void toggleImageSelection(ImageView imageView, int pos) {
        currentPos = pos;
        if (isSelectMode) {
            updateCountToUI();
            if (selectedImages.contains(imageView)) {
                // Image is already selected, so deselect it
                selectedImages.remove(imageView);
                imageView.setBackgroundColor(Color.TRANSPARENT);
                imageView.setPadding(0, 0, 0, 0);
                imageView.setImageAlpha(255);
            } else {
                // Image is not selected, so select it
                selectedImages.add(imageView);
                imageView.setBackgroundColor(Color.parseColor("#FF2196F3"));
                imageView.setPadding(5, 5, 5, 5);
                imageView.setImageAlpha(155);
            }
            updateCountToUI();
            if (selectedImages.isEmpty()) {
                selectAllImages.setChecked(false);
                selectedCount.setVisibility(View.VISIBLE);
                deleteSelected.setVisibility(View.GONE);
                selectedCount.setText("Select Images");
            }
        } else {
            if (selectedCount.getVisibility() == View.VISIBLE) {
                selectedCount.setVisibility(View.GONE);
                deleteSelected.setVisibility(View.GONE);
            }
            zoomPanImageView = new ZoomPanImageView(this);
            ZoomPanImageView oldZoomPanImageView = findViewById(R.id.zoom_pan_image_view); // Replace with actual ID
            zoomPanImageView.setScaleType(oldZoomPanImageView.getScaleType());
            zoomPanImageView.setLayoutParams(oldZoomPanImageView.getLayoutParams());

            int oldViewId = oldZoomPanImageView.getId();
            ImageviewLinear.removeView(oldZoomPanImageView);
            zoomPanImageView.setId(oldViewId);
            ImageviewLinear.addView(zoomPanImageView);

            String imageURL = String.valueOf(uri.get(pos));
            Uri imageUri = uri.get(pos);
            String imagePath = new File(Objects.requireNonNull(imageUri.getPath())).getAbsolutePath();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);
            // Use safe target dimensions for thumbnails
            options.inSampleSize = calculateInSampleSize(options, 300, 300);
            options.inJustDecodeBounds = false;

            Bitmap thumbnailBitmap = BitmapFactory.decodeFile(imagePath, options);
            File file = new File(imageURL);
            zoomPanImageView.setImageBitmap(thumbnailBitmap);
            //set filename here
            String fileName = file.getName();
            TextView setFileName = findViewById(R.id.filename);
            setFileName.setText(fileName);
            //get position to delete exact image
            IMAGE_ID_FOR_DELETE = String.valueOf(pos);
            IMAGE_URL_FOR_DELETE = fileName;
            IMAGE_URL_FOR_MASK = imageURL;
            //ui control functions
            imageViewLogo.setVisibility(View.GONE);
            zoomPanImageView.setVisibility(View.VISIBLE);
            recyclerLinear.setVisibility(View.GONE);
            startLinearHideShowBackLinear();
            optionLinearShowHideCaptureLinear();
            zoomPanImageView.setRotation(0);
        }

        Log.d("Selected images: ", String.valueOf(selectedImages.size()));
    }

    private void OpenQRCaptureActivity() {
        try {
            Intent CameraIntent = new Intent(this, Class.forName("com.extrieve.quickcapture.sdk.OpticalCodeHelper"));
            Uri photoURI = Uri.parse(Config.CaptureSupport.OutputPath);
            this.grantUriPermission(this.getPackageName(), photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                CameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            captureActivityResultLauncher.launch(CameraIntent);

        } catch (ClassNotFoundException e) {
            Toast.makeText(this, "Failed to open scanner", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    //KARTHIK: double press back implementation
    private void handleCustomBackPressed() {
        if (isPressesOnce) {
            // 🔹 clear only uri list, not files in app storage path
            clear();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("EXIT", true);
            startActivity(intent);
            finishAffinity();

        } else {
            // 🔹 tell user to press once more to exit
            isPressesOnce = true;

            int uric = getCount();

            if (uric == 0) {
                imageViewLogo.setVisibility(View.VISIBLE);
                zoomPanImageView.setVisibility(View.GONE);
                recyclerLinear.setVisibility(View.GONE);
                startLinearShowHideBackLinear();
                optionLinearHideShowCaptureLinear();
                multiSelectLinear.setVisibility(View.GONE);
                imageViewLogo.setImageResource(R.drawable.ic_final_logo1);
            }

            // 🔹 show gridview if uri has images
            if (uric > 0) showMoreBtn.performClick();

            // 🔹 clear multi-select state
            if (multiSelectLinear.getVisibility() == View.VISIBLE) {
                selectAllImages.setChecked(false);
                setAllSelected();
                isSelectMode = false;
                multiSelectLinear.setVisibility(View.GONE);
                startLinear.setVisibility(View.VISIBLE);
            }

            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper())
                    .postDelayed(() -> isPressesOnce = false, 2000);
        }
    }

    private void printCurrentImage() {
        String fileName = IMAGE_URL_FOR_DELETE;
        if (Objects.equals(fileName, "")) return;
        String path = MainActivity.this.BuildStoragePath();
        path = path + "/" + fileName;
        File printCur = new File(path);
        Bitmap printBitmap = BitmapFactory.decodeFile(printCur.getAbsolutePath());

        PrintHelper photoPrinter = new PrintHelper(getActivity());
        photoPrinter.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        photoPrinter.printBitmap(fileName, printBitmap);
    }

    private void rotateDummy(String type) {
        angle = (int) zoomPanImageView.getRotation();
        if (angle == 0) {
            angleRight = 90;
            angleLeft = 270;
        } else if (angle == 90) {
            angleRight = 180;
            angleLeft = 0;
        } else if (angle == 180) {
            angleRight = 270;
            angleLeft = 90;
        } else if (angle == 270) {
            angleRight = 0;
            angleLeft = 180;
        }

        if (Objects.equals(type, "right")) {
            zoomPanImageView.setRotation(angleRight);
        } else if (Objects.equals(type, "left")) {
            zoomPanImageView.setRotation(angleLeft);
        }
    }

    private void shareDummy() {
        //new updated working share
        BitmapDrawable bitmapDrawable = (BitmapDrawable) zoomPanImageView.getDrawable();
        Bitmap bitmap = bitmapDrawable.getBitmap();
        Uri uri2 = getImageToShare(bitmap);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, uri2);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("image/*");
        startActivity(Intent.createChooser(intent, "Share Via"));
    }

    private Uri getImageToShare(Bitmap bitmap) {
        File folder = new File(getCacheDir(), "images");
        Uri uri2 = null;
        try {
            folder.mkdirs();
            File file = new File(folder, "share_image.jpg");
            FileOutputStream fileOutputStream = null;
            fileOutputStream = new FileOutputStream(file);
            ImageHelper.CompressToJPEG(bitmap, String.valueOf(file));
            fileOutputStream.flush();
            fileOutputStream.close();
            uri2 = FileProvider.getUriForFile(this, "com.extrieve.exScan.FileProvider", file);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to share", Toast.LENGTH_SHORT).show();
        }
        return uri2;
    }

    private void multiSelectLinearShowHideOtherLinear() {
        backLinear.setVisibility(View.GONE);
        startLinear.setVisibility(View.VISIBLE);
        multiSelectLinear.setVisibility(View.GONE);
    }

    private void startLinearShowHideBackLinear() {
        backLinear.setVisibility(View.GONE);
        multiSelectLinear.setVisibility(View.GONE);
        startLinear.setVisibility(View.VISIBLE);
    }

    private void startLinearHideShowBackLinear() {
        multiSelectLinear.setVisibility(View.GONE);
        startLinear.setVisibility(View.GONE);
        backLinear.setVisibility(View.VISIBLE);
    }

    private void optionLinearShowHideCaptureLinear() {
        captureLinear.setVisibility(View.GONE);
        optionLinear.setVisibility(View.VISIBLE);
    }

    private void optionLinearHideShowCaptureLinear() {
        optionLinear.setVisibility(View.GONE);
        captureLinear.setVisibility(View.VISIBLE);
    }

    private void AlertForDelete(boolean exitAlertForDelete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Are you sure you want to delete?");
        builder.setTitle("Delete Warning!");
        builder.setCancelable(false);
        if (isSelectMode) {
            builder.setNegativeButton("Delete", (dialog, which) -> DeleteIfExit(false, false, "selected"));
        } else if (exitAlertForDelete) {
            builder.setNegativeButton("Delete & Exit", (dialog, which) -> DeleteIfExit(true, false, "null"));
        } else {
            builder.setNegativeButton("Delete", (dialog, which) -> DeleteIfExit(true, true, "current"));
        }
        builder.setPositiveButton("Cancel", (dialog, which) -> dialog.cancel());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void AlertForInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Image Info");
        String date = Date();
        String time = Time();
        String imagePath = MainActivity.this.BuildStoragePath() + "/" + IMAGE_URL_FOR_DELETE;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        int width = options.outWidth;
        int height = options.outHeight;
        if (width == 0 || height == 0) {
            width = zoomPanImageView.getDrawable().getIntrinsicWidth();
            height = zoomPanImageView.getDrawable().getIntrinsicHeight();
        }
        String fileSize = getFileSize(imagePath, "kb");
        builder.setMessage("Width: " + width + "\n" + "Height: " + height + "\n" + "File Size: " + fileSize + "\n" + "Captured Date: " + date + "\n" + "Captured Time: " + time);
        builder.setCancelable(false);
        builder.setNegativeButton("Close", (dialog, which) -> {
            dialog.cancel();
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private String Date() {
        String[] separated = IMAGE_URL_FOR_DELETE.split("_");
        String separatedDate = separated[1].trim();
        String year = separatedDate.substring(0, 4);
        String month = separatedDate.substring(4, 6);
        String date = separatedDate.substring(6, 8);
        return date + "-" + month + "-" + year;
    }

    private String Time() {
        String[] separated = IMAGE_URL_FOR_DELETE.split("_");
        String separatedDate = separated[2].trim();
        String hour = separatedDate.substring(0, 2);
        String min = separatedDate.substring(2, 4);
        String sec = separatedDate.substring(4, 6);
        return hour + ":" + min + ":" + sec;
    }

    private void checkCaptureType(String captureType, String buildType, Boolean alertOnResponse) {
        SharedPreferences sh = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
        enableDisableAddImagesToGallery = Boolean.parseBoolean(sh.getString("enableDisableAddToGallery", "false"));
        alertInfoOnResponse = alertOnResponse;
        if (Objects.equals(captureType, "camera")) {
            clearImageSelection();
            if (!MainActivity.this.allPermissionsGranted()) {
                Toast.makeText(MainActivity.this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                //redirecting to camera
                OpenCameraActivity();
            } catch (Exception ex) {
                // Error occurred while creating the File
                //Log.e(TAG, "photoFile  creation failed", exception);
                Log.d("KARTHIK", "Exception Occurred -" + ex.getMessage());
            }
        } else if (Objects.equals(captureType, "galleryOne")) {
            clearImageSelection();
            Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(gallery, RESULT_CODE_PICK_1IMAGE);
        } else if (Objects.equals(captureType, "galleryMulti")) {
            clearImageSelection();
            Intent gallery = new Intent();
            gallery.setType("image/*");
            gallery.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            gallery.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(gallery, "Select Images"), RESULT_CODE_PICK_IMAGE);
        } else if (Objects.equals(captureType, "outputBuild")) {
            addOutputToFinalCollection();
            Config.CaptureSupport.OutputPath = BuildFileStoragePath();
            buildTiffFile(buildType, null, null);
        }
    }

    private void AlertForBuild() {
        checkCaptureType("outputBuild", "tiff", false);
        final int[] checkOutsideClick = {0};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Select the type of output build");
        builder.setTitle("Build Output File");
        builder.setCancelable(true);
        builder.setPositiveButton("Both PDF & TIFF", (dialog, which) -> {
            checkOutsideClick[0] = 1;
            checkCaptureType("outputBuild", "both", false);
        });
        builder.setNeutralButton("PDF only", (dialog, which) -> {
            checkOutsideClick[0] = 1;
            checkCaptureType("outputBuild", "pdf", false);
        });
        builder.setNegativeButton("TIFF only", (dialog, which) -> {
            checkOutsideClick[0] = 1;
            checkCaptureType("outputBuild", "tiff", false);
        });
        builder.setOnDismissListener(dialog -> {
            if (checkOutsideClick[0] == 0) {
                updateCountToUI();
                multiSelectLinearShowHideOtherLinear();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void AlertForLoadImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Select image selection type");
        builder.setTitle("Load Images");
        builder.setCancelable(true);
        builder.setPositiveButton("Single Image", (dialog, which) -> checkCaptureType("galleryOne", null, false));
        builder.setNegativeButton("Multiple Images", (dialog, which) -> checkCaptureType("galleryMulti", null, false));
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void UseCaseDemo(int useType) {
        useCaseType = useType;
        switch (useType) {
            case 1:
                    try {
                        SetCameraConfig();
                        Config.CaptureSupport.DocumentCropping = Config.CaptureSupport.CroppingType.Disabled;
                        Config.CaptureSupport.CropFilter = Config.CaptureSupport.CropImageFilterType.NONE;
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage("Select asset capture type");
                        builder.setTitle("Asset Capture");
                        builder.setCancelable(true);
                        builder.setPositiveButton("Attach", (dialog, which) -> checkCaptureType("galleryMulti", null, false));
                        builder.setNegativeButton("Scan", (dialog, which) -> checkCaptureType("camera", null, false));
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                break;

            case 2:
                try {
                    SetCameraConfig();
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Select document capture type");
                    builder.setTitle("Document Capture");
                    builder.setCancelable(true);
                    builder.setPositiveButton("Attach", (dialog, which) -> checkCaptureType("galleryOne", null, false));
                    builder.setNegativeButton("Scan", (dialog, which) -> checkCaptureType("camera", null, false));
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case 3:
                try {
                    type = "Mask";
                    SetCameraConfig();
                    Config.CaptureSupport.MaxPage = 1;
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Select document capture type");
                    builder.setTitle("Aadhaar Masking");
                    builder.setCancelable(true);
                    builder.setPositiveButton("Attach", (dialog, which) -> {
                        Intent gallery = new Intent();
                        gallery.setType("image/*");
                        gallery.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(gallery, "Select Image"), PICK_IMAGE);
                    });
                    builder.setNegativeButton("Scan", (dialog, which) -> checkCaptureType("camera", null, false));
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case 4:
                try {
                    type = "KycDocumentUseCase";
                    SetCameraConfig();
                    Config.CaptureSupport.MaxPage = 1;
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Select document capture type");
                    builder.setTitle("KYC Extraction");
                    builder.setCancelable(true);
                    builder.setPositiveButton("Attach", (dialog, which) -> {
                        Intent gallery1 = new Intent();
                        gallery1.setType("image/*");
                        gallery1.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(gallery1, "Select Image"), PICK_IMAGE);
                    });
                    builder.setNegativeButton("Scan", (dialog, which) -> checkCaptureType("camera", null, false));
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            default:
                break;
        }
    }

    private void AlertForCaptureImage() {
        try {
            SetCameraConfig();
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Select document capture type");
            builder.setTitle("Capture Document");
            builder.setCancelable(true);
            builder.setPositiveButton("Attach", (dialog, which) -> checkCaptureType("galleryOne", null, false));
            builder.setNegativeButton("Scan", (dialog, which) -> checkCaptureType("camera", null, false));
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String deleteAllOnce() {
        String path = MainActivity.this.BuildStoragePath();
        path = path + "/";
        File deleteCur = new File(path);
        if (deleteCur.isDirectory()) {
            deleteFiles(path);
            return "0";
        }
        return "1";
    }

    private void DeleteIfExit(boolean performClick, boolean newTask, String deleteType) {
        String ret = null;
        if (deleteType.equals("current")) {
            ret = deleteCurrent();
        } else if (deleteType.equals("selected")) {
            boolean rt = deleteSelected();
            if (rt) ret = "1";
        } else {
            ret = deleteAllOnce();
        }
        if (performClick) {
            showMoreBtn.performClick();
        }
        //clear all selection if other options clicked
        clearImageSelection();

        if (Integer.parseInt(ret) == 0) {
            String path = MainActivity.this.BuildStoragePath();
            File deleteAll = new File(path);
            deleteAll(deleteAll);
            Intent intent = new Intent(this, MainActivity.class);
            if (performClick && newTask) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            } else {
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("EXIT", true);
            }
            startActivity(intent);
            finishAffinity();
        }
        if (Integer.parseInt(ret) == 1) {
            selectAllImages.setChecked(false);
            setAllSelected();
            isSelectMode = false;
            try {
                setSelectedOnCreate(false);
                Log.d("KARTHIK", "Deleted Selected Images");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setSelectedOnCreate(Boolean show) throws IOException {
        clear();
        String path = MainActivity.this.BuildStoragePath();
        FileCollection = new ArrayList<>();
        File directory = new File(path);
        if (!directory.isDirectory()) {
            return;
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String filePath = path + "/" + file.getName();
                FileCollection.add(filePath);
            }
        }
        if (FileCollection.isEmpty()) {
            isSelectMode = false;
            imageViewLogo.setVisibility(View.VISIBLE);
            zoomPanImageView.setVisibility(View.GONE);
            recyclerLinear.setVisibility(View.GONE);
            startLinearShowHideBackLinear();
            optionLinearHideShowCaptureLinear();
            multiSelectLinear.setVisibility(View.GONE);
            imageViewLogo.setImageResource(R.drawable.ic_final_logo1);
        } else {
            showImages(FileCollection);
            showMoreBtn.performClick();
            if (show) {
                Log.d("KARTHIK", "Loaded previous images: successfully");
            }
        }
    }

    private Boolean deleteSelected() {
        ArrayList<String> DeleteOutputCollection = new ArrayList<>();
        String path = MainActivity.this.BuildStoragePath();
        File directory = new File(path);
        if (!directory.isDirectory()) {
            return false;
        }
        List<Integer> selectedItems = getSelectedItems();
        int selectedSize = selectedItems.size();
        if (selectedSize > 0) {
            for (int i = 0; i < selectedSize; i++) {
                int p = i;
                i = selectedItems.get(i);
                if (!uri.isEmpty()) DeleteOutputCollection.add(String.valueOf(uri.get(i)));
                i = p;
            }
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (int i = files.length - 1; i > -1; i--) {
                String filePath = path + "/" + files[i].getName();
                File fileName = new File(filePath);
                if (!fileName.exists()) return false;
                for (int j = DeleteOutputCollection.size() - 1; j > -1; j--) {
                    if (filePath.equals(DeleteOutputCollection.get(j))) {
                        fileName.delete();
                        clearSpecific(i);
                    }
                }
            }
        }
        return true;
    }

    private boolean deleteAll(File file) {
        if (file.isDirectory() && file.exists()) {
            String[] children = file.list();
            for (String child : children) {
                boolean success = deleteAll(new File(file, child));
                if (!success) {
                    return false;
                }
            }
        }
        clear();
        Log.d("KARTHIK", "Deleted all: success");
        imageViewLogo.setImageResource(R.drawable.ic_final_logo1);
        startLinearShowHideBackLinear();
        optionLinearHideShowCaptureLinear();
        return false;
    }

    private String deleteCurrent() {
        String getPos = IMAGE_ID_FOR_DELETE;
        String fileName = IMAGE_URL_FOR_DELETE;
        String path = MainActivity.this.BuildStoragePath();
        path = path + "/" + fileName;
        File deleteCur = new File(path);
        if (deleteCur.exists()) {
            deleteCur.delete();
            Log.d("KARTHIK", "Deleted Specific: success");
            clearSpecific(Integer.parseInt(getPos));
            int checkUriFileCount = uri.size();
            if (checkUriFileCount == 0) {
                return "0";
            } else {
                return "1";
            }
        }
        return "0";
    }

    public void clear() {
        int size = uri.size();
        if (size > 0) {
            uri.subList(0, size).clear();
        }
    }

    public void clearSpecific(int getPos) {
        if (getPos < 0) return;
        int size = uri.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                if (i == getPos) uri.remove(getPos);
            }
        }
    }

    private void clearImageSelection() {
        if (isSelectMode) {
            clearSelection();
            isSelectMode = false;
            selectAllImages.setChecked(false);
            selectedCount.setVisibility(View.GONE);
            deleteSelected.setVisibility(View.GONE);
        }
    }

    private void clearSelection() {
        for (int i = 0; i < selectedImages.size(); i++) {
            ImageView imageViewer = selectedImages.get(i);
            imageViewer.setImageAlpha(255);
            imageViewer.setPadding(0, 0, 0, 0);
            imageViewer.setBackgroundColor(Color.TRANSPARENT);
        }
        selectedImages.clear();
    }

    public void setAllSelected() {
        if (selectAllImages.isChecked()) {
            isSelectMode = true;
            selectedImages.clear();
            //mark new selected
            int childCount = galleryLayout.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View childView = galleryLayout.getChildAt(i);
                if (childView instanceof LinearLayout) {
                    for (int j = 0; j < ((LinearLayout) childView).getChildCount(); j++) {
                        View childView1 = ((LinearLayout) childView).getChildAt(j);
                        if (childView1 instanceof ImageView) {
                            ImageView imageView = (ImageView) childView1;
                            imageView.setBackgroundColor(Color.parseColor("#FF2196F3"));
                            imageView.setPadding(5, 5, 5, 5);
                            imageView.setImageAlpha(155);
                            selectedImages.add(imageView);
                        }
                    }
                }
            }
            int selLen = selectedImages.size();
            selectAllImages.setChecked(selLen > 0);
            selectedCount.setVisibility(View.VISIBLE);
            deleteSelected.setVisibility(View.VISIBLE);
        } else {
            clearSelection();
            selectAllImages.setChecked(false);
            selectedCount.setVisibility(View.VISIBLE);
            deleteSelected.setVisibility(View.GONE);
        }
    }

    public void addOutputToFinalCollection() {
        FinalOutputCollection = new ArrayList<>();
        //checking custom select mode is on or off
        if (isSelectMode) {
            List<Integer> selectedItems = getSelectedItems();
            int selectedSize = selectedItems.size();
            if (selectedSize > 0) {
                for (int i = 0; i < selectedSize; i++) {
                    int p = i;
                    i = selectedItems.get(i);
                    FinalOutputCollection.add(String.valueOf(uri.get(i)));
                    i = p;
                }
            }
            clearSelection();
            isSelectMode = false;
            selectAllImages.setChecked(false);
            selectedCount.setVisibility(View.GONE);
            deleteSelected.setVisibility(View.GONE);
        } else {
            int size = uri.size();
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    FinalOutputCollection.add(String.valueOf(uri.get(i)));
                }
            }
        }
    }

    //get thumbnails of particular ratio
    public void getThumbnail() {
        String fileName = IMAGE_URL_FOR_DELETE;
        if (Objects.equals(fileName, "")) return;
        String path = MainActivity.this.BuildStoragePath();
        path = path + "/" + fileName;
        File deleteCur = new File(path);
        try {
            Bitmap myBitmap = BitmapFactory.decodeFile(deleteCur.getAbsolutePath());
            Bitmap thumb = ImageHelper.GetThumbnail(myBitmap, 300, 300, true);
            ImageView myImage = findViewById(R.id.zoom_pan_image_view);
            myImage.setImageBitmap(thumb);
        } catch (ImgException e) {
            e.printStackTrace();
            Log.d("KARTHIK", String.valueOf(e));
        }
    }

    public void rotateToLeft() {
        String fileName = IMAGE_URL_FOR_DELETE;
        if (Objects.equals(fileName, "")) return;
        String path = MainActivity.this.BuildStoragePath();
        path = path + "/" + fileName;
        File deleteCur = new File(path);
        try {
            Bitmap myBitmap = BitmapFactory.decodeFile(deleteCur.getAbsolutePath());
            ImgHelper.rotateBitmapDegree(bitmap, -90);
            ImageView myImage = findViewById(R.id.zoom_pan_image_view);
            myImage.setImageBitmap(myBitmap);
        } catch (ImgException e) {
            e.printStackTrace();
            Log.d("AMAL", String.valueOf(e));
        }
    }

    public void rotateToRight() {
        String fileName = IMAGE_URL_FOR_DELETE;
        if (Objects.equals(fileName, "")) return;
        String path = MainActivity.this.BuildStoragePath();
        path = path + "/" + fileName;
        File deleteCur = new File(path);
        try {
            Bitmap myBitmap = BitmapFactory.decodeFile(deleteCur.getAbsolutePath());
            ImgHelper.rotateBitmapDegree(myBitmap, 90);
            ImageView myImage = findViewById(R.id.zoom_pan_image_view);
            myImage.setImageBitmap(myBitmap);
        } catch (ImgException e) {
            e.printStackTrace();
            Log.d("AMAL", String.valueOf(e));
        }
    }

    private String buildSDDir() {
        File dir = new File("sdcard/QUICK_CAPTURE");
        try {
            if (dir.mkdir()) {
                return dir.toString();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void handleAttachedActivityResult(ActivityResult result) {
        //for single image append from gallery
        int resultCode = result.getResultCode();
        if (resultCode == Activity.RESULT_OK) {
            Intent data = result.getData();
            Boolean Status = null;
            if (data != null) {
                Status = (Boolean) data.getExtras().get("STATUS");
            }
            String Description = (String) data.getExtras().get("DESCRIPTION");
            if (Boolean.FALSE.equals(Status)) {
                String imageCaptureLog = "Description : " + Description + ".Exception: " + Config.CaptureSupport.LastLogInfo;
                Log.d("INFO", imageCaptureLog);
                finishActivity(RESULT_CODE_PICK_1IMAGE);
                return;
            }
            FileCollection = (ArrayList<String>) data.getExtras().get("fileCollection");
            if (FileCollection == null || FileCollection.isEmpty()) {
                //clear all selection if other options clicked
                clearImageSelection();
                return;
            }

            if(alertInfoOnResponse){
                SharedPreferences sh = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
                String pgl = sh.getString("getLayoutOption", "A4");
                String imd = sh.getString("getDPIOption", "200");

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Document Compressed & Optimised successfully");
                String fileSize = getFileSize(FileCollection.get(0), "kb");
                builder.setMessage("Optimised DPI : " + imd + "\n" + "Compressed Size : " + fileSize.toUpperCase() + "\n" + "Optimized Layout : " + pgl);
                builder.setCancelable(false);
                builder.setNegativeButton("Ok", (dialog, which) -> {
                    dialog.cancel();
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                alertInfoOnResponse = false;
            }

            try {
                showImages(FileCollection);
                startLinearShowHideBackLinear();
                optionLinearHideShowCaptureLinear();
                imageViewLogo.setVisibility(View.GONE);
                zoomPanImageView.setVisibility(View.GONE);
                recyclerLinear.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //finishActivity(RESULT_CODE_PICK_1IMAGE);
        } else {
            Log.d("KARTHIK", "Unhandled result code");
        }
    }

    private void handleCaptureActivityResult(ActivityResult result) {
        {
            int resultCode = result.getResultCode();
            if (resultCode == Activity.RESULT_OK) {
                Intent data = result.getData();
                Boolean Status = null;
                if (data != null) {
                    Status = (Boolean) data.getExtras().get("STATUS");
                }
                String Description = (String) data.getExtras().get("DESCRIPTION");
                if (Boolean.FALSE.equals(Status)) {
                    String imageCaptureLog = "Description : " + Description + ".Exception: " + Config.CaptureSupport.LastLogInfo;
                    Log.d("INFO", imageCaptureLog);
                    Toast.makeText(this, imageCaptureLog, Toast.LENGTH_LONG).show();
                    finishActivity(REQUEST_CODE_FILE_RETURN);
                    return;
                }
//                if (Config.CaptureSupport.DocumentCropping == Config.CaptureSupport.CroppingType.QrBarCode) {
//                    String qrData = (String) data.getExtras().get("DATA");
//                    String qrType = (String) data.getExtras().get("TYPE");
//                    Toast.makeText(this, "QR_BAR_Code Data: " + qrData, Toast.LENGTH_SHORT).show();
//                    finishActivity(REQUEST_CODE_FILE_RETURN);
//                    return;
//                }
                FileCollection = (ArrayList<String>) data.getExtras().get("fileCollection");
                if (FileCollection == null || FileCollection.isEmpty()) return;

                try {
                    showImages(FileCollection);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                finishActivity(REQUEST_CODE_FILE_RETURN);
            } else {
                finishActivity(REQUEST_CODE_FILE_RETURN);
            }
        }
    }

    private void handleAadhaarMaskActivityResult(ActivityResult result) {
        onActivityResult(MASK_IMAGE, result.getResultCode(), result.getData());
        Log.d("DoMask", "MaskProcessData: " + result.getData().getExtras().get("DATA"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        bitmap = null;
        // for masked image response activity
        if (requestCode == MASK_IMAGE && resultCode == RESULT_OK && requestCode != RESULT_CANCELED) {
            try {
                String maskResponse = Objects.requireNonNull(data.getExtras()).getString("DATA");
                if (maskResponse == null) {
                    finishActivity(MASK_IMAGE);
                    return;
                }
                JSONObject respDtObj = new JSONObject(maskResponse);
                boolean STATUS = respDtObj.getBoolean("STATUS");
                String DESCRIPTION = respDtObj.getString("DESCRIPTION");
                FileCollection = new ArrayList<>();
                if (STATUS) {
                    String maskedImgPath = respDtObj.getString("MASK_PATH");
                    FileCollection.add(maskedImgPath);
                    try {
                        showImages(FileCollection);
                        startLinearShowHideBackLinear();
                        optionLinearHideShowCaptureLinear();
                        imageViewLogo.setVisibility(View.GONE);
                        zoomPanImageView.setVisibility(View.GONE);
                        recyclerLinear.setVisibility(View.VISIBLE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, DESCRIPTION, Toast.LENGTH_LONG).show();
                }
                finishActivity(MASK_IMAGE);
            } catch (JSONException e) {
                Log.d("Error", "Runtime error in onActivityResult: "+ e);
            }
        }
        //for manual image append from file system
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && requestCode != RESULT_CANCELED) {
            //for pick image and handover to respected activity
            imgUri = Objects.requireNonNull(data).getData();
            File photoFile = null;
            String outPath = "";
            File imgFile = null;

            try {
                if (type.equals("KycDocument") || type.equals("KycDocumentUseCase")) {
                    imgFile = new File(BuildSplicerStoragePath());
                } else {
                    imgFile = new File(BuildStoragePath());
                }
                photoFile = createImageFile(imgFile);
                outPath = photoFile.getAbsolutePath();
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imgUri);
            } catch (IOException ex) {
                Toast.makeText(this, "photoFile  creation failed", Toast.LENGTH_LONG).show();
                return;
            }
            if (!outPath.isEmpty()) {
                if (bitmap == null) {
                    Toast.makeText(this, "Failed to open the photo", Toast.LENGTH_LONG).show();
                    return;
                }
                bmp = saveImage(bitmap);
                Intent processIntent = null;
                if (type == null) {
                    Toast.makeText(this, "Please try again", Toast.LENGTH_SHORT).show();
                    return;
                }
                switch (type) {
                    case "Mask":
                        try {
                            AiDocument aadhaarDoc1 = new AiDocument(this, bmp);
                            Log.d(TAG, "Document initialised successfully");
                            Log.d(TAG, "ID: " + aadhaarDoc1.ID);
                            Log.d(TAG, "TYPE: " + aadhaarDoc1.TYPE);
                            Log.d(TAG, "FILE_PATH: " + aadhaarDoc1.FILE_PATH);
                            processIntent = new Intent(this, Class.forName("com.extrieve.splicer.aisdk.AadhaarMask"));
                            processIntent.putExtra("DOCUMENT_ID", aadhaarDoc1.ID);
                            com.extrieve.splicer.aisdk.Config.AadhaarMasking.EXTRACT_AADHAAR_NUMBER = true;
                            aadhaarActivityResultLauncher.launch(processIntent);
                        } catch (Exception e) {
                            Log.d("Error", "Runtime error in AadhaarMask: "+ e);
                        }
                        break;
                    case "KycDocument":
                        processIntent = new Intent(this, KYCDocAnalysis.class);
                        if (bmp != null) processIntent.putExtra("KYCDocImagePath", bmp);
                        startActivity(processIntent);
                        break;
                    case "KycDocumentUseCase":
                        processIntent = new Intent(this, KYCDocUseCase.class);
                        if (bmp != null) processIntent.putExtra("KYCDocImagePath", bmp);
                        startActivity(processIntent);
                        break;
                    case "GeneralDocument":
                        processIntent = new Intent(this, GeneralDocAnalysis.class);
                        if (bmp != null) processIntent.putExtra("GeneralDocImagePath", bmp);
                        startActivity(processIntent);
                        break;
                }
                //end added by karthik
                Log.d("KARTHIK", "Image Received");
            }
            finishActivity(PICK_IMAGE);
        }
        //for camera append
        if (requestCode == REQUEST_CODE_FILE_RETURN && resultCode == RESULT_OK && requestCode != RESULT_CANCELED) {
            Boolean Status = (Boolean) data.getExtras().get("STATUS");
            String Description = (String) data.getExtras().get("DESCRIPTION");
            if (Boolean.FALSE.equals(Status)) {
                String imageCaptureLog = "Description : " + Description + ".Exception: " + Config.CaptureSupport.LastLogInfo;
                Log.d("INFO", imageCaptureLog);
                finishActivity(REQUEST_CODE_FILE_RETURN);
                return;
            }
            FileCollection = (ArrayList<String>) data.getExtras().get("fileCollection");
            if (FileCollection == null || FileCollection.isEmpty()) {
                //clear all selection if other options clicked
                clearImageSelection();
                return;
            }

            try {
                if (useCaseType == 3 || useCaseType == 4) {
                    bitmap = BitmapFactory.decodeFile(FileCollection.get(0));
                    if (bitmap == null) {
                        Toast.makeText(this, "Failed to open the photo", Toast.LENGTH_LONG).show();
                        return;
                    }
                    bmp = saveImage(bitmap);
                    Intent processIntent = null;
                    if (type == null) {
                        Toast.makeText(this, "Please try again", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    switch (type) {
                        case "Mask":
                            try {
                                AiDocument aadhaarDoc1 = new AiDocument(this, bmp);
                                Log.d(TAG, "Document initialised successfully");
                                Log.d(TAG, "ID: " + aadhaarDoc1.ID);
                                Log.d(TAG, "TYPE: " + aadhaarDoc1.TYPE);
                                Log.d(TAG, "FILE_PATH: " + aadhaarDoc1.FILE_PATH);
                                processIntent = new Intent(this, Class.forName("com.extrieve.splicer.aisdk.AadhaarMask"));
                                processIntent.putExtra("DOCUMENT_ID", aadhaarDoc1.ID);
                                com.extrieve.splicer.aisdk.Config.AadhaarMasking.EXTRACT_AADHAAR_NUMBER = true;
                                aadhaarActivityResultLauncher.launch(processIntent);
                            } catch (Exception e) {
                                Log.d("Error", "Runtime error in AadhaarMask: " + e);
                            }
                            break;
                        case "KycDocument":
                            processIntent = new Intent(this, KYCDocAnalysis.class);
                            if (bmp != null) processIntent.putExtra("KYCDocImagePath", bmp);
                            startActivity(processIntent);
                            break;
                        case "KycDocumentUseCase":
                            processIntent = new Intent(this, KYCDocUseCase.class);
                            if (bmp != null) processIntent.putExtra("KYCDocImagePath", bmp);
                            startActivity(processIntent);
                            break;
                        case "GeneralDocument":
                            processIntent = new Intent(this, GeneralDocAnalysis.class);
                            if (bmp != null) processIntent.putExtra("GeneralDocImagePath", bmp);
                            startActivity(processIntent);
                            break;
                    }
                } else {
                    showImages(FileCollection);
                    startLinearShowHideBackLinear();
                    optionLinearHideShowCaptureLinear();
                    imageViewLogo.setVisibility(View.GONE);
                    zoomPanImageView.setVisibility(View.GONE);
                    recyclerLinear.setVisibility(View.VISIBLE);
                }
            } catch(IOException e){
                e.printStackTrace();
            }
            if (useCaseType == 2) {
                Config.CaptureSupport.OutputPath = BuildFileStoragePath();
                buildTiffFile("both", true, FileCollection);
            }
            finishActivity(REQUEST_CODE_FILE_RETURN);
        }
        //for single image append from gallery
        if (requestCode == RESULT_CODE_PICK_1IMAGE && resultCode == RESULT_OK && requestCode != RESULT_CANCELED) {
            Uri ImUri = data.getData();
            try {
                SetCameraConfig();
                Config.CaptureSupport.CaptureMode = Config.CaptureSupport.CaptureModes.IMAGE_ATTACH_REVIEW;
                Intent ReviewIntent = new Intent(this, Class.forName("com.extrieve.quickcapture.sdk.CameraHelper"));
                ReviewIntent.putExtra("ATTACHED_IMAGE", ImUri);
                finishActivity(RESULT_CODE_PICK_1IMAGE);
                attachedActivityResultLauncher.launch(ReviewIntent);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        //for multiple images append from gallery
        if (requestCode == RESULT_CODE_PICK_IMAGE && resultCode == RESULT_OK && requestCode != RESULT_CANCELED) {
            Uri ImUri = null;
            int count = 0;
            FileCollection = new ArrayList<>();
            if (data.getClipData() != null) {
                count = data.getClipData().getItemCount();
            } else {
                count = 1;
            }
            if (count <= 0) {
                //clear all selection if other options clicked
                clearImageSelection();
            }
            for (int i = 0; i < count; i++) {
                if (data.getClipData() != null) {
                    ImUri = data.getClipData().getItemAt(i).getUri();
                }
                if (data.getData() != null) {
                    ImUri = data.getData();
                }
                if (ImUri == null) {
                    return;
                }
                File photoFile = null;
                String outPath = "";
                Bitmap photo = null;
                File imgFile = null;

                try {
                    imgFile = new File(BuildStoragePath());
                    photoFile = createImageFile(imgFile);
                    outPath = photoFile.getAbsolutePath();
                    photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), ImUri);
                } catch (IOException ex) {
                    Toast.makeText(getActivity(), "photoFile  creation failed", Toast.LENGTH_LONG).show();
                    return;
                }
                if (!outPath.isEmpty()) {
                    if (photo == null) {
                        Toast.makeText(getActivity(), "Failed to open the photo", Toast.LENGTH_LONG).show();
                        return;
                    }
                    try {
                        Log.d("AMAL", "CompressToJPEG");
                        ImageHelper.setMaxSize(50);
                        ImageHelper.CompressToJPEG(photo, outPath);
                        Log.d("AMAL", "CompressToJPEG END");
                    } catch (ImgException e) {
                        e.printStackTrace();
                        Log.d("AMAL", String.valueOf(e));
                    }
                    FileCollection.add(outPath);
                }
            }
            try {
                showImages(FileCollection);
                startLinearShowHideBackLinear();
                optionLinearHideShowCaptureLinear();
                imageViewLogo.setVisibility(View.GONE);
                zoomPanImageView.setVisibility(View.GONE);
                recyclerLinear.setVisibility(View.VISIBLE);
                //reload all images
                setSelectedOnCreate(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d("KARTHIK", count + " Images compressed successfully");
            //Toast.makeText(this, count + " Images compressed successfully", Toast.LENGTH_SHORT).show();
            finishActivity(RESULT_CODE_PICK_IMAGE);
        }
    }

    public File createImageFile(File BaseFolder) throws IOException {
        Log.d("KARTHIK", "START createImageFile");
        // Create an image file name
        String timeStamp;
        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "QuickCapture_" + timeStamp + "_" + generate5Digit() + ".jpg";
        File image = new File(BaseFolder, imageFileName);
        Log.d("KARTHIK", "END createImageFile");
        return image;
    }

    public int generate5Digit() {
        Random r = new Random(System.currentTimeMillis());
        return ((1 + r.nextInt(2)) * 10000 + r.nextInt(10000));
    }

    private void buildTiffFile(String buildType, Boolean customCol, ArrayList<String> CustomOutputCollection) {
        runOnUiThread(() -> {
            if (Objects.equals(buildType, "both")) {
                ShowProgressToast("building both output files", true);
            } else if (Objects.equals(buildType, "tiff")) {
                ShowProgressToast("building tiff output file", true);
            } else if (Objects.equals(buildType, "pdf")) {
                ShowProgressToast("building pdf output file", true);
            }
        });
        AsyncTask.execute(() -> {
            /*Note : All apps that target Android 11 (API level 30) and above are subject to Scoped Storage restrictions
              and cannot request legacy access to device storage. Instead, they must request a new permission
              called MANAGE_EXTERNAL_STORAGE (shown to the user as “All Files Access”) to be given broad storage
              access (excluding a handful of directories like /Android/data or /Android/obb).*/
            /* User can able to save output in DOCUMENTS & DOWNLOADS Folders only: 21-06-2023*/
            SharedPreferences sh = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
            enableDisableOutputDir = Boolean.parseBoolean(sh.getString("enableDisableOutputDir", "false"));
            String internalAppPath = BuildFileStoragePath();
            File dir2 = new File(internalAppPath);
            if (!dir2.exists()) {
                dir2.mkdirs();
            }
            String strTifFilePublic = null, strPDFFilePubic = null;
            String tiffFile = null, pdfFilePublic = null;
            if (folderPath == null) {
                strTifFilePublic = dir2 + "/TIF_OUTPUT_" + UUID.randomUUID() + ".tif";
                strPDFFilePubic = dir2 + "/PDF_OUTPUT_" + UUID.randomUUID() + ".pdf";
            } else {
                strTifFilePublic = folderPath + "/TIF_OUTPUT_" + UUID.randomUUID() + ".tif";
                strPDFFilePubic = folderPath + "/PDF_OUTPUT_" + UUID.randomUUID() + ".pdf";
            }

            if (Objects.equals(buildType, "both")) {
                if (customCol) {
                    FinalOutputCollection = CustomOutputCollection;
                }
                tiffFile = ImageHelper.BuildTiff(FinalOutputCollection, strTifFilePublic);
                pdfFilePublic = ImageHelper.BuildPDF(FinalOutputCollection, strPDFFilePubic);
                ShowProgressToast("building both output files", false);
                if (tiffFile == null || pdfFilePublic == null || tiffFile.contains("FAILED") || pdfFilePublic.contains("FAILED")) {
                    showToast("Output Creation - Failed", Gravity.CENTER);
                } else {
                    showToast("Output Creation - Success", Gravity.CENTER);
                    if (tiffFile.contains("SUCCESS")) {
                        File inFile1 = new File(tiffFile);
                        String outputPath1 = "";
                        if (enableDisableOutputDir) {
                            outputPath1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/QuickCapture";
                        } else {
                            outputPath1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/QuickCapture";
                        }
                        FileCopier.moveFile(strTifFilePublic, outputPath1 + "/" + inFile1.getName());
                    }
                    if (pdfFilePublic.contains("SUCCESS")) {
                        File inFile = new File(strPDFFilePubic);
                        String outputPath = "";
                        if (enableDisableOutputDir) {
                            outputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/QuickCapture";
                        } else {
                            outputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/QuickCapture";
                        }
                        FileCopier.moveFile(strPDFFilePubic, outputPath + "/" + inFile.getName());
                        openPdfFileTrigger(outputPath + "/" + inFile.getName());
                    }
                }
            } else if (Objects.equals(buildType, "tiff")) {
                tiffFile = ImageHelper.BuildTiff(FinalOutputCollection, strTifFilePublic);
                ShowProgressToast("building tiff output file", false);
                if (tiffFile == null || tiffFile.contains("FAILED")) {
                    showToast("Output Creation - Failed", Gravity.CENTER);
                } else {
                    showToast("Output Creation - Success", Gravity.CENTER);
                    if (tiffFile.contains("SUCCESS")) {
                        File inFile1 = new File(tiffFile);
                        String outputPath1 = "";
                        if (enableDisableOutputDir) {
                            outputPath1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/QuickCapture";
                        } else {
                            outputPath1 = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/QuickCapture";
                        }
                        FileCopier.moveFile(strTifFilePublic, outputPath1 + "/" + inFile1.getName());
                    }
                }
            } else if (Objects.equals(buildType, "pdf")) {
                pdfFilePublic = ImageHelper.BuildPDF(FinalOutputCollection, strPDFFilePubic);
                ShowProgressToast("building pdf output file", false);
                if (pdfFilePublic == null || pdfFilePublic.contains("FAILED")) {
                    showToast("Output Creation - Failed", Gravity.CENTER);
                } else {
                    showToast("Output Creation - Success", Gravity.CENTER);
                    if (pdfFilePublic.contains("SUCCESS")) {
                        File inFile = new File(strPDFFilePubic);
                        String outputPath = "";
                        if (enableDisableOutputDir) {
                            outputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/QuickCapture";
                        } else {
                            outputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/QuickCapture";
                        }
                        FileCopier.moveFile(strPDFFilePubic, outputPath + "/" + inFile.getName());
                        openPdfFileTrigger(outputPath + "/" + inFile.getName());
                    }
                }
            }
        });
    }

    private void openPdfFileTrigger(String pdfFilePath) {
        File file = new File(pdfFilePath);
        //Uri path = Uri.fromFile(file);
        String PackageName = this.getApplicationContext().getPackageName();
        Uri photoURI = FileProvider.getUriForFile(this, PackageName + ".FileProvider", file);

        Intent pdfOpenintent = new Intent(Intent.ACTION_VIEW);
        pdfOpenintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        List<ResolveInfo> resolvedIntentActivities = this.getPackageManager().queryIntentActivities(pdfOpenintent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
            String packageName = resolvedIntentInfo.activityInfo.packageName;
            this.grantUriPermission(packageName, photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        pdfOpenintent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            //Log.d(TAG, "Build <= LOLLIPOP");
            pdfOpenintent.setClipData(ClipData.newRawUri("", photoURI));
            pdfOpenintent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        pdfOpenintent.setDataAndType(photoURI, "application/pdf");
        try {
            startActivity(pdfOpenintent);
        } catch (ActivityNotFoundException ignored) {

        }
    }

    private void openFolder(String FolderPath) {
        Uri selectedUri = Uri.parse(FolderPath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(selectedUri, "resource/folder");

        if (intent.resolveActivityInfo(getPackageManager(), 0) != null) {
            startActivity(intent);
        }
        // if you reach this place, it means there is no any file
        // explorer app installed on your device
    }

    private void writeTiffFileOutput(File tiffFile) throws IOException {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/QuickCapture_Tif/");
        if (!dir.exists()) dir.mkdirs();

        FileOutputStream out = null;
        FileInputStream in = null;
        int cursor;
        try {
            in = new FileInputStream(tiffFile);
            out = new FileOutputStream(dir + "/TIF_OUTPUT.tif");
            while ((cursor = in.read()) != -1) {
                out.write(cursor);
            }
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
            ShowProgressToast("building tiff output", false);
            showToast("Tiff output created : " + dir + "/TIF_OUTPUT.tif", Gravity.CENTER);
        }
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight &&
                    (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void showImages(ArrayList<String> FilesPath) throws IOException {
        int FileCollectionLength = FilesPath.size();
        for (int i = 0; i < FileCollectionLength; i++) {
            String dir = FilesPath.get(i);
            File imgFile = new File(dir);
            if (enableDisableAddImagesToGallery) notifyMediaStoreScanner(imgFile);
            if (imgFile.exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options); // Populates outWidth/outHeight
                options.inSampleSize = calculateInSampleSize(options, 1080, 1920); // or any safe target size
                options.inJustDecodeBounds = false;
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                ImageView myImage = findViewById(R.id.zoom_pan_image_view);
                myImage.setImageBitmap(myBitmap);
                if (i == FileCollectionLength - 1) {
                    Log.d("KARTHIK", "SDK captured " + FileCollectionLength + " images");
                }
                String imageURL = String.valueOf(FileCollection.get(i));
                uri.add(Uri.parse(imageURL));
                String fileName = imgFile.getName();
                TextView setFileName = findViewById(R.id.filename);
                setFileName.setText(fileName);
            }
        }
        addThumbnails();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Toast.makeText(this, "All Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void requestAllStoragePermission() {

        if (Build.VERSION_CODES.R >= 30) {
            if (!Environment.isExternalStorageManager()) {
                Snackbar.make(findViewById(android.R.id.content), "Permission needed!", Snackbar.LENGTH_INDEFINITE).setAction("Settings", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        try {
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                            startActivity(intent);
                        } catch (Exception ex) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            startActivity(intent);
                        }
                    }
                }).show();
            }
            if (Environment.isExternalStorageManager()) {
                Toast.makeText(getActivity(), "All folder access granted", Toast.LENGTH_LONG).show();
            }
        }
    }

    @NotNull
    private String BuildStoragePath() {
        ContextWrapper c = new ContextWrapper(this);
        return c.getExternalFilesDir(".fileStore").getAbsolutePath();
    }

    @NotNull
    private String BuildFileStoragePath() {
        ContextWrapper c = new ContextWrapper(this);
        return c.getExternalFilesDir(".fileStorage").getAbsolutePath();
    }

    @NotNull
    private String BuildSplicerStoragePath() {
        ContextWrapper c = new ContextWrapper(this);
        return c.getExternalFilesDir(".splicerStorage").getAbsolutePath();
    }

    private void removeFilesInSplicerStorage() {
        String path = BuildSplicerStoragePath();
        File directory = new File(path);

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    public String readFile(String filePath) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            FileInputStream fileInputStream = new FileInputStream("http://localhost:91/ras/com.extrieve.qctestapp_extrieve%20technologies%20internal_Internal_11042023_1month_pd%20-%20Copy.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    private static String readAssetFile(String AssetFileName, Context assetActivityContext) throws IOException {
        String mLine;
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(assetActivityContext.getAssets().open(AssetFileName)));
            while ((mLine = reader.readLine()) != null) {
                builder.append(mLine);
            }
        } finally {
            if (reader == null) {
                return null;
            } else {
                reader.close();
            }
        }
        return (builder.length() > 1) ? builder.toString() : null;
    }

    private void SetCameraConfig2() throws IOException {

        //can set output file path
        Config.CaptureSupport.OutputPath = BuildStoragePath();

        //Capture sound
        Config.CaptureSupport.CaptureSound = false;

        Config.CaptureSupport.EnableFlash = true;

        Config.CaptureSupport.ShowCaptureCountAndLimit = true;

        Config.CaptureSupport.CameraToggle = Config.CaptureSupport.CameraToggleType.ENABLE_BACK_DEFAULT;
        //0-Disable camera toggle option
        //1-Enable camera toggle option with Front camera by default
        //2-Enable camera toggle option with Back camera by default

        Config.CaptureSupport.DocumentCropping = Config.CaptureSupport.CroppingType.AssistedCapture;

        //Config.CaptureSupport.CaptureProfile = 0;//ID CARD
    }

    private void SetCameraConfigID() throws IOException {

        //can set output file path
        Config.CaptureSupport.OutputPath = BuildStoragePath();

        //Capture sound
        Config.CaptureSupport.CaptureSound = false;

        Config.CaptureSupport.EnableFlash = true;

        Config.CaptureSupport.ShowCaptureCountAndLimit = true;

        Config.CaptureSupport.CameraToggle = Config.CaptureSupport.CameraToggleType.ENABLE_BACK_DEFAULT;
        //0-Disable camera toggle option
        //1-Enable camera toggle option with Front camera by default
        //2-Enable camera toggle option with Back camera by default

        Config.CaptureSupport.DocumentCropping = Config.CaptureSupport.CroppingType.AssistedCapture;

        //Config.CaptureSupport.CaptureProfile = 1;//ID CARD
    }

    private void SetCameraConfig() throws IOException {
        SharedPreferences sh = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
        String pgl = sh.getString("getLayoutOption", "A4");
        String imq = sh.getString("getQualityOption", "DOCUMENT QUALITY");
        String rzm = sh.getString("getResizeMode", "PRESERVE ASPECT ONLY");
        String rzmValues = sh.getString("getResizeModeValues", "200,200");
        String imd = sh.getString("getDPIOption", "200");
        String reo = sh.getString("getReviewOption", "ENABLE");
        String scm = sh.getString("getCaptureMode", "DEFAULT");
        String crf = sh.getString("getCropFilterOption", "NONE");
        String clo = sh.getString("getColorOption", "RGB");
        String act = sh.getString("getAutoCaptureToggle", "DISABLED");
        String dds = sh.getString("getDocDetectSensitivity", "SENS 50");
        String pro = sh.getString("getProfileOption", "NORMAL");
        String cro = sh.getString("getCropOption", "ASSISTED CAPTURE");
        String flo = sh.getString("getFlashOption", "ENABLE");
        String cmo = sh.getString("getCameraOption", "APP");
        String tgo = sh.getString("getToggleOption", "ENABLE BACK DEFAULT");
        String sdo = sh.getString("getSoundOption", "DISABLE");
        String sco = sh.getString("getShowCountOption", "ENABLE");
        String pgs = sh.getString("getMaxPages", "UNLIMITED PAGES");
        String opd = sh.getString("enableDisableOutputDir", "false");
        int pageLayout = 0, imqData = 0, imdData = 0, pgData = 0;
        //setting Image Page Layout to config data
        if (pgl.length() == 2) {
            pageLayout = Integer.parseInt(String.valueOf(pgl.charAt(1)));
        } else if (pgl.length() == 5) {
            pageLayout = 8;
        }
        ImageHelper.SetPageLayout(pageLayout);//A1-A7(1-7),PHOTO,CUSTOM,ID(8,9,10)
        //setting Image Quality to config data
        switch (imq) {
            case "PHOTO QUALITY":
                break;
            case "DOCUMENT QUALITY":
                imqData = 1;
                break;
            case "COMPRESSED QUALITY":
                imqData = 2;
                break;
        }
        ImageHelper.SetImageQuality(imqData);//0,1,2 - Photo_Quality, Document_Quality, Compressed_Document
        //setting Image Resize Mode to config data
        switch (rzm) {
            case "PRESERVE ASPECT ONLY":
                ImageHelper.SetResizeMode(ImgHelper.ResizeMode.PRESERVE_ASPECT_ONLY);
                break;
            case "STRETCH TO EXACT SIZE":
                ImageHelper.SetResizeMode(ImgHelper.ResizeMode.STRETCH_TO_EXACT_SIZE);
                String[] values = rzmValues.split(",");
                int width = Integer.parseInt(values[0].trim()); // First value
                int height = Integer.parseInt(values[1].trim());
                ImageHelper.SetCustomLayOutInPixels(width, height);
                break;
            case "FIT WITH ASPECT":
                ImageHelper.SetResizeMode(ImgHelper.ResizeMode.FIT_WITH_ASPECT);
                break;
        }
        //setting Image DPI to config data
        imdData = Integer.parseInt(imd);
        ImageHelper.SetDPI(imdData);//int dpi_val = 100, 150, 200, 300, 500, 600;
        //setting path to save images for temporary
        Config.CaptureSupport.OutputPath = BuildStoragePath();
        //setting Max Images to config data
        switch (pgs) {
            case "UNLIMITED PAGES":
                break;
            case "SINGLE PAGE":
                pgData = 1;
                break;
            case "2 PAGES":
                pgData = 2;
                break;
            case "5 PAGES":
                pgData = 5;
                break;
            case "10 PAGES":
                pgData = 10;
                break;
            case "20 PAGES":
                pgData = 20;
                break;
            case "50 PAGES":
                pgData = 50;
                break;
        }
        Config.CaptureSupport.MaxPage = pgData;
        //setting crop filter to set in config data
        if (cro.equals("DISABLE")) {
            Config.CaptureSupport.CropFilter = Config.CaptureSupport.CropImageFilterType.NONE;
        } else {
            switch (crf) {
                case "ENHANCE COLOR":
                    Config.CaptureSupport.CropFilter = Config.CaptureSupport.CropImageFilterType.ENHANCE;
                    break;
                case "REMOVE SHADOW":
                    Config.CaptureSupport.CropFilter = Config.CaptureSupport.CropImageFilterType.GRAY;
                    break;
                case "XEROX FILTER":
                    Config.CaptureSupport.CropFilter = Config.CaptureSupport.CropImageFilterType.XEROX;
                    break;
                default:
                    Config.CaptureSupport.CropFilter = Config.CaptureSupport.CropImageFilterType.NONE;
                    break;
            }
        }
        //setting Color Mode to config data
        if (clo.equals("RGB")) {
            Config.CaptureSupport.ColorMode = Config.CaptureSupport.ColorModes.RBG;
        } else if (clo.equals("GRAY")) {
            Config.CaptureSupport.ColorMode = Config.CaptureSupport.ColorModes.GRAY;
        } else {
            Config.CaptureSupport.ColorMode = Config.CaptureSupport.ColorModes.BLACK_WHITE;
        }
        //setting Auto capture type to config data
//        if (act.equals("DISABLED")) {
//            Config.CaptureSupport.AutoCaptureToggle = Config.CaptureSupport.AutoCaptureToggleType.DISABLED;
//        } else if (act.equals("ENABLE AUTO DEFAULT")) {
//            Config.CaptureSupport.AutoCaptureToggle = Config.CaptureSupport.AutoCaptureToggleType.ENABLE_AUTO_DEFAULT;
//        } else {
//            Config.CaptureSupport.AutoCaptureToggle = Config.CaptureSupport.AutoCaptureToggleType.ENABLE_MANUAL_DEFAULT;
//        }
        //setting Doc detect sensitivity to config data
        switch (dds) {
            case "SENS 20":
                Config.CaptureSupport.DocDetectSensitivity = Config.CaptureSupport.Sensitivity.SENS_20;
                break;
            case "SENS 30":
                Config.CaptureSupport.DocDetectSensitivity = Config.CaptureSupport.Sensitivity.SENS_30;
                break;
            case "SENS 40":
                Config.CaptureSupport.DocDetectSensitivity = Config.CaptureSupport.Sensitivity.SENS_40;
                break;
            case "SENS 50":
                Config.CaptureSupport.DocDetectSensitivity = Config.CaptureSupport.Sensitivity.SENS_50;
                break;
            case "SENS 60":
                Config.CaptureSupport.DocDetectSensitivity = Config.CaptureSupport.Sensitivity.SENS_60;
                break;
            case "SENS 70":
                Config.CaptureSupport.DocDetectSensitivity = Config.CaptureSupport.Sensitivity.SENS_70;
                break;
            case "SENS 80":
                Config.CaptureSupport.DocDetectSensitivity = Config.CaptureSupport.Sensitivity.SENS_80;
                break;
            default:
                Config.CaptureSupport.DocDetectSensitivity = Config.CaptureSupport.Sensitivity.SENS_90;
                break;
        }
        //setting Sound Mode to config data
        Config.CaptureSupport.CaptureSound = !sdo.equals("DISABLE");
        //setting output directory to documents or downloads
        enableDisableOutputDir = opd.equals("DOCUMENTS");
        //setting Flash Mode to config data
        Config.CaptureSupport.EnableFlash = !flo.equals("DISABLE");
        //setting Capture count show to config data
        Config.CaptureSupport.ShowCaptureCountAndLimit = !sco.equals("DISABLE");
        //setting Capture default camera to config data
        if (cmo.equals("APP")) {
            Config.CaptureSupport.CaptureMode = Config.CaptureSupport.CaptureModes.CAMERA_CAPTURE_REVIEW;
        } else {
            Config.CaptureSupport.CaptureMode = Config.CaptureSupport.CaptureModes.SYSTEM_CAMERA_CAPTURE_REVIEW;
        }
        //setting Capture mode aadhaar/default to config data
        if (scm.equals("DEFAULT")) {
            Config.CaptureSupport.DocumentType = Config.CaptureSupport.Document.DEFAULT;
        } else {
            Config.CaptureSupport.DocumentType = Config.CaptureSupport.Document.AADHAAR;
        }
        //setting Capture crop Mode to config data
        switch (cro) {
            case "DISABLE":
                Config.CaptureSupport.DocumentCropping = Config.CaptureSupport.CroppingType.Disabled;
                break;
            case "AUTO CAPTURE":
                Config.CaptureSupport.DocumentCropping = Config.CaptureSupport.CroppingType.AutoCapture;
                break;
            case "AUTO CROP":
                Config.CaptureSupport.DocumentCropping = Config.CaptureSupport.CroppingType.AutoCrop;
                break;
            default:
                Config.CaptureSupport.DocumentCropping = Config.CaptureSupport.CroppingType.AssistedCapture;
                break;
        }
        //setting Capture profile Mode to config data
        if (pro.equals("NORMAL")) {
            Config.CaptureSupport.CaptureProfile = Config.CaptureSupport.ProfileType.DEFAULT;
        } else {
            Config.CaptureSupport.CaptureProfile = Config.CaptureSupport.ProfileType.ID_CAPTURE;
        }
        //setting Capture toggle Mode to config data
        if (tgo.equals("DISABLE")) {
            Config.CaptureSupport.CameraToggle = Config.CaptureSupport.CameraToggleType.DISABLED;
        } else if (tgo.equals("ENABLE FRONT DEFAULT")) {
            Config.CaptureSupport.CameraToggle = Config.CaptureSupport.CameraToggleType.ENABLE_FRONT_DEFAULT;
        } else {
            Config.CaptureSupport.CameraToggle = Config.CaptureSupport.CameraToggleType.ENABLE_BACK_DEFAULT;
        }
        Config.CaptureSupport.BottomStampData = "";
    }

    private void OpenCameraActivity() {
        //before starting camera - configuration can set
        //String quality = ImageHelper.getCurrentImageQuality();
        try {
            Config.CaptureSupport.BottomStampData = "Captured With QuickCapture Mobile scanning SDK - Extrieve Technologies - Enterprise DMS | Workflow | OCR | PDF SDK API with AI  $  www.extrieve.com | info@extrieve.com |  globalsales@extrieve.com | Captured on : {DATETIME}";
            //moving to camera activity in library
            Intent CameraIntent = new Intent(this, Class.forName("com.extrieve.quickcapture.sdk.CameraHelper"));
            Uri photoURI = Uri.parse(Config.CaptureSupport.OutputPath);
            getActivity().grantUriPermission(this.getPackageName(), photoURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                CameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
            startActivityForResult(CameraIntent, REQUEST_CODE_FILE_RETURN);

        } catch (ClassNotFoundException e) {
            Toast.makeText(getActivity(), "Failed to open camera + ", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void CheckAndPromptPermissions() {
        if (allPermissionsGranted()) {
            permissionStatus = true;
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS_ABOVE13, REQUEST_CODE_PERMISSIONS);
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }
        }
    }

    public boolean allPermissionsGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            for (String permission : REQUIRED_PERMISSIONS_ABOVE13) {
                String test = String.valueOf(ContextCompat.checkSelfPermission(getActivity(), permission));
                if (permission.equals("android.permission.MANAGE_EXTERNAL_STORAGE")) {
                    requestAllStoragePermission();
                } else if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED)
                    return false;
            }
        } else {
            for (String permission : REQUIRED_PERMISSIONS) {
                String test = String.valueOf(ContextCompat.checkSelfPermission(getActivity(), permission));
                if (permission.equals("android.permission.MANAGE_EXTERNAL_STORAGE")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        requestAllStoragePermission();
                    }
                } else if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED)
                    return false;
            }
        }
        return true;
    }

    public Context getActivity() {
        return this;
    }

    public String setMergedFolderName() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("HHmmss");
        return mdformat.format(calendar.getTime());
    }

    public static void deleteFiles(String path) {
        File file = new File(path);
        if (file.exists()) {
            String deleteCmd = "rm -r " + path;
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
            } catch (IOException ignored) {
            }
        }
    }

    private void shareDataExternal(String pathToImage) {

        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Hello!");
        // (Optional) Here we're setting the title of the content
        sendIntent.putExtra(Intent.EXTRA_TITLE, "Send message");
        // (Optional) Here we're passing a content URI to an image to be displayed
        File file = new File(pathToImage);
        if (file.canWrite()) {
            Uri uri = Uri.fromFile(file);
            sendIntent.setData(uri);
            // Show the ShareSheet
            startActivity(Intent.createChooser(sendIntent, null));
        } else {
            Toast.makeText(this, "No able to write file - shareDataExternal.", Toast.LENGTH_SHORT).show();
        }
    }

    public final void notifyMediaStoreScanner(final File file) {
        try {
            MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), file.getAbsolutePath(), file.getName(), null);
            getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private ProgressDialog pd = null;

    private void ShowProgressToast(String Message, boolean show) {
        try {
            if (pd != null) pd.dismiss();
            if (!show) return;
            pd = new ProgressDialog(MainActivity.this);
            pd.setCancelable(false);
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.setMessage(Message);
            pd.show();
        } catch (Exception e) {
            Log.d("mainApp", "ShowProgressToast: " + e);
        }
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text, final int TPosition) {
        final Activity activity = MainActivity.this;
        activity.runOnUiThread(() -> {
            Toast toast = Toast.makeText(activity, text, Toast.LENGTH_SHORT);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                toast.setGravity(TPosition, 0, 30);
            }
            toast.show();
        });
    }

    protected void updateCountToUI() {
        if (isSelectMode) {
            int selectedCountTotal = selectedItemCount();
            selectedCount.setVisibility(View.VISIBLE);
            deleteSelected.setVisibility(View.VISIBLE);
            if (selectedCountTotal == 0) {
                selectedCount.setText("Select Images");
                deleteSelected.setVisibility(View.GONE);
            }
            if (selectedCountTotal == 1) {
                selectedCount.setText("Selected 1 Image");
                selectAllImages.setChecked(false);
            }
            if (selectedCountTotal > 1 && selectedCountTotal < uri.size()) {
                selectedCount.setText("Selected " + selectedCountTotal + " Images");
                selectAllImages.setChecked(false);
            }
            if (selectedCountTotal == uri.size()) {
                selectedCount.setText("Selected " + selectedCountTotal + " Images");
                selectAllImages.setChecked(true);
            }
        } else {
            selectedCount.setVisibility(View.GONE);
            deleteSelected.setVisibility(View.GONE);
        }
    }

    private String getFileSize(String fileName, String type) {
        Path path = null;
        String inByte = null, inKb = null, inMb = null, data = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            path = Paths.get(fileName);
        }
        try {
            // size of a file (in bytes)
            long bytes = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bytes = Files.size(path);
            }
            inByte = String.format("%,d bytes", bytes);
            inKb = String.format("%,d kb", bytes / 1024);
            inMb = String.format("%,d mb", bytes / 1024 / 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Objects.equals(type, "byte")) data = inByte;
        if (Objects.equals(type, "kb")) data = inKb;
        if (Objects.equals(type, "mb")) data = inMb;
        return data;
    }

    private void openAboutDialog() {
        Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.about_show);
        TextView copyright = dialog.findViewById(R.id.copyright);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        copyright.setText("Copyright © 1996 - " + currentYear + " Extrieve® Technologies. \n All Rights Reserved");
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().getAttributes().windowAnimations = R.style.animation;
        dialog.setCancelable(true);
        dialog.show();
    }

    private void openPrivacyPolicy() {
        String urlString = "https://www.extrieve.com/privacy/quickcapture-app/";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage("com.android.chrome");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            intent.setPackage(null);
            startActivity(intent);
        }
    }

    private void showSDKInfo() throws JSONException {
        String sdkInfo = Config.Common.SDKInfo;
        String sdkInfo1 = com.extrieve.splicer.aisdk.Config.Common.SDKInfo;

        JSONObject jsonObject = new JSONObject(sdkInfo);
        JSONObject jsonObject1 = new JSONObject(sdkInfo1);

        StringBuilder formattedData = new StringBuilder();

        // First SDK Info
        formattedData.append("QC SDK Info :\n");
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = jsonObject.getString(key);
            formattedData.append(key).append(": ").append(value).append("\n");
        }

        formattedData.append("\nSPLICER SDK Info :\n");
        Iterator<String> keys1 = jsonObject1.keys();
        while (keys1.hasNext()) {
            String key1 = keys1.next();
            String value1 = jsonObject1.getString(key1); // Corrected!
            formattedData.append(key1).append(": ").append(value1).append("\n");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("SDK Information");
        builder.setMessage(formattedData.toString());
        builder.setCancelable(true);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showDeviceInfo() {
        String DeviceInfo = CameraHelper.GetDeviceInfo().trim();
        DeviceInfo = DeviceInfo.replace('.', ' ');
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Device Information");
        builder.setMessage(DeviceInfo);
        builder.setCancelable(true);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showBottomSheetDialog() {

        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog);

        LinearLayout sdkInfo = bottomSheetDialog.findViewById(R.id.sdkInfo);
        LinearLayout deviceInfo = bottomSheetDialog.findViewById(R.id.deviceInfo);
        LinearLayout privacy = bottomSheetDialog.findViewById(R.id.privacy);
        LinearLayout aboutApp = bottomSheetDialog.findViewById(R.id.about);
        LinearLayout exit = bottomSheetDialog.findViewById(R.id.exit);
        if (sdkInfo != null) {
            sdkInfo.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                try {
                    showSDKInfo();
                } catch (JSONException ignored) {
                    Toast.makeText(this, "Unable to load info", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (deviceInfo != null) {
            deviceInfo.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                showDeviceInfo();
            });
        }
        if (privacy != null) {
            privacy.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                openPrivacyPolicy();
            });
        }
        if (aboutApp != null) {
            aboutApp.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                openAboutDialog();
            });
        }
        if (exit != null) {
            exit.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                //karthik: clear only uri list, not files in app storage path
                clear();
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("EXIT", true);
                startActivity(intent);
                finishAffinity();
            });
        }
        bottomSheetDialog.show();
    }

    public void showUseCasePopup() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.popupusecases, null);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(popupView)
                .create();

        // Close Button
        ImageView closeBtn = popupView.findViewById(R.id.closePopup);
        closeBtn.setOnClickListener(view -> dialog.dismiss());

        // Tiles Click Listeners
        LinearLayout tile1 = popupView.findViewById(R.id.tile_1);
        LinearLayout tile2 = popupView.findViewById(R.id.tile_2);
        LinearLayout tile3 = popupView.findViewById(R.id.tile_3);
        LinearLayout tile4 = popupView.findViewById(R.id.tile_4);

        tile1.setOnClickListener(v -> {
            dialog.dismiss();
            UseCaseDemo(1);
        });
        tile2.setOnClickListener(v -> {
            dialog.dismiss();
            UseCaseDemo(2);
        });
        tile3.setOnClickListener(v -> {
            dialog.dismiss();
            UseCaseDemo(3);
        });
        tile4.setOnClickListener(v -> {
            dialog.dismiss();
            UseCaseDemo(4);
        });

        dialog.show();
    }


    private void showPopupDialog() {
        Dialog dialog = new Dialog(MainActivity.this, R.style.DialogTheme);
        dialog.setContentView(R.layout.popupinputdialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.setCancelable(false);
        dialog.getWindow().getAttributes().windowAnimations = R.style.animation;

        LinearLayout privacyLink = dialog.findViewById(R.id.privacy_policy);
        LinearLayout resetSettingToDefault = dialog.findViewById(R.id.resetSettingToDefault);
        LinearLayout seeOutputDirectory = dialog.findViewById(R.id.seeOutputDirectory);
        ImageView save = dialog.findViewById(R.id.button_save);
        ImageView cancel = dialog.findViewById(R.id.button_cancel);
        Switch enableDisableAddImagesToGalleryBtn = dialog.findViewById(R.id.enableDisableAddImagesToGalleryBtn);
        Switch enableDisableOutputDirBtn = dialog.findViewById(R.id.enableDisableOutputDirBtn);

        //reset settings to default values
        resetSettingToDefault.setOnClickListener(v -> {
            defaultInputForConfig();
            dialog.dismiss();
        });
        //open saved documents folder, show only for downloads
        seeOutputDirectory.setOnClickListener(v -> {
            dialog.dismiss();
            openDownloadFolder();
        });
        //open privacy policy url in chrome
        privacyLink.setOnClickListener(v -> {
            dialog.dismiss();
            openPrivacyPolicy();
        });
        //GET SAVED DATA FROM SHARED
        SharedPreferences sh = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
        //for add images to gallery enable disable
        enableDisableAddImagesToGallery = Boolean.parseBoolean(sh.getString("enableDisableAddToGallery", "true"));
        enableDisableAddImagesToGalleryBtn.setChecked(enableDisableAddImagesToGallery);
        enableDisableAddImagesToGalleryBtn.setOnCheckedChangeListener((buttonView, isChecked) -> enableDisableAddImagesToGallery = isChecked);
        //for save output to internal public storage
        enableDisableOutputDir = Boolean.parseBoolean(sh.getString("enableDisableOutputDir", "false"));
        if (enableDisableOutputDir) {
            enableDisableOutputDirBtn.setChecked(true);
            seeOutputDirectory.setVisibility(View.GONE);
        } else {
            enableDisableOutputDirBtn.setChecked(false);
            seeOutputDirectory.setVisibility(View.VISIBLE);
        }
        enableDisableOutputDirBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                enableDisableOutputDir = true;
                seeOutputDirectory.setVisibility(View.GONE);
            } else {
                enableDisableOutputDir = false;
                seeOutputDirectory.setVisibility(View.VISIBLE);
            }
        });
        //for camera select
        Switch switchSetCamera = dialog.findViewById(R.id.switch_getSetCamera);
        //set saved data here from shared
        setCameraOption = sh.getString("getCameraOption", "APP");
        switchSetCamera.setChecked(setCameraOption.equals("SYSTEM"));
        switchSetCamera.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setCameraOption = "SYSTEM";
            } else {
                setCameraOption = "APP";
            }
        });
        //for page layout
        Spinner spinnerLayouts = dialog.findViewById(R.id.spinner_layouts);
        TextView layoutSelectedText = dialog.findViewById(R.id.layoutSelectedText);
        ArrayAdapter<CharSequence> adapter1 = ArrayAdapter.createFromResource(this, R.array.pageLayouts, R.layout.spinner_row);
        adapter1.setDropDownViewResource(R.layout.spinner_row);
        spinnerLayouts.setAdapter(adapter1);
        //set saved data here from shared
        setLayoutOption = sh.getString("getLayoutOption", "A4");
        int spinnerPosition1 = adapter1.getPosition(setLayoutOption);
        spinnerLayouts.setSelection(spinnerPosition1);
        layoutSelectedText.setText(setLayoutOption);
        spinnerLayouts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setLayoutOption = spinnerLayouts.getSelectedItem().toString();
                {
                    int spinnerPosition1 = adapter1.getPosition(setLayoutOption);
                    spinnerLayouts.setSelection(spinnerPosition1);
                    layoutSelectedText.setText(setLayoutOption);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("KARTHIK", "NO SELECTION:" + setLayoutOption);
            }
        });
        LinearLayout layoutLinear = (LinearLayout) dialog.findViewById(R.id.layoutLinear);
        layoutLinear.setOnClickListener((View.OnClickListener) v -> {
            if (spinnerLayouts.getSelectedItem() != null) {
                spinnerLayouts.performClick();
            }
        });
        //for camera capture mode
        Spinner spinnerCapture = dialog.findViewById(R.id.spinner_captureMode);
        TextView captureSelectedText = dialog.findViewById(R.id.captureSelectedText);
        ArrayAdapter<CharSequence> adapter0 = ArrayAdapter.createFromResource(this, R.array.captureMode, R.layout.spinner_row);
        adapter0.setDropDownViewResource(R.layout.spinner_row);
        spinnerCapture.setAdapter(adapter0);
        //set saved data here from shared
        setCaptureMode = sh.getString("getCaptureMode", "DEFAULT");
        {
            int spinnerPosition0 = adapter0.getPosition(setCaptureMode);
            spinnerCapture.setSelection(spinnerPosition0);
            captureSelectedText.setText(setCaptureMode);
        }
        spinnerCapture.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setCaptureMode = spinnerCapture.getSelectedItem().toString();
                int spinnerPosition0 = adapter0.getPosition(setCaptureMode);
                spinnerCapture.setSelection(spinnerPosition0);
                captureSelectedText.setText(setCaptureMode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("KARTHIK", "NO SELECTION:" + setCaptureMode);
            }
        });
        LinearLayout captureLinear = (LinearLayout) dialog.findViewById(R.id.captureLinear);
        captureLinear.setOnClickListener((View.OnClickListener) v -> {
            if (spinnerCapture.getSelectedItem() != null) {
                spinnerCapture.performClick();
            }
        });
        //for image quality
        Spinner spinnerImgQuality = dialog.findViewById(R.id.spinner_imgQuality);
        TextView qualitySelectedText = dialog.findViewById(R.id.qualitySelectedText);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this, R.array.imgQuality, R.layout.spinner_row);
        adapter2.setDropDownViewResource(R.layout.spinner_row);
        spinnerImgQuality.setAdapter(adapter2);
        //set saved data here from shared
        setQualityOption = sh.getString("getQualityOption", "DOCUMENT QUALITY");
        {
            int spinnerPosition2 = adapter2.getPosition(setQualityOption);
            spinnerImgQuality.setSelection(spinnerPosition2);
            qualitySelectedText.setText(setQualityOption);
        }
        spinnerImgQuality.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setQualityOption = spinnerImgQuality.getSelectedItem().toString();
                int spinnerPosition2 = adapter2.getPosition(setQualityOption);
                spinnerImgQuality.setSelection(spinnerPosition2);
                qualitySelectedText.setText(setQualityOption);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("KARTHIK", "NO SELECTION:" + setLayoutOption);
            }
        });
        LinearLayout qualityLinear = (LinearLayout) dialog.findViewById(R.id.qualityLinear);
        qualityLinear.setOnClickListener((View.OnClickListener) v -> {
            if (spinnerImgQuality.getSelectedItem() != null) {
                spinnerImgQuality.performClick();
            }
        });
        //for image dpi
        Spinner spinnerImgDPI = dialog.findViewById(R.id.spinner_imgDPI);
        TextView dpiSelectedText = dialog.findViewById(R.id.dpiSelectedText);
        ArrayAdapter<CharSequence> adapter3 = ArrayAdapter.createFromResource(this, R.array.imgDPI, R.layout.spinner_row);
        adapter3.setDropDownViewResource(R.layout.spinner_row);
        spinnerImgDPI.setAdapter(adapter3);
        //set saved data here from shared
        setDPIOption = sh.getString("getDPIOption", "200");
        {
            int spinnerPosition3 = adapter3.getPosition(setDPIOption);
            spinnerImgDPI.setSelection(spinnerPosition3);
            dpiSelectedText.setText(setDPIOption);
        }
        spinnerImgDPI.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setDPIOption = spinnerImgDPI.getSelectedItem().toString();
                int spinnerPosition3 = adapter3.getPosition(setDPIOption);
                spinnerImgDPI.setSelection(spinnerPosition3);
                dpiSelectedText.setText(setDPIOption);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("KARTHIK", "NO SELECTION:" + setDPIOption);
            }
        });
        LinearLayout dpiLinear = (LinearLayout) dialog.findViewById(R.id.dpiLinear);
        dpiLinear.setOnClickListener((View.OnClickListener) v -> {
            if (spinnerImgDPI.getSelectedItem() != null) {
                spinnerImgDPI.performClick();
            }
        });
        //for crop filter
        Spinner spinnerCrpFilter = dialog.findViewById(R.id.spinner_crpFilter);
        TextView crpFilterSelectedText = dialog.findViewById(R.id.crpFilterSelectedText);
        ArrayAdapter<CharSequence> adapter4 = ArrayAdapter.createFromResource(this, R.array.cropFilter, R.layout.spinner_row);
        adapter4.setDropDownViewResource(R.layout.spinner_row);
        spinnerCrpFilter.setAdapter(adapter4);
        //set saved data here from shared
        setCrpFilterOption = sh.getString("getCropFilterOption", "ENHANCE COLOR");
        {
            int spinnerPosition4 = adapter4.getPosition(setCrpFilterOption);
            spinnerCrpFilter.setSelection(spinnerPosition4);
            crpFilterSelectedText.setText(setCrpFilterOption);
        }
        spinnerCrpFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setCrpFilterOption = spinnerCrpFilter.getSelectedItem().toString();
                int spinnerPosition4 = adapter4.getPosition(setCrpFilterOption);
                if (setCropOption.equals("DISABLE")) {
                    Log.d("KARTHIK", "DON'T ALLOW TO SELECT, IF CROP DISABLED");
                    spinnerCrpFilter.setSelection(0);
                } else {
                    spinnerCrpFilter.setSelection(spinnerPosition4);
                }
                crpFilterSelectedText.setText(setCrpFilterOption);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("KARTHIK", "NO SELECTION:" + setCrpFilterOption);
            }
        });
        LinearLayout cropfilterLinear = (LinearLayout) dialog.findViewById(R.id.cropfilterLinear);
        cropfilterLinear.setOnClickListener((View.OnClickListener) v -> {
            if (spinnerCrpFilter.getSelectedItem() != null) {
                spinnerCrpFilter.performClick();
            }
        });
        //for review mode
        Switch switchCaptureReview = dialog.findViewById(R.id.switch_captureReview);
        //set saved data here from shared
        setReviewOption = sh.getString("getReviewOption", "null");
        switchCaptureReview.setChecked(setReviewOption.equals("ENABLE"));
        switchCaptureReview.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setReviewOption = "ENABLE";
            } else {
                setReviewOption = "DISABLE";
            }
        });
        //for color mode
        Spinner spinnerColorMode = dialog.findViewById(R.id.spinner_colorMode);
        TextView colorModeSelectedText = dialog.findViewById(R.id.colorModeSelectedText);
        ArrayAdapter<CharSequence> adapter5 = ArrayAdapter.createFromResource(this, R.array.colorMode, R.layout.spinner_row);
        adapter5.setDropDownViewResource(R.layout.spinner_row);
        spinnerColorMode.setAdapter(adapter5);
        //set saved data here from shared
        setColorOption = sh.getString("getColorOption", "RGB");
        {
            int spinnerPosition5 = adapter5.getPosition(setColorOption);
            spinnerColorMode.setSelection(spinnerPosition5);
            colorModeSelectedText.setText(setColorOption);
        }
        spinnerColorMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setColorOption = spinnerColorMode.getSelectedItem().toString();
                int spinnerPosition5 = adapter5.getPosition(setColorOption);
                spinnerColorMode.setSelection(spinnerPosition5);
                colorModeSelectedText.setText(setColorOption);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("KARTHIK", "NO SELECTION:" + setDPIOption);
            }
        });
        LinearLayout colorLinear = (LinearLayout) dialog.findViewById(R.id.colorLinear);
        colorLinear.setOnClickListener((View.OnClickListener) v -> {
            if (spinnerColorMode.getSelectedItem() != null) {
                spinnerColorMode.performClick();
            }
        });

        //for auto capture toggle type
        Spinner spinneracToggleMode = dialog.findViewById(R.id.spinner_acToggleMode);
        TextView acToggleModeSelectedText = dialog.findViewById(R.id.acToggleModeSelectedText);
        ArrayAdapter<CharSequence> adapter6 = ArrayAdapter.createFromResource(this, R.array.acToggleMode, R.layout.spinner_row);
        adapter6.setDropDownViewResource(R.layout.spinner_row);
        spinneracToggleMode.setAdapter(adapter6);
        //set saved data here from shared
        setacToggleOption = sh.getString("getAutoCaptureToggle", "DISABLED");
        {
            int spinnerPosition6 = adapter6.getPosition(setacToggleOption);
            spinneracToggleMode.setSelection(spinnerPosition6);
            acToggleModeSelectedText.setText(setacToggleOption);
        }
        spinneracToggleMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setacToggleOption = spinneracToggleMode.getSelectedItem().toString();
                int spinnerPosition6 = adapter6.getPosition(setacToggleOption);
                spinneracToggleMode.setSelection(spinnerPosition6);
                acToggleModeSelectedText.setText(setacToggleOption);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("KARTHIK", "NO SELECTION:" + setacToggleOption);
            }
        });
        LinearLayout acToggleLinear = (LinearLayout) dialog.findViewById(R.id.acToggle_Linear);
        acToggleLinear.setOnClickListener((View.OnClickListener) v -> {
            if (spinneracToggleMode.getSelectedItem() != null) {
                spinneracToggleMode.performClick();
            }
        });

        //for doc detect sensitivity
        Spinner spinnerSensitivity = dialog.findViewById(R.id.spinner_sensitivityMode);
        TextView sensitivityModeSelectedText = dialog.findViewById(R.id.sensitivityModeSelectedText);
        ArrayAdapter<CharSequence> adapter8 = ArrayAdapter.createFromResource(this, R.array.sensitivityMode, R.layout.spinner_row);
        adapter8.setDropDownViewResource(R.layout.spinner_row);
        spinnerSensitivity.setAdapter(adapter8);
        //set saved data here from shared
        setSensitivityOption = sh.getString("getDocDetectSensitivity", "SENS 50");
        {
            int spinnerPosition8 = adapter8.getPosition(setSensitivityOption);
            spinnerSensitivity.setSelection(spinnerPosition8);
            sensitivityModeSelectedText.setText(setSensitivityOption);
        }
        spinnerSensitivity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setSensitivityOption = spinnerSensitivity.getSelectedItem().toString();
                int spinnerPosition8 = adapter8.getPosition(setSensitivityOption);
                spinnerSensitivity.setSelection(spinnerPosition8);
                sensitivityModeSelectedText.setText(setSensitivityOption);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("KARTHIK", "NO SELECTION:" + setSensitivityOption);
            }
        });
        LinearLayout sensitivityLinear = (LinearLayout) dialog.findViewById(R.id.sensitivityLinear);
        sensitivityLinear.setOnClickListener((View.OnClickListener) v -> {
            if (spinnerSensitivity.getSelectedItem() != null) {
                spinnerSensitivity.performClick();
            }
        });

        //for profile mode
        Switch switchCaptureProfile = dialog.findViewById(R.id.switch_profileMode);
        //set saved data here from shared
        setProfileOption = sh.getString("getProfileOption", "NORMAL");
        switchCaptureProfile.setChecked(!setProfileOption.equals("NORMAL"));
        switchCaptureProfile.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setProfileOption = "ID CARD";
            } else {
                setProfileOption = "NORMAL";
            }
        });
        //for crop enable
        Spinner spinnerCrop = dialog.findViewById(R.id.spinner_cropMode);
        TextView cropModeSelectedText = dialog.findViewById(R.id.cropModeSelectedText);
        ArrayAdapter<CharSequence> adapter7 = ArrayAdapter.createFromResource(this, R.array.cropEnable, R.layout.spinner_row);
        adapter7.setDropDownViewResource(R.layout.spinner_row);
        spinnerCrop.setAdapter(adapter7);
        //set saved data here from shared
        setCropOption = sh.getString("getCropOption", "ASSISTED CAPTURE");
        {
            int spinnerPosition7 = adapter7.getPosition(setCropOption);
            spinnerCrop.setSelection(spinnerPosition7);
            cropModeSelectedText.setText(setCropOption);
        }
        spinnerCrop.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setCropOption = spinnerCrop.getSelectedItem().toString();
                int spinnerPosition7 = adapter7.getPosition(setCropOption);
                if (setCropOption.equals("DISABLE")) {
                    Log.d("KARTHIK", "DON'T ALLOW TO SELECT, IF CROP DISABLED");
                    spinnerCrop.setSelection(0);
                    spinnerCrpFilter.setSelection(0);
                    setCrpFilterOption = spinnerCrpFilter.getSelectedItem().toString();
                    crpFilterSelectedText.setText(setCrpFilterOption);
                    cropModeSelectedText.setText(setCropOption);
                } else {
                    spinnerCrop.setSelection(spinnerPosition7);
                    cropModeSelectedText.setText(setCropOption);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("KARTHIK", "NO SELECTION:" + setCropOption);
            }
        });
        LinearLayout cropLinear = (LinearLayout) dialog.findViewById(R.id.cropLinear);
        cropLinear.setOnClickListener((View.OnClickListener) v -> {
            if (spinnerCrop.getSelectedItem() != null) {
                spinnerCrop.performClick();
            }
        });
        //for toggle enable
        Spinner spinnerToggle = dialog.findViewById(R.id.spinner_toggleMode);
        TextView toggleSelectedText = dialog.findViewById(R.id.toggleSelectedText);
        ArrayAdapter<CharSequence> adapter9 = ArrayAdapter.createFromResource(this, R.array.cameraToggleEnable, R.layout.spinner_row);
        adapter9.setDropDownViewResource(R.layout.spinner_row);
        spinnerToggle.setAdapter(adapter9);
        //set saved data here from shared
        setToggleOption = sh.getString("getToggleOption", "ENABLE BACK DEFAULT");
        {
            int spinnerPosition9 = adapter9.getPosition(setToggleOption);
            spinnerToggle.setSelection(spinnerPosition9);
            toggleSelectedText.setText(setToggleOption);
        }
        spinnerToggle.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setToggleOption = spinnerToggle.getSelectedItem().toString();
                int spinnerPosition9 = adapter9.getPosition(setToggleOption);
                spinnerToggle.setSelection(spinnerPosition9);
                toggleSelectedText.setText(setToggleOption);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("KARTHIK", "NO SELECTION:" + setToggleOption);
            }
        });
        LinearLayout toggleLinear = (LinearLayout) dialog.findViewById(R.id.toggleLinear);
        toggleLinear.setOnClickListener((View.OnClickListener) v -> {
            if (spinnerToggle.getSelectedItem() != null) {
                spinnerToggle.performClick();
            }
        });
        //for flash enable
        Switch switchFlash = dialog.findViewById(R.id.switch_flashMode);
        //set saved data here from shared
        setFlashOption = sh.getString("getFlashOption", "ENABLE");
        switchFlash.setChecked(setFlashOption.equals("ENABLE"));
        switchFlash.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setFlashOption = "ENABLE";
            } else {
                setFlashOption = "DISABLE";
            }
        });
        //for sound enable
        Switch switchSound = dialog.findViewById(R.id.switch_soundMode);
        //set saved data here from shared
        setSoundOption = sh.getString("getSoundOption", "DISABLE");
        switchSound.setChecked(setSoundOption.equals("ENABLE"));
        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setSoundOption = "ENABLE";
            } else {
                setSoundOption = "DISABLE";
            }
        });
        //for show count
        Switch switchShowCount = dialog.findViewById(R.id.switch_captureCount);
        //set saved data here from shared
        setShowCountOption = sh.getString("getShowCountOption", "ENABLE");
        switchShowCount.setChecked(setShowCountOption.equals("ENABLE"));
        switchShowCount.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setShowCountOption = "ENABLE";
            } else {
                setShowCountOption = "DISABLE";
            }
        });

        //set saved data here from shared
        Spinner spinnerMaxPages = dialog.findViewById(R.id.spinner_setMaxPages);
        TextView maxpageSelectedText = dialog.findViewById(R.id.maxpageSelectedText);
        ArrayAdapter<CharSequence> adapter10 = ArrayAdapter.createFromResource(this, R.array.maxPages, R.layout.spinner_row);
        adapter10.setDropDownViewResource(R.layout.spinner_row);
        spinnerMaxPages.setAdapter(adapter10);
        //set saved data here from shared
        setMaxPages = sh.getString("getMaxPages", "UNLIMITED PAGES");
        {
            int spinnerPosition10 = adapter10.getPosition(setMaxPages);
            spinnerMaxPages.setSelection(spinnerPosition10);
            maxpageSelectedText.setText(setMaxPages);
        }
        spinnerMaxPages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setMaxPages = spinnerMaxPages.getSelectedItem().toString();
                int spinnerPosition10 = adapter10.getPosition(setMaxPages);
                spinnerMaxPages.setSelection(spinnerPosition10);
                maxpageSelectedText.setText(setMaxPages);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("KARTHIK", "NO SELECTION:" + setMaxPages);
            }
        });
        LinearLayout maxPageLinear = (LinearLayout) dialog.findViewById(R.id.maxPageLinear);
        maxPageLinear.setOnClickListener((View.OnClickListener) v -> {
            if (spinnerMaxPages.getSelectedItem() != null) {
                spinnerMaxPages.performClick();
            }
        });
        //for image resize mode
        Spinner spinnerImgResizeMode = dialog.findViewById(R.id.spinner_imgResizeMode);
        TextView resizeModeSelectedText = dialog.findViewById(R.id.resizeModeSelectedText);
        ArrayAdapter<CharSequence> adapter11 = ArrayAdapter.createFromResource(this, R.array.imgResizeMode, R.layout.spinner_row);
        adapter11.setDropDownViewResource(R.layout.spinner_row);
        spinnerImgResizeMode.setAdapter(adapter11);
        AtomicBoolean isClicked = new AtomicBoolean(false);
        //set saved data here from shared
        setResizeModeOption = sh.getString("getResizeMode", "PRESERVE ASPECT ONLY");
        {
            int spinnerPosition11 = adapter11.getPosition(setResizeModeOption);
            spinnerImgResizeMode.setSelection(spinnerPosition11);
            resizeModeSelectedText.setText(setResizeModeOption);
        }
        spinnerImgResizeMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setResizeModeOption = spinnerImgResizeMode.getSelectedItem().toString();
                if(setResizeModeOption.equals("STRETCH TO EXACT SIZE") && isClicked.get()) {
                    showDimensionInputDialog();
                }
                int spinnerPosition11 = adapter2.getPosition(setResizeModeOption);
                spinnerImgResizeMode.setSelection(spinnerPosition11);
                resizeModeSelectedText.setText(setResizeModeOption);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.d("KARTHIK", "NO SELECTION:" + setResizeModeOption);
            }
        });
        LinearLayout resizeModeLinear = (LinearLayout) dialog.findViewById(R.id.resizeModeLinear);
        resizeModeLinear.setOnClickListener((View.OnClickListener) v -> {
            if (spinnerImgResizeMode.getSelectedItem() != null) {
                isClicked.set(true);
                spinnerImgResizeMode.performClick();
            }
        });

        //save all trigger set camera config
        save.setOnClickListener(v -> {
            //Taking data from input to global variable then saving in shared preferences
            SharedPreferences sharedPreferences = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
            SharedPreferences.Editor myEdit = sharedPreferences.edit();
            myEdit.putString("getCameraOption", setCameraOption);
            myEdit.putString("getCaptureMode", setCaptureMode);
            myEdit.putString("getLayoutOption", setLayoutOption);
            myEdit.putString("getQualityOption", setQualityOption);
            myEdit.putString("getResizeMode", setResizeModeOption);
            myEdit.putString("getResizeModeValues", setResizeModeValues);
            myEdit.putString("getDPIOption", setDPIOption);
            myEdit.putString("getReviewOption", setReviewOption);
            myEdit.putString("getCropFilterOption", setCrpFilterOption);
            myEdit.putString("getColorOption", setColorOption);
            myEdit.putString("getDocDetectSensitivity", setSensitivityOption);
            myEdit.putString("getAutoCaptureToggle", setacToggleOption);
            myEdit.putString("getProfileOption", setProfileOption);
            myEdit.putString("getCropOption", setCropOption);
            myEdit.putString("getFlashOption", setFlashOption);
            myEdit.putString("getToggleOption", setToggleOption);
            myEdit.putString("getSoundOption", setSoundOption);
            myEdit.putString("getShowCountOption", setShowCountOption);
            myEdit.putString("getMaxPages", setMaxPages);
            myEdit.putString("enableDisableAddToGallery", String.valueOf(enableDisableAddImagesToGallery));
            myEdit.putString("enableDisableOutputDir", String.valueOf(enableDisableOutputDir));
            if (Config.Common.SDKInfo.isEmpty()) {
                myEdit.putString("sdkInfo", sh.getString("sdkInfo", ""));
            } else {
                myEdit.putString("sdkInfo", Config.Common.SDKInfo.trim());
            }
            myEdit.putString("userChanges", "true");
            myEdit.apply();

            dialog.dismiss();
            Toast.makeText(MainActivity.this, "SAVE: SUCCESS", Toast.LENGTH_SHORT).show();
        });
        //to close the popup
        cancel.setOnClickListener(v -> {
//            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//            builder.setMessage("Are you sure to exit without saving settings?");
//            builder.setTitle("Save Settings");
//            builder.setCancelable(false);
//            builder.setNegativeButton("Exit", (dialog2, which) -> {
//                dialog2.cancel();
//                dialog.dismiss();
//            });
//            builder.setPositiveButton("Save", (dialog2, which) -> {
            save.performClick();
            dialog.dismiss();
//            });
//            AlertDialog alertDialog1 = builder.create();
//            alertDialog1.show();
        });
        dialog.setOnKeyListener((dialog1, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
//                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                builder.setMessage("Are you sure to exit without saving settings?");
//                builder.setTitle("Save Settings");
//                builder.setCancelable(false);
//                builder.setNegativeButton("Exit", (dialog2, which) -> {
//                    dialog2.cancel();
//                    dialog1.dismiss();
//                });
//                builder.setPositiveButton("Save", (dialog2, which) -> {
                save.performClick();
                //dialog2.cancel();
                dialog1.dismiss();
//                });
//                AlertDialog alertDialog1 = builder.create();
//                alertDialog1.show();
            }
            return true;
        });
        dialog.show();
    }

    private void defaultInputForConfig() {
        //Taking data from input to global variable then saving in shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sharedPreferences.edit();
        myEdit.putString("getCameraOption", "APP");
        myEdit.putString("getCaptureMode", "DEFAULT");
        myEdit.putString("getLayoutOption", "A4");
        myEdit.putString("getQualityOption", "DOCUMENT QUALITY");
        myEdit.putString("getResizeMode", "PRESERVE ASPECT ONLY");
        myEdit.putString("getResizeModeValues", "200,200");
        myEdit.putString("getDPIOption", "200");
        myEdit.putString("getReviewOption", "ENABLE");
        myEdit.putString("getCropFilterOption", "NONE");
        myEdit.putString("getColorOption", "RGB");
        myEdit.putString("getDocDetectSensitivity", "SENS 50");
        myEdit.putString("getAutoCaptureToggle", "DISABLED");
        myEdit.putString("getProfileOption", "NORMAL");
        myEdit.putString("getCropOption", "ASSISTED CAPTURE");
        myEdit.putString("getFlashOption", "ENABLE");
        myEdit.putString("getToggleOption", "ENABLE BACK DEFAULT");
        myEdit.putString("getSoundOption", "DISABLE");
        myEdit.putString("getShowCountOption", "ENABLE");
        myEdit.putString("getMaxPages", "UNLIMITED PAGES");
        myEdit.putString("enableDisableOutputDir", "false");
        if (Config.Common.SDKInfo.isEmpty()) {
            myEdit.putString("sdkInfo", sharedPreferences.getString("sdkInfo", ""));
        } else {
            myEdit.putString("sdkInfo", Config.Common.SDKInfo.trim());
        }
        myEdit.putString("userChanges", "false");
        myEdit.apply();
    }

    private void openDownloadFolder() {
        String checkFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/QuickCapture/";
        File f = new File(checkFolder);
        if (f.isDirectory()) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/QuickCapture/");
            intent.setDataAndType(uri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } else {
            Toast.makeText(this, "No files found", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isInputValid(EditText input, int min, int max) {
        try {
            String inputValue = input.getText().toString().trim();
            if (inputValue.isEmpty()) {
                input.setError("This field is required.");
                return false;
            }

            int value = Integer.parseInt(inputValue);
            if (value < min || value > max) {
                input.setError("Value must be between " + min + " and " + max);
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            input.setError("Please enter a valid number.");
            return false;
        }
    }

    private void showDimensionInputDialog() {
        SharedPreferences sharedPreferences = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sharedPreferences.edit();
        // Create a vertical linear layout to hold the input fields
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 10, 50, 10);

        // Create the EditText for width input
        final EditText widthInput = new EditText(this);
        widthInput.setHint("Enter Width in pixels");
        widthInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(widthInput);

        // Create the EditText for height input
        final EditText heightInput = new EditText(this);
        heightInput.setHint("Enter Height in pixels");
        heightInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(heightInput);
        // Create the AlertDialog for asking input
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Custom Page Layout");
        builder.setMessage("Please enter the Width and Height in pixels.");
        builder.setView(layout);
        builder.setCancelable(false);
        builder.setPositiveButton("Ok", ((dialog, which) -> {
            boolean isWidthValid = isInputValid(widthInput, 20, 4000);
            boolean isHeightValid = isInputValid(heightInput, 20, 4000);
            if (isWidthValid && isHeightValid) {
                // Retrieve inputs for width and height
                int width = Integer.parseInt(widthInput.getText().toString());
                int height = Integer.parseInt(heightInput.getText().toString());
                setResizeModeValues =  width+","+height;
                myEdit.putString("getResizeMode", "STRETCH TO EXACT SIZE");
                myEdit.putString("getResizeModeValues", setResizeModeValues);
                myEdit.apply();
                dialog.dismiss();
            } else {
                Toast.makeText(MainActivity.this, "Please enter valid pixel values!", Toast.LENGTH_SHORT).show();
            }
        }));
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            myEdit.putString("getResizeMode", "PRESERVE ASPECT ONLY");
            myEdit.putString("getResizeModeValues", "200,200");
            myEdit.apply();
            dialog.dismiss();
        });
        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void doTestProcess(String imagePath) {
        try {
            // Activate SDK, before initiating AiDocument class
            com.extrieve.splicer.aisdk.Config.License.Activate(this, licStringAadhaar);
            AiDocument aadhaarDoc1 = new AiDocument(this, imagePath);
            Log.d(TAG, "Document initialised successfully");
            Log.d(TAG, "ID: " + aadhaarDoc1.ID);
            Log.d(TAG, "TYPE: " + aadhaarDoc1.TYPE);
            Log.d(TAG, "FILE_PATH: " + aadhaarDoc1.FILE_PATH);

            aadhaarDoc1.KYCDetect(response -> {
                if (response == null) {
                    Log.d(TAG, "Response is null");
                    return;
                }
                try {
                    JSONObject data = new JSONObject(response);
                    String PREDICTED_DOCS = data.getString("PREDICTED_DOCS");
                    Log.d(TAG, "KYCDetect_PREDICTED_DOCS: " + PREDICTED_DOCS);
                } catch (JSONException e) {
                    Log.d("Error", "Runtime error in KYCDetect: "+ e);
                }
                Log.d(TAG, "KYCDetect_RESULT_DATA: " + response);
            });

            aadhaarDoc1.KYCExtract(response -> {
                if (response == null) {
                    Log.d(TAG, "Response is null");
                    return;
                }
                try {
                    JSONObject data = new JSONObject(response);
                    String PREDICTED_DOCS = data.getString("PREDICTED_DOCS");
                    Log.d(TAG, "KYCExtract_PREDICTED_DOCS: " + PREDICTED_DOCS);
                } catch (JSONException e) {
                    Log.d("Error", "Runtime error in KYCExtract: "+ e);
                }
                Log.d(TAG, "KYCExtract_RESULT_DATA: " + response);
            });

            ArrayList<String> listOfDoc = aadhaarDoc1.GetKYCDocList();
            Log.d(TAG, "GetKYCDocList: " + listOfDoc);

            // if same document contain multiple docs images... how verification need to respond
            aadhaarDoc1.KYCVerify("Aadhaar", response -> {
                if (response == null) {
                    Log.d(TAG, "Response is null");
                    return;
                }
                try {
                    JSONObject data = new JSONObject(response);
                    String DESCRIPTION = data.getString("DESCRIPTION");
                    Log.d(TAG, "KYCVerify_DESCRIPTION: " + DESCRIPTION);
                } catch (JSONException e) {
                    Log.d("Error", "Runtime error in KYCVerify: "+ e);
                }
                Log.d(TAG, "KYCVerify_RESULT_DATA: " + response);
            });

            //Intent maskIntent = new Intent(hostAppContext, AadhaarMask.class);
            Intent maskIntent = null;
            try {
                // Intent maskIntent = new Intent(hostAppContext, AadhaarMask.class);
                maskIntent = new Intent(this, Class.forName("com.extrieve.splicer.aisdk.AadhaarMask"));
                //maskIntent.putExtra("BitmapImagePath", imagePath);
                maskIntent.putExtra("DOCUMENT_ID", aadhaarDoc1.ID);
                //maskIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                com.extrieve.splicer.aisdk.Config.AadhaarMasking.EXTRACT_AADHAAR_NUMBER = true;
                aadhaarActivityResultLauncher.launch(maskIntent);
            } catch (Exception e) {
                Log.d("Error", "Runtime error in AadhaarMask: "+ e);
            }

        } catch (Exception e) {
            Log.d("Error", "Runtime error in doTestProcess: "+ e);
            Toast.makeText(this, "Failed to create document object :" + e, Toast.LENGTH_LONG).show();
        }
    }

    private String saveImage(Bitmap finalBitmap) {
        removeFilesInSplicerStorage();
        String root;
        if (type.equals("KycDocument") || type.equals("KycDocumentUseCase")) {
            root = BuildSplicerStoragePath();
        } else {
            root = BuildStoragePath();
        }
        File myDir = new File(root);
        myDir.mkdirs();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fName = "Splicer_" + timeStamp + ".jpg";
        File file = new File(myDir, fName);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fName);
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            Log.d("Error", "Runtime error in saveImage: "+ e);
        }
        return String.valueOf(file);
    }

    private void uploadFile() throws IOException {
        String url = "http://example.com/upload";
        String charset = "UTF-8";
        String param = "value";
        File textFile = new File("/path/to/file.txt");
        File binaryFile = new File("/path/to/file.bin");
        String boundary = Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.

        URLConnection connection = new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream output = connection.getOutputStream(); PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);) {
            // Send normal param.
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"param\"").append(CRLF);
            writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
            writer.append(CRLF).append(param).append(CRLF).flush();

            // Send text file.
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"textFile\"; filename=\"" + textFile.getName() + "\"").append(CRLF);
            writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF); // Text file itself must be saved in this charset!
            writer.append(CRLF).flush();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.copy(textFile.toPath(), output);
            }
            output.flush(); // Important before continuing with writer!
            writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

            // Send binary file.
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"binaryFile\"; filename=\"" + binaryFile.getName() + "\"").append(CRLF);
            writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(binaryFile.getName())).append(CRLF);
            writer.append("Content-Transfer-Encoding: binary").append(CRLF);
            writer.append(CRLF).flush();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.copy(binaryFile.toPath(), output);
            }
            output.flush(); // Important before continuing with writer!
            writer.append(CRLF).flush(); // CRLF is important! It indicates end of boundary.

            // End of multipart/form-data.
            writer.append("--" + boundary + "--").append(CRLF).flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Request is lazily fired whenever you need to obtain information about response.
        int responseCode = ((HttpURLConnection) connection).getResponseCode();
        System.out.println(responseCode); // Should be 200
    }

    public void sendPOSTRequest(String url, String authData, String attachmentFilePath, String outputFilePathName) {
        String charset = "UTF-8";
        File binaryFile = new File(attachmentFilePath);
        String boundary = "------------------------" + Long.toHexString(System.currentTimeMillis()); // Just generate some unique random value.
        String CRLF = "\r\n"; // Line separator required by multipart/form-data.
        int responseCode = 0;

        try {
            //Set POST general headers along with the boundary string (the seperator string of each part)
            URLConnection connection = new URL(url).openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.addRequestProperty("User-Agent", "CheckpaySrv/1.0.0");
            connection.addRequestProperty("Accept", "*/*");
            connection.addRequestProperty("Authentication", authData);

            OutputStream output = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);

            // Send binary file - part
            // Part header
            writer.append("--" + boundary).append(CRLF);
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + binaryFile.getName() + "\"").append(CRLF);
            writer.append("Content-Type: application/octet-stream").append(CRLF);// + URLConnection.guessContentTypeFromName(binaryFile.getName())).append(CRLF);
            writer.append(CRLF).flush();

            // File data
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.copy(binaryFile.toPath(), output);
            }
            output.flush();

            // End of multipart/form-data.
            writer.append(CRLF).append("--" + boundary + "--").flush();

            responseCode = ((HttpURLConnection) connection).getResponseCode();


            if (responseCode != 200) //We operate only on HTTP code 200
                return;

            InputStream Instream = ((HttpURLConnection) connection).getInputStream();

            // Write PDF file
            BufferedInputStream BISin = new BufferedInputStream(Instream);
            FileOutputStream FOSfile = new FileOutputStream(outputFilePathName);
            BufferedOutputStream out = new BufferedOutputStream(FOSfile);

            int i;
            while ((i = BISin.read()) != -1) {
                out.write(i);
            }

            // Cleanup
            out.flush();
            out.close();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}