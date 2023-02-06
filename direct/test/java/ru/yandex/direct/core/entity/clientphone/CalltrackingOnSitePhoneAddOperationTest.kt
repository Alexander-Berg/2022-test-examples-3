package ru.yandex.direct.core.entity.clientphone

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhone
import ru.yandex.direct.core.entity.trackingphone.model.ClientPhoneType
import ru.yandex.direct.core.entity.trackingphone.model.PhoneNumber
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class CalltrackingOnSitePhoneAddOperationTest {

    @Autowired
    lateinit var clientPhoneRepository: ClientPhoneRepository

    @Autowired
    lateinit var steps: Steps

    lateinit var clientId: ClientId

    @Before
    fun setUp() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
    }

    @Test
    fun apply_success() {
        val clientPhone1 = newClientPhone("+79001111111")
        val secondRedirectPhone = "+79001112222"
        val clientPhone2 = newClientPhone(secondRedirectPhone)
        val clientPhones = listOf(clientPhone1, clientPhone2)

        val telephonyPhoneService = mock(TelephonyPhoneService::class.java)
        // Для первого вызова вернём непустое значение
        val telephonyServiceId = "16e00b9e-c22e-4cc6-a30c-0ca229950a47"
        Mockito.`when`(telephonyPhoneService.attachTelephony(any(), any(), any()))
                .thenReturn(TelephonyPhoneValue(telephonyServiceId, "+79990009988"))
        // Для второго -- пустое. Эмулируем исчерпание пула
        Mockito.`when`(telephonyPhoneService.attachTelephony(any(), any(), eq(secondRedirectPhone))).thenReturn(null)

        val operation = CalltrackingOnSitePhoneAddOperation(
                clientId,
                clientPhones,
                clientPhoneRepository,
                telephonyPhoneService
        )
        val result = operation.prepareAndApply()

        // Ожидаем, что запишем в БД успешно выданный подменник
        assertThat(result.isSuccessful).describedAs("result.isSuccessful").isTrue()
        val firstResult = result[0]
        assertThat(firstResult.isSuccessful).isTrue()
        val newPhoneId = firstResult.result
        val phoneFromDb = clientPhoneRepository.getByPhoneIds(clientId, listOf(newPhoneId))[0]
        assertThat(phoneFromDb.telephonyServiceId).isEqualTo(telephonyServiceId)

        // для неуспешного номера ожидаем ошибку
        assertThat(result.validationResult).`is`(matchedBy(hasDefectDefinitionWith<Any>(validationError(
                path(index(1), field(ClientPhone.TELEPHONY_SERVICE_ID)),
                notNull()
        ))))
    }

    @Test
    fun apply_noException_whenNoTelephonyPhones() {
        val clientPhone = newClientPhone("+79001112233")
        val clientPhones = listOf(clientPhone)
        val telephonyPhoneService = mock(TelephonyPhoneService::class.java)
        // Эмулируем неуспешную привязку: возращаем null
        Mockito.`when`(telephonyPhoneService.attachTelephony(any(), any(), any())).thenReturn(null)

        val operation = CalltrackingOnSitePhoneAddOperation(
                clientId,
                clientPhones,
                clientPhoneRepository,
                telephonyPhoneService
        )
        val result = operation.prepareAndApply()

        assertThat(result.isSuccessful).describedAs("result.isSuccessful").isTrue()
        assertThat(result.validationResult).`is`(matchedBy(
                hasDefectDefinitionWith<Any>(validationError(
                        path(index(0), field(ClientPhone.TELEPHONY_SERVICE_ID)),
                        notNull()
                ))
        ))
    }

    private fun newClientPhone(redirectPhone: String) =
            ClientPhone()
                    .withClientId(clientId)
                    .withPhoneType(ClientPhoneType.TELEPHONY)
                    .withPhoneNumber(PhoneNumber().withPhone(redirectPhone))
                    .withCounterId(1L)
                    .withPermalinkId(0L)
                    .withIsDeleted(false)
}
