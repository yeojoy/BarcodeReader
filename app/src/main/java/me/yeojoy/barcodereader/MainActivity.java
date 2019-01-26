package me.yeojoy.barcodereader;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.zxing.Result;

import java.io.ByteArrayOutputStream;
import java.io.File;

import me.dm7.barcodescanner.core.IViewFinder;
import me.yeojoy.barcodereader.widget.MyViewFinderView;
import me.yeojoy.barcodereader.widget.MyZxingScannerView;

public class MainActivity extends AppCompatActivity implements MyZxingScannerView.ResultHandler {
    private static final String TAG = MainActivity.class.getSimpleName();

    private MyZxingScannerView mScannerView;

    private TextView mTextView;
    private TextView mTextViewImageBytes;
    private Button mButtonRetake;

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1000;

    private String[] permissions = new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE };

    private static int mCatchCount;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            // 이 권한을 필요한 이유를 설명해야하는가?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {

                // 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
                // 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다

            } else {

                ActivityCompat.requestPermissions(this, permissions,
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다

            }

        } else {
            initCamera();
        }

        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // 다이어로그같은것을 띄워서 사용자에게 해당 권한이 필요한 이유에 대해 설명합니다
                // 해당 설명이 끝난뒤 requestPermissions()함수를 호출하여 권한허가를 요청해야 합니다

            } else {

                ActivityCompat.requestPermissions(this, permissions,
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // 필요한 권한과 요청 코드를 넣어서 권한허가요청에 대한 결과를 받아야 합니다

            }
        }

    }

    private void initCamera() {
        setContentView(R.layout.activity_main);                // Set the scanner view as the content view
        FrameLayout container = findViewById(R.id.frame_layout_scanner_container);
        mTextView = findViewById(R.id.text_view_result);
        mTextViewImageBytes = findViewById(R.id.text_view_image_bytes);

        mScannerView = new MyZxingScannerView(this) {
            @Override
            protected IViewFinder createViewFinderView(Context context) {
                return new MyViewFinderView(context);
            }
        };

        mScannerView.setAutoFocus(true);
        mScannerView.setLaserEnabled(false);
        mScannerView.setBorderColor(Color.WHITE);
        mScannerView.setBorderStrokeWidth((int) (3 * getResources().getDisplayMetrics().density));
        mScannerView.setSquareViewFinder(true);
        mScannerView.setBorderLineLength((int) (130 * getResources().getDisplayMetrics().density));
        mScannerView.setAspectTolerance(0.5f);
        mScannerView.stopCamera();

        container.removeAllViews();
        container.addView(mScannerView);

        mButtonRetake = findViewById(R.id.button_retake);
        mButtonRetake.setEnabled(false);
        mButtonRetake.setOnClickListener(view -> {
            mScannerView.resumeCameraPreview(this);
            mTextView.setText(null);
            mTextViewImageBytes.setText(null);
            mButtonRetake.setEnabled(false);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS:

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한 허가
                    // 해당 권한을 사용해서 작업을 진행할 수 있습니다
                    initCamera();
                } else {
                    // 권한 거부
                    // 사용자가 해당권한을 거부했을때 해주어야 할 동작을 수행합니다
                    finish();
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mScannerView != null) {
            mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
            mScannerView.startCamera();          // Start camera on resume
        }
        mCatchCount = 0;
        mTextView.setText(null);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mScannerView != null) {
            mScannerView.stopCamera();           // Stop camera on pause
        }
    }

    @Override
    public void handleResult(Result rawResult, int width, int height) {
        // Do something with the result here
        Log.v(TAG, rawResult.getText()); // Prints scan results
        Log.v(TAG, rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode, pdf417 etc.)
        mCatchCount++;

        mTextView.setText(mCatchCount + "-->" + rawResult.getText() + " / " + rawResult.getBarcodeFormat().toString());

        convertImage(rawResult.getRawBytes(), width, height);

//        mTextViewImageBytes.setText(data.toString());

        mButtonRetake.setEnabled(true);
        // If you would like to resume scanning, call this method below:
//        mScannerView.resumeCameraPreview(this);
    }

    private void convertImage(byte[] data, int width, int height) {
        ImageAsyncTask imageAsyncTask = new ImageAsyncTask(data, width, height);
        imageAsyncTask.execute();
    }

    private static class ImageAsyncTask extends AsyncTask<Void, Void, String> {

        private byte[] data;
        private int width, height;
        ImageAsyncTask(byte[] data, int width, int height) {
            this.data = data;
            this.width = width;
            this.height = height;
        }

        @Override
        protected String doInBackground(Void... voids) {
            Log.d(TAG, "doInBackground()");
            /*
            Bitmap original = BitmapFactory.decodeByteArray(data, 0, data.length);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            original.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();
            original.recycle();
            Rect rect = mScannerView.getMyViewFinderView().getFramingRect();
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            int width = getResources().getDisplayMetrics().widthPixels;
            int bitmapHeight = original.getHeight();
            Log.d(TAG, "bitmapHeight : " + bitmapHeight);
            float level = (float) bitmapHeight / (float) width;

            int cropX = (int) ((rect.top + getStatusBarHeight()) * level);
            int cropY = (int) (width - (rect.right - rect.left) * level);
            int cropWidth = (int) ((rect.bottom - rect.top));
            int cropHeight = (int) ((width - rect.left));
            Log.d(TAG, "rect > " + rect.toShortString());
            Log.d(TAG, "level : " + level + ", cropX : " + cropX + ", cropY : " + cropY + ", cropWidth : " + cropWidth + ", cropHeight : " + cropHeight);
            // left, top, right, bottom
            // [0,200][720,920]
            // level : 2.8, cropX : 795, cropY : -576, cropWidth : 2016, cropHeight : 4032
            Bitmap bitmap = Bitmap.createBitmap(original, cropX, cropY, cropWidth, cropHeight, matrix, false);

            if (data == null) {
                Log.e(TAG, "data is null.");
                return "data is null.";
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.outWidth = width;
            options.outHeight = height;

            Bitmap original = BitmapFactory.decodeByteArray(data, 0, data.length, options);

            if (original == null) {
                Log.e(TAG, "bitmap is null.");
                return "bitmap is null.";
            }
            */
            byte[] bytes = null;
//            try {
//                String fileName = String.format("%1$s/Pictures/KakaoTalk/image_%2$s.jpg",
//                        Environment.getExternalStorageDirectory().getPath(),
//                        new SimpleDateFormat("HHmmss", Locale.KOREA).format(new Date()));
//                String fileName = String.format("%1$s/Pictures/KakaoTalk/1524879081751.jpg",
//                        Environment.getExternalStorageDirectory().getPath());
//
//                Log.d(TAG, "file path : " + fileName);
//                File file = new File(fileName);
//                if (!file.exists()) {
//                    return "file doesn't exist.";
//                }
//                Bitmap bitmap = BitmapFactory.decodeFile(fileName);
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//                bytes = baos.toByteArray();

//                FileOutputStream fos = new FileOutputStream(fileName);
//                original.compress(Bitmap.CompressFormat.JPEG, 100, fos);

//            } catch (IOException e) {
//                Log.e(TAG, e.getMessage());
//                e.printStackTrace();
//            }

            if (bytes == null) {
                return "bytes is null.";
            }

//            String encodedString = Base64.encodeToString(bytes, Base64.DEFAULT);
//            return encodedString;
            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG, "onPostExecute()");
            Log.d(TAG, "--------------------------------------------------------------------------------");
            Log.d(TAG, s);
            Log.d(TAG, "--------------------------------------------------------------------------------");
        }
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
