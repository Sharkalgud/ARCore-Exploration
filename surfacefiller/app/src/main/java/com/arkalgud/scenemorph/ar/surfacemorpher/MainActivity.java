package com.arkalgud.scenemorph.ar.surfacemorpher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.arkalgud.scenemorph.ar.surfacemorpher.utilities.PointCloudNode;
import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.PlaneRenderer;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.ArFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ArSceneView arSceneView;
    private PointCloudNode pointCloudNode;
    private Boolean showSurfaceFiller;
    private Boolean showFeaturePoints;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showSurfaceFiller = true;
        showFeaturePoints = false;

        if(!checkIsSupportedDeviceOrFinish(this)){
            return;
        }
        setContentView(R.layout.activity_main);

        //Get ARFragment that is being used
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        arSceneView = arFragment.getArSceneView();

        addTextureToDetectedPlanes();

        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(showSurfaceFiller){
                    renderFeaturePoints();
                    button.setText(R.string.fillsurface_btn_txt);
                    showSurfaceFiller = false;
                    showFeaturePoints = true;
                } else {
                    addTextureToDetectedPlanes();
                    button.setText(R.string.featurepts_btn_txt);
                    showSurfaceFiller = true;
                    showFeaturePoints = false;
                }
            }
        });
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

    private void addTextureToDetectedPlanes(){
        PlaneRenderer planeRenderer = arSceneView.getPlaneRenderer();
        planeRenderer.setVisible(true);

        //The Texture Sampler applies the texture repeatedly to the detected planes
        Texture.Sampler sampler =
                Texture.Sampler.builder()
                        .setMinFilter(Texture.Sampler.MinFilter.LINEAR)
                        .setMagFilter(Texture.Sampler.MagFilter.LINEAR)
                        .setWrapMode(Texture.Sampler.WrapMode.REPEAT)
                        .build();

        //This sets up the texture which right now is a grass png but can be replaced by anything you want
        Texture.builder()
                .setSource(this, R.drawable.grass)
                .setSampler(sampler)
                .build()
                .thenAccept(texture -> {
                    planeRenderer.getMaterial().thenAccept(materialL ->
                            materialL.setTexture(PlaneRenderer.MATERIAL_TEXTURE, texture));
                });
    }

    private void renderFeaturePoints(){
        arSceneView.getPlaneRenderer().setVisible(false);
        Scene scene = arSceneView.getScene();
        scene.addOnUpdateListener(this::onFrame);
        pointCloudNode = new PointCloudNode(this);
        scene.addChild(pointCloudNode);
    }

    private void onFrame(FrameTime frameTime) {
        arFragment.onUpdate(frameTime);
        Frame frame = arSceneView.getArFrame();

        if (frame == null) {
            return;
        }

        // Visualize tracked points.
        if(showFeaturePoints) {
            PointCloud pointCloud = frame.acquirePointCloud();
            pointCloudNode.update(pointCloud);
            // Application is responsible for releasing the point cloud resources after using it.
            pointCloud.release();

        } else {
            arSceneView.getScene().removeChild(pointCloudNode);
        }
    }
}
