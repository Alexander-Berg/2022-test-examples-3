package ru.yandex.direct.core.entity.clientphone

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.RandomNumberUtils
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class ClientPhoneDeleteOperationTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var clientPhoneRepository: ClientPhoneRepository

    private lateinit var clientInfo: ClientInfo

    private lateinit var clientId: ClientId

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
    }

    @Test
    fun validate_success() {
        val clientPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId)
        val clientPhoneIds = listOf(clientPhone.id)

        val vr = validateInDeleteOperation(clientPhoneIds)
        assertThat(vr).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun validate_notValidId_failure() {
        val clientPhoneIds = listOf(-1L)

        val vr = validateInDeleteOperation(clientPhoneIds)

        val expectedError = validationError(path(index(0)), CommonDefects.validId())
        assertThat(vr).`is`(matchedBy(hasDefectWithDefinition<Any>(expectedError)))
    }

    @Test
    fun validate_nonExistentId_failure() {
        val clientPhoneIds = listOf(RandomNumberUtils.nextPositiveLong())

        val vr = validateInDeleteOperation(clientPhoneIds)

        val expectedError = validationError(path(index(0)), CommonDefects.objectNotFound())
        assertThat(vr).`is`(matchedBy(hasDefectWithDefinition<Any>(expectedError)))
    }

    @Test
    fun validate_otherClientPhoneId_failure() {
        val otherClientId = steps.clientSteps().createDefaultClient().clientId
        val otherClientPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(otherClientId)
        val clientPhoneIds = listOf(otherClientPhone.id)

        val vr = validateInDeleteOperation(clientPhoneIds)

        val expectedError = validationError(path(index(0)), CommonDefects.objectNotFound())
        assertThat(vr).`is`(matchedBy(hasDefectWithDefinition<Any>(expectedError)))
    }

    @Test
    fun validate_notManualPhone_failure() {
        val clientPhone = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId)
        val clientPhoneIds = listOf(clientPhone.id)

        val vr = validateInDeleteOperation(clientPhoneIds)

        val expectedError = validationError(path(index(0)), CommonDefects.inconsistentState())
        assertThat(vr).`is`(matchedBy(hasDefectWithDefinition<Any>(expectedError)))
    }

    private fun validateInDeleteOperation(phoneIds: List<Long>): ValidationResult<MutableList<Long>, Defect<Any>> {
        val operation = ClientPhoneDeleteOperation(
            clientId,
            clientInfo.shard,
            phoneIds,
            clientPhoneRepository,
            null
        )
        return operation.validate(phoneIds)
    }

}
