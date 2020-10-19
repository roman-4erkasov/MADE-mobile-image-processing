package com.asav.processimage;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowMetrics;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.features2d.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.opencv.core.Core.DFT_SCALE;
import static org.opencv.core.CvType.CV_8U;

public class MainActivity extends AppCompatActivity {
    /** Tag for the {@link Log}. */
    private static final String TAG = "MainActivity";
    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private ImageView imageView;
    private Mat sampledImage=null;
    private GAPITester tester=null;
    private ArrayList<org.opencv.core.Point> corners=new ArrayList<org.opencv.core.Point>();

    private static native void niBlackThreshold(long matAddrIn, long matAddrOut);
    private static native void stitchImages(long matAddrIn1,long matAddrIn2, long matAddrOut);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        imageView=(ImageView)findViewById(R.id.inputImageView);
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                Log.i(TAG, "event.getX(), event.getY(): " + event.getX() +" "+ event.getY());
                if(sampledImage!=null) {
                    Log.i(TAG, "sampledImage.width(), sampledImage.height(): " + sampledImage.width() +" "+ sampledImage.height());
                    Log.i(TAG, "view.getWidth(), view.getHeight(): " + view.getWidth() +" "+ view.getHeight());
                    int left=(view.getWidth()-sampledImage.width())/2;
                    int top=(view.getHeight()-sampledImage.height())/2;
                    int right=(view.getWidth()+sampledImage.width())/2;
                    int bottom=(view.getHeight()+sampledImage.height())/2;
                    Log.i(TAG, "left: " + left +" right: "+ right +" top: "+ top +" bottom:"+ bottom);
                    if(event.getX()>=left && event.getX()<=right && event.getY()>=top && event.getY()<=bottom) {
                        int projectedX = (int)event.getX()-left;
                        int projectedY = (int)event.getY()-top;
                        org.opencv.core.Point corner = new org.opencv.core.Point(projectedX, projectedY);
                        corners.add(corner);
                        if(corners.size()>4)
                            corners.remove(0);
                        Mat sampleImageCopy=sampledImage.clone();
                        for(org.opencv.core.Point c : corners)
                            Imgproc.circle(sampleImageCopy, c, (int) 5, new Scalar(0, 0, 255), 2);
                        displayImage(sampleImageCopy);
                    }
                }
                return false;
            }
        });

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
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    System.loadLibrary("ImageProcessLib");
//                    if(tester==null)
//                        tester=new GAPITester();
                    Log.i(TAG, "After loading all libraries" );
                    Toast.makeText(getApplicationContext(),
                            "OpenCV loaded successfully",
                            Toast.LENGTH_SHORT).show();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                    Toast.makeText(getApplicationContext(),
                            "OpenCV error",
                            Toast.LENGTH_SHORT).show();
                } break;
            }
        }
    };
    private void init(){
    }
    @Override
    public void onResume()
    {
        super.onResume();
        if(tester==null) {
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
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

    private static final int SELECT_PICTURE = 1;
    private static final int SELECT_FROM_CAMERA = 2;
    private static final int SELECT_PICTURE_STITCHING = 3;

//    private void dispatchTakePictureIntent() {
//        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        try {
//            startActivityForResult(takePictureIntent, SELECT_FROM_CAMERA);
//
//        } catch (ActivityNotFoundException e) {
//            // display error state to the user
//            Toast.makeText(getApplicationContext(),
//                    "ActivityNotFoundException",
//                    Toast.LENGTH_SHORT).show();
//        }
//    }
    private Uri uriphoto;
    private String pictureFilePath;
//    private File createImageFile() throws IOException {
//        // Create an image file name
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES);
//        File image = File.createTempFile(
//                imageFileName,  /* prefix */
//                ".jpg",         /* suffix */
//                storageDir      /* directory */
//        );
//
////        // Save a file: path for use with ACTION_VIEW intents
////        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
//        return image;
//    }
    private File createCustomFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String pictureFile = "ZOFTINO_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(pictureFile,  ".jpg", storageDir);
        pictureFilePath = image.getAbsolutePath();
        return image;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_openGallery:
                corners.clear();
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"),
                        SELECT_PICTURE);
                return true;
            case R.id.action_camera:
                corners.clear();
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(cameraIntent, SELECT_FROM_CAMERA);

                    File pictureFile = null;
                    try {
                        pictureFile = createCustomFile();
                    } catch (IOException ex) {
                        Toast.makeText(this,
                                "Photo file can't be created, please try again",
                                Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    if (pictureFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(this,
                                "com.zoftino.android.fileprovider",
                                pictureFile);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(cameraIntent, SELECT_FROM_CAMERA);
                    }
                }
//                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
////                ContextWrapper context = new ContextWrapper(getApplicationContext());
//                // path to /data/data/yourapp/app_data/imageDir
////                File directory = context.getDir("imageDir", Context.MODE_PRIVATE);
//                File directory = Environment.getExternalStorageDirectory();
//                // Create imageDir
////                File photoFile = new File(directory,"photo.jpg");
//                if(!directory.exists()){
//                    directory.mkdirs();
//                }
//
//                uriphoto = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", new File(directory, "photo.jpg"));
////                File dir = new File("content://com.asav.processimage.provider/external_files/");
////                if(!dir.exists()){
////                    dir.mkdirs();
////                }
//
////                File photoFile = createCustomFile("photo");
////                uriphoto = Uri.fromFile(photoFile);
//                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriphoto);
//                startActivityForResult(takePictureIntent, SELECT_FROM_CAMERA);


//                String filename = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() + "/Download/testfile.jpg";
//                Uri imageUri = Uri.fromFile(new File(filename));
//                takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,
//                        imageUri);
//                startActivityForResult(Intent.createChooser(takePictureIntent,"Select Picture"),
//                        SELECT_FROM_CAMERA);
                return true;

            case R.id.action_grayscale:
                if(isImageLoaded()) {
                    grayscale();
                }
                return true;
            case R.id.action_median:
                if(isImageLoaded()) {
                    median();
                }
                return true;
            case R.id.action_bilateral:
                if(isImageLoaded()) {
                    bilateral();
                }
                return true;
            case R.id.action_edgedetector:
                if(isImageLoaded()) {
                    edgedetector();
                }
                return true;
            case R.id.action_canny:
                if(isImageLoaded()) {
                    canny();
                }
                return true;
            case R.id.action_binary:
                if(isImageLoaded()) {
                    binary();
                }
                return true;
            case R.id.action_contrast:
                if(isImageLoaded()) {
                    contrast();
                }
                return true;
            case R.id.action_gamma:
                if(isImageLoaded()) {
                    gammaCorrection();
                }
                return true;
            case R.id.action_equalizehisto:
                if(isImageLoaded()) {
                    equalizeHisto();
                }
                return true;
            case R.id.action_blur:
                if(isImageLoaded()) {
                    blur();
                }
                return true;
            case R.id.action_linesdetector:
                if(isImageLoaded()) {
                    linesDetector();
                }
                return true;
            case R.id.action_transformer:
                if(isImageLoaded()) {
                    perspectiveTransform();
                }
                return true;
            case R.id.action_transformer_auto:
                if(isImageLoaded()) {
                    autoPerspectiveTransform();
                }
                return true;

            case R.id.action_stitchimages:
                if(isImageLoaded()) {
                    corners.clear();
                    Intent stitch_intent = new Intent();
                    stitch_intent.setType("image/*");
                    stitch_intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(stitch_intent,"Select Picture"),
                            SELECT_PICTURE_STITCHING);
                }
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }
    private boolean isImageLoaded(){
        if(sampledImage==null)
            Toast.makeText(getApplicationContext(),
                    "It is necessary to open image firstly",
                    Toast.LENGTH_SHORT).show();
        return sampledImage!=null;
    }

//    private Bitmap scaleBitmap(Bitmap bm, int maxWidth, int maxHeight) {
//            int width = bm.getWidth();
//            int height = bm.getHeight();
//
//            if (width > height) {
//            // landscape
//            float ratio = width / maxWidth;
//            width = maxWidth;
//            height = (int)(height / ratio);
//            } else if (height > width) {
//            // portrait
//            float ratio = height / maxHeight;
//            height = maxHeight;
//            width = (int)(width / ratio);
//            } else {
//            // square
//            height = maxHeight;
//            width = maxWidth;
//            }
//
//            bm = Bitmap.createScaledBitmap(bm, width, height, true);
//            return bm;
//    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SELECT_PICTURE && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData(); //The uri with the location of the file
            Log.d(TAG,"uri"+selectedImageUri);
            sampledImage=convertToMat(selectedImageUri);
            if(sampledImage!=null)
                displayImage(sampledImage);
        }
        else if (requestCode == SELECT_FROM_CAMERA && resultCode == RESULT_OK) {
//            String filename = Environment.getExternalStorageDirectory().getPath() + "/Download/testfile.jpg";
//            Uri imageUri = Uri.fromFile(new File(filename));
//            convertToMat(imageUri);
            File imgFile = new File(pictureFilePath);
            if (imgFile.exists()) {
                uriphoto = Uri.fromFile(imgFile);
                sampledImage = convertToMat(uriphoto);
                if (sampledImage != null)
                    displayImage(sampledImage);
            }

//            Bitmap photo = (Bitmap) data.getExtras().get("data");
//            convertToMat(photo);

//            imageView.setImageBitmap(photo);
        }
        else if(requestCode==SELECT_PICTURE_STITCHING && resultCode == RESULT_OK) {
            Uri selectedImageUri = data.getData(); //The uri with the location of the file
            Mat image2 = convertToMat(selectedImageUri);
            Mat resImage = new Mat();
            long startTime = System.nanoTime();
            stitchImages(sampledImage.getNativeObjAddr(), image2.getNativeObjAddr(), resImage.getNativeObjAddr());
            long elapsedTime = System.nanoTime() - startTime;
            elapsedTime = elapsedTime / 1000000; // Milliseconds (1:1000000)
            Log.i(this.getClass().getSimpleName(), "OpenCV Stitching 2 images took " + elapsedTime + "ms");
            if (resImage.rows() <= 0 || resImage.cols() <= 0) {
                Toast.makeText(getApplicationContext(),
                        "Panorama not found",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            // can do panorama with many photos
            sampledImage = resImage;
            displayImage(sampledImage);
        }
    }
    private Mat convertToMat(Uri selectedImageUri)
    {
        Mat resImage=null;
        try {
            InputStream ims = getContentResolver().openInputStream(selectedImageUri);
            Bitmap bmp=BitmapFactory.decodeStream(ims);
            Mat rgbImage=new Mat();
            Utils.bitmapToMat(bmp, rgbImage);
            ims.close();
            ims = getContentResolver().openInputStream(selectedImageUri);
            ExifInterface exif = new ExifInterface(ims);//selectedImageUri.getPath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    1);
            switch (orientation)
            {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    //get the mirrored image
                    rgbImage=rgbImage.t();
                    //flip on the y-axis
                    Core.flip(rgbImage, rgbImage, 1);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    //get up side down image
                    rgbImage=rgbImage.t();
                    //Flip on the x-axis
                    Core.flip(rgbImage, rgbImage, 0);
                    break;
            }

            Display display = getWindowManager().getDefaultDisplay();
            android.graphics.Point size = new android.graphics.Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;
            double downSampleRatio= calculateSubSampleSize(rgbImage,width,height);
            resImage=new Mat();
            Imgproc.resize(rgbImage, resImage, new
                    Size(),downSampleRatio,downSampleRatio,Imgproc.INTER_AREA);
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown: " + e+" "+Log.getStackTraceString(e));
            resImage=null;
        }
        return resImage;
    }
    private static double calculateSubSampleSize(Mat srcImage, int reqWidth,
                                                 int reqHeight) {
        final int height = srcImage.height();
        final int width = srcImage.width();
        double inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final double heightRatio = (double) reqHeight / (double) height;
            final double widthRatio = (double) reqWidth / (double) width;
            inSampleSize = heightRatio<widthRatio ? heightRatio :widthRatio;
        }
        return inSampleSize;
    }
    private void grayscale(){
        Mat grayImage=new Mat();
        Imgproc.cvtColor(sampledImage,grayImage, Imgproc.COLOR_RGB2GRAY);
        displayImage(grayImage);
    }
    private void binary(){
        Mat binImage = new Mat();
        if(true) {
            Mat grayImage = new Mat();
            Imgproc.cvtColor(sampledImage, grayImage, Imgproc.COLOR_RGB2GRAY);
            Imgproc.GaussianBlur(grayImage,grayImage,new Size(5,5),0,0);
            // адаптивный в нашем случае должен отрабатывать лучше всего:
            // затрет фон вокруг документа(нет резких локальных переходов) + чернила выделяются четче чем клетки страницы
            Imgproc.adaptiveThreshold(grayImage, binImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 10);
            //Imgproc.threshold(grayImage,binImage,128,255,Imgproc.THRESH_BINARY);
//            Imgproc.threshold(grayImage,binImage,0,255,Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);
        }
        else{
            niBlackThreshold(sampledImage.getNativeObjAddr(),binImage.getNativeObjAddr());
        }
        displayImage(binImage);
    }
    private final boolean useColor=true;
    private void contrast(){
        Mat grayImage=new Mat();
        Imgproc.cvtColor(sampledImage,grayImage, Imgproc.COLOR_RGB2GRAY);
        Mat out=new Mat();
        if(useColor){
            Mat HSV=new Mat();
            Imgproc.cvtColor(sampledImage, HSV, Imgproc.COLOR_RGB2HSV);
            ArrayList<Mat> hsv_list = new ArrayList(3);
            Core.split(HSV,hsv_list);

            for(int channel=1;channel<=2;++channel) {
                Core.MinMaxLocResult minMaxLocRes = Core.minMaxLoc(hsv_list.get(channel));
                double minVal = minMaxLocRes.minVal;//+20;
                double maxVal = minMaxLocRes.maxVal;//-50;
                Mat corrected = new Mat();
                hsv_list.get(channel).convertTo(corrected, CV_8U, 255.0 / (maxVal - minVal), -minVal * 255.0 / (maxVal - minVal));
                hsv_list.set(channel, corrected);
            }
            Core.merge(hsv_list,HSV);
            Imgproc.cvtColor(HSV, out, Imgproc.COLOR_HSV2RGB);
        }
        else {
            Core.MinMaxLocResult minMaxLocRes = Core.minMaxLoc(grayImage);
            double minVal = minMaxLocRes.minVal;//+20;
            double maxVal = minMaxLocRes.maxVal;//-50;
            grayImage.convertTo(out, CV_8U, 255.0 / (maxVal - minVal), -minVal * 255.0 / (maxVal - minVal));
        }
        displayImage(out);
    }
    private void gammaCorrection(){
        double gammaValue = 1.3;
        Mat lookUpTable = new Mat(1, 256, CV_8U);
        byte[] lookUpTableData = new byte[(int) (lookUpTable.total() * lookUpTable.channels())];
        for (int i = 0; i < lookUpTable.cols(); i++) {
            lookUpTableData[i] = saturate(Math.pow(i / 255.0, gammaValue) * 255.0);
        }
        lookUpTable.put(0, 0, lookUpTableData);

        Mat out=new Mat();
        if(useColor){
            Mat HSV=new Mat();
            Imgproc.cvtColor(sampledImage, HSV, Imgproc.COLOR_RGB2HSV);
            ArrayList<Mat> hsv_list = new ArrayList(3);
            Core.split(HSV,hsv_list);

            for(int channel=1;channel<=2;++channel) {
                Mat corrected = new Mat();
                Core.LUT(hsv_list.get(channel), lookUpTable, corrected);
                hsv_list.set(channel, corrected);
            }
            Core.merge(hsv_list,HSV);
            Imgproc.cvtColor(HSV, out, Imgproc.COLOR_HSV2RGB);
        }
        else {
            Mat grayImage = new Mat();
            Imgproc.cvtColor(sampledImage, grayImage, Imgproc.COLOR_RGB2GRAY);

            Core.LUT(grayImage, lookUpTable, out);
        }
        displayImage(out);
    }
    private byte saturate(double val) {
        int iVal = (int) Math.round(val);
        iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);
        return (byte) iVal;
    }
    private void equalizeHisto(){
        Mat out=new Mat();
        if(useColor){
            Mat HSV=new Mat();
            Imgproc.cvtColor(sampledImage, HSV, Imgproc.COLOR_RGB2HSV);
            ArrayList<Mat> hsv_list = new ArrayList(3);
            Core.split(HSV,hsv_list);
            for(int channel=1;channel<=2;++channel) {
                Mat equalizedValue = new Mat();
                Imgproc.equalizeHist(hsv_list.get(channel), equalizedValue);
                hsv_list.set(channel, equalizedValue);
            }
            Core.merge(hsv_list,HSV);
            Imgproc.cvtColor(HSV, out, Imgproc.COLOR_HSV2RGB);
        }
        else {
            Imgproc.cvtColor(sampledImage, out, Imgproc.COLOR_RGB2GRAY);
            Imgproc.equalizeHist(out, out);
        }
        displayImage(out);
    }
    private void blur(){
        Mat out=new Mat();
        //Imgproc.cvtColor(sampledImage,out, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(sampledImage,out,new Size(7,7),0,0);
        displayImage(out);
    }
    private Mat getNoisyImage(boolean add_noise){
        Mat noisyImage;
        if(add_noise) {
            Mat noise = new Mat(sampledImage.size(), sampledImage.type());
            MatOfDouble mean = new MatOfDouble ();
            MatOfDouble dev = new MatOfDouble ();
            Core.meanStdDev(sampledImage,mean,dev);
            Core.randn(noise,0, 1*dev.get(0,0)[0]);
            noisyImage = new Mat();
            Core.add(sampledImage, noise, noisyImage);
        }
        else{
            noisyImage=sampledImage;
        }
        return noisyImage;
    }
    private void median(){
        Mat noisyImage=getNoisyImage(true);
        Mat blurredImage=new Mat();
        Imgproc.medianBlur(noisyImage,blurredImage, 7);
        displayImage(blurredImage);
    }
    private void bilateral(){
        Mat noisyImage=getNoisyImage(false);
        Mat outImage=new Mat();
        Mat rgb=new Mat();
        Imgproc.cvtColor(noisyImage, rgb, Imgproc.COLOR_RGBA2RGB);
        Imgproc.bilateralFilter(rgb,outImage,9,75,75);
        displayImage(outImage);
    }
    private void edgedetector(){
        Mat grayImage=new Mat();
        Imgproc.cvtColor(sampledImage,grayImage, Imgproc.COLOR_RGB2GRAY);

        Mat xFirstDervative =new Mat(),yFirstDervative =new Mat();
        Imgproc.Scharr(grayImage, xFirstDervative,-1 , 1,0);
        Imgproc.Scharr(grayImage, yFirstDervative,-1 , 0,1);
        Mat absXD=new Mat(),absYD=new Mat();
        Core.convertScaleAbs(xFirstDervative, absXD);
        Core.convertScaleAbs(yFirstDervative, absYD);
        Mat edgeImage=new Mat();
        double alpha=0.5;
        Core.addWeighted(absXD, alpha, absYD, 1-alpha, 0, edgeImage);
        displayImage(edgeImage);
    }
    private void canny(){
        Mat grayImage=new Mat();
        Imgproc.cvtColor(sampledImage,grayImage, Imgproc.COLOR_RGB2GRAY);
        Mat edgeImage=new Mat();
        Imgproc.Canny(grayImage, edgeImage, 100, 200);
        displayImage(edgeImage);
    }

    private void test_gapi(){
        Mat outImage =null;
        long startTime = SystemClock.uptimeMillis();
        if(false) {
            if (tester != null)
                outImage = tester.processImage(sampledImage);
        }
        else{
            outImage =new Mat();
            Imgproc.resize(sampledImage, outImage,new Size(),0.5,0.5);
            Mat tmpImg=new Mat();
            Imgproc.cvtColor(outImage,tmpImg,Imgproc.COLOR_RGB2GRAY);
            Imgproc.blur(tmpImg,tmpImg,new Size(5,5));
            Mat edges=new Mat();
            Imgproc.Canny(tmpImg,edges, 32, 128,3);
            ArrayList<Mat> rgb_list = new ArrayList(3);
            Core.split(outImage,rgb_list);
            Core.bitwise_or(rgb_list.get(1),edges,edges);
            rgb_list.set(1,edges);
            Core.merge(rgb_list,outImage);

        }
        //Mat outImage = new Mat();
        //gapiTest(rgb.getNativeObjAddr(),outImage.getNativeObjAddr());
        Log.i(TAG, "Timecost to process image in G-API: " + Long.toString(SystemClock.uptimeMillis() - startTime));
        displayImage(outImage);
    }

    private void linesDetector(){
        Mat grayImage=new Mat();
        Imgproc.cvtColor(sampledImage, grayImage, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(grayImage, grayImage, 100, 200);
        Mat lines = new Mat();
        if(true) {
            int threshold = 150;
            Imgproc.HoughLinesP(grayImage, lines, 2, 2*Math.PI / 180, threshold);
            Imgproc.cvtColor(grayImage, grayImage, Imgproc.COLOR_GRAY2RGB);
            Log.w(TAG, "rows:" + lines.rows() + " cols:" + lines.cols());
            for (int i = 0; i < lines.rows(); i++) {
                double[] line = lines.get(i, 0);
                double xStart = line[0],
                        yStart = line[1],
                        xEnd = line[2],
                        yEnd = line[3];
                org.opencv.core.Point lineStart = new org.opencv.core.Point(xStart,
                        yStart);
                org.opencv.core.Point lineEnd = new org.opencv.core.Point(xEnd, yEnd);
                Imgproc.line(grayImage, lineStart, lineEnd, new Scalar(0, 255, 0), 3);
            }
        }
        else{
            int threshold = 200;
            Imgproc.HoughLines(grayImage, lines, 2, 2*Math.PI / 180, threshold);
            Imgproc.cvtColor(grayImage, grayImage, Imgproc.COLOR_GRAY2RGB);
            Log.w(TAG, "rows:" + lines.rows() + " cols:" + lines.cols());
            for (int i = 0; i < lines.rows(); i++) {
                double[] data = lines.get(i, 0);
                double rho1 = data[0];
                double theta1 = data[1];
                double cosTheta = Math.cos(theta1);
                double sinTheta = Math.sin(theta1);
                double x0 = cosTheta * rho1;
                double y0 = sinTheta * rho1;
                int line_len=100;
                Point pt1 = new Point(x0 + line_len * (-sinTheta), y0 + line_len * cosTheta);
                Point pt2 = new Point(x0 - line_len * (-sinTheta), y0 - line_len * cosTheta);
                Imgproc.line(grayImage, pt1, pt2, new Scalar(0, 255, 0), 2);
            }
        }
        displayImage(grayImage);
    }

    private void displayImage(Mat image)
    {
        Bitmap bitmap = Bitmap.createBitmap(image.cols(),
                image.rows(),Bitmap.Config.RGB_565);
        Utils.matToBitmap(image, bitmap);
        displayImage(bitmap);
    }
    private void displayImage(Bitmap bitmap)
    {
        imageView.setImageBitmap(bitmap);
    }
    private void autoPerspectiveTransform(){
        // Edge Detection
        Mat grayImage=new Mat();
        Imgproc.cvtColor(sampledImage,grayImage, Imgproc.COLOR_RGB2GRAY);
        Imgproc.blur(grayImage,grayImage,new Size(3,3));
        Mat edgeImage=new Mat();
        Imgproc.Canny(grayImage, edgeImage, 65, 130);

        // Finding Contours
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edgeImage, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // Draw Contours
        Mat drawing = Mat.zeros(edgeImage.size(), CvType.CV_8UC3);
        Random rng = new Random(12345);
        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(rng.nextInt(256), rng.nextInt(256), rng.nextInt(256));
            Imgproc.drawContours(drawing, contours, i, color, 2, 8, hierarchy, 0, new Point());
        }
        displayImage(drawing);




//        outImage =new Mat();
//        Imgproc.resize(sampledImage, outImage,new Size(),0.5,0.5);
//        Mat tmpImg=new Mat();
//        Imgproc.cvtColor(outImage,tmpImg,Imgproc.COLOR_RGB2GRAY);
//        Imgproc.blur(tmpImg,tmpImg,new Size(5,5));
//        Mat edges=new Mat();
//        Imgproc.Canny(tmpImg,edges, 32, 128,3);
//        ArrayList<Mat> rgb_list = new ArrayList(3);
//        Core.split(outImage,rgb_list);
//        Core.bitwise_or(rgb_list.get(1),edges,edges);
//        rgb_list.set(1,edges);
//        Core.merge(rgb_list,outImage);

    }
    private void perspectiveTransform(){
        if(corners.size()<4){
            Toast.makeText(getApplicationContext(),
                    "It is necessary to choose 4 corners",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        org.opencv.core.Point centroid=new org.opencv.core.Point(0,0);
        for(org.opencv.core.Point point:corners)
        {
            centroid.x+=point.x;
            centroid.y+=point.y;
        }
        centroid.x/=corners.size();
        centroid.y/=corners.size();

        sortCorners(corners,centroid);
        Mat correctedImage=new Mat(sampledImage.rows(),sampledImage.cols(),sampledImage.type());
        Mat srcPoints= Converters.vector_Point2f_to_Mat(corners);

        Mat destPoints=Converters.vector_Point2f_to_Mat(Arrays.asList(new org.opencv.core.Point[]{
                new org.opencv.core.Point(0, 0),
                new org.opencv.core.Point(correctedImage.cols(), 0),
                new org.opencv.core.Point(correctedImage.cols(),correctedImage.rows()),
                new org.opencv.core.Point(0,correctedImage.rows())}));

        Mat transformation=Imgproc.getPerspectiveTransform(srcPoints, destPoints);
        Imgproc.warpPerspective(sampledImage, correctedImage, transformation, correctedImage.size());

        corners.clear();
        displayImage(correctedImage);
    }
    void sortCorners(ArrayList<Point> corners, org.opencv.core.Point center)
    {
        ArrayList<org.opencv.core.Point> top=new ArrayList<org.opencv.core.Point>();
        ArrayList<org.opencv.core.Point> bottom=new ArrayList<org.opencv.core.Point>();

        for (int i = 0; i < corners.size(); i++)
        {
            if (corners.get(i).y < center.y)
                top.add(corners.get(i));
            else
                bottom.add(corners.get(i));
        }

        double topLeft=top.get(0).x;
        int topLeftIndex=0;
        for(int i=1;i<top.size();i++)
        {
            if(top.get(i).x<topLeft)
            {
                topLeft=top.get(i).x;
                topLeftIndex=i;
            }
        }

        double topRight=0;
        int topRightIndex=0;
        for(int i=0;i<top.size();i++)
        {
            if(top.get(i).x>topRight)
            {
                topRight=top.get(i).x;
                topRightIndex=i;
            }
        }

        double bottomLeft=bottom.get(0).x;
        int bottomLeftIndex=0;
        for(int i=1;i<bottom.size();i++)
        {
            if(bottom.get(i).x<bottomLeft)
            {
                bottomLeft=bottom.get(i).x;
                bottomLeftIndex=i;
            }
        }

        double bottomRight=bottom.get(0).x;
        int bottomRightIndex=0;
        for(int i=1;i<bottom.size();i++)
        {
            if(bottom.get(i).x>bottomRight)
            {
                bottomRight=bottom.get(i).x;
                bottomRightIndex=i;
            }
        }

        org.opencv.core.Point topLeftPoint = top.get(topLeftIndex);
        org.opencv.core.Point topRightPoint = top.get(topRightIndex);
        org.opencv.core.Point bottomLeftPoint = bottom.get(bottomLeftIndex);
        org.opencv.core.Point bottomRightPoint = bottom.get(bottomRightIndex);

        corners.clear();
        corners.add(topLeftPoint);
        corners.add(topRightPoint);
        corners.add(bottomRightPoint);
        corners.add(bottomLeftPoint);
    }
}