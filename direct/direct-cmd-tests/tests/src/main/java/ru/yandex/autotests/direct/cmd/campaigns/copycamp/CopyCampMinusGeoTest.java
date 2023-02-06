package ru.yandex.autotests.direct.cmd.campaigns.copycamp;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersMinusGeoType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.BannersMinusGeoRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.contains;
import static ru.yandex.autotests.direct.cmd.util.CampaignHelper.deleteAdGroupMobileContent;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Features(TestFeatures.Groups.MINUS_GEO)
@Stories(TestFeatures.GROUPS)
@Description("Проверка копирования минус гео при копировании кампании")
@Tag(CmdTag.SHOW_CAMP)
@Tag(CmdTag.COPY_CAMP)
@Tag(CampTypeTag.TEXT)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.GROUP)
@RunWith(Parameterized.class)
public class CopyCampMinusGeoTest {

    private static final String CLIENT = "at-direct-backend-c";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    private Long newCid;

    @Parameterized.Parameters(name = "Проверка копирования минус гео при копировании кампании. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO},
        });
    }

    public CopyCampMinusGeoTest(CampaignTypeEnum campaignType) {
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType).withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        BsSyncedHelper.moderateCamp(cmdRule, bannersRule.getCampaignId());

        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .saveBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.current, Geo.UKRAINE.getGeo());
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .saveBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.bs_synced, Geo.AUSTRIA.getGeo());
    }

    @After
    public void after() {
        if (newCid != null) {
            deleteAdGroupMobileContent(newCid, CLIENT);
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCid);
        }
    }

    @Test
    @Description("Проверка сброса минус гео при копировании кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10844")
    public void copyCampMinusGeoTest() {
        newCid = cmdRule.cmdSteps().copyCampSteps()
                .copyCamp(CLIENT, CLIENT, bannersRule.getCampaignId(), "1");

        Banner actualGroup = cmdRule.cmdSteps().campaignSteps()
                .getShowCamp(CLIENT, newCid.toString()).getGroups().get(0);
        List<Map<String, Object>> actualRecords = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bannersSteps().getBannersMinusGeo(actualGroup.getBid()).stream()
                .map(r -> r.intoMap())
                .collect(Collectors.toList());

        assertThat("минус гео скопировалось", actualRecords,
                contains(
                        beanDiffer(getExpectedRecordMap(actualGroup.getBid(), BannersMinusGeoType.current,
                                Geo.UKRAINE.getGeo()))));
    }

    private Map<String, Object> getExpectedRecordMap(Long bid, BannersMinusGeoType type, String minusGeo) {
        return new BannersMinusGeoRecord()
                .setBid(bid)
                .setType(type)
                .setMinusGeo(minusGeo).intoMap();
    }
}
