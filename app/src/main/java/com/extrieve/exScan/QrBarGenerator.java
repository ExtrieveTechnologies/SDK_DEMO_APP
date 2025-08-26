/*
Copyright (c) $originalComment.match("Copyright \(c\) (\d+)", 1, "-", "$today.year")$today.year. Extrieve Technologies Pvt. Ltd. All rights reserved.

 * No part of this software/program/application/documentation may be reproduced, distributed, or transmitted in any form or by any means, including photocopying, recording, or other electronic or mechanical methods, without the prior written permission of the publisher, except in the case of brief quotations embodied in critical reviews and certain other noncommercial uses permitted by copyright law. For permission requests, write to the publisher, addressed “Attention: Permissions Coordinator,” at the address below.
 *
 * Extrieve Technologies
 * Enterprise DMS, Workflow, OCR, PDF solutions & SDKs with AI
 * www.extrieve.com
 * Info@extrieve.com | devsupport@extrieve.com
 * Author : Team Extrieve | Amal Karunakaran.
 */

//DEV_HELP: Package declaration for the ExScan demo app using Extrieve SDK.
package com.extrieve.exScan;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.extrieve.quickcapture.sdk.OpticalCodeHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

//DEV_HELP: Main activity class demonstrating QR and Barcode generation using the Extrieve SDK.
public class QrBarGenerator extends AppCompatActivity {
    //DEV_HELP: Declaration of UI elements.
    LinearLayout result, resOptions;
    ImageView reset, resClose, resImage, share, save;
    EditText inputText;
    ProgressBar resLoader;
    Button qrGenerate, barGenerate;

    //DEV_HELP: Bitmap to store generated image and integer to store image type (QR or Barcode).
    Bitmap resBitmap;
    int resType;

    //DEV_HELP: OpticalCodeHelper instance from Extrieve SDK for generating QR and Barcode images.
    OpticalCodeHelper opticalCodeObj;

    //DEV_HELP: onCreate method initializes UI elements, listeners, and SDK instance.
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qrbargenerate);

        //DEV_HELP: Initialize the OpticalCodeHelper instance.
        opticalCodeObj = new OpticalCodeHelper();

        //DEV_HELP: Assign UI elements to their corresponding views.
        result = findViewById(R.id.result);
        resOptions = findViewById(R.id.resOptions);
        reset = findViewById(R.id.reset);
        resClose = findViewById(R.id.resClose);
        resImage = findViewById(R.id.resImage);
        inputText = findViewById(R.id.inputText);
        inputText.setTextColor(ContextCompat.getColor(this, R.color.black));
        inputText.setHintTextColor(ContextCompat.getColor(this, R.color.black));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            inputText.setTextAppearance(R.style.Theme_EditText);
        }
        resLoader = findViewById(R.id.resLoader);
        qrGenerate = findViewById(R.id.qrGenerate);
        barGenerate = findViewById(R.id.barGenerate);
        share = findViewById(R.id.share);
        save = findViewById(R.id.save);

        //DEV_HELP: Set click listener for reset button to clear input and animate the reset icon.
        reset.setOnClickListener(v -> {
            //DEV_HELP: Create and start a rotate animation for the reset icon.
            RotateAnimation rotateAnimation = new RotateAnimation(0F, 360F, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setDuration(1000);
            rotateAnimation.setRepeatCount(1);
            reset.startAnimation(rotateAnimation);

            //DEV_HELP: Clear the text input and restore the default UI.
            inputText.setText("");
            showOnCreateUI();
        });

        //DEV_HELP: Set click listener for closing the result view.
        resClose.setOnClickListener(v -> showOnCreateUI());

        //DEV_HELP: Set click listener for QR code generation.
        qrGenerate.setOnClickListener(v -> {
            showOnProcessUI();
            String iData = String.valueOf(inputText.getText());
            //DEV_HELP: If no input is provided, revert back to the default UI.
            if (iData.isEmpty()) {
                showOnCreateUI();
                return;
            }
            //DEV_HELP: Generate QR code using the OpticalCodeHelper SDK.
            Bitmap qrcode = opticalCodeObj.GenerateQRCode(iData);
            showOnSuccessUI(1, qrcode, v);
        });

        //DEV_HELP: Set click listener for Barcode generation.
        barGenerate.setOnClickListener(v -> {
            showOnProcessUI();
            String iData = String.valueOf(inputText.getText());
            //DEV_HELP: If no input is provided, revert back to the default UI.
            if (iData.isEmpty()) {
                showOnCreateUI();
                return;
            }
            //DEV_HELP: Generate Barcode with text using the OpticalCodeHelper SDK.
            Bitmap barcode = opticalCodeObj.GenerateBarcodeWithText(iData);
            // Bitmap barcode = opticalCodeObj.GenerateBarcode(iData); //DEV_HELP: Alternate method to generate Barcode without text.
            showOnSuccessUI(2, barcode, v);
        });

        //DEV_HELP: Set click listener to share the generated image.
        share.setOnClickListener(v -> {
            shareResImage(resBitmap);
        });

        //DEV_HELP: Set click listener to save the generated image to storage.
        save.setOnClickListener(v -> {
            String filePath = saveBitmap();
            Toast.makeText(this, "Saved to pictures", Toast.LENGTH_SHORT).show();
        });

        //DEV_HELP: Initialize the default UI on app start.
        showOnCreateUI();
    }

    //DEV_HELP: Hides the result view and shows the initial input UI.
    private void showOnCreateUI() {
        result.setVisibility(View.GONE);
    }

    //DEV_HELP: Displays a loading UI state while processing image generation.
    private void showOnProcessUI() {
        result.setVisibility(View.VISIBLE);
        resClose.setVisibility(View.GONE);
        resLoader.setVisibility(View.VISIBLE);
        resImage.setVisibility(View.GONE);
        resOptions.setVisibility(View.GONE);
    }

    //DEV_HELP: Displays the generated image along with options once generation is complete.
    //DEV_HELP: 'type' parameter distinguishes between QR (1) and Barcode (2).
    private void showOnSuccessUI(int type, Bitmap bmp, View v) {
        if (type == 1) {
            //DEV_HELP: Resize the QR code bitmap for display.
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bmp, 750, 750, true);
            resBitmap = resizedBitmap;
            resType = 1;
            resImage.setImageBitmap(resizedBitmap);
        }
        if (type == 2) {
            //DEV_HELP: Resize the Barcode bitmap for display.
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bmp, 750, 250, true);
            resBitmap = resizedBitmap;
            resType = 2;
            resImage.setImageBitmap(resizedBitmap);
        }
        //DEV_HELP: Update UI to display the generated image and options.
        resClose.setVisibility(View.VISIBLE);
        resLoader.setVisibility(View.GONE);
        resImage.setVisibility(View.VISIBLE);
        resOptions.setVisibility(View.VISIBLE);

        //DEV_HELP: Hide the soft keyboard after generation.
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    //DEV_HELP: Saves the generated image to the device's public Pictures directory.
    public String saveBitmap() {
        String type = (resType == 1) ? "QR" : "BAR";
        String fileName = type + "_" + System.currentTimeMillis() + ".png";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "QuickCapture");

        //DEV_HELP: Create the directory if it doesn't exist.
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        File file = new File(storageDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            //DEV_HELP: Compress and write the bitmap image as PNG.
            resBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    //DEV_HELP: Shares the generated image using Android's share Intent.
    public void shareResImage(Bitmap bitmap) {
        String imagePath = saveBitmapToCache(bitmap);

        if (imagePath != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            //DEV_HELP: Get a content URI for the image file using FileProvider.
            Uri imageUri = FileProvider.getUriForFile(this, "com.extrieve.exScan.FileProvider", new File(imagePath));
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share Image"));
        } else {
            Toast.makeText(this, "Error sharing image", Toast.LENGTH_SHORT).show();
        }
    }

    //DEV_HELP: Saves the bitmap temporarily to the cache directory to be shared.
    private String saveBitmapToCache(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "image.png");

            FileOutputStream stream = new FileOutputStream(imageFile);
            //DEV_HELP: Compress and write the bitmap image as PNG.
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            return imageFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
