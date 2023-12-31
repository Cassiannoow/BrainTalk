package com.bt.braintalk

import Models.Like
import PostAdapter
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import Models.Post
import Models.User
import android.graphics.Color
import android.widget.TextView
import com.android.volley.toolbox.StringRequest
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.UUID

class MainScreen : AppCompatActivity(), OnPostItemClickListener {
    private lateinit var user: User;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        user = intent.getSerializableExtra("user") as User

        val imageViewPhoto = findViewById<ImageView>(R.id.imgUser)
        adicionarImagens(user.photo, imageViewPhoto)


        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewPosts)
        val postAdapter = PostAdapter(listOf(), user, this) // Inicialize com uma lista vazia por enquanto

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = postAdapter

        // Chame a função para obter os posts da API
        getPostsFromApi(postAdapter)

    }

    fun adicionarImagens(s: String, imageView: ImageView){
        val imageData = s.split(",")[1]
        val decodedBytes = Base64.decode(imageData, Base64.DEFAULT)
        val decodedBitmap = BitmapFactory.decodeStream(ByteArrayInputStream(decodedBytes))
        imageView.setImageBitmap(decodedBitmap)
    }

    fun getPostsFromApi(postAdapter: PostAdapter) {
        val queue = Volley.newRequestQueue(this)
        val url = "http://192.168.0.14:3000/posts" // Substitua pela URL correta da sua API

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                val posts = mutableListOf<Post>()

                for (i in 0 until response.length()) {
                    val jsonObject = response.getJSONObject(i)
                    val id = jsonObject.getString("id")
                    val username = jsonObject.getString("username")
                    val content = jsonObject.getString("content")
                    val dataPostObject = jsonObject.getJSONObject("dataPost")
                    val seconds = dataPostObject.getLong("seconds")
                    val nanoseconds = dataPostObject.getLong("nanoseconds")
                    val dataPost = seconds * 1000 + nanoseconds / 1000000 // Convertendo para milissegundos

                    // Agora você pode usar o valor de dataPost (que é um Long) conforme necessário

                    val file = jsonObject.getString("file")
                    val contenttype = jsonObject.getString("contenttype")
                    // Outros campos da postagem

                    val post = Post(id,username, content, dataPost, file, contenttype)
                    posts.add(post)


                }
                Log.d("Lista Posts", posts.toString())
                postAdapter.updatePosts(posts) // Atualiza o adaptador com os novos posts
            },
            { error ->
                // Tratar erros na solicitação
            }
        )

        // Adicione a solicitação à fila de solicitações Volley (substitua mRequestQueue pelo seu RequestQueue existente)
        queue.add(request)
    }

    fun perfilPage(view: View){
        val intent = Intent(this, PerfilActivity::class.java)

        val queue = Volley.newRequestQueue(this)
        val url = "http://192.168.0.14:3000/users/" + user.username

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val correctPassword = response.optString("password")
                if (user.password.toString() == correctPassword) {
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
    private fun showToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_SHORT).show()
    }

     fun onPostItemClick(view: View) {
        this.postId = (view.getTag() as String?) ?: ""
        darLike(postId, view)
    }

    private var postId: String = " "

    override fun onPostItemClick(postId: String, view: View) {
        this.postId = postId
    }

    override fun onLikeButtonClick(postId: String, view: View) {
        val queue2 = Volley.newRequestQueue(this)
        val url2 = "http://192.168.0.14:3000/likes/"

        val request = JsonArrayRequest(
            Request.Method.GET, url2, null,
            { response ->
                try {
                    // Crie uma cópia da lista de likes
                    val likesList = mutableListOf<Like>()
                    for (i in 0 until response.length()) {
                        val jsonObject = response.getJSONObject(i)
                        val id = jsonObject.getString("id")
                        val idPost = jsonObject.getString("postId")
                        val username = jsonObject.getString("username")

                        val like = Like(id, idPost, username)
                        likesList.add(like)
                    }

                    var hasUserLiked = false

                    // Itere sobre a cópia da lista
                    for (like in likesList) {
                        if (postId == like.postID) {
                            Log.d("Comparação de Strings", "user.username: ${user.username}, username: ${like.username}")
                            if (user.username == like.username) {
                                delete(view, like.id)
                                showToast("entrei aqui no delete")
                                hasUserLiked = true
                                break
                            }
                        }
                    }

                    // Se o usuário não tiver um like para este postId, adicione um novo like
                    if (!hasUserLiked) {
                        post(view, postId)
                        showToast("entrei aqui no post")
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            { error ->
                // Tratar erros na solicitação
            }
        )

        // Adicione a solicitação à fila de solicitações Volley
        queue2.add(request)
    }


    fun darLike(postId: String, view: View) {
        var imgLike: ImageView
        imgLike = view.findViewById<ImageView>(R.id.imageFile2)
        imgLike.setImageResource(R.drawable.heart_red)
    }


    fun delete(view: View, id: String) {
        val queue2 = Volley.newRequestQueue(this)
        val url2 = "http://192.168.0.14:3000/like/$id"

        val request = object : StringRequest(
            Method.DELETE, url2,
            { response ->
                runOnUiThread {
                    // Alterações na interface do usuário
                    var imgLike: ImageView = view.findViewById(R.id.imageFile2)
                    imgLike.setImageResource(R.drawable.heart_black)

                    var textLikes: TextView
                    textLikes = view.findViewById<TextView>(R.id.textLIkes)
                    var j = (textLikes.text.toString().toInt() - 1)
                    textLikes.text = j.toString()
                }
            },
            { error ->
                // Tratar erros na solicitação
            }
        ) {
            // Você pode adicionar cabeçalhos personalizados aqui, se necessário
        }

        // Adicione a solicitação à fila de solicitações Volley
        queue2.add(request)
    }




    fun post(view: View, postId: String){
        val queue = Volley.newRequestQueue(this)
        val url = "http://192.168.0.14:3000/like"
        val userJson = JSONObject()
        userJson.put("id", UUID.randomUUID().toString())
        userJson.put("postId", postId)
        userJson.put("username", user.username)

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, userJson,
            { _ ->
                var imgLike: ImageView
                imgLike = view.findViewById<ImageView>(R.id.imageFile2)
                showToast(postId + " heart red")
                imgLike.setImageResource(R.drawable.heart_red)
                var textLikes: TextView
                textLikes = view.findViewById<TextView>(R.id.textLIkes)
                var j = (textLikes.text.toString().toInt() + 1)
                textLikes.text = j.toString()
            },
            { error ->
            }
        )
        queue.add(jsonObjectRequest)
    }


    fun criarPost(view: View)
    {
        val intent = Intent(this, create_post::class.java)
        intent.putExtra("username", user.username)
        startActivity(intent)
    }

    fun disableAllBorder(){
        var viewPost = this.findViewById<View>(R.id.bordaForYou)
        var viewMaterial = this.findViewById<View>(R.id.bordaMostSeen)
        var TextPost = this.findViewById<TextView>(R.id.txtForYou)
        var TextLiked = this.findViewById<TextView>(R.id.txtMostSeen)
        viewPost.visibility = View.INVISIBLE
        viewMaterial.visibility = View.INVISIBLE
        TextPost.setTextColor(Color.parseColor("#002C4D"))
        TextLiked.setTextColor(Color.parseColor("#002C4D"))
    }

    fun alterarColorTextForYou(view: View){
        var viewPost = this.findViewById<View>(R.id.bordaForYou)
        var TextPost = this.findViewById<TextView>(R.id.txtForYou)
        disableAllBorder()
        viewPost.visibility = View.VISIBLE
        TextPost.setTextColor(Color.parseColor("#000000"))
    }

    fun alterarColorTextMostSeen(view: View){
        var viewPost = this.findViewById<View>(R.id.bordaMostSeen)
        var TextLiked = this.findViewById<TextView>(R.id.txtMostSeen)
        disableAllBorder()
        viewPost.visibility = View.VISIBLE
        TextLiked.setTextColor(Color.parseColor("#000000"))
    }

}