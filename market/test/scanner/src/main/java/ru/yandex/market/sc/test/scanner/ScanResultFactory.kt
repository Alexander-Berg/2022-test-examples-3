package ru.yandex.market.sc.test.scanner

import ru.yandex.market.sc.core.data.scanner.ScanFormat
import ru.yandex.market.sc.core.data.scanner.ScanResult
import ru.yandex.market.sc.core.utils.data.ExternalId

object ScanResultFactory {
    fun getScanResultManual(value: String) =
        ScanResult(value, ScanFormat.Manual)

    fun getScanResultQR(value: String) =
        ScanResult(value, ScanFormat.QRCode)

    fun getScanResultQR(value: Long) = getScanResultQR(value.toString())

    fun getScanResultQR(value: ExternalId) = getScanResultQR(value.value)

    fun getScanResultBarcode(externalId: String) =
        ScanResult(externalId, ScanFormat.Barcode)

    fun getScanResultBarcode(externalId: ExternalId) = getScanResultBarcode(externalId.value)

    fun getOrderDefaultScanResult(externalId: ExternalId) = getScanResultQR(externalId)

    fun getPlaceDefaultScanResult(externalId: ExternalId) = getScanResultQR(externalId)

    fun getUnsupportedScanResult(id: ExternalId) = ScanResult(
        value = id.value,
        format = ScanFormat.Unsupported,
    )

    fun getUnsupportedScanResult(value: Long) = ScanResult(
        value = value.toString(),
        format = ScanFormat.Unsupported,
    )
}
