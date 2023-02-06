package ru.yandex.autotests.direct.cmd.campaigns.setautopriceajax.rarelyloaded;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.campaigns.SetAutoPriceAjaxRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CampaignStrategy;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BidsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.strategy.data.Strategies;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;


@Aqua.Test
@Description("Изменение в мастере цен в сетях через setAutoPriceAjax для групп с флагом мало показов")
@Features(TestFeatures.Groups.RARELY_LOADED_FLAG)
@Stories(TestFeatures.GROUPS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SET_AUTO_PRICE_AJAX)
@RunWith(Parameterized.class)
public class SetWizardNetAutoPriceAjaxRarelyLoadedTest extends SetAutoPriceAjaxRarelyLoadedTestBase {

    private Long secondGroupId;

    public SetWizardNetAutoPriceAjaxRarelyLoadedTest(CampaignTypeEnum campaignType) {
        super(campaignType);
    }

    @Override
    protected CampaignStrategy getCampStrategy() {
        return CmdStrategyBeans.getStrategyBean(Strategies.SHOWS_DISABLED_MAX_COVERADGE);
    }

    @Before
    @Override
    public void before() {
        super.before();
        secondGroupId = Long.valueOf(getSecondGroupId());
        List<BidsRecord> bids = TestEnvironment.newDbSteps().useShard(shard).bidsSteps()
                .getBidsByCid(bannersRule.getCampaignId());
        bids.forEach(b -> {
            b.setPriceContext(BigDecimal.valueOf(EXPECTED_PRICE));
            TestEnvironment.newDbSteps().useShard(shard).bidsSteps().updateBids(b);
        });
    }

    @Test
    @Description("Изменение цены в мастере цен в сетях")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10681")
    public void changeSimplePriceNetTest() {
        autoPriceAjaxRequest = new SetAutoPriceAjaxRequest()
                .withCid(bannersRule.getCampaignId())
                .withTabSimple(0)
                .withWizardCtx(1)
                .withWizardCtxPhrases(1)
                .withCtxMaxPrice("300")
                .withCtxProc(30)
                .withCtxScope(100);

        setPrice();
        assumeThat("ставка фраз группы изменилась",
                getGroupPhrase(secondGroupId).getPriceContext(), not(equalTo(EXPECTED_PRICE)));

        assertThat("ставка фраз группы с мало показов не изменилась",
                getGroupPhrase(bannersRule.getGroupId()).getPriceContext(), equalTo(EXPECTED_PRICE));
    }

}
