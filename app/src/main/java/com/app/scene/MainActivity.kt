package com.app.scene

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.app.scene.arscene.ScreenArCoreSunGlass
import com.app.scene.ui.theme.ArSceneTheme
import com.google.android.filament.Engine
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.AugmentedFace
import io.github.sceneview.ar.node.AugmentedFaceNode
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.ModelNode

const val kModelFile = "models/black1.glb"

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