package ru.yandex.direct.grid.processing.service.banner

import com.google.common.base.Preconditions
import org.assertj.core.api.Assertions
import org.hamcrest.Matcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher
import ru.yandex.direct.core.entity.user.repository.UserRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.api.GdApiResponse
import ru.yandex.direct.grid.processing.model.api.GdDefect
import ru.yandex.direct.grid.processing.model.api.GdValidationResult
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAdsMassAction
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAdsMassActionPayload
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers
import ru.yandex.direct.test.utils.assertj.Conditions

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class AdGraphQlServiceRemoderateTest {

    private companion object {
        private const val MUTATION_NAME = "remoderateAds"

        private val QUERY_TEMPLATE = """
           mutation {
              ${MUTATION_NAME}(input: %s) {
                processedAdIds
                skippedAdIds
                successCount
                totalCount
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

    private lateinit var adIdsToRemoderate: List<Long>


    fun <T : GdApiResponse> T.hasError(matcher: Matcher<GdDefect>) = Assert.assertThat(validationResult, GridValidationMatchers.hasErrorsWith(matcher))

    @Before
    fun setUp() {
        val ad = steps.bannerSteps().createActiveTextBanner()
        val clientInfo = ad.clientInfo!!
        clientId = clientInfo.clientId!!
        val user = userRepository.fetchByUids(clientInfo.shard, listOf(clientInfo.uid)).first()
        TestAuthHelper.setDirectAuthentication(user)
        context = ContextHelper.buildContext(user)
        adIdsToRemoderate = listOf(ad.bannerId)
        steps.featureSteps().addClientFeature(clientId, FeatureName.CLIENT_ALLOWED_TO_REMODERATE, true)
    }

    @Test
    fun testRemoderateAds() {
        val input = GdAdsMassAction().apply { adIds = adIdsToRemoderate }
        val payload = sendRequestAndParsePayload(input)

        val expectedPayload = GdAdsMassActionPayload().apply {
            processedAdIds = emptyList()
            skippedAdIds = adIdsToRemoderate
            successCount = 0
            totalCount = 1
            validationResult = GdValidationResult().apply {
                errors = listOf(
                    GdDefect().apply {
                        code = "BannerDefectIds.Gen.MODERATE_BANNER_IN_GROUP_WITHOUT_SHOW_CONDITIONS"
                        path = "adIds[0]"
                    }
                )
                warnings = emptyList()
            }
        }
        Assertions.assertThat(payload).`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(expectedPayload)))
    }

    private fun sendRequestAndParsePayload(input: GdAdsMassAction): GdAdsMassActionPayload {
        val serializedInput = GraphQlJsonUtils.graphQlSerialize(input)
        val query = QUERY_TEMPLATE.format(serializedInput)
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)
        val data = result.getData<Map<String, Any>>()
        Preconditions.checkState(data.containsKey(MUTATION_NAME))
        return GraphQlJsonUtils.convertValue(data[MUTATION_NAME], GdAdsMassActionPayload::class.java)
    }

}
