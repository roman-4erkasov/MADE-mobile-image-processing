package com.asav.android.db;

import com.asav.android.FaceData;
import com.asav.android.FaceFeatures;
import com.asav.android.PhotoProcessor;

import java.io.Serializable;
import java.util.List;

/**
 * Created by avsavchenko.
 */
public class ImageAnalysisResults implements Serializable {
    public String filename=null;
    public SceneData scene=null;
    public List<FaceFeatures> faceFeatures=null;
    public EXIFData locations=null;

    public ImageAnalysisResults() {}

    public ImageAnalysisResults(String filename, SceneData scene, List<FaceFeatures> faceFeatures, EXIFData locations){
        this.filename=filename;
        this.scene=scene;
        this.faceFeatures = faceFeatures;
        this.locations=locations;
    }
    public ImageAnalysisResults(SceneData scene){
        this.scene=scene;
    }
    public ImageAnalysisResults(List<FaceFeatures> faceFeatures){ this.faceFeatures=faceFeatures; }
}
