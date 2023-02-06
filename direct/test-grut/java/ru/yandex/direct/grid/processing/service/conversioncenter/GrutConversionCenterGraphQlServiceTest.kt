package ru.yandex.direct.grid.processing.service.conversioncenter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.conversionsource.model.ConversionSource
import ru.yandex.direct.core.entity.conversionsource.model.ConversionSourceSettings
import ru.yandex.direct.core.entity.conversionsource.service.ConversionSourceService
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.defaultConversionSourceLink
import ru.yandex.direct.core.testing.data.defaultConversionSourceMetrika
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GrutGridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.goal.GdConversionActionValueType
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.metrika.client.model.response.CounterGoal
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect
import java.util.Collections.singletonList

private const val QUERY_CONVERSION_SOURCES_TEMPLATE = """{
  client(searchBy: {login: "%s"}) {
    conversionSourcesInfo(input: {preset: LAST_365DAYS}) {
      sources {
        id
        name
        type
        processingIsRunning
        errors
        ... on GdMetrikaConversionSource {
          counterId
        }
        ... on GdLinkConversionSource {
          counterId
          updateFileReminder
          updateFileReminderDaysCount
          settings {
            url
          }
          info {
            updatedSecAgo
            matchingRatio
          }
        }
        conversionActions {
          name
          value {
            type
            ... on GdConversionActionValueFixed {
              cost
            }
            ... on GdConversionActionValueDynamic {
              costFrom
              costTo
            }
          }
          ... on GdMetrikaConversionAction {
            goal {
              id
            }
          }
          ... on GdExternalConversionAction {
            info {
              updatedSecAgo
              matchingRatio
            }
            externalActionGoal: goal {
              id
            }
          }
        }
      }
    }
  }
}
"""

@GrutGridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class GrutConversionCenterGraphQlServiceTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var conversionSourceService: ConversionSourceService

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    private lateinit var context: GridGraphQLContext
    private lateinit var operator: User

    @Before
    fun before() {
        operator = createOrdinaryUser()
        context = ContextHelper.buildContext(operator)
    }

    @Test
    fun getConversionSourcesInfo_DefaultOperator_Empty() {
        val result = graphQlGetConversionSourcesInfo(context, operator.login)

        assertThat(result).isEmpty()
    }

    @Test
    fun getConversionSourcesInfo_MetrikaConversionSource() {
        val metrikaConversionSource = defaultConversionSourceMetrika(operator.clientId)
        prepareMetrikaClientStub(metrikaConversionSource)
        val id = conversionSourceService.add(operator.clientId, singletonList(metrikaConversionSource)).result[0].result

        val result = graphQlGetConversionSourcesInfo(context, operator.login)

        assertThat(result).contains(graphQlResponseFromMetrikaConversionSource(metrikaConversionSource.copy(id = id)))
    }

    @Test
    fun getConversionSourcesInfo_LinkConversionSource() {
        val source = defaultConversionSourceLink(operator.clientId)
        prepareMetrikaClientStub(source)
        val id = conversionSourceService.add(operator.clientId, listOf(source)).result[0].result

        val result = graphQlGetConversionSourcesInfo(context, operator.login)

        assertThat(result).contains(
            graphQlResponseFromExternalConversionSource(source.copy(id = id)) + mapOf(
                "settings" to mapOf(
                    "url" to (source.settings as ConversionSourceSettings.Link).url
                )
            )
        )
    }

    private fun prepareMetrikaClientStub(source: ConversionSource) {
        val counterId = source.counterId
        val domain = source.settings.let {
            if (it is ConversionSourceSettings.Metrika) it.domain else "some-domain.com"
        }

        metrikaClientStub.addUserCounter(
            operator.uid,
            CounterInfoDirect().withId(counterId.toInt()).withSitePath(domain)
        )
        source.actions.filter { it.goalId != null }
            .forEach { action ->
                metrikaClientStub.addCounterGoal(
                    counterId.toInt(),
                    CounterGoal().withId(action.goalId!!.toInt()).withName(action.name)
                )
            }
    }

    private fun graphQlGetConversionSourcesInfo(context: GridGraphQLContext, login: String): List<*> {
        val query = String.format(QUERY_CONVERSION_SOURCES_TEMPLATE, login)

        val response = processor.processQuery(null, query, null, context)
        GraphQLUtils.logErrors(response.errors)
        assertThat(response.errors).isEmpty()
        val data: Map<String, Any> = response.getData()
        return ((data["client"] as Map<*, *>)["conversionSourcesInfo"] as Map<*, *>)["sources"] as List<*>
    }

    private fun createOrdinaryUser() = steps.clientSteps().createDefaultClient().chiefUserInfo!!.user!!

    private fun graphQlResponseFromMetrikaConversionSource(source: ConversionSource): Map<*, *> {
        return mapOf(
            "id" to source.id.toString(),
            "name" to source.name,
            "type" to source.typeCode.toGrid().toString(),
            "conversionActions" to source.actions.map { action ->
                mapOf(
                    "goal" to mapOf("id" to action.goalId),
                    "name" to action.name,
                    "value" to mapOf("type" to GdConversionActionValueType.NOT_SET.toString())
                )
            },
            "counterId" to source.counterId,
            "processingIsRunning" to false,
            "errors" to emptyList<String>()
        )
    }

    private fun graphQlResponseFromExternalConversionSource(source: ConversionSource): Map<*, *> {
        return mapOf(
            "id" to source.id.toString(),
            "name" to source.name,
            "type" to source.typeCode.toGrid().toString(),
            "conversionActions" to source.actions.map { action ->
                mapOf(
                    "externalActionGoal" to action.goalId?.let { mapOf("id" to action.goalId) },
                    "name" to action.name,
                    "value" to mapOf("type" to GdConversionActionValueType.NOT_SET.toString()),
                    "info" to mapOf(
                        "matchingRatio" to 1.0,
                        "updatedSecAgo" to 0L,
                    ),
                )
            },
            "counterId" to source.counterId,
            "processingIsRunning" to false,
            "errors" to emptyList<String>(),
            "updateFileReminder" to false,
            "updateFileReminderDaysCount" to null,
            "info" to null,
        )
    }
}
