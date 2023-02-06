package ru.yandex.autotests.direct.cmd.bssynced.banners;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.banners.AdWarningFlag;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.ContactInfo;
import ru.yandex.autotests.direct.cmd.data.commons.banner.SiteLink;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusbssynced;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper.makeCampSynced;

@Aqua.Test
@Description("Проверка сброса статуса bsSynced при изменении параметров баннера в текстовой кампании")
@Features(TestFeatures.BANNERS)
@Stories(TestFeatures.Banners.STATUS_BS_SYNCED)
@Tag(CampTypeTag.PERFORMANCE)
public class TextBannerBsSyncedChangeParametersTest {

    protected static final String CLIENT = "at-direct-bssync-banners1";
    protected static final String ANOTHER_HREF = "https://translate.yandex.ru";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);
    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private Banner createdBanner;
    private Group createdGroup;

    @Before
    public void before() {
        createdGroup = bannersRule.getCurrentGroup();
        createdBanner = createdGroup.getBanners().get(0);
        makeCampSynced(cmdRule, bannersRule.getCampaignId());
    }

    @Test
    @Description("Изменение заголовка 1 должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9318")
    public void changeTitle() {
        createdBanner.withTitle("some new title");
        editGroup();
        check();
    }

    @Test
    @Description("Изменение заголовка 2 должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9318")
    public void changeTitleExtension() {
        createdBanner.withTitleExtension("some new titleExtension");
        editGroup();
        check();
    }

    @Test
    @Description("Изменение заголовка  должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9309")
    public void changeBody() {
        createdBanner.withBody("some new body");
        editGroup();
        check();
    }

    @Test
    @Description("Изменение ссылки  должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9319")
    public void changeHref() {
        createdBanner.withHref(ANOTHER_HREF);
        editGroup();
        check();
    }

    @Test
    @Description("Добавление новых сайтлинков  должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9310")
    public void addSiteLinks() {
        setSiteLink();
        editGroup();
        makeCampSynced(cmdRule, bannersRule.getCampaignId());

        bannersRule.getBanner().getSiteLinks().get(1)
                .withHref("translate.yandex.ru")
                .withTitle("Yandex Translate");
        createdBanner.setSiteLinks(
                bannersRule.getBanner().getSiteLinks()
        );
        editGroup();
        check();
    }

    @Test
    @Description("Удаление сайтлинков  должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9311")
    public void deleteSiteLinks() {
        setSiteLink();
        editGroup();
        makeCampSynced(cmdRule, bannersRule.getCampaignId());

        List<SiteLink> emptySiteLinks = new ArrayList<>();
        for (int i = 0; i < 4; ++i) {
            emptySiteLinks.add(new SiteLink()
                    .withUrlProtocol("https")
                    .withHref("")
                    .withTitle(""));
        }

        createdBanner.setSiteLinks(emptySiteLinks);

        editGroup();
        check();
    }

    @Test
    @Description("Изменение сайтлинков  должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9312")
    public void editSiteLinks() {
        setSiteLink();
        editGroup();
        check();
    }

    @Test
    @Description("Добавление визитки  должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9313")
    public void addVCard() {
        setVcard();
        editGroup();
        check();
    }

    @Test
    @Description("Изменение визитки  должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9314")
    public void editVCard() {
        setVcard();
        editGroup();

        makeCampSynced(cmdRule, bannersRule.getCampaignId());

        createdBanner.withContactInfo(
                createdBanner.getContactInfo()
                        .withCompanyName("new company name")
        );

        editGroup();
        check();
    }

    @Test
    @Description("Удаление визитки  должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9315")
    public void deleteVCard() {
        setVcard();
        editGroup();

        makeCampSynced(cmdRule, bannersRule.getCampaignId());

        createdBanner.withContactInfo(null).withHasVcard(0);

        editGroup();
        check();
    }

    @Test
    @Description("Добавление geoflag в ppc.banners.opts должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9316")
    public void setGeoFlag() {
        createdGroup.setGeo(Geo.BALASHIHA.getGeo());
        editGroup();

        check();
    }

    private void setVcard() {
        ContactInfo contactInfo = BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_VCARD_FULL, ContactInfo.class);
        createdBanner
                .withContactInfo(contactInfo)
                .withHasVcard(1);
    }

    private void setSiteLink() {
        bannersRule.getBanner().getSiteLinks().get(0)
                .withHref("yandex.ru")
                .withTitle("Yandex")
                .withUrlProtocol("https");
        createdBanner.setSiteLinks(
                bannersRule.getBanner().getSiteLinks()
        );
    }

    private void editGroup() {
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(createdGroup, bannersRule.getMediaType());
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), createdGroup));
    }

    private void check() {
        BsSyncedHelper.checkBannerBsSynced(CLIENT, bannersRule.getBannerId(), BannersStatusbssynced.No);
    }

}
