package ru.yandex.autotests.direct.cmd.banners.additions.disclaimer;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.*;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersAdditionsAdditionsType;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsNull.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Получение дисклеймера баннера")
@Stories(TestFeatures.Banners.BANNERS_DISCLAIMER)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.GET_BANNERS_ADDITIONS)
@Tag(TrunkTag.YES)
public class DoNotCopyDisclaimerTest {
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();

    public static final String CLIENT = "at-direct-banners-disclaimer";

    private TextBannersRule textBannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(textBannersRule);

    private Long disclaimerId;
    private Long newCid;

    @After
    public void after() {
        if (disclaimerId != null) {
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                    .bannerAdditionsSteps().deleteBannerAdditions(textBannersRule.getBannerId(), disclaimerId);
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                    .bannerAdditionsSteps().deleteAdditionsItemDisclaimers(disclaimerId);
        }
        if (newCid != null) {
            cmdRule.apiAggregationSteps().makeCampaignReadyForDelete(newCid);
            cmdRule.cmdSteps().campaignSteps().deleteCampaign(CLIENT, newCid);
        }
    }

    @Before
    public void before() {
        disclaimerId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannerAdditionsSteps()
                .saveAdditionsItemDisclaimers(
                        Long.parseLong(User.get(CLIENT).getClientID()),
                        RandomStringUtils.randomAlphanumeric(24));

        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannerAdditionsSteps().saveBannerAdditions(
                textBannersRule.getBannerId(),
                disclaimerId,
                BannersAdditionsAdditionsType.disclaimer);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10634")
    public void doNotCopyDisclaimerTest() {
        newCid = cmdRule.cmdSteps().copyCampSteps()
                .copyCampWithinClient(CLIENT, textBannersRule.getCampaignId());
        Banner copiedBanner = cmdRule.cmdSteps().campaignSteps().getShowCamp(CLIENT, newCid.toString())
                .getGroups().get(0);
        assertThat("При копировании баннера дисклеймер не копируется", copiedBanner.getDisclaimer(), nullValue());
    }

}
