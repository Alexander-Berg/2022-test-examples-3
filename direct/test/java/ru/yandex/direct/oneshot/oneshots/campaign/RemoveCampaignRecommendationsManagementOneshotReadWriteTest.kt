package ru.yandex.direct.oneshot.oneshots.campaign

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.model.CampaignOpts
import ru.yandex.direct.core.entity.campaign.repository.CampaignMappings
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbschema.ppc.enums.CampaignsMetatype
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import java.util.EnumSet

@OneshotTest
@RunWith(Parameterized::class)
class RemoveCampaignRecommendationsManagementOneshotReadWriteTest(
    private val metatype: CampaignsMetatype,
    private val opts: EnumSet<CampaignOpts>,
    private val expectedOpts: EnumSet<CampaignOpts>,
) {
    @Rule
    @JvmField
    val springMethodRule = SpringMethodRule()

    @Autowired
    lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var steps: Steps

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()

        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(CampaignsMetatype.ecom,
                EnumSet.noneOf(CampaignOpts::class.java),
                EnumSet.noneOf(CampaignOpts::class.java)),
            arrayOf(CampaignsMetatype.ecom,
                EnumSet.of(CampaignOpts.RECOMMENDATIONS_MANAGEMENT_ENABLED,
                    CampaignOpts.PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED),
                EnumSet.noneOf(CampaignOpts::class.java)),
            arrayOf(CampaignsMetatype.ecom,
                EnumSet.of(CampaignOpts.RECOMMENDATIONS_MANAGEMENT_ENABLED,
                    CampaignOpts.PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED,
                    CampaignOpts.ENABLE_CPC_HOLD,
                    CampaignOpts.HIDE_PERMALINK_INFO),
                EnumSet.of(CampaignOpts.ENABLE_CPC_HOLD,
                    CampaignOpts.HIDE_PERMALINK_INFO)),
            arrayOf(CampaignsMetatype.ecom,
                EnumSet.of(CampaignOpts.RECOMMENDATIONS_MANAGEMENT_ENABLED,
                    CampaignOpts.ENABLE_CPC_HOLD,
                    CampaignOpts.HIDE_PERMALINK_INFO),
                EnumSet.of(CampaignOpts.ENABLE_CPC_HOLD,
                    CampaignOpts.HIDE_PERMALINK_INFO)),
            arrayOf(CampaignsMetatype.ecom,
                EnumSet.of(CampaignOpts.ENABLE_CPC_HOLD,
                    CampaignOpts.HIDE_PERMALINK_INFO),
                EnumSet.of(CampaignOpts.ENABLE_CPC_HOLD,
                    CampaignOpts.HIDE_PERMALINK_INFO)),
            arrayOf(CampaignsMetatype.default_,
                EnumSet.of(CampaignOpts.RECOMMENDATIONS_MANAGEMENT_ENABLED,
                    CampaignOpts.PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED,
                    CampaignOpts.ENABLE_CPC_HOLD,
                    CampaignOpts.HIDE_PERMALINK_INFO),
                EnumSet.of(CampaignOpts.RECOMMENDATIONS_MANAGEMENT_ENABLED,
                    CampaignOpts.PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED,
                    CampaignOpts.ENABLE_CPC_HOLD,
                    CampaignOpts.HIDE_PERMALINK_INFO)),
        )
    }

    @Test
    fun apply_and_compare() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val oneshot = RemoveCampaignRecommendationsManagementOneshot(
            dslContextProvider,
            mock(),
            mock(),
        )

        val campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo)
        if (metatype == CampaignsMetatype.ecom) {
            updateCampaignMetatypeToEcom(campaignInfo.shard, campaignInfo.campaignId)
        }
        oneshot.setOpts(dslContextProvider.ppc(campaignInfo.shard), campaignInfo.campaignId, opts)
        oneshot.processCampaign(campaignInfo.shard, campaignInfo.campaignId)
        val resultingOpts = getOpts(campaignInfo.shard, campaignInfo.campaignId)
        Assertions.assertThat(resultingOpts).isEqualTo(expectedOpts)
    }

    private fun updateCampaignMetatypeToEcom(shard: Int, campaignId: Long) {
        dslContextProvider.ppc(shard)
            .update(Tables.CAMPAIGNS)
            .set(Tables.CAMPAIGNS.METATYPE, CampaignsMetatype.ecom)
            .where(Tables.CAMPAIGNS.CID.eq(campaignId))
            .execute()
    }

    private fun getOpts(shard: Int, campaignId: Long): EnumSet<CampaignOpts>? {
        return CampaignMappings.optsFromDb(
            dslContextProvider.ppc(shard)
                .select(Tables.CAMPAIGNS.OPTS)
                .from(Tables.CAMPAIGNS)
                .where(Tables.CAMPAIGNS.CID.eq(campaignId))
                .fetchOne(Tables.CAMPAIGNS.OPTS)
        )
    }
}
