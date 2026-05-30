package com.filevault.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class FileAdapter(
    private val onFileClick: (VaultFile) -> Unit,
    private val onFileLongClick: (VaultFile) -> Unit,
    private val onMoreClick: (VaultFile, View) -> Unit
) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    private val allFiles = mutableListOf<VaultFile>()
    private val displayedFiles = mutableListOf<VaultFile>()
    private var isSelectionMode = false
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    inner class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardView)
        val ivFileIcon: ImageView = itemView.findViewById(R.id.ivFileIcon)
        val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        val tvFileSize: TextView = itemView.findViewById(R.id.tvFileSize)
        val tvFileDate: TextView = itemView.findViewById(R.id.tvFileDate)
        val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val vaultFile = displayedFiles[position]
        val ctx = holder.itemView.context

        holder.tvFileName.text = vaultFile.name
        holder.tvFileSize.text = formatBytes(vaultFile.size)

        // Smart date: show time if today, else show date
        val now = Calendar.getInstance()
        val fileCal = Calendar.getInstance().apply { time = vaultFile.lastModified }
        holder.tvFileDate.text = if (
            now.get(Calendar.YEAR) == fileCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == fileCal.get(Calendar.DAY_OF_YEAR)
        ) {
            "Today ${timeFormat.format(vaultFile.lastModified)}"
        } else {
            dateFormat.format(vaultFile.lastModified)
        }

        // Set icon based on file type
        val (iconRes, tintColor) = getIconForFile(vaultFile)
        holder.ivFileIcon.setImageResource(iconRes)
        holder.ivFileIcon.setColorFilter(ContextCompat.getColor(ctx, tintColor))

        // Selection mode UI
        if (isSelectionMode) {
            holder.checkbox.visibility = View.VISIBLE
            holder.checkbox.isChecked = vaultFile.isSelected
            holder.btnMore.visibility = View.GONE
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(ctx,
                    if (vaultFile.isSelected) R.color.primary_light else R.color.surface)
            )
        } else {
            holder.checkbox.visibility = View.GONE
            holder.btnMore.visibility = View.VISIBLE
            holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(ctx, R.color.surface)
            )
        }

        holder.cardView.setOnClickListener {
            if (isSelectionMode) {
                vaultFile.isSelected = !vaultFile.isSelected
                notifyItemChanged(position)
                onFileLongClick(vaultFile)
            } else {
                onFileClick(vaultFile)
            }
        }

        holder.cardView.setOnLongClickListener {
            onFileLongClick(vaultFile)
            true
        }

        holder.btnMore.setOnClickListener {
            onMoreClick(vaultFile, holder.btnMore)
        }
    }

    override fun getItemCount(): Int = displayedFiles.size

    fun submitList(files: List<VaultFile>) {
        allFiles.clear()
        allFiles.addAll(files)
        displayedFiles.clear()
        displayedFiles.addAll(files)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        displayedFiles.clear()
        if (query.isEmpty()) {
            displayedFiles.addAll(allFiles)
        } else {
            displayedFiles.addAll(
                allFiles.filter { it.name.contains(query, ignoreCase = true) }
            )
        }
        notifyDataSetChanged()
    }

    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        if (!enabled) {
            displayedFiles.forEach { it.isSelected = false }
        }
        notifyDataSetChanged()
    }

    fun selectAll() {
        displayedFiles.forEach { it.isSelected = true }
        notifyDataSetChanged()
    }

    fun getSelectedFiles(): List<VaultFile> = displayedFiles.filter { it.isSelected }

    fun getSelectedCount(): Int = displayedFiles.count { it.isSelected }

    fun isInSelectionMode(): Boolean = isSelectionMode

    private fun getIconForFile(file: VaultFile): Pair<Int, Int> {
        return when (MimeTypeHelper.getCategory(file.extension)) {
            FileCategory.IMAGE    -> Pair(R.drawable.ic_file_image, R.color.file_image)
            FileCategory.VIDEO    -> Pair(R.drawable.ic_file_video, R.color.file_video)
            FileCategory.AUDIO    -> Pair(R.drawable.ic_file_audio, R.color.file_audio)
            FileCategory.PDF      -> Pair(R.drawable.ic_file_pdf,   R.color.file_pdf)
            FileCategory.DOCUMENT -> Pair(R.drawable.ic_file_doc,   R.color.file_doc)
            FileCategory.ARCHIVE  -> Pair(R.drawable.ic_file_zip,   R.color.file_zip)
            FileCategory.GENERIC  -> Pair(R.drawable.ic_file_generic, R.color.file_generic)
        }
    }
}
