package ru.yandex.market.pricingmgmt.service.promo.converters

import ru.yandex.mj.generated.server.model.PromoRestrictionItemDto

internal abstract class AbstractCommonRestrictionExtendedConverterTest<CONVERTER : RestrictionExtendedConverter<Long, Long, PromoRestrictionItemDto>>(
    notEmptyOk: List<Long>,
    notEmptyOkExpected: List<PromoRestrictionItemDto>,
    notExistsThrows: List<Long>,
    notExistsExpected: String
) : AbstractRestrictionExtendedConverterTest<Long, Long, PromoRestrictionItemDto, CONVERTER>(
    notEmptyOk,
    notEmptyOkExpected,
    notExistsThrows,
    notExistsExpected
)
