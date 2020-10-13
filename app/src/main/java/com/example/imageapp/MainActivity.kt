package com.example.imageapp

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.URL


class MainActivity : AppCompatActivity() {

    companion object {
        private const val usersNumber = 10
        private const val token =
                "cd5d2043cd5d2043cd5d20437fcd294285ccd5dcd5d204392d89afc0ca1f71780804998"
        private const val version = "5.92"
        private const val user = ("https://api.vk.com/method/friends.get?user_id=" + "115252151"
                + "&fields=city"
//                + "&order=random"
                + "&count=$usersNumber"
                + "&lang=en&access_token=" + token + "&v=" + version)
        private const val usersPulledTag = "USERS_PULLED"
        private const val usersListTag = "USERS_LIST"
        private const val userTag = "USER"
    }

    private var pulledUsers = false
    private lateinit var usersList: ArrayList<User>
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        Log.d("MainActivity", "OnCreate")
        layContacts(savedInstanceState)

    }


    override fun onRestart() {
        super.onRestart()
        if (!pulledUsers) {
//            Log.d("MainActivity", "Contacts laid")
            layContacts(null)
        }
//        Log.d("MainActivity", "OnRestart: pulledUsers: $pulledUsers")
    }

    @Suppress("DEPRECATION")
    private fun haveNetwork(): Boolean {
        val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }


    override fun onSaveInstanceState(outState: Bundle) {
        if (/*haveNetwork() && */::usersList.isInitialized) {
            outState.putBoolean(usersPulledTag, pulledUsers)
            outState.putParcelableArrayList(usersListTag, usersList)
        }
        super.onSaveInstanceState(outState)
    }

    private class LinksLoader(activity: MainActivity) :
            AsyncTask<Void, Void, ArrayList<User>>() {
        private val activityRef = WeakReference(activity)
        override fun doInBackground(vararg params: Void?): ArrayList<User> {
            Log.d("LinksLoader", "doInBackground")
            val parser = JSONParser()
            val result = StringBuilder()
            val list = ArrayList<User>()
            val url = URL(user)
            val conn = url.openConnection()
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
            val getHTML = result.toString()
            val obj = parser.parse(getHTML)
            val jsonObject = obj as JSONObject
            val info = jsonObject["response"] as JSONObject?
            val array = info!!["items"] as JSONArray?
            for (i in 0 until usersNumber) {
                val friend = array!![i] as JSONObject
                list.add(
                        User(
                                friend["first_name"].toString(),
                                friend["last_name"].toString(),
                                friend["id"].toString()
                        )
                )
            }
            return list

        }


        override fun onPostExecute(result: ArrayList<User>) {
            val activity = activityRef.get()
            Log.d("LinksLoader", "onPostExecute")
            activity?.setAdapter(result)
            activity?.setRecycler()
        }
    }


    fun setAdapter(list: ArrayList<User>) {
        viewAdapter = UserAdapter(list) {
            val intent = Intent(this, AsyncTaskActivity::class.java)
            val b = Bundle()
            b.putParcelable(userTag, it)
            intent.putExtras(b)
            startActivity(intent)
        }
        usersList = list
    }

    fun setRecycler() {
        recyclerView = findViewById<RecyclerView>(R.id.my_recycler_view).apply {
            layoutManager = viewManager
            adapter = viewAdapter
        }
    }


    val linksList = (0..30).map {
        User("First name #$it", "Last name #$it", "$it")
    }


    fun layContacts(savedInstanceState: Bundle?) {
        if (!haveNetwork() && savedInstanceState == null) {
            Toast.makeText(
                    this,
                    getString(R.string.checkInternetConnectionAndTry),
                    Toast.LENGTH_LONG
            )
                    .show()
        } else {

            usersList = ArrayList()
            viewManager = LinearLayoutManager(this)
            if (savedInstanceState != null) {
                pulledUsers = savedInstanceState.getBoolean(usersPulledTag)
                usersList = savedInstanceState.getParcelableArrayList<User>(usersListTag) ?: ArrayList()
            }
            if (pulledUsers) {
                setAdapter(usersList)
                setRecycler()
            } else {
                LinksLoader(this).execute()
            }
            pulledUsers = true
        }
    }

}