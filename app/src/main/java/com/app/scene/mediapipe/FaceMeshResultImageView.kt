// Copyright 2021 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.app.scene.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Size
import androidx.appcompat.widget.AppCompatImageView
import com.google.common.collect.ImmutableSet
import com.google.mediapipe.formats.proto.LandmarkProto
import com.google.mediapipe.solutions.facemesh.FaceMesh
import com.google.mediapipe.solutions.facemesh.FaceMeshConnections
import com.google.mediapipe.solutions.facemesh.FaceMeshResult
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt

/** An ImageView implementation for displaying [FaceMeshResult].  */
class FaceMeshResultImageView(context: Context) : AppCompatImageView(context) {
    private var latest: Bitmap? = null

    init {
        setScaleType(ScaleType.FIT_CENTER)
    }

    /**
     * Sets a [FaceMeshResult] to render.
     *
     * @param result a [FaceMeshResult] object that contains the solution outputs and the input
     * [Bitmap].
     */
    fun setFaceMeshResult(result: FaceMeshResult?) {
        if (result == null) {
            return
        }
        val bmInput: Bitmap = result.inputBitmap()
        val width = bmInput.getWidth()
        val height = bmInput.getHeight()
        latest = createBitmap(width, height, bmInput.getConfig()!!)
        val canvas = Canvas(latest!!)
        val imageSize = Size(width, height)
        canvas.drawBitmap(bmInput, Matrix(), null)
        val numFaces: Int = result.multiFaceLandmarks().size
        for (i in 0..<numFaces) {
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_TESSELATION,
                imageSize,
                TESSELATION_COLOR,
                TESSELATION_THICKNESS
            )
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_RIGHT_EYE,
                imageSize,
                RIGHT_EYE_COLOR,
                RIGHT_EYE_THICKNESS
            )
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_RIGHT_EYEBROW,
                imageSize,
                RIGHT_EYEBROW_COLOR,
                RIGHT_EYEBROW_THICKNESS
            )
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_LEFT_EYE,
                imageSize,
                LEFT_EYE_COLOR,
                LEFT_EYE_THICKNESS
            )
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_LEFT_EYEBROW,
                imageSize,
                LEFT_EYEBROW_COLOR,
                LEFT_EYEBROW_THICKNESS
            )
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_FACE_OVAL,
                imageSize,
                FACE_OVAL_COLOR,
                FACE_OVAL_THICKNESS
            )
            drawLandmarksOnCanvas(
                canvas,
                result.multiFaceLandmarks()[i].landmarkList,
                FaceMeshConnections.FACEMESH_LIPS,
                imageSize,
                LIPS_COLOR,
                LIPS_THICKNESS
            )
            if (result.multiFaceLandmarks()[i].landmarkCount == FaceMesh.FACEMESH_NUM_LANDMARKS_WITH_IRISES
            ) {
                drawLandmarksOnCanvas(
                    canvas,
                    result.multiFaceLandmarks()[i].landmarkList,
                    FaceMeshConnections.FACEMESH_RIGHT_IRIS,
                    imageSize,
                    RIGHT_EYE_COLOR,
                    RIGHT_EYE_THICKNESS
                )
                drawLandmarksOnCanvas(
                    canvas,
                    result.multiFaceLandmarks()[i].landmarkList,
                    FaceMeshConnections.FACEMESH_LEFT_IRIS,
                    imageSize,
                    LEFT_EYE_COLOR,
                    LEFT_EYE_THICKNESS
                )
            }
        }
    }

    /** Updates the image view with the latest [FaceMeshResult].  */
    fun update() {
        postInvalidate()
        if (latest != null) {
            setImageBitmap(latest)
        }
    }

    private fun drawLandmarksOnCanvas(
        canvas: Canvas,
        faceLandmarkList: MutableList<LandmarkProto.NormalizedLandmark>,
        connections: ImmutableSet<FaceMeshConnections.Connection>,
        imageSize: Size,
        color: Int,
        thickness: Int
    ) {
        // Draw connections.
        for (c in connections) {
            val connectionPaint = Paint()
            connectionPaint.setColor(color)
            connectionPaint.strokeWidth = thickness.toFloat()
            val start: LandmarkProto.NormalizedLandmark = faceLandmarkList[c.start()]
            val end: LandmarkProto.NormalizedLandmark = faceLandmarkList[c.end()]
            canvas.drawLine(
                start.x * imageSize.width,
                start.y * imageSize.height,
                end.x * imageSize.width,
                end.y * imageSize.height,
                connectionPaint
            )
        }
    }

    companion object {
        private const val TAG = "FaceMeshResultImageView"

        private val TESSELATION_COLOR = "#70C0C0C0".toColorInt()
        private const val TESSELATION_THICKNESS = 3 // Pixels
        private val RIGHT_EYE_COLOR = "#FF3030".toColorInt()
        private const val RIGHT_EYE_THICKNESS = 5 // Pixels
        private val RIGHT_EYEBROW_COLOR = "#FF3030".toColorInt()
        private const val RIGHT_EYEBROW_THICKNESS = 5 // Pixels
        private val LEFT_EYE_COLOR = "#30FF30".toColorInt()
        private const val LEFT_EYE_THICKNESS = 5 // Pixels
        private val LEFT_EYEBROW_COLOR = "#30FF30".toColorInt()
        private const val LEFT_EYEBROW_THICKNESS = 5 // Pixels
        private val FACE_OVAL_COLOR = "#E0E0E0".toColorInt()
        private const val FACE_OVAL_THICKNESS = 5 // Pixels
        private val LIPS_COLOR = "#FF03DAC5".toColorInt()
        private const val LIPS_THICKNESS = 5 // Pixels
    }
}