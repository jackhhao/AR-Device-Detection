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

import com.google.android.material.snackbar.Snackbar;
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
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.*;

import static com.google.ar.core.Plane.Type.HORIZONTAL_DOWNWARD_FACING;

public class MainActivity extends AppCompatActivity implements Scene.OnPeekTouchListener, Scene.OnUpdateListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private GestureDetector gestureDetector;
    private ModelRenderable modelRenderable, cubeRenderable;
//    private ViewRenderable testTextView;
    private TextView dist;
    private FloatingActionButton fab;

    private LinkedHashMap<String, Plane> planes = new LinkedHashMap<String, Plane>();
    private final LinkedHashMap<Plane.Type, String> planeTypes = new LinkedHashMap<Plane.Type, String>();
    private Iterator<Plane.Type> planeTypesIterator;
    private Plane.Type currentPlaneType;
    private List<Anchor> anchors = new ArrayList<Anchor>();
    private List<Node> floorNodes = new ArrayList<Node>();
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

        initRenderables();
//        initFAB();
        initPlaneTypes();

//        ViewRenderable.builder()
//                .setView(this, R.id.dist)
//                .build()
//                .thenAccept(renderable -> testTextView = renderable);



        dist.setText("Text View initialized");

        arFragment.setOnTapArPlaneListener(
            (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
//                Plane.Type planeType = plane.getType();
//                if(planeType == Plane.Type.HORIZONTAL_UPWARD_FACING) {
//                    dist.setText("floor plane");
//                }
//                // Ceiling, camera looking up at
//                else if(planeType == Plane.Type.HORIZONTAL_DOWNWARD_FACING) {
//                    dist.setText("ceiling plane");
//                }
                // Floor, ground, camera looking down at
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
        if (!floorNodes.isEmpty())
        {
            getDistance(floorNodes.get(floorNodes.size() - 1), frame.getCamera().getPose());
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

    private void getDistance(Node startNode, Pose endPose) {
//        Frame frame = arFragment.getArSceneView().getArFrame();
        float xDist = startNode.getWorldPosition().x - endPose.tx();
//        float yDist = startNode.ty() - endPose.ty();
        float zDist = startNode.getWorldPosition().z - endPose.tz();

        float distanceMeters = (float) Math.sqrt(xDist*xDist + zDist*zDist);
        dist.setText("Distance to Node: " + distanceMeters);
    }

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

        if ((modelRenderable == null) || (cubeRenderable == null)) {
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

//        addAnchor(motionEvent);
        if (planes.size() < 2) {
            if (detectPlanes(motionEvent, currentPlaneType) && planeTypesIterator.hasNext()) {
                currentPlaneType = planeTypesIterator.next();
            }
            return;
        }
        addAnchor(motionEvent);
//        detectPlanes(motionEvent, "ceiling");

//        gestureDetector.onTouchEvent(motionEvent);

    }

    private boolean detectPlanes(MotionEvent motionEvent, Plane.Type planeType){
        Frame frame = arFragment.getArSceneView().getArFrame();
        dist.setText("detecting planes");
        if (frame != null && motionEvent != null && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
            for (HitResult hitResult : frame.hitTest(motionEvent)) {
//                dist.setText("bruh");
                Trackable trackable = hitResult.getTrackable();
                if (trackable instanceof Plane && ((Plane)trackable).isPoseInPolygon(hitResult.getHitPose())) {
                    Plane plane = (Plane) trackable;

                    if(plane.getType() == planeType) {
                        dist.setText(planeTypes.get(planeType));
                        planes.put(planeTypes.get(planeType), plane);
                        return true;
                    }
                }
            }
        }
        return false;
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
//                node.getScaleController().setMaxScale(0.09f);
//                node.getScaleController().setMinScale(0.03f);
                node.setRenderable(cubeRenderable);
                node.setParent(anchorNode);

//                    arFragment.getArSceneView().getScene().setOnTouchListener(this);
                arFragment.getArSceneView().getScene().addChild(anchorNode);
                node.select();

                generateProjection(anchor, anchorNode);

                dist.setText("Andy placed!" + count);
                count++;
            }
            catch(IndexOutOfBoundsException exception) {
                Log.e(TAG, "index out of bounds for hit test");
            }
        }
    }

    private void generateProjection(Anchor ceilingAnchor, AnchorNode ceilingAnchorNode) {
//        Pose oldPose = ceilingAnchor.getPose();
//        Pose newPose = oldPose.compose(Pose.makeTranslation(-0.05f,0,0));
        TransformableNode floorNode = new TransformableNode(arFragment.getTransformationSystem());
//        dist.setText("" + floorNode.getLocalPosition());
        floorNode.setParent(ceilingAnchorNode);
//        floorNode.setLocalPosition(new Vector3(0,  ceilingAnchor.getPose().ty() - planes.get("Floor Plane").getCenterPose().ty(), 0));
//        floorNode.setLocalPosition(new Vector3(0,  planes.get("Ceiling Plane").getCenterPose().ty() - planes.get("Floor Plane").getCenterPose().ty(), 0));
//        floorNode.getScaleController().setMaxScale(0.09f);
        floorNode.setWorldPosition(new Vector3(ceilingAnchor.getPose().tx(),  planes.get("Floor Plane").getCenterPose().ty(), ceilingAnchor.getPose().tz()));
//        floorNode.getScaleController().setMinScale(0.03f);
        floorNode.setRenderable(cubeRenderable);
        floorNodes.add(floorNode);
        generateConnection(ceilingAnchorNode, floorNode, ceilingAnchor);
    }

    private void generateConnection(Node ceilingNode, Node floorNode, Anchor ceilingAnchor) {
        Vector3 pointA, pointB;
        pointA = ceilingNode.getWorldPosition();
        pointB = floorNode.getWorldPosition();

        final Vector3 difference = Vector3.subtract(pointA, pointB);
        final Vector3 directionFromTopToBottom = difference.normalized();
        final Quaternion rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());

        MaterialFactory.makeTransparentWithColor(getApplicationContext(), new Color(0, 255, 244, 20))
                .thenAccept(
                        material -> {
//                            ModelRenderable model = ShapeFactory.makeCube(
//                                    new Vector3(.01f, difference.length(), .01f),
//                                    Vector3.zero(), material);
                            ModelRenderable model = ShapeFactory.makeCylinder(
                                    0.01f,
                                    difference.length(),
                                    new Vector3(.01f, difference.length(), .01f).zero(),
                                    material);
                            Node connectionNode = new Node();
                            connectionNode.setRenderable(model);
                            connectionNode.setParent(ceilingNode);
//                            connectionNode.setWorldPosition(Vector3.add(pointA, pointB).scaled(.5f));
                            connectionNode.setWorldPosition(new Vector3(ceilingAnchor.getPose().tx(),  (planes.get("Floor Plane").getCenterPose().ty() + (difference.length() / 2)), ceilingAnchor.getPose().tz()));
//                            connectionNode.setWorldRotation(rotationFromAToB);
                        }
                );
//        currentAnchorNode = anchorNode;
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
                node.setRenderable(cubeRenderable);
                node.setParent(anchorNode);

//                    arFragment.getArSceneView().getScene().setOnTouchListener(this);
                arFragment.getArSceneView().getScene().addChild(anchorNode);
                node.select();

                dist.setText("Andy placed!" + count);
                count++;
            }
        }
    }

    private void initRenderables() {
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
        MaterialFactory.makeTransparentWithColor(this, new Color(0F, 0F, 244F))
                .thenAccept(
                    material -> {
                        Vector3 vector3 = new Vector3(0.09f, 0.01f, 0.09f);
                        cubeRenderable = ShapeFactory.makeCube(vector3, Vector3.zero(), material);
                        cubeRenderable.setShadowCaster(false);
                        cubeRenderable.setShadowReceiver(false);
                    });
    }

//    private void initFAB() {
//        fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Here's a Snackbar", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
//    }

    private void initPlaneTypes() {
        planeTypes.put(Plane.Type.HORIZONTAL_UPWARD_FACING, "Floor Plane");
        planeTypes.put(Plane.Type.HORIZONTAL_DOWNWARD_FACING, "Ceiling Plane");
        planeTypesIterator = planeTypes.keySet().iterator();
        currentPlaneType = planeTypesIterator.next();
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