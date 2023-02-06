package ru.yandex.market.pricingmgmt.service.promo.hack

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.reset
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest
import ru.yandex.market.pricingmgmt.TestUtils.any
import ru.yandex.market.pricingmgmt.config.security.passport.PassportAuthenticationToken
import ru.yandex.market.pricingmgmt.model.postgres.User
import ru.yandex.market.pricingmgmt.model.promo.hack.HackPromo

@DbUnitDataBaseConfig(
    DbUnitDataBaseConfig.Entry(
        name = "datatypeFactory",
        value = "ru.yandex.market.pricingmgmt.pg.ExtendedPostgresqlDataTypeFactory"
    )
)
internal class HackPromoServiceTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var hackPromoService: HackPromoService

    @MockBean
    private lateinit var hackPromoJournalsService: HackPromoJournalsService

    companion object {
        const val DEPT_PROMO_ID = 1L
        const val LIST_ID = 1L
        const val LIST_NAME = "Детские вещи"
        const val UPLOADER_NAME = "localDeveloper"
    }

    @BeforeEach
    fun setUp() {
        // Задаем авторизованного пользователя
        val securityContext = SecurityContextImpl()
        securityContext.authentication = PassportAuthenticationToken(User(1, "localDeveloper"), null, listOf())
        SecurityContextHolder.setContext(securityContext)

        reset(hackPromoJournalsService)
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoServiceTest.importList.before.csv"],
        after = ["HackPromoServiceTest.importList.after.csv"]
    )
    fun importList_ok() {
        hackPromoService.importList(
            DEPT_PROMO_ID,
            LIST_NAME,
            UPLOADER_NAME,
            getFile("/xlsx-template/hack-list.xlsx").inputStream
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoServiceTest.importList.before.csv"],
        after = ["HackPromoServiceTest.importList.before.csv"]
    )
    fun importList_rollbackTransactionOnError() {
        `when`(
            hackPromoJournalsService.createPriceJournalsForHackListPromo(
                any(HackPromo::class.java),
                any(Long::class.java)
            )
        ).thenThrow(RuntimeException("unexpected error"))

        assertThrows(RuntimeException::class.java) {
            hackPromoService.importList(
                DEPT_PROMO_ID,
                LIST_NAME,
                UPLOADER_NAME,
                getFile("/xlsx-template/hack-list.xlsx").inputStream
            )
        }
    }

    @Test
    @DbUnitDataSet(
        before = ["HackPromoServiceTest.reimportList.before.csv"],
        after = ["HackPromoServiceTest.reimportList.after.csv"]
    )
    fun reimportList_ok() {
        hackPromoService.reimportList(
            LIST_ID,
            UPLOADER_NAME,
            "обновление цен",
            getFile("/xlsx-template/hack-list-reUpload.xlsx").inputStream
        )
    }

    private fun getFile(fileName: String): MockMultipartFile {
        val bytes = javaClass.getResourceAsStream(fileName)?.readAllBytes()
        return MockMultipartFile("excelFile", fileName, "application/vnd.ms-excel", bytes)
    }
}
