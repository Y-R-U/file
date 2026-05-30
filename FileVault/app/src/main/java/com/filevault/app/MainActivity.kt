package com.filevault.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var repo: FileRepository
    private lateinit var adapter: FileAdapter

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private lateinit var fabImport: ExtendedFloatingActionButton
    private lateinit var tvStorageInfo: TextView
    private lateinit var tvSelectedCount: TextView
    private lateinit var etSearch: TextInputEditText

    private var currentSort = SortOrder.DATE_DESC

    // File picker launcher — picks MULTIPLE files
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@registerForActivityResult
        val imported = repo.importFiles(this, uris)
        refreshFileList()
        val msg = when {
            imported.isEmpty() -> "Failed to import files"
            imported.size == 1 -> "Imported: ${imported[0].name}"
            else -> "Imported ${imported.size} files"
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // Export / Save to Downloads
    private var pendingExportFile: VaultFile? = null
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { destUri: Uri? ->
        val fileToExport = pendingExportFile ?: return@registerForActivityResult
        pendingExportFile = null
        if (destUri == null) return@registerForActivityResult

        try {
            contentResolver.openOutputStream(destUri)?.use { out ->
                fileToExport.file.inputStream().use { it.copyTo(out) }
            }
            Toast.makeText(this, "Exported: ${fileToExport.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repo = FileRepository(this)

        bindViews()
        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupFab()

        refreshFileList()
    }

    private fun bindViews() {
        toolbar        = findViewById(R.id.toolbar)
        recyclerView   = findViewById(R.id.recyclerView)
        emptyState     = findViewById(R.id.emptyState)
        fabImport      = findViewById(R.id.fabImport)
        tvStorageInfo  = findViewById(R.id.tvStorageInfo)
        tvSelectedCount = findViewById(R.id.tvSelectedCount)
        etSearch       = findViewById(R.id.etSearch)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        toolbar.setOnMenuItemClickListener { item ->
            handleMenuAction(item)
        }
    }

    private fun handleMenuAction(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort -> {
                showSortDialog()
                true
            }
            R.id.action_select_all -> {
                adapter.setSelectionMode(true)
                adapter.selectAll()
                updateSelectionUI()
                true
            }
            R.id.action_export_selected -> {
                exportSelectedFiles()
                true
            }
            R.id.action_delete_selected -> {
                deleteSelectedFiles()
                true
            }
            else -> false
        }
    }

    private fun setupRecyclerView() {
        adapter = FileAdapter(
            onFileClick = { file -> openFile(file) },
            onFileLongClick = { _ ->
                if (!adapter.isInSelectionMode()) {
                    adapter.setSelectionMode(true)
                }
                updateSelectionUI()
            },
            onMoreClick = { file, anchor -> showFileContextMenu(file, anchor) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupFab() {
        fabImport.setOnClickListener {
            importLauncher.launch(arrayOf("*/*"))
        }
    }

    private fun refreshFileList() {
        val files = repo.listFiles(currentSort)
        adapter.submitList(files)
        updateStorageInfo()
        updateEmptyState(files.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateStorageInfo() {
        val info = repo.getStorageInfo()
        tvStorageInfo.text = info.summary()
    }

    private fun updateSelectionUI() {
        val count = adapter.getSelectedCount()
        if (adapter.isInSelectionMode()) {
            tvSelectedCount.visibility = View.VISIBLE
            tvSelectedCount.text = "$count selected"
        } else {
            tvSelectedCount.visibility = View.GONE
        }
    }

    private fun exitSelectionMode() {
        adapter.setSelectionMode(false)
        tvSelectedCount.visibility = View.GONE
    }

    // ─── File Actions ───────────────────────────────────────────────────────────

    private fun openFile(file: VaultFile) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file.file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, file.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportFile(file: VaultFile) {
        pendingExportFile = file
        exportLauncher.launch(file.name)
    }

    private fun shareFile(file: VaultFile) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file.file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = file.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share ${file.name}"))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not share file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDeleteFile(file: VaultFile) {
        AlertDialog.Builder(this)
            .setTitle("Delete file?")
            .setMessage("\"${file.name}\" will be permanently deleted.")
            .setPositiveButton("Delete") { _, _ ->
                if (repo.deleteFile(file)) {
                    refreshFileList()
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportSelectedFiles() {
        val selected = adapter.getSelectedFiles()
        if (selected.isEmpty()) {
            Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show()
            return
        }
        if (selected.size == 1) {
            exportFile(selected[0])
            return
        }
        // Multiple files: share them all via ACTION_SEND_MULTIPLE
        val uris = ArrayList<Uri>(selected.map { file ->
            FileProvider.getUriForFile(this, "${packageName}.fileprovider", file.file)
        })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Export ${selected.size} files"))
    }

    private fun deleteSelectedFiles() {
        val selected = adapter.getSelectedFiles()
        if (selected.isEmpty()) {
            Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete ${selected.size} file${if (selected.size != 1) "s" else ""}?")
            .setMessage("This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val count = repo.deleteFiles(selected)
                exitSelectionMode()
                refreshFileList()
                Toast.makeText(this, "Deleted $count file${if (count != 1) "s" else ""}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─── Context Menu ────────────────────────────────────────────────────────────

    private fun showFileContextMenu(file: VaultFile, anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.file_context_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_open   -> { openFile(file); true }
                    R.id.action_export -> { exportFile(file); true }
                    R.id.action_share  -> { shareFile(file); true }
                    R.id.action_delete -> { confirmDeleteFile(file); true }
                    else -> false
                }
            }
            show()
        }
    }

    // ─── Sort ────────────────────────────────────────────────────────────────────

    private fun showSortDialog() {
        val options = arrayOf(
            "Name (A → Z)", "Name (Z → A)",
            "Date (newest first)", "Date (oldest first)",
            "Size (largest first)", "Size (smallest first)",
            "File type"
        )
        val sortOrders = arrayOf(
            SortOrder.NAME_ASC, SortOrder.NAME_DESC,
            SortOrder.DATE_DESC, SortOrder.DATE_ASC,
            SortOrder.SIZE_DESC, SortOrder.SIZE_ASC,
            SortOrder.TYPE_ASC
        )
        val currentIdx = sortOrders.indexOf(currentSort)

        AlertDialog.Builder(this)
            .setTitle("Sort by")
            .setSingleChoiceItems(options, currentIdx) { dialog, which ->
                currentSort = sortOrders[which]
                refreshFileList()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ─── Back navigation ─────────────────────────────────────────────────────────

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (adapter.isInSelectionMode()) {
            exitSelectionMode()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
