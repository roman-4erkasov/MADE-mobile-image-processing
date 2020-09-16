package com.asav.test_pytorch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.ImageView;
import android.widget.TextView;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

  private static String TAG="TestPyTorchActivity";
  private static String[] CLASSES={"cat", "dog"};

  private Module module = null;
  private ImageView imageView=null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    String fileName="cat.2013.jpg"; //"dog.2061.jpg";//
    Bitmap bitmap = null;
    try {
      // creating bitmap from packaged into app android asset 'image.jpg',
      // app/src/main/assets/image.jpg
      bitmap = BitmapFactory.decodeStream(getAssets().open(fileName));
      // loading serialized torchscript module from packaged into app android asset model.pt,
      // app/src/model/assets/model.pt
      module = Module.load(assetFilePath(this, "catdog_model.pt"));
    } catch (IOException e) {
      Log.e(TAG, "Error reading assets", e);
      finish();
    }

    // showing image on UI
    imageView = findViewById(R.id.image);
    imageView.setImageBitmap(bitmap);

    Pair<String,Float> recResult =recognize(bitmap);
    String result="Predicted: "+recResult.first+ " for file: "+fileName;
    // showing className on UI
    TextView textView = findViewById(R.id.text);
    textView.setText(result);
  }

  /**
   * Copies specified asset to the file in /files app directory and returns this file absolute path.
   *
   * @return absolute file path
   */
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

  private Pair<String,Float> recognize(Bitmap bitmap){
    //Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
    bitmap=Bitmap.createScaledBitmap(bitmap, 256, 256, false);
    final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);

    final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

    final float[] scores = outputTensor.getDataAsFloatArray();
    float maxScore = -Float.MAX_VALUE;
    int maxScoreIdx = -1;
    for (int i = 0; i < scores.length; i++) {
      if (scores[i] > maxScore) {
        maxScore = scores[i];
        maxScoreIdx = i;
      }
    }
    String className = CLASSES[maxScoreIdx];
    return new Pair<String,Float>(className,maxScore);
  }
}
