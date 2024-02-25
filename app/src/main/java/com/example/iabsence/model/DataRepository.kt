package com.example.iabsence.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.example.iabsence.ml.FaceRecognitionModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import javax.inject.Inject
import android.os.Handler
import android.os.Looper

private lateinit var outputDirectory: File
private lateinit var cameraExecutor: ExecutorService
private lateinit var photoUri: Uri
private var shouldShowPhoto: MutableState<Boolean> = mutableStateOf(false)
private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)

class DataRepository @Inject constructor(@ApplicationContext private val context: Context) {
    fun takePhoto(
        filenameFormat: String,
        imageCapture: ImageCapture,
        outputDirectory: File,
        executor: Executor,
        onImageCaptured: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val imageFile = createFile(outputDirectory, filenameFormat, ".jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(imageFile)
                    onImageCaptured(savedUri)

                    handleImageCapture(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    fun createFile(baseFolder: File, format: String, extension: String): File {
        val timeStamp = SimpleDateFormat(format, Locale.US).format(System.currentTimeMillis())
        return File(baseFolder, "${timeStamp}$extension")
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleImageCapture(uri: Uri) {

        val currentTime = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val formattedTime = currentTime.format(formatter)
        Log.i("IAbsence", "Image captured: $uri")
        photoUri = uri
        shouldShowPhoto.value = true

        // Load the image into a Bitmap
        val bitmap = BitmapFactory.decodeFile(photoUri.path)

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
                byteBuffer.putFloat(((value shr 16 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value and 0xFF) - 127.5f) / 127.5f)
            }
        }

        // Load the model
        val model = FaceRecognitionModel.newInstance(context)

        // Create the inputs for the model
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        inputFeature0.loadBuffer(byteBuffer)

        // Run the model and get the result
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        // Map the output indices to names
        val names = arrayOf("Alkaya Sidi TOURE", "Riane SMILI", "Yassine HAFID")
        val maxIndex = outputFeature0.floatArray.indices.maxByOrNull { outputFeature0.floatArray[it] } ?: -1
        val className = names[maxIndex]

        if (outputFeature0.floatArray[maxIndex] < 0.75) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Étudiant non reconnu", Toast.LENGTH_SHORT).show()
            }
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Bienvenue $className !!", Toast.LENGTH_SHORT).show()
            }
                if (outputFeature0.floatArray[maxIndex] > 0.75) {
                    GlobalScope.launch(Dispatchers.Main) {
                        val jsonArray = JSONArray()
                        val jsonObject1 = JSONObject()
                        jsonObject1.put("name", className)
                        jsonObject1.put("time", formattedTime)
                        jsonArray.put(jsonObject1)

                        val response = sendToServer(jsonArray)
                        // handle response here
                    }
                }
        }
        Log.i("IAbsence", "Model output: $className")

        model.close()
    }

    suspend fun sendToServer(jsonArray: JSONArray): String? {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = jsonArray.toString().toRequestBody(JSON)

            val request = Request.Builder()
                .url("http://10.0.80.184:5000/android_ml")
                .post(body)
                .build()

            try {
                Log.i("IAbsence", "Sending data to server: $jsonArray")
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    response.body?.string()
                }
            } catch (e: Exception) {
                Log.e("IAbsence", "Error sending data to server", e)
                null
            }
        }
    }

}