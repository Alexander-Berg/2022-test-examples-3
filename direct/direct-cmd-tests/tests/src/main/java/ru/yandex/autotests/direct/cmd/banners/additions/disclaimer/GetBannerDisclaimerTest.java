package ru.yandex.autotests.direct.cmd.banners.additions.disclaimer;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.banners.additions.GetBannersAdditionsResponse;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersAdditionsAdditionsType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.AdditionsItemDisclaimersRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestStepsEn.assumeThat;


@Aqua.Test
@Description("Получение дисклеймера баннера")
@Stories(TestFeatures.Banners.BANNERS_DISCLAIMER)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.GET_BANNERS_ADDITIONS)
@Tag(TrunkTag.YES)
public class GetBannerDisclaimerTest {

    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();

    public static final String CLIENT = "at-direct-banners-disclaimer";

    private TextBannersRule textBannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(textBannersRule);

    private Long disclaimerId;

    @After
    public void after() {
        if (disclaimerId != null) {
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                    .bannerAdditionsSteps().deleteBannerAdditions(textBannersRule.getBannerId(), disclaimerId);
            TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                    .bannerAdditionsSteps().deleteAdditionsItemDisclaimers(disclaimerId);
        }
    }

    public void addDisclaimer() {
        disclaimerId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bannerAdditionsSteps().saveAdditionsItemDisclaimers(
                        Long.parseLong(User.get(CLIENT).getClientID()),
                        RandomStringUtils.randomAlphanumeric(24));

        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannerAdditionsSteps().saveBannerAdditions(
                textBannersRule.getBannerId(),
                disclaimerId,
                BannersAdditionsAdditionsType.disclaimer);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10635")
    public void getDisclaimerTest() {
        Banner banner = cmdRule.cmdSteps().groupsSteps().getBanner(
                CLIENT,
                textBannersRule.getCampaignId(),
                textBannersRule.getBannerId());
        assumeThat("У баннера нет дисклеймера", banner.getDisclaimer(), nullValue());
        addDisclaimer();
        AdditionsItemDisclaimersRecord disclaimer = TestEnvironment.newDbSteps().useShardForLogin(CLIENT)
                .bannerAdditionsSteps().getDisclaimer(disclaimerId);
        banner = cmdRule.cmdSteps().groupsSteps().getBanner(CLIENT, textBannersRule.getCampaignId(), textBannersRule.getBannerId());
        assertThat("У баннера появился дисклеймер", banner.getDisclaimer(), equalTo(disclaimer.getDisclaimerText()));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10636")
    public void getDisclaimersByClientTest() {
        GetBannersAdditionsResponse response = cmdRule.cmdSteps().bannersAdditionsSteps().getDisclaimers(CLIENT);
        assertThat("Не поддерживаем запрос дисклеймеров по клиенту", response.getSuccess(), equalTo("0"));
    }
}
