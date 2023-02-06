package ru.yandex.market.pricingmgmt.service.promo.converters

import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.mj.generated.server.model.PromoRestrictionItemDto

@DbUnitDataSet(before = ["MskuRestrictionExtendedConverterTest.csv"])
internal class MskuRestrictionPromoConverterTest :
    AbstractCommonRestrictionExtendedConverterTest<MskuRestrictionPromoConverter>(
        notEmptyOk = listOf(41L, 42L),
        notEmptyOkExpected = listOf(
            PromoRestrictionItemDto().id(41).name("msku41").outdated(false),
            PromoRestrictionItemDto().id(42).name("msku42").outdated(true)
        ),
        notExistsThrows = listOf(41, 42, 43),
        notExistsExpected = "msku keys not found: 43"
    )
