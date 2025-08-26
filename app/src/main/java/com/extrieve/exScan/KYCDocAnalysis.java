package com.extrieve.exScan;

import static android.content.ContentValues.TAG;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.extrieve.splicer.aisdk.AiDocument;
import com.extrieve.splicer.aisdk.Config;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class KYCDocAnalysis extends AppCompatActivity {
    ImageView inputImage, copy, share, saveJson;
    Button kycDetect, kycExtract, kycVerify;
    Spinner selectDocument;
    TextView showResult, plainText, jsonText, loaderMessage;
    ScrollView showResultInTableView;
    LinearLayout showResultInTable, loaderContainer;
    String filePath, resData, extractedJson;
    List<String> lastParentKeyName = new ArrayList<>();
    Boolean resStatus;
    File imgFile;
    AiDocument aiDocument;
    List<String> dynamicData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.kyc_doc_analysis);

        inputImage = findViewById(R.id.inputImage);
        copy = findViewById(R.id.copy);
        share = findViewById(R.id.share);
        saveJson = findViewById(R.id.saveJson);
        kycDetect = findViewById(R.id.kycDetect);
        kycExtract = findViewById(R.id.kycExtract);
        kycVerify = findViewById(R.id.kycVerify);
        selectDocument = findViewById(R.id.selectDocument);
        showResult = findViewById(R.id.showResult);
        showResultInTable = findViewById(R.id.showResultInTable);
        showResultInTableView = findViewById(R.id.showResultInTableView);
        plainText = findViewById(R.id.plainText);
        jsonText = findViewById(R.id.jsonText);
        loaderContainer = findViewById(R.id.loaderContainer);
        loaderMessage = findViewById(R.id.loaderMessage);

        //added for textView scroll
        showResult.setMovementMethod(new ScrollingMovementMethod());
        //get intent
        Intent TextractIntent = getIntent();
        filePath = TextractIntent.getStringExtra("KYCDocImagePath");
        //to trigger all on create actions
        onInit();

        kycDetect.setOnClickListener(v -> {
            doKycDetection();
        });
        kycExtract.setOnClickListener(v -> {
            doKycExtraction();
        });
        kycVerify.setOnClickListener(v -> {
            doKycVerification();
        });
        plainText.setOnClickListener(view -> {
            convertResponse(1);
        });
        jsonText.setOnClickListener(view -> {
            convertResponse(2);
        });
        copy.setOnClickListener(v -> {
            copyDataFromTextview();
        });
        share.setOnClickListener(v -> {
            shareText();
        });
        saveJson.setOnClickListener(v -> {
            saveJsonToDisk();
        });
    }

    private void onInit() {
        resStatus = false;
        extractedJson = null;
        saveJson.setVisibility(View.GONE);
        kycVerify.setVisibility(View.GONE);
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
                    dynamicData = new ArrayList<>();
                    dynamicData = aiDocument.GetKYCDocList();

                    if (!dynamicData.isEmpty()) kycVerify.setVisibility(View.VISIBLE);
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, dynamicData);
                    adapter.setDropDownViewResource(R.layout.spinner_item);
                    selectDocument.setAdapter(adapter);
                    selectDocument.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                            String selectedOption = dynamicData.get(position);
                            Log.d(TAG, "Doc_selected: " + selectedOption);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                            // Do nothing here
                        }
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void doKycDetection() {
        resData = null;
        showLoader("");
        aiDocument.KYCDetect(response -> {
            try {
                if (response == null) {
                    resData = "Detection is not enabled";
                    Toast.makeText(this, "Detection is not enabled", Toast.LENGTH_SHORT).show();
                    return;
                }
                JSONObject data = new JSONObject(response);
                resData = data.toString(4);
                convertResponse(1);
            } catch (JSONException e) {
                resData = "Runtime Json error";
            }
            hideLoader();
        });
    }

    private void doKycExtraction() {
        resData = null;
        showLoader("");
        aiDocument.KYCExtract(response -> {
            try {
                if (response == null) {
                    resData = "Enable extraction feature";
                    Toast.makeText(this, "Enable extraction feature", Toast.LENGTH_SHORT).show();
                    return;
                }
                JSONObject data = new JSONObject(response);
                resData = extractedJson = data.toString(4);
                if (extractedJson != null) saveJson.setVisibility(View.VISIBLE);
                convertResponse(1);
            } catch (JSONException e) {
                resData = "Runtime Json error";
            }
            hideLoader();
        });
    }

    private void doKycVerification() {
        String docName = dynamicData.get(selectDocument.getSelectedItemPosition());
        if (docName.isEmpty()) return;
        resData = null;
        showLoader("");
        aiDocument.KYCVerify(docName, response -> {
            try {
                if (response == null) {
                    resData = "Enable extraction feature";
                    Toast.makeText(this, "Enable extraction feature", Toast.LENGTH_SHORT).show();
                    return;
                }
                JSONObject data = new JSONObject(response);
                resData = data.toString(4);
                convertResponse(1);
            } catch (JSONException e) {
                resData = "Runtime Json error";
            }
            hideLoader();
        });
    }

    private void convertResponse(int i) {
        showResult.setText("");
        lastParentKeyName = new ArrayList<>();

        if (i == 1) {
            plainText.setBackgroundResource(R.drawable.button_bg);
            jsonText.setBackgroundResource(0);
            showResult.setVisibility(View.GONE);
            showResultInTableView.setVisibility(View.VISIBLE);

            if (resData != null) {
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(resData);
                    TableLayout tableLayout = new TableLayout(this);
                    tableLayout.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                    tableLayout.setStretchAllColumns(true);
                    createTableRows(jsonObject, tableLayout, "", false);
                    showResultInTable.removeAllViews();
                    showResultInTable.addView(tableLayout);
                    showResult.setText(resData);
                } catch (JSONException e) {
                    Log.d("Error", "Runtime error in convertResponse: " + e);
                }
            } else {
                showResult.setVisibility(View.VISIBLE);
                showResultInTableView.setVisibility(View.GONE);
                showResult.setText(R.string.no_data_found);
            }
        } else {
            plainText.setBackgroundResource(0);
            jsonText.setBackgroundResource(R.drawable.button_bg);
            showResult.setVisibility(View.VISIBLE);
            showResultInTableView.setVisibility(View.GONE);

            if (resData != null) {
                showResult.setText(resData);
            } else {
                showResult.setText(R.string.no_data_found);
            }
        }
    }

    private void createTableRows(JSONObject jsonObject, TableLayout tableLayout, String parentKeyName, Boolean Child) {
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.equals("KEYVALUE")) continue;
            TableRow tableRow = new TableRow(this);

            TextView keyTextView = new TextView(this);
            keyTextView.setText(key);
            keyTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            keyTextView.setPadding(20, 10, 10, 10);
            keyTextView.setGravity(Gravity.CENTER);
            keyTextView.setTextColor(Color.BLACK);
            keyTextView.setSingleLine(false);
            TableRow.LayoutParams keyParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f); // 1f = 50% width
            keyTextView.setLayoutParams(keyParams);

            Object value = null;
            String confidence = "";
            String actualValue = "";
            try {
                value = jsonObject.get(key);
                if (value instanceof JSONObject) {
                    JSONObject innerObj = (JSONObject) value;
                    if (innerObj.has("VALUE") && innerObj.has("CONFIDENCE")) {
                        confidence = innerObj.optString("CONFIDENCE", "");
                        actualValue = innerObj.optString("VALUE", "");
                        value = actualValue;  // Override with actual value for downstream usage
                    }
                }
            } catch (JSONException ignored) {
            }

            if (!lastParentKeyName.contains(parentKeyName))
                if (Child && !parentKeyName.isEmpty()) {
                    TextView nestedHeaderTextView = new TextView(this);
                    nestedHeaderTextView.setText(parentKeyName);
                    nestedHeaderTextView.setTypeface(null, Typeface.BOLD);
                    nestedHeaderTextView.setGravity(Gravity.CENTER);
                    nestedHeaderTextView.setPadding(10, 10, 10, 10);
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(10, 10, 10, 10);
                    nestedHeaderTextView.setLayoutParams(layoutParams);
                    nestedHeaderTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                    nestedHeaderTextView.setBackgroundColor(Color.WHITE);
                    nestedHeaderTextView.setTextColor(Color.parseColor("#076a8e"));
                    nestedHeaderTextView.setSingleLine(false); // Allow text to wrap to the next line
                    tableLayout.addView(nestedHeaderTextView);
                    lastParentKeyName.add(parentKeyName);
                }

            if (value instanceof JSONObject) {
                createTableRows((JSONObject) value, tableLayout, key, true);
            } else if (value instanceof JSONArray) {
                // Handle JSON array
                JSONArray jsonArray = (JSONArray) value;
                int jsArrLen = jsonArray.length();
                if (jsArrLen == 0) {
                    // Handle no value in the array
                    TableRow arrayRow = new TableRow(this);
                    TextView arrayValueTextView = new TextView(this);
                    arrayValueTextView.setText("");
                    arrayValueTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                    arrayValueTextView.setPadding(10, 10, 10, 10);
                    arrayValueTextView.setTextColor(Color.BLACK);
                    arrayValueTextView.setSingleLine(false);
                    arrayRow.addView(arrayValueTextView);
                    tableLayout.addView(arrayRow);
                } else {
                    for (int i = 0; i < jsArrLen; i++) {
                        try {
                            Object arrayElement = jsonArray.get(i);
                            if (arrayElement instanceof JSONObject) {
                                // Handle each JSONObject in the array
                                createTableRows((JSONObject) arrayElement, tableLayout, key, true);
                            } else {
                                // Handle simple values in the array
                                TableRow arrayRow = new TableRow(this);
                                TextView arrayValueTextView = new TextView(this);
                                arrayValueTextView.setText(arrayElement.toString());
                                arrayValueTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                                arrayValueTextView.setPadding(10, 10, 10, 10);
                                arrayValueTextView.setTextColor(Color.BLACK);
                                arrayValueTextView.setSingleLine(false);
                                arrayRow.addView(arrayValueTextView);
                                tableLayout.addView(arrayRow);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                // Handle simple values
                TextView valueTextView = new TextView(this);
                valueTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                valueTextView.setSingleLine(false); // Allow text to wrap to the next line
                valueTextView.setEllipsize(null); // Ensure text is not truncated
                // Set layout parameters for the TextView to ensure it wraps content within the TableRow
                TableRow.LayoutParams valueParams = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f);
                valueTextView.setPadding(10, 10, 10, 10);
                valueTextView.setLayoutParams(valueParams);

                if (value != null && value.equals(true)) {
                    resStatus = true;
                }
                if (Boolean.TRUE.equals(resStatus)) { // Use Boolean.TRUE for comparison
                    valueTextView.setTextColor(Color.parseColor("#029902"));
                } else {
                    if (!confidence.isEmpty() && confidence.equals("HIGH")) {
                        valueTextView.setTextColor(Color.parseColor("#029902"));
                    } else if (!confidence.isEmpty() && confidence.equals("MEDIUM")) {
                        valueTextView.setTextColor(Color.parseColor("#ff8600"));
                    } else if (!confidence.isEmpty() && confidence.equals("LOW")) {
                        valueTextView.setTextColor(Color.parseColor("#ff0000"));
                    } else {
                        valueTextView.setTextColor(Color.BLACK);
                    }
                }
                String text = (value == null || value.equals("")) ? "-" : value.toString();
                valueTextView.setText(text);
                valueTextView.setGravity(Gravity.CENTER);
                valueTextView.setTypeface(null, Typeface.BOLD);
                tableRow.addView(keyTextView);

                tableRow.addView(valueTextView);
                tableLayout.addView(tableRow);
            }
        }
        resStatus = false;
    }

    private void copyDataFromTextview() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("KYCDocAnalysis", showResult.getText());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Response Json copied", Toast.LENGTH_SHORT).show();
    }

    private void shareText() {
        String s = String.valueOf(showResult.getText());
        Intent shareIn = new Intent(Intent.ACTION_SEND);
        shareIn.setType("text/plain");
        shareIn.putExtra(Intent.EXTRA_SUBJECT, "KYCDocAnalysis");
        shareIn.putExtra(Intent.EXTRA_TEXT, s);
        startActivity(Intent.createChooser(shareIn, "Share Response Json Via"));
    }

    private void saveJsonToDisk() {
        File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File splicerFolder = new File(downloadsFolder, "Splicer");

        // Ensure the directory exists
        if (!splicerFolder.exists()) {
            boolean dirCreated = splicerFolder.mkdirs();
            if (!dirCreated) {
                Log.d("Error", "Failed to create Splicer directory");
                Toast.makeText(this, "Write permission not found", Toast.LENGTH_LONG).show();
                return;
            }
        }

        String baseFileName = "extraction_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = baseFileName + ".json";
        File file = new File(splicerFolder, fileName);

        // Generate a new filename if the file already exists
        int counter = 1;
        while (file.exists()) {
            fileName = baseFileName + "_" + counter + ".json";
            file = new File(splicerFolder, fileName);
            counter++;
        }

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(extractedJson);
            Toast.makeText(this, "Saved in Downloads/Splicer", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.d("Error", "Runtime error in saveJsonToDisk: " + e);
            Toast.makeText(this, "Unable to save", Toast.LENGTH_LONG).show();
            saveJson.setVisibility(View.GONE);
        }
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