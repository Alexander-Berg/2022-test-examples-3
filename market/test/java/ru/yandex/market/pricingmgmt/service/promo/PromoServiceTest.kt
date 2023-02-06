package ru.yandex.market.pricingmgmt.service.promo

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.TestUtils.captureNotNull
import ru.yandex.market.pricingmgmt.client.promo.api.PromoApiClient
import ru.yandex.market.pricingmgmt.config.security.passport.PassportAuthenticationToken
import ru.yandex.market.pricingmgmt.exception.ValidationException
import ru.yandex.market.pricingmgmt.exception.ExceptionCode
import ru.yandex.market.pricingmgmt.model.postgres.User
import ru.yandex.market.pricingmgmt.model.promo.Compensation
import ru.yandex.market.pricingmgmt.model.promo.Promo
import ru.yandex.market.pricingmgmt.model.promo.PromoBudgetOwner
import ru.yandex.market.pricingmgmt.model.promo.PromoKind
import ru.yandex.market.pricingmgmt.model.promo.PromoMechanicsType
import ru.yandex.market.pricingmgmt.model.promo.PromoPurpose
import ru.yandex.market.pricingmgmt.model.promo.PromoStatus
import ru.yandex.market.pricingmgmt.model.promo.SupplierType
import ru.yandex.market.pricingmgmt.model.promo.mechanics.CheapestAsGift
import ru.yandex.market.pricingmgmt.model.promo.mechanics.CompleteSetKind
import ru.yandex.market.pricingmgmt.service.ManagerService
import ru.yandex.market.pricingmgmt.service.promo.validators.CreatePromoValidator
import ru.yandex.mj.generated.client.promoservice.model.PromoRequestV2
import java.time.OffsetDateTime

class PromoServiceTest : AbstractFunctionalTest() {

    @MockBean
    private lateinit var promoApiClient: PromoApiClient

    @MockBean
    private lateinit var managerService: ManagerService

    @Autowired
    private lateinit var promoService: PromoService

    @MockBean
    private lateinit var createPromoValidator: CreatePromoValidator

    @Captor
    private lateinit var promoRequestCaptor: ArgumentCaptor<PromoRequestV2>

    @BeforeEach
    fun setUp() {
        // Задаем авторизованного пользователя т.к.
        // при создании акции устанавливается автор - текущий авторизованный пользователь
        val securityContext = SecurityContextImpl()
        securityContext.authentication = PassportAuthenticationToken(User(1, "login"), null, listOf())
        SecurityContextHolder.setContext(securityContext)
    }

    @AfterEach
    fun tearDown() {
        clearInvocations(promoApiClient)
    }

    @Test
    fun generatePromoIdTest() {
        doNothing().`when`(promoApiClient)?.createPromo(notNull())
        doReturn(true).`when`(managerService)?.isUserTrade("tradeManager")
        doReturn(true).`when`(managerService)?.isUserMarkom("catManager")

        promoService.createPromo(buildPromo())

        verify(promoApiClient)?.createPromo(promoRequestCaptor.captureNotNull())
        val capturedPromoRequest = promoRequestCaptor.value

        Assertions.assertEquals("cf_100001", capturedPromoRequest.promoId)
    }

    @Test
    fun generateSeveralPromoIdsTest() {
        doNothing().`when`(promoApiClient)?.createPromo(notNull())
        doReturn(true).`when`(managerService)?.isUserTrade("tradeManager")
        doReturn(true).`when`(managerService)?.isUserMarkom("catManager")

        for (i in 1..10) {
            promoService.createPromo(buildPromo())
        }

        verify(promoApiClient, times(10))?.createPromo(promoRequestCaptor.captureNotNull())
        val capturedPromoRequest = promoRequestCaptor.value

        Assertions.assertEquals("cf_100010", capturedPromoRequest.promoId)
    }

    @Test
    fun doNotGeneratePromoIdOnErrorBeforeSendTest() {
        doNothing().`when`(promoApiClient)?.createPromo(notNull())
        doReturn(true).`when`(managerService)?.isUserTrade("tradeManager")
        doReturn(true).`when`(managerService)?.isUserMarkom("catManager")

        doThrow(
            ValidationException(
                code = ExceptionCode.PROMO_PROMOCODE_CODE_CHANGE,
                message = "Some error"
            )
        ).`when`(createPromoValidator).validate(notNull())
        assertThrows<ValidationException> { promoService.createPromo(buildPromo()) }
        verifyZeroInteractions(promoApiClient)

        doNothing().`when`(createPromoValidator).validate(notNull())
        assertDoesNotThrow { promoService.createPromo(buildPromo()) }
        verify(promoApiClient)?.createPromo(promoRequestCaptor.captureNotNull())
        Assertions.assertEquals("cf_100001", promoRequestCaptor.value?.promoId)
    }

    @Test
    fun generatePromoIdOnErrorOnSendTest() {
        doReturn(true).`when`(managerService)?.isUserTrade("tradeManager")
        doReturn(true).`when`(managerService)?.isUserMarkom("catManager")

        doThrow(
            ValidationException(
                code = ExceptionCode.PROMO_PROMOCODE_CODE_CHANGE,
                message = "Some error"
            )
        ).`when`(promoApiClient).createPromo(notNull())
        assertThrows<ValidationException> { promoService.createPromo(buildPromo()) }
        verify(promoApiClient, times(1))?.createPromo(promoRequestCaptor.captureNotNull())
        Assertions.assertEquals("cf_100001", promoRequestCaptor.value?.promoId)

        promoRequestCaptor.allValues.clear()

        doNothing().`when`(promoApiClient)?.createPromo(notNull())
        assertDoesNotThrow { promoService.createPromo(buildPromo()) }
        verify(promoApiClient, times(2))?.createPromo(promoRequestCaptor.captureNotNull())
        Assertions.assertEquals("cf_100002", promoRequestCaptor.value?.promoId)
    }

    private fun buildPromo(): Promo {
        return Promo(
            mechanicsType = PromoMechanicsType.CHEAPEST_AS_GIFT,
            startDate = OffsetDateTime.now().toEpochSecond(),
            endDate = OffsetDateTime.now().plusHours(1).toEpochSecond(),
            status = PromoStatus.NEW,
            cheapestAsGift = CheapestAsGift(CompleteSetKind.SET_OF_3),
            departments = listOf("department"),
            tradeManager = "tradeManager",
            markom = "catManager",
            promoKind = PromoKind.VENDOR,
            purpose = PromoPurpose.GMV_GENERATION,
            budgetOwner = PromoBudgetOwner.PRODUCT,
            supplierType = SupplierType.THIRD_PARTY,
            compensationSource = Compensation.MARKET,
            landingUrlAutogenerated = true,
            rulesUrlAutogenerated = true,
            warehousesRestriction = listOf(1L)
        )
    }
}
