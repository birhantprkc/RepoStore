package com.samyak.repostore.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samyak.repostore.data.model.AppItem
import com.samyak.repostore.databinding.ItemAppPlaystoreBinding
import com.samyak.repostore.databinding.ItemAppTileBinding
import com.samyak.repostore.util.loadIconWithFallback
import com.samyak.repostore.util.loadRealAppName
import java.util.Locale

/**
 * Adapter for both Play Store shelf styles. [AppShelf.Style.ROWS] renders
 * compact rows carrying a category line and download size; [AppShelf.Style.TILES]
 * renders artwork tiles with just the name and rating.
 */
class PlayStoreAppAdapter(
    private val style: AppShelf.Style = AppShelf.Style.ROWS,
    private val onItemClick: (AppItem) -> Unit
) : ListAdapter<AppItem, PlayStoreAppAdapter.ShelfViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShelfViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (style) {
            AppShelf.Style.ROWS -> {
                val binding = ItemAppPlaystoreBinding.inflate(inflater, parent, false)
                // A row spans almost the full width so the next column peeks in.
                binding.root.layoutParams = RecyclerView.LayoutParams(
                    AppShelf.rowWidthPx(parent.context),
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
                RowViewHolder(binding)
            }

            AppShelf.Style.TILES -> TileViewHolder(
                ItemAppTileBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: ShelfViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    abstract inner class ShelfViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        abstract fun bind(item: AppItem)
    }

    /** Compact row: icon, name, "language • topic" line, then stars and size. */
    inner class RowViewHolder(
        private val binding: ItemAppPlaystoreBinding
    ) : ShelfViewHolder(binding.root) {

        override fun bind(item: AppItem) {
            val repo = item.repo

            binding.apply {
                tvAppName.loadRealAppName(repo)
                tvRating.text = formatNumber(repo.stars)
                ivAppIcon.loadIconWithFallback(item.iconUrls, repo.owner.avatarUrl)

                val subtitle = buildSubtitle(item)
                tvSubtitle.text = subtitle
                tvSubtitle.visibility = if (subtitle == null) View.GONE else View.VISIBLE

                val size = formatApkSize(item)
                tvSize.text = size
                tvSize.visibility = if (size == null) View.GONE else View.VISIBLE
            }
        }
    }

    /** Artwork tile: large icon, two-line name, stars. */
    inner class TileViewHolder(
        private val binding: ItemAppTileBinding
    ) : ShelfViewHolder(binding.root) {

        override fun bind(item: AppItem) {
            val repo = item.repo

            binding.apply {
                tvAppName.loadRealAppName(repo)
                tvRating.text = formatNumber(repo.stars)
                ivAppIcon.loadIconWithFallback(item.iconUrls, repo.owner.avatarUrl)
            }
        }
    }

    /**
     * The Play Store equivalent is a category line such as "Finance • Stock".
     * The closest data available here is the repository language plus its first
     * topic. Returns null when neither is known so the line can be hidden.
     */
    private fun buildSubtitle(item: AppItem): String? {
        val parts = listOfNotNull(
            item.repo.language?.takeIf { it.isNotBlank() },
            item.repo.topics?.firstOrNull()?.takeIf { it.isNotBlank() }
        )
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
    }

    /** Download size of the release's APK asset, or null when there isn't one. */
    private fun formatApkSize(item: AppItem): String? {
        val apkSize = item.latestRelease?.assets
            ?.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?.size
            ?: return null

        if (apkSize <= 0) return null

        val megabytes = apkSize / (1024.0 * 1024.0)
        return if (megabytes >= 10) {
            String.format(Locale.US, "%.0f MB", megabytes)
        } else {
            String.format(Locale.US, "%.1f MB", megabytes)
        }
    }

    private fun formatNumber(number: Int): String {
        return when {
            number >= 1_000_000 -> String.format(Locale.US, "%.1fM", number / 1_000_000.0)
            number >= 1_000 -> String.format(Locale.US, "%.1fK", number / 1_000.0)
            else -> number.toString()
        }
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(oldItem: AppItem, newItem: AppItem) =
            oldItem.repo.id == newItem.repo.id

        override fun areContentsTheSame(oldItem: AppItem, newItem: AppItem) =
            oldItem == newItem
    }
}
