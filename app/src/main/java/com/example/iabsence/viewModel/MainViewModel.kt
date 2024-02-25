package com.example.iabsence.viewModel

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.iabsence.model.DataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import java.io.File
import java.util.concurrent.Executor
import javax.inject.Inject
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: DataRepository
) : ViewModel() {
    suspend fun takePhoto(
        filenameFormat: String,
        imageCapture: ImageCapture,
        outputDirectory: File,
        executor: Executor,
        onImageCaptured: (Uri) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        repository.takePhoto(
            filenameFormat,
            imageCapture,
            outputDirectory,
            executor,
            onImageCaptured,
            onError
        )
    }

}