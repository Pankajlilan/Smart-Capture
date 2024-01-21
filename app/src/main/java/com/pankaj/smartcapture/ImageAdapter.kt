package com.pankaj.smartcapture

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImageAdapter : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {
  
  private var images: List<ImageModel> = emptyList()
  private var onItemClickListener: OnItemClickListener? = null
  
  fun setImages(images: List<ImageModel>) {
    this.images = images
    notifyDataSetChanged()
  }
  
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
    val view = LayoutInflater.from(parent.context)
      .inflate(R.layout.item_image, parent, false)
    
    val layoutParams = RecyclerView.LayoutParams(
      (parent.width * 0.2).toInt(),  // Set the width dynamically (adjust as needed)
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    view.layoutParams = layoutParams
    return ImageViewHolder(view)
  }
  
  override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
    val image = images[position]
    Glide.with(holder.imageView.context)
      .load(image.contentUri)
      .centerCrop()
      .into(holder.imageView)
    holder.cardView.setOnClickListener {
      onItemClickListener?.onItemClick(images[position], position)
    }
  }
  
  override fun getItemCount(): Int = images.size
  
  class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imageView: ImageView = itemView.findViewById(R.id.imageView)
    val cardView: CardView = itemView.findViewById(R.id.cardView)
  }
  
  interface OnItemClickListener {
    fun onItemClick(image: ImageModel, position: Int)
  }
  
  fun setOnItemClickListener(listener: OnItemClickListener?) {
    onItemClickListener = listener
  }
}


