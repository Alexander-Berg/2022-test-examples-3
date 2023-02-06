@file:Suppress("UNCHECKED_CAST")

package ru.yandex.market.integration.npd.service

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.retrofit.ExecuteCall
import ru.yandex.market.common.retrofit.RetryStrategy
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.integration.npd.AbstractFunctionalTest
import ru.yandex.market.integration.npd.dbqueue.status.NpdStatusSyncProducer
import ru.yandex.market.integration.npd.repository.PartnerAppNpdRepository
import ru.yandex.market.integration.npd.repository.enums.PartnerAppNpdStatus
import ru.yandex.market.integration.npd.repository.model.PartnerAppNpdModel
import ru.yandex.market.mbi.open.api.client.model.PartnerNpdRequest
import ru.yandex.mj.generated.client.fns_integration_client.model.BindStatusRequest
import ru.yandex.mj.generated.client.fns_integration_client.model.BindStatusResponse
import ru.yandex.mj.generated.client.fns_integration_client.model.BindStatusResponseResponse
import ru.yoomoney.tech.dbqueue.api.EnqueueParams
import java.time.Clock
import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture

class RegistrationServiceImplTest : AbstractFunctionalTest() {
    @Autowired
    lateinit var service: RegistrationServiceImpl

    @Test
    @DbUnitDataSet(
        before = ["Integration.bindStatus.before.csv"],
        after = ["Integration.bindStatus.after.csv"]
    )
    fun testCompletedBindStatus() {
        val expected = BindStatusResponse()
        val response = BindStatusResponseResponse()
        response.resultCode = BindStatusResponseResponse.ResultCodeEnum.COMPLETED
        response.inn = "123456789"
        expected.response = response
        val callMock: ExecuteCall<BindStatusResponse, RetryStrategy> = Mockito.mock(ExecuteCall::class.java)
            as ExecuteCall<BindStatusResponse, RetryStrategy>
        Mockito.`when`(callMock.schedule()).thenReturn(CompletableFuture.completedFuture(expected))
        Mockito.`when`(client.bindStatus(any(BindStatusRequest::class.java))).thenReturn(callMock)
        service.actualizeApplication(333)
    }

    @Test
    @DbUnitDataSet(
        before = ["Integration.bindStatus.before.csv"],
        after = ["Integration.bindStatus.before.csv"]
    )
    fun testInProgressBindStatus() {
        val expected = BindStatusResponse()
        expected.response = BindStatusResponseResponse().resultCode(BindStatusResponseResponse.ResultCodeEnum.IN_PROGRESS)
        val callMock: ExecuteCall<BindStatusResponse, RetryStrategy> = Mockito.mock(ExecuteCall::class.java)
            as ExecuteCall<BindStatusResponse, RetryStrategy>
        Mockito.`when`(callMock.schedule()).thenReturn(CompletableFuture.completedFuture(expected))
        Mockito.`when`(client.bindStatus(any(BindStatusRequest::class.java))).thenReturn(callMock)
        //изменений не было - не должно быть отправки в mbi
        Mockito.`when`(mbiOpenApiClient.saveNpdSelfEmployed(anyLong(), anyLong(), any(PartnerNpdRequest::class.java)))
                            .thenThrow(IllegalStateException())
        service.actualizeApplication(333)
        Mockito.verifyNoMoreInteractions(mbiOpenApiClient)
    }

}
