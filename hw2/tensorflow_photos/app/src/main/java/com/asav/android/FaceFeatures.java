package com.asav.android;


import com.asav.android.mtcnn.Box;

import java.io.Serializable;

public class FaceFeatures implements Serializable {
    public FaceFeatures(FaceData data, Box b){
        faceData=data;
        box=b;
    }
    public FaceData faceData;
    public Box box;
}