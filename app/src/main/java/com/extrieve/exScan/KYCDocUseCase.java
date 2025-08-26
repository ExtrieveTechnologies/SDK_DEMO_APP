package com.extrieve.exScan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.extrieve.splicer.aisdk.AiDocument;
import com.extrieve.splicer.aisdk.Config;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KYCDocUseCase extends AppCompatActivity {
    Button completed;
    TextView loaderMessage, DocName, docSubType;
    ScrollView showResultInTableView;
    LinearLayout showResultInLinear, loaderContainer;
    String filePath, resData, extractedJson;
    Boolean resStatus;
    File imgFile;
    AiDocument aiDocument;
    ZoomPanImageView inputImage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kyc_doc_usecase);

        inputImage = findViewById(R.id.inputImage);
        completed = findViewById(R.id.submitBtn);
        showResultInLinear = findViewById(R.id.showResultInLinear);
        showResultInTableView = findViewById(R.id.showResult);
        loaderContainer = findViewById(R.id.loaderContainer);
        loaderMessage = findViewById(R.id.loaderMessage);
        DocName = findViewById(R.id.docName);
        docSubType = findViewById(R.id.docSubType);
        docSubType.setVisibility(View.GONE);
        showResultInTableView.setVisibility(View.GONE);

        //get intent
        Intent TextractIntent = getIntent();
        filePath = TextractIntent.getStringExtra("KYCDocImagePath");
        completed.setOnClickListener(v -> {
//            if (validateFields(showResultInLinear, this)) {
                new AlertDialog.Builder(this).setTitle("Confirm Submission").setMessage("Cross-check all fields highlighted in red and yellow before submitting").setPositiveButton("Submit", (dialog, which) -> {
                    Toast.makeText(this, "Submitted successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }).setNegativeButton("Cancel", null).show();
//            } else {
//                Toast.makeText(this, "Please verify all fields", Toast.LENGTH_SHORT).show();
//            }
        });
        //to trigger all on create actions
        onInit();
    }

    private void onInit() {
        resStatus = false;
        extractedJson = null;
        if (filePath != null) {
            imgFile = new File(filePath);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                inputImage.setImageBitmap(myBitmap);
                try {
                    // Activate SDK, before initiating AiDocument class
                    SharedPreferences prefs = getSharedPreferences("LIC_SETTING", MODE_PRIVATE);
                    String licenseStringExtract = prefs.getString("LIC_SPLICER_DATA", "");
                    boolean ret = Config.License.Activate(this, licenseStringExtract);
                    if (!ret)
                        Toast.makeText(this, "Expired license, Use valid one", Toast.LENGTH_SHORT).show();
                    aiDocument = new AiDocument(this, filePath);
                    doKycExtraction();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void doKycExtraction() {
        resData = null;
        showLoader("");

        aiDocument.KYCExtract(response -> {
            try {
                if (response == null) {
                    Toast.makeText(this, "Enable extraction feature", Toast.LENGTH_SHORT).show();
                    hideLoader();
                    return;
                }
                JSONObject data = new JSONObject(response);
                if (!data.getBoolean("STATUS")) {
                    Toast.makeText(this, "Unable to perform extraction", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    return;
                }
                resData = extractedJson = data.toString(4);
                // Step 1: Render all fields based on TYPE
                generateDynamicFields(data, showResultInLinear, this);
                // Step 2: Start typing animations
                animateFillFields(data, showResultInLinear, this);
            } catch (JSONException e) {
                resData = "Runtime Json error";
            }
            hideLoader();
        });
    }

    private String normalizeDocType(String rawType, Map<String, String[]> docFieldsMap) {
        if (rawType == null) return "";
        // Normalize input: uppercase and strip non-alphabetic characters
        String cleanedType = rawType.trim().toUpperCase().replaceAll("[^A-Z]", "");
        // Search for a matching normalized key in the map
        for (String key : docFieldsMap.keySet()) {
            String normalizedKey = key.toUpperCase().replaceAll("[^A-Z]", "");
            if (normalizedKey.equals(cleanedType)) {
                return key; // Return the actual key from the map
            }
        }
        return ""; // No match found
    }

    public void generateDynamicFields(JSONObject responseJson, LinearLayout showResultInLinear, Context context) {
        showResultInLinear.removeAllViews();

        Map<String, String[]> docFieldsMap = new HashMap<>();
        docFieldsMap.put("PAN_CARD", new String[]{"NAME", "FATHER'S NAME", "DOB", "PAN NO", "DATE OF INCORPORATION", "BUSINESS NAME"});
        docFieldsMap.put("AADHAAR", new String[]{"NAME", "DOB", "GENDER", "AADHAAR NO", "ADDRESS"});
        docFieldsMap.put("DRIVING_LICENSE", new String[]{"NAME", "DOB", "S/D/W", "ADDRESS", "DATE OF ISSUE", "DATE OF EXPIRY", "LICENSE NO"});
        docFieldsMap.put("VOTER_ID", new String[]{"NAME", "DOB", "GUARDIAN'S NAME", "ADDRESS", "UID", "GENDER"});
        docFieldsMap.put("PASSPORT", new String[]{"SURNAME", "GIVEN NAME", "DOB", "DATE OF ISSUE", "DATE OF EXPIRY", "PASSPORT NO", "PLACE OF BIRTH", "PLACE OF ISSUE", "GENDER", "COUNTRY", "COUNTRY CODE"});

        String docType = responseJson.optString("TYPE", "").toUpperCase();
        String docSType = responseJson.optString("SUBTYPE", "").toUpperCase();

        DocName.setText("Document: " + docType);
        docSubType.setText((docSType != null && !docSType.isEmpty()) ? docSType : docType);

        //String docTypeMap = normalizeDocType(docType, docFieldsMap);
        String[] requiredFields = docFieldsMap.containsKey(docType) ? docFieldsMap.get(docType) : new String[0];

        List<EditText> editTextList = new ArrayList<>();

        for (int i = 0; i < requiredFields.length; i++) {
            String field = requiredFields[i];

            // Container for TextInputLayout
            LinearLayout fieldContainer = new LinearLayout(context);
            fieldContainer.setOrientation(LinearLayout.HORIZONTAL);
            fieldContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            fieldContainer.setGravity(Gravity.CENTER_VERTICAL);

            // TextInputLayout and EditText
            TextInputLayout inputLayout = new TextInputLayout(context);
            LinearLayout.LayoutParams inputLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            inputLayoutParams.setMargins(0, 6, 0, 6);
            inputLayout.setLayoutParams(inputLayoutParams);
            inputLayout.setHint(field);  // Label/hint

            EditText editText = new EditText(context);
            editText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            editText.setEnabled(false);  // Initially disabled
            editText.setTag(field);      // Store field name

            // Set IME options
            if (i < requiredFields.length - 1) {
                editText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            } else {
                editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            }

            // Set Editor Action Listener
            final int nextIndex = i + 1;
            editText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    if (nextIndex < editTextList.size()) {
                        EditText nextEditText = editTextList.get(nextIndex);
                        if (nextEditText.isEnabled()) {
                            nextEditText.requestFocus();
                        }
                    }
                    return true;
                } else if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    }
                    editText.clearFocus();
                    return true;
                }
                return false;
            });

            inputLayout.addView(editText);
            fieldContainer.addView(inputLayout);
            showResultInLinear.addView(fieldContainer);
            editTextList.add(editText);
        }
        docSubType.setVisibility(View.VISIBLE);
        showResultInTableView.setVisibility(View.VISIBLE);
    }

    public void animateFillFields(JSONObject responseJson, LinearLayout showResultInLinear, Context context) {
        int delayPerChar = 75;

        JSONObject keyValues = responseJson.optJSONObject("DATA");
        if (keyValues == null) return;

        new Thread(() -> {
            for (int i = 0; i < showResultInLinear.getChildCount(); i++) {
                View containerView = showResultInLinear.getChildAt(i);
                if (!(containerView instanceof LinearLayout)) continue;

                LinearLayout fieldContainer = (LinearLayout) containerView;
                TextInputLayout inputLayout = (TextInputLayout) fieldContainer.getChildAt(0);
                EditText editText = inputLayout.getEditText();
                if (editText == null) continue;

                String fieldKey = (String) editText.getTag();
                JSONObject fieldObj = keyValues.optJSONObject(fieldKey);
                if (fieldObj == null) fieldObj = new JSONObject();

                String value = fieldObj.optString("VALUE", "");
                String confidence = fieldObj.optString("CONFIDENCE", "").toUpperCase();

                if (value == null || value.isEmpty()) {
                    // No value to type — enable EditText immediately
                    runOnUiThread(() -> {
                        editText.setEnabled(true);
                        editText.setText("-");
                        // Apply confidence color even if no value (optional)
                        int color = getConfidenceColor("LOW");
                        editText.setTextColor(color);
                        editText.setTag(R.id.tag_confidence, "LOW");
                    });
                    continue;  // Skip typing animation for this field
                }

                // Start clear text
                runOnUiThread(() -> {
                    editText.setEnabled(false);
                    editText.setText("-");
                });

                StringBuilder currentText = new StringBuilder();
                for (char c : value.toCharArray()) {
                    currentText.append(c);
                    String displayText = currentText.toString();
                    runOnUiThread(() -> {
                        editText.requestFocus();
                        editText.setText(displayText);
                    });
                    try {
                        Thread.sleep(delayPerChar);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // Set colors and error
                int color = getConfidenceColor(confidence);
                runOnUiThread(() -> {
                    editText.setEnabled(true);
                    editText.setTextColor(color);
                    editText.setTag(R.id.tag_confidence, confidence);
                });

                try {
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // Step: Adding the watcher for change detection
            monitorEditsAndSetHighConfidence(showResultInTableView);
        }).start();
    }

    private int getConfidenceColor(String confidence) {
        switch (confidence.toUpperCase()) {
            case "LOW":
                return Color.RED;
            case "MEDIUM":
                return Color.parseColor("#FFA500"); // orange
            case "HIGH":
                return Color.parseColor("#2E7D32"); // green
            default:
                return Color.DKGRAY; // fallback
        }
    }

    public void monitorEditsAndSetHighConfidence(ScrollView mainScrollView) {
        ViewGroup root = (ViewGroup) mainScrollView.getChildAt(0);
        if (root == null) return;

        int childCount = root.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View fieldContainer = root.getChildAt(i);

            if (fieldContainer instanceof LinearLayout) {
                LinearLayout linear = (LinearLayout) fieldContainer;
                if (linear.getChildCount() > 0 && linear.getChildAt(0) instanceof TextInputLayout) {
                    TextInputLayout inputLayout = (TextInputLayout) linear.getChildAt(0);
                    EditText editText = inputLayout.getEditText();

                    if (editText != null) {
                        editText.setOnFocusChangeListener((v, hasFocus) -> {
                            if (hasFocus) {
                                String text = editText.getText() != null ? editText.getText().toString().trim() : "";

                                String confidence = text.isEmpty() ? "" : "HIGH";
                                editText.setTag(R.id.tag_confidence, confidence);

                                int color = getConfidenceColor(confidence);
                                ColorStateList csl = ColorStateList.valueOf(color);
                                editText.setTextColor(color);

                                ViewParent frame = editText.getParent(); // FrameLayout
                                if (frame instanceof View) {
                                    ViewParent parent = ((View) frame).getParent(); // TextInputLayout
                                    if (parent instanceof TextInputLayout) {
                                        TextInputLayout layout = (TextInputLayout) parent;
                                        layout.setDefaultHintTextColor(csl);
                                        layout.setError(null);
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    public boolean validateFields(LinearLayout showResultInLinear, Context context) {
        boolean allValid = true;

        for (int i = 0; i < showResultInLinear.getChildCount(); i++) {
            View containerView = showResultInLinear.getChildAt(i);

            if (containerView instanceof LinearLayout) {
                LinearLayout fieldContainer = (LinearLayout) containerView;

                TextInputLayout inputLayout = null;
                EditText editText = null;

                for (int j = 0; j < fieldContainer.getChildCount(); j++) {
                    View child = fieldContainer.getChildAt(j);
                    if (child instanceof TextInputLayout) {
                        inputLayout = (TextInputLayout) child;
                        editText = inputLayout.getEditText();
                    }
                }

                if (inputLayout == null || editText == null) continue;
                String label = inputLayout.getHint() != null ? inputLayout.getHint().toString() : "";
                String confidence = (String) editText.getTag(R.id.tag_confidence);

                if (confidence.isEmpty()) {
                    allValid = false;
                    inputLayout.setError(label + " field is required");
                    continue;
                }

                if ("LOW".equalsIgnoreCase(confidence)) {
                    inputLayout.setError("Please correct the input value for \"" + label + "\"");
                    inputLayout.setErrorTextColor(ColorStateList.valueOf(Color.parseColor("#C62828")));
                    allValid = false;
                } else if ("MEDIUM".equalsIgnoreCase(confidence)) {
                    inputLayout.setError("Please review the input value for \"" + label + "\"");
                    inputLayout.setErrorTextColor(ColorStateList.valueOf(Color.parseColor("#FB8C00")));
                    allValid = false;
                } else {
                    inputLayout.setError(null);
                }
            }
        }

        return allValid;
    }

    public void showLoader(String msg) {
        runOnUiThread(() -> {
            if (msg != null && !msg.isEmpty()) {
                loaderMessage.setText(msg);
            } else {
                loaderMessage.setText(R.string.loaderText);
            }
            loaderContainer.setVisibility(View.VISIBLE);
        });
    }

    public void hideLoader() {
        runOnUiThread(() -> loaderContainer.setVisibility(View.GONE));
    }
}