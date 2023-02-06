package ru.yandex.direct.core.entity.adgroup.service.complex.cpm

import org.assertj.core.api.Assertions
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.adgroup.service.complex.ComplexAdGroupAddOperationFactory
import ru.yandex.direct.core.entity.adgroup.service.complex.cpm.ComplexCpmAdGroupTestData.cpmVideoAdGroupForPriceSales
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects
import ru.yandex.direct.core.entity.bidmodifier.BidModifier
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType
import ru.yandex.direct.core.entity.bidmodifier.OsType
import ru.yandex.direct.core.entity.bidmodifiers.Constants
import ru.yandex.direct.core.entity.bidmodifiers.container.ComplexBidModifierConverter.convertToComplexBidModifier
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestPricePackages
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.regions.GeoTreeFactory
import ru.yandex.direct.result.MassResult
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.validation.result.PathHelper

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class ComplexCpmAddPriceVideoTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var addOperationFactory: ComplexAdGroupAddOperationFactory

    @Autowired
    private lateinit var geoTreeFactory: GeoTreeFactory

    @Autowired
    private lateinit var bidModifierRepository: BidModifierRepository
    private lateinit var clientInfo: ClientInfo
    private lateinit var campaign: CpmPriceCampaign

    //обязательно десктоп и опциональный андроид
    private val BID_MODIFIERS_ANDROID_OPT_DESKTOP_FIXED: List<BidModifier> = listOf(
        BidModifierMobile()
            .withType(BidModifierType.MOBILE_MULTIPLIER)
            .withMobileAdjustment(
                BidModifierMobileAdjustment()
                    .withOsType(OsType.ANDROID)
                    .withIsRequiredInPricePackage(false)
            ),
        BidModifierDesktop()
            .withType(BidModifierType.DESKTOP_MULTIPLIER)
            .withDesktopAdjustment(
                BidModifierDesktopAdjustment()
                    .withIsRequiredInPricePackage(true)
            ),
    )

    //опциональный ios
    private val BID_MODIFIERS_OPT_IOS: List<BidModifier> = listOf(
        BidModifierMobile()
            .withType(BidModifierType.MOBILE_MULTIPLIER)
            .withMobileAdjustment(
                BidModifierMobileAdjustment()
                    .withOsType(OsType.IOS)
                    .withIsRequiredInPricePackage(false)
            )
    )
    private val BID_MODIFIERS_ALL_OPT: List<BidModifier> = listOf(
        BidModifierMobile()
            .withType(BidModifierType.MOBILE_MULTIPLIER)
            .withMobileAdjustment(
                BidModifierMobileAdjustment()
                    .withOsType(null)
                    .withIsRequiredInPricePackage(false)
            ),
        BidModifierMobile()
            .withType(BidModifierType.MOBILE_MULTIPLIER)
            .withMobileAdjustment(
                BidModifierMobileAdjustment()
                    .withOsType(OsType.ANDROID)
                    .withIsRequiredInPricePackage(false)
            ),
        BidModifierMobile()
            .withType(BidModifierType.MOBILE_MULTIPLIER)
            .withMobileAdjustment(
                BidModifierMobileAdjustment()
                    .withOsType(OsType.IOS)
                    .withIsRequiredInPricePackage(false)
            ),
        BidModifierDesktop()
            .withType(BidModifierType.DESKTOP_MULTIPLIER)
            .withDesktopAdjustment(
                BidModifierDesktopAdjustment()
                    .withIsRequiredInPricePackage(false)
            ),
    )
    private val BID_MODIFIERS_FIXED_ANDROID_DESKTOP: List<BidModifier> = listOf(
        BidModifierMobile()
            .withType(BidModifierType.MOBILE_MULTIPLIER)
            .withMobileAdjustment(
                BidModifierMobileAdjustment()
                    .withOsType(OsType.ANDROID)
                    .withIsRequiredInPricePackage(true)
            ),
        BidModifierDesktop()
            .withType(BidModifierType.DESKTOP_MULTIPLIER)
            .withDesktopAdjustment(
                BidModifierDesktopAdjustment()
                    .withIsRequiredInPricePackage(true)
            ),
    )

    @Test
    fun emptyAdGroupBidModifiers_Success() {
        //прайсовое видео. Пустые bidModifier. Проставятся с пакета
        val result = prepareAndApply(BID_MODIFIERS_ANDROID_OPT_DESKTOP_FIXED, emptyList())
        assertThat(result, isFullySuccessful())
        val actualModifiers = getActualModifiers(result.result[0].result)
        //в базу записалось все мобилки минус. И не записался десктоп.
        // IOS не минусуется, так как андроид опциональный
        val mobiles = actualModifiers.filter { it.type == BidModifierType.MOBILE_MULTIPLIER }
        assertThat(actualModifiers, hasSize(1))
        assertThat(mobiles, hasSize(1))
        val mobile = mobiles[0]
        if (mobile is BidModifierMobile) {
            val mobileAdjustment = mobile.mobileAdjustment
            assertSame(0, mobileAdjustment.percent)
            assertNull(mobileAdjustment.osType)
        } else {
            fail()
        }
    }

    @Test
    fun zeroPercentOnAdGroupBidModifiers_Success() {
        //прайсовое видео. Пустые bidModifier. Проставятся с пакета
        val result = prepareAndApply(
            BID_MODIFIERS_ANDROID_OPT_DESKTOP_FIXED, listOf(
                BidModifierMobile()
                    .withType(BidModifierType.MOBILE_MULTIPLIER)
                    .withMobileAdjustment(
                        BidModifierMobileAdjustment()
                            .withPercent(100)
                            .withOsType(OsType.ANDROID)
                    ),
                BidModifierDesktop()
                    .withType(BidModifierType.DESKTOP_MULTIPLIER)
                    .withDesktopAdjustment(BidModifierDesktopAdjustment().withPercent(0))
            )
        )
        assertThat(result, isFullySuccessful())
        val actualModifiers = getActualModifiers(result.result[0].result)
        //в базу записалось IOS.
        assertThat(actualModifiers, hasSize(1))
        val mobiles = actualModifiers.filter { it.type == BidModifierType.MOBILE_MULTIPLIER }

        assertThat(mobiles, hasSize(1))
        val mobile = mobiles[0]
        if (mobile is BidModifierMobile) {
            assertSame(OsType.IOS, mobile.mobileAdjustment.osType)
        }
    }

    @Test
    fun nonEmptyAdGroupBidModifiers_Success() {
        //Опциональноый Android и обязательный desktop
        val result = prepareAndApply(
            BID_MODIFIERS_ANDROID_OPT_DESKTOP_FIXED, listOf(
                BidModifierMobile()
                    .withType(BidModifierType.MOBILE_MULTIPLIER)
                    .withMobileAdjustment(
                        BidModifierMobileAdjustment()
                            .withPercent(100)
                            .withOsType(OsType.ANDROID)
                    )
            )
        )
        assertThat(result, isFullySuccessful())
        val actualModifiers = getActualModifiers(result.result[0].result)
        //в базу записалось IOS.
        assertThat(actualModifiers, hasSize(1))
        val mobiles = actualModifiers.filter { it.type == BidModifierType.MOBILE_MULTIPLIER }

        assertThat(mobiles, hasSize(1))
        val mobile = mobiles[0]
        if (mobile is BidModifierMobile) {
            assertSame(OsType.IOS, mobile.mobileAdjustment.osType)
        }
    }

    @Test
    fun emptyAdGroupAndEmptyPackageBidModifiers_Success() {
        //Опциональноый Android и обязательный desktop
        val result = prepareAndApply(emptyList(), emptyList())
        assertThat(result, isFullySuccessful())
        val actualModifiers = getActualModifiers(result.result[0].result)
        //в базу ничего не запишется
        assertThat(actualModifiers, hasSize(0))
    }

    @Test
    fun allAdGroupAndAllPackageBidModifiers_Success() {
        //Все опциональноый корректировки на пакете и все на группе
        val result = prepareAndApply(
            BID_MODIFIERS_ALL_OPT, listOf(
                BidModifierMobile()
                    .withType(BidModifierType.MOBILE_MULTIPLIER)
                    .withMobileAdjustment(
                        BidModifierMobileAdjustment()
                            .withPercent(100)
                            .withOsType(null)
                    ),
                BidModifierDesktop()
                    .withType(BidModifierType.DESKTOP_MULTIPLIER)
                    .withDesktopAdjustment(BidModifierDesktopAdjustment().withPercent(100))
            )
        )
        assertThat(result, isFullySuccessful())
        val actualModifiers = getActualModifiers(result.result[0].result)
        //в базу ничего не запишется
        assertThat(actualModifiers, hasSize(0))
    }

    @Test
    fun adGroupModifiersNotAllowedOnPackage_ValidationError() {
        val result = prepareAndApply(
            BID_MODIFIERS_OPT_IOS, listOf(
                BidModifierMobile()
                    .withType(BidModifierType.MOBILE_MULTIPLIER)
                    .withMobileAdjustment(
                        BidModifierMobileAdjustment()
                            .withPercent(100)
                            .withOsType(OsType.ANDROID)
                    )
            )
        ).validationResult
        val expectedError = Matchers.validationError(
            PathHelper.path(PathHelper.index(0)),
            AdGroupDefects.cpmPriceAdGroupUseNotAllowedBidModifiers()
        )
        Assertions.assertThat(result).`is`(Conditions.matchedBy(Matchers.hasDefectWithDefinition<Any>(expectedError)))
    }

    @Test
    fun useNotAllowedBidModifiers_ValidationError() {
        var result = prepareAndApply(
            BID_MODIFIERS_FIXED_ANDROID_DESKTOP, listOf(
                BidModifierMobile()
                    .withType(BidModifierType.MOBILE_MULTIPLIER)
                    .withMobileAdjustment(
                        BidModifierMobileAdjustment()
                            .withPercent(100)
                            .withOsType(OsType.IOS)
                    )
            )
        ).validationResult
        val expectedError = Matchers.validationError(
            PathHelper.path(PathHelper.index(0)),
            AdGroupDefects.cpmPriceAdGroupUseNotAllowedBidModifiers()
        )
        Assertions.assertThat(result).`is`(Conditions.matchedBy(Matchers.hasDefectWithDefinition<Any>(expectedError)))
    }

    fun prepareAndApply(
        packageBidModifiers: List<BidModifier>,
        adGroupBidModifiers: List<BidModifier>
    ): MassResult<Long> {
        clientInfo = steps.clientSteps().createDefaultClient()
        val pricePackage = steps.pricePackageSteps().createPricePackage(pricePackage(packageBidModifiers)).pricePackage
        campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage)
        val complexAdGroup: ComplexCpmAdGroup = cpmVideoAdGroupForPriceSales(campaign)
        complexAdGroup.complexBidModifier = convertToComplexBidModifier(adGroupBidModifiers.toList())

        val operation: ComplexCpmAdGroupAddOperation = addOperationFactory.createCpmAdGroupAddOperation(
            true,
            listOf(complexAdGroup),
            geoTreeFactory.globalGeoTree,
            false,
            null,
            clientInfo.uid,
            clientInfo.clientId!!,
            clientInfo.uid,
            true
        )
        return operation.prepareAndApply()
    }

    private fun pricePackage(bidModifiers: List<BidModifier>): PricePackage {
        val pricePackage = TestPricePackages.approvedPricePackage()
            .withAvailableAdGroupTypes(setOf(AdGroupType.CPM_VIDEO))
        pricePackage.bidModifiers = bidModifiers
        return pricePackage
    }

    private fun getActualModifiers(id: Long): List<BidModifier> {
        return bidModifierRepository
            .getByAdGroupIds(
                clientInfo.shard, mapOf(id to campaign.id),
                Constants.ALL_TYPES, setOf(BidModifierLevel.ADGROUP)
            );
    }
}
