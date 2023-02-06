package ru.yandex.market.pricingmgmt.service.promo.validators

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.exception.ExceptionCode
import ru.yandex.market.pricingmgmt.exception.ValidationException
import ru.yandex.market.pricingmgmt.model.postgres.Assortment
import ru.yandex.market.pricingmgmt.model.promo.Compensation
import ru.yandex.market.pricingmgmt.model.promo.CompensationReceiveMethod
import ru.yandex.market.pricingmgmt.model.promo.Promo
import ru.yandex.market.pricingmgmt.model.promo.PromoBudgetOwner
import ru.yandex.market.pricingmgmt.model.promo.PromoKind
import ru.yandex.market.pricingmgmt.model.promo.PromoMechanicsType
import ru.yandex.market.pricingmgmt.model.promo.PromoPurpose
import ru.yandex.market.pricingmgmt.model.promo.PromoStatus
import ru.yandex.market.pricingmgmt.model.promo.SupplierType
import ru.yandex.market.pricingmgmt.model.promo.mechanics.CheapestAsGift
import ru.yandex.market.pricingmgmt.model.promo.mechanics.CompleteSetKind
import ru.yandex.market.pricingmgmt.repository.postgres.AssortmentRepository
import ru.yandex.market.pricingmgmt.service.ManagerService
import java.time.OffsetDateTime

class UpdatePromoValidatorTest : AbstractFunctionalTest() {

    @Autowired
    private val updatePromoValidator: UpdatePromoValidator? = null

    @MockBean
    private val managerService: ManagerService? = null

    @MockBean
    private val assortmentRepository: AssortmentRepository? = null

    @Test
    fun testValidatePromo_validationIsOk() {
        Mockito.doReturn(true).`when`(managerService)?.isUserMarkom("catManager")
        Mockito.doReturn(true).`when`(managerService)?.isUserTrade("tradeManager")
        Mockito.doReturn(HashSet(listOf(Assortment(msku = 123, title = "msku123")))).`when`(assortmentRepository)
            ?.findByMskus(Mockito.anyCollection())
        Mockito.doReturn(HashSet(listOf(Assortment(msku = 124, title = "msku124")))).`when`(assortmentRepository)
            ?.findByOutdatedMskus(Mockito.anyCollection())

        assertDoesNotThrow { updatePromoValidator?.validate(buildNewPromo(), buildOldPromo()) }
    }

    @Test
    fun testValidatePromo_modifiedMechanicsType_throwsException() {
        val newPromo = buildNewPromo()
        newPromo.mechanicsType = PromoMechanicsType.UNKNOWN

        val e = assertThrows<ValidationException> { updatePromoValidator?.validate(newPromo, buildOldPromo()) }

        assertEquals(ExceptionCode.PROMO_UNMODIFIED_FIELD_CHANGED, e.code)
        assertEquals("Поле \"Тип механики\" не может быть изменено.", e.message)
        assertEquals(listOf("mechanicsType"), e.errorFields)
    }

    @Test
    fun testValidatePromo_wrongDateTime_throwsException() {
        val newPromo = buildNewPromo()
        newPromo.endDate = OffsetDateTime.now().minusDays(1).toEpochSecond()

        val e = assertThrows<ValidationException> { updatePromoValidator?.validate(newPromo, buildOldPromo()) }

        assertEquals(ExceptionCode.PROMO_START_DATE_AFTER_END_DATE, e.code)
        assertEquals("Дата окончания промо раньше даты старта", e.message)
        assertEquals(listOf("startDate", "endDate"), e.errorFields)
    }

    @Test
    fun testValidatePromo_wrongStatus_throwsException() {
        val newPromo = buildNewPromo()
        newPromo.status = PromoStatus.UNKNOWN

        val e = assertThrows<ValidationException> { updatePromoValidator?.validate(newPromo, buildOldPromo()) }

        assertEquals(ExceptionCode.PROMO_FIELD_VALUE_INVALID, e.code)
        assertEquals("Значение поля \"Статус\" не распознано", e.message)
        assertEquals(listOf("status"), e.errorFields)
    }

    @Test
    fun testValidatePromo_wrongField_throwsException() {
        val newPromo = buildNewPromo()
        newPromo.compensationSource = Compensation.UNKNOWN

        val e = assertThrows<ValidationException> { updatePromoValidator?.validate(newPromo, buildOldPromo()) }

        assertEquals(ExceptionCode.PROMO_FIELD_VALUE_INVALID, e.code)
        assertEquals("Значение поля \"Источник компенсации\" не распознано", e.message)
        assertEquals(listOf("compensationSource"), e.errorFields)
    }

    @Test
    fun testValidatePromo_unknownCompensationReceiveMethod_throwsException() {

        val newPromo = buildNewPromo()
        newPromo.compensationReceiveMethods =
            listOf(CompensationReceiveMethod.WITHOUT_COMPENSATION, CompensationReceiveMethod.UNKNOWN)

        val e = assertThrows<ValidationException> { updatePromoValidator?.validate(newPromo, buildOldPromo()) }

        assertEquals(ExceptionCode.PROMO_FIELD_VALUE_INVALID, e.code)
        assertEquals("Значение поля \"Способы получения компенсации\" не распознано", e.message)
        assertEquals(listOf("compensationReceiveMethods"), e.errorFields)
    }

    @Test
    fun testValidatePromo_emptyListCompensationReceiveMethod_throwsException() {

        val newPromo = buildNewPromo()
        newPromo.compensationSource = Compensation.PARTNER
        newPromo.compensationReceiveMethods = emptyList()

        val e = assertThrows<ValidationException> { updatePromoValidator?.validate(newPromo, buildOldPromo()) }

        assertEquals(ExceptionCode.PROMO_COMPENSATIONS_RECEIVE_METHODS_EMPTY, e.code)
        assertEquals("Не указаны способы получения компенсации при источнике компенсации \"Партнёр\"", e.message)
        assertEquals(listOf("compensationReceiveMethods"), e.errorFields)
    }

    @Test
    fun testValidatePromo_notEmptyCompensationReceiveMethod_isOk() {

        val newPromo = buildNewPromo()
        newPromo.compensationSource = Compensation.PARTNER
        newPromo.compensationReceiveMethods = listOf(
            CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE,
            CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE
        )
        newPromo.landingUrlAutogenerated = true
        newPromo.rulesUrlAutogenerated = true

        Mockito.doReturn(HashSet(listOf(Assortment(msku = 123, title = "msku123")))).`when`(assortmentRepository)
            ?.findByMskus(Mockito.anyCollection())
        Mockito.doReturn(HashSet(listOf(Assortment(msku = 124, title = "msku124")))).`when`(assortmentRepository)
            ?.findByOutdatedMskus(Mockito.anyCollection())

        assertDoesNotThrow { updatePromoValidator?.validate(newPromo, buildOldPromo()) }
    }

    @Test
    fun testValidatePromo_unknownMsku_throwsException() {
        Mockito.doReturn(true).`when`(managerService)?.isUserMarkom("catManager")
        Mockito.doReturn(true).`when`(managerService)?.isUserTrade("tradeManager")
        Mockito.doReturn(HashSet(listOf(Assortment(msku = 123, title = "msku123")))).`when`(assortmentRepository)
            ?.findByMskus(Mockito.anyCollection())

        val e = assertThrows<ValidationException> { updatePromoValidator?.validate(buildNewPromo(), buildOldPromo()) }

        assertEquals(ExceptionCode.PROMO_RESTRICTION_INTERNAL, e.code)
        assertEquals("MSKU не найдены: 124", e.message)
        assertEquals(listOf("mskusRestriction"), e.errorFields)
    }

    @Test
    fun testValidatePromo_ParentPromoValidator_throwsException() {
        val newPromo = buildNewPromo()
        newPromo.promoKind = PromoKind.NATIONAL
        newPromo.parentPromoId = null

        Mockito.doReturn(HashSet(listOf(Assortment(msku = 123, title = "msku123")))).`when`(assortmentRepository)
            ?.findByMskus(Mockito.anyCollection())
        Mockito.doReturn(HashSet(listOf(Assortment(msku = 124, title = "msku124")))).`when`(assortmentRepository)
            ?.findByOutdatedMskus(Mockito.anyCollection())

        val e = assertThrows<ValidationException> { updatePromoValidator?.validate(newPromo, buildOldPromo()) }

        assertEquals(ExceptionCode.PROMO_PARENT_PROMO_NULL, e.code)
        assertEquals("Не указано родительское промо", e.message)
        assertEquals(listOf("parentPromoId"), e.errorFields)
    }

    private fun buildNewPromo(): Promo {
        return Promo(
            promoId = "123",
            startDate = OffsetDateTime.now().toEpochSecond(),
            endDate = OffsetDateTime.now().plusDays(1).toEpochSecond(),
            cheapestAsGift = CheapestAsGift(CompleteSetKind.SET_OF_3),
            departments = listOf("department1", "departments"),
            promoKind = PromoKind.VENDOR,
            purpose = PromoPurpose.GMV_GENERATION,
            budgetOwner = PromoBudgetOwner.PRODUCT,
            supplierType = SupplierType.THIRD_PARTY,
            compensationSource = Compensation.PARTNER,
            status = PromoStatus.NEW,
            mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT,
            tradeManager = "tradeManager",
            markom = "catManager",
            author = "authorLogin",
            compensationReceiveMethods = listOf(
                CompensationReceiveMethod.WITHOUT_COMPENSATION,
                CompensationReceiveMethod.VENDOR_CABINET_OFF_INVOICE
            ),
            mskusRestriction = listOf(123L, 124L),
            rulesUrlAutogenerated = true,
            landingUrlAutogenerated = true
        )
    }

    private fun buildOldPromo(): Promo {
        return Promo(
            promoId = "123",
            startDate = OffsetDateTime.now().plusDays(10).toEpochSecond(),
            endDate = OffsetDateTime.now().plusDays(15).toEpochSecond(),
            cheapestAsGift = CheapestAsGift(CompleteSetKind.SET_OF_2),
            departments = listOf("department1", "department2"),
            promoKind = PromoKind.CATEGORY,
            purpose = PromoPurpose.CLIENT_ACQUISITION,
            budgetOwner = PromoBudgetOwner.CALL_CENTER,
            supplierType = SupplierType.FIRST_PARTY,
            compensationSource = Compensation.MARKET,
            status = PromoStatus.NEW,
            mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT,
            tradeManager = "oldTradeManager",
            markom = "oldCatManager",
            author = "authorLogin",
            mskusRestriction = listOf(123L)
        )
    }
}
