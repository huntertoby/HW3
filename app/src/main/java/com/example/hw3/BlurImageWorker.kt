package com.example.hw3

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.FileOutputStream

class BlurImageWorker(private val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val inputImagePath = inputData.getString("IMAGE_PATH") ?: return Result.failure()
        val outputImagePath = inputData.getString("OUTPUT_PATH") ?: return Result.failure()

        return try {
            val inputBitmap = BitmapFactory.decodeFile(inputImagePath)
            val outputBitmap = blurBitmap(inputBitmap)

            val outputFile = File(outputImagePath)
            outputFile.outputStream().use { out ->
                outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val outputData = workDataOf("BLURRED_IMAGE_PATH" to outputImagePath)
            makeStatusNotification("你的照片已經模糊完畢", context)
            Result.success(outputData)
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun blurBitmap(bitmap: Bitmap,): Bitmap {
        val input = Bitmap.createScaledBitmap(
            bitmap,
            bitmap.width/(10),
            bitmap.height/(10),
            true
        )
        return Bitmap.createScaledBitmap(input, bitmap.width, bitmap.height, true)
    }



}


fun makeStatusNotification(message: String, context: Context) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val name = "BlurImageNotice"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("1", name, importance)
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        notificationManager?.createNotificationChannel(channel)
    }

    val builder = NotificationCompat.Builder(context, "1")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("照片模糊")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVibrate(LongArray(0))

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }
    NotificationManagerCompat.from(context).notify(1, builder.build())
}