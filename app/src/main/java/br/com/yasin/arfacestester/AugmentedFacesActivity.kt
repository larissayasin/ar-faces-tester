package br.com.yasin.arfacestester

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.AugmentedFaceNode
import kotlinx.android.synthetic.main.activity_augmented_faces.*
import java.util.*


class AugmentedFacesActivity : AppCompatActivity() {

    private val TAG = AugmentedFacesActivity::class.java.simpleName

    private val MIN_OPENGL_VERSION = 3.0

    private var id = 0

    private var arFragment: FaceArFragment? = null

    private var faceRegionsRenderable: ModelRenderable? = null

    private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        id = intent.getIntExtra("id", 0)
        val url = intent.getStringExtra("url")
        val isGltf = intent.getBooleanExtra("isGltf", true)

        val sourceType =
            if (isGltf) RenderableSource.SourceType.GLTF2 else RenderableSource.SourceType.GLB

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_augmented_faces)

        arFragment = supportFragmentManager.findFragmentById(R.id.face_fragment) as FaceArFragment?

        // Load the face regions renderable.
        // This is a skinned model that renders 3D objects mapped to the regions of the augmented face.

        var error = ""
        ModelRenderable.builder()
            .setSource(
                this, RenderableSource.builder().setSource(
                    this,
                    Uri.parse(url),
                    sourceType
                )
                    .setRecenterMode(RenderableSource.RecenterMode.ROOT)
                    .build()
            )
            .setRegistryId(url)
            .build()
            .thenAccept { renderable: ModelRenderable ->
                faceRegionsRenderable = renderable

                result_load.text = "Model loaded $url"
            }
            .exceptionally { throwable: Throwable? ->
                error = "error: ${throwable.toString()}"
                Log.e(TAG, error)
                null
            }

        val sceneView = arFragment!!.arSceneView

        // This is important to make sure that the camera stream renders first so that
        // the face mesh occlusion works correctly.
        sceneView.cameraStreamRenderPriority = Renderable.RENDER_PRIORITY_FIRST

        val scene = sceneView.scene

        scene.addOnUpdateListener {
            if (faceRegionsRenderable == null) {
                result_load.text = "$error"
                return@addOnUpdateListener
            }

            val faceList = sceneView.session!!.getAllTrackables(AugmentedFace::class.java)

            // Make new AugmentedFaceNodes for any new faces.
            for (face in faceList) {
                if (!faceNodeMap.containsKey(face)) {
                    val faceNode = AugmentedFaceNode(face)
                    faceNode.setParent(scene)
                    faceNode.faceRegionsRenderable = faceRegionsRenderable
                    faceNodeMap[face] = faceNode
                }
            }

            // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
            val iter = faceNodeMap.entries.iterator()
            while (iter.hasNext()) {
                val entry = iter.next()
                val face = entry.key
                if (face.trackingState == TrackingState.STOPPED) {
                    val faceNode = entry.value
                    faceNode.setParent(null)
                    iter.remove()
                }
            }
        }
    }

    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (ArCoreApk.getInstance()
                .checkAvailability(activity) === ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE
        ) {
            Log.e(TAG, "Augmented Faces requires ArCore.")
            Toast.makeText(activity, "Augmented Faces requires ArCore", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        val openGlVersionString =
            (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .deviceConfigurationInfo
                .glEsVersion
        if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                .show()
            activity.finish()
            return false
        }
        return true
    }
}
