package com.asav.android;

import com.asav.android.db.ImageAnalysisResults;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class FeaturesPoint implements Clusterable {
    public ImageAnalysisResults results;
    public float[] features;

    public FeaturesPoint(ImageAnalysisResults r, float[] f)
    {
        results = r;
        features = f;
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