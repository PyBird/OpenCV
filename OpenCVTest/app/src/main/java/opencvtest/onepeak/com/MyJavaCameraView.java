package opencvtest.onepeak.com;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.tzutalin.dlib.Constants;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.FileUtils;
import com.tzutalin.dlib.ImageUtils;
import com.tzutalin.dlib.VisionDetRet;

import junit.framework.Assert;

import org.opencv.android.JavaCameraView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MyJavaCameraView extends JavaCameraView {

    public MyJavaCameraView(Context context, int cameraId) {
        super(context, cameraId);

        init(context);
    }

    public MyJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    private int sum=0;
    @Override
    public void onPreviewFrame(byte[] frame, Camera arg1) {
        super.onPreviewFrame(frame,arg1);

        if(mIsComputing){
            return;
        }
        mIsComputing=true;
        sum++;
//        Log.e("fff"," ----------------------sum= "+sum);
        if(sum%50==0){

            Bitmap bitmap = ImageUtils.decodeFrameToBitmap(frame);
            if(null != bitmap){
                Log.e("fff"," ----------------------save bitmap "+bitmap);
//                ImageUtils.saveBitmap(bitmap);
            }else{
                Log.e("fff"," ----------------------bitmap=null ");
            }
        }

        if (!new File(Constants.getFaceShapeModelPath()).exists()) {
//            FileUtils.copyFileFromRawToOthers(mContext, R.raw.shape_predictor_68_face_landmarks, Constants.getFaceShapeModelPath());
        }

//        List<VisionDetRet> results;
//        synchronized (MyJavaCameraView.this) {
//            results = mFaceDet.detect(mCroppedBitmap);
//        }
//        drawPoint(results);
    }

    private Bitmap mRGBframeBitmap = null;
    private Bitmap mCroppedBitmap = null;
    private Paint mFaceLandmardkPaint;
    private int mScreenRotation = 90;
    private Context mContext;
    private FaceDet mFaceDet;
    private boolean mIsComputing = false;

    private void init(Context context){
        mContext = context;
        mFaceLandmardkPaint = new Paint();
        mFaceLandmardkPaint.setColor(Color.GREEN);
        mFaceLandmardkPaint.setStrokeWidth(2);
        mFaceLandmardkPaint.setStyle(Paint.Style.STROKE);

        mFaceDet = new FaceDet(Constants.getFaceShapeModelPath());
    }

    private void drawPoint(List<VisionDetRet> results){
        Log.e("fff","--------------------results= "+results);
        if (results != null) {
            for (final VisionDetRet ret : results) {
                float resizeRatio = 1.0f;
                Rect bounds = new Rect();
                bounds.left = (int) (ret.getLeft() * resizeRatio);
                bounds.top = (int) (ret.getTop() * resizeRatio);
                bounds.right = (int) (ret.getRight() * resizeRatio);
                bounds.bottom = (int) (ret.getBottom() * resizeRatio);
                Canvas canvas = new Canvas(mCroppedBitmap);
                canvas.drawRect(bounds, mFaceLandmardkPaint);

                // Draw landmark
                ArrayList<Point> landmarks = ret.getFaceLandmarks();
                for (Point point : landmarks) {
                    int pointX = (int) (point.x * resizeRatio);
                    int pointY = (int) (point.y * resizeRatio);
                    canvas.drawCircle(pointX, pointY, 2, mFaceLandmardkPaint);
                }
            }
        }

        mIsComputing = false;
    }

    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {

        Display getOrient = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        Point point = new Point();
        getOrient.getSize(point);
        int screen_width = point.x;
        int screen_height = point.y;
        Log.d("fff", String.format("screen size (%d,%d)", screen_width, screen_height));
        if (screen_width < screen_height) {
            orientation = Configuration.ORIENTATION_PORTRAIT;
            mScreenRotation = 90;
        } else {
            orientation = Configuration.ORIENTATION_LANDSCAPE;
            mScreenRotation = 0;
        }

        Assert.assertEquals(dst.getWidth(), dst.getHeight());
        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);

        final float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);

        // Rotate around the center if necessary.
        if (mScreenRotation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(mScreenRotation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }

    @Override
    protected void releaseCamera() {
        synchronized (this) {
            if (mFaceDet != null) {
                mFaceDet.release();
            }
        }
    }
}
