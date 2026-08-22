package com.example.callrecorder

import android.content.Context
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class RecordingsAdapter(
    private val context: Context,
    private var items: MutableList<Recording>,
    private val onDelete: (Recording) -> Unit,
    private val onShare: (Recording) -> Unit
) : RecyclerView.Adapter<RecordingsAdapter.VH>() {

    private var player: MediaPlayer? = null
    private var playingPosition: Int = -1

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val btnPlay: android.widget.ImageButton = view.findViewById(R.id.btnPlay)
        val btnMore: android.widget.ImageButton = view.findViewById(R.id.btnMore)
        val tvName: android.widget.TextView = view.findViewById(R.id.tvName)
        val tvMeta: android.widget.TextView = view.findViewById(R.id.tvMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvName.text = item.displayName

        val dateStr = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
            .format(item.dateAddedMillis)
        val sizeStr = formatSize(item.sizeBytes)
        holder.tvMeta.text = "$dateStr  \u2022  $sizeStr"

        val isPlaying = position == playingPosition
        holder.btnPlay.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        )

        holder.btnPlay.setOnClickListener {
            if (isPlaying) stopPlayback() else playItem(position)
        }

        holder.btnMore.setOnClickListener { anchor ->
            val menu = PopupMenu(context, anchor)
            menu.menu.add(context.getString(R.string.share))
            menu.menu.add(context.getString(R.string.delete))
            menu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    context.getString(R.string.share) -> onShare(item)
                    context.getString(R.string.delete) -> {
                        if (playingPosition == position) stopPlayback()
                        onDelete(item)
                    }
                }
                true
            }
            menu.show()
        }
    }

    private fun playItem(position: Int) {
        stopPlayback()
        val item = items[position]
        try {
            player = MediaPlayer().apply {
                setDataSource(context, item.uri)
                setOnCompletionListener { stopPlayback() }
                prepare()
                start()
            }
            playingPosition = position
            notifyItemChanged(position)
        } catch (e: Exception) {
            player = null
            playingPosition = -1
        }
    }

    private fun stopPlayback() {
        val prev = playingPosition
        player?.apply {
            try {
                if (isPlaying) stop()
            } catch (_: Exception) {
            }
            release()
        }
        player = null
        playingPosition = -1
        if (prev >= 0 && prev < items.size) notifyItemChanged(prev)
    }

    fun releasePlayer() = stopPlayback()

    fun updateData(newItems: List<Recording>) {
        stopPlayback()
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        return if (kb < 1024) "%.0f KB".format(kb) else "%.1f MB".format(kb / 1024.0)
    }
}
