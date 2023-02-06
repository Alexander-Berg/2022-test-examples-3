package ru.yandex.autotests.direct.intapi.java.tests.metrikaexport;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.models.MetrikaGroupsParam;
import ru.yandex.autotests.direct.intapi.models.MetrikaGroupsResult;
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
@Description("metrika-export/groups")
@Stories(TestFeatures.Metrika.GROUPS)
@Features(TestFeatures.METRIKA)
@Tag(Tags.METRIKA)
@Tag(TagDictionary.TRUNK)
@Issue("DIRECT-91807")
public class MetrikaExportGetGroupsTest {
    public static final String ULOGIN = "at-direct-intapi-metrika2";

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    private TextBannersRule bannersRule = new TextBannersRule()
            .withMediaType(CampaignTypeEnum.TEXT)
            .withUlogin(ULOGIN);

    private DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(bannersRule);
    @Rule
    public DirectRule directRule = DirectRule.defaultRule()
            .withRules(cmdRule);

    @Test
    public void getGroups() {
        Long orderId = RandomUtils.nextLong(0L, Integer.MAX_VALUE);
        Long clientId = directRule.dbSteps().shardingSteps().getClientIdByLogin(ULOGIN);

        directRule.dbSteps().shardingSteps().createOrderIdMapping(orderId, clientId);

        directRule.dbSteps()
                .useShardForLogin(ULOGIN)
                .campaignsSteps().setOrderId(bannersRule.getCampaignId(), orderId);


        MetrikaGroupsParam requestParam = new MetrikaGroupsParam()
                .withGroupId(bannersRule.getGroupId())
                .withOrderId(orderId);

        List<MetrikaGroupsResult> results =
                directRule.intapiSteps().metrikaExportControllerSteps()
                        .getGroups(Collections.singletonList(requestParam));
        assumeThat("В результате получили 1 ну группу", results, Matchers.hasSize(1));

        MetrikaGroupsResult expected = new MetrikaGroupsResult()
                .withOrderId(orderId)
                .withGroupId(bannersRule.getGroupId())
                .withGroupName(bannersRule.getGroup().getAdGroupName());

        assertThat("Получили верные параметры группы", results.get(0), beanDiffer(expected));
    }
}
