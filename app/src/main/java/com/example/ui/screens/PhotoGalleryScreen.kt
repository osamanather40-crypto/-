package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.LedgerEntry
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BurnishedAmber
import com.example.ui.theme.CrimsonBreach
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonText
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.viewmodel.UiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PhotoGalleryScreen(
    state: UiState<List<LedgerEntry>>,
) {
    var inspectedEntry by remember { mutableStateOf<LedgerEntry?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (state) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BurnishedAmber)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color = CrimsonContainer,
                        border = BorderStroke(1.dp, CrimsonBreach),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = CrimsonText,
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
            }

            is UiState.Success -> {
                val photos = state.data
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "gallery_header") {
                        Surface(
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, BorderHairline),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoLibrary,
                                        contentDescription = null,
                                        tint = BurnishedAmber,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Column {
                                        Text(
                                            text = "سجل الصور الموثقة",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "كل صورة أضفتها لإثبات مهامك محفوظة هنا للرجوع إليها.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Surface(
                                    color = ObsidianElevated,
                                    border = BorderStroke(1.dp, BorderHairline),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(
                                        text = "${photos.size}",
                                        style = MonoMetricMedium,
                                        color = BurnishedAmber,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }

                    if (photos.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "gallery_empty") {
                            Surface(
                                color = ObsidianSurface,
                                border = BorderStroke(1.dp, BorderHairline),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhotoLibrary,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(40.dp),
                                    )
                                    Text(
                                        text = "لا توجد صور إثبات مضافة بعد",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "عندما تصوّر أي مهمة لتوثيق إنجازها ستظهر صورتها هنا مباشرة.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        items(items = photos, key = { it.id }) { entry ->
                            PhotoProofGridCard(
                                entry = entry,
                                onClick = { inspectedEntry = entry },
                            )
                        }
                    }
                }
            }
        }
    }

    val selected = inspectedEntry
    if (selected != null) {
        PhotoInspectDialog(
            entry = selected,
            onDismiss = { inspectedEntry = null },
        )
    }
}

@Composable
private fun PhotoProofGridCard(
    entry: LedgerEntry,
    onClick: () -> Unit,
) {
    val photoModel = remember(entry.proofPhotoUri) {
        val raw = entry.proofPhotoUri.orEmpty()
        if (raw.startsWith("/")) File(raw) else raw
    }
    val dateLabel = entry.createdAt?.toDate()?.let { date ->
        SimpleDateFormat("MM/dd HH:mm", Locale.US).format(date)
    } ?: "الآن"

    Surface(
        color = ObsidianSurface,
        border = BorderStroke(1.dp, BorderHairline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("photo_gallery_card_${entry.id}"),
    ) {
        Column {
            AsyncImage(
                model = photoModel,
                contentDescription = stringResource(id = R.string.cd_proof_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            )
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = entry.taskTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = EmeraldVerified,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoInspectDialog(
    entry: LedgerEntry,
    onDismiss: () -> Unit,
) {
    val photoModel = remember(entry.proofPhotoUri) {
        val raw = entry.proofPhotoUri.orEmpty()
        if (raw.startsWith("/")) File(raw) else raw
    }
    val dateLabel = entry.createdAt?.toDate()?.let { date ->
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(date)
    } ?: "الآن"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = ObsidianSurface,
            border = BorderStroke(1.dp, BurnishedAmber.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "برهان مصور موثق",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldVerified,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "إغلاق",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                AsyncImage(
                    model = photoModel,
                    contentDescription = stringResource(id = R.string.cd_proof_photo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )

                Text(
                    text = entry.taskTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (entry.proofNote.isNotBlank()) {
                    Text(
                        text = entry.proofNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "تاريخ التوثيق: $dateLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = BurnishedAmber,
                )
            }
        }
    }
}
