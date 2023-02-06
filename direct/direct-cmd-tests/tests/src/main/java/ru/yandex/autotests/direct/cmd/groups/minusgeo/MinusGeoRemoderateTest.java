package ru.yandex.autotests.direct.cmd.groups.minusgeo;


import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.BannersRuleFactory;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
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
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@Aqua.Test
@Features(TestFeatures.Groups.MINUS_GEO)
@Stories(TestFeatures.GROUPS)
@Description("Проверка изменения минус гео при перемодерации кампании")
@Tag(CampTypeTag.TEXT)
@Tag(CampTypeTag.MOBILE)
@Tag(CampTypeTag.DYNAMIC)
@Tag(CampTypeTag.PERFORMANCE)
@Tag(TrunkTag.YES)
@RunWith(Parameterized.class)
public class MinusGeoRemoderateTest {

    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule;

    @Rule
    public DirectCmdRule cmdRule;

    @Parameterized.Parameters(name = "Проверка изменения минус гео при перемодерации кампании. Тип кампании: {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {CampaignTypeEnum.TEXT},
                {CampaignTypeEnum.MOBILE},
                {CampaignTypeEnum.DTO},
        });
    }

    public MinusGeoRemoderateTest(CampaignTypeEnum campaignType) {
        bannersRule = BannersRuleFactory.getBannersRuleBuilderByCampType(campaignType)
                .overrideGroupTemplate(new Group().withGeo(Geo.RUSSIA.getGeo() + "," + Geo.UKRAINE.getGeo()))
                .withUlogin(CLIENT);
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Before
    public void before() {
        cmdRule.apiSteps().campaignFakeSteps().makeCampaignFullyModerated(bannersRule.getCampaignId());
        cmdRule.apiSteps().groupFakeSteps().makeGroupFullyModerated(bannersRule.getGroupId());
        cmdRule.apiSteps().bannersFakeSteps().makeBannerFullyModerated(bannersRule.getBannerId());
        cmdRule.apiSteps().phrasesFakeSteps().setPhraseStatusModerateYes(bannersRule.getGroupId());

        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .saveBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.current, Geo.UKRAINE.getGeo());
    }

    @Test
    @Description("Проверка изменения гео после перемодерации")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10852")
    public void minusGeoDeleteAfterRemoderateTest() {
        cmdRule.cmdSteps().campaignSteps().remoderateCamp(bannersRule.getCampaignId(), CLIENT);

        BannersMinusGeoRecord actualRecord = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bannersSteps().getBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.current);
        assertThat("Минус гео удалилось", actualRecord, nullValue());
    }

    @Test
    @Description("Проверка изменения гео после перемодерации")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10853")
    public void twoMinusGeoDeleteAfterRemoderateTest() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .saveBannersMinusGeo(bannersRule.getBannerId(), BannersMinusGeoType.bs_synced, Geo.AUSTRIA.getGeo());

        cmdRule.cmdSteps().campaignSteps().remoderateCamp(bannersRule.getCampaignId(), CLIENT);

        List<Map<String, Object>> actualRecords = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bannersSteps().getBannersMinusGeo(bannersRule.getBannerId()).stream()
                .map(r -> r.intoMap())
                .collect(Collectors.toList());
        assertThat("минус гео скопировалось", actualRecords,
                contains(
                        beanDiffer(getExpectedRecordMap(bannersRule.getBannerId(), BannersMinusGeoType.bs_synced,
                                Geo.AUSTRIA.getGeo()))));
    }

    private Map<String, Object> getExpectedRecordMap(Long bid, BannersMinusGeoType type, String minusGeo) {
        return new BannersMinusGeoRecord()
                .setBid(bid)
                .setType(type)
                .setMinusGeo(minusGeo).intoMap();
    }
}
