package com.asav.android;


import com.asav.android.mtcnn.Box;

public class FaceFeatures{
    public FaceFeatures(FaceData data, Box b){
        faceData=data;
        box=b;
    }
    public FaceData faceData;
    public Box box;
}