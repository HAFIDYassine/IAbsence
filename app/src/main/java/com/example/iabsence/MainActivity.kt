package com.example.iabsence

import android.annotation.SuppressLint
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
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.rememberImagePainter
import com.example.iabsence.ml.FaceRecognitionModel
import com.example.iabsence.ui.theme.IAbsenceTheme
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private lateinit var outputDirectory: File
private lateinit var cameraExecutor: ExecutorService
private lateinit var photoUri: Uri
private var shouldShowPhoto: MutableState<Boolean> = mutableStateOf(false)

private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IAbsenceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Create the necessary parameters for CameraScreen
                    val outputDirectory = getExternalFilesDir("Images")
                    val executor = Executors.newSingleThreadExecutor()
                    val onImageCaptured: (Uri) -> Unit = { /* Your code here */ }
                    val onError: (ImageCaptureException) -> Unit = { /* Your code here */ }

                    // Call CameraScreen with the parameters
                    if (outputDirectory != null) {
                        CameraScreen(
                            outputDirectory = outputDirectory,
                            executor = executor,
                            onImageCaptured = onImageCaptured,
                            onError = onError
                        )
                    }
                }
            }
        }
    }
}

