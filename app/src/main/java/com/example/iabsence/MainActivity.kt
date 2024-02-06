package com.example.iabsence

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.rememberImagePainter
import com.example.iabsence.ml.FaceRecognitionModel2
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private lateinit var outputDirectory: File
private lateinit var cameraExecutor: ExecutorService
private lateinit var photoUri: Uri
private var shouldShowPhoto: MutableState<Boolean> = mutableStateOf(false)

private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            if (shouldShowCamera.value) {
                CameraView(
                    outputDirectory = outputDirectory,
                    executor = cameraExecutor,
                    onImageCaptured = ::handleImageCapture,
                    onError = { Log.e("IAbsence", "View error:", it) }
                )
            }

            if (shouldShowPhoto.value) {
                Image(
                    painter = rememberImagePainter(photoUri),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        requestCameraPermission()

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("IAbsence", "Permission granted")
            shouldShowCamera.value = true
        } else {
            Log.i("IAbsence", "Permission denied")
        }
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i("IAbsence", "Permission previously granted")
                shouldShowCamera.value = true
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.CAMERA
            ) -> Log.i("IAbsence", "Show camera permissions dialog")

            else -> requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
private fun handleImageCapture(uri: Uri): FloatArray {
    val currentTime = Calendar.getInstance().time
    val formatter = android.icu.text.SimpleDateFormat("dd/MM/yyyy-HH:mm:ss")
    var formattedTime = formatter.format(currentTime)
    Log.i("IAbsence", "Image captured: $uri")

    // Load the image into a Bitmap
    val bitmap = BitmapFactory.decodeFile(uri.path)

    // Resize the Bitmap to 224x224 pixels which is the input size of the model
    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

    // Convert the Bitmap to a ByteBuffer
    val byteBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3)
    byteBuffer.order(ByteOrder.nativeOrder())
    val intValues = IntArray(224 * 224)
    resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)
    var pixel = 0
    for (i in 0 until 224) {
        for (j in 0 until 224) {
            val value = intValues[pixel++]
            byteBuffer.putFloat(((value shr 16 and 0xFF) / 255.0f))
            byteBuffer.putFloat(((value shr 8 and 0xFF) / 255.0f))
            byteBuffer.putFloat(((value and 0xFF) / 255.0f))
        }
    }

    // Load the model
        val model = FaceRecognitionModel2.newInstance(this)

        // Create an input tensor
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)

        // Run model inference and get the result
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        // Log the model output
        Log.d("IAbsence", "Model output: ${outputFeature0.floatArray.contentToString()}")

        runOnUiThread {
            Toast.makeText(this, "Bienvenue ${outputFeature0.floatArray.contentToString()} !!", Toast.LENGTH_LONG).show()
        }
        // Release model resources if no longer used
        model.close()

        // Delete the photo file
        uri.path?.let { File(it).delete() }

        // Return the model output
        return outputFeature0.floatArray
}

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }

        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}