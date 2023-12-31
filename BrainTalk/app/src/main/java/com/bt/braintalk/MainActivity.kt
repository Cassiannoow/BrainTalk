package com.bt.braintalk

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import Models.User

class MainActivity : AppCompatActivity() {
    private lateinit var txtTeste: TextView;
    private lateinit var TextEmail: EditText;
    private lateinit var TextSenha: EditText;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        getSupportActionBar()?.hide()
        setContentView(R.layout.activity_main)
        TextEmail = findViewById<EditText>(R.id.txtUsernameLogin)
        TextSenha = findViewById<EditText>(R.id.txtPassword)
        txtTeste = findViewById<EditText>(R.id.txtEsqueceuSenha)
    }



    private fun showToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_SHORT).show()
    } //optimize

    fun registerPage(view: View) {
        val intent = Intent(this, ResgisterActivity::class.java)
        startActivity(intent)
    }


    fun menuPage(view: View){
        val intent = Intent(this, MainScreen::class.java)

        val queue = Volley.newRequestQueue(this)
        val url = "http://192.168.0.14:3000/users/" + TextEmail.text

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val correctPassword = response.optString("password")
                if (TextSenha.text.toString() == correctPassword) {
                    val user = User(
                        name = response.optString("name"),
                        email = response.optString("email"),
                        password = response.optString("password"),
                        photo = response.optString("photo"),
                        biograpy = response.optString("biograpy"),
                        username = response.optString("username"),
                        bannerPhoto = response.optString("bannerPhoto")
                    )
                    intent.putExtra("user", user)
                    startActivity(intent)
                } else {
                    showToast("Wrong password")
                }
            },
            { error ->
                showToast("Invalid username $error")
            }
        )
        queue.add(jsonObjectRequest)
    }
}