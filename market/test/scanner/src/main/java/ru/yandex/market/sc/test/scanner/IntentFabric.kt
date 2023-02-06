package ru.yandex.market.sc.test.scanner

import android.content.Intent

object IntentFabric {
    private fun create(activityName: String): Intent {
        return Intent("$activityName.ACTION").apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
    }

    fun createM3Scan(activityName: String, data: String, isQr: Boolean = false): Intent {
        val type = if (isQr) "QR Code" else "Code 128"
        val extraMap = mapOf(
            "m3scannerdata" to data,
            "m3scanner_code_type" to type,
        )

        return create(activityName).apply {
            extraMap.entries.forEach {
                putExtra(it.key, it.value)
            }
        }
    }

    fun createZebraScan(activityName: String, data: String, isQr: Boolean = false): Intent {
        val type = if (isQr) "LABEL-TYPE-QRCODE" else "LABEL-TYPE-CODE-128"
        val extraMap = mapOf(
            "com.symbol.datawedge.data_string" to data,
            "com.symbol.datawedge.label_type" to type,
        )

        return create(activityName).apply {
            extraMap.entries.forEach {
                putExtra(it.key, it.value)
            }
        }
    }

    fun createHoneywellScan(activityName: String, data: String, isQr: Boolean = false): Intent {
        val type = if (isQr) "s" else "b"
        val extraMap = mapOf(
            "version" to 1,
            "data" to data,
            "codeId" to type,
        )

        return create(activityName).apply {
            extraMap.entries.forEach {
                putExtra(it.key, it.value)
            }
        }
    }
}