package com.example.portraitcamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var captureBtn: ImageButton
    private lateinit var zoomSeek: SeekBar
    private lateinit var exposureSeek: SeekBar
    private lateinit var zoom2Btn: Button

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: java.util.concurrent.ExecutorService

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        captureBtn = findViewById(R.id.captureBtn)
        zoomSeek = findViewById(R.id.zoomSeek)
        exposureSeek = findViewById(R.id.exposureSeek)
        zoom2Btn = findViewById(R.id.zoom2Btn)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }

        captureBtn.setOnClickListener { takePhoto() }

        zoomSeek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val zoomRatio = 1f + (progress / 100f) * 4f
                camera?.cameraControl?.setZoomRatio(zoomRatio)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        zoom2Btn.setOnClickListener {
            camera?.cameraControl?.setZoomRatio(2f)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, selector, preview, imageCapture)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val img = imageCapture ?: return
        val photoFile = File(getExternalFilesDir(null), "IMG_${'$'}{System.currentTimeMillis()}.jpg")
        val out = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        img.takePicture(out, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                CoroutineScope(Dispatchers.Default).launch {
                    val bmp = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                    val outFile = File(getExternalFilesDir(null), "SAVED_${'$'}{System.currentTimeMillis()}.jpg")
                    FileOutputStream(outFile).use { fos -> bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos) }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Saved: ${'$'}{outFile.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            override fun onError(exception: ImageCaptureException) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show() }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
