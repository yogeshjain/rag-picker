package com.yogesh.ragpicker.view

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.ContextCompat
import android.view.View
import com.yogesh.ragpicker.R
import com.yogesh.ragpicker.providers.ImageProvider
import com.yogesh.ragpicker.utils.Constants
import com.yogesh.ragpicker.utils.ImageUtils
import kotlinx.android.synthetic.main.content_main.*
import java.io.*


class MainActivity : BaseActivity(R.layout.activity_main), View.OnClickListener {

    val shirtImgProvider=  ImageProvider("shirt")
    val pantImgProvider=  ImageProvider("pant")

    var posTop = 0
    var posBot = 0

    lateinit var shirtImageArray: MutableList<String>
    lateinit var pantImageArray: MutableList<String>

    var userChoosenTask: String? = null
    var typeBeingPicked = "shirt"
    val REQUEST_CAMERA = 122
    val SELECT_FILE = 133

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ImageUtils.checkPermission(this)) {
            shirtImageArray = shirtImgProvider.getImageList(this)
            pantImageArray = pantImgProvider.getImageList(this)

            shuffle()
        }

        btn_top_next.setOnClickListener(this)
        btn_top_prev.setOnClickListener(this)
        btn_bot_next.setOnClickListener(this)
        btn_bot_prev.setOnClickListener(this)

        fab_shuffle.setOnClickListener(this)
        fab_fav.setOnClickListener(this)
        fab_add_top.setOnClickListener(this)
        fab_add_bot.setOnClickListener(this)

    }

    private fun shuffle() {
        posTop = Math.floor(Math.random()*shirtImageArray.size).toInt()
        posBot = Math.floor(Math.random()*pantImageArray.size).toInt()

        img_temp_top.setImageBitmap(shirtImgProvider.getBitmapFromAssets(this, "shirt/"+shirtImageArray[posTop]))
        img_temp_bot.setImageBitmap(pantImgProvider.getBitmapFromAssets(this, "pant/"+pantImageArray[posBot]))
        processFav()
    }


    override fun onClick(p0: View?) {
        when(p0?.id) {
            R.id.btn_top_next -> {
                posTop = (posTop+1)%shirtImageArray.size
                img_temp_top.setImageBitmap(shirtImgProvider.getBitmapFromAssets(this, "shirt/"+shirtImageArray[posTop]))
                processFav()
            }
            R.id.btn_top_prev -> {
                posTop = (posTop-1)
                if (posTop < 0) {
                    posTop = shirtImageArray.size - 1
                }
                img_temp_top.setImageBitmap(shirtImgProvider.getBitmapFromAssets(this, "shirt/"+shirtImageArray[posTop]))
                processFav()
            }
            R.id.btn_bot_next -> {
                posBot = (posBot+1)%pantImageArray.size
                img_temp_bot.setImageBitmap(pantImgProvider.getBitmapFromAssets(this, "pant/"+pantImageArray[posBot]))
                processFav()
            }
            R.id.btn_bot_prev -> {
                posBot = posBot-1
                if (posBot < 0) {
                    posBot = pantImageArray.size - 1
                }
                img_temp_bot.setImageBitmap(pantImgProvider.getBitmapFromAssets(this, "pant/"+pantImageArray[posBot]))
                processFav()
            }
            R.id.fab_shuffle -> {
                shuffle()
            }
            R.id.fab_fav -> {
                toggleFav()
            }
            R.id.fab_add_top -> {
                typeBeingPicked = "shirt"
                selectImage()
            }
            R.id.fab_add_bot -> {
                typeBeingPicked = "pant"
                selectImage()
            }
        }
    }

    private fun toggleFav() {
        val combo = shirtImageArray[posTop] +"###"+ pantImageArray[posBot]
        val sprefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        val toggle = !sprefs.getStringSet(Constants.PREF_FAV, emptySet()).contains(combo)
        val editor = sprefs.edit()
        val set = sprefs.getStringSet(Constants.PREF_FAV, emptySet()).toMutableSet()
        if(toggle) {
            set.add(combo)
            fab_fav.setColorFilter(ContextCompat.getColor(this, R.color.red), PorterDuff.Mode.MULTIPLY);
        } else {
            set.remove(combo)
            fab_fav.setColorFilter(ContextCompat.getColor(this, R.color.grey), PorterDuff.Mode.MULTIPLY)
        }
        editor.putStringSet(Constants.PREF_FAV, set)
        editor.apply()
    }

    fun processFav() {
        val combo = shirtImageArray[posTop] +"###"+ pantImageArray[posBot]
        val sprefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)
        val isSet = sprefs.getStringSet(Constants.PREF_FAV, emptySet()).contains(combo)
        if(isSet) {
            fab_fav.setColorFilter(ContextCompat.getColor(this, R.color.red), PorterDuff.Mode.MULTIPLY);
        } else {
            fab_fav.setColorFilter(ContextCompat.getColor(this, R.color.grey), PorterDuff.Mode.MULTIPLY)
        }
    }


    private fun selectImage() {
        val items = arrayOf<CharSequence>("Use camera", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Add Photo!")
        builder.setItems(items, DialogInterface.OnClickListener { dialog, item ->
            val result = ImageUtils.checkPermission(this@MainActivity)
            if (items[item] == "Use camera") {
                userChoosenTask = "Use camera"
                if (result)
                    cameraIntent()
            } else if (items[item] == "Choose from Gallery") {
                userChoosenTask = "Choose from Gallery"
                if (result)
                    galleryIntent()
            } else if (items[item] == "Cancel") {
                dialog.dismiss()
            }
        })
        builder.show()
    }

    private fun cameraIntent() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_CAMERA)
    }

    private fun galleryIntent() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            ImageUtils.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (userChoosenTask.equals("Use camera"))
                    cameraIntent()
                else if (userChoosenTask.equals("Choose from Gallery"))
                    galleryIntent()
                else {
                    shirtImageArray = shirtImgProvider.getImageList(this)
                    pantImageArray = pantImgProvider.getImageList(this)

                    shuffle()
                }
            } else {
                //code for deny
            }
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE)
                onSelectFromGalleryResult(data)
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data)
        }
    }

    private fun onSelectFromGalleryResult(data: Intent?) {
        var bm: Bitmap? = null
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, data.data)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        val path = ImageUtils.saveBitmapToAssets(this, bm, typeBeingPicked)
        addToListAndLoad(path, typeBeingPicked)
    }

    private fun onCaptureImageResult(data: Intent) {
        val bm = data.extras!!.get("data") as Bitmap
        val path = ImageUtils.saveBitmapToAssets(this, bm, typeBeingPicked)
        addToListAndLoad(path, typeBeingPicked)
    }

    private fun addToListAndLoad(path: String, typeBeingPicked: String) {
        when(typeBeingPicked) {
            "shirt" -> {
                shirtImageArray.add(path)
                posTop = shirtImageArray.size-1
                img_temp_top.setImageBitmap(shirtImgProvider.getBitmapFromAssets(this, "shirt/"+shirtImageArray[posTop]))
            }
            "pant" -> {
                pantImageArray.add(path)
                posBot = pantImageArray.size-1
                img_temp_bot.setImageBitmap(pantImgProvider.getBitmapFromAssets(this, "pant/"+pantImageArray[posBot]))
            }
        }
        processFav()
    }
}
