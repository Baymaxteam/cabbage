package com.example.momo.cabbage5;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.EnvironmentCompat;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private final int REQUEST_PERMISSON_CAMERA = 100;

    private boolean mbFaceDetAvailable;
    private int miMaxFaceCount = 0;
    private int miFaceDetMode;

    private TextView mTextMessage;
    private TextureView mTextureView = null;
    private Button mCameraTakePicture;

    private Size mPrevewSize = null;
    private CameraDevice mCameraDevice = null;
    private CaptureRequest.Builder mPrevewBuilder = null;
    private CameraCaptureSession mCameraPreviewCaptureSession = null;
    private CameraCaptureSession mCameraTakePicCaptureSession = null;

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            //CHECK CAMERA PERMISSIONS
            if(askForPermissions())
                openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }

    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextureView = (TextureView) findViewById(R.id.textureView);
        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mCameraTakePicture = (Button) findViewById(R.id.btnTakePicture);

        Button btnTakePicture = (Button) findViewById(R.id.btnTakePicture);
        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askForPermissions();
                takePicture();
            }
        });
    }
    @Override
    protected void onResume(){
        super.onResume();
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    @Override
    protected void onStop(){
        super.onStop();

        if (mCameraDevice != null)
        {
            mCameraDevice.close();
            mCameraDevice = null;
        }

    }
    @Override
    public void onRequestPermissionsResult (int requestCode, String[] permissions, int[] grantResults){
        switch (requestCode){
            case REQUEST_PERMISSON_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                  openCamera();
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean askForPermissions(){
        String[] permissions = new String[]{Manifest.permission.CAMERA};

        final List<String> listPermissionsNeeded = new ArrayList<>();
        boolean bShowPermissionRationable = false;

        for (String p: permissions){
            int result = ContextCompat.checkSelfPermission(MainActivity.this, p);
            if (result != PackageManager.PERMISSION_GRANTED){
                listPermissionsNeeded.add(p);

                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, p)){
                    bShowPermissionRationable = true;
                }
            }
        }
        if(!listPermissionsNeeded.isEmpty()){
            if (bShowPermissionRationable){
                AlertDialog.Builder altDlgBulider = new AlertDialog.Builder(MainActivity.this);
                altDlgBulider.setTitle("提示");
                altDlgBulider.setMessage("App 需要你的許可");
                altDlgBulider.setIcon(android.R.drawable.ic_dialog_info);
                altDlgBulider.setCancelable(false);
                altDlgBulider.setPositiveButton("確定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(MainActivity.this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_PERMISSON_CAMERA);
                    }
                });
                altDlgBulider.show();
            }
            else
                ActivityCompat.requestPermissions(MainActivity.this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),REQUEST_PERMISSON_CAMERA);
            return false;
        }
        return true;
    }

    private void openCamera(){
        CameraManager camMgr = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String cameraId = camMgr.getCameraIdList()[0];
            CameraCharacteristics camChar = camMgr.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = camChar.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mPrevewSize = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)
                camMgr.openCamera(cameraId, mCameraStateCallback, null);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback(){
        @Override
        public void onOpened(CameraDevice cameraDevice){
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice){
            Toast.makeText(MainActivity.this, "無法使用camera", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error){
            Toast.makeText(MainActivity.this, "無法開啟camera", Toast.LENGTH_LONG).show();
        }
    };

    private CameraCaptureSession.StateCallback mCameraCaptureSessionCallback = new CameraCaptureSession.StateCallback(){
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession){
            closeAllCameraCaptureSession();
            mCameraPreviewCaptureSession = cameraCaptureSession;
            mPrevewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            //mPrevewBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, miFaceDetMode);
            HandlerThread backgroundThread = new HandlerThread("CameraPreview");
            backgroundThread.start();
            Handler backgroundHandler = new Handler(backgroundThread.getLooper());
            try {
                mCameraPreviewCaptureSession.setRepeatingRequest(mPrevewBuilder.build(), null, backgroundHandler);
            }catch (CameraAccessException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession){
            Toast.makeText(MainActivity.this, "預覽錯誤", Toast.LENGTH_LONG).show();
        }
    };

    private void startPreview(){
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPrevewSize.getWidth(),mPrevewSize.getHeight());
        Surface surface = new Surface(surfaceTexture);
        try{
            mPrevewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }

        mPrevewBuilder.addTarget(surface);
        try{
            mCameraDevice.createCaptureSession(Arrays.asList(surface), mCameraCaptureSessionCallback, null);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void closeAllCameraCaptureSession(){
        if (mCameraPreviewCaptureSession!=null){
            mCameraPreviewCaptureSession.close();
            mCameraPreviewCaptureSession = null;
        }
        if (mCameraTakePicCaptureSession != null){
            mCameraTakePicCaptureSession.close();
            mCameraTakePicCaptureSession = null;
        }
    }

    private void takePicture() {
        if (mCameraDevice == null) {
            Toast.makeText(MainActivity.this, "Camera錯誤", Toast.LENGTH_LONG).show();
            return;
        }


        final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath(), "photo.jpg");
        ImageReader.OnImageAvailableListener imgReaderOnImageAvailable = new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader imageReader) {
                Image image = null;
                try {
                    image = imageReader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);

                    OutputStream output = null;
                    try {
                        output = new FileOutputStream(file);
                        output.write(bytes);
                    } finally {
                        if (null != output) {
                            output.close();
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (image != null)
                        image.close();
                }
            }
        };

        CameraManager camMgr = (CameraManager) getSystemService(CAMERA_SERVICE);

        try {
            CameraCharacteristics camChar = camMgr.getCameraCharacteristics(mCameraDevice.getId());
            Size[] jpegSizes = null;
            if (camChar != null)
                jpegSizes = camChar.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            int picWidth = 320;
            int picHeight = 240;
            if (jpegSizes != null && jpegSizes.length > 0) {
                picWidth = jpegSizes[0].getWidth();
                picHeight = jpegSizes[0].getHeight();
            }

            ImageReader imgReader = ImageReader.newInstance(picWidth, picHeight, ImageFormat.JPEG, 1);
            HandlerThread thread = new HandlerThread("CameraTakePicture");
            thread.start();
            final Handler backgroundHandler = new Handler(thread.getLooper());

            imgReader.setOnImageAvailableListener(imgReaderOnImageAvailable, backgroundHandler);
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);

            outputSurfaces.add(imgReader.getSurface());
            outputSurfaces.add(new Surface(mTextureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imgReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            SparseIntArray PICTURE_ORIENTATIONS = new SparseIntArray();
            PICTURE_ORIENTATIONS.append(Surface.ROTATION_0, 90);
            PICTURE_ORIENTATIONS.append(Surface.ROTATION_90, 0);
            PICTURE_ORIENTATIONS.append(Surface.ROTATION_180, 270);
            PICTURE_ORIENTATIONS.append(Surface.ROTATION_270, 180);

            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, PICTURE_ORIENTATIONS.get(rotation));

            final CameraCaptureSession.CaptureCallback camCaptureCallback = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    startPreview();
                    //Integer mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);
                }

                @Override
                public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
                }
            };
            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback()
            {
                @Override
                public void onConfigured (CameraCaptureSession cameraCaptureSession){
                try {
                    closeAllCameraCaptureSession();
                    ;
                    mCameraTakePicCaptureSession = cameraCaptureSession;
                    cameraCaptureSession.capture(captureBuilder.build(), camCaptureCallback, backgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
                @Override
                public void onConfigureFailed (CameraCaptureSession cameraCaptureSession){
                    Toast.makeText(MainActivity.this, "拍照起始錯誤", Toast.LENGTH_LONG).show();
                }
            },backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
