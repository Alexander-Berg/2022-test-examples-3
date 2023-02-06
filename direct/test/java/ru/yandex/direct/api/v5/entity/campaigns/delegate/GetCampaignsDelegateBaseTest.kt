package ru.yandex.direct.api.v5.entity.campaigns.delegate

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import com.yandex.direct.api.v5.campaigns.GetRequest
import com.yandex.direct.api.v5.campaigns.GetResponse
import com.yandex.direct.api.v5.campaigns.ObjectFactory
import com.yandex.direct.api.v5.campaigns.PriorityGoalsItem
import com.yandex.direct.api.v5.general.YesNoEnum
import org.junit.After
import org.junit.Before
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import ru.yandex.direct.api.v5.entity.GenericApiService
import ru.yandex.direct.api.v5.entity.campaigns.service.CampaignDataFetcher
import ru.yandex.direct.api.v5.entity.campaigns.service.CampaignSumAvailableForTransferCalculator
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource
import ru.yandex.direct.core.entity.campaign.model.MeaningfulGoal
import ru.yandex.direct.core.entity.statistics.service.OrderStatService
import ru.yandex.direct.core.entity.timetarget.model.GeoTimezone
import ru.yandex.direct.core.entity.timetarget.repository.GeoTimezoneRepository
import ru.yandex.direct.core.entity.user.model.ApiUser
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.stub.MetrikaClientStub
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.currency.Money
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter

abstract class GetCampaignsDelegateBaseTest {

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    private lateinit var metrikaClientStub: MetrikaClientStub

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var auth: ApiAuthenticationSource

    @Autowired
    private lateinit var genericApiService: GenericApiService

    @Mock
    private lateinit var orderStatService: OrderStatService

    @Mock
    private lateinit var geoTimezoneRepository: GeoTimezoneRepository

    private lateinit var campaignSumAvailableForTransferCalculator: CampaignSumAvailableForTransferCalculator

    @Autowired
    protected lateinit var delegate: GetCampaignsDelegate

    protected lateinit var clientInfo: ClientInfo

    @Before
    fun prepareTestData() {
        MockitoAnnotations.openMocks(this)

        clientInfo = steps.clientSteps().createDefaultClient()
        metrikaClientStub.addUserCounter(clientInfo.uid, COUNTER_ID.toInt())
        metrikaClientStub.addCounterGoal(
            COUNTER_ID.toInt(),
            VALID_GOAL_ID.toInt()
        )
        steps.sspPlatformsSteps().addSspPlatforms(listOf(SSP_PLATFORM_1, SSP_PLATFORM_2))

        val user = ApiUser()
            .withUid(clientInfo.uid)
            .withClientId(clientInfo.clientId)

        whenever(auth.operator) doReturn user
        whenever(auth.chiefSubclient) doReturn user
        whenever(orderStatService.getCampBsstatForecast(any(), any())).doAnswer {
            val campaignIds = it.getArgument<Collection<Long>>(0)
            val currencyCode = it.getArgument<CurrencyCode>(1)
            campaignIds.associateWith { Money.valueOf(BigDecimal.ZERO, currencyCode) }
        }
        whenever(geoTimezoneRepository.getGeoTimezonesByTimezoneIds(eq(hashSetOf(AMSTERDAM_TIMEZONE.timezoneId)))) doReturn
            listOf(AMSTERDAM_TIMEZONE)

        campaignSumAvailableForTransferCalculator =
            CampaignSumAvailableForTransferCalculator(
                orderStatService = orderStatService,
                campaignService = applicationContext.getBean(),
            )

        val campaignDataFetcher = with(applicationContext) {
            CampaignDataFetcher(
                affectedCampaignIdsContainer = getBean(),
                aggregatedStatusesCampaignService = getBean(),
                auth = auth,
                campOperationQueueRepository = getBean(),
                campaignRepository = getBean(),
                campaignSumAvailableForTransferCalculator = campaignSumAvailableForTransferCalculator,
                campaignTypedRepository = getBean(),
                clientNdsService = getBean(),
                clientService = getBean(),
                featureService = getBean(),
                geoTimezoneRepository = geoTimezoneRepository,
                rbacService = getBean(),
                shardHelper = getBean(),
                userRepository = getBean(),
                userService = getBean(),
            )
        }

        delegate = with(applicationContext) {
            GetCampaignsDelegate(
                apiAuthenticationSource = auth,
                getResponseConverter = getBean(),
                campaignDataFetcher = campaignDataFetcher,
            )
        }
    }

    @After
    fun resetMocks() {
        Mockito.reset(
            auth,
            geoTimezoneRepository,
            orderStatService,
        )
    }

    fun doGetRequest(block: GetRequest.() -> Unit): GetResponse {
        return genericApiService.doAction(delegate, GetRequest().apply(block))
    }

    protected companion object TestData {
        val FACTORY = ObjectFactory()

        val AMSTERDAM_TIMEZONE: GeoTimezone = GeoTimezone()
            .withTimezone(ZoneId.of("Europe/Amsterdam"))
            .withTimezoneId(174L)

        val DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        const val COUNTER_ID = 1L

        const val VALID_GOAL_ID = 55L

        const val SSP_PLATFORM_1 = "Rubicon"

        const val SSP_PLATFORM_2 = "sspplatform.ru"

        /* notification fields */
        const val WARNING_BALANCE = 20

        const val EMAIL = "test@email.com"

        val MEANINGFUL_GOAL = MeaningfulGoal().apply {
            goalId = 12
            conversionValue = 13.toBigDecimal()
        }

        val EXPECTED_PRIORITY_GOAL = PriorityGoalsItem().apply {
            goalId = 12
            value = 13_000_000
            isMetrikaSourceOfValue = YesNoEnum.NO
        }
    }
}
