package com.dynamsoft.cameraxvideo


import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView


class FilesAdapter(var context: Context?, var dataList: MutableList<String>): RecyclerView.Adapter<FilesAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view:View = LayoutInflater.from(context).inflate(R.layout.files_list_item,parent,false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.fileTextView?.text = dataList[position]
        holder.fileTextView?.setOnClickListener {
            Toast.makeText(context, "click:$position",Toast.LENGTH_LONG).show()
        }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var fileTextView:TextView? = null

        init {
            fileTextView = itemView.findViewById(R.id.fileTextView)
        }
    }
}