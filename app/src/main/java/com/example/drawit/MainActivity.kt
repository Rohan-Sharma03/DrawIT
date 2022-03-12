package com.example.drawit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View

import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dailog_bursh_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint:ImageButton?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view.setSizeForBrush(10.toFloat())

        mImageButtonCurrentPaint = ll_paint_colors[1] as ImageButton

        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.pallet_pressed
            )
        )

        ib_brush.setOnClickListener {
            showBrushSizechooserDialog()
        }

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if (result.resultCode==Activity.RESULT_OK){
                if(result.resultCode== GALLERY){
                    try {
                        if(result.data!!.data!=null){
                            back_image.visibility=View.VISIBLE
                            back_image.setImageURI(result.data!!.data)
                        }else{
                            Toast.makeText(
                                this@MainActivity,
                                "Error in parsing the image or its corrupted.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }catch (e :Exception){
                        e.printStackTrace()
                    }
                }

            }
        }


        ib_gallery.setOnClickListener {
            if(isRequestAlredyAllowed()){
                Toast.makeText(this,"happy",Toast.LENGTH_SHORT).show()
                val pickPhoto = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                resultLauncher.launch(pickPhoto)

            }else{
                requestStroagePermission()
            }
        }


        ib_undo.setOnClickListener {
            drawing_view.undopaths()
        }


        ib_save.setOnClickListener {
            if(isRequestAlredyAllowed()){
                BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
            }else
                requestStroagePermission()
        }



    }
    private fun showBrushSizechooserDialog(){
        val brushDialog =Dialog(this)
        brushDialog.setContentView(R.layout.dailog_bursh_size)
        brushDialog.setTitle("Brush Size")
        brushDialog.show()
        val smallbtn = brushDialog.findViewById<ImageView>(R.id.ib_small_brush)
        smallbtn.setOnClickListener {
            drawing_view.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumbtn = brushDialog.ib_medium_brush
        mediumbtn.setOnClickListener {
            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largebtn = brushDialog.findViewById<ImageView>(R.id.ib_large_brush)
        largebtn.setOnClickListener {
            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }


    }

    fun paintClicked(view :View){
        if(view!=mImageButtonCurrentPaint){
            val imagebutton = view as ImageButton
            val colortag = imagebutton.tag.toString()
            drawing_view.setcolor(colortag)
            imagebutton.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_pressed))
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal))

            mImageButtonCurrentPaint=view
        }

    }

    private fun requestStroagePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this,"Need permission to add a Background",Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    private fun isRequestAlredyAllowed():Boolean{
        var flag =ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        return flag == PackageManager.PERMISSION_GRANTED
    }


    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode== STORAGE_PERMISSION_CODE){
            if(permissions.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permission Garnted",Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this,"you denied it",Toast.LENGTH_SHORT).show()
            }
        }
    }
    companion object{
        private const val STORAGE_PERMISSION_CODE=1
        private const val GALLERY=2
    }


    /**
     * Create bitmap from view and returns it
     */


    private fun getBitmapFromView(view: View): Bitmap {

        //Define a bitmap with the same size as the view.
        // CreateBitmap : Returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        //Get the view's background
        val bgDrawable = view.background
        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }



    /**
     * “A nested class marked as inner can access the members of its outer class.
     * Inner classes carry a reference to an object of an outer class:”
     * source: https://kotlinlang.org/docs/reference/nested-classes.html
     *
     * This is the background class is used to save the edited image of user in form of bitmap to the local storage.
     *
     * For Background we have used the AsyncTask
     *
     * Asynctask : Creates a new asynchronous task. This constructor must be invoked on the UI thread.
     */

    @SuppressLint("StaticFieldLeak")
    private inner class BitmapAsyncTask(val mBitmap: Bitmap?) :
        AsyncTask<Any, Void, String>() {

        /**
         * ProgressDialog is a modal dialog, which prevents the user from interacting with the app.
         *
         * The progress dialog in newer versions is deprecated so we will create a custom progress dialog later on.
         * This is just an idea to use progress dialog.
         */

        @Suppress("DEPRECATION")
        private var mDialog: ProgressDialog? = null

        override fun onPreExecute() {
            super.onPreExecute()

//            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any): String {

            var result = ""

            if (mBitmap != null) {

                try {
                    val bytes = ByteArrayOutputStream() // Creates a new byte array output stream.
                    // The buffer capacity is initially 32 bytes, though its size increases if necessary.

                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    /**
                     * Write a compressed version of the bitmap to the specified outputstream.
                     * If this returns true, the bitmap can be reconstructed by passing a
                     * corresponding inputstream to BitmapFactory.decodeStream(). Note: not
                     * all Formats support all bitmap configs directly, so it is possible that
                     * the returned bitmap from BitmapFactory could be in a different bitdepth,
                     * and/or may have lost per-pixel alpha (e.g. JPEG only supports opaque
                     * pixels).
                     *
                     * @param format   The format of the compressed image
                     * @param quality  Hint to the compressor, 0-100. 0 meaning compress for
                     *                 small size, 100 meaning compress for max quality. Some
                     *                 formats, like PNG which is lossless, will ignore the
                     *                 quality setting
                     * @param stream   The outputstream to write the compressed data.
                     * @return true if successfully compressed to the specified stream.
                     */

                    val f = File(
                        externalCacheDir!!.absoluteFile.toString()
                                + File.separator + "KidDrawingApp_" + System.currentTimeMillis() / 1000 + ".jpg"
                    )
                    // Here the Environment : Provides access to environment variables.
                    // getExternalStorageDirectory : returns the primary shared/external storage directory.
                    // absoluteFile : Returns the absolute form of this abstract pathname.
                    // File.separator : The system-dependent default name-separator character. This string contains a single character.

                    val fo = FileOutputStream(f)
                        // Creates a file output stream to write to the file represented by the specified object.
                        fo.write(bytes.toByteArray())
                        // Writes bytes from the specified byte array to this file output stream.
                        fo.close()
                        // Closes this file output stream and releases any system resources associated with this stream.
                        // This file output stream may no longer be used for writing bytes.

                    result = f.absolutePath // The file absolute path is return as a result.

                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)

//            cancelProgressDialog()

            if (!result.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "File saved successfully :$result",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Something went wrong while saving the file.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            /*MediaScannerConnection provides a way for applications to pass a
           newly created or downloaded media file to the media scanner service.
           The media scanner service will read metadata from the file and add
           the file to the media content provider.
           The MediaScannerConnectionClient provides an interface for the
           media scanner service to return the Uri for a newly scanned file
           to the client of the MediaScannerConnection class.*/

            /*scanFile is used to scan the file when the connection is established with MediaScanner.*/

            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null){ path, uri ->
                // This is used for sharing the image after it has being stored in the storage.
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra( Intent.EXTRA_STREAM,uri)
                // A content: URI holding a stream of data associated with the Intent, used to supply the data being sent.

                shareIntent.type = "image/jpeg"
                // The MIME type of the data being handled by this intent.

                startActivity(Intent.createChooser( shareIntent,"Share"))

                // Activity Action: Display an activity chooser,
                // allowing the user to pick what they want to before proceeding.
                // This can be used as an alternative to the standard activity picker
                // that is displayed by the system when you try to start an activity with multiple possible matches,
                // with these differences in behavior:
            }
            // END
        }

        }

}