package com.rfyt.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.rfyt.app.databinding.ItemVideoBinding

class VideoAdapter(
    private val onItemClick: (YtVideoItem) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private val items = mutableListOf<YtVideoItem>()

    fun submitList(newItems: List<YtVideoItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VideoViewHolder(private val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: YtVideoItem) {
            binding.tvTitle.text = item.title
            binding.tvChannel.text = item.author
            
            val meta = listOfNotNull(item.views, item.publishedTime).joinToString(" • ")
            binding.tvMetadata.text = if (meta.isNotEmpty()) meta else "YouTube Video"

            binding.tvDuration.text = item.duration ?: "HD"

            Glide.with(binding.root.context)
                .load(item.thumbnail)
                .transform(RoundedCorners(12))
                .into(binding.ivThumbnail)

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}
