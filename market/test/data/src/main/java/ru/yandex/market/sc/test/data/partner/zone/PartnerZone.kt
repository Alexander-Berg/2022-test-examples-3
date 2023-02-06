package ru.yandex.market.sc.test.data.partner.zone

data class PartnerZone(
    val zone: Zone
) {
    data class Zone(
        val id: Long,
        val sortingCenterId: Long?,
        val name: String?,
        val deleted: Boolean?,
    )
}
