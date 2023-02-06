package ru.yandex.market.mbi.marketing.yt

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.marketing.FunctionalTest
import ru.yandex.market.mbi.marketing.dao.model.Tables.YT_EXP_MARKETING_CAMPAIGNS_FOR_BILLING
import ru.yandex.market.mbi.marketing.dao.model.tables.records.YtExpMarketingCampaignsForBillingRecord

class YtExpMarketingCamapignForBillingFunctionalTest(@Autowired private val dslContext: DSLContext) : FunctionalTest() {

    @Test
    @DbUnitDataSet(
        before = ["/ru/yandex/market/mbi/marketing/yt/YtExpMarketingCamapignForBillingFunctionalTest/validate_view/before.csv"]
    )
    fun `test yt_exp_marketing_campaigns_for_billing view`() {
        val actual: List<YtExpMarketingCampaignsForBillingRecord> =
            dslContext.selectFrom(YT_EXP_MARKETING_CAMPAIGNS_FOR_BILLING)
                .where(DSL.trueCondition())
                .fetch()
        val expected = YtExpMarketingCampaignsForBillingRecord(
            2,
            100500,
            10,
            "Новая кампания",
            "MARKETPLACE",
            90401,
            "EMAIL",
            "0.01",
            "2020-05-24T00:00:00.FF6+03:00",
            "2020-05-31T00:00:00.FF6+03:00",
            1,
            2,
            500,
            "RUB",
            true
        )
        Assertions.assertEquals(listOf(expected), actual)
    }

}
