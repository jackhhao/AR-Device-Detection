package com.jahao.ar_device_detection;

import androidx.appcompat.app.AppCompatActivity;
//import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.*;

public class MainActivity extends AppCompatActivity implements Scene.OnPeekTouchListener, Scene.OnUpdateListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private GestureDetector gestureDetector;
    private ModelRenderable modelRenderable;
//    private ViewRenderable testTextView;
    private TextView dist;
    private FloatingActionButton fab;

    private List<Anchor> anchors = new ArrayList<Anchor>();
    private Anchor currentAnchor = null;
    private AnchorNode currentAnchorNode = null;

    private int count = 0;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        dist = findViewById(R.id.dist);
        fab = findViewById(R.id.fab);

        initModel();

//        ViewRenderable.builder()
//                .setView(this, R.id.dist)
//                .build()
//                .thenAccept(renderable -> testTextView = renderable);


//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Here's a Snackbar", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        dist.setText("Text View initialized");

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {

                });

        arFragment.getArSceneView().getScene().addOnPeekTouchListener(this);
        arFragment.getArSceneView().getScene().addOnUpdateListener(this);
        this.gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public boolean onSingleTapUp(MotionEvent e) {
                onSingleTap(e);
                return true;
            }

            public boolean onDown(MotionEvent e) {
                return true;
            }
        });
    }

    @Override
    public void onUpdate(FrameTime frametime) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        if (currentAnchorNode != null)
        {
//            getDistance(currentAnchor.getPose(), frame.getCamera().getPose());
        }

    }
//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        // ARCore requires camera permission to operate.
////        if (!CameraPermissionHelper.hasCameraPermission(this)) {
////            CameraPermissionHelper.requestCameraPermission(this);
////            return;
//    }

    private void getDistance(Pose startPose, Pose endPose) {
//        Frame frame = arFragment.getArSceneView().getArFrame();
        float xDist = startPose.tx() - endPose.tx();
        float yDist = startPose.ty() - endPose.ty();
        float zDist = startPose.tz() - endPose.tz();

        float distanceMeters = (float) Math.sqrt(xDist*xDist + yDist*yDist + zDist*zDist);
        dist.setText("Distance: " + distanceMeters);
    }

    @Override
    public void onPeekTouch(HitTestResult hitTestResult, MotionEvent motionEvent)
    {
        arFragment.onPeekTouch(hitTestResult, motionEvent);

        Node resultNode = hitTestResult.getNode();

        if (motionEvent.getAction() != MotionEvent.ACTION_UP) {
            return;
        }

        if (modelRenderable == null) {
            Log.e(TAG, "No  model loaded. Exiting...");
            return;
        }

        if (resultNode != null) {
            dist.setText("node exists");
            if (resultNode.getParent() instanceof AnchorNode) // resultNode instanceof TransformableNode
            {
                currentAnchorNode = (AnchorNode)resultNode.getParent();
                Anchor resultAnchor = ((AnchorNode)resultNode.getParent()).getAnchor();
                if (resultAnchor != null) {
                    currentAnchor = resultAnchor;
                    getDistance(anchors.get(0).getPose(), resultAnchor.getPose());
                }
            }
            return;
        }

        addAnchor(motionEvent);

//        gestureDetector.onTouchEvent(motionEvent);

    }

    private void addAnchor(MotionEvent motionEvent) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        dist.setText("creating anchor");
        if (frame != null && motionEvent != null && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
//            List <HitResult> hitResults = frame.hitTest(motionEvent);
            try {
                HitResult hitResult = frame.hitTest(motionEvent).get(frame.hitTest(motionEvent).size() - 1);
                Anchor anchor = hitResult.createAnchor();
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                currentAnchor = anchor;
                currentAnchorNode = anchorNode;
                anchors.add(currentAnchor);

                TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
                node.getScaleController().setMaxScale(0.09f);
                node.getScaleController().setMinScale(0.03f);
                node.setRenderable(modelRenderable);
                node.setParent(anchorNode);

//                    arFragment.getArSceneView().getScene().setOnTouchListener(this);
                arFragment.getArSceneView().getScene().addChild(anchorNode);
                node.select();

                dist.setText("Andy placed!" + count);
                count++;
            }
            catch(IndexOutOfBoundsException exception) {
                Log.e(TAG, "index out of bounds for hit test");
            }
        }
    }

    private void onSingleTap(MotionEvent motionEvent) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        dist.setText("creating anchor");
//        Session session = arFragment.getArSceneView().getSession();
        if (frame != null && motionEvent != null && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            for (HitResult hitResult : frame.hitTest(motionEvent)) {
                Anchor anchor = hitResult.createAnchor();
                AnchorNode anchorNode = new AnchorNode(anchor);
                anchorNode.setParent(arFragment.getArSceneView().getScene());

                currentAnchor = anchor;
                currentAnchorNode = anchorNode;
                anchors.add(currentAnchor);

                TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
                node.setRenderable(modelRenderable);
                node.setParent(anchorNode);

//                    arFragment.getArSceneView().getScene().setOnTouchListener(this);
                arFragment.getArSceneView().getScene().addChild(anchorNode);
                node.select();

                dist.setText("Andy placed!" + count);
                count++;
            }
        }
    }

    private void initModel() {
        ModelRenderable.builder()
                .setSource(this, R.raw.model)
                .build()
                .thenAccept(renderable -> modelRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
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
}