package ru.yandex.market.pricingmgmt.service.promo.converters

import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.model.promo.restrictions.PromoCategoryRestrictionItem
import ru.yandex.mj.generated.server.model.PromoCategoriesRestrictionItemDto

@DbUnitDataSet(before = ["CategoryRestrictionExtendedConverterTest.csv"])
internal class CategoryRestrictionPromoConverterTest :
    AbstractRestrictionExtendedConverterTest<Long, PromoCategoryRestrictionItem, PromoCategoriesRestrictionItemDto, CategoryRestrictionPromoConverter>(
        notEmptyOk = listOf(
            PromoCategoryRestrictionItem(id = 21, percent = 10),
            PromoCategoryRestrictionItem(id = 22, percent = 20)
        ),
        notEmptyOkExpected = listOf(
            PromoCategoriesRestrictionItemDto().id(21).name("category21").outdated(false).percent(10),
            PromoCategoriesRestrictionItemDto().id(22).name("category22").outdated(true).percent(20)
        ),
        notExistsThrows = listOf(
            PromoCategoryRestrictionItem(id = 21, percent = 10),
            PromoCategoryRestrictionItem(id = 22, percent = 20),
            PromoCategoryRestrictionItem(id = 23, percent = 30)
        ),
        notExistsExpected = "category keys not found: 23"
    )
