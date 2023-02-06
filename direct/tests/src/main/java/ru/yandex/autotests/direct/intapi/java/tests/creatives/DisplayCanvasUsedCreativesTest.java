package ru.yandex.autotests.direct.intapi.java.tests.creatives;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.CreativeBannerRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.autotests.direct.intapi.java.core.DirectRule;
import ru.yandex.autotests.direct.intapi.java.features.TestFeatures;
import ru.yandex.autotests.direct.intapi.java.features.tags.Tags;
import ru.yandex.autotests.direct.intapi.models.GetUsedCreativesResponse;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.direct.utils.tags.TagDictionary;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка работы DisplayCanvas get_used_creatives")
@Stories(TestFeatures.DisplayCanvas.GET_USED_CREATIVES)
@Features(TestFeatures.DISPLAY_CANVAS)
@Tag(Tags.DISPLAY_CANVAS)
@Tag(TagDictionary.TRUNK)
@Tag("DIRECT-67130")
@Issue("DIRECT-67130")
public class DisplayCanvasUsedCreativesTest {

    private static final String CLIENT = "at-direct-constr-used-cr";

    @ClassRule
    public static DirectRule directClassRule = DirectRule.defaultClassRule();

    private static Long clientId;

    private CreativeBannerRule bannersRule = new CreativeBannerRule(CampaignTypeEnum.TEXT).withUlogin(CLIENT);

    private DirectCmdRule cmdRule = DirectCmdRule.defaultRule()
            .withRules(bannersRule);
    @Rule
    public DirectRule directRule = DirectRule.defaultRule()
            .withRules(cmdRule);

    @BeforeClass
    public static void beforeClass() {
        clientId = directClassRule.dbSteps().shardingSteps().getClientIdByLogin(CLIENT);
        directClassRule.dbSteps().useShardForLogin(CLIENT);
        List<Long> creativeIds = directClassRule.dbSteps().perfCreativesSteps().getPerfCreativesByClientId(clientId)
                .stream().map(PerfCreativesRecord::getCreativeId)
                .collect(Collectors.toList());
        directClassRule.dbSteps().bannersPerformanceSteps().deleteBannersPerformanceRecords(creativeIds);
        directClassRule.dbSteps().perfCreativesSteps().deletePerfCreatives(creativeIds);
    }

    @Test
    public void getUsedCreatives() {
        GetUsedCreativesResponse response = directClassRule.intapiSteps().displayCanvasSteps()
                .getCreativesCampaigns(clientId, "image");
        assertThat("Получили нужный id креатива", response.getUsedCreatives(),
                containsInAnyOrder(Collections.singletonList(equalTo(bannersRule.getCreativeId()))));
    }
}
