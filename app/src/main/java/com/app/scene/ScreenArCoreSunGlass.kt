package com.app.scene

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.FilamentInstance
import com.google.ar.core.AugmentedFace
import com.google.ar.core.AugmentedFace.RegionType
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Config.AugmentedFaceMode
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.rendering.Renderable
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.getUpdatedAugmentedFaces
import io.github.sceneview.ar.node.AugmentedFaceNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.managers.build
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import java.util.EnumSet
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.containsKey
import kotlin.collections.set


@SuppressLint("ContextCastToActivity")
@Composable
fun ScreenArCoreSunGlass(modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberARCameraNode(engine)
    val childNodes = rememberNodes()
    val view = rememberView(engine)
    val render = rememberRenderer(engine)
    val scene = rememberScene(engine)
    val collisionSystem = rememberCollisionSystem(view)
    var planeRenderer by remember { mutableStateOf(true) }
    var trackingFailureReason by remember {
        mutableStateOf<TrackingFailureReason?>(
            null
        )
    }
    val faceNodes = remember { mutableMapOf<AugmentedFace, AppAugmentedFaceNode>() }
    val faceCustomNodes = remember { mutableMapOf<AugmentedFace, CustomAugmentedFaceNode>() }

    var logger by remember { mutableStateOf<Pair<String, String>>(Pair("", "")) }

    var frame by remember { mutableStateOf<Frame?>(null) }

    var loadedFilamentAsset = produceState<FilamentInstance?>(
        null,
        { value = modelLoader.loadModelInstance(kModelFile) })

    fun customSolution(session: Session, updatedFrame: Frame) {
        val modelInstance by lazy { modelLoader.createModelInstance(kModelFile) }
        frame = updatedFrame
        try {
            val faces = session.getAllTrackables(AugmentedFace::class.java)
            faces.forEach { face ->
                if (!faceNodes.containsKey(face) &&
                    face.trackingState == TrackingState.TRACKING &&
                    (face.meshVertices?.limit() ?: 0) > 0
                ) {
                    val faceNode = AppAugmentedFaceNode(engine, face, modelInstance)
                    faceNodes[face] = faceNode
                    childNodes += faceNode
                }
            }

            faceNodes.forEach { (face, node) ->
                node.update()
            }

            faceNodes.entries.removeIf { (face, node) ->
                if (face.trackingState != TrackingState.TRACKING) {
                    childNodes -= node
                    true
                } else false
            }
        } catch (e: Exception) {
            logger = "FaceNodeError" to (e.message ?: "Unknown Error")
        }
    }

    fun discordThread(session: Session, updatedFrame: Frame){
        val augmentedFaces = session.getAllTrackables<AugmentedFace>(AugmentedFace::class.java)
        augmentedFaces.forEach { augmentedFace ->
            var existingFaceNode: CustomAugmentedFaceNode? = faceCustomNodes[augmentedFace]
            when (augmentedFace.trackingState) {
                TrackingState.TRACKING -> {
                    if (faceCustomNodes[augmentedFace] == null) {
                        val faceNode = CustomAugmentedFaceNode(
                            engine = engine,
                            augmentedFace = augmentedFace,
                            builder = {
                                this.geometryType(RenderableManager.Builder.GeometryType.STATIC_BOUNDS)
                                this.culling(false)
                                this.build(engine)
                            })

                        faceNode.addChildNode(
                            ModelNode(
                                modelInstance = modelLoader.createModelInstance(
                                    assetFileLocation = "models/glasses.glb"
                                ),
                                scaleToUnits = 1.0f,
                                centerOrigin = Position(0.0f, 0.0f, 0.0f)
                            ).apply {
                                setCulling(false)
                                isShadowCaster = false
                                isShadowReceiver = true
                            }
                        )
                        faceCustomNodes[augmentedFace] = faceNode
                    }
                }

                TrackingState.STOPPED -> {
                    existingFaceNode?.onRemovedFromScene(scene)
                    faceCustomNodes.remove(augmentedFace)
                }

                else -> {
                    faceCustomNodes.remove(augmentedFace)
                }
            }
        }
    }

    LaunchedEffect(logger) {
        if (logger.first.isNotEmpty()) println("LOG :: ${logger.first} :: ${logger.second}")
    }

    Box(modifier = modifier.fillMaxSize()) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            scene = scene,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            view = view,
            collisionSystem = collisionSystem,
            cameraNode = cameraNode,
            cameraStream = rememberARCameraStream(materialLoader),
            childNodes = childNodes,
            planeRenderer = planeRenderer,
            onTrackingFailureChanged = { trackingFailureReason = it },
            onSessionFailed = { ex -> logger = Pair("Exception", ex.message ?: "") },
            sessionFeatures = EnumSet.of(Session.Feature.FRONT_CAMERA),
            sessionCameraConfig = { session ->
                val filter =
                    CameraConfigFilter(session).setFacingDirection(CameraConfig.FacingDirection.FRONT)
                session.cameraConfig = session.getSupportedCameraConfigs(filter).first()
                session.cameraConfig
            },
            sessionConfiguration = { session, config ->
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.depthMode = Config.DepthMode.DISABLED
                config.augmentedFaceMode = AugmentedFaceMode.MESH3D
                session.configure(config)
            },
            renderer = render,
            /*environmentLoader = rememberEnvironmentLoader(engine),
             environment = rememberEnvironment(engine),
             mainLightNode = rememberMainLightNode(engine),*/
            onSessionCreated = {},
            onSessionResumed = {},
            onSessionPaused = {},
            onSessionUpdated = { session, updatedFrame ->
                discordThread(session,updatedFrame)
            },
            onGestureListener = rememberOnGestureListener(),
            /*onTouchEvent = { motionEvent,hitResult -> true},
            activity = context as ComponentActivity ,
            lifecycle = LocalLifecycleOwner.current.lifecycle,
            onViewUpdated = {

            },
            onViewCreated = {

            }*/
        )
    }


}





fun createFaceAnchorNode(
    engine: Engine,
    face: AugmentedFace
): Node {
    val augmentedFaceMode = AugmentedFaceNode(engine, face)
    return augmentedFaceMode
}