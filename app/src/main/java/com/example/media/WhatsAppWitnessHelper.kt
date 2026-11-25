package com.example.media

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

data class WhatsAppLaunchResult(
    val launched: Boolean,
    val statusMessage: String,
)

object WhatsAppWitnessHelper {

    private const val PKG_WHATSAPP = "com.whatsapp"
    private const val PKG_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    fun sanitizeDigits(rawPhone: String): String {
        val normalized = StringBuilder()
        for (ch in rawPhone) {
            val mapped = when (ch) {
                in '٠'..'٩' -> ('0' + (ch - '٠'))
                in '۰'..'۹' -> ('0' + (ch - '۰'))
                in '0'..'9' -> ch
                else -> null
            }
            if (mapped != null) normalized.append(mapped)
        }
        var digits = normalized.toString()
        if (digits.startsWith("00")) {
            digits = digits.drop(2)
        }
        return digits
    }

    fun isValidWitnessPhone(rawPhone: String): Boolean {
        val digits = sanitizeDigits(rawPhone)
        return digits.length in 8..18
    }

    fun formatStoredPhone(rawPhone: String): String {
        val digits = sanitizeDigits(rawPhone)
        return if (digits.isNotEmpty()) "+$digits" else ""
    }

    fun sendProofPhotoToWitness(
        context: Context,
        witnessPhone: String,
        witnessName: String?,
        taskTitle: String,
        proofNote: String,
        photoPath: String,
    ): WhatsAppLaunchResult {
        val digits = sanitizeDigits(witnessPhone)
        if (digits.length !in 8..18) {
            return WhatsAppLaunchResult(
                launched = false,
                statusMessage = "رقم واتساب المراقب غير صالح — أعد ضبطه مع رمز الدولة.",
            )
        }

        val shareUri = ProofPhotoStorage.getShareableUri(context, photoPath)
            ?: return WhatsAppLaunchResult(
                launched = false,
                statusMessage = "تعذّر تجهيز ملف الصورة للمشاركة عبر واتساب.",
            )

        val recipientLabel = witnessName?.takeIf { it.isNotBlank() } ?: "+$digits"
        val caption = buildString {
            append("✅ برهان إنجاز مصوّر — قيد الإلزام\n")
            append("المهمة: $taskTitle\n")
            if (proofNote.isNotBlank()) {
                append("التوثيق: $proofNote\n")
            }
            append("تم إتمام المهمة وتصوير البرهان بنجاح.")
        }

        val installedPkg = resolveInstalledWhatsAppPackage(context)
        if (installedPkg != null) {
            try {
                context.grantUriPermission(
                    installedPkg,
                    shareUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: Exception) {
                // Best effort; Intent flag also grants read permission.
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_TEXT, caption)
                putExtra("jid", "$digits@s.whatsapp.net")
                setPackage(installedPkg)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (tryStartActivity(context, sendIntent)) {
                return WhatsAppLaunchResult(
                    launched = true,
                    statusMessage = "فُتح واتساب تلقائياً على محادثة $recipientLabel مع الصورة والتقرير جاهزاً للإرسال.",
                )
            }
        }

        // Fallback 1: generic share sheet with photo + caption
        val chooserBase = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(chooserBase, "إرسال صورة البرهان إلى $recipientLabel").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (tryStartActivity(context, chooser)) {
            return WhatsAppLaunchResult(
                launched = true,
                statusMessage = "تم تجهيز الصورة للمشاركة إلى $recipientLabel عبر تطبيق الرسائل المتاح.",
            )
        }

        // Fallback 2: wa.me deep link for message text
        val waUri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(caption)}")
        val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (tryStartActivity(context, waIntent)) {
            return WhatsAppLaunchResult(
                launched = true,
                statusMessage = "فُتح رابط واتساب المباشر للمراقب $recipientLabel.",
            )
        }

        return WhatsAppLaunchResult(
            launched = false,
            statusMessage = "تطبيق واتساب غير مثبّت على هذا الجهاز.",
        )
    }

    fun sendPunishmentAlertToWitness(
        context: Context,
        witnessPhone: String,
        witnessName: String?,
        taskTitle: String,
        targetTimeLabel: String?,
    ): WhatsAppLaunchResult {
        val digits = sanitizeDigits(witnessPhone)
        if (digits.length !in 8..18) {
            return WhatsAppLaunchResult(
                launched = false,
                statusMessage = "رقم واتساب المراقب غير مضبوط.",
            )
        }

        val recipientLabel = witnessName?.takeIf { it.isNotBlank() } ?: "+$digits"
        val alertText = buildString {
            append("🚨 إنذار إخلال وعقاب تلقائي — قيد الإلزام\n")
            append("المهمة: $taskTitle\n")
            if (!targetTimeLabel.isNullOrBlank()) {
                append("الموعد المحدد: $targetTimeLabel\n")
            }
            append("انتهت المهلة المحددة دون تصوير البرهان!\n")
            append("العقاب المطبّق فوراً: خصم ٨٠ نقطة سيادة، إضافة ٣٠ دقيقة دين زمني، وتجميد إضافة المهام حتى تنفيذ كفّارة مصوّرة.")
        }

        val waUri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(alertText)}")
        val installedPkg = resolveInstalledWhatsAppPackage(context)
        if (installedPkg != null) {
            val directIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
                setPackage(installedPkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (tryStartActivity(context, directIntent)) {
                return WhatsAppLaunchResult(
                    launched = true,
                    statusMessage = "فُتح واتساب لإبلاغ $recipientLabel بالعقاب التلقائي.",
                )
            }
        }

        val fallbackIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (tryStartActivity(context, fallbackIntent)) {
            return WhatsAppLaunchResult(
                launched = true,
                statusMessage = "فُتح رابط إبلاغ $recipientLabel بالعقاب التلقائي.",
            )
        }

        return WhatsAppLaunchResult(
            launched = false,
            statusMessage = "تعذّر فتح واتساب لإرسال بلاغ العقاب.",
        )
    }

    private fun resolveInstalledWhatsAppPackage(context: Context): String? {
        val pm = context.packageManager
        val candidates = listOf(PKG_WHATSAPP, PKG_WHATSAPP_BUSINESS)
        return candidates.firstOrNull { pkg -> isPackageInstalled(pm, pkg) }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun tryStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
