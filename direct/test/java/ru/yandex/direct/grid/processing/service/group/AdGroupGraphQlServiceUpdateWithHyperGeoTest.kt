package ru.yandex.direct.grid.processing.service.group

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSegment
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.repository.UserRepository
import ru.yandex.direct.core.testing.data.TestRegions
import ru.yandex.direct.core.testing.data.defaultHyperGeo
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroupItem.HYPER_GEO_ID
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroup
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateTextAdGroupItem
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.group.AdGroupMutationService.PATH_FOR_UPDATE_TEXT_AD_GROUP
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.processing.util.validation.GridValidationHelper
import ru.yandex.direct.test.utils.TestUtils.assumeThat
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.validation.defect.CommonDefects.objectNotFound
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

private const val MUTATION_NAME = "updateTextAdGroup"

private val QUERY_TEMPLATE = """
        mutation {
            %s (input: %s) {
                validationResult {
                    errors {
                        code
                        path
                        params
                    }
                }
                updatedAdGroupItems {
                    adGroupId
                }
            }
        }
    """.trimIndent()
private const val ADGROUP_NAME = "group name"

private val UPDATE_MUTATION = GraphQlTestExecutor.TemplateMutation(MUTATION_NAME, QUERY_TEMPLATE,
    GdUpdateTextAdGroup::class.java, GdUpdateAdGroupPayload::class.java)

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class AdGroupGraphQlServiceUpdateWithHyperGeoTest {

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    lateinit var processor: GridGraphQLProcessor

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var adGroupRepository: AdGroupRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var graphQlTestExecutor: GraphQlTestExecutor

    private var shard: Int = 1
    private lateinit var operator: User
    private lateinit var clientId: ClientId
    private lateinit var clientInfo: ClientInfo
    private lateinit var adGroupInfo: AdGroupInfo
    private lateinit var minusKeywords: List<String>
    private lateinit var hyperGeo: HyperGeo
    private lateinit var hyperGeoWithMultipleSegments: HyperGeo

    @Before
    fun before() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup()
        operator = adGroupInfo.clientInfo.chiefUserInfo!!.user!!
        shard = adGroupInfo.shard
        clientId = adGroupInfo.clientId
        clientInfo = adGroupInfo.clientInfo

        TestAuthHelper.setDirectAuthentication(operator)

        hyperGeo = createHyperGeo()
        hyperGeoWithMultipleSegments = createHyperGeo(listOf(defaultHyperGeoSegment(), defaultHyperGeoSegment()))
        minusKeywords = listOf(RandomStringUtils.randomAlphabetic(5))
    }

    /**
     * Проверка обновления группы с гипергео
     */
    @Test
    fun updateTextAdGroups_withHyperGeo() {
        val gdUpdateTextAdGroupItem = createGdUpdateTextAdGroupItem(hyperGeo.id)

        val gdUpdateTextAdGroup = GdUpdateTextAdGroup().withUpdateItems(listOf(gdUpdateTextAdGroupItem))
        val payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, gdUpdateTextAdGroup, operator)
        GraphQlTestExecutor.validateResponseSuccessful(payload)

        assumeThat(payload.updatedAdGroupItems, Matchers.iterableWithSize(1))

        val adGroupId = payload.updatedAdGroupItems[0].adGroupId
        val actualAdGroup = adGroupRepository.getAdGroups(shard, listOf(adGroupId))[0]
        val expectedAdGroup = getExpectedAdGroup(hyperGeo.id)

        assertThat(actualAdGroup)
            .`as`("Группа с гипергео")
            .`is`(matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())))
    }

    /**
     * Проверка что при обновлении группы с не существующим гипергео - не проходим валидацию
     */
    @Test
    fun updateTextAdGroups_withWrongHyperGeo_GetError() {
        val gdUpdateTextAdGroupItem = createGdUpdateTextAdGroupItem(556L)

        val gdUpdateTextAdGroup = GdUpdateTextAdGroup().withUpdateItems(listOf(gdUpdateTextAdGroupItem))
        val payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, gdUpdateTextAdGroup, operator)

        val expectedValidationResult = GridValidationHelper.toGdValidationResult(
            path(field(PATH_FOR_UPDATE_TEXT_AD_GROUP), index(0), field(HYPER_GEO_ID.name())), objectNotFound())
            .withWarnings(null)

        assertThat(payload.validationResult)
            .`as`("Ошибка валидации")
            .`is`(matchedBy(beanDiffer(expectedValidationResult).useCompareStrategy(onlyExpectedFields())))
    }

    /**
     * Проверка что при добавлении новой группы с несколькими сегментами гипергео - група создается
     */
    @Test
    fun updateTextAdGroups_withMultipleSegmentsOfHyperGeoAndWithFeature() {
        val gdUpdateTextAdGroupItem = createGdUpdateTextAdGroupItem(hyperGeoWithMultipleSegments.id)

        val gdUpdateTextAdGroup = GdUpdateTextAdGroup().withUpdateItems(listOf(gdUpdateTextAdGroupItem))
        val payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, gdUpdateTextAdGroup, operator)
        GraphQlTestExecutor.validateResponseSuccessful(payload)

        assumeThat(payload.updatedAdGroupItems, Matchers.iterableWithSize(1))

        val adGroupId = payload.updatedAdGroupItems[0].adGroupId
        val actualAdGroup = adGroupRepository.getAdGroups(shard, listOf(adGroupId))[0]
        val expectedAdGroup = getExpectedAdGroup(hyperGeoWithMultipleSegments.id)

        assertThat(actualAdGroup)
            .`as`("Группа с несколькими сегментами гипергео")
            .`is`(matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())))
    }

    private fun createHyperGeo(
        hyperGeoSegments: List<HyperGeoSegment> = listOf(defaultHyperGeoSegment())
    ): HyperGeo {
        val hyperGeo = defaultHyperGeo(hyperGeoSegments = hyperGeoSegments)
        return steps.hyperGeoSteps().createHyperGeo(clientInfo, hyperGeo)
    }

    private fun createGdUpdateTextAdGroupItem(hyperGeoId: Long): GdUpdateTextAdGroupItem {
        return GdUpdateTextAdGroupItem()
            .withAdGroupName(ADGROUP_NAME)
            .withAdGroupId(adGroupInfo.adGroupId)
            .withRegionIds(listOf(TestRegions.RUSSIA.toInt()))
            .withHyperGeoId(hyperGeoId)
            .withAdGroupMinusKeywords(minusKeywords)
            .withLibraryMinusKeywordsIds(emptyList())
            .withBidModifiers(GdUpdateBidModifiers())
    }

    private fun getExpectedAdGroup(hyperGeoId: Long): TextAdGroup {
        return TextAdGroup()
            .withType(AdGroupType.BASE)
            .withName(ADGROUP_NAME)
            .withId(adGroupInfo.adGroupId)
            .withHyperGeoId(hyperGeoId)
            .withMinusKeywords(minusKeywords)
    }
}
