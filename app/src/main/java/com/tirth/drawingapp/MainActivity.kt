package com.tirth.drawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var mIBBrushCurrentPaint: ImageButton? = null
    var customProgressDialog: Dialog? = null

// URI: the location of image on your device
    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result.data != null){
                val imageBackground: ImageView = findViewById(R.id.iv_Canvas_Background)
//              It just uses the location of the image instead of creating a copy in our app.
                imageBackground.setImageURI(result.data?.data)
            }
        }

    private var galleryResultLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                        Toast.makeText(
                            this@MainActivity,
                            "Permission for Reading from External Storage is Granted",
                            Toast.LENGTH_LONG
                        ).show()

                        val pickIntent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        openGalleryLauncher.launch(pickIntent)
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI => Image location in our device

                } else {
                    Toast.makeText(this, "Permission Denied for Reading from External Storage", Toast.LENGTH_LONG).show()
                }
            }
        }

//    private var cameraAndLocationResultLauncher: ActivityResultLauncher<Array<String>> =
//        registerForActivityResult(
//            ActivityResultContracts.RequestMultiplePermissions()){
//            permissions ->
//            permissions.entries.forEach{
//                val permissionName = it.key
//                val isGranted = it.value
//                if(isGranted){
//                    if(permissionName == Manifest.permission.ACCESS_FINE_LOCATION) {
//                        Toast.makeText(
//                            this, "Permission for Location is Granted",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//                    else if(permissionName == Manifest.permission.CAMERA){
//                        Toast.makeText(this, "Permission for Camera is Granted",
//                            Toast.LENGTH_LONG).show()
//                    }
//                }else{
//                    if(permissionName == Manifest.permission.ACCESS_FINE_LOCATION) {
//                        Toast.makeText(
//                            this, "Permission for Location is Denied",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//                    else if(permissionName == Manifest.permission.CAMERA){
//                        Toast.makeText(this, "Permission for Camera is Denied",
//                            Toast.LENGTH_LONG).show()
//                    }
//                }
//            }
//
//        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ibBtnGallery: ImageButton = findViewById(R.id.ibGallery)

        ibBtnGallery.setOnClickListener{
            requestStoragePermission()
        }


//        btnGallery.setOnClickListener{
////            if the permission hasn't been granted
//            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
////                it automatically checks if the system has granted the required permission
//                    shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
//                showRationaleDialog("Camera Permission", "Camera cannot be used as access is denied")
//            }
//            else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
////                it automatically checks if the system has granted the required permission
//                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
//                showRationaleDialog("Location Permission", "Location cannot be used as access is denied")
//            }
////            if permission hasn't been decided then show the camera permisssion launcher
//            else{
//                 cameraAndLocationResultLauncher.launch(
//                     arrayOf(Manifest.permission.CAMERA,
//                         Manifest.permission.ACCESS_FINE_LOCATION)
//                 )
//            }
//        }


        drawingView = findViewById(R.id.drawingView)
        drawingView!!.setBrushSize(15.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.llBrushColor)

        mIBBrushCurrentPaint = linearLayoutPaintColors[0] as ImageButton
        mIBBrushCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallete_pressed)
        )

        val ibBrushBtn: ImageButton = findViewById(R.id.ibBrush)
        ibBrushBtn.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        val ibUndoBtn: ImageButton = findViewById(R.id.ibUndo)
        ibUndoBtn.setOnClickListener {
            drawingView!!.onClickUndo()
        }

        val ibSaveBtn: ImageButton = findViewById(R.id.ibSave)
        ibSaveBtn.setOnClickListener {

            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
        }

//        val ibRedoBtn: ImageButton = findViewById(R.id.ibRedo)
//        ibRedoBtn.setOnClickListener {
//            drawingView!!.onClicKRedo()
//        }
    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this,
        Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }


    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            showRationaleDialog("Gallery Permission", "The App needs Access to your External Storage.")
        }else{
            galleryResultLauncher.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun showBrushSizeChooserDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ibBrushSizeSmall)
        smallBtn.setOnClickListener() {
            drawingView!!.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        }

        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ibBrushSizeMedium)
        mediumBtn.setOnClickListener() {
            drawingView!!.setBrushSize(15.toFloat())
            brushDialog.dismiss()
        }

        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ibBrushSizeLarge)
        largeBtn.setOnClickListener() {
            drawingView!!.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

     fun setBrushColor(view: View){
        if(mIBBrushCurrentPaint != view){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setBrushColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallete_pressed)
            )

            mIBBrushCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallete_normal)
            )

            mIBBrushCurrentPaint = view
        }
    }
    private fun showRationaleDialog(
        title: String,
        message: String
    ){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setNeutralButton("Cancel"){ dialog, _ ->
                dialog.dismiss()
            }
        builder.create()
        builder.setCancelable(true)
        builder.show()
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap): String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)

                    val f = File(
                        externalCacheDir?.absoluteFile.toString() +
                                File.separator + "DrawingApp_" + System.currentTimeMillis() / 1000 + ".jpg"
                    )
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread{
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity, "File Saved Successfully: $result", Toast.LENGTH_LONG).show()
//                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity, "Something went wrong while saving the file", Toast.LENGTH_LONG).show()
                        }
                    }
                }catch(e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null){
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
//            Putting the address of image from devise to intent
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }
}