package com.ttu.mbam

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.ByteBuffer

class MeterDetectionHelper(private val context: Context) {
    private var interpreter: Interpreter
    private val imageProcessor: ImageProcessor

    init {
        val model = FileUtil.loadMappedFile(context, "model_unquant.tflite")
        interpreter = Interpreter(model)

        // Reuse processor sekali saja
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()
    }

    fun isMeterDetected(bitmap: Bitmap): Boolean {
        val sharedPreferences = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val accuracyThreshold = sharedPreferences.getFloat("ai_accuracy", 0.6f)

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        val processedImage = imageProcessor.process(tensorImage)

        val inputBuffer: ByteBuffer = processedImage.buffer
        val output = Array(1) { FloatArray(2) }

        interpreter.run(inputBuffer, output)

        return output[0][0] > accuracyThreshold
    }
}

