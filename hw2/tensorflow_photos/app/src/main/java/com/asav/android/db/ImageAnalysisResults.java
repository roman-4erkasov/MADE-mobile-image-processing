package com.asav.android.db;

import com.asav.android.FaceData;

import java.io.Serializable;

/**
 * Created by avsavchenko.
 */
public class ImageAnalysisResults implements Serializable {
    public String filename=null;
    public SceneData scene=null;
    public FaceData face=null;
    public EXIFData locations=null;

    public ImageAnalysisResults() {}

    public ImageAnalysisResults(String filename, SceneData scene, FaceData face, EXIFData locations){
        this.filename=filename;
        this.scene=scene;
        this.face = face;
        this.locations=locations;
    }
    public ImageAnalysisResults(SceneData scene){
        this.scene=scene;
    }
    public ImageAnalysisResults(FaceData face){ this.face=face; }
}
