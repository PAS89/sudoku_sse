package sse.sudo.sceneform

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture

const val TAG = "SetMaterialSample"
private const val MIN_OPENGL_VERSION = 3.0

class MainActivity : AppCompatActivity() {


    private lateinit var materialFuture: CompletableFuture<Setmaterial>

    private lateinit var setMat: Setmaterial
    private lateinit var arFragment: ArFragment
    private lateinit var renderableModel: ModelRenderable

    private lateinit var renderableFuture: CompletableFuture<ModelRenderable>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!checkIsSupportedDeviceOrFinish(this)) return

        arFragment = arFragmentView as ArFragment

        materialFuture = Setmaterial.build(this) {
            baseColorSource = Uri.parse("textures/board.png")

        }

        renderableFuture = ModelRenderable.builder()
            .setSource(this, Uri.parse("board.sfb"))
            .build()

        renderableFuture.thenAcceptBoth(materialFuture) { renderableResult, materialResult ->
            Log.i(TAG, "init material and renderable")
            setMat = materialResult
            renderableModel = renderableResult
            renderableModel.material = setMat.value
        }

        colorButton.setText(R.string.set_color)

        colorButton.setOnClickListener {
            setMat.switchBaseColor()
            if (setMat.isDefaultBaseColorMap) {
                colorButton.setText(R.string.set_color)
            } else
                colorButton.setText(R.string.reset_color)
        }


        resetMaterialButton.setOnClickListener {
            setMat.reset()
            colorButton.setText(R.string.set_color)
        }

        arFragment.setOnTapArPlaneListener { hitResult: HitResult, _: Plane, _: MotionEvent ->
            val anchor: Anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment.arSceneView.scene)

            val modelNode = TransformableNode(arFragment.transformationSystem)
            modelNode.setParent(anchorNode)
            modelNode.renderable = renderableModel
            modelNode.select()
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later")
            Toast.makeText(this, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        val openGlVersionString: String = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .deviceConfigurationInfo
            .glEsVersion
        if (openGlVersionString.toDouble() < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 or later")
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG).show()
            activity.finish()
            return false
        }
        return true
    }
}
