package com.example.imageapp

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_item.view.*

class UserViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
    val name = root.name
    val vkId = root.vk_id
}