package ru.yandex.market.pricingmgmt.service.promo.converters

import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.mj.generated.server.model.PromoRestrictionItemDto

@DbUnitDataSet(before = ["VendorRestrictionExtendedConverterTest.csv"])
internal class VendorRestrictionPromoConverterTest :
    AbstractCommonRestrictionExtendedConverterTest<VendorRestrictionPromoConverter>(
        notEmptyOk = listOf(31L, 32L),
        notEmptyOkExpected = listOf(
            PromoRestrictionItemDto().id(31).name("vendor31").outdated(false),
            PromoRestrictionItemDto().id(32).name("vendor32").outdated(true)
        ),
        notExistsThrows = listOf(31, 32, 33),
        notExistsExpected = "vendor keys not found: 33"
    )

