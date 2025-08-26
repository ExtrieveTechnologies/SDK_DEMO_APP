package com.extrieve.exScan;

import static android.content.ContentValues.TAG;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.extrieve.exScan.R.color;
import com.extrieve.quickcapture.sdk.Config;
import com.extrieve.quickcapture.sdk.HumanFaceHelper;
import com.extrieve.quickcapture.sdk.HumanFaceHelper.ResponseCallback;
import com.extrieve.quickcapture.sdk.ImgHelper;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FaceIdentifyAndMatcher extends AppCompatActivity {

    LinearLayout bodyLinear, addDoc;
    Button matchFace;
    Snackbar snackbar, snackbar1, snackbar2;
    ImageView refresh;
    Bitmap currentImageBitmap;
    HumanFaceHelper humanFaceHelper;
    ImgHelper ImageHelper;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String TAP_TARGET_SHOWN = "tap_target_shown";
    List<Map<String, Object>> docCollection = new ArrayList<>();
    private final List<ImageView> imageViews = new ArrayList<>();
    private int firstSelectedIndex = -1;
    private int secondSelectedIndex = -1;
    private long firstSelectedDocId = -1;
    private long secondSelectedDocId = -1;
    private int firstSelectedDocIdx = -1;
    private int secondSelectedDocIdx = -1;
    private static final int PICK_IMAGE = 1000;
    private final int REQUEST_CODE_PERMISSIONS = 1001;
    public final int REQUEST_CODE_FILE_RETURN = 1002;
    private boolean permissionStatus = false;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private final String[] REQUIRED_PERMISSIONS_ABOVE13 = new String[]{"android.permission.CAMERA", "android.permission.READ_MEDIA_IMAGES"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.facematch);
        CheckAndPromptPermissions();

        try {
            SharedPreferences prefs = getSharedPreferences("LIC_SETTING", MODE_PRIVATE);
            String licenseStringFaceMatch = prefs.getString("LIC_QC_DATA", "");
            boolean validLicense = Config.License.Activate(this, licenseStringFaceMatch);
            humanFaceHelper = new HumanFaceHelper(this);
            if(!validLicense) {
                Toast.makeText(this, "License activation failed", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                return;
            }
        } catch (Exception e) {
            Toast.makeText(this, "License activation failed", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            return;
        }
        ImageHelper = new ImgHelper(this);
        bodyLinear = findViewById(R.id.body);
        addDoc = findViewById(R.id.addDoc);
        matchFace = findViewById(R.id.matchFace);
        refresh = findViewById(R.id.refresh);

        addDoc.setOnClickListener(v -> addNewDocumentInList());
        matchFace.setOnClickListener(v -> matchFaceWithId());
        refresh.setOnClickListener(v -> refreshOrResetUI(1));

        //show initially face recognise logo
        buildDocumentDisplayUI(1);
    }

    /* IMAGE ADDITION, DELETION, FACE DETECTION & MATCHING RELATED PART */
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

    private void OpenCameraActivity() {
        //before starting camera - configuration can set
        //String quality = ImageHelper.getCurrentImageQuality();
        try {
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

    private void checkCaptureType(String captureType, String buildType, Boolean alertOnResponse) {
        if (Objects.equals(captureType, "camera")) {
            if (!allPermissionsGranted()) {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
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
            Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            gallery.setType("image/*");
            startActivityForResult(gallery, PICK_IMAGE);
        }
    }

    private void addNewDocumentInList() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Select face capture type");
        builder.setTitle("Face Capture");
        builder.setCancelable(true);
        builder.setPositiveButton("Attach", (dialog, which) -> checkCaptureType("galleryOne", null, false));
        builder.setNegativeButton("Capture", (dialog, which) -> checkCaptureType("camera", null, false));
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteDocumentInList(int deId) {
        int documentCollectionIndex = deId - 1000;
        docCollection.remove(documentCollectionIndex);
        refreshOrResetUI(0);
        snackbar = Snackbar.make(bodyLinear, "Document deleted from list", Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private void matchFaceWithId() {
        if (firstSelectedDocId == -1 || secondSelectedDocId == -1 || firstSelectedDocIdx == -1 || secondSelectedDocIdx == -1) {
            Toast.makeText(this, "Select 2 faces to match", Toast.LENGTH_SHORT).show();
            return;
        }
        matchFace.setVisibility(View.INVISIBLE);

        humanFaceHelper.matchHumanFaces(firstSelectedDocId, firstSelectedDocIdx, secondSelectedDocId, secondSelectedDocIdx, new HumanFaceHelper.ResponseCallback() {
            @Override
            public void onCompleted(String response) {
                handleFaceMatchingResponse(response); // Call the separate method
                matchFace.setVisibility(View.VISIBLE); // Restore visibility
            }

            @Override
            public void onFailed(String failedReason) {
                Log.e(TAG, "Error occurred in face matching");
                matchFace.setVisibility(View.VISIBLE); // Restore visibility
            }
        });
    }

    private void handleFaceMatchingResponse(String response) {
        if (response == null || response.isEmpty()) {
            Log.d(TAG, "Response is null or empty");
            return;
        }

        try {
            JSONObject jsonData = new JSONObject(response);

            // Extract and format accuracy data
            String acu = jsonData.getString("ACCURACY");
            double accuracyValue = Double.parseDouble(acu);
            int accuracy = (int) Math.round(accuracyValue);

            // Get accuracy level string
            String accuracyLevel = getAccuracyLevel(accuracy);
            //String msg = accuracyLevel;
            // String msg = "Faces with " + accuracyLevel + " (" + accuracy + "%)";
            // Display the accuracy level with a Snackbar
            snackbar1 = Snackbar.make(bodyLinear, accuracyLevel, Snackbar.LENGTH_INDEFINITE);
            snackbar1.setAction("Ok", v1 -> snackbar1.dismiss());
            snackbar1.show();
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON response: " + response, e);
        }
    }

    private void refreshOrResetUI(int type) {
        RotateAnimation rotateAnimation = new RotateAnimation(0F, 360F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(1000);
        rotateAnimation.setRepeatCount(1);
        refresh.startAnimation(rotateAnimation);

        buildDocumentDisplayUI(2);
        if (type == 1) {
            snackbar = Snackbar.make(bodyLinear, "Refreshed document list", Snackbar.LENGTH_LONG);
            snackbar.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                currentImageBitmap = loadBitmapFromUri(selectedFileUri);
                if (requestCode == 1000) {
                    if (currentImageBitmap != null) {
                        humanFaceHelper.detectHumanFaces(currentImageBitmap, new ResponseCallback() {
                            @Override
                            public void onCompleted(String response) {
                                onDetectHumanFacesCompleted(response);
                            }

                            @Override
                            public void onFailed(String failedReason) {
                                Log.e(TAG, "Error occurred in face detection");
                                //Toast.makeText(this, "Invalid bitmap", Toast.LENGTH_SHORT).show();
                                matchFace.setVisibility(View.VISIBLE); // Restore visibility
                            }
                        });
                    } else {
                        Toast.makeText(this, "Invalid bitmap", Toast.LENGTH_SHORT).show();
                        finishActivity(PICK_IMAGE);
                    }
                }
            }
        }
        //for camera append
        else if (requestCode == REQUEST_CODE_FILE_RETURN && resultCode == RESULT_OK && data != null) {
            Boolean Status = (Boolean) data.getExtras().get("STATUS");
            String Description = (String) data.getExtras().get("DESCRIPTION");
            if (Boolean.FALSE.equals(Status)) {
                String imageCaptureLog = "Description : " + Description + ".Exception: " + Config.CaptureSupport.LastLogInfo;
                Log.d("INFO", imageCaptureLog);
                finishActivity(REQUEST_CODE_FILE_RETURN);
                return;
            }
            ArrayList<String> FileCollection = (ArrayList<String>) data.getExtras().get("fileCollection");
            if (FileCollection == null || FileCollection.isEmpty()) {
                //clear all selection if other options clicked
            } else {
                String filePath = FileCollection.get(0);
                File file = new File(filePath);
                Uri fileUri = Uri.fromFile(file);  // gives file://... URI
                currentImageBitmap = loadBitmapFromUri(fileUri);
                if (requestCode == 1002) {
                    if (currentImageBitmap != null) {
                        humanFaceHelper.detectHumanFaces(currentImageBitmap, new ResponseCallback() {
                            @Override
                            public void onCompleted(String response) {
                                onDetectHumanFacesCompleted(response);
                            }

                            @Override
                            public void onFailed(String failedReason) {
                                Log.e(TAG, "Error occurred in face detection");
                                //Toast.makeText(this, "Invalid bitmap", Toast.LENGTH_SHORT).show();
                                matchFace.setVisibility(View.VISIBLE); // Restore visibility
                            }
                        });
                    } else {
                        Toast.makeText(this, "Invalid bitmap", Toast.LENGTH_SHORT).show();
                        finishActivity(REQUEST_CODE_FILE_RETURN);
                    }
                }
            }
        }
    }

    private void onDetectHumanFacesCompleted(String response) {
        int photoIdentifiedDataLen = 0;
        JSONArray photoIdentifiedData = null;
        if (response == null) {
            Log.d(TAG, "Response is null");
            return;
        }
        try {
            JSONObject jsonData = new JSONObject(response);
            String STATUS = jsonData.getString("STATUS");
            if (STATUS.equals("true")) {
                photoIdentifiedData = jsonData.getJSONArray("DATA");
                photoIdentifiedDataLen = photoIdentifiedData.length();
            }
            Map<String, Object> docData = new HashMap<>();
            List<Map<String, Object>> faceDataList = new ArrayList<>();

            docData.put("DOC_IMAGE", currentImageBitmap);
            docData.put("DOC_IDENTIFIER", jsonData.getString("IDENTIFIER"));
            docData.put("FACE_COUNT", photoIdentifiedDataLen);
            docData.put("DOC_ME_ID", View.generateViewId());
            docData.put("DOC_FL_ID", View.generateViewId());
            docData.put("DOC_SL_ID", View.generateViewId());
            docData.put("DOC_EX_ID", View.generateViewId());
            docData.put("DOC_CL_ID", View.generateViewId());

            for (int i = 0; i < photoIdentifiedDataLen; i++) {
                JSONObject object = photoIdentifiedData.getJSONObject(i);

                Map<String, Object> cropData = new HashMap<>();
                cropData.put("DOC_EL_ID", View.generateViewId());
                cropData.put("INDEX", object.getInt("INDEX"));
                cropData.put("LEFT", object.getInt("LEFT"));
                cropData.put("RIGHT", object.getInt("RIGHT"));
                cropData.put("TOP", object.getInt("TOP"));
                cropData.put("BOTTOM", object.getInt("BOTTOM"));
                Bitmap croppedImg = ImageHelper.cropBitmap(currentImageBitmap, object.getInt("LEFT"), object.getInt("TOP"), object.getInt("RIGHT"), object.getInt("BOTTOM"));
                cropData.put("FACE_IMAGE", croppedImg);
                faceDataList.add(cropData);
            }
            docData.put("FACE_DATA", faceDataList);
            docCollection.add(docData);

            buildDocumentDisplayUI(2);
        } catch (JSONException e) {
            //Toast.makeText(this, "JSON parse error", Toast.LENGTH_SHORT).show();
        }
        finishActivity(PICK_IMAGE);
    }

    private Bitmap loadBitmapFromUri(Uri uri) {
        try {
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
        } catch (Exception e) {
            return null;
        }
    }

    /* DOCUMENT UI BUILD, SHOW, SELECT RELATED PART */
    private void buildDocumentDisplayUI(int type) {
        // To reset the view & selection each time
        bodyLinear.removeAllViews();
        imageViews.clear();
        matchFace.setVisibility(View.GONE);
        int TotalFaceCounts = 0;
        firstSelectedIndex = secondSelectedIndex = -1;
        firstSelectedDocId = secondSelectedDocId = -1;
        firstSelectedDocIdx = secondSelectedDocIdx = -1;
        // To check & show view accordingly
        if (type == 1) {
            ImageView img1 = new ImageView(this);
             img1.setImageResource(R.drawable.face_recognise_main);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            img1.setLayoutParams(layoutParams);
            bodyLinear.addView(img1);
        } else {
            int docCollectionLen = docCollection.size();
            if (docCollectionLen > 0) {
                for (int i = 0; i < docCollectionLen; i++) {
                    long docId = Long.parseLong((String) docCollection.get(i).get("DOC_IDENTIFIER"));
                    LinearLayout docIdLayout = new LinearLayout(this);
                    docIdLayout.setId((Integer) docCollection.get(i).get("DOC_ME_ID"));
                    docIdLayout.setOrientation(LinearLayout.VERTICAL);
                    docIdLayout.setBackgroundResource(R.drawable.button_bg);
                    docIdLayout.setPadding(dpToPx(5), dpToPx(5), dpToPx(5), dpToPx(5));
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(5, 5, 5, 5);
                    docIdLayout.setLayoutParams(layoutParams);
                    // First inner linear layout
                    LinearLayout firstLinear = new LinearLayout(this);
                    firstLinear.setId((Integer) docCollection.get(i).get("DOC_FL_ID"));
                    firstLinear.setOrientation(LinearLayout.HORIZONTAL);
                    firstLinear.setPadding(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));

                    TextView textView1 = new TextView(this);
                    textView1.setText("Document " + (i + 1));
                    textView1.setTextColor(Color.parseColor("#FF6200EE"));
                    textView1.setTextSize(16);
                    textView1.setGravity(Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params1.weight = 1;
                    textView1.setLayoutParams(params1);

                    ImageView deleteImageView = new ImageView(this);
                    deleteImageView.setId(1000 + i);
                    Drawable iconDrawable = getResources().getDrawable(R.drawable.round_delete_forever_white_24);
                    iconDrawable.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#FF6200EE"), PorterDuff.Mode.SRC_IN));
                    deleteImageView.setImageDrawable(iconDrawable);
                    deleteImageView.setAdjustViewBounds(true);
                    deleteImageView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(25), dpToPx(25)));
                    deleteImageView.setOnClickListener(view -> {
                        int deId = deleteImageView.getId();
                        deleteDocumentInList(deId);
                    });
                    ImageView expandImageView = new ImageView(this);
                    expandImageView.setId((Integer) docCollection.get(i).get("DOC_EX_ID"));
                    expandImageView.setImageResource(R.drawable.expand_white);
                    expandImageView.setAdjustViewBounds(true);
                    expandImageView.setBackgroundColor(Color.parseColor("#FF6200EE"));
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(25), dpToPx(25));
                    params.setMargins(dpToPx(30), 0, 0, 0);
                    expandImageView.setLayoutParams(params);
                    expandImageView.setOnClickListener(view -> {
                        int exId = expandImageView.getId();
                        showHideUI(exId, "EXPAND");
                    });
                    ImageView collapseImageView = new ImageView(this);
                    collapseImageView.setId((Integer) docCollection.get(i).get("DOC_CL_ID"));
                    collapseImageView.setImageResource(R.drawable.collapse_white);
                    collapseImageView.setVisibility(View.GONE);
                    collapseImageView.setAdjustViewBounds(true);
                    collapseImageView.setBackgroundColor(Color.parseColor("#FF6200EE"));
                    collapseImageView.setLayoutParams(params);
                    collapseImageView.setOnClickListener(view -> {
                        int clId = collapseImageView.getId();
                        showHideUI(clId, "COLLAPSE");
                    });
                    firstLinear.addView(textView1);
                    firstLinear.addView(deleteImageView);
                    firstLinear.addView(expandImageView);
                    firstLinear.addView(collapseImageView);
                    // Second inner linear layout
                    LinearLayout secondLinear = new LinearLayout(this);
                    secondLinear.setId((Integer) docCollection.get(i).get("DOC_SL_ID"));
                    secondLinear.setOrientation(LinearLayout.VERTICAL);
                    secondLinear.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10));
                    secondLinear.setVisibility(View.GONE);

                    TextView headerText = new TextView(this);
                    headerText.setText("Face Id: " + docId);
                    headerText.setTextColor(Color.parseColor("#FF6200EE"));
                    headerText.setTextSize(16);
                    headerText.setVisibility(View.GONE);
                    headerText.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                    ImageView inputImage = new ImageView(this);
                    inputImage.setImageBitmap((Bitmap) docCollection.get(i).get("DOC_IMAGE"));
                    LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(dpToPx(200), dpToPx(200));
                    params2.gravity = Gravity.CENTER;
                    inputImage.setLayoutParams(params2);
                    inputImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    inputImage.setAdjustViewBounds(true);

                    TextView faceCountText = new TextView(this);
                    faceCountText.setText("Total Identified Faces: " + docCollection.get(i).get("FACE_COUNT"));
                    faceCountText.setTextColor(Color.parseColor("#FF6200EE"));
                    faceCountText.setTextSize(16);
                    LinearLayout.LayoutParams params4 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                    params4.setMargins(0, 0, 0, 10);
                    faceCountText.setLayoutParams(params4);
                    // Horizontal scroll view
                    HorizontalScrollView horizontalScroll = new HorizontalScrollView(this);
                    horizontalScroll.setScrollbarFadingEnabled(true);
                    horizontalScroll.setHorizontalScrollBarEnabled(true);

                    LinearLayout horizontalScrollLinear = new LinearLayout(this);
                    horizontalScrollLinear.setOrientation(LinearLayout.HORIZONTAL);

                    List faceDataList = (List) docCollection.get(i).get("FACE_DATA");
                    int faceDataListLen = faceDataList.size();
                    for (int j = 0; j < faceDataListLen; j++) {
                        TotalFaceCounts++;
                        int imgIdx = imageViews.size();
                        int id = (int) ((HashMap) faceDataList.get(j)).get("INDEX");
                        ImageView index = new ImageView(this);
                        index.setId((int) ((HashMap) faceDataList.get(j)).get("DOC_EL_ID"));
                        index.setTag(docId + ":" + id + ":" + imgIdx);
                        index.setImageBitmap((Bitmap) ((HashMap) faceDataList.get(j)).get("FACE_IMAGE"));
                        LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(dpToPx(60), dpToPx(60), 1);
                        layoutParams1.setMargins(dpToPx(2), 0, 0, 0);
                        index.setLayoutParams(layoutParams1);
                        index.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        index.setAdjustViewBounds(true);
                        index.setPadding(dpToPx(0), dpToPx(3), dpToPx(0), dpToPx(3));
                        index.setOnClickListener(view -> {
                            handleImageClick((String) view.getTag());
                        });
                        imageViews.add(index);
                        horizontalScrollLinear.addView(index);
                    }
                    horizontalScroll.addView(horizontalScrollLinear);
                    secondLinear.addView(headerText);
                    secondLinear.addView(inputImage);
                    secondLinear.addView(faceCountText);
                    secondLinear.addView(horizontalScroll);
                    // Adding both view to mainLinear, then adding to bodyLinear
                    docIdLayout.addView(firstLinear);
                    docIdLayout.addView(secondLinear);
                    bodyLinear.addView(docIdLayout);

                    if (TotalFaceCounts > 1) {
                        matchFace.setVisibility(View.VISIBLE);
                    }
                    if (i == docCollectionLen - 1) {
                        expandImageView.performClick();
                    }
                }
            } else {
                buildDocumentDisplayUI(1);
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private void handleImageClick(String docWithIndex) {
        String[] parts = docWithIndex.split(":");
        int idx = Integer.parseInt(parts[2]);
        int docIdx = Integer.parseInt(parts[1]);
        long docId = Long.parseLong(parts[0]);
        if (firstSelectedIndex == idx) {
            // Same image clicked again, reset first selection
            firstSelectedIndex = -1;
            firstSelectedDocId = -1;
            firstSelectedDocIdx = -1;
        } else if (secondSelectedIndex == idx) {
            // Same image clicked again, reset second selection
            secondSelectedIndex = -1;
            secondSelectedDocId = -1;
            secondSelectedDocIdx = docIdx;
        } else if (firstSelectedIndex == -1) {
            // No first selection, set as first selection
            firstSelectedIndex = idx;
            firstSelectedDocId = docId;
            firstSelectedDocIdx = docIdx;
        } else if (secondSelectedIndex == -1) {
            // No second selection, set as second selection
            secondSelectedIndex = idx;
            secondSelectedDocId = docId;
            secondSelectedDocIdx = docIdx;
        } else {
            // Both selections are made, alert user
            snackbar2 = Snackbar.make(bodyLinear, "Maximum 2 faces can be selected", Snackbar.LENGTH_LONG);
            snackbar2.show();
        }
        // Update the UI based on the current selections
        updateUI();
    }

    private void updateUI() {
        for (int i = 0; i < imageViews.size(); i++) {
            ImageView imageView = imageViews.get(i);
            imageView.setBackground(null);
            if (i == firstSelectedIndex) {
                addBorderToImageView(imageView, android.R.color.holo_blue_light);
            }
            if (i == secondSelectedIndex) {
                addBorderToImageView(imageView, android.R.color.holo_green_light);
            }
        }
    }

    private void addBorderToImageView(ImageView imageView, int borderColor) {
        int borderWidth = 6;
        GradientDrawable borderDrawable = new GradientDrawable();
        borderDrawable.setColor(0);
        borderDrawable.setStroke(borderWidth, getResources().getColor(borderColor));
        Drawable[] layers = new Drawable[2];
        layers[0] = imageView.getBackground();
        layers[1] = borderDrawable;
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        imageView.setBackground(layerDrawable);
    }

    private String getAccuracyLevel(int matchPercentage) {
        String matchLevel;
        if (matchPercentage >= 85 && matchPercentage <= 100) {
            matchLevel = "STRONG MATCH: High confidence the faces belong to the same person (" + matchPercentage + "%).";
        } else if (matchPercentage >= 70 && matchPercentage < 85) {
            matchLevel = "POSSIBLE MATCH: Faces show similarity but require further validation (" + matchPercentage + "%).";
        } else if (matchPercentage >= 50 && matchPercentage < 70) {
            matchLevel = "LOW CONFIDENCE MATCH: Faces might share minor features; not reliable for a match (" + matchPercentage + "%).";
        } else if (matchPercentage >= 0 && matchPercentage < 50) { // Fixed the missing range
            matchLevel = "NO MATCH: Faces are highly unlikely to be the same (" + matchPercentage + "%).";
        } else {
            matchLevel = "INVALID MATCH: Input percentage is out of range.";
        }
        return matchLevel;
    }

    private boolean isTapTargetViewShown() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(TAP_TARGET_SHOWN, false);
    }

    private void setTapTargetViewShown() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(TAP_TARGET_SHOWN, true);
        editor.apply();
    }

    private void showHideUI(int eleId, String type) {
        View targetView = null;
        for (int k = 0; k < docCollection.size(); k++) {
            int docMeId = (int) docCollection.get(k).get("DOC_ME_ID");
            int docFlId = (int) docCollection.get(k).get("DOC_FL_ID");
            int docSlId = (int) docCollection.get(k).get("DOC_SL_ID");
            int docExId = (int) docCollection.get(k).get("DOC_EX_ID");
            int docClId = (int) docCollection.get(k).get("DOC_CL_ID");
            if (type.equals("EXPAND")) {
                int exId = (int) docCollection.get(k).get("DOC_EX_ID");
                if (eleId == exId) {
                    findViewById(docMeId).setVisibility(View.VISIBLE);
                    findViewById(docFlId).setVisibility(View.VISIBLE);
                    findViewById(docSlId).setVisibility(View.VISIBLE);
                    findViewById(docExId).setVisibility(View.GONE);
                    findViewById(docClId).setVisibility(View.VISIBLE);
                    ObjectAnimator.ofFloat(findViewById(docSlId), "alpha", 0f, 1f).setDuration(500).start();

                    List fdList = (List) docCollection.get(k).get("FACE_DATA");
                    if (!isTapTargetViewShown() && targetView == null) {
                        int fdListLen = fdList.size();
                        if (fdListLen == 0) {
                            //nothing to do
                        } else {
                            targetView = findViewById((int) ((HashMap) fdList.get(0)).get("DOC_EL_ID"));
                            targetView.setBackgroundColor(Color.TRANSPARENT);
                            TapTarget tapTarget = TapTarget.forView(targetView, "Select Face", "Select 2 faces to match face")
                                    .outerCircleColor(color.secondary)
                                    .targetCircleColor(color.transparent)
                                    .titleTextColor(color.white)
                                    .descriptionTextColor(color.white);
                            View finalTargetView = targetView;
                            TapTargetView.showFor(this, tapTarget, new TapTargetView.Listener() {
                                @Override
                                public void onTargetClick(TapTargetView view) {
                                    super.onTargetClick(view);
                                    view.dismiss(true);
                                    setTapTargetViewShown();
                                    handleImageClick((String) finalTargetView.getTag());
                                }
                            });
                        }
                    }
                }
            } else {
                int clId = (int) docCollection.get(k).get("DOC_CL_ID");
                if (eleId == clId) {
                    findViewById(docMeId).setVisibility(View.VISIBLE);
                    findViewById(docFlId).setVisibility(View.VISIBLE);
                    findViewById(docExId).setVisibility(View.VISIBLE);
                    findViewById(docClId).setVisibility(View.GONE);
                    ObjectAnimator.ofFloat(findViewById(docSlId), "alpha", 1f, 0f).setDuration(500).start();
                    findViewById(docSlId).setVisibility(View.GONE);
                }
            }
        }
    }
}