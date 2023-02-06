package ru.yandex.market.replenishment.autoorder.service.tender

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.dto.MaybeSupplierId
import ru.yandex.market.replenishment.autoorder.model.TenderNotification
import ru.yandex.market.replenishment.autoorder.model.TenderNotificationType
import ru.yandex.market.replenishment.autoorder.service.client.Cabinet1PClient
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

@MockBean(Cabinet1PClient::class)
class TenderPartnerServiceTest : FunctionalTest() {

    @Autowired
    private lateinit var cabinet1PClient: Cabinet1PClient

    @Autowired
    private lateinit var tenderPartnerService: TenderPartnerService

    @Test
    @DbUnitDataSet(before = ["getSupplierIdByPartnerId.before.csv"])
    fun getSupplierIdByPartnerId() {
        Assertions.assertEquals(
            tenderPartnerService.getMaybeSupplierIdByPartnerId(10),
            MaybeSupplierId(42, false)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["getSupplierIdByPartnerId_mocked.before.csv"],
        after = ["getSupplierIdByPartnerId.before.csv"]
    )
    fun getSupplierIdByPartnerId_mocked() {
        Mockito.`when`(cabinet1PClient.getRsId(10)).thenReturn("1")
        Assertions.assertEquals(
            tenderPartnerService.getMaybeSupplierIdByPartnerId(10),
            MaybeSupplierId(42, false)
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["sendNotification.before.csv"],
        after = ["sendNotification.before.csv"]
    )
    fun sendNotification() {
        tenderPartnerService.sendNotification(TenderNotificationType.STARTED, 13, 42).join()
        verify(cabinet1PClient).sendNotification(
            eq("supplier@42"),
            eq(
                TenderNotification(
                    "Приглашаем принять участие в запросе на закупку №13",
                    """Добрый день!<br /><br />
Яндекс.Маркет приглашаем вас принять участие в запросе на закупку №13 размещенного в <a href="https://supplier-test.market.yandex.ru/tenders">кабинете
    поставщика</a>.
<br /><br /><br />
<small><font color="gray">
        Данное уведомление сформировано автоматически и не требует ответа.
        <br />
        Вы получили это письмо, так как зарегистрированы в кабинете поставщика Яндекс.Маркет
    </font></small>
"""
                )
            )
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["sendNotification_no_cache.before.csv"],
        after = ["sendNotification.before.csv"]
    )
    fun sendNotification_no_cache() {
        Mockito.`when`(cabinet1PClient.getPartnerId("1")).thenReturn(CompletableFuture.completedFuture(10))
        Mockito.`when`(cabinet1PClient.getSupplierGid(10)).thenReturn(CompletableFuture.completedFuture("supplier@42"))

        tenderPartnerService.sendNotification(TenderNotificationType.STARTED, 13, 42).join()

        verify(cabinet1PClient).sendNotification(eq("supplier@42"), any())
    }

    @Test
    @DbUnitDataSet(
        before = ["sendNotification_no_gid.before.csv"],
        after = ["sendNotification.before.csv"]
    )
    fun sendNotification_no_gid() {
        Mockito.`when`(cabinet1PClient.getSupplierGid(10)).thenReturn(CompletableFuture.completedFuture("supplier@42"))

        tenderPartnerService.sendNotification(TenderNotificationType.STARTED, 13, 42).join()

        verify(cabinet1PClient).sendNotification(eq("supplier@42"), any())
    }

    @Test
    @DbUnitDataSet(before = ["sendNotification_no_cache.before.csv"])
    fun sendNotification_no_partner_id() {
        Mockito.`when`(cabinet1PClient.getPartnerId("1")).thenReturn(CompletableFuture.completedFuture(null))
        Mockito.`when`(cabinet1PClient.getSupplierGid(10)).thenReturn(CompletableFuture.completedFuture("supplier@42"))

        Assertions.assertEquals(
            assertThrows<CompletionException> {
                tenderPartnerService.sendNotification(TenderNotificationType.STARTED, 13, 42).join()
            }.message,
            "ru.yandex.market.replenishment.autoorder.exception.BadRequestException: Cabinet 1P partner with rs_id 1 does not exists"
        )

        verify(cabinet1PClient, never()).sendNotification(eq("supplier@42"), any())
    }

    @Test
    @DbUnitDataSet(before = ["sendNotification_no_cache.before.csv"])
    fun sendNotification_no_partner_gid() {
        Mockito.`when`(cabinet1PClient.getPartnerId("1")).thenReturn(CompletableFuture.completedFuture(10))
        Mockito.`when`(cabinet1PClient.getSupplierGid(10)).thenReturn(CompletableFuture.completedFuture(null))

        Assertions.assertEquals(
            assertThrows<CompletionException> {
                tenderPartnerService.sendNotification(TenderNotificationType.STARTED, 13, 42).join()
            }.message,
            "ru.yandex.market.replenishment.autoorder.exception.BadRequestException: Cabinet 1P supplier with partnerId 10 does not exists"
        )

        verify(cabinet1PClient, never()).sendNotification(eq("supplier@42"), any())
    }
}
