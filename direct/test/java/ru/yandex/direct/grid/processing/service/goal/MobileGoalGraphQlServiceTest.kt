package ru.yandex.direct.grid.processing.service.goal

import graphql.ExecutionResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.repository.UserRepository
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.goal.GdMobileGoalSharingMainRep
import ru.yandex.direct.grid.processing.model.goal.GdMobileGoalSharingPayload
import ru.yandex.direct.grid.processing.model.goal.GdMobileGoalSharingReq
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.processing.util.validation.GridValidationHelper
import ru.yandex.direct.rbac.RbacClientsRelations
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

private const val QUERY_TEMPLATE = """{
  client(searchBy: {login: "%s"}) {
    mobileGoalsSharing {
        login
    }
  }
}
"""

private const val MUTATION_TEMPLATE = """mutation {
  %s (input: %s) {
  	validationResult {
      errors {
        code
        path
        params
      }
    }
  }
}"""

private val ADD_MUTATION = GraphQlTestExecutor.TemplateMutation(
    "addMobileGoalSharing", MUTATION_TEMPLATE,
    GdMobileGoalSharingReq::class.java, GdMobileGoalSharingPayload::class.java
)

private val REMOVE_MUTATION = GraphQlTestExecutor.TemplateMutation(
    "removeMobileGoalSharing", MUTATION_TEMPLATE,
    GdMobileGoalSharingReq::class.java, GdMobileGoalSharingPayload::class.java
)

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class MobileGoalGraphQlServiceTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var rbacClientsRelations: RbacClientsRelations

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var graphQlTestExecutor: GraphQlTestExecutor

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    private lateinit var context: GridGraphQLContext

    private lateinit var operator: User

    private lateinit var ownerClientInfo: ClientInfo
    private lateinit var consumer1ClientInfo: ClientInfo
    private lateinit var consumer2ClientInfo: ClientInfo
    private lateinit var consumer3ClientInfo: ClientInfo

    @Before
    fun before() {
        ownerClientInfo = steps.clientSteps().createDefaultClient()
        consumer1ClientInfo = steps.clientSteps().createDefaultClient()
        consumer2ClientInfo = steps.clientSteps().createDefaultClient()
        consumer3ClientInfo = steps.clientSteps().createDefaultClient()

        rbacClientsRelations.addMobileGoalsAccessRelations(
            listOf(consumer1ClientInfo.clientId, consumer2ClientInfo.clientId), ownerClientInfo.clientId!!
        )

        operator = userRepository.fetchByUids(ownerClientInfo.shard, listOf(ownerClientInfo.uid))[0]
        TestAuthHelper.setDirectAuthentication(operator)
        context = configureTestGridContext(operator, ownerClientInfo.chiefUserInfo!!)
    }

    @Test
    fun getMobileSharingInfo() {
        val query = String.format(QUERY_TEMPLATE, ownerClientInfo.login)
        val result: ExecutionResult = processor.processQuery(null, query, null, context)
        GraphQLUtils.logErrors(result.errors)
        assertThat(result.errors).isEmpty()

        val data: Map<String, Any> = result.getData()
        val payloadRaw = (data["client"] as LinkedHashMap<*, *>)["mobileGoalsSharing"]
        assertThat(payloadRaw)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                listOf(
                    mapOf("login" to consumer1ClientInfo.login),
                    mapOf("login" to consumer2ClientInfo.login),
                )
            )
    }

    @Test
    fun addMobileGoalSharing() {
        val input = GdMobileGoalSharingReq(listOf(GdMobileGoalSharingMainRep(consumer3ClientInfo.login)))
        val payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator)
        GraphQlTestExecutor.validateResponseSuccessful(payload)

        val consumersOfMobileGoals = rbacClientsRelations.getConsumersOfMobileGoals(ownerClientInfo.clientId!!)
        assertThat(consumersOfMobileGoals).containsExactlyInAnyOrder(
            consumer1ClientInfo.clientId, consumer2ClientInfo.clientId, consumer3ClientInfo.clientId,
        )
    }

    @Test
    fun addMobileGoalSharingWhenSharingAlreadyExist() {
        val input = GdMobileGoalSharingReq(listOf(GdMobileGoalSharingMainRep(consumer1ClientInfo.login)))
        val payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator)
        GraphQlTestExecutor.validateResponseSuccessful(payload)

        val consumersOfMobileGoals = rbacClientsRelations.getConsumersOfMobileGoals(ownerClientInfo.clientId!!)
        assertThat(consumersOfMobileGoals).containsExactlyInAnyOrder(
            consumer1ClientInfo.clientId, consumer2ClientInfo.clientId,
        )
    }

    @Test
    fun addMobileGoalSharingWithValidationError() {
        val input = GdMobileGoalSharingReq(
            listOf(
                GdMobileGoalSharingMainRep(consumer3ClientInfo.login),
                GdMobileGoalSharingMainRep("unknownLogin?"),
            )
        )
        val payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator)
        val consumersOfMobileGoals = rbacClientsRelations.getConsumersOfMobileGoals(ownerClientInfo.clientId!!)

        val expectedValidationResult = GridValidationHelper.toGdValidationResult(
            path(field("mainReps"), index(1)), CommonDefects.objectNotFound()
        ).withWarnings(null)

        softly {
            assertThat(payload.validationResult)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(expectedValidationResult)

            assertThat(consumersOfMobileGoals).containsExactlyInAnyOrder(
                consumer1ClientInfo.clientId, consumer2ClientInfo.clientId,
            )
        }

    }

    @Test
    fun removeMobileGoalSharing() {
        val input = GdMobileGoalSharingReq(listOf(GdMobileGoalSharingMainRep(consumer2ClientInfo.login)))
        val payload = graphQlTestExecutor.doMutationAndGetPayload(REMOVE_MUTATION, input, operator)
        GraphQlTestExecutor.validateResponseSuccessful(payload)

        val consumersOfMobileGoals = rbacClientsRelations.getConsumersOfMobileGoals(ownerClientInfo.clientId!!)
        assertThat(consumersOfMobileGoals).containsOnly(
            consumer1ClientInfo.clientId,
        )
    }

    @Test
    fun removeMobileGoalSharingWhenSharingDoNotExist() {
        val input = GdMobileGoalSharingReq(listOf(GdMobileGoalSharingMainRep(consumer3ClientInfo.login)))
        val payload = graphQlTestExecutor.doMutationAndGetPayload(REMOVE_MUTATION, input, operator)
        GraphQlTestExecutor.validateResponseSuccessful(payload)

        val consumersOfMobileGoals = rbacClientsRelations.getConsumersOfMobileGoals(ownerClientInfo.clientId!!)
        assertThat(consumersOfMobileGoals).containsExactlyInAnyOrder(
            consumer1ClientInfo.clientId, consumer2ClientInfo.clientId,
        )
    }

    private fun configureTestGridContext(operator: User, subjectUserInfo: UserInfo): GridGraphQLContext {
        val context = ContextHelper.buildContext(operator, subjectUserInfo.user)
        gridContextProvider.gridContext = context
        return context
    }
}
