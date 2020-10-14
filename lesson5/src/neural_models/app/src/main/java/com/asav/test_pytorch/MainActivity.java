package com.asav.test_pytorch;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.IntStream;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

  private static String TAG="MainActivity";
  private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

  private List<Module> modules=new ArrayList<>();
  private List<String> labels;

  private ImageView imageView=null;
  private TextView textView;
  private Spinner modelsSpinner;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    imageView = findViewById(R.id.image);
    textView = findViewById(R.id.text);
    textView.setMovementMethod(new ScrollingMovementMethod());
    modelsSpinner=findViewById(R.id.models_spinner);

    loadLabels();
    modules.clear();
    List<String> modelNames=new ArrayList<>();
    try {
      for (String asset : getAssets().list("")){
        if(asset.toLowerCase().endsWith(".pt")){
          modules.add(Module.load(assetFilePath(this, asset)));
          String modelName=asset.substring(0,asset.length()-3);
          modelNames.add(modelName);
        }
      }
    } catch (IOException e) {
      Log.e(TAG, "Error reading assets: " + e+" "+Log.getStackTraceString(e));
    }
    String modelNamesArr[]=new String[modelNames.size()];
    modelNames.toArray(modelNamesArr);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.spinner_item, modelNamesArr);
    modelsSpinner.setAdapter(adapter);
    modelsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> parent,
                                 View itemSelected, int selectedItemPosition, long selectedId) {
        BitmapDrawable drawable=(BitmapDrawable)imageView.getDrawable();
        if(drawable!=null) {
          Bitmap bmp = drawable.getBitmap();
          String result = recognize(bmp);
          textView.setText(result);
        }
      }

      public void onNothingSelected(AdapterView<?> parent) {
      }
    });
  Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
    //setSupportActionBar(toolbar);
    if (!allPermissionsGranted()) {
      ActivityCompat.requestPermissions(this, getRequiredPermissions(), REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
    }

  }
  private void loadLabels(){
    BufferedReader br = null;
    labels=new ArrayList<>();
    try {
      br = new BufferedReader(new InputStreamReader(getAssets().open("imagenet_labels.txt")));
      String line;
      int line_ind=0;
      while ((line = br.readLine()) != null) {
        ++line_ind;
        //line=line.toLowerCase();
        String[] categoryInfo=line.trim().split(":");
        String category=categoryInfo[1];
        labels.add(category);
      }
      br.close();
    } catch (IOException e) {
      throw new RuntimeException("Problem reading event label file!" , e);
    }
  }
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.toolbar_menu, menu);
    return true;
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
      int status= ContextCompat.checkSelfPermission(this,permission);
      if (ContextCompat.checkSelfPermission(this,permission)
              != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
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
          //init();
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

  public static String assetFilePath(Context context, String assetName) throws IOException {
    File file = new File(context.getFilesDir(), assetName);
    if (file.exists() && file.length() > 0) {
      return file.getAbsolutePath();
    }

    try (InputStream is = context.getAssets().open(assetName)) {
      try (OutputStream os = new FileOutputStream(file)) {
        byte[] buffer = new byte[4 * 1024];
        int read;
        while ((read = is.read(buffer)) != -1) {
          os.write(buffer, 0, read);
        }
        os.flush();
      }
      return file.getAbsolutePath();
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

      imageView.setImageBitmap(bmp);
      String result=recognize(bmp);
      textView.setText(result);
    } catch (Exception e) {
      Log.e(TAG, "Exception thrown: " + e+" "+Log.getStackTraceString(e));
    }
  }
  private String recognize(Bitmap bitmap){
    //imageView.getImageB
    bitmap=Bitmap.createScaledBitmap(bitmap, 224, 224, false);
    final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
    long startTime = SystemClock.uptimeMillis();
    final Tensor outputTensor = modules.get(modelsSpinner.getSelectedItemPosition()).forward(IValue.from(inputTensor)).toTensor();
    Log.i(TAG, "Timecost to run model inference: " + Long.toString(SystemClock.uptimeMillis() - startTime));

    final float[] scores = outputTensor.getDataAsFloatArray();
    Integer index[] = new Integer[scores.length];
    for (int i = 0; i < scores.length; i++) {
      index[i]=i;
    }

    Arrays.sort(index, new Comparator<Integer>() {
      @Override
      public int compare(Integer idx1, Integer idx2) {
        return Float.compare(scores[idx2],scores[idx1]);
      }
    });
    int K=5;
    StringBuilder str=new StringBuilder();
    for(int i=0;i<K;++i){
      str.append(labels.get(index[i])+" "+String.valueOf(index[i])+" "+String.valueOf(scores[index[i]])+"\n");
    }
    return str.toString();//labels.get(index[0]);
  }
}
