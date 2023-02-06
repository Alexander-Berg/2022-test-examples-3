package ru.yandex.direct.core.entity.pricepackage.service

import org.hamcrest.MatcherAssert
import org.junit.Assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.configuration.GrutCoreTest
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage
import ru.yandex.direct.core.entity.pricepackage.repository.PricePackageRepository
import ru.yandex.direct.core.grut.api.PricePackageGrut
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.operation.Applicability
import ru.yandex.direct.testing.matchers.result.MassResultMatcher

@GrutCoreTest
@ExtendWith(SpringExtension::class)
class PricePackageDeleteOperationGrutTest {
    @Autowired
    private lateinit var operationFactory: PricePackageDeleteOperationFactory

    @Autowired
    private lateinit var pricePackageRepository: PricePackageRepository

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @BeforeEach
    fun initTestData() {
        ppcPropertiesSupport.set(PpcPropertyNames.UPDATE_AUCTION_PRIORITY_IN_GRUT, "true")
    }

    @AfterEach
    fun afterTest() {
        ppcPropertiesSupport.set(PpcPropertyNames.UPDATE_AUCTION_PRIORITY_IN_GRUT, "false")
    }

    @Test
    fun notApprovedPackage_success() {
        val notApprovedPricePackage = steps.pricePackageSteps().createNewPricePackage().pricePackage
        grutApiService.pricePackageGrutApi.createOrUpdatePackages(
            listOf(
                PricePackageGrut(
                    id = notApprovedPricePackage.id,
                    auctionPriority = notApprovedPricePackage.auctionPriority
                )
            )
        )
        val operation: PricePackageDeleteOperation = createDeleteOperation(notApprovedPricePackage)
        val result = operation.prepareAndApply()
        MatcherAssert.assertThat(result, MassResultMatcher.isFullySuccessful())
        assertPricePackageDeleted(notApprovedPricePackage.id)
    }

    @Test
    fun notPackageInGrut_success() {
        val notApprovedPricePackage = steps.pricePackageSteps().createNewPricePackage().pricePackage
        val operation: PricePackageDeleteOperation = createDeleteOperation(notApprovedPricePackage)
        val result = operation.prepareAndApply()
        MatcherAssert.assertThat(result, MassResultMatcher.isFullySuccessful())
        assertPricePackageDeleted(notApprovedPricePackage.id)
    }

    private fun createDeleteOperation(pricePackage: PricePackage): PricePackageDeleteOperation {
        return operationFactory.newInstance(Applicability.FULL, listOf(pricePackage.id))
    }

    private fun assertPricePackageDeleted(pricePackageId: Long) {
        val packages = pricePackageRepository.getPricePackages(listOf(pricePackageId))
        val grutPackage = grutApiService.pricePackageGrutApi.getPricePackages(listOf(pricePackageId))
        Assertions.assertTrue(packages.isEmpty())
        Assertions.assertTrue(grutPackage.isEmpty())
    }
}
