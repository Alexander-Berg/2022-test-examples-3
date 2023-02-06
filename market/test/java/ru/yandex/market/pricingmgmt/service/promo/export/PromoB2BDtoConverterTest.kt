package ru.yandex.market.pricingmgmt.service.promo.export

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.model.postgres.Category
import ru.yandex.market.pricingmgmt.model.promo.*
import ru.yandex.market.pricingmgmt.model.promo.mechanics.Promocode
import ru.yandex.market.pricingmgmt.model.promo.mechanics.PromocodeType
import ru.yandex.market.pricingmgmt.model.promo.restrictions.PromoCategoryRestrictionItem
import ru.yandex.market.pricingmgmt.service.promo.export.dto.*
import java.time.OffsetDateTime
import java.util.*

class PromoB2BDtoConverterTest : AbstractFunctionalTest() {

    @Autowired
    private val promoB2BDtoConverter: PromoB2BDtoConverter? = null

    @Test
    fun convertNewFlashPromoWithoutConstraintsAndChannels() {
        val promo = Promo(
            promoId = "promoid",
            parentPromoId = "parentPromoId",
            mechanicsType = PromoMechanicsType.BLUE_FLASH,
            tradeManager = "trade",
            name = "best flash",
            landingUrl = "landingUrl",
            rulesUrl = "rulesUrl",
            piPublishDate = OffsetDateTime.parse("2022-03-03T00:05:00Z").toEpochSecond(),
            createdAt = OffsetDateTime.parse("2022-04-03T00:05:00Z").toEpochSecond(),
            status = PromoStatus.NEW,
            active = true
        )

        val actualResult = promoB2BDtoConverter?.toDto(promo, CategoryCache(categories = Collections.emptyList()))

        val expectedResult = PromoDescriptionRequestDto(
            promoId = "promoid",
            parentPromoId = "parentPromoId",
            promoMechanic = PiPromoMechanicDto.BLUE_FLASH,
            promoResponsibles = PromoResponsibleDto("trade"),
            additionalInfo = PromoAdditionalInfoDto(
                promoName = "best flash",
                status = PromoStatusDto.NEW,
                landingUrl = "landingUrl",
                rulesUrl = "rulesUrl",
                publishPiDate = OffsetDateTime.parse("2022-03-03T00:05:00Z").toEpochSecond(),
                createdAtDate = OffsetDateTime.parse("2022-04-03T00:05:00Z").toEpochSecond()
            ),
            promoMechanicsData = null,
            channelsDto = PromoChannelsDto(Collections.emptyList()),
            constraints = PromoConstraintsDto(
                startDateTime = null,
                endDateTime = null,
                enabled = false,
                categoryRestrictions = CategoryRestrictionDto(Collections.emptyList()),
                originalCategoryRestrictions = OriginalCategoryRestrictionDto(Collections.emptyList()),
                originalBrandRestrictions = OriginalBrandRestrictionDto(Collections.emptyList()),
                mskuRestrictions = MskuRestrictionDto(Collections.emptyList()),
                supplierRestrictions = SupplierRestrictionDto(Collections.emptyList()),
                warehouseRestrictions = WarehouseRestrictionDto(Collections.emptyList())
            )
        )

        Assertions.assertEquals(expectedResult, actualResult)
    }

    @DbUnitDataSet(
        before = ["PromoB2BDtoConverterTest.channels.csv"]
    )
    @Test
    fun convertRunningPromocodePromo() {
        val promo = Promo(
            promoId = "promoid",
            parentPromoId = "parentPromoId",
            mechanicsType = PromoMechanicsType.PROMO_CODE,
            tradeManager = "trade",
            name = "best flash",
            landingUrl = "landingUrl",
            rulesUrl = "rulesUrl",
            piPublishDate = OffsetDateTime.parse("2022-03-03T00:05:00Z").toEpochSecond(),
            createdAt = OffsetDateTime.parse("2022-04-03T00:05:00Z").toEpochSecond(),
            status = PromoStatus.READY,
            active = true,
            promocode = Promocode(
                code = "code",
                codeType = PromocodeType.PERCENTAGE,
                value = 50,
                minCartPrice = 0L,
                maxCartPrice = 1234567L,
                additionalConditions = "additionalConditions",
                applyMultipleTimes = true
            ),
            promotions = listOf(
                PromoPromotion(
                    catteamName = "DiY",
                    categoryName = "Внешнее продвижение",
                    channelName = "Mobile Performance"
                ),
                PromoPromotion(
                    catteamName = "DiY",
                    categoryName = "Внешнее продвижение",
                    channelName = "Web Performance"
                ),
                PromoPromotion(
                    catteamName = "ЭиБТ",
                    categoryName = "Контентное присутствие",
                    channelName = "Landing page"
                ),
            )
        )

        val actualResult = promoB2BDtoConverter?.toDto(promo, CategoryCache(categories = Collections.emptyList()))

        val expectedResult = PromoDescriptionRequestDto(
            promoId = "promoid",
            parentPromoId = "parentPromoId",
            promoMechanic = PiPromoMechanicDto.MARKET_PROMOCODE,
            promoResponsibles = PromoResponsibleDto("trade"),
            additionalInfo = PromoAdditionalInfoDto(
                promoName = "best flash",
                status = PromoStatusDto.RUNNING,
                landingUrl = "landingUrl",
                rulesUrl = "rulesUrl",
                publishPiDate = OffsetDateTime.parse("2022-03-03T00:05:00Z").toEpochSecond(),
                createdAtDate = OffsetDateTime.parse("2022-04-03T00:05:00Z").toEpochSecond()
            ),
            promoMechanicsData = PromoMechanicsDataDto(
                marketPromocodeMechanic = MarketPromocodeMechanicDto(
                    promocode = "code",
                    discountType = PromocodeDiscountTypeDto.PERCENTAGE,
                    discountValue = 50,
                    cartMinPrice = 0L,
                    orderMaxPrice = 1234567L,
                    additionalConditionsText = "additionalConditions",
                    applyingType = PromocodeApplyingTypeDto.REUSABLE
                )
            ),
            channelsDto = PromoChannelsDto(listOf(55, 54, 61)),
            constraints = PromoConstraintsDto(
                startDateTime = null,
                endDateTime = null,
                enabled = true,
                categoryRestrictions = null,
                originalCategoryRestrictions = OriginalCategoryRestrictionDto(Collections.emptyList()),
                originalBrandRestrictions = OriginalBrandRestrictionDto(Collections.emptyList()),
                mskuRestrictions = MskuRestrictionDto(Collections.emptyList()),
                supplierRestrictions = SupplierRestrictionDto(Collections.emptyList()),
                warehouseRestrictions = WarehouseRestrictionDto(Collections.emptyList())
            )
        )

        Assertions.assertEquals(expectedResult, actualResult)
    }

    @DbUnitDataSet(
        before = ["PromoB2BDtoConverterTest.channels.csv"]
    )
    @Test
    fun convertPromoWithConstrains() {
        val promo = Promo(
            promoId = "promoid",
            parentPromoId = "parentPromoId",
            mechanicsType = PromoMechanicsType.DIRECT_DISCOUNT,
            tradeManager = "trade",
            name = "best flash",
            landingUrl = "landingUrl",
            rulesUrl = "rulesUrl",
            piPublishDate = OffsetDateTime.parse("2022-03-03T00:05:00Z").toEpochSecond(),
            createdAt = OffsetDateTime.parse("2022-04-03T00:05:00Z").toEpochSecond(),
            status = PromoStatus.READY,
            active = true,
            promotions = listOf(
                PromoPromotion(
                    catteamName = "DiY",
                    categoryName = "Внешнее продвижение",
                    channelName = "Mobile Performance"
                ),
                PromoPromotion(
                    catteamName = "DiY",
                    categoryName = "Внешнее продвижение",
                    channelName = "Web Performance"
                ),
                PromoPromotion(
                    catteamName = "ЭиБТ",
                    categoryName = "Контентное присутствие",
                    channelName = "Landing page"
                ),
            ),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 1L, percent = 30),
                PromoCategoryRestrictionItem(id = 2L, percent = 25)
            ),
            vendorsRestriction = listOf(1L, 2L, 3L),
            mskusRestriction = listOf(4L, 5L, 6L),
            warehousesRestriction = listOf(7L, 8L, 9L),
            partnersRestriction = listOf(10L, 11L, 12L),
        )

        val actualResult = promoB2BDtoConverter?.toDto(promo, CategoryCache(categories = Collections.emptyList()))

        val expectedResult = PromoDescriptionRequestDto(
            promoId = "promoid",
            parentPromoId = "parentPromoId",
            promoMechanic = PiPromoMechanicDto.DIRECT_DISCOUNT,
            promoResponsibles = PromoResponsibleDto("trade"),
            additionalInfo = PromoAdditionalInfoDto(
                promoName = "best flash",
                status = PromoStatusDto.RUNNING,
                landingUrl = "landingUrl",
                rulesUrl = "rulesUrl",
                publishPiDate = OffsetDateTime.parse("2022-03-03T00:05:00Z").toEpochSecond(),
                createdAtDate = OffsetDateTime.parse("2022-04-03T00:05:00Z").toEpochSecond()
            ),
            promoMechanicsData = null,
            channelsDto = PromoChannelsDto(listOf(55, 54, 61)),
            constraints = PromoConstraintsDto(
                startDateTime = null,
                endDateTime = null,
                enabled = true,
                categoryRestrictions = CategoryRestrictionDto(Collections.emptyList()),
                originalCategoryRestrictions = OriginalCategoryRestrictionDto(
                    listOf(
                        PromoCategoryDto(1L, 30),
                        PromoCategoryDto(2L, 25)
                    )
                ),
                originalBrandRestrictions = OriginalBrandRestrictionDto(
                    listOf(
                        PromoBrandDto(1L),
                        PromoBrandDto(2L),
                        PromoBrandDto(3L)
                    )
                ),
                mskuRestrictions = MskuRestrictionDto(listOf(4L, 5L, 6L)),
                warehouseRestrictions = WarehouseRestrictionDto(listOf(7L, 8L, 9L)),
                supplierRestrictions = SupplierRestrictionDto(listOf(10L, 11L, 12L))
            )
        )

        Assertions.assertEquals(expectedResult, actualResult)
    }


    @DbUnitDataSet(
        before = ["PromoB2BDtoConverterTest.channels.csv"]
    )
    @Test
    fun convertPromoWithLeafsCategory() {
        val promo = Promo(
            //region promo init
            promoId = "promoid",
            parentPromoId = "parentPromoId",
            mechanicsType = PromoMechanicsType.DIRECT_DISCOUNT,
            tradeManager = "trade",
            name = "best flash",
            landingUrl = "landingUrl",
            rulesUrl = "rulesUrl",
            piPublishDate = OffsetDateTime.parse("2022-03-03T00:05:00Z").toEpochSecond(),
            createdAt = OffsetDateTime.parse("2022-04-03T00:05:00Z").toEpochSecond(),
            status = PromoStatus.READY,
            active = true,
            promotions = listOf(
                PromoPromotion(
                    catteamName = "DiY",
                    categoryName = "Внешнее продвижение",
                    channelName = "Mobile Performance"
                ),
                PromoPromotion(
                    catteamName = "DiY",
                    categoryName = "Внешнее продвижение",
                    channelName = "Web Performance"
                ),
                PromoPromotion(
                    catteamName = "ЭиБТ",
                    categoryName = "Контентное присутствие",
                    channelName = "Landing page"
                ),
            ),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 1L, percent = 30),
                PromoCategoryRestrictionItem(id = 2L, percent = 25),
                PromoCategoryRestrictionItem(id = 4L, percent = 60)
            ),
            vendorsRestriction = listOf(1L, 2L, 3L),
            mskusRestriction = listOf(4L, 5L, 6L),
            warehousesRestriction = listOf(7L, 8L, 9L),
            partnersRestriction = listOf(10L, 11L, 12L),
        )
        //endregion

        val actualResult = promoB2BDtoConverter?.toDto(
            promo,
            CategoryCache(
                categories = listOf(
                    Category(id = 1L, parentId = -1L, name = "c1"),
                    Category(id = 2L, parentId = 7L, name = "c2"),
                    Category(id = 3L, parentId = 2L, name = "c3")
                )
            )
        )

        val expectedResult = PromoDescriptionRequestDto(
            //region init expectedResult
            promoId = "promoid",
            parentPromoId = "parentPromoId",
            promoMechanic = PiPromoMechanicDto.DIRECT_DISCOUNT,
            promoResponsibles = PromoResponsibleDto("trade"),
            additionalInfo = PromoAdditionalInfoDto(
                promoName = "best flash",
                status = PromoStatusDto.RUNNING,
                landingUrl = "landingUrl",
                rulesUrl = "rulesUrl",
                publishPiDate = OffsetDateTime.parse("2022-03-03T00:05:00Z").toEpochSecond(),
                createdAtDate = OffsetDateTime.parse("2022-04-03T00:05:00Z").toEpochSecond()
            ),
            promoMechanicsData = null,
            channelsDto = PromoChannelsDto(listOf(55, 54, 61)),
            constraints = PromoConstraintsDto(
                startDateTime = null,
                endDateTime = null,
                enabled = true,
                categoryRestrictions = CategoryRestrictionDto(
                    listOf(
                        PromoCategoryDto(1L, 30),
                        PromoCategoryDto(3L, 25)
                    )
                ),
                originalCategoryRestrictions = OriginalCategoryRestrictionDto(
                    listOf(
                        PromoCategoryDto(1L, 30),
                        PromoCategoryDto(2L, 25),
                        PromoCategoryDto(4L, 60)
                    )
                ),
                originalBrandRestrictions = OriginalBrandRestrictionDto(
                    listOf(
                        PromoBrandDto(1L),
                        PromoBrandDto(2L),
                        PromoBrandDto(3L)
                    )
                ),
                mskuRestrictions = MskuRestrictionDto(listOf(4L, 5L, 6L)),
                warehouseRestrictions = WarehouseRestrictionDto(listOf(7L, 8L, 9L)),
                supplierRestrictions = SupplierRestrictionDto(
                    listOf(10L, 11L, 12L),
                )
            )
        )
        //endregion

        Assertions.assertEquals(expectedResult, actualResult)
    }

    @DbUnitDataSet(
        before = ["PromoB2BDtoConverterTest.channels.csv"]
    )
    @Test
    fun convertPromoWithDuplicatedLeafsCategory() {
        val promo = Promo(
            //region promo init
            promoId = "promoid",
            parentPromoId = "parentPromoId",
            mechanicsType = PromoMechanicsType.DIRECT_DISCOUNT,
            tradeManager = "trade",
            name = "best flash",
            landingUrl = "landingUrl",
            rulesUrl = "rulesUrl",
            piPublishDate = OffsetDateTime.parse("2022-03-03T00:05:00Z").toEpochSecond(),
            createdAt = OffsetDateTime.parse("2022-04-03T00:05:00Z").toEpochSecond(),
            status = PromoStatus.READY,
            active = true,
            promotions = listOf(
                PromoPromotion(
                    catteamName = "DiY",
                    categoryName = "Внешнее продвижение",
                    channelName = "Mobile Performance"
                ),
                PromoPromotion(
                    catteamName = "DiY",
                    categoryName = "Внешнее продвижение",
                    channelName = "Web Performance"
                ),
                PromoPromotion(
                    catteamName = "ЭиБТ",
                    categoryName = "Контентное присутствие",
                    channelName = "Landing page"
                ),
            ),
            categoriesRestriction = listOf(
                PromoCategoryRestrictionItem(id = 1L, percent = 30),
                PromoCategoryRestrictionItem(id = 2L, percent = 25),
                PromoCategoryRestrictionItem(id = 4L, percent = 60),
                PromoCategoryRestrictionItem(id = 5L, percent = 60)
            ),
            vendorsRestriction = listOf(1L, 2L, 3L),
            mskusRestriction = listOf(4L, 5L, 6L),
            warehousesRestriction = listOf(7L, 8L, 9L),
            partnersRestriction = listOf(10L, 11L, 12L),
        )
        //endregion

        val actualResult = promoB2BDtoConverter?.toDto(
            promo,
            CategoryCache(
                categories = listOf(
                    Category(id = 1L, parentId = -1L, name = "c1"),
                    Category(id = 2L, parentId = 7L, name = "c2"),
                    Category(id = 3L, parentId = 2L, name = "c3"),
                    Category(id = 5L, parentId = 2L, name = "c5")
                )
            )
        )

        val expectedResult = PromoDescriptionRequestDto(
            //region init expectedResult
            promoId = "promoid",
            parentPromoId = "parentPromoId",
            promoMechanic = PiPromoMechanicDto.DIRECT_DISCOUNT,
            promoResponsibles = PromoResponsibleDto("trade"),
            additionalInfo = PromoAdditionalInfoDto(
                promoName = "best flash",
                status = PromoStatusDto.RUNNING,
                landingUrl = "landingUrl",
                rulesUrl = "rulesUrl",
                publishPiDate = OffsetDateTime.parse("2022-03-03T00:05:00Z").toEpochSecond(),
                createdAtDate = OffsetDateTime.parse("2022-04-03T00:05:00Z").toEpochSecond()
            ),
            promoMechanicsData = null,
            channelsDto = PromoChannelsDto(listOf(55, 54, 61)),
            constraints = PromoConstraintsDto(
                startDateTime = null,
                endDateTime = null,
                enabled = true,
                categoryRestrictions = CategoryRestrictionDto(
                    listOf(
                        PromoCategoryDto(1L, 30),
                        PromoCategoryDto(3L, 25),
                        PromoCategoryDto(5L, 60)
                    )
                ),
                originalCategoryRestrictions = OriginalCategoryRestrictionDto(
                    listOf(
                        PromoCategoryDto(1L, 30),
                        PromoCategoryDto(2L, 25),
                        PromoCategoryDto(4L, 60),
                        PromoCategoryDto(5L, 60)
                    )
                ),
                originalBrandRestrictions = OriginalBrandRestrictionDto(
                    listOf(
                        PromoBrandDto(1L),
                        PromoBrandDto(2L),
                        PromoBrandDto(3L)
                    )
                ),
                mskuRestrictions = MskuRestrictionDto(listOf(4L, 5L, 6L)),
                warehouseRestrictions = WarehouseRestrictionDto(listOf(7L, 8L, 9L)),
                supplierRestrictions = SupplierRestrictionDto(
                    listOf(10L, 11L, 12L),
                )
            )
        )
        //endregion

        Assertions.assertEquals(expectedResult, actualResult)
    }
}
