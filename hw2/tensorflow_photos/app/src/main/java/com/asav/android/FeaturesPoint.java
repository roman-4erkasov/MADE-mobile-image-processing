package com.asav.android;

import com.asav.android.db.ImageAnalysisResults;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class FeaturesPoint implements Clusterable {
    public ImageAnalysisResults results;
    public float[] features;
    public FaceFeatures face;

    public FeaturesPoint(ImageAnalysisResults r, float[] ft, FaceFeatures fc)
    {
        results = r;
        features = ft;
        face = fc;
    }

    public double[] getPoint()
    {
        double[] d_features = new double[features.length];
        for ( int i = 0 ; i < features.length; i++) {
            d_features[i] = features[i];
        }
        return d_features;
    }
}