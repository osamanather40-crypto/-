package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.media.ProofPhotoStorage
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BurnishedAmber
import com.example.ui.theme.CrimsonBreach
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonText
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianVoid
import java.io.File

@Composable
fun PhysicalProofGateConsole(
    isOverdue: Boolean,
    isBusy: Boolean,
    onPhotoProofVerified: (photoPath: String, summary: String) -> Unit,
) {
    val context = LocalContext.current
    var savedPhotoPath by remember { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var caption by remember { mutableStateOf("") }
    var photoError by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            val file = pendingCameraFile
            if (success && file != null && file.exists() && file.length() > 0L) {
                savedPhotoPath = file.absolutePath
                photoError = null
            } else {
                photoError = "لم يتم التقاط الصورة. صوّر المهمة لتتمكن من إنهائها."
            }
        },
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                val persisted = ProofPhotoStorage.persistPickedPhoto(context, uri)
                if (persisted != null) {
                    savedPhotoPath = persisted
                    photoError = null
                } else {
                    photoError = "تعذّر حفظ الصورة المختارة. جرّب مرة أخرى."
                }
            }
        },
    )

    Surface(
        color = ObsidianElevated,
        border = BorderStroke(1.dp, if (isOverdue) CrimsonBreach else BorderHairline),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = null,
                    tint = BurnishedAmber,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "بوابة الإثبات الوحيدة: تصوير المهمة",
                    style = MaterialTheme.typography.titleMedium,
                    color = BurnishedAmber,
                )
            }

            Text(
                text = "لا تُغلق هذه المهمة إلا بتقديم صورة حقيقية لإنجازها. التأخر أو الامتناع عن التصوير يفرض عليك قيوداً فورية.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (photoError != null) {
                Surface(
                    color = CrimsonContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = CrimsonBreach)
                        Text(
                            text = photoError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CrimsonText,
                        )
                    }
                }
            }

            val currentPath = savedPhotoPath
            if (currentPath != null) {
                AsyncImage(
                    model = File(currentPath),
                    contentDescription = stringResource(id = R.string.cd_proof_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(14.dp)),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        val (file, contentUri) = ProofPhotoStorage.createCameraDestination(context)
                        pendingCameraFile = file
                        cameraLauncher.launch(contentUri)
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BurnishedAmber,
                        contentColor = ObsidianVoid,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("capture_camera_proof_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentPath == null) "تصوير الآن" else "إعادة التصوير",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isBusy,
                    border = BorderStroke(1.dp, BurnishedAmber.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pick_gallery_proof_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = BurnishedAmber,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "من المعرض",
                        color = BurnishedAmber,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            if (currentPath != null) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 200) caption = it },
                    label = { Text("ملاحظة مرافقة للصورة (اختياري)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("photo_caption_input"),
                )

                Button(
                    onClick = {
                        val note = caption.trim().ifBlank { "تم إثبات المهمة بالتصوير" }
                        onPhotoProofVerified(currentPath, note)
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldVerified,
                        contentColor = ObsidianVoid,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_photo_proof_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اعتماد الصورة وحفظها في السجل",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
