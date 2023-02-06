package ru.yandex.autotests.direct.cmd.campaigns.copycamp;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannerDisplayHrefsStatusmoderate;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersPhoneflag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatussitelinksmoderate;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Проверка копирования статусов модерации визиток, сайтлинков и отображаемых ссылок в баннере ТГО кампании контроллером copyCamp")
@Stories(TestFeatures.Campaigns.COPY_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.COPY_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(CampTypeTag.TEXT)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.GROUP)
public class CopyCampModerateBannerAdditionsTest {

    private static final String CLIENT = "at-direct-mod-camp";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().overrideBannerTemplate(new Banner()
                .withHasVcard(1)
                .withContactInfo(BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class))
                .withDisplayHref("somehref"))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);;

    private Long newCid;

    @Before
    public void before() {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .setStatusSitelinksModerate(bannersRule.getBannerId(), BannersStatussitelinksmoderate.Sending);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .setPhoneflag(bannersRule.getBannerId(), BannersPhoneflag.Sending);
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannerDisplayHrefsSteps()
                .updateStatusModeration(bannersRule.getBannerId(), BannerDisplayHrefsStatusmoderate.Sending);

        newCid = cmdRule.cmdSteps().copyCampSteps().copyCamp(CLIENT, CLIENT, bannersRule.getCampaignId(), "copy_moderate_status");
    }

    @After
    public void after() {
        if (newCid != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(CLIENT, newCid);
        }

    }

    @Test
    @Description("Проверка сброса в Ready статуса модерации на визитке при копировании кампании")
    @TestCaseId("11031")
    public void copyCampVcard() {
        Banner actualBanner = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, newCid).get(0);
        assertThat("статус модерации сбросился в Ready", actualBanner.getPhoneFlag(),
                equalTo(BannersPhoneflag.Ready.toString()));
    }

    @Test
    @Description("Проверка сброса в Ready статуса модерации на сайтлинках при копировании кампании")
    @TestCaseId("11030")
    public void copyCampSiteLinks() {
        Banner actualBanner = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, newCid).get(0);

        BannersStatussitelinksmoderate actualStatus = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .getBannersByCid(newCid).get(0).getStatussitelinksmoderate();
        assertThat("статус модерации сбросился в Ready", actualStatus,
                equalTo(BannersStatussitelinksmoderate.Ready));
    }

    @Test
    @Description("Проверка сброса в Ready статуса модерации на отображаемой ссылке при копировании кампании")
    @TestCaseId("11032")
    @Ignore("DIRECT-118180 Джоба displayhrefs.DisplayHrefsModerationEventsProcessor успевает сменить статус раньше " +
            "этой проверки")
    public void copyCampDisplayHref() {
        Banner actualBanner = cmdRule.cmdSteps().groupsSteps().getBanners(CLIENT, newCid).get(0);

        BannerDisplayHrefsStatusmoderate actualStatus = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannerDisplayHrefsSteps()
                .getBannerDisplayHrefs(actualBanner.getBid()).getStatusmoderate();
        assertThat("статус модерации сбросился в Ready", actualStatus,
                equalTo(BannerDisplayHrefsStatusmoderate.Ready));
    }
}
