package com.example.parenttry

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.http.HttpResponseCache.install
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.privacysandbox.tools.core.model.Method
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.HttpClient
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar

class MainActivity : AppCompatActivity() {



    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {


        // setting view
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // reading last words that entered in the last use of the parent application from
        // local file, if file doesn't exist (first time opening app) => create the file
        var file : File = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "words.txt")
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }

        }

        val bufferedReader: BufferedReader = file.bufferedReader()
        val inputString = bufferedReader.use { it.readText() }



        // getting button variable and textedit variable with their ids
        var badwordsbutton : Button = findViewById(R.id.update_badwords)
        var textedit : EditText = findViewById(R.id.editText)


        // setting the edittext filed with the loaded file
        textedit.text = inputString.toEditable()
        val json = JSONObject()


        // filling 'from' of json var
        json.put("from", "parent")


        // when user clicked on update words button...
        badwordsbutton.setOnClickListener {



            // first write these words that user entered in edittext to the local words.txt file
            File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "words.txt").printWriter().use { out ->
                out.println(textedit.text)
            }


            // put the words in the json var
            json.put("words", textedit.text)
            // set 'images' var to false => so server doesn't response with screenshots
            json.put("images", false)


            // send the json var to server on 127.0.0.1:8000 (using 10.0.2.2 because we are running on a emulator)
            val jsonRequest = object : JsonObjectRequest(
                Request.Method.POST, "http://10.0.2.2:8000/parent", json,

                Response.Listener { response ->
                    try {
                        // get the response from user and print it in logcat
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
            // json send
        }



        // when user clicked on the screenshot button...
        var screenshotbtn : Button = findViewById(R.id.update_screenshots)
        screenshotbtn.setOnClickListener {


            // like the last button, store the words entered in edittext to the files (we are saving and sending words again
            // but this time we will get the screenshots too
            File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "words.txt").printWriter().use { out ->
                out.println(textedit.text)
            }

            // puting words in json var
            json.put("words", textedit.text)
            // setting images to true so we get the screenshots with the response
            json.put("images", true)


            // send the json var to server on 127.0.0.1:8000 (using 10.0.2.2 because we are running on a emulator)
            val jsonRequest = object : JsonObjectRequest(
                Request.Method.POST, "http://10.0.2.2:8000/parent", json,

                Response.Listener { response ->
                    try {


                        // getting the response
                        // json format:

                        // {
                        // images: {
                        // 'imageName' : 'base64.encode(imagefile)'
                        //  ...
                        //          }
                        // }

                        // so we are going to read key and value of the images element,
                        // then create a file, setting key as the name,
                        // then decoding the string with base64 and filling the file
                        val imagesJson = response.optJSONObject("images")
                        imagesJson?.let { images ->
                            images.keys().forEach { key ->
//                                println("IMAGE " + key)
                                val value = imagesJson.get(key).toString()
                                val imageBytes = Base64.decode(value, Base64.DEFAULT)

                                // saving file in Android/data/com.example.parentTry/files/Pictures/
                                val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),key)

                                val fileOutputStream = FileOutputStream(file)
                                fileOutputStream.write(imageBytes)
                                fileOutputStream.close()
                            }}


                        // printing the response to logcat
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


        }
    }

}
fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)
