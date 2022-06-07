package com.tuk.myapp

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import java.util.*
import kotlin.collections.ArrayList

class MainActivity3 : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var ttxt:TextView
    private lateinit var ttxt1:TextView

    private var dataArrayList: ArrayList<String>? = null
    private var adapter: ArrayAdapter<String>? = null
    private lateinit var prefe1: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)
        declararVariaveis()
        deleteInformation()
        addInformations()
        if(intent.hasExtra("content"))
        {
            ttxt1.text=intent.getStringExtra("content")
        }
        if(intent.hasExtra("title"))
        {
            ttxt.text=intent.getStringExtra("title")
        }
        addInformations()
    }
    private fun declararVariaveis(){

        dataArrayList = java.util.ArrayList()
        leerSharedPreferences()
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, dataArrayList!!)
        listView = findViewById<View>(R.id.listVw_actvMain) as ListView
        listView.adapter = adapter

        ttxt=findViewById(R.id.txt)
        ttxt1=findViewById(R.id.txt1)

    }


    private fun addInformations() {

        if (ttxt.text.toString() != "" && ttxt1.text.toString() != ""){

            dataArrayList!!.add(ttxt.text.toString() + "\n메뉴 : " + ttxt1.text.toString())
            adapter!!.notifyDataSetChanged()
            val elemento = prefe1.edit()
            elemento.putString(ttxt.text.toString(), ttxt1.text.toString())
            elemento.apply()
//            ttxt.setText("")
//            ttxt1.setText("")
        }
    }

    private fun deleteInformation(){

        listView.onItemLongClickListener =
            AdapterView.OnItemLongClickListener { adapterView, view, i, l ->
                val dialogo1 = AlertDialog.Builder(this@MainActivity3)
                dialogo1.setTitle("삭제 하시겠습니까?")
                dialogo1.setMessage(null)
                dialogo1.setCancelable(false)
                dialogo1.setPositiveButton(
                    "확인"
                ) { dialogo1, id ->
                    val s = dataArrayList!![i]
                    val tok1 = StringTokenizer(s, "\n")
                    val nom = tok1.nextToken().trim { it <= ' ' }
                    val elemento = prefe1!!.edit()
                    elemento.remove(nom)
                    elemento.commit()
                    dataArrayList!!.removeAt(i)
                    adapter!!.notifyDataSetChanged()
                }
                dialogo1.setNegativeButton(
                    "취소"
                ) { dialogo1, id -> }
                dialogo1.show()
                false
            }
    }

    private fun leerSharedPreferences() {

        prefe1 = getSharedPreferences("list", MODE_PRIVATE)
        val claves = prefe1.all
        for ((key, value) in claves) {
            dataArrayList!!.add(key + "\n메뉴 : " + value.toString())

        }
    }
}