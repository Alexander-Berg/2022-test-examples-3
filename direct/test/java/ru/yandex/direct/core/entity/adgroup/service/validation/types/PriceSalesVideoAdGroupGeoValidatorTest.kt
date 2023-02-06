package ru.yandex.direct.core.entity.adgroup.service.validation.types

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage
import ru.yandex.direct.core.entity.pricepackage.service.PricePackageService
import ru.yandex.direct.core.entity.pricepackage.service.validation.PricePackageValidator
import ru.yandex.direct.core.entity.region.validation.RegionIdDefects
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestPricePackages
import ru.yandex.direct.core.testing.data.TestRegions.KRASNODAR_KRAI
import ru.yandex.direct.core.testing.data.TestRegions.URYUPINSK_REGION_ID
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.regions.Region.CENTRAL_FEDERAL_DISTRICT_REGION_ID
import ru.yandex.direct.regions.Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID
import ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID
import ru.yandex.direct.regions.Region.SOUTH_FEDERAL_DISTRICT_REGION_ID
import ru.yandex.direct.regions.Region.YAROSLAVL_OBLAST_REGION_ID
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathHelper
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class PriceSalesVideoAdGroupGeoValidatorTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var pricePackageService: PricePackageService

    private fun validateGeo(adGroupGeo: List<Long?>, pricePackage: PricePackage): ValidationResult<List<Long>, Defect<*>> {
        val clientInfo = steps.clientSteps().createDefaultClient()
        steps.pricePackageSteps().createPricePackage(pricePackage)
        val campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage)
        val validator = PriceSalesSpecificAdGroupGeoValidator(pricePackageService.geoTree, campaign, pricePackage,
            null)
        return validator.apply(adGroupGeo)
    }

    @Test
    fun geoItemNull() {
        val geo: MutableList<Long?> = ArrayList()
        geo.add(null)
        val result = validateGeo(geo, southPricePackage())
        assertThat(result).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(PathHelper.path(), RegionIdDefects.geoEmptyRegions()))))
    }

    @Test
    fun `Гео валидное`() {
        val result = validateGeo(listOf(SAINT_PETERSBURG_REGION_ID, KRASNODAR_KRAI),
                southPricePackage())
        assertThat(result).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun `Гео с минус регионом`() {
        val result = validateGeo(listOf(SAINT_PETERSBURG_REGION_ID,
                SOUTH_FEDERAL_DISTRICT_REGION_ID, -KRASNODAR_KRAI),
                southPricePackage())
        assertThat(result).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun `Лишнее гео пытаемся добавить`() {
        val result = validateGeo(listOf(
                SAINT_PETERSBURG_REGION_ID, URYUPINSK_REGION_ID),
                southPricePackage())
        assertThat(result).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(PathHelper.path(), CommonDefects.invalidValue()))))
    }

    @Test
    fun `Не хватает обязательного гео`() {
        val result = validateGeo(listOf(
                SOUTH_FEDERAL_DISTRICT_REGION_ID, KRASNODAR_KRAI),
                southPricePackage())
        assertThat(result).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(PathHelper.path(), CommonDefects.invalidValue()))))
    }

    @Test
    fun `Фиксированные не должны приходить как минус регион`() {
        val result = validateGeo(listOf(
                CENTRAL_FEDERAL_DISTRICT_REGION_ID, -MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID),
                centralPricePackage())
        assertThat(result).`is`(matchedBy(hasDefectDefinitionWith<Any>(
            validationError(PathHelper.path(), CommonDefects.invalidValue()))))
    }

    @Test
    fun `Фиксированные могут не приходить, если есть родительский`() {
        val result = validateGeo(listOf(CENTRAL_FEDERAL_DISTRICT_REGION_ID),
                centralPricePackage())
        assertThat(result).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    private fun southPricePackage(): PricePackage {
        val pricePackage = TestPricePackages.approvedPricePackage()
                .withAvailableAdGroupTypes(setOf(AdGroupType.CPM_VIDEO))
                .withIsFrontpage(false);
        pricePackage.targetingsFixed
                .withGeo(listOf(SAINT_PETERSBURG_REGION_ID))
                .withGeoType(PricePackageValidator.REGION_TYPE_REGION)
        pricePackage.targetingsCustom
                .withGeo(listOf(SOUTH_FEDERAL_DISTRICT_REGION_ID, SAINT_PETERSBURG_REGION_ID, KRASNODAR_KRAI))
                .withGeoType(PricePackageValidator.REGION_TYPE_REGION)
        return pricePackage
    }

    private fun centralPricePackage(): PricePackage {
        val pricePackage = TestPricePackages.approvedPricePackage()
                .withAvailableAdGroupTypes(setOf(AdGroupType.CPM_VIDEO))
                .withIsFrontpage(false)
        pricePackage.targetingsFixed
                .withGeo(listOf(CENTRAL_FEDERAL_DISTRICT_REGION_ID, MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID))
                .withGeoType(PricePackageValidator.REGION_TYPE_REGION)
        pricePackage.targetingsCustom
                .withGeo(listOf(CENTRAL_FEDERAL_DISTRICT_REGION_ID, MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID,
                        YAROSLAVL_OBLAST_REGION_ID))
                .withGeoType(PricePackageValidator.REGION_TYPE_REGION)
        return pricePackage
    }
}
