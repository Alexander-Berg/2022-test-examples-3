package ru.yandex.direct.core.copyentity.model

import com.fasterxml.jackson.databind.MapperFeature
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.direct.utils.JsonUtils

class CopyCampaignFlagsSerializationTest {

    private val flags = CopyCampaignFlags(
        isCopyStopped = true,
        isCopyArchived = true,
        isCopyArchivedCampaigns = true,
        isCopyNotificationSettings = true,
        isCopyCampaignStatuses = true,
        isCopyConversionStrategies = true,
        isDoNotCheckRightsToMetrikaGoals = true,
        isCopyBannerStatuses = true,
        isCopyKeywordStatuses = true,
        isStopCopiedCampaigns = true,
        isGenerateReport = true,
    )

    private val serializedFlags: String = "{\"copy_archived\":true,\"copy_archived_campaigns\":true,\"copy_banner_sta" +
        "tuses\":true,\"copy_campaign_statuses\":true,\"copy_conversion_strategies\":true,\"copy_keyword_statuses\":t" +
        "rue,\"copy_notification_settings\":true,\"copy_stopped\":true,\"do_not_check_rights_to_metrika_goals\":true," +
        "\"generate_report\":true,\"stop_copied_campaigns\":true}"

    @Test
    fun testSerialization() {
        val serialized = JsonUtils.MAPPER.copy()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .writeValueAsString(flags)
        assertThat(serialized).isEqualTo(serializedFlags)
    }

    @Test
    fun testDeserialization() {
        val deserialized = JsonUtils.fromJson(serializedFlags, CopyCampaignFlags::class.java)
        assertThat(deserialized).isEqualTo(flags)
    }

}
