package me.pankiewicz.will.cameratia;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import android.view.WindowManager;
import android.provider.MediaStore.Images;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;
import java.io.File;
import java.util.List;
import java.util.concurrent.RunnableFuture;

import me.pankiewicz.will.cameratia.filters.Filter;
import me.pankiewicz.will.cameratia.filters.NoneFilter;
import me.pankiewicz.will.cameratia.filters.convolution.StrokeEdgesFilter;
import me.pankiewicz.will.cameratia.filters.curve.CrossProcessCurveFilter;
import me.pankiewicz.will.cameratia.filters.curve.PortraCurveFilter;
import me.pankiewicz.will.cameratia.filters.curve.ProviaCurveFilter;
import me.pankiewicz.will.cameratia.filters.curve.VelviaCurveFilter;
import me.pankiewicz.will.cameratia.filters.mixer.RecolorCMVFilter;
import me.pankiewicz.will.cameratia.filters.mixer.RecolorRCFilter;
import me.pankiewicz.will.cameratia.filters.mixer.RecolorRGVFilter;

// Use theh deprecated Camera class
@SuppressWarnings("deprecation")
public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // a tag for log output.
    private static final String TAG = CameraActivity.class.getSimpleName();

    // A key for storing hte index of the active camera
    private static final String STATE_CAMERA_INDEX = "cameraIndex";

    // A key for storing the index of the active image size.
    private static final String STATE_IMAGE_SIZE_INDEX = "imageSizeIndex";

    // Keys for stroing the indices of the active filters.
    private static final String STATE_CURVE_FILTER_INDEX = "curveFilterIndex";
    private static final String STATE_MIXER_FILTER_INDEX = "mixerFilterIndex";
    private static final String STATE_CONVOLUTION_FILTER_INDEX = "convolutionFilterIndex";

    // An ID for items in the image size submenu
    private static final int MENU_GROUP_ID_SIZE=2;


    // the filters
    private Filter[] mCurveFilters;
    private Filter[] mMixerFilters;
    private Filter[] mConvolutionFilters;

    // The indices of the active filters
    private int mCurveFilterIndex;
    private int mMixerFilterIndex;
    private int mConvolutionFilterIndex;


    // The index of the active camera
    private int mCameraIndex;

    // The index of the active image size
    private int mImageSizeIndex;

    // Whether the active camera is front-facing
    // if so the camera view should be mirrored.
    private boolean mIsCameraFrontFacing;

    // The number of camera on the device
    private int mNumCameras;

    // The camera view
    private CameraBridgeViewBase mCameraView;

    // The image sizes supported by the active camera
    private List<Size> mSupportedImageSizes;

    // Whether the next camera frame should be saved as a photo
    private boolean mIsPhotoPending;

    // A matrix that is used when saving photos
    private Mat mBgr;

    // Whether an asynchronous menu action is in progress
    // if so, menu interaction should be disabled
    private boolean mIsMenuLocked;





    // the OpenCV loader callback
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(final int status) {
            switch(status){
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");
                    mCameraView.enableView();
                    mBgr = new Mat();
                    mCurveFilters = new Filter[]{
                            new NoneFilter(),
                            new PortraCurveFilter(),
                            new ProviaCurveFilter(),
                            new VelviaCurveFilter(),
                            new CrossProcessCurveFilter()
                    };
                    mMixerFilters = new Filter[]{
                            new NoneFilter(),
                            new RecolorRCFilter(),
                            new RecolorRGVFilter(),
                            new RecolorCMVFilter()
                    };
                    mConvolutionFilters = new Filter[] {
                            new NoneFilter(),
                            new StrokeEdgesFilter()
                    };
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    // Suppress backward incampatibility errors because we provide
    // backward-compatible fallbakcs
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState != null){
            mCameraIndex = savedInstanceState.getInt(STATE_CAMERA_INDEX, 0);
            mImageSizeIndex = savedInstanceState.getInt(STATE_IMAGE_SIZE_INDEX, 0);
            mCurveFilterIndex = savedInstanceState.getInt(STATE_CURVE_FILTER_INDEX, 0);
            mMixerFilterIndex = savedInstanceState.getInt(STATE_MIXER_FILTER_INDEX, 0);
            mConvolutionFilterIndex = savedInstanceState.getInt(STATE_CONVOLUTION_FILTER_INDEX, 0);
        }else {
            mCameraIndex = 0;
            mImageSizeIndex = 0;
            mCurveFilterIndex = 0;
            mMixerFilterIndex = 0;
            mConvolutionFilterIndex = 0;
        }

        final Camera camera;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD){
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(mCameraIndex, cameraInfo);
            mIsCameraFrontFacing = (cameraInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
            mNumCameras = android.hardware.Camera.getNumberOfCameras();
            camera = Camera.open(mCameraIndex);
        }else { // pre-gingerbread
            // Assume there  is only 1 camera and its rear-facing
            mIsCameraFrontFacing = false;
            mNumCameras = 1;
            camera = Camera.open();
        }
        final Parameters parameters = camera.getParameters();
        camera.release();
        mSupportedImageSizes = parameters.getSupportedPreviewSizes();
        final Size size = mSupportedImageSizes.get(mImageSizeIndex);

        mCameraView = new JavaCameraView(this, mCameraIndex);
        mCameraView.setMaxFrameSize(size.width, size.height);
        mCameraView.setCvCameraViewListener(this);
        setContentView(mCameraView);
    }

    public void onSaveInstanceState(Bundle savedInstanceState){
        // Save the current camera index.
        savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);

        // Save the current image size index
        savedInstanceState.putInt(STATE_IMAGE_SIZE_INDEX, mImageSizeIndex);

        // Save the current filter indices
        savedInstanceState.putInt(STATE_CURVE_FILTER_INDEX, mCurveFilterIndex);
        savedInstanceState.putInt(STATE_MIXER_FILTER_INDEX, mMixerFilterIndex);
        savedInstanceState.putInt(STATE_CONVOLUTION_FILTER_INDEX, mConvolutionFilterIndex);
        super.onSaveInstanceState(savedInstanceState);
    }

    // Suppress backward incompatibility errors because we provide
    // backward-compatible fallbacks.
    @SuppressLint("NewApi")
    @Override
    public void recreate(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            super.recreate();
        } else {
            finish();
            startActivity(getIntent());
        }
    }

    @Override
    public void onPause(){
        if (mCameraView != null){
            mCameraView.disableView();
        }
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        mIsMenuLocked = false;
    }

    @Override
    public void onDestroy(){
        if (mCameraView != null){
            mCameraView.disableView();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu){
        getMenuInflater().inflate(R.menu.activity_camera, menu);
        if (mNumCameras < 2){
            // Remove the option to switch camera, since there is only 1
            menu.removeItem(R.id.menu_other_camera);
        }
        int numSupportedImageSizes = mSupportedImageSizes.size();
        if (numSupportedImageSizes>1){
            final SubMenu sizeSubMenu = menu.addSubMenu(R.string.menu_image_size);
            for (int i = 0; i < numSupportedImageSizes; i ++){
                final Size size = mSupportedImageSizes.get(i);
                sizeSubMenu.add(MENU_GROUP_ID_SIZE, i, Menu.NONE, String.format("%dx%d", size.width, size.height));
            }
        }
        return true;
    }

    // Suppress backward incompatibility errors because we provide backward-compatible fallbacks (for recreate).
    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(final MenuItem item){
        if (mIsMenuLocked){
            return true;
        }
        if (item.getGroupId() == MENU_GROUP_ID_SIZE){
            mImageSizeIndex = item.getItemId();
            recreate();

            return true;
        }
        switch(item.getItemId()){
            case R.id.menu_next_curve_filter:
                mCurveFilterIndex++;
                if (mCurveFilterIndex == mCurveFilters.length){
                    mCurveFilterIndex = 0;
                }
                return true;
            case R.id.menu_next_mixer_filter:
                mMixerFilterIndex++;
                if (mMixerFilterIndex == mMixerFilters.length){
                    mMixerFilterIndex = 0;
                }
                return true;
            case R.id.menu_next_convolution_filter:
                mConvolutionFilterIndex++;
                if (mConvolutionFilterIndex == mConvolutionFilters.length){
                    mConvolutionFilterIndex = 0;
                }
                return true;
            case R.id.menu_other_camera:
                mIsMenuLocked = true;

                // With another camera index, recreate the activity
                mCameraIndex++;
                if (mCameraIndex == mNumCameras){
                    mCameraIndex = 0;
                }
                mImageSizeIndex = 0;
                recreate();

                return true;
            case R.id.menu_take_photo:
                mIsMenuLocked = true;

                // Next frame, take the photo
                mIsPhotoPending = true;

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCameraViewStarted(final int width, final int height){}

    @Override
    public void onCameraViewStopped(){

    }

    @Override
    public Mat onCameraFrame(final CvCameraViewFrame inputFrame){
        final Mat rgba = inputFrame.rgba();
        // Apply the active filters
        mCurveFilters[mCurveFilterIndex].apply(rgba, rgba);
        mMixerFilters[mMixerFilterIndex].apply(rgba, rgba);
        mConvolutionFilters[mConvolutionFilterIndex].apply(rgba, rgba);

        if (mIsPhotoPending){
            mIsPhotoPending = false;
            takePhoto(rgba);
        }

        if (mIsCameraFrontFacing){
            Core.flip(rgba, rgba, 1);
        }

        return rgba;
    }


    private void takePhoto(final Mat rgba){

        // Determine the path and metadata for the photo.
        final long currentTimeMillis = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + "/" + appName;
        final String photoPath = albumPath + "/" + currentTimeMillis + LabActivity.PHOTO_FILE_EXTENSION;
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, photoPath);
        values.put(Images.Media.MIME_TYPE, LabActivity.PHOTO_MIME_TYPE);
        values.put(Images.Media.TITLE, appName);
        values.put(Images.Media.DESCRIPTION, appName);
        values.put(Images.Media.DATE_TAKEN, currentTimeMillis);

        // Ensure that the album directory exists
        File album = new File(albumPath);
        if (!album.isDirectory() && !album.mkdirs()){
            Log.e(TAG, "Failed to create album directory at " + albumPath);
            onTakePhotoFailed();
            return;
        }

        // Try to create the photo
        Imgproc.cvtColor(rgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        if (!Imgcodecs.imwrite(photoPath, mBgr)){
            //Log.e(TAG, "Failed to save photo to " + photoPath);
            //onTakePhotoFailed();
        }

        Log.d(TAG, "Photo saved sucessfully to " + photoPath);

        // Try to insert the photo into the MediaStore
        Uri uri;
        try{
            uri= getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (final Exception e){
            Log.e(TAG, "Failed to insert photo into MediaStore");
            e.printStackTrace();

            // Since the insertion failed, delete the photo
            File photo = new File(photoPath);
            if (!photo.delete()){
                Log.e(TAG, "Failed to delete non-inserted photo");
            }

            onTakePhotoFailed();
            return;
        }

        // Open the photo in LabActivity
        final Intent intent = new Intent(this, LabActivity.class);
        intent.putExtra(LabActivity.EXTRA_PHOTO_URI, uri);
        intent.putExtra(LabActivity.EXTRA_PHOTO_DATA_PATH, photoPath);
        startActivity(intent);

    }

    private void onTakePhotoFailed(){
        mIsMenuLocked = false;

        // Show an error message.
        final String errorMessage = getString(R.string.photo_error_message);
        runOnUiThread(new Runnable() {
            @Override
            public void run(){
                Toast.makeText(CameraActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
