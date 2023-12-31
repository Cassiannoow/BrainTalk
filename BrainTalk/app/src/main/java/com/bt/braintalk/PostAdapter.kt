import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley.newRequestQueue
import com.bt.braintalk.OnPostItemClickListener
import com.bt.braintalk.R
import Models.Post
import Models.User
import org.json.JSONException
import java.io.ByteArrayInputStream

class PostAdapter(private var posts: List<Post>, user: User, clickListener: OnPostItemClickListener) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>(){
    private val usuario: User = user
    private val clickListener: OnPostItemClickListener = clickListener
    var color = "black"
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view: View =
            LayoutInflater.from(parent.context).inflate(R.layout.layout_post, parent, false)
        return PostViewHolder(view, clickListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.textTitle.text = "@" + post.username
        holder.textContent.text = post.content
        holder.bind(post.id)

        holder.imgLike.setOnClickListener {
            clickListener.onLikeButtonClick(post.id, holder.itemView)
        }
        adicionarImagens(post.file, holder.imgPost)
        val queue = newRequestQueue(holder.context)
        val url = "http://192.168.0.14:3000/users/" + post.username

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                adicionarImagens(response.optString("photo").toString(), holder.imgUser)
            },
            { error ->
            }
        )
        queue.add(jsonObjectRequest)
        var likes = 0

        val queue2 = newRequestQueue(holder.context)
        val url2 = "http://192.168.0.14:3000/likes/"

        val request = JsonArrayRequest(
            Request.Method.GET, url2, null,
            { response ->
                try {
                    // A resposta é uma matriz JSON (JSONArray)
                    for (i in 0 until response.length()) {
                        val jsonObject = response.getJSONObject(i)
                        val idPost = jsonObject.getString("postId")
                        val username = jsonObject.getString("username")

                        // Faça algo com os dados aqui, por exemplo, adicione-os a uma lista
                        if(idPost == post.id)
                        {
                           likes++
                            if(usuario.username == username)
                            {
                                var imgLike: ImageView
                                imgLike = holder.itemView.findViewById<ImageView>(R.id.imageFile2)

                                imgLike.setImageResource(R.drawable.heart_red)
                                color = "red"
                            }
                        }
                    }

                    holder.textLikes.text = likes.toString()

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            { error ->
                // Tratar erros na solicitação
            }
        )

        // Adicione a solicitação à fila de solicitações Volley (substitua mRequestQueue pelo seu RequestQueue existente)
        queue2.add(request)


    }

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
        Log.d("PostAdapter", "Updated")
    }
    override fun getItemCount(): Int {
        return posts.size
    }



    class PostViewHolder(itemView: View, private val clickListener: OnPostItemClickListener) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        var textTitle: TextView
        var textContent: TextView
        var textLikes: TextView
        var imgPost: ImageView
        var imgUser: ImageView
        var context: Context
        var imgLike: ImageView
        private var postId: String = ""
        init {
            textLikes= itemView.findViewById<TextView>(R.id.textLIkes)
            textTitle = itemView.findViewById<TextView>(R.id.textUsername)
            textContent = itemView.findViewById<TextView>(R.id.textContent)
            imgPost = itemView.findViewById<ImageView>(R.id.imageFile)
            imgUser = itemView.findViewById<ImageView>(R.id.imageUser)
            context = itemView.context
            imgLike = itemView.findViewById<ImageView>(R.id.imageFile2)
            itemView.setOnClickListener(this)

        }

        fun bind(postId: String) {
            this.postId = postId

            // Resto do código para vincular outros dados ao ViewHolder
        }

        override fun onClick(v: View) {
            clickListener.onPostItemClick(postId, v)
        }
    }

    fun adicionarImagens(s: String, imageView: ImageView){
        val imageData = s.split(",")[1]
        val decodedBytes = Base64.decode(imageData, Base64.DEFAULT)
        val decodedBitmap = BitmapFactory.decodeStream(ByteArrayInputStream(decodedBytes))
        imageView.setImageBitmap(decodedBitmap)
    }
}