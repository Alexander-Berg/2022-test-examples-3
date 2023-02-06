package ru.yandex.direct.grid.processing.service.hypergeo

import org.assertj.core.api.SoftAssertions
import org.jooq.Configuration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.hypergeo.model.GeoSegmentType
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoRepository
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.PERIOD_LENGTH
import ru.yandex.direct.core.testing.data.TIMES_QUANTITY
import ru.yandex.direct.core.testing.data.defaultHyperGeo
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegmentDetails
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables.HYPERGEO_SEGMENTS
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.hypergeo.GdGeoSegmentType
import ru.yandex.direct.grid.processing.model.hypergeo.GdGeoSegmentType.REGULAR
import ru.yandex.direct.grid.processing.model.hypergeo.GdHyperGeoRetargetingContainer
import ru.yandex.direct.grid.processing.model.hypergeo.GdHyperGeoRetargetingFilter
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.test.utils.checkEquals

private val QUERY_TEMPLATE = """
        query {
            client(searchBy: {id: %s}) {
                hyperGeo (input: %s) {
                    rowset {
                        adGroupHyperGeo {
                            hyperRegions {
                                hyperGeoSegment {
                                    periodLength
                                    timesQuantity
                                    geoSegmentType
                                }
                            }
                        }
                    }
                }
            }
        }
    """.trimIndent()

private const val SEGMENT_DETAILS_WITHOUT_TYPE = "{" +
    "\"points\": [{\"address\": \"Каширское\", \"latitude\": 55.43, \"longitude\": 37.76}], " +
    "\"radius\": 55, " +
    "\"segment_name\": \"name\"" +
    "}"

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class HyperGeoGraphQlServiceTest {
    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var graphQlTestExecutor: GraphQlTestExecutor

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var hyperGeoRepository: HyperGeoRepository

    private var shard: Int = 1
    private lateinit var operator: User
    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private lateinit var adGroupInfo: AdGroupInfo
    private lateinit var dslConfig: Configuration
    private var adGroupId: Long = 0L

    @Before
    fun before() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup()
        adGroupId = adGroupInfo.adGroupId

        clientInfo = adGroupInfo.clientInfo
        clientId = clientInfo.clientId!!
        operator = clientInfo.chiefUserInfo!!.user!!
        shard = clientInfo.shard

        dslConfig = dslContextProvider.ppc(adGroupInfo.shard).configuration()

        TestAuthHelper.setDirectAuthentication(operator)
    }

    /**
     * Проверка получения гипергео с типом condition, c частотой и периодом посещений
     */
    @Test
    fun getHyperGeo_ConditionType() {
        adGroupInfo.adGroup.hyperGeoId
            .checkEquals(null)

        val hyperGeo = createHyperGeo(
            geoSegmentType = GeoSegmentType.CONDITION,
            periodLength = PERIOD_LENGTH,
            timeQuantity = TIMES_QUANTITY,
        )

        hyperGeoRepository.linkHyperGeosToAdGroups(dslConfig, mapOf(adGroupId to hyperGeo.id))

        val resultData = sendRequest()
        checkResult(resultData, GdGeoSegmentType.CONDITION, PERIOD_LENGTH, TIMES_QUANTITY)
    }

    /**
     * Проверка получения гипергео с типом regular
     */
    @Test
    fun getHyperGeo_RegularType() {
        adGroupInfo.adGroup.hyperGeoId
            .checkEquals(null)

        val hyperGeo = createHyperGeo()

        hyperGeoRepository.linkHyperGeosToAdGroups(dslConfig, mapOf(adGroupId to hyperGeo.id))

        val resultData = sendRequest()
        checkResult(resultData, REGULAR, null, null)
    }

    /**
     * Проверка получения гипергео с типом regular, когда в базе типа нет
     */
    @Test
    fun getHyperGeo_WithoutTypeInDatabase() {
        adGroupInfo.adGroup.hyperGeoId
            .checkEquals(null)

        val hyperGeo = createHyperGeo()

        setHyperGeoSegmentDetailsWithoutType()

        hyperGeoRepository.linkHyperGeosToAdGroups(dslConfig, mapOf(adGroupId to hyperGeo.id))

        val resultData = sendRequest()
        checkResult(resultData, REGULAR, null, null)
    }

    private fun sendRequest(): Map<String, Any> {
        val context = ContextHelper.buildContext(operator).withFetchedFieldsReslover(null)
        val gdHyperGeoRetargetingContainer = GdHyperGeoRetargetingContainer()
            .withFilter(GdHyperGeoRetargetingFilter()
                .withAdGroupIdIn(setOf(adGroupId)))

        val serializedContainer = GraphQlJsonUtils.graphQlSerialize(gdHyperGeoRetargetingContainer)
        val query = String.format(QUERY_TEMPLATE, clientInfo.clientId, serializedContainer)
        gridContextProvider.gridContext = context
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)
        return result.getData()
    }

    private fun checkResult(
        resultData: Map<String, Any>,
        expectGdGeoSegmentType: GdGeoSegmentType,
        expectPeriodLength: Int?,
        expectTimeQuantity: Int?,
    ) {
        val periodLength: Int? = GraphQLUtils.getDataValue<Int?>(resultData,
            "client/hyperGeo/rowset/0/adGroupHyperGeo/hyperRegions/0/hyperGeoSegment/periodLength")
        val timesQuantity: Int? = GraphQLUtils.getDataValue<Int?>(resultData,
            "client/hyperGeo/rowset/0/adGroupHyperGeo/hyperRegions/0/hyperGeoSegment/timesQuantity")
        val geoSegmentTypeName = GraphQLUtils.getDataValue<String>(resultData,
            "client/hyperGeo/rowset/0/adGroupHyperGeo/hyperRegions/0/hyperGeoSegment/geoSegmentType")

        val soft = SoftAssertions()
        if (expectPeriodLength == null) {
            soft.assertThat(periodLength).isNull()
        } else {
            soft.assertThat(periodLength).isEqualTo(expectPeriodLength)
        }
        if (expectTimeQuantity == null) {
            soft.assertThat(timesQuantity).isNull()
        } else {
            soft.assertThat(timesQuantity).isEqualTo(expectTimeQuantity)
        }
        soft.assertThat(geoSegmentTypeName).isEqualTo(expectGdGeoSegmentType.name)
        soft.assertAll()
    }

    private fun createHyperGeo(
        geoSegmentType: GeoSegmentType = GeoSegmentType.REGULAR,
        periodLength: Int? = null,
        timeQuantity: Int? = null,
    ): HyperGeo = steps.hyperGeoSteps().createHyperGeo(
        clientInfo,
        hyperGeo = defaultHyperGeo(
            hyperGeoSegments = listOf(defaultHyperGeoSegment(
                clientId = clientId.asLong(),
                segmentDetails = defaultHyperGeoSegmentDetails(
                    geoSegmentType = geoSegmentType,
                    periodLength = periodLength,
                    timesQuantity = timeQuantity,
                )
            ))
        )
    )

    private fun setHyperGeoSegmentDetailsWithoutType() {
        dslContextProvider.ppc(adGroupInfo.shard)
            .update(HYPERGEO_SEGMENTS)
            .set(HYPERGEO_SEGMENTS.GEOSEGMENT_DETAILS, SEGMENT_DETAILS_WITHOUT_TYPE)
            .where(HYPERGEO_SEGMENTS.CLIENT_ID.eq(clientId.asLong()))
            .execute()
    }
}
