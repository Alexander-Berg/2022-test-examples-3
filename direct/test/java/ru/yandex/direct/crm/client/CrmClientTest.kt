package ru.yandex.direct.crm.client

import okhttp3.mockwebserver.MockResponse
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import ru.yandex.direct.tvm.TvmIntegration
import ru.yandex.direct.tvm.TvmService


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CrmClientTest {
    @JvmField
    @RegisterExtension
    var mockedCrm = MockedCrmServer()

    lateinit var crmClient: CrmClient

    @BeforeAll
    fun init() {
        val tvmIntegration = mock(TvmIntegration::class.java)
        `when`(tvmIntegration.getTicket(any())).thenReturn("TVM_TICKET")
        `when`(tvmIntegration.isEnabled).thenReturn(true)
        crmClient = mockedCrm.createClient(tvmIntegration, TvmService.CRM_TEST)

        val responseBody = "response body"
        val response =
            MockResponse()
                .addHeader("Content-Type", "plain/text")
                .setBody(responseBody)

        mockedCrm.add("POST:/api/internal/forms/fos/mail:request body", response)
    }

    @Test
    internal fun success_all_parameters() {
        assertThatCode {
            crmClient.sendFosMail(
                login = "userLogin",
                email = "user@email",
                source = "DC3 Commander",
                body = "request body",
                attachmentUrl = "https://s3/url"
            )
        }.doesNotThrowAnyException()
    }

    @Test
    internal fun success_without_attachmentUrl() {
        assertThatCode {
            crmClient.sendFosMail(
                login = "userLogin",
                email = "user@email",
                source = "DC3 Commander",
                body = "request body",
                attachmentUrl = null
            )
        }.doesNotThrowAnyException()
    }
}
