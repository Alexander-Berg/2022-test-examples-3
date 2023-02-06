package ru.yandex.direct.core.entity.pricepackage.service.validation

import org.apache.commons.lang3.RandomStringUtils.randomAlphabetic
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_DOMAIN_LENGTH
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects
import ru.yandex.direct.core.entity.client.Constants
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestPricePackages.defaultPricePackage
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.regions.GeoTreeFactory
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CollectionDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult


@CoreTest
@RunWith(SpringRunner::class)
class PricePackageAllowedDomainsValidationTest {
    private lateinit var pricePackage: PricePackage

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    private lateinit var geoTreeFactory: GeoTreeFactory

    @Autowired
    private lateinit var service: PricePackageValidationService

    private lateinit var operator: User

    @Before
    fun setUp() {
        pricePackage = defaultPricePackage()
        steps.sspPlatformsSteps().addSspPlatforms(pricePackage.allowedSsp)
        operator = User().withUid(1L)
    }

    fun validate(): ValidationResult<List<PricePackage>, Defect<*>> {
        return service.validatePricePackages(listOf(pricePackage), geoTreeFactory.globalGeoTree, operator)
    }

    @Test
    fun defaultPackage_success() {
        assertThat(validate()).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun all_null_success() {
        pricePackage.allowedDomains = null
        pricePackage.allowedSsp = null
        assertThat(validate()).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun all_empty_lists_success() {
        pricePackage.allowedDomains = listOf()
        pricePackage.allowedSsp = listOf()
        assertThat(validate()).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun domains1000() {
        pricePackage.allowedDomains = (1..1000).map { randomAlphabetic(10) + ".ru" }
        pricePackage.allowedSsp = listOf()
        assertThat(validate()).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun domains1001() {
        pricePackage.allowedDomains = (1..1001).map { randomAlphabetic(10) + ".ru" }
        pricePackage.allowedSsp = listOf()
        assertThat(validate()).`is`(
            (matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(
                        path(index(0), field(PricePackage.ALLOWED_DOMAINS)),
                        CollectionDefects.maxCollectionSize(Constants.DEFAULT_DISABLED_PLACES_COUNT_LIMIT)
                    )
                )
            ))
        )
    }

    @Test
    fun invalid_ssp() {
        pricePackage.allowedSsp = listOf("invalid")
        assertThat(validate()).`is`(
            (matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(
                        path(index(0), field(PricePackage.ALLOWED_SSP), index(0)),
                        CampaignDefects.unknownSsp("invalid")
                    )
                )
            ))
        )
    }

    @Test
    fun duplicated() {
        pricePackage.allowedDomains = listOf("ya.ru", "ya.ru")
        assertThat(validate()).`is`(
            (matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(
                        path(index(0), field(PricePackage.ALLOWED_DOMAINS)),
                        CampaignDefects.duplicatedStrings(listOf("ya.ru"))
                    )
                )
            ))
        )
    }

    @Test
    fun duplicatedSsp() {
        pricePackage.allowedSsp = listOf("Smaato", "Smaato")
        assertThat(validate()).`is`(
            (matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(
                        path(index(0), field(PricePackage.ALLOWED_SSP)),
                        CampaignDefects.duplicatedStrings(listOf("Smaato"))
                    )
                )
            ))
        )
    }

    @Test
    fun max255() {
        //каждый домен не больше 255 символов
        pricePackage.allowedDomains = listOf("ya.ru", randomAlphabetic(MAX_DOMAIN_LENGTH) + ".ru")
        assertThat(validate()).`is`(
            (matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(
                        path(index(0), field(PricePackage.ALLOWED_DOMAINS), index(1)),
                        CollectionDefects.maxStringLength(MAX_DOMAIN_LENGTH)
                    )
                )
            ))
        )
    }
}
