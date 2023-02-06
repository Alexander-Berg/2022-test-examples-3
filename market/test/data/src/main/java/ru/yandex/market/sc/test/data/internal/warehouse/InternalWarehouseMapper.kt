package ru.yandex.market.sc.test.data.internal.warehouse

import ru.yandex.market.sc.core.utils.data.Exceptional
import ru.yandex.market.sc.core.utils.data.ExternalId
import ru.yandex.market.sc.core.utils.data.functional.Functional.catch
import ru.yandex.market.sc.core.utils.data.validation.Validation.validateAll

object InternalWarehouseMapper {
    fun map(dto: InternalWarehouseDto): Exceptional<InternalWarehouse> = catch {
        validateAll {
            notNull(dto.id, "id")
        }

        InternalWarehouse(
            id = dto.id!!,
            incorporation = dto.incorporation,
            partnerId = dto.partnerId,
            yandexId = ExternalId(dto.yandexId),
        )
    }
}