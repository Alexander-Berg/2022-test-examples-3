package ru.yandex.direct.grid.processing.service.conversioncenter

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.conversionsource.model.ConversionAction
import ru.yandex.direct.core.entity.conversionsource.model.ConversionSource
import ru.yandex.direct.core.entity.conversionsource.model.ConversionSourceSettings
import ru.yandex.direct.core.entity.conversionsource.model.Destination
import ru.yandex.direct.core.entity.conversionsource.model.ProcessingInfo
import ru.yandex.direct.core.entity.conversionsource.model.ProcessingStatus
import ru.yandex.direct.core.entity.conversionsource.service.ConversionSourceService
import ru.yandex.direct.core.entity.conversionsource.validation.ACTION_NAME_IN_PROGRESS
import ru.yandex.direct.core.entity.conversionsource.validation.ACTION_NAME_PAID
import ru.yandex.direct.core.entity.conversionsourcetype.model.ConversionSourceTypeCode
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.defaultConversionSourceLink
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.uac.GrutSteps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.grid.processing.configuration.GrutGridProcessingTest
import ru.yandex.direct.grid.processing.model.goal.GdConversionSourceSettingsUnion
import ru.yandex.direct.grid.processing.model.goal.GdConversionSourceTypeCode
import ru.yandex.direct.grid.processing.model.goal.GdLinkConversionSourceSettings
import ru.yandex.direct.grid.processing.model.goal.GdUpdateConversionSource
import ru.yandex.direct.grid.processing.model.goal.GdUpdateConversionSourceConversionAction
import ru.yandex.direct.grid.processing.model.goal.GdUpdateConversionSourceConversionActionValue
import ru.yandex.direct.grid.processing.model.goal.GdUpdateConversionSourceConversionActionValueType
import ru.yandex.direct.grid.processing.model.goal.GdUpdateConversionSourcePayload
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.metrika.client.model.response.CounterInfoDirect
import ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher
import java.time.LocalDateTime

private val MUTATION = GraphQlTestExecutor.TemplateMutation(
    "conversionCenterUpdateConversionSource",
    """mutation {
      %s (input: %s) {
        isMetrikaAvailable
      }
    }""",
    GdUpdateConversionSource::class.java, GdUpdateConversionSourcePayload::class.java
)

private const val CONVERSION_SOURCE_NAME1 = "some source name 1"

private const val LINK = "https://example.com/some-path-to-file"

private const val COUNTER_ID = 3234234L

@GrutGridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class GrutConversionCenterGraphQlServiceUpdateConversionSourceTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var grutSteps: GrutSteps

    @Autowired
    private lateinit var graphQlTestExecutor: GraphQlTestExecutor

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    private lateinit var conversionSourceService: ConversionSourceService

    private lateinit var clientInfo: ClientInfo
    private lateinit var operator: User

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        operator = clientInfo.chiefUserInfo!!.user!!
        TestAuthHelper.setDirectAuthentication(operator)
        gridContextProvider.gridContext = ContextHelper.buildContext(operator)
        grutSteps.createClient(clientInfo)
        metrikaClientStub.addUserCounters(
            clientInfo.uid, listOf(
                CounterInfoDirect()
                    .withId(COUNTER_ID.toInt())
                    .withName("some counter name")
                    .withSitePath("some-domain"),
            )
        )
    }

    @Test
    fun conversionCenterUpdateConversionSource_CreateWithValidLink() {
        val input = validUpdateConversionSourceInput()

        val payload = graphQlTestExecutor.doMutationAndGetPayload(MUTATION, input, operator)
        GraphQlTestExecutor.validateResponseSuccessful(payload)

        val conversionSources = conversionSourceService.getByClientId(clientInfo.clientId!!)

        Assertions.assertThat(conversionSources)
            .usingRecursiveComparison()
            .ignoringFields("id", "processingInfo.lastStartTime", "processingInfo.lastScheduledTime")
            .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
            .isEqualTo(
                listOf(
                    ConversionSource(
                        id = null,
                        typeCode = ConversionSourceTypeCode.LINK,
                        clientId = clientInfo.clientId!!,
                        name = CONVERSION_SOURCE_NAME1,
                        settings = ConversionSourceSettings.Link(url = LINK),
                        counterId = COUNTER_ID,
                        actions = listOf(
                            ConversionAction(ACTION_NAME_IN_PROGRESS, goalId = null, value = null),
                            ConversionAction(ACTION_NAME_PAID, goalId = null, value = null),
                        ),
                        updatePeriodHours = 48,
                        destination = Destination.CrmApi(COUNTER_ID, operator.uid),
                        processingInfo = ProcessingInfo(ProcessingStatus.NEW, null, null)
                    )
                )
            )
    }

    @Test
    fun conversionCenterUpdateConversionSource_UpdateWithValidLink() {
        val origConversionSource = defaultConversionSourceLink(clientInfo.clientId!!, COUNTER_ID)
        val origId = conversionSourceService.add(clientInfo.clientId!!, listOf(origConversionSource)).result[0].result

        val input = validUpdateConversionSourceInput().withId(origId)

        val payload = graphQlTestExecutor.doMutationAndGetPayload(MUTATION, input, operator)
        GraphQlTestExecutor.validateResponseSuccessful(payload)

        val conversionSources = conversionSourceService.getByClientId(clientInfo.clientId!!)

        Assertions.assertThat(conversionSources)
            .usingRecursiveComparison()
            .ignoringFields("id", "processingInfo.lastStartTime", "processingInfo.lastScheduledTime")
            .withComparatorForType(LocalDateTimeMatcher.approximatelyTimeComparator(1), LocalDateTime::class.java)
            .isEqualTo(
                listOf(
                    ConversionSource(
                        id = null,
                        typeCode = ConversionSourceTypeCode.LINK,
                        clientId = clientInfo.clientId!!,
                        name = CONVERSION_SOURCE_NAME1,
                        settings = ConversionSourceSettings.Link(url = LINK),
                        counterId = COUNTER_ID,
                        actions = listOf(
                            ConversionAction(ACTION_NAME_IN_PROGRESS, goalId = null, value = null),
                            ConversionAction(ACTION_NAME_PAID, goalId = null, value = null),
                        ),
                        updatePeriodHours = 48,
                        destination = Destination.CrmApi(COUNTER_ID, operator.uid),
                        processingInfo = ProcessingInfo(ProcessingStatus.NEW, null, null)
                    )
                )
            )
    }

    @Test
    fun conversionCenterUpdateConversionSource_InvalidRequest() {
        val input = validUpdateConversionSourceInput()
            .withSettings(GdConversionSourceSettingsUnion()) // without settings

        val payload = graphQlTestExecutor.doMutationAndGetPayload(MUTATION, input, operator)
        GraphQlTestExecutor.validateResponseSuccessful(payload)
    }

    private fun validUpdateConversionSourceInput() = GdUpdateConversionSource()
        .withType(GdConversionSourceTypeCode.LINK)
        .withName(CONVERSION_SOURCE_NAME1)
        .withSettings(
            GdConversionSourceSettingsUnion()
                .withLinkSettings(
                    GdLinkConversionSourceSettings()
                        .withUrl(LINK)
                )
        )
        .withConversionActions(
            listOf(
                GdUpdateConversionSourceConversionAction()
                    .withName(ACTION_NAME_IN_PROGRESS)
                    .withValue(
                        GdUpdateConversionSourceConversionActionValue()
                            .withValueType(GdUpdateConversionSourceConversionActionValueType.NOT_SET)
                    ),
                GdUpdateConversionSourceConversionAction()
                    .withName(ACTION_NAME_PAID)
                    .withValue(
                        GdUpdateConversionSourceConversionActionValue()
                            .withValueType(GdUpdateConversionSourceConversionActionValueType.NOT_SET)
                    ),
            )
        )
        .withCounterId(COUNTER_ID)
        .withUpdateFileReminder(true)
        .withUpdateFileReminderDaysCount(2)
}
