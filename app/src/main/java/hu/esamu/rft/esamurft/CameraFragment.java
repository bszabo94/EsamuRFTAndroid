package hu.esamu.rft.esamurft;

import android.app.Fragment;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.hardware.camera2.*;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import hu.esamu.rft.esamurft.R;

public class CameraFragment extends Fragment {

    private Size prevsize;
    private Size jpegSizes[] = null;

    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;
    Button getPicture;

    private static final SparseIntArray ORIENTATIONS=new SparseIntArray();
    static
    {
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {



        super.onCreate(savedInstanceState);

    }

    void getPicture(){
        if (cameraDevice == null)
            return;

        CameraManager manager=(CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            //if (characteristics != null){
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            //}

            int width=640,height=480;

            if(jpegSizes!=null && jpegSizes.length>0) {
                width=jpegSizes[0].getWidth();
                height=jpegSizes[0].getHeight();
            }

            ImageReader reader=ImageReader.newInstance(width,height,ImageFormat.JPEG,1);
            List<Surface> outputSurfaces=new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder capturebuilder=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            capturebuilder.addTarget(reader.getSurface());
            capturebuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            int rotation= getActivity().getWindowManager().getDefaultDisplay().getRotation();
            capturebuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));
            ImageReader.OnImageAvailableListener imageAvailableListener=new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    //try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    //} catch (Exception ee) {
                    //}
                    //finally {
                        if(image!=null)
                            image.close();
                    //}
                }
                void save(byte[] bytes)
                {
                    File file12=getOutputMediaFile();
                    OutputStream outputStream=null;
                    try
                    {
                        outputStream=new FileOutputStream(file12);
                        outputStream.write(bytes);
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }finally {
                        try {
                            if (outputStream != null)
                                outputStream.close();
                        }catch (Exception e){}
                    }
                }
            };
            HandlerThread handlerThread=new HandlerThread("takepicture");
            handlerThread.start();
            final Handler handler=new Handler(handlerThread.getLooper());
            reader.setOnImageAvailableListener(imageAvailableListener,handler);
            final CameraCaptureSession.CaptureCallback  previewSSession=new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    startCamera();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try
                    {
                        session.capture(capturebuilder.build(),previewSSession,handler);
                    }catch (Exception e)
                    {
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            },handler);

        }catch (Exception e){

        }
    }

    public  void openCamera()
    {
        CameraManager manager=(CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
        try
        {
            String camerId=manager.getCameraIdList()[0];
            CameraCharacteristics characteristics=manager.getCameraCharacteristics(camerId);
            StreamConfigurationMap map=characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            prevsize=map.getOutputSizes(SurfaceTexture.class)[0];
            manager.openCamera(camerId,stateCallback,null);
        }catch (SecurityException e)
        {
        }catch (Exception e){

        }
    }
    private TextureView.SurfaceTextureListener surfaceTextureListener=new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private CameraDevice.StateCallback stateCallback=new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice=camera;
            startCamera();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
        }
        @Override
        public void onError(CameraDevice camera, int error) {
        }
    };
    @Override
    public void onPause() {
        super.onPause();
        if(cameraDevice!=null)
        {
            cameraDevice.close();
        }
    }
    void  startCamera()
    {
        if(cameraDevice==null||!textureView.isAvailable()|| prevsize==null)
        {
            return;
        }
        SurfaceTexture texture=textureView.getSurfaceTexture();
        if(texture==null)
        {
            return;
        }
        texture.setDefaultBufferSize(prevsize.getWidth(),prevsize.getHeight());
        Surface surface=new Surface(texture);
        try
        {
            previewBuilder=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        }catch (Exception e)
        {
        }
        previewBuilder.addTarget(surface);
        try
        {
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    previewSession=session;
                    getChangedPreview();
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            },null);
        }catch (Exception e)
        {
        }
    }
    void getChangedPreview()
    {
        if(cameraDevice==null)
        {
            return;
        }
        previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        HandlerThread thread=new HandlerThread("changed Preview");
        thread.start();
        Handler handler=new Handler(thread.getLooper());
        try
        {
            previewSession.setRepeatingRequest(previewBuilder.build(), null, handler);
        }catch (Exception e){}
    }


    private static File getOutputMediaFile() {
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "MyCameraApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMG_" + timeStamp + ".jpg");
        return mediaFile;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.camera_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){

        try{
            textureView = (TextureView) getView().findViewById(R.id.frag_camera_textureview);
            textureView.setSurfaceTextureListener(surfaceTextureListener);

            getPicture = (Button) getView().findViewById(R.id.frag_btn_getpicture);
            getPicture.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    getPicture();
                }
            });
        }catch (NullPointerException e){
            Log.d("camera_fragment", "findViewById threw a NullPointerException. This should not happen here, really.");
        }

    }

}
