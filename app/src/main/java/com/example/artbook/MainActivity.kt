package com.example.artbook

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.artbook.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var artList : ArrayList<Art>
    private lateinit var artAdapter: ArtAdapter
    private lateinit var swipeHandler: SwipeToDeleteCallback

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        artList = ArrayList()

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        artAdapter = ArtAdapter(artList)
        binding.recyclerView.adapter = artAdapter

        try {
            val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
            val cursor = database.rawQuery("SELECT * FROM arts",null)
            val artNameIx = cursor.getColumnIndex("artname")
            val idIx = cursor.getColumnIndex("id")

            while (cursor.moveToNext()) {
                val name = cursor.getString(artNameIx)
                val id = cursor.getInt(idIx)
                val art = Art(name,id)
                artList.add(art)
            }

            artAdapter.notifyDataSetChanged() //Adaptere haber ver veriler değişti
            cursor.close()

        } catch (e:Exception) {
            e.printStackTrace()
        }

        //Kaydırarak Silme İşlemi
        swipeHandler = object : SwipeToDeleteCallback(artAdapter) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val deletedArt = artList[position]
                try {
                    val database = this@MainActivity.openOrCreateDatabase("Arts", MODE_PRIVATE, null)
                    val sqlString = "DELETE FROM arts WHERE id = ?"
                    val statement = database.compileStatement(sqlString)
                    statement.bindLong(1, deletedArt.id.toLong())
                    statement.execute()

                    artList.removeAt(position)
                    artAdapter.notifyItemRemoved(position)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //inflater
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.art_menu,menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.addArt){
            val intent = Intent(this,DetailsActivity::class.java)
            intent.putExtra("info","new")
            startActivity(intent)
        }

        return super.onOptionsItemSelected(item)
    }

}