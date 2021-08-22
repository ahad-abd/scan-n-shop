package com.example.scanandshop

import android.Manifest
import android.Manifest.permission.CAMERA
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.scanandshop.ml.MobilenetV110224Quant
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import kotlinx.android.synthetic.main.activity_main.*
import java.io.InputStream


class MainActivity : AppCompatActivity() {

    private lateinit var bitmap: Bitmap


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fileName = "label.txt"
        val inputString = application.assets.open(fileName).bufferedReader().use { it.readText() }
        val townList = inputString.split("\n")

        ibSearch.isEnabled = false

        // BUTTON PICK IMAGE
        btnPick.setOnClickListener{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                    // permission denied
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    requestPermissions(permissions, PERMISSION_CODE)
                }
                else {
                    //permission already granted
                    pickImageFromGallery()
                }
            }
            else {
//                system os is >= Marshmallow
                pickImageFromGallery()
            }
        }



        btnCapture.isEnabled = false
        if(ActivityCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(CAMERA),111)
        }
        else
            btnCapture.isEnabled = true

        btnCapture.setOnClickListener{
            val i = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(i,101)
        }
        ibSearch.setOnClickListener{
            val resized : Bitmap = Bitmap.createScaledBitmap(bitmap,224,224,true)

            val model = MobilenetV110224Quant.newInstance(this)

// Creates inputs for reference.

            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.UINT8)
            val tbuffer: TensorImage = TensorImage.fromBitmap(resized)
            val byteBuffer = tbuffer.buffer
            inputFeature0.loadBuffer(byteBuffer)

// Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            val max = getMax(outputFeature0.floatArray)



// Releases model resources if no longer used.
            model.close()

//            https://www.amazon.in/s?k=dining+table&ref=nb_sb_noss_2

            var resultobject : String = townList[max]
            resultobject = resultobject.replace(" ","+")
            gotourl(resultobject)

            ibSearch.isEnabled = false
        }
    }

    private fun gotourl(s: String) {
        val s1 = "https://www.amazon.in/s?k="
        val s2 = "&ref=nb_sb_noss_2"
        val s3 = s1+s+s2
        val openURL = Intent(Intent.ACTION_VIEW)
        openURL.data = Uri.parse(s3)
        startActivity(openURL)
    }

    private fun pickImageFromGallery() {
        // intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    companion object {
        private const val IMAGE_PICK_CODE = 1000
        private const val PERMISSION_CODE = 1001
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 101){

            val pic = data?.getParcelableExtra<Bitmap>("data")
            ivImage.setImageBitmap(pic)
            if (pic != null) {
                bitmap = pic

        }}
        if(resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE){
            ivImage.setImageURI(data?.data)
            val inputstream : InputStream? = data?.data?.let { contentResolver.openInputStream(it) }
            bitmap = BitmapFactory.decodeStream(inputstream)

        }
            ibSearch.isEnabled = true
    }

    private fun getMax(arr:FloatArray): Int{
        var ind = 0
        var min = 0.0f
        for(i in 0..1000){
            if(arr[i]>min){
                ind = i
                min = arr[i]
            }
        }
        return ind

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 111 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            btnCapture.isEnabled = true
        }
        if(requestCode == IMAGE_PICK_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            // permission from popup granted
            pickImageFromGallery()
        }
        else {
            // permission from popup denied
            Toast.makeText(this,"Permission denied",Toast.LENGTH_SHORT).show()
        }
    }}
