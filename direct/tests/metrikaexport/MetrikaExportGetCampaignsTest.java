package ru.yandex.autotests.direct.intapi.java.tests.metrikaexport;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.CampaignRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.models.MetrikaCampaignsResult;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Description("metrika-export/campaigns")
@Stories(TestFeatures.Metrika.CAMPAIGNS)
@Features(TestFeatures.METRIKA)
@Tag(Tags.METRIKA)
@Tag(TagDictionary.TRUNK)
@Issue("DIRECT-91807")
public class MetrikaExportGetCampaignsTest {
    public static final String ULOGIN = "at-direct-intapi-metrika2";
    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();
    private CampaignRule campaignRule = new CampaignRule()
            .withMediaType(CampaignTypeEnum.TEXT)
            .withUlogin(ULOGIN);
    private DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(campaignRule);
    @Rule
    public DirectRule directRule = DirectRule.defaultRule()
            .withRules(cmdRule);

    @Test
    public void getCampaigns() {
        Long orderId = RandomUtils.nextLong(0L, Integer.MAX_VALUE);
        Long clientId = directRule.dbSteps().shardingSteps().getClientIdByLogin(ULOGIN);
        directRule.dbSteps().shardingSteps().createOrderIdMapping(orderId, clientId);
        directRule.dbSteps().useShardForLogin(ULOGIN).campaignsSteps()
                .setOrderId(campaignRule.getCampaignId(), orderId);
        List<MetrikaCampaignsResult> results =
                directRule.intapiSteps().metrikaExportControllerSteps()
                        .getCampaigns(Collections.singletonList(orderId));
        assumeThat("В результате получили 1 ну кампанию", results, Matchers.hasSize(1));

        ShowCampResponse createdCamp =
                cmdRule.cmdSteps().campaignSteps().getShowCamp(ULOGIN, campaignRule.getCampaignId().toString());
        MetrikaCampaignsResult expected = new MetrikaCampaignsResult()
                .withOrderId(orderId)
                .withCid(campaignRule.getCampaignId())
                .withClientId(clientId)
                .withCurrency(createdCamp.getCampaignCurrency())
                .withName(createdCamp.getName())
                .withMetrikaCounters(null);

        assertThat("Получили верные параметры кампании", results.get(0), beanDiffer(expected));
    }
}
