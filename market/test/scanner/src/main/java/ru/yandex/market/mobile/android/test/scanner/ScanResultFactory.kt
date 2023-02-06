package ru.yandex.market.mobile.android.test.scanner

import ru.yandex.market.mobile.android.core.scanner.api.ScanFormat
import ru.yandex.market.mobile.android.core.scanner.api.ScanResult

object ScanResultFactory {
    fun getManualScanResult(value: String) = ScanResult(value, ScanFormat.Manual)

    fun getQRScanResult(value: String) = ScanResult(value, ScanFormat.QRCode)

    fun getBarcodeScanResult(value: String) = ScanResult(value, ScanFormat.Barcode)
}
