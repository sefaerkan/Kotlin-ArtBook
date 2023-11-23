package com.example.artbook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.artbook.databinding.ActivityDetailsBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private lateinit var activityResult: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var selectedBitmap : Bitmap? = null
    private lateinit var database : SQLiteDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
        database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR,year VARCHAR,image BLOB)")

        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")
        if(info.equals("new")) {
            binding.imageView.setImageResource(R.drawable.select)
            binding.artName.setText("")
            binding.artistName.setText("")
            binding.yearText.setText("")
            binding.button.visibility = View.VISIBLE
            binding.button2.visibility = View.INVISIBLE
        } else {
            binding.button.visibility = View.INVISIBLE
            binding.button2.visibility = View.VISIBLE
            val selectedId = intent.getIntExtra("id",1)

            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artArtistName = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()) {
                binding.artName.setText(cursor.getString(artNameIx))
                binding.artistName.setText(cursor.getString(artArtistName))
                binding.yearText.setText(cursor.getString(yearIx))

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }
            cursor.close()
        }

    }

    fun selectImage(view: View) {

        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.TIRAMISU) {
            //Android 33+ -> READ_MEDIA_IMAGES
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_MEDIA_IMAGES)) {

                    Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"
                    ) {
                        //İzin istiyoruz
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }.show()

                } else {
                    //İzin istiyoruz
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }

            } else {
                //zaten izin varsa
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI) //Galeriye git Actionpick ile fotoğrafı al
                activityResult.launch(intentToGallery)
            }
        } else {
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission"
                    ) {
                        //İzin istiyoruz
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }.show()

                } else {
                    //İzin istiyoruz
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

            } else {
                //zaten izin varsa
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI) //Galeriye git Actionpick ile fotoğrafı al
                activityResult.launch(intentToGallery)
            }
        }
    }

    private fun registerLauncher() {

        activityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> //Galeriye gitmek ve fotoğrafı seçme
            if(result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if(intentFromResult != null) {
                    val imageUri = intentFromResult.data
                    if(imageUri != null) {
                        try {
                            if(Build.VERSION.SDK_INT >=28) {
                                val source = ImageDecoder.createSource(this.contentResolver,imageUri)
                                selectedBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            } else {
                                selectedBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,imageUri)
                                binding.imageView.setImageBitmap(selectedBitmap)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result -> //İzin istiyoruz
            if(result) {
                //izin verildi
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResult.launch(intentToGallery)
            } else {
                Toast.makeText(this,"Permission needed!",Toast.LENGTH_LONG).show()
            }

        }

    }

    fun saveButton(view: View) {

        val artName = binding.artName.text.toString()
        val artistName = binding.artistName.text.toString()
        val year = binding.yearText.text.toString()

        if(selectedBitmap != null) {
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)

            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                val sqlString = "INSERT INTO arts(artname, artistname, year, image) VALUES (?,?,?,?)"
                val statment = database.compileStatement(sqlString)
                statment.bindString(1,artName)
                statment.bindString(2,artistName)
                statment.bindString(3,year)
                statment.bindBlob(4,byteArray)
                statment.execute()

            }  catch (e: Exception) {
                e.printStackTrace()
            }

            val intent = Intent(this,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) //Benden önce kaç tane sayfa varsa kapat
            startActivity(intent)

        }
    }

    fun updateArt(view: View) {
        val selectedId = intent.getIntExtra("id", 0)

        val artName = binding.artName.text.toString()
        val artistName = binding.artistName.text.toString()
        val year = binding.yearText.text.toString()

        if (selectedId != 0) {
            if (selectedBitmap != null) {
                val smallBitmap = makeSmallerBitmap(selectedBitmap!!, 300)
                val outputStream = ByteArrayOutputStream()
                smallBitmap.compress(Bitmap.CompressFormat.PNG, 50, outputStream)
                val byteArray = outputStream.toByteArray()

                try {
                    val sqlString =
                        "UPDATE arts SET artname = ?, artistname = ?, year = ?, image = ? WHERE id = $selectedId"
                    val statement = database.compileStatement(sqlString)
                    statement.bindString(1, artName)
                    statement.bindString(2, artistName)
                    statement.bindString(3, year)
                    statement.bindBlob(4, byteArray)
                    statement.execute()
                    Toast.makeText(this, "Güncelleme başarılı!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("GüncellemeHata", "Güncelleme hatası: ${e.message}")
                    Toast.makeText(this, "Güncelleme başarısız oldu!", Toast.LENGTH_SHORT).show()
                }
            } else {
                try {
                    val sqlString =
                        "UPDATE arts SET artname = ?, artistname = ?, year = ? WHERE id = $selectedId"
                    val statement = database.compileStatement(sqlString)
                    statement.bindString(1, artName)
                    statement.bindString(2, artistName)
                    statement.bindString(3, year)
                    statement.execute()
                    Toast.makeText(this, "Resimsiz güncelleme başarılı!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("GüncellemeHata", "Güncelleme hatası: ${e.message}")
                    Toast.makeText(this, "Güncelleme başarısız oldu!", Toast.LENGTH_SHORT).show()
                }
            }

            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    private fun makeSmallerBitmap(image: Bitmap, maxSize: Int) : Bitmap { // Fotoğrafın boyutunu küçültüyoruz
        var width = image.width //300 olsun
        var height = image.height //200 olsun

        val bitmapRatio : Double = width.toDouble() / height.toDouble() //  3/2

        if(bitmapRatio > 1) {
            //Yatay fotoğraf
            width = maxSize //Maximum verdiğimiz değer olacak
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()

        } else {
            //Dikey fotoğraf
            height = maxSize //Maximum verdiğimiz değer olacak
            val scaledWidht = height * bitmapRatio
            width = scaledWidht.toInt()
        }

        return Bitmap.createScaledBitmap(image,width,height,true)
    }

}