package ru.yandex.direct.grid.processing.service.trackingphone

import com.google.common.base.Preconditions.checkState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.direct.core.entity.user.repository.UserRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.api.GdDefect
import ru.yandex.direct.grid.processing.model.api.GdValidationResult
import ru.yandex.direct.grid.processing.model.trackingphone.mutation.GdDeleteClientPhone
import ru.yandex.direct.grid.processing.model.trackingphone.mutation.GdDeleteClientPhonePayload
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class TrackingPhoneGraphQLServiceDeleteTest {

    private companion object {
        private const val MUTATION_NAME = "deleteClientPhone"

        private val QUERY_TEMPLATE = """
           mutation {
              ${MUTATION_NAME}(input: %s) {
                deletePhoneIds
                validationResult {
                  errors {
                    code
                    params
                    path
                  }
                  warnings {
                    code
                    params
                    path
                  }
                }
              }
            }
        """.trimIndent()
    }

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var context: GridGraphQLContext

    private lateinit var clientId: ClientId

    @Before
    fun setUp() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        val user = userRepository.fetchByUids(clientInfo.shard, listOf(clientInfo.uid)).first()
        TestAuthHelper.setDirectAuthentication(user)
        context = ContextHelper.buildContext(user)
    }

    @Test
    fun deleteClientPhone() {
        val manualPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(clientId)
        val orgPhone = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientId)
        val otherClientId = steps.clientSteps().createDefaultClient().clientId
        val manualOtherClientPhone = steps.clientPhoneSteps().addDefaultClientManualPhone(otherClientId)
        val clientPhoneIds = listOf(-1L, manualPhone.id, orgPhone.id, manualOtherClientPhone.id)

        val input = GdDeleteClientPhone().apply { phoneIds = clientPhoneIds }
        val payload = sendRequestAndParsePayload(input)

        val expectedPayload = GdDeleteClientPhonePayload().apply {
            deletePhoneIds = listOf(manualPhone.id)
            validationResult = GdValidationResult().apply {
                errors = listOf(
                    GdDefect().apply {
                        code = "DefectIds.MUST_BE_VALID_ID"
                        path = "phoneIds[0]"
                    },
                    GdDefect().apply {
                        code = "DefectIds.INCONSISTENT_STATE"
                        path = "phoneIds[2]"
                    },
                    GdDefect().apply {
                        code = "DefectIds.OBJECT_NOT_FOUND"
                        path = "phoneIds[3]"
                    }
                )
                warnings = emptyList()
            }
        }
        assertThat(payload).`is`(matchedBy(beanDiffer(expectedPayload)))
    }

    private fun sendRequestAndParsePayload(input: GdDeleteClientPhone): GdDeleteClientPhonePayload {
        val serializedInput = GraphQlJsonUtils.graphQlSerialize(input)
        val query = QUERY_TEMPLATE.format(serializedInput)
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)
        val data = result.getData<Map<String, Any>>()
        checkState(data.containsKey(MUTATION_NAME))
        return GraphQlJsonUtils.convertValue(data[MUTATION_NAME], GdDeleteClientPhonePayload::class.java)
    }

}
