package ru.yandex.direct.grid.processing.service.pricepackage

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestClients
import ru.yandex.direct.core.testing.data.TestPricePackages
import ru.yandex.direct.core.testing.data.TestRegions
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdUpdatePricePackages
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdUpdatePricePackagesItem
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdUpdatePricePackagesPayload
import ru.yandex.direct.grid.processing.model.pricepackage.mutation.GdUpdatePricePackagesPayloadItem
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.pricepackage.converter.PricePackageDataConverter
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer
import ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher
import java.time.ZoneOffset
import javax.annotation.ParametersAreNonnullByDefault

import org.assertj.core.api.Assertions.assertThat
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage
import ru.yandex.direct.grid.processing.configuration.GrutGridProcessingTest

@GrutGridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
@ParametersAreNonnullByDefault
class PricePackageGraphQlServiceUpdatePricePackagesGrutTest {
    private val MUTATION_HANDLE = "updatePricePackages"
    private val MUTATION_TEMPLATE = """mutation {
  %s(input: %s) {
    updatedItems {
      id
    }
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
}"""

    private val GD_MUTATION_PRICE_RETARGETING_CONDITION =
        PricePackageDataConverter.toGdMutationRetargetingCondition(TestPricePackages.DEFAULT_RETARGETING_CONDITION)

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var repository: PricePackageRepository

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var grutApiService: GrutApiService

    private lateinit var clientInfo: ClientInfo
    private lateinit var operator: User

    @Before
    fun initTestData() {
        ppcPropertiesSupport.set(PpcPropertyNames.UPDATE_AUCTION_PRIORITY_IN_GRUT, "true")
        clientInfo = steps.clientSteps().createClient(
            TestClients.defaultClient(RbacRole.SUPER)
                .withCountryRegionId(TestRegions.RUSSIA)
        )
        operator = clientInfo.chiefUserInfo!!.user!!
        TestAuthHelper.setDirectAuthentication(operator)
        steps.pricePackageSteps().clearPricePackages()
        steps.sspPlatformsSteps().addSspPlatforms(TestPricePackages.defaultPricePackage().allowedSsp)
        steps.sspPlatformsSteps().addSspPlatforms(TestPricePackages.anotherPricePackage().allowedSsp)
    }

    @After
    fun afterTest() {
        ppcPropertiesSupport.set(PpcPropertyNames.UPDATE_AUCTION_PRIORITY_IN_GRUT, "false")
        SecurityContextHolder.clearContext()
    }

    @Test
    fun test() {
        val pricePackage = steps.pricePackageSteps().createPricePackage(
            TestPricePackages.defaultPricePackage()
                .withAuctionPriority(10)
        ).pricePackage
        val update = GdUpdatePricePackagesItem()
            .withId(pricePackage.id)
            .withAuctionPriority(20)
            .withLastSeenUpdateTime(pricePackage.lastUpdateTime)
        val payload = updatePricePackagesGraphQl(listOf(update))

        val expectedPricePackage: PricePackage = pricePackage
            .withAuctionPriority(20)
        val actual: Map<Long, PricePackage> = repository.getPricePackages(listOf(pricePackage.id))
        assertThat(actual.size).isEqualTo(1)
        assertThat(actual[pricePackage.id]!!)
            .`is`(
                Conditions.matchedBy(
                    BeanDifferMatcher.beanDiffer(expectedPricePackage)
                        .useCompareStrategy(getDefaultCompareStrategy())
                )
            )
        val expectedPayload = GdUpdatePricePackagesPayload()
            .withUpdatedItems(
                listOf(
                    GdUpdatePricePackagesPayloadItem().withId(pricePackage.id)
                )
            )
        assertThat(payload).`is`(Conditions.matchedBy(BeanDifferMatcher.beanDiffer(expectedPayload)))
        val grutPricePackage = grutApiService.pricePackageGrutApi.getPricePackage(pricePackage.id)
        assertThat(grutPricePackage).isNotNull
        assertThat(grutPricePackage!!.spec.auctionPriority).isEqualTo(20)
    }

    private fun updatePricePackagesGraphQl(items: List<GdUpdatePricePackagesItem>): GdUpdatePricePackagesPayload {
        val request = GdUpdatePricePackages().withUpdateItems(items)
        val query = String.format(
            MUTATION_TEMPLATE,
            MUTATION_HANDLE,
            GraphQlJsonUtils.graphQlSerialize(request)
        )
        val result = processor.processQuery(null, query, null, ContextHelper.buildContext(operator))
        assertThat(result.errors)
            .isEmpty()
        val data = result.getData<Map<String, Any>>()
        assertThat(data).containsOnlyKeys(MUTATION_HANDLE)
        return GraphQlJsonUtils.convertValue(
            data[MUTATION_HANDLE],
            GdUpdatePricePackagesPayload::class.java
        )
    }

    private fun getDefaultCompareStrategy() = DefaultCompareStrategies
        .allFields()
        .forFields(BeanFieldPath.newPath("price")).useDiffer(BigDecimalDiffer())
        .forFields(BeanFieldPath.newPath("eshow")).useDiffer(BigDecimalDiffer())
        .forFields(BeanFieldPath.newPath("lastUpdateTime"))
        .useMatcher(LocalDateTimeMatcher.approximatelyNow(ZoneOffset.UTC))
}
