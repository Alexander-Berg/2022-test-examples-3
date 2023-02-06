package ru.yandex.direct.logicprocessor.processors.bsexport.adgroup.resource.handler

import com.nhaarman.mockitokotlin2.mock
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.expression.keywords.KeywordEnum
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledApp
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledAppsAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent
import ru.yandex.direct.core.entity.mobilecontent.model.OsType
import ru.yandex.direct.test.utils.randomPositiveLong

class AdditionalTargetingTypeConfigGeneratorTest {

    private lateinit var testClass: AdditionalTargetingTypeConfigGenerator
    private lateinit var mobileContents: Map<Long, MobileContent>
    private lateinit var mobileInstalledTargeting: MobileInstalledAppsAdGroupAdditionalTargeting
    private lateinit var mobileAppsTargetingToStr: String

    @BeforeEach
    fun initData() {
        testClass = AdditionalTargetingTypeConfigGenerator(mock(), mock())

        val mobileContent = MobileContent()
            .withId(randomPositiveLong())
            .withOsType(OsType.ANDROID)
            .withStoreContentId(RandomStringUtils.random(7))
        mobileContents = mapOf(mobileContent.id to mobileContent)
        mobileAppsTargetingToStr = MobileAppsCommon.mobileAppsTargetingToStr(mobileContent)
        mobileInstalledTargeting = MobileInstalledAppsAdGroupAdditionalTargeting()
            .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
            .withValue(setOf(MobileInstalledApp().withMobileContentId(mobileContent.id)))
    }

    @Test
    fun mobileInstalledAppsFixedKeywordList_With723KeywordTest() {
        val method = testClass.mobileInstalledAppsFixedKeywordList(mobileContents)
        val result = method.invoke(mobileInstalledTargeting)

        val expected = listOf(mobileAppsTargetingToStr to KeywordEnum.ExceptAppsOnCpi, mobileAppsTargetingToStr to KeywordEnum.CryptaAdditionalApps)
        assertThat(result)
            .isEqualTo(expected)
    }
}
