package com.example.hw3

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.hw3.ui.theme.HW3Theme
import java.io.File


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.CAMERA), 1
            )
        }
        setContent {
            HW3Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BlurImage(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun BlurImage(modifier: Modifier = Modifier) {


    var hasCameraPermission by remember { mutableStateOf(false) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current


    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageBitmap = getBitmapFromUri(context, it)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            imageBitmap = it
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
        if (isGranted) {
            takePictureLauncher.launch(null)
        }
    }

    LaunchedEffect(Unit) {
        WorkManager.getInstance(context).getWorkInfosByTagLiveData("image_blur")
            .observeForever { workInfos ->
                if (workInfos.isNullOrEmpty()) return@observeForever

                val workInfo = workInfos[0]
                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    val blurredImagePath = workInfo.outputData.getString("BLURRED_IMAGE_PATH")
                    blurredImagePath?.let {
                        imageBitmap = BitmapFactory.decodeFile(it)
                    }
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = modifier
                .padding(24.dp)
                .weight(2f)
        ) {
            imageBitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            Row {
                Button(
                    onClick = {
                        if (!hasCameraPermission) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            takePictureLauncher.launch(null)
                        }
                    },
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(text = "Take a picture")
                }
                Button(
                    onClick = {
                        pickImageLauncher.launch("image/*")
                    },
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(text = "Choose a picture")
                }
            }
            Button(
                onClick = {
                    imageBitmap?.let { bitmap ->
                        val inputFile = saveBitmapToFile(context, bitmap)
                        val inputImagePath = inputFile.absolutePath
                        val outputImagePath =
                            File(context.cacheDir, "blurred_image.png").absolutePath
                        val workRequest = OneTimeWorkRequestBuilder<BlurImageWorker>()
                            .setInputData(
                                workDataOf(
                                    "IMAGE_PATH" to inputImagePath,
                                    "OUTPUT_PATH" to outputImagePath
                                )
                            )
                            .addTag("image_blur")
                            .build()
                        WorkManager.getInstance(context).enqueue(workRequest)
                    }
                },
                Modifier.fillMaxWidth()
            ) {
                Text(text = "Blur Picture")
            }
        }
    }
}

fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return context.contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    }
}

fun saveBitmapToFile(context: Context, bitmap: Bitmap): File {
    val file = File(context.cacheDir, "selected_image.png")
    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return file
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HW3Theme {
        BlurImage()
    }
}