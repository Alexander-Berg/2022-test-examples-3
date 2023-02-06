package ru.yandex.market.sc.test.data.common.inbound

import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.core.utils.data.functional.Functional.catch
import ru.yandex.market.sc.core.utils.ext.parseOrDefault
import ru.yandex.market.sc.test.data.constants.Global.DEFAULT_EXTERNAL_ID

object InboundRegistryOrderMapper {
    fun map(dto: InboundRegistryOrderDto): Exceptional<InboundRegistryOrder> = catch {
        InboundRegistryOrder(
            orderExternalId = ExternalId(dto.orderExternalId ?: DEFAULT_EXTERNAL_ID),
            placeExternalId = ExternalId(dto.orderExternalId ?: DEFAULT_EXTERNAL_ID),
            status = parseOrDefault(dto.status, InboundRegistryOrder.Status.UNKNOWN),
            palledId = ExternalId(dto.orderExternalId ?: DEFAULT_EXTERNAL_ID),
        )
    }
}