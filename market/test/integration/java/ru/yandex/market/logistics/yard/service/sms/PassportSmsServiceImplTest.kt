package ru.yandex.market.logistics.yard.service.sms


import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.apache.http.client.HttpClient
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpUriRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.inside.passport.sms.SmsPassportException
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.config.sms.PassportSmsService


class PassportSmsServiceImplTest(
    @Autowired private val passportSmsService: PassportSmsService,
    @Autowired @Qualifier("passportSmsClient") private val passportSmsClient: HttpClient,
) :
    AbstractSecurityMockedContextualTest() {

    @BeforeEach
    fun before() {
        Mockito.clearInvocations(passportSmsClient)
    }

    @Test
    @DatabaseSetup("classpath:fixtures/service/sms/before.xml")
    @ExpectedDatabase(
        value = "classpath:fixtures/service/sms/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testSendToPhone() {
        val smsId = passportSmsService.sendToPhone("234", "text")
        Mockito.verify(passportSmsClient)
            .execute(Mockito.any(HttpUriRequest::class.java), Mockito.any(ResponseHandler::class.java))
        assertions().assertThat(smsId).isEqualTo("123")
    }

    @Test
    @DatabaseSetup("classpath:fixtures/service/sms/before.xml")
    fun testLimitExceed() {
        assertions().assertThatThrownBy {
            passportSmsService.sendToPhone("123", "text")
        }.isInstanceOf(SmsPassportException::class.java).hasMessageContaining("limit exceeded")

        Mockito.verify(passportSmsClient, Mockito.never())
            .execute(Mockito.any(HttpUriRequest::class.java), Mockito.any(ResponseHandler::class.java))
    }
}
