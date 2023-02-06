package ru.yandex.direct.api.v5.entity.campaigns.converter

import com.yandex.direct.api.v5.campaigns.CampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.CpmBannerCampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.DynamicTextCampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.MobileAppCampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.SmartCampaignFieldEnum
import com.yandex.direct.api.v5.campaigns.TextCampaignFieldEnum
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.api.v5.entity.campaigns.container.toAnyFieldEnum

@RunWith(JUnitParamsRunner::class)
class CampaignAnyFieldEnumConversionTest {

    @Test
    @TestCaseName("{0}")
    @Parameters(source = CampaignFieldEnum::class)
    fun `every value from FieldNames is supported`(
        value: CampaignFieldEnum,
    ) {
        val enumWrapper = value.toAnyFieldEnum()
        assertThat(enumWrapper.apiValue).isEqualTo(value)
        assertThat(enumWrapper.name).isEqualTo(value.name)
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(source = CpmBannerCampaignFieldEnum::class)
    fun `every value from CpmBannerCampaignFieldNames is supported`(
        value: CpmBannerCampaignFieldEnum,
    ) {
        val enumWrapper = value.toAnyFieldEnum()
        assertThat(enumWrapper.apiValue).isEqualTo(value)
        assertThat(enumWrapper.name).isEqualTo("CPM_BANNER_CAMPAIGN_${value.name}")
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(source = DynamicTextCampaignFieldEnum::class)
    fun `every value from DynamicTextCampaignFieldNames is supported`(
        value: DynamicTextCampaignFieldEnum,
    ) {
        val enumWrapper = value.toAnyFieldEnum()
        assertThat(enumWrapper.apiValue).isEqualTo(value)
        assertThat(enumWrapper.name).isEqualTo("DYNAMIC_TEXT_CAMPAIGN_${value.name}")
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(source = MobileAppCampaignFieldEnum::class)
    fun `every value from MobileAppCampaignFieldNames is supported`(
        value: MobileAppCampaignFieldEnum,
    ) {
        val enumWrapper = value.toAnyFieldEnum()
        assertThat(enumWrapper.apiValue).isEqualTo(value)
        assertThat(enumWrapper.name).isEqualTo("MOBILE_APP_CAMPAIGN_${value.name}")
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(source = SmartCampaignFieldEnum::class)
    fun `every value from SmartCampaignFieldNames is supported`(
        value: SmartCampaignFieldEnum,
    ) {
        val enumWrapper = value.toAnyFieldEnum()
        assertThat(enumWrapper.apiValue).isEqualTo(value)
        assertThat(enumWrapper.name).isEqualTo("SMART_CAMPAIGN_${value.name}")
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(source = TextCampaignFieldEnum::class)
    fun `every value from TextCampaignFieldNames is supported`(
        value: TextCampaignFieldEnum,
    ) {
        val enumWrapper = value.toAnyFieldEnum()
        assertThat(enumWrapper.apiValue).isEqualTo(value)
        assertThat(enumWrapper.name).isEqualTo("TEXT_CAMPAIGN_${value.name}")
    }
}
