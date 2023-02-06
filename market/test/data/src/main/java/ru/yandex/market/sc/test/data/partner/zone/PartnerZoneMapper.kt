package ru.yandex.market.sc.test.data.partner.zone

import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.functional.Functional


object PartnerZoneMapper {
    fun map(dto: PartnerZoneDto): Exceptional<PartnerZone> = Functional.catch {
        PartnerZone(
            zone = PartnerZone.Zone(
                id = dto.zone.id!!,
                sortingCenterId = dto.zone.sortingCenterId,
                name = dto.zone.name,
                deleted = dto.zone.deleted,
            )
        )
    }
}
