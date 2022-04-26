package com.dynamsoft.cameraxvideo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class FilesAdapter(var context: Context?, var dataList: ArrayList<String>): RecyclerView.Adapter<FilesAdapter.MyViewHolder>() {
    var onItemClick: ((Int) -> Unit)? = null

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
            onItemClick?.invoke(position)
        }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var fileTextView:TextView? = null

        init {
            fileTextView = itemView.findViewById(R.id.fileTextView)
        }
    }
}