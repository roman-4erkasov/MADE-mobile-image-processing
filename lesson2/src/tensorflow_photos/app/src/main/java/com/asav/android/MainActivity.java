package com.asav.android;


import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.asav.android.db.SceneData;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Created by avsavchenko.
 */

public class MainActivity extends AppCompatActivity {

    /** Tag for the {@link Log}. */
    private static final String TAG = "MainActivity";
    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private ScenesTfLiteClassifier scenesClassifier;
    private ImageView imageView;
    private TextView recResultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        imageView=(ImageView)findViewById(R.id.photoView);
        recResultTextView=(TextView)findViewById(R.id.recResultTextView);
        recResultTextView.setMovementMethod(new ScrollingMovementMethod());

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, getRequiredPermissions(), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        }
        else
            init();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    private void init(){
        try {
            scenesClassifier = new ScenesTfLiteClassifier(getAssets());
        } catch (IOException e) {
            Log.e(TAG, "Failed to load ScenesTfClassifier.", e);
        }
    }
    private static final int SELECT_PICTURE = 1;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_openGallery:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"),
                        SELECT_PICTURE);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SELECT_PICTURE && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData(); //The uri with the location of the file
            Log.d(TAG,"uri"+selectedImageUri);
            //imageView.setImageURI(selectedImageUri);
            /*String path=getPath1(selectedImageUri);
            Log.d(TAG,"path "+path);*/

            processImage(selectedImageUri);
        }
    }
    private String classifyScenes(Bitmap bmp) {
        long startTime = SystemClock.uptimeMillis();
        Bitmap scenesBitmap = Bitmap.createScaledBitmap(bmp, scenesClassifier.getImageSizeX(), scenesClassifier.getImageSizeY(), false);
        SceneData scene = (SceneData) scenesClassifier.classifyFrame(scenesBitmap);
        long sceneTimeCost = SystemClock.uptimeMillis() - startTime;
        Log.i(TAG, "Timecost to run scene model inference: " + Long.toString(sceneTimeCost));
        StringBuilder text=new StringBuilder();
        text.append(scene).append("\n").append("Scenes classification time:").append(sceneTimeCost).append(" ms\n");
        return text.toString();
    }

    private void processImage(Uri selectedImageUri)
    {
        try {
            InputStream ims = getContentResolver().openInputStream(selectedImageUri);
            Bitmap bmp= BitmapFactory.decodeStream(ims);
            ims.close();
            ims = getContentResolver().openInputStream(selectedImageUri);
            ExifInterface exif = new ExifInterface(ims);//selectedImageUri.getPath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
            int degreesForRotation=0;
            switch (orientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degreesForRotation=90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degreesForRotation=270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degreesForRotation=180;
                    break;
            }
            if(degreesForRotation!=0) {
                Matrix matrix = new Matrix();
                matrix.setRotate(degreesForRotation);
                bmp=Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(),
                        bmp.getHeight(), matrix, true);
            }

            String result=classifyScenes(bmp);
            recResultTextView.setText(result);
            imageView.setImageBitmap(bmp);

            /*runOnUiThread(new Runnable() {
                @Override
                public void run() {
                }
            });*/
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown: " + e+" "+Log.getStackTraceString(e));
        }
    }


    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                   getPackageManager()
                            .getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }
    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            int status=ContextCompat.checkSelfPermission(this,permission);
            if (ContextCompat.checkSelfPermission(this,permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
                Map<String, Integer> perms = new HashMap<String, Integer>();
                boolean allGranted = true;
                for (int i = 0; i < permissions.length; i++) {
                    perms.put(permissions[i], grantResults[i]);
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                        allGranted = false;
                }
                // Check for ACCESS_FINE_LOCATION
                if (allGranted) {
                    // All Permissions Granted
                    init();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Some Permission is Denied", Toast.LENGTH_SHORT)
                            .show();
                    finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
