package ru.yandex.market.pricingmgmt.service.promo.converters

import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.mj.generated.server.model.PromoRestrictionItemDto

@DbUnitDataSet(before = ["PartnerRestrictionExtendedConverterTest.csv"])
internal class PartnerRestrictionPromoConverterTest :
    AbstractCommonRestrictionExtendedConverterTest<PartnerRestrictionPromoConverter>(
        notEmptyOk = listOf(1L, 2L),
        notEmptyOkExpected = listOf(
            PromoRestrictionItemDto().id(1).name("partner01").outdated(false),
            PromoRestrictionItemDto().id(2).name("partner02").outdated(true)
        ),
        notExistsThrows = listOf(1, 2, 3),
        notExistsExpected = "partner keys not found: 3"
    )
