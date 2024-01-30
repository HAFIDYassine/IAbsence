package com.example.iabsence

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer

class CameraActivity : Activity() {
    private val REQUEST_IMAGE_CAPTURE = 1
    private lateinit var interpreter: Interpreter
    private val num_classes = 3
    private val image_width = 224
    private val image_height = 224
    private val channels =3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }

        // Load your TensorFlow Lite model
        val modelFile: MappedByteBuffer = FileUtil.loadMappedFile(this, "face_recognition_model.tflite")
        interpreter = Interpreter(modelFile)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap

            // Preprocess the image
            val input = preprocessImage(imageBitmap)

            // Run the model
            val output = Array(1) { FloatArray(num_classes) }
            interpreter.run(input, output)

            // Postprocess and display the results
            val result = postprocessOutput(output[0])
            Toast.makeText(this, "Predicted class: $result", Toast.LENGTH_LONG).show()
        }
    }

    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // Resize the bitmap to the size expected by your model
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, image_width, image_height, true)

        // Convert the resized bitmap to a ByteBuffer
        val byteBuffer = ByteBuffer.allocateDirect(4 * image_height * image_width * channels).order(ByteOrder.nativeOrder())
        for (y in 0 until image_height) {
            for (x in 0 until image_width) {
                val pixelValue = resizedBitmap.getPixel(x, y)

                // Normalize pixel value to [0..1]
                byteBuffer.putFloat((pixelValue shr 16 and 0xFF) / 255.0f) // Red
                byteBuffer.putFloat((pixelValue shr 8 and 0xFF) / 255.0f) // Green
                byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f) // Blue
            }
        }
        return byteBuffer
    }

    private fun postprocessOutput(output: FloatArray): String {
        // Find the class with the highest probability
        val maxIndex = output.indices.maxByOrNull { output[it] } ?: -1

        val classNames = listOf("Messi", "CR7", "Hakimi")
        return classNames[maxIndex]
    }
}