package ru.yandex.direct.ess.router.rules.feeds.usagetypes

import org.assertj.core.api.Assertions
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.binlog.model.Operation
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusEnum
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason
import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupCounters
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AdGroupStatesEnum
import ru.yandex.direct.core.entity.aggregatedstatuses.adgroup.AggregatedStatusAdGroupData
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.AggregatedStatusCampaignData
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignCounters
import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignStatesEnum
import ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_DYNAMIC
import ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_TEXT
import ru.yandex.direct.dbschema.ppc.Tables.AGGR_STATUSES_ADGROUPS
import ru.yandex.direct.dbschema.ppc.Tables.AGGR_STATUSES_CAMPAIGNS
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType
import ru.yandex.direct.ess.logicobjects.feeds.usagetypes.FeedUsageTypesObject
import ru.yandex.direct.ess.router.configuration.TestConfiguration
import ru.yandex.direct.ess.router.testutils.AdgroupsDynamicTableChange
import ru.yandex.direct.ess.router.testutils.AdgroupsDynamicTableChange.createAdgroupsDynamicEvent
import ru.yandex.direct.ess.router.testutils.AdgroupsTextTableChange
import ru.yandex.direct.ess.router.testutils.AdgroupsTextTableChange.createAdgroupsTextEvent
import ru.yandex.direct.ess.router.testutils.AggrStatusesAdGroupsTableChange
import ru.yandex.direct.ess.router.testutils.AggrStatusesCampaignsTableChange
import ru.yandex.direct.ess.router.testutils.createdAggrStatusesAdGroupsEvent
import ru.yandex.direct.ess.router.testutils.createdAggrStatusesCampaignsEvent
import ru.yandex.direct.utils.JsonUtils
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [TestConfiguration::class])
class RecalculateFeedUsageTypesRuleTest {
    @Autowired
    private lateinit var rule: RecalculateFeedUsageTypesRule

    @Test
    fun testInsertDynamicWithFeedChanges() {
        val change = AdgroupsDynamicTableChange().withPid(ADGROUP_ID)
        change.addInsertedColumn(ADGROUPS_DYNAMIC.FEED_ID, FEED_ID)
        val binlogEvent = createAdgroupsDynamicEvent(listOf(change), Operation.INSERT)
        val actual = rule.mapBinlogEvent(binlogEvent)
        val expected = FeedUsageTypesObject(FEED_ID, CampaignsType.dynamic, null, null, Operation.INSERT)

        SoftAssertions.assertSoftly { softly: SoftAssertions ->
            softly.assertThat(actual).hasSize(1)
            softly.assertThat(actual[0]).isEqualTo(expected).usingRecursiveComparison()
        }
    }

    @Test
    fun testInsertDynamicWithUrlAndNullFeedIdChanges() {
        val change = AdgroupsDynamicTableChange().withPid(ADGROUP_ID)
        change.addInsertedColumn(ADGROUPS_DYNAMIC.FEED_ID, null)
        change.addInsertedColumn(ADGROUPS_DYNAMIC.MAIN_DOMAIN_ID, FEED_ID)
        val binlogEvent = createAdgroupsDynamicEvent(listOf(change), Operation.INSERT)
        val actual = rule.mapBinlogEvent(binlogEvent)
        Assertions.assertThat(actual).hasSize(0)
    }

    @Test
    fun testInsertTextWithFeedIdChanges() {
        val change = AdgroupsTextTableChange().withPid(ADGROUP_ID)
        change.addInsertedColumn(ADGROUPS_TEXT.FEED_ID, null)
        val binlogEvent = createAdgroupsTextEvent(listOf(change), Operation.INSERT)
        val actual = rule.mapBinlogEvent(binlogEvent)
        Assertions.assertThat(actual).hasSize(0)
    }

    @Test
    fun testInsertTextWithFeedChanges() {
        val change = AdgroupsTextTableChange().withPid(ADGROUP_ID)
        change.addInsertedColumn(ADGROUPS_TEXT.FEED_ID, null)
        change.addInsertedColumn(ADGROUPS_DYNAMIC.FEED_ID, FEED_ID)
        val binlogEvent = createAdgroupsTextEvent(listOf(change), Operation.INSERT)
        val actual = rule.mapBinlogEvent(binlogEvent)
        val expected = FeedUsageTypesObject(FEED_ID, CampaignsType.text, null, null, Operation.INSERT)

        SoftAssertions.assertSoftly { softly: SoftAssertions ->
            softly.assertThat(actual).hasSize(1)
            softly.assertThat(actual[0]).isEqualTo(expected).usingRecursiveComparison()
        }
    }

    @Test
    fun testUpdateFeedIdInDynamicChanges() {
        val change = AdgroupsDynamicTableChange().withPid(ADGROUP_ID)
        change.addChangedColumn(ADGROUPS_DYNAMIC.FEED_ID, FEED_ID + 1, FEED_ID)
        val binlogEvent = createAdgroupsDynamicEvent(listOf(change), Operation.UPDATE)
        val actual = rule.mapBinlogEvent(binlogEvent)
        Assertions.assertThat(actual).hasSize(0)
    }

    @Test
    fun testUpdateAggrStatusesAdgroupsNotValuableField() {
        val change = AggrStatusesAdGroupsTableChange(ADGROUP_ID)
        change.addChangedColumn(AGGR_STATUSES_ADGROUPS.UPDATED, LocalDateTime.now().minusDays(1), LocalDateTime.now())
        val binlogEvent = createdAggrStatusesAdGroupsEvent(listOf(change), Operation.UPDATE)
        val actual = rule.mapBinlogEvent(binlogEvent)
        Assertions.assertThat(actual).hasSize(0)
    }

    @Test
    fun testUpdateAggrStatusesAdgroupsNotValuableFieldChange() {
        val change = AggrStatusesAdGroupsTableChange(ADGROUP_ID)
        change.addChangedColumn(
            AGGR_STATUSES_ADGROUPS.AGGR_DATA,
            getAdgroupJson(4, GdSelfStatusEnum.RUN_OK),
            getAdgroupJson(10, GdSelfStatusEnum.RUN_OK)
        )
        val binlogEvent = createdAggrStatusesAdGroupsEvent(listOf(change), Operation.UPDATE)
        val actual = rule.mapBinlogEvent(binlogEvent)
        Assertions.assertThat(actual).hasSize(0)
    }

    @Test
    fun testUpdateAggrStatusesAdgroupsNotValuableStatusChange() {
        val change = AggrStatusesAdGroupsTableChange(ADGROUP_ID)
        change.addChangedColumn(
            AGGR_STATUSES_ADGROUPS.AGGR_DATA,
            getAdgroupJson(4, GdSelfStatusEnum.RUN_OK),
            getAdgroupJson(4, GdSelfStatusEnum.RUN_WARN)
        )
        val binlogEvent = createdAggrStatusesAdGroupsEvent(listOf(change), Operation.UPDATE)
        val actual = rule.mapBinlogEvent(binlogEvent)
        Assertions.assertThat(actual).hasSize(0)
    }

    @Test
    fun testUpdateAggrStatusesAdgroupsValuableStatusChange() {
        val change = AggrStatusesAdGroupsTableChange(ADGROUP_ID)
        change.addChangedColumn(
            AGGR_STATUSES_ADGROUPS.AGGR_DATA,
            getAdgroupJson(4, GdSelfStatusEnum.STOP_OK),
            getAdgroupJson(4, GdSelfStatusEnum.RUN_WARN)
        )
        val binlogEvent = createdAggrStatusesAdGroupsEvent(listOf(change), Operation.UPDATE)
        val actual = rule.mapBinlogEvent(binlogEvent)
        val expected = FeedUsageTypesObject(null, null, null, ADGROUP_ID, Operation.UPDATE)

        SoftAssertions.assertSoftly { softly: SoftAssertions ->
            softly.assertThat(actual).hasSize(1)
            softly.assertThat(actual[0]).isEqualTo(expected).usingRecursiveComparison()
        }
    }

    @Test
    fun testUpdateAggrStatusesCampaignsNotValuableField() {
        val change = AggrStatusesCampaignsTableChange(CAMPAIGN_ID)
        change.addChangedColumn(AGGR_STATUSES_CAMPAIGNS.UPDATED, LocalDateTime.now().minusDays(1), LocalDateTime.now())
        val binlogEvent = createdAggrStatusesCampaignsEvent(listOf(change), Operation.UPDATE)
        val actual = rule.mapBinlogEvent(binlogEvent)
        Assertions.assertThat(actual).hasSize(0)
    }

    @Test
    fun testUpdateAggrStatusesCampaignsNotValuableFieldChange() {
        val change = AggrStatusesCampaignsTableChange(CAMPAIGN_ID)
        change.addChangedColumn(
            AGGR_STATUSES_CAMPAIGNS.AGGR_DATA,
            getCampaignJson(4, GdSelfStatusEnum.RUN_OK),
            getCampaignJson(10, GdSelfStatusEnum.RUN_OK)
        )
        val binlogEvent = createdAggrStatusesCampaignsEvent(listOf(change), Operation.UPDATE)
        val actual = rule.mapBinlogEvent(binlogEvent)
        Assertions.assertThat(actual).hasSize(0)
    }

    @Test
    fun testUpdateAggrStatusesCampaignsNotValuableStatusChange() {
        val change = AggrStatusesCampaignsTableChange(CAMPAIGN_ID)
        change.addChangedColumn(
            AGGR_STATUSES_CAMPAIGNS.AGGR_DATA,
            getCampaignJson(4, GdSelfStatusEnum.RUN_OK),
            getCampaignJson(4, GdSelfStatusEnum.RUN_WARN)
        )
        val binlogEvent = createdAggrStatusesCampaignsEvent(listOf(change), Operation.UPDATE)
        val actual = rule.mapBinlogEvent(binlogEvent)
        Assertions.assertThat(actual).hasSize(0)
    }

    @Test
    fun testUpdateAggrStatusesCampaignsValuableStatusChange() {
        val change = AggrStatusesCampaignsTableChange(CAMPAIGN_ID)
        change.addChangedColumn(
            AGGR_STATUSES_CAMPAIGNS.AGGR_DATA,
            getCampaignJson(4, GdSelfStatusEnum.STOP_OK),
            getCampaignJson(4, GdSelfStatusEnum.RUN_WARN)
        )
        val binlogEvent = createdAggrStatusesCampaignsEvent(listOf(change), Operation.UPDATE)
        val actual = rule.mapBinlogEvent(binlogEvent)
        val expected = FeedUsageTypesObject(null, null, CAMPAIGN_ID, null, Operation.UPDATE)

        SoftAssertions.assertSoftly { softly: SoftAssertions ->
            softly.assertThat(actual).hasSize(1)
            softly.assertThat(actual[0]).isEqualTo(expected).usingRecursiveComparison()
        }
    }

    companion object {
        private const val FEED_ID = 1L
        private const val CAMPAIGN_ID = 2L
        private const val ADGROUP_ID = 3L

        fun getAdgroupJson(countAds: Int, selfStatus: GdSelfStatusEnum): String =
            JsonUtils.toJson(
                AggregatedStatusAdGroupData(
                    listOf(AdGroupStatesEnum.BS_RARELY_SERVED),
                    AdGroupCounters(
                        countAds,
                        14,
                        0,
                        mapOf(GdSelfStatusEnum.RUN_OK to 1),
                        mapOf(AdStatesEnum.ACTIVE_IN_BS to 1),
                        mapOf(GdSelfStatusEnum.RUN_OK to 1),
                        mapOf(),
                        mapOf(),
                        mapOf()
                    ),
                    selfStatus,
                    GdSelfStatusReason.ADGROUP_RARELY_SERVED,
                )
            )

        fun getCampaignJson(countGroups: Int, selfStatus: GdSelfStatusEnum): String =
            JsonUtils.toJson(
                AggregatedStatusCampaignData(
                    listOf(CampaignStatesEnum.PAYED, CampaignStatesEnum.DOMAIN_MONITORED),
                    CampaignCounters(
                        countGroups,
                        mapOf(GdSelfStatusEnum.RUN_WARN to 1),
                        mapOf(AdGroupStatesEnum.BS_RARELY_SERVED to 1)
                    ),
                    selfStatus,
                    GdSelfStatusReason.CAMPAIGN_HAS_ADGROUPS_WITH_WARNINGS
                )
            )
    }
}
