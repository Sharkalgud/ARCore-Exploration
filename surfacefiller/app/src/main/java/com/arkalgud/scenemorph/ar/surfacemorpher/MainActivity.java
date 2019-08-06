package com.arkalgud.scenemorph.ar.surfacemorpher;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.arkalgud.scenemorph.ar.surfacemorpher.utilities.PointCloudNode;
import com.google.ar.core.Anchor;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.PlaneRenderer;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.Collection;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private PointCloudNode pointCloudNode;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!checkIsSupportedDeviceOrFinish(this)){
            return;
        }

        //Get ARFragment that is being used
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        buildRenderableObject();

        //addTextureToDetectedPlanes();

        renderFeaturePoints();

        //Listener to take action when a user taps the screen
        //It does something called a hit test to see if your touch connects to a detected plane
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {

            if (andyRenderable == null) {
                return;
            }

            Pose hitPose = hitResult.getHitPose();
            float[] hitTranslation = hitPose.getTranslation();
            float[] hitRotation =  hitPose.getRotationQuaternion();

            Pose compositePose = new Pose(hitTranslation, hitRotation);

            Anchor anchor = hitResult.getTrackable().createAnchor(compositePose);
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());

            TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
            andy.setParent(anchorNode);
            andy.setRenderable(andyRenderable);
            andy.select();
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
        //The Texture Sampler applies the texture repeatadly to the detected planes
        Texture.Sampler sampler =
                Texture.Sampler.builder()
                        .setMinFilter(Texture.Sampler.MinFilter.LINEAR)
                        .setWrapMode(Texture.Sampler.WrapMode.REPEAT)
                        .build();

        //This sets up the texture which right now is a fractal png but can be replaced by anything you want
        Texture.builder()
                .setSource(this, R.drawable.fractal)
                .setSampler(sampler)
                .build()
                .thenAccept(texture -> {
                    arFragment.getArSceneView().getPlaneRenderer()
                            .getMaterial().thenAccept(material ->
                            material.setTexture(PlaneRenderer.MATERIAL_TEXTURE, texture));
                });
    }

    //Create renderable objects, in this case just the Android logo
    private void buildRenderableObject(){

        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build()
                .thenAccept(renderable->andyRenderable=renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
    }

    private void renderFeaturePoints(){

        Scene scene = arFragment.getArSceneView().getScene();
        //scene.setOnUpdateListener(this::onFrame);
        pointCloudNode = new PointCloudNode(this);
        scene.addChild(pointCloudNode);
    }

    private void onFrame(FrameTime frameTime) {
        arFragment.onUpdate(frameTime);
        Frame frame = arFragment.getArSceneView().getArFrame();

        if (frame == null) {
            return;
        }

        Camera camera = frame.getCamera();
        Collection<Anchor> updatedAnchors = frame.getUpdatedAnchors();
        TrackingState cameraTrackingState = camera.getTrackingState();


        // If not tracking, don't draw 3d objects.
        if (cameraTrackingState == TrackingState.PAUSED) {
            return;
        }

        // Visualize tracked points.
        PointCloud pointCloud = frame.acquirePointCloud();
        pointCloudNode.update(pointCloud);

        // Application is responsible for releasing the point cloud resources after using it.
        pointCloud.release();

    }
}
