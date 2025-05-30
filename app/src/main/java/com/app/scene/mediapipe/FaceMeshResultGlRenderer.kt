package com.app.scene.mediapipe

import android.graphics.Canvas
import android.graphics.Paint
import android.opengl.GLES20
import android.util.Size
import com.google.common.collect.ImmutableSet
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark
import com.google.mediapipe.solutioncore.ResultGlRenderer
import com.google.mediapipe.solutions.facemesh.FaceMesh
import com.google.mediapipe.solutions.facemesh.FaceMeshConnections
import com.google.mediapipe.solutions.facemesh.FaceMeshResult
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A custom implementation of [ResultGlRenderer] to render [FaceMeshResult].  */
class FaceMeshResultGlRenderer : ResultGlRenderer<FaceMeshResult?> {
    private var program = 0
    private var positionHandle = 0
    private var projectionMatrixHandle = 0
    private var colorHandle = 0

    private fun loadShader(type: Int, shaderCode: String?): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    override fun setupRendering() {
        program = GLES20.glCreateProgram()
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        projectionMatrixHandle = GLES20.glGetUniformLocation(program, "uProjectionMatrix")
        colorHandle = GLES20.glGetUniformLocation(program, "uColor")
    }

    override fun renderResult(result: FaceMeshResult?, projectionMatrix: FloatArray?) {
        if (result == null) { return }
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(projectionMatrixHandle, 1, false, projectionMatrix, 0)

        val numFaces = result.multiFaceLandmarks().size
        for (i in 0..<numFaces) {
//            drawLandmarks(
//                result.multiFaceLandmarks()[i].landmarkList,
//                FaceMeshConnections.FACEMESH_TESSELATION,
//                TESSELATION_COLOR,
//                TESSELATION_THICKNESS
//            )
//            drawLandmarks(
//                result.multiFaceLandmarks()[i].landmarkList,
//                FaceMeshConnections.FACEMESH_RIGHT_EYE,
//                RIGHT_EYE_COLOR,
//                RIGHT_EYE_THICKNESS
//            )
//            drawLandmarks(
//                result.multiFaceLandmarks()[i].landmarkList,
//                FaceMeshConnections.FACEMESH_RIGHT_EYEBROW,
//                RIGHT_EYEBROW_COLOR,
//                RIGHT_EYEBROW_THICKNESS
//            )
//            drawLandmarks(
//                result.multiFaceLandmarks()[i].landmarkList,
//                FaceMeshConnections.FACEMESH_LEFT_EYE,
//                LEFT_EYE_COLOR,
//                LEFT_EYE_THICKNESS
//            )
//            drawLandmarks(
//                result.multiFaceLandmarks()[i].landmarkList,
//                FaceMeshConnections.FACEMESH_LEFT_EYEBROW,
//                LEFT_EYEBROW_COLOR,
//                LEFT_EYEBROW_THICKNESS
//            )
            drawLandmarks(
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_FACE_OVAL,
                FACE_OVAL_COLOR,
                FACE_OVAL_THICKNESS
            )
//            drawLipsFilled(
//                result.multiFaceLandmarks()[i].landmarkList,
//                FaceMeshConnections.FACEMESH_LIPS,
//                LIPS_COLOR
//            )
//            if (result.multiFaceLandmarks()[i].landmarkCount == FaceMesh.FACEMESH_NUM_LANDMARKS_WITH_IRISES
//            ) {
//                drawLandmarks(
//                    result.multiFaceLandmarks()[i].landmarkList,
//                    FaceMeshConnections.FACEMESH_RIGHT_IRIS,
//                    RIGHT_EYE_COLOR,
//                    RIGHT_EYE_THICKNESS
//                )
//                drawLandmarks(
//                    result.multiFaceLandmarks()[i].landmarkList,
//                    FaceMeshConnections.FACEMESH_LEFT_IRIS,
//                    LEFT_EYE_COLOR,
//                    LEFT_EYE_THICKNESS
//                )
//            }
        }
    }

    /**
     * Deletes the shader program.
     *
     *
     * This is only necessary if one wants to release the program while keeping the context around.
     */
    fun release() {
        GLES20.glDeleteProgram(program)
    }


    private fun drawLandmarksFill(
        faceLandmarkList: MutableList<NormalizedLandmark>,
        connections: ImmutableSet<FaceMeshConnections.Connection>,
        colorArray: FloatArray?,
        thickness: Int
    ) {
        GLES20.glUniform4fv(colorHandle, 1, colorArray, 0)
        GLES20.glLineWidth(thickness.toFloat())
        for (c in connections) {
            val start = faceLandmarkList.get(c.start())
            val end = faceLandmarkList.get(c.end())
            val vertex = floatArrayOf(start.getX(), start.getY(), end.getX(), end.getY())
            val vertexBuffer =
                ByteBuffer.allocateDirect(vertex.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(vertex)
            vertexBuffer.position(0)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_POLYGON_OFFSET_FILL, 0, faceLandmarkList.size)
        }
    }

    private fun drawLipsFilled(
        faceLandmarkList: MutableList<NormalizedLandmark>,
        connections: ImmutableSet<FaceMeshConnections.Connection>,
        colorArray: FloatArray?
    ) {
        GLES20.glUniform4fv(colorHandle, 1, colorArray, 0)

        // Collect unique lip points
        val lipPoints = connections.flatMap {
            listOf(faceLandmarkList[it.start()], faceLandmarkList[it.end()])
        }.distinctBy { it.x to it.y }

        if (lipPoints.size < 3) return // Can't form a triangle fan

        // Convert to OpenGL coords (-1 to 1)
        val vertexData = FloatArray(lipPoints.size * 2)
        for (i in lipPoints.indices) {
            vertexData[i * 2] = lipPoints[i].x * 2 - 1       // X in [-1, 1]
            vertexData[i * 2 + 1] = 1 - lipPoints[i].y * 2   // Y in [-1, 1], inverted
        }

        val vertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexData)
        vertexBuffer.position(0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, lipPoints.size)
    }


    private fun drawLandmarks(
        faceLandmarkList: MutableList<NormalizedLandmark>,
        connections: ImmutableSet<FaceMeshConnections.Connection>,
        colorArray: FloatArray?,
        thickness: Int
    ) {
        GLES20.glUniform4fv(colorHandle, 1, colorArray, 0)
        GLES20.glLineWidth(thickness.toFloat())
        for (c in connections) {
            val start = faceLandmarkList.get(c.start())
            val end = faceLandmarkList.get(c.end())
            val vertex = floatArrayOf(start.getX(), start.getY(), end.getX(), end.getY())
            val vertexBuffer =
                ByteBuffer.allocateDirect(vertex.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(vertex)
            vertexBuffer.position(0)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 2)
        }
    }

    companion object {
        private const val TAG = "FaceMeshResultGlRenderer"

        private val TESSELATION_COLOR = floatArrayOf(0.75f, 0.75f, 0.75f, 0.5f)
        private const val TESSELATION_THICKNESS = 5
        private val RIGHT_EYE_COLOR = floatArrayOf(1f, 0.2f, 0.2f, 1f)
        private const val RIGHT_EYE_THICKNESS = 8
        private val RIGHT_EYEBROW_COLOR = floatArrayOf(1f, 0.2f, 0.2f, 1f)
        private const val RIGHT_EYEBROW_THICKNESS = 8
        private val LEFT_EYE_COLOR = floatArrayOf(0.2f, 1f, 0.2f, 1f)
        private const val LEFT_EYE_THICKNESS = 8
        private val LEFT_EYEBROW_COLOR = floatArrayOf(0.2f, 1f, 0.2f, 1f)
        private const val LEFT_EYEBROW_THICKNESS = 8
        private val FACE_OVAL_COLOR = floatArrayOf(0.9f, 0.9f, 0.9f, 1f)
        private const val FACE_OVAL_THICKNESS = 8
        private val LIPS_COLOR = floatArrayOf(0.47f, 0.25f, 0.59f, 1f)
        private const val LIPS_THICKNESS = 8
        private val VERTEX_SHADER = ("uniform mat4 uProjectionMatrix;\n"
                + "attribute vec4 vPosition;\n"
                + "void main() {\n"
                + "  gl_Position = uProjectionMatrix * vPosition;\n"
                + "}")
        private val FRAGMENT_SHADER = ("precision mediump float;\n"
                + "uniform vec4 uColor;\n"
                + "void main() {\n"
                + "  gl_FragColor = uColor;\n"
                + "}")
    }
}