package com.example.imageapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


class UserAdapter(
    val users: List<User>,
    val onClick: (User) -> Unit
) : RecyclerView.Adapter<UserViewHolder>() {
    override fun getItemCount(): Int = users.size
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.name.text = "${users[position].name} ${users[position].surname}"
        holder.vkId.text = users[position].id

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserViewHolder {
        val holder = UserViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.list_item,
                parent,
                false
            )
        )
        holder.root.setOnClickListener {
//            val intent = Intent(this, AsyncTaskActivity::class.java)
            onClick(users[holder.adapterPosition])
        }
        return holder
    }

}