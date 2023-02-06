package ru.yandex.market.pricingmgmt.service.promo.validators

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import ru.yandex.market.pricingmgmt.exception.ExceptionCode
import ru.yandex.market.pricingmgmt.exception.ValidationException
import ru.yandex.market.pricingmgmt.model.promo.AssortmentLoadMethod
import ru.yandex.market.pricingmgmt.model.promo.Promo
import ru.yandex.market.pricingmgmt.model.promo.PromoMechanicsType
import ru.yandex.market.pricingmgmt.model.promo.mechanics.CheapestAsGift
import ru.yandex.market.pricingmgmt.model.promo.mechanics.CompleteSetKind
import ru.yandex.market.pricingmgmt.model.promo.restrictions.PromoCategoryRestrictionItem
import ru.yandex.market.pricingmgmt.service.TimeService
import java.time.OffsetDateTime

internal class PiPromoValidatorTest {
    private val timeService = Mockito.mock(TimeService::class.java)
    private val piPromoValidator = PiPromoValidator(timeService)

    private var oldPromo: Promo = Promo()
    private var promo: Promo = Promo()

    private val now = OffsetDateTime.now()
    private val promoPublicationInPast = now.minusDays(1).toEpochSecond()
    private val promoPublicationInFuture = now.plusSeconds(1).toEpochSecond()

    @BeforeEach
    fun setUp() {
        `when`(timeService.getNowOffsetDateTime()).thenReturn(now)

        oldPromo = Promo()
        oldPromo.startDate = now.plusMonths(2).toEpochSecond()
        oldPromo.endDate = now.plusMonths(5).toEpochSecond()
        oldPromo.piPublishDate = now.plusMonths(1).toEpochSecond()
        oldPromo.mechanicsType = PromoMechanicsType.DIRECT_DISCOUNT
        oldPromo.categoriesRestriction = listOf(
            PromoCategoryRestrictionItem(1, 1),
            PromoCategoryRestrictionItem(2, 2)
        )
        oldPromo.warehousesRestriction = listOf(1L)
        oldPromo.partnersRestriction = listOf(2L, 3L)
        oldPromo.vendorsRestriction = listOf(4L, 5L)
        oldPromo.mskusRestriction = listOf(6L, 7L)
        oldPromo.cheapestAsGift = CheapestAsGift(completeSetKind = CompleteSetKind.SET_OF_10)
        oldPromo.assortmentLoadMethod = AssortmentLoadMethod.PI

        promo = Promo()
        promo.startDate = now.plusMonths(2).toEpochSecond()
        promo.endDate = now.plusMonths(5).toEpochSecond()
        promo.piPublishDate = now.plusMonths(1).toEpochSecond()
        promo.mechanicsType = PromoMechanicsType.DIRECT_DISCOUNT
        promo.categoriesRestriction = listOf(
            PromoCategoryRestrictionItem(1, 1),
            PromoCategoryRestrictionItem(2, 2)
        )
        promo.warehousesRestriction = listOf(1L)
        promo.partnersRestriction = listOf(2L, 3L)
        promo.vendorsRestriction = listOf(4L, 5L)
        promo.mskusRestriction = listOf(6L, 7L)
        promo.cheapestAsGift = CheapestAsGift(completeSetKind = CompleteSetKind.SET_OF_10)
        promo.assortmentLoadMethod = AssortmentLoadMethod.PI
    }

    @Test
    fun test_validate_create_notPublished_ok() {
        assertDoesNotThrow { piPromoValidator.validate(promo, null) }
    }

    @Test
    fun test_validate_create_published_ok() {
        promo.piPublishDate = promoPublicationInPast
        assertDoesNotThrow { piPromoValidator.validate(promo, null) }
    }

    @Test
    fun test_validate_publicationDateIsMissing_throws() {
        promo.piPublishDate = null
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_PI_PUBLISH_DATE_NULL, e.code)
        assertEquals("Не указана дата публикации в ПИ", e.message)
        assertEquals(listOf("piPublishDate"), e.errorFields)
    }

    @Test
    fun test_validate_publicationAfterStartDate_throws() {
        promo.piPublishDate = promo.startDate!! + 1
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_PI_PUBLISH_DATE_AFTER_START_DATE, e.code)
        assertEquals("Дата публикации в ПИ позже даты старта", e.message)
        assertEquals(listOf("piPublishDate"), e.errorFields)

    }

    @Test
    fun test_validate_create_emptyCategories_throws() {
        promo.categoriesRestriction = emptyList()
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_CATEGORIES_EMPTY, e.code)
        assertEquals("Не указаны категории", e.message)
        assertEquals(listOf("categoriesRestriction"), e.errorFields)

    }

    @ParameterizedTest
    @EnumSource(
        value = PromoMechanicsType::class,
        names = ["CHEAPEST_AS_GIFT"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun test_validate_create_emptyWarehouse_ok(promoMechanicsType: PromoMechanicsType) {
        promo.mechanicsType = promoMechanicsType
        promo.warehousesRestriction = null
        assertDoesNotThrow { piPromoValidator.validate(promo, null) }
    }

    @ParameterizedTest
    @EnumSource(
        value = PromoMechanicsType::class,
        names = ["CHEAPEST_AS_GIFT"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun test_validate_create_emptyWarehouse_throws(promoMechanicsType: PromoMechanicsType) {
        promo.mechanicsType = promoMechanicsType
        promo.warehousesRestriction = null
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_WAREHOUSES_EMPTY, e.code)
        assertEquals("Не указаны склады", e.message)
        assertEquals(listOf("warehousesRestriction"), e.errorFields)
    }

    @ParameterizedTest
    @EnumSource(
        value = PromoMechanicsType::class,
        names = ["CHEAPEST_AS_GIFT"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun test_validate_create_oneWarehouse_ok(promoMechanicsType: PromoMechanicsType) {
        promo.mechanicsType = promoMechanicsType
        promo.warehousesRestriction = listOf(1L)
        assertDoesNotThrow { piPromoValidator.validate(promo, null) }
    }

    @ParameterizedTest
    @EnumSource(
        value = PromoMechanicsType::class,
        names = ["CHEAPEST_AS_GIFT"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun test_validate_create_severalWarehouses_throws(promoMechanicsType: PromoMechanicsType) {
        promo.mechanicsType = promoMechanicsType
        promo.warehousesRestriction = listOf(1L, 2L)
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, null) }
        assertEquals(ExceptionCode.PROMO_WAREHOUSES_MORE_THAN_ONE, e.code)
        assertEquals("Указано более одного склада", e.message)
        assertEquals(listOf("warehousesRestriction"), e.errorFields)
    }

    @ParameterizedTest
    @EnumSource(
        value = PromoMechanicsType::class,
        names = ["CHEAPEST_AS_GIFT"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun test_validate_create_severalWarehouses_ok(promoMechanicsType: PromoMechanicsType) {
        promo.mechanicsType = promoMechanicsType
        promo.warehousesRestriction = listOf(1L, 2L)
        assertDoesNotThrow { piPromoValidator.validate(promo, null) }
    }

    @Test
    fun test_validate_update_notPublished_ok() {
        assertDoesNotThrow { piPromoValidator.validate(promo, oldPromo) }
    }

    @Test
    fun test_validate_update_published_ok() {
        promo.piPublishDate = promoPublicationInPast
        assertDoesNotThrow { piPromoValidator.validate(promo, oldPromo) }
    }

    @Test
    fun test_validate_update_published_in_future_ok() {
        promo.piPublishDate = promoPublicationInFuture
        promo.startDate = promo.startDate!! - 1
        assertDoesNotThrow { piPromoValidator.validate(promo, oldPromo) }
    }

    @Test
    fun test_validate_updateNotPublished_startDateSetLess_skip() {
        promo.startDate = promo.startDate!! - 1
        assertDoesNotThrow { piPromoValidator.validate(promo, oldPromo) }
    }

    @Test
    fun test_validate_updatePublished_startDateSetLess_throws() {
        promo.piPublishDate = promoPublicationInPast
        promo.startDate = promo.startDate!! - 1
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_START_DATE_BEFORE_PREV_START_DATE, e.code)
        assertEquals("Дата старта меньше предыдущей даты старта", e.message)
        assertEquals(listOf("startDate"), e.errorFields)
    }

    @Test
    fun test_validate_updateNotPublished_endDateSetGreater_skip() {
        promo.endDate = promo.endDate!! + 1
        assertDoesNotThrow { piPromoValidator.validate(promo, oldPromo) }
    }

    @Test
    fun test_validate_updatePublished_endDateSetGreater_throws() {
        promo.piPublishDate = promoPublicationInPast
        promo.endDate = promo.endDate!! + 1
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_END_DATE_AFTER_PREV_END_DATE, e.code)
        assertEquals("Дата окончания больше предыдущей даты окончания", e.message)
        assertEquals(listOf("endDate"), e.errorFields)
    }

    @Test
    fun test_validate_update_emptyCategories_throws() {
        promo.categoriesRestriction = emptyList()
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_CATEGORIES_EMPTY, e.code)
        assertEquals("Не указаны категории", e.message)
        assertEquals(listOf("categoriesRestriction"), e.errorFields)
    }

    @Test
    fun test_validate_updateNotPublishedPromo_modifyPercent_skip() {
        promo.mechanicsType = PromoMechanicsType.DIRECT_DISCOUNT
        promo.categoriesRestriction = listOf(
            PromoCategoryRestrictionItem(1, 11),
            PromoCategoryRestrictionItem(2, 2)
        )
        assertDoesNotThrow { piPromoValidator.validate(promo, oldPromo) }
    }

    @Test
    fun test_validate_updatePublishedPromo_modifyPercent_throws() {
        promo.piPublishDate = promoPublicationInPast
        promo.mechanicsType = PromoMechanicsType.DIRECT_DISCOUNT
        promo.categoriesRestriction = listOf(
            PromoCategoryRestrictionItem(1, 11),
            PromoCategoryRestrictionItem(2, 2)
        )
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_CATEGORIES_PERCENT_CHANGED, e.code)
        assertEquals("Изменение процента скидки категории запрещено", e.message)
        assertEquals(listOf("categoriesRestriction"), e.errorFields)
    }

    @ParameterizedTest
    @EnumSource(
        value = PromoMechanicsType::class,
        names = ["CHEAPEST_AS_GIFT"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun test_validate_update_emptyWarehouse_ok(promoMechanicsType: PromoMechanicsType) {
        promo.mechanicsType = promoMechanicsType
        promo.warehousesRestriction = null
        assertDoesNotThrow { piPromoValidator.validate(promo, oldPromo) }
    }

    @ParameterizedTest
    @EnumSource(
        value = PromoMechanicsType::class,
        names = ["CHEAPEST_AS_GIFT"],
        mode = EnumSource.Mode.INCLUDE
    )
    fun test_validate_update_emptyWarehouse_throws(promoMechanicsType: PromoMechanicsType) {
        promo.mechanicsType = promoMechanicsType
        promo.warehousesRestriction = null
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_WAREHOUSES_EMPTY, e.code)
        assertEquals("Не указаны склады", e.message)
        assertEquals(listOf("warehousesRestriction"), e.errorFields)
    }

    @ParameterizedTest
    @EnumSource(value = PromoMechanicsType::class)
    fun test_validate_updateNotPublishedPromo_changeWarehouse_skip(promoMechanicsType: PromoMechanicsType) {
        promo.mechanicsType = promoMechanicsType
        promo.warehousesRestriction = listOf(3L)
        assertDoesNotThrow { piPromoValidator.validate(promo, oldPromo) }
    }

    @Test
    fun test_validate_updateNotPublishedPromo_changeCompleteSetKind_skip() {
        promo.mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT
        promo.cheapestAsGift!!.completeSetKind = CompleteSetKind.SET_OF_2
        assertDoesNotThrow { piPromoValidator.validate(promo, oldPromo) }
    }

    @Test
    fun test_validate_updatePublishedPromo_changeCompleteSetKind_throws() {
        promo.piPublishDate = promoPublicationInPast
        promo.mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT
        promo.cheapestAsGift!!.completeSetKind = CompleteSetKind.SET_OF_2
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_CHEAPEST_AS_GIFT_COMPLETE_SET_KIND_CHANGED, e.code)
        assertEquals("Изменение количества товаров в комплекте запрещено", e.message)
        assertEquals(listOf("cheapestAsGift"), e.errorFields)
    }

    @Test
    fun test_validate_updatePublishedPromo_partnersRemoved_throws() {
        promo.piPublishDate = promoPublicationInPast
        promo.partnersRestriction = listOf(1L)
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_REMOVE, e.code)
        assertEquals("Ограничение на партнеров не может быть удалено после публикации в ПИ", e.message)
        assertEquals(listOf("partnersRestriction"), e.errorFields)
    }

    @Test
    fun test_validate_updatePublishedPromo_partnersAddedToEmptyList_throws() {
        promo.piPublishDate = promoPublicationInPast
        oldPromo.partnersRestriction = listOf()
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_ADD, e.code)
        assertEquals(
            "Ограничение на партнеров не может быть добавлено, если оно было пустым после публикации в ПИ",
            e.message
        )
        assertEquals(listOf("partnersRestriction"), e.errorFields)
    }

    @Test
    fun test_validate_updatePublishedPromo_vendorsRemoved_throws() {
        promo.piPublishDate = promoPublicationInPast
        promo.vendorsRestriction = listOf(1L)
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_REMOVE, e.code)
        assertEquals("Ограничение на вендоров не может быть удалено после публикации в ПИ", e.message)
        assertEquals(listOf("vendorsRestriction"), e.errorFields)
    }

    @Test
    fun test_validate_updatePublishedPromo_vendorsAddedToEmptyList_throws() {
        promo.piPublishDate = promoPublicationInPast
        oldPromo.vendorsRestriction = listOf()
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_ADD, e.code)
        assertEquals(
            "Ограничение на вендоров не может быть добавлено, если оно было пустым после публикации в ПИ",
            e.message
        )
        assertEquals(listOf("vendorsRestriction"), e.errorFields)
    }

    @Test
    fun test_validate_updatePublishedPromo_warehouseAddedToEmptyList_throws() {
        promo.piPublishDate = promoPublicationInPast
        oldPromo.warehousesRestriction = listOf()
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_ADD, e.code)
        assertEquals(
            "Ограничение на склады не может быть добавлено, если оно было пустым после публикации в ПИ",
            e.message
        )
        assertEquals(listOf("warehousesRestriction"), e.errorFields)
    }

    @Test
    fun test_validate_updatePublishedPromo_categoriesAddedToEmptyList_throws() {
        promo.piPublishDate = promoPublicationInPast
        oldPromo.categoriesRestriction = listOf()
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_ADD, e.code)
        assertEquals(
            "Ограничение на категории не может быть добавлено, если оно было пустым после публикации в ПИ",
            e.message
        )
        assertEquals(listOf("categoriesRestriction"), e.errorFields)
    }

    @Test
    fun test_validate_updatePublishedPromo_mskusRemoved_throws() {
        promo.piPublishDate = promoPublicationInPast
        promo.mskusRestriction = listOf(1L)
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_REMOVE, e.code)
        assertEquals("Ограничение на msku не может быть удалено после публикации в ПИ", e.message)
        assertEquals(listOf("mskusRestriction"), e.errorFields)
    }

    @Test
    fun test_validate_updatePublishedPromo_mskusAddedToEmptyList_throws() {
        promo.piPublishDate = promoPublicationInPast
        oldPromo.mskusRestriction = listOf()
        val e = assertThrows<ValidationException> { piPromoValidator.validate(promo, oldPromo) }
        assertEquals(ExceptionCode.PROMO_RESTRICTION_ADD, e.code)
        assertEquals(
            "Ограничение на msku не может быть добавлено, если оно было пустым после публикации в ПИ",
            e.message
        )
        assertEquals(listOf("mskusRestriction"), e.errorFields)
    }

    @Test
    fun test_validate_updatePublishedPromo_mskusDuplicates_ok() {
        promo.piPublishDate = promoPublicationInPast
        oldPromo.mskusRestriction = listOf(6L, 7L, 7L)
        assertDoesNotThrow { piPromoValidator.validate(promo, oldPromo) }
    }
}
