package ru.yandex.market.sc.test.data.manual.inbound

import ru.yandex.market.sc.core.data.inbound.Inbound
import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.core.utils.data.functional.Functional.catch
import ru.yandex.market.sc.core.utils.data.functional.Functional.orThrow
import ru.yandex.market.sc.core.utils.ext.parseOrDefault
import ru.yandex.market.sc.test.data.common.sortingcenter.SortingCenterMapper
import ru.yandex.market.sc.test.data.constants.Global.DEFAULT
import ru.yandex.market.sc.test.data.constants.Global.DEFAULT_DATE
import ru.yandex.market.sc.test.data.constants.Global.DEFAULT_EXTERNAL_ID

object ManualInboundMapper {
    fun map(dto: ManualInboundDto): Exceptional<ManualInbound> = catch {
        ManualInbound(
            externalId = ExternalId(dto.inboundExternalId ?: DEFAULT_EXTERNAL_ID),
            fromDate = dto.fromDate ?: DEFAULT_DATE,
            toDate = dto.toDate ?: DEFAULT_DATE,
            status = parseOrDefault(dto.status, Inbound.Status.UNKNOWN),
            sortingCenter = dto.sortingCenter?.let(SortingCenterMapper::map).orThrow(),
            warehouseFrom = dto.warehouseFrom ?: DEFAULT,
            transportationId = ExternalId(dto.transportationId ?: DEFAULT_EXTERNAL_ID),
            inboundType = parseOrDefault(dto.inboundType, Inbound.Type.UNKNOWN),
            carNumber = dto.carNumber,
        )
    }
}
