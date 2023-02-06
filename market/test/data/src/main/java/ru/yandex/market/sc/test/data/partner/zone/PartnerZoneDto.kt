package ru.yandex.market.sc.test.data.partner.zone

data class PartnerZoneDto(
    val zone: ZoneDto
) {
    data class ZoneDto(
        val id: Long?,
        val sortingCenterId: Long?,
        val name: String?,
        val deleted: Boolean?,
    )
}
