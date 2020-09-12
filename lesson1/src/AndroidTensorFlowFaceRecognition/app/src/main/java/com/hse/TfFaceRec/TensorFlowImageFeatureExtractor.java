
package com.hse.TfFaceRec;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.IOException;


/**
 * A classifier specialized to extract image features using TensorFlow.
 */
public class TensorFlowImageFeatureExtractor {

    private static final String TAG = "TFImageFeatureExtractor";

    // Config values.
    private String inputName;
    private String isTrainingName;
    private int inputSize;

    // Pre-allocated buffers.
    private int[] intValues;
    private boolean[] isTrainingValues;
    private float[] floatValues;
    private float[][] outputs;
    private String[] outputNames;

    private boolean logStats = false;

    private TensorFlowInferenceInterface inferenceInterface;

    private TensorFlowImageFeatureExtractor() {
    }

    public static TensorFlowImageFeatureExtractor create(
            AssetManager assetManager,
            String modelFilename,
            int inputSize,
            String inputName,
            String[] outputNames)
            throws IOException {
        TensorFlowImageFeatureExtractor c = new TensorFlowImageFeatureExtractor();
        c.inputName = inputName;
        c.outputNames = outputNames;

        c.inferenceInterface = new TensorFlowInferenceInterface(assetManager,modelFilename);

        // Ideally, inputSize could have been retrieved from the shape of the input operation.  Alas,
        // the placeholder node for input in the graphdef typically used does not specify a shape, so it
        // must be passed in as a parameter.
        c.inputSize = inputSize;

        // Pre-allocate buffers.
        c.intValues = new int[inputSize * inputSize];
        c.floatValues = new float[inputSize * inputSize * 3];

        c.outputs = new float[outputNames.length][];
        for(int i=0;i<outputNames.length;++i) {
            String featureOutputName = outputNames[i];
            // The shape of the output is [N, NUM_OF_FEATURES], where N is the batch size.
            int numOFFeatures = (int) c.inferenceInterface.graph().operation(featureOutputName).output(0).shape().size(1);
            Log.i(TAG, "Read output layer size is " + numOFFeatures);
            c.outputs[i] = new float[numOFFeatures];
        }
        return c;
    }

    public float[][] recognizeImage(final Bitmap bitmap) {
        long start=System.currentTimeMillis();
        //Log.i(TAG,"1!!!!!!!!!!!!!!!!!!!!!!!!! start feature extraction");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            //'RGB'->'BGR'
            floatValues[i * 3 + 0] = ((val & 0xFF) - 103.939f);
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - 116.779f);
            floatValues[i * 3 + 2] = (((val >> 16) & 0xFF) - 123.68f);
        }

        // Copy the input data into TensorFlow.
        //Trace.beginSection("fillNodeFloat");
        inferenceInterface.feed(inputName, floatValues, 1, inputSize, inputSize, 3);

        //Log.i(TAG,"2!!!!!!!!!!!!!!!!!!!!!!!!! start feature extraction");

        // Run the inference call.
        //Trace.beginSection("runInference");
        inferenceInterface.run(outputNames);
        //Log.i(TAG,"3!!!!!!!!!!!!!!!!!!!!!!!!! start feature extraction");

        // Copy the output Tensor back into the output array.
        for(int i=0;i<outputNames.length;++i) {
            inferenceInterface.fetch(outputNames[i], outputs[i]);
        }

        long total=System.currentTimeMillis()-start;

        //normalize features (first dim)
        float sum=0;
        for(int i=0;i<outputs[0].length;++i)
            sum+=outputs[0][i]*outputs[0][i];
        sum=(float)Math.sqrt(sum);
        for(int i=0;i<outputs[0].length;++i)
            outputs[0][i]/=sum;
        Log.i(TAG,"!!!!!!!!!!!!!!!!!!!!!!!!! end feature extraction total time (s)="+(total/1000.0)+" first feat="+outputs[0][0]+" last feat="+outputs[0][outputs[0].length-1]);
        return outputs;
    }

    public void enableStatLogging(boolean logStats) {
        this.logStats = logStats;
    }

    public String getStatString() {
        return inferenceInterface.getStatString();
    }

    public void close() {
        inferenceInterface.close();
    }
}
