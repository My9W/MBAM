package com.ttu.mbam

import android.graphics.BitmapFactory
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.android.gms.tasks.Tasks
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BenchmarkTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()
    @get:Rule
    val grantPermissionRule = GrantPermissionRule.grant(
        if (Build.VERSION.SDK_INT >= 33) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else if (Build.VERSION.SDK_INT >= 29) {
            android.Manifest.permission.READ_EXTERNAL_STORAGE // masih diperbolehkan di SDK 29-32
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    @Test
    fun testMeterDetectionAndOcr30x() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val meterDetector = MeterDetectionHelper(context)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        for (i in 1..10) {
            val filename = "test_meter_$i.jpg"
            val inputStream = context.assets.open(filename)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            val startTimeDetect = System.currentTimeMillis()
            val isDetected = meterDetector.isMeterDetected(bitmap)
            val durationDetect = System.currentTimeMillis() - startTimeDetect

            Log.d("BENCHMARK", "[$i] AI: $isDetected dalam $durationDetect ms")

            if (isDetected) {
                val image = InputImage.fromBitmap(bitmap, 0)
                val startOcr = System.currentTimeMillis()
                val task = recognizer.process(image)
                val result = Tasks.await(task)
                val durationOcr = System.currentTimeMillis() - startOcr
                val digits = result.text.filter { it.isDigit() }

                Log.d("BENCHMARK", "[$i] OCR: ${digits} dalam $durationOcr ms")
            } else {
                Log.d("BENCHMARK", "[$i] OCR dilewati")
            }

            Thread.sleep(500)
        }
    }
}
