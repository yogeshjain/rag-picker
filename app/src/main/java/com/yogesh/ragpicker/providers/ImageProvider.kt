package com.yogesh.ragpicker.providers

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.Environment.getExternalStorageDirectory
import android.content.res.AssetManager
import android.os.Environment
import android.util.Log
import java.io.*
import java.nio.file.Files.isDirectory
import java.nio.file.Files.exists




/**
 * Created by yogesh on 5/3/18.
 */
class ImageProvider(val type: String) {
    public fun getImageList(context: Context): MutableList<String> {
        return getAssetList(context, type)
    }

    private fun getAssetList(context: Context, path: String): MutableList<String> {
        val path = context.filesDir.path+"/$type"
        val dir = File(path)
        if(dir.exists() && dir.isDirectory) {
            return dir.list().toMutableList()
        } else {
            try {
                //empty dir, copy assets to initialize
                copyDirorfileFromAssetManager(context, "", context.filesDir.path)
                val dirNew = File(path)
                return dirNew.list().toMutableList()
            } catch (e: IOException) {
                return mutableListOf()
            }
        }
    }

    fun getBitmapFromAssets(context: Context, fileName: String): Bitmap {

        val f = File(context.filesDir.path+"/"+fileName)
        val istr = FileInputStream(f)
        val bmp = BitmapFactory.decodeStream(istr)
        istr.close()
        return bmp
    }

    @Throws(IOException::class)
    fun copyDirorfileFromAssetManager(context: Context, arg_assetDir: String, arg_destinationDir: String): String {
        val dest_dir_path = arg_destinationDir
        val dest_dir = File(dest_dir_path)

        createDir(dest_dir)

        val asset_manager = context.getAssets()
        val files = asset_manager.list(arg_assetDir)

        for (i in files.indices) {

            val abs_asset_file_path = addTrailingSlash(arg_assetDir) + files[i]
            val sub_files = asset_manager.list(abs_asset_file_path)

            if (sub_files.size == 0) {
                // It is a file
                val dest_file_path = addTrailingSlash(dest_dir_path) + files[i]
                copyAssetFile(context, abs_asset_file_path, dest_file_path)
            } else {
                // It is a sub directory
                copyDirorfileFromAssetManager(context, abs_asset_file_path, addTrailingSlash(arg_destinationDir) + files[i])
            }
        }

        return dest_dir_path
    }


    @Throws(IOException::class)
    fun copyAssetFile(context: Context, assetFilePath: String, destinationFilePath: String) {
        val `in` = context.getAssets().open(assetFilePath)
        val out = FileOutputStream(destinationFilePath)

        val buf = ByteArray(1024)
        var len: Int = `in`.read(buf)
        while (len > 0) {
            out.write(buf, 0, len)
            len = `in`.read(buf)
        }
        `in`.close()
        out.close()
    }

    fun addTrailingSlash(path: String): String {
        var path = path
        if (path.length > 0 && path[path.length - 1] != '/') {
            path += "/"
        }
        return path
    }

    fun addLeadingSlash(path: String): String {
        var path = path
        if (path[0] != '/') {
            path = "/" + path
        }
        return path
    }

    @Throws(IOException::class)
    fun createDir(dir: File) {
        if (dir.exists()) {
            if (!dir.isDirectory) {
                throw IOException("Can't create directory, a file is in the way")
            }
        } else {
            dir.mkdirs()
            if (!dir.isDirectory) {
                throw IOException("Unable to create directory")
            }
        }
    }
}