package com.app.scene

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.app.scene.ui.theme.ArSceneTheme
import com.google.android.filament.Engine
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config.AugmentedFaceMode
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.github.sceneview.SceneView
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.configure
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.node.AugmentedFaceNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import kotlinx.coroutines.delay
import java.util.EnumSet

const val kModelFile = "models/glasses.glb"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability != Availability.SUPPORTED_INSTALLED) {
            Toast.makeText(this, "ARCore not supported on this device", Toast.LENGTH_LONG).show()
        }

        setContent {
            ArSceneTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    //ScreenSample(modifier = Modifier.padding(innerPadding))
                    ScreenArCoreSunGlass(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}


fun createAnchorNode(
    engine: Engine,
    modelLoader: ModelLoader,
    face: AugmentedFace,
): AugmentedFaceNode {
    val anchorNode = AugmentedFaceNode(engine, face)
    val modelNode = ModelNode(
        modelInstance = modelLoader.createModelInstance(kModelFile),
        scaleToUnits = 0.5f
    )
    anchorNode.addChildNode(modelNode)
    return anchorNode
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ArSceneTheme {
        Greeting("Android")
    }
}