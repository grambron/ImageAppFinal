package com.example.imageapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.LruCache
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL


class AsyncTaskActivity : AppCompatActivity() {
    companion object {
        private const val token =
            "cd5d2043cd5d2043cd5d20437fcd294285ccd5dcd5d204392d89afc0ca1f71780804998"
        private const val version = "5.92"
        private const val req1 = "https://api.vk.com/method/photos.get?owner_id="
        private const val req2 = ("&rev=1"
                + "&album_id=profile"
                + "&count=1"
                + "&lang=en&access_token=" + token + "&v=" + version)
        private const val retainFragmentTag = "RetainFragment"
        private const val userTag = "USER"
    }


    private lateinit var downloadedImage: ImageView
    private lateinit var userName: TextView
    private lateinit var memoryCache: LruCache<String, Bitmap>

    private var asyncTask: ImageLoaderWeak? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_async_task)
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8


        val retainFragment = RetainFragment.findOrCreateRetainFragment(supportFragmentManager)
        memoryCache = retainFragment.retainedCache ?: run {
            LruCache<String, Bitmap>(cacheSize).also { memoryCache ->
                retainFragment.retainedCache = memoryCache
            }
        }

        downloadedImage = findViewById(R.id.image)
        userName = findViewById(R.id.label)
        val user = intent.extras?.getParcelable<User>(userTag)
        ImageLoaderWeak(this).execute(user)
    }

    internal fun onLoadCompleted(result: Bitmap?, str: String) {
        downloadedImage.setImageBitmap(result)
        userName.text = str
        asyncTask = null
    }


    private class ImageLoaderWeak(activity: AsyncTaskActivity) : AsyncTask<User, Void, Bitmap>() {
        private lateinit var user: User
        private val activityRef = WeakReference(activity)
        override fun doInBackground(vararg params: User): Bitmap? {
            user = params[0]

            val bitmap = activityRef.get()?.getBitmapFromMemCache(user.id)
            if (bitmap != null) {
                return bitmap
            }
            val url = getUrl(user.id)

            if (url != null) {
                return getBitmapFromURL(url)
            } else {
                return null
            }
        }

        fun getUrl(id: String): String? {
            val parser = JSONParser()
            val result = StringBuilder()
            val url = URL(req1 + id + req2)
            val conn = url.openConnection()
            try {
                val input = BufferedReader(InputStreamReader(conn.getInputStream()))
                var next: String?
                while (true) {
                    next = input.readLine()
                    if (next == null) {
                        break
                    } else {
                        result.append(next)
                    }
                }
            } catch (e: Exception) {
                user = User(activityRef.get()!!.getString(R.string.checkInternetConnection))
                return null
            }
            try {
                val getHTML = result.toString()
                val obj = parser.parse(getHTML)
                val jsonObject = obj as JSONObject
                val info = jsonObject["response"] as JSONObject
                val array = info["items"] as JSONArray
                val photo = array[0] as JSONObject
                val sizes = photo["sizes"] as JSONArray
                for (i in 0 until sizes.size) {
                    val size = sizes[i] as JSONObject
                    if (size["type"]!!.equals("x")) {
                        return size["url"] as String
                    }
                }
                val size = sizes[0] as JSONObject
                return size["url"] as String
            } catch (e: Exception) {
                user = User(activityRef.get()!!.getString(R.string.unableToGetPicture))
                return activityRef.get()!!.getString(R.string.catURL)
            }
        }


        override fun onPostExecute(result: Bitmap?) {
            val activity = activityRef.get()
            activity?.onLoadCompleted(result, user.name)
        }

        fun getBitmapFromURL(src: String): Bitmap {
            val url = URL(src)
            val connection = url
                .openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            activityRef.get()?.addBitmapToMemoryCache(user.id, bitmap)
            return bitmap
        }


    }

    fun addBitmapToMemoryCache(key: String, bitmap: Bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap)
        }
    }

    fun getBitmapFromMemCache(key: String): Bitmap? {
        return memoryCache.get(key)
    }

    class RetainFragment : Fragment() {
        var retainedCache: LruCache<String, Bitmap>? = null

        companion object {
            fun findOrCreateRetainFragment(fm: FragmentManager): RetainFragment {
                return (fm.findFragmentByTag(retainFragmentTag) as? RetainFragment) ?: run {
                    RetainFragment().also {
                        fm.beginTransaction().add(it, retainFragmentTag).commit()
                    }
                }
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            retainInstance = true
        }
    }


}