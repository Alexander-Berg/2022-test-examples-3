package ru.yandex.direct.api.v5.entity.campaignsext.converter

import com.yandex.direct.api.v5.campaignsext.CampaignFieldEnum
import com.yandex.direct.api.v5.campaignsext.ContentPromotionCampaignFieldEnum
import com.yandex.direct.api.v5.campaignsext.CpmBannerCampaignFieldEnum
import com.yandex.direct.api.v5.campaignsext.DynamicTextCampaignFieldEnum
import com.yandex.direct.api.v5.campaignsext.MobileAppCampaignFieldEnum
import com.yandex.direct.api.v5.campaignsext.SmartCampaignFieldEnum
import com.yandex.direct.api.v5.campaignsext.TextCampaignFieldEnum
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.api.v5.entity.campaigns.container.CampaignAnyFieldEnum
import ru.yandex.direct.api.v5.entity.campaigns.container.toAnyFieldEnum

@RunWith(JUnitParamsRunner::class)
class CampaignExtAnyFieldEnumConversionTest {

    @Test
    @TestCaseName("{0}")
    @Parameters(source = CampaignFieldEnum::class)
    fun `value from FieldNames is supported`(
        value: CampaignFieldEnum,
    ) {
        val enumWrapper = value.toAnyFieldEnum()
        assertThat(enumWrapper.extApiValue).isEqualTo(value)
        assertThat(enumWrapper.name).isEqualTo(value.name)
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(source = ContentPromotionCampaignFieldEnum::class)
    fun `value from ContentPromotionCampaignFieldNames is supported`(
        value: ContentPromotionCampaignFieldEnum,
    ) {
        val enumWrapper = value.toAnyFieldEnum()
        assertThat(enumWrapper.extApiValue).isEqualTo(value)
        assertThat(enumWrapper.name).isEqualTo("CONTENT_PROMOTION_CAMPAIGN_${value.name}")
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(source = CpmBannerCampaignFieldEnum::class)
    fun `value from CpmBannerCampaignFieldNames is supported`(
        value: CpmBannerCampaignFieldEnum,
    ) {
        val enumWrapper = value.toAnyFieldEnum()
        assertThat(enumWrapper.extApiValue).isEqualTo(value)
        assertThat(enumWrapper.name).isEqualTo("CPM_BANNER_CAMPAIGN_${value.name}")
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(source = DynamicTextCampaignFieldEnum::class)
    fun `value from DynamicTextCampaignFieldNames is supported`(
        value: DynamicTextCampaignFieldEnum,
    ) {
        val enumWrapper = value.toAnyFieldEnum()
        assertThat(enumWrapper.extApiValue).isEqualTo(value)
        assertThat(enumWrapper.name).isEqualTo("DYNAMIC_TEXT_CAMPAIGN_${value.name}")
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(source = MobileAppCampaignFieldEnum::class)
    fun `value from MobileAppCampaignFieldNames is supported`(
        value: MobileAppCampaignFieldEnum,
    ) {
        val enumWrapper = value.toAnyFieldEnum()
        assertThat(enumWrapper.extApiValue).isEqualTo(value)
        assertThat(enumWrapper.name).isEqualTo("MOBILE_APP_CAMPAIGN_${value.name}")
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(source = SmartCampaignFieldEnum::class)
    fun `value from SmartCampaignFieldNames is supported`(
        value: SmartCampaignFieldEnum,
    ) {
        val enumWrapper = value.toAnyFieldEnum()
        assertThat(enumWrapper.extApiValue).isEqualTo(value)
        assertThat(enumWrapper.name).isEqualTo("SMART_CAMPAIGN_${value.name}")
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(source = TextCampaignFieldEnum::class)
    fun `value from TextCampaignFieldNames is supported`(
        value: TextCampaignFieldEnum,
    ) {
        val enumWrapper = value.toAnyFieldEnum()
        assertThat(enumWrapper.extApiValue).isEqualTo(value)
        assertThat(enumWrapper.name).isEqualTo("TEXT_CAMPAIGN_${value.name}")
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(source = CampaignAnyFieldEnum::class)
    fun `every field is supported in CampaignsExt Get`(
        anyField: CampaignAnyFieldEnum,
    ) {
        assertThat(anyField.extApiValue).isNotNull
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(source = CampaignAnyFieldEnum::class)
    fun `field supported in Campaigns Get is also supported in CampaignsExt Get`(
        anyField: CampaignAnyFieldEnum,
    ) {
        val field = anyField.apiValue
        val extField = anyField.extApiValue

        if (field != null) {
            assertThat(field.name).isEqualTo(extField.name)
        }
    }
}
