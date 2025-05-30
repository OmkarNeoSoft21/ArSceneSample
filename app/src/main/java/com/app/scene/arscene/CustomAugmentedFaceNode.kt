package com.app.scene.arscene

import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.VertexBuffer
import com.google.android.filament.VertexBuffer.AttributeType
import com.google.android.filament.VertexBuffer.VertexAttribute.POSITION
import com.google.android.filament.VertexBuffer.VertexAttribute.TANGENTS
import com.google.android.filament.VertexBuffer.VertexAttribute.UV0
import com.google.ar.core.AugmentedFace
import com.google.ar.core.AugmentedFace.RegionType
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.node.PoseNode
import io.github.sceneview.ar.node.TrackableNode
import io.github.sceneview.node.MeshNode
import kotlin.collections.component1
import kotlin.collections.component2



open class CustomAugmentedFaceNode(
    engine: Engine,
    val augmentedFace: AugmentedFace,
    meshMaterialInstance: MaterialInstance? = null,
    builder: RenderableManager.Builder.() -> Unit = {},
    onTrackingStateChanged: ((TrackingState) -> Unit)? = null,
    onUpdated: ((AugmentedFace) -> Unit)? = null
) : TrackableNode<AugmentedFace>(
    engine = engine,
    onTrackingStateChanged = onTrackingStateChanged,
    onUpdated = onUpdated
) {

    /**
     * The center of the face, defined to have the origin located behind the nose and between the
     * two cheek bones.
     *
     * Z+ is forward out of the nose, Y+ is upwards, and X+ is towards the left.
     * The units are in meters. When the face trackable state is TRACKING, this pose is synced with
     * the latest frame. When face trackable state is PAUSED, an identity pose will be returned.
     *
     * Use [regionNodes] to retrieve poses of specific regions of the face.
     */
    val centerNode = PoseNode(engine).apply { parent =this@CustomAugmentedFaceNode }

    val meshNode = MeshNode(
        engine = engine,
        primitiveType = PrimitiveType.TRIANGLES,
        vertexBuffer = VertexBuffer.Builder()
            // Position + Normals + UV Coordinates
            .bufferCount(3)
            // Position Attribute (x, y, z)
            .attribute(POSITION, 1, AttributeType.FLOAT3)
            // Tangents Attribute (Quaternion: x, y, z, w)
            .attribute(TANGENTS, 1, AttributeType.FLOAT4)
            .normalized(TANGENTS)
            // Uv Attribute (x, y)
            .attribute(UV0, 2, AttributeType.FLOAT2)
            .build(engine).apply {
                setBufferAt(engine, 2, augmentedFace.meshTextureCoordinates)
            },
        indexBuffer = IndexBuffer.Builder()
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .indexCount(augmentedFace.meshTriangleIndices.limit())
            .build(engine).apply {
                setBuffer(engine, augmentedFace.meshTriangleIndices)
            },
        materialInstance = meshMaterialInstance,
        builder = builder
    ).apply { parent = centerNode }

    /**
     * The region nodes at the tip of the nose, the detected face's left side of the forehead,
     * the detected face's right side of the forehead.
     *
     * Defines face regions to query the pose for. Left and right are defined relative to the person
     * that the mesh belongs to. To retrieve the center pose use [AugmentedFace.getCenterPose].
     */
    val regionNodes = RegionType.entries.associateWith {
        PoseNode(engine).apply { parent =this@CustomAugmentedFaceNode }
    }

    init {
        trackable = augmentedFace
    }

    override fun update(trackable: AugmentedFace?) {
        super.update(trackable)

        if (augmentedFace.trackingState == TrackingState.TRACKING) {
            centerNode.pose = augmentedFace.centerPose
            meshNode.vertexBuffer.setBufferAt(engine, 1, augmentedFace.meshVertices)
            meshNode.vertexBuffer.setBufferAt(engine, 1, augmentedFace.meshTextureCoordinates)
            regionNodes.forEach { (regionType, regionNode) ->
                regionNode.pose = augmentedFace.getRegionPose(regionType)
            }
        }
    }
}