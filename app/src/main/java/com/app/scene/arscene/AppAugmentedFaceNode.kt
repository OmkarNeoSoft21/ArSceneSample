package com.app.scene.arscene

import com.google.android.filament.Engine
import com.google.ar.core.AugmentedFace
import com.google.ar.core.AugmentedFace.RegionType
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.node.PoseNode
import io.github.sceneview.collision.Vector3
import io.github.sceneview.math.toFloat3
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlin.math.pow
import kotlin.math.sqrt


class AppAugmentedFaceNode(
    engine: Engine,
    private val face: AugmentedFace,
    private val modelInstance: ModelInstance
) : Node(engine) {

    private val centerNode = PoseNode(engine).apply {
        parent = this@AppAugmentedFaceNode
    }

    private var hasPlacedModel = false
    private var glassesNode: ModelNode? = null

    init {
        tryPlaceModel()
    }

    private fun isMeshReady(): Boolean {
        return (face.meshVertices?.limit() ?: 0) > 0 &&
                (face.meshTextureCoordinates?.limit() ?: 0) > 0 &&
                (face.meshTriangleIndices?.limit() ?: 0) > 0
    }

    private fun tryPlaceModel() {
        if (!hasPlacedModel && isMeshReady()) {
            glassesNode = ModelNode(modelInstance).apply {
                // Initial scale - will update dynamically
                scale = Vector3(0.1f, 0.5f, 1f).toFloat3()
                // Start at origin relative to centerNode
                position = Vector3(0f, 0f, 0f).toFloat3()
                rotation = Vector3(0f, 0f, 0f).toFloat3()
                parent = centerNode
            }
            hasPlacedModel = true
        }
    }

    // Helper: compute Euclidean distance between 2 float arrays (3D points)
    private fun distance(a: FloatArray, b: FloatArray): Float {
        return sqrt(
            (a[0] - b[0]).pow(2) + (a[1] - b[1]).pow(2) + (a[2] - b[2]).pow(2)
        )
    }

    operator fun Vector3.plus(other: Vector3) = Vector3(this.x + other.x, this.y + other.y, this.z + other.z)

    fun update() {
        if (face.trackingState == TrackingState.TRACKING) {
            val centerPose = face.centerPose
            val nosePose = face.getRegionPose(RegionType.NOSE_TIP)
            val leftCheekPose = face.getRegionPose(RegionType.FOREHEAD_LEFT)
            val rightCheekPose = face.getRegionPose(RegionType.FOREHEAD_RIGHT)

            if (centerPose != null && nosePose != null && leftCheekPose != null && rightCheekPose != null) {
                // Set centerNode pose to full face pose for position + rotation
                centerNode.pose = centerPose

                // Calculate offset from center to nose tip
                val noseOffset = Vector3(
                    nosePose.tx() - centerPose.tx(),
                    nosePose.ty() - centerPose.ty(),
                    nosePose.tz() - centerPose.tz()
                )

                // Position glasses relative to nose tip with small tuning offset
                val verticalOffset = 0.012f // lift glasses a bit above nose
                val forwardOffset = -0.10f  // pull glasses slightly forward
                glassesNode?.position = noseOffset.toFloat3() + Vector3(0f, verticalOffset, forwardOffset).toFloat3()

                // Calculate distance between cheeks (approximate face width)
                val cheekDistance = distance(leftCheekPose.translation, rightCheekPose.translation)

                // Default glasses base width (tweak as needed for your model)
                val defaultWidth = 0.075f

                // Scale glasses proportionally to face width
                val scaleRatio = cheekDistance / defaultWidth
                glassesNode?.scale = Vector3(scaleRatio, scaleRatio, scaleRatio).toFloat3()
            }
            tryPlaceModel()
        }
    }
}
