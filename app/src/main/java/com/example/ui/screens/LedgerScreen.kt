package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.LedgerEntry
import com.example.data.model.LedgerEventType
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BurnishedAmber
import com.example.ui.theme.CrimsonBreach
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonText
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.viewmodel.UiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LedgerScreen(
    state: UiState<List<LedgerEntry>>,
) {
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
                val entries = state.data
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "ledger_intro") {
                        Surface(
                            color = ObsidianSurface,
                            border = BorderStroke(1.dp, BorderHairline),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.HistoryEdu,
                                    contentDescription = null,
                                    tint = BurnishedAmber,
                                    modifier = Modifier.size(28.dp),
                                )
                                Column {
                                    Text(
                                        text = "سجل الشرف والقيود الأبدي",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "توثيق غير قابل للتعديل أو المسح لكل إنجاز مصوّر أو قيد مفروض.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    if (entries.isEmpty()) {
                        item(key = "ledger_empty") {
                            Surface(
                                color = ObsidianSurface,
                                border = BorderStroke(1.dp, BorderHairline),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp),
                                    )
                                    Text(
                                        text = "السجل لا يزال أبيضاً",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "بمجرد توقيع أول ميثاق أو تصوير مهمة سيُنقش أثرها هنا للأبد.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        items(items = entries, key = { it.id }) { entry ->
                            LedgerEntryCard(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerEntryCard(entry: LedgerEntry) {
    val eventType = entry.parsedEventType()
    val accentColor = when (eventType) {
        LedgerEventType.TASK_COMPLETED, LedgerEventType.DEBT_REPAID -> EmeraldVerified
        LedgerEventType.CONTRACT_SEALED -> BurnishedAmber
        LedgerEventType.TASK_BREACHED, LedgerEventType.FORCE_MAJEURE -> CrimsonBreach
    }
    val icon = when (eventType) {
        LedgerEventType.TASK_COMPLETED, LedgerEventType.DEBT_REPAID -> Icons.Filled.CheckCircle
        LedgerEventType.CONTRACT_SEALED -> Icons.Filled.Shield
        LedgerEventType.TASK_BREACHED, LedgerEventType.FORCE_MAJEURE -> Icons.Filled.Gavel
    }
    val dateLabel = entry.createdAt?.toDate()?.let { date ->
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(date)
    } ?: "الآن"
    val photoModel = remember(entry.proofPhotoUri) {
        val raw = entry.proofPhotoUri
        if (raw.isNullOrBlank()) null else if (raw.startsWith("/")) File(raw) else raw
    }

    Surface(
        color = ObsidianSurface,
        border = BorderStroke(1.dp, BorderHairline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = eventType.arabicLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = accentColor,
                        )
                    }
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = entry.taskTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (entry.proofNote.isNotBlank()) {
                    Text(
                        text = entry.proofNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val scorePrefix = if (entry.scoreDelta >= 0) "+" else ""
                    Surface(
                        color = if (entry.scoreDelta >= 0) EmeraldContainer else CrimsonContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "سيادة $scorePrefix${entry.scoreDelta}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (entry.scoreDelta >= 0) EmeraldVerified else CrimsonText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    if (entry.debtMinutesDelta != 0L) {
                        val debtPrefix = if (entry.debtMinutesDelta > 0) "+" else ""
                        Surface(
                            color = ObsidianElevated,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "دين زمني $debtPrefix${entry.debtMinutesDelta}د",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (entry.debtMinutesDelta > 0) CrimsonText else EmeraldVerified,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            if (photoModel != null) {
                AsyncImage(
                    model = photoModel,
                    contentDescription = stringResource(id = R.string.cd_proof_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
        }
    }
}
