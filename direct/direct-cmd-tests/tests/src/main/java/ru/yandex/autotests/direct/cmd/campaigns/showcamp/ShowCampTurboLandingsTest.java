package ru.yandex.autotests.direct.cmd.campaigns.showcamp;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.banner.SiteLink;
import ru.yandex.autotests.direct.cmd.data.commons.banner.TurboLanding;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.performanceGroups.savePerformanceAdGroups.GroupErrorsResponse;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.TurbolandingsRecord;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.List;

import static ru.yandex.autotests.direct.cmd.steps.groups.GroupHelper.saveAdGroup;
import static ru.yandex.autotests.direct.cmd.steps.groups.GroupHelper.saveInvalidAdGroup;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

@Aqua.Test
@Description("Просмотр и добавление турболендингов в ТГО кампаниях")
@Stories(TestFeatures.Banners.BANNER_TURBO_LANDINGS)
@Features(TestFeatures.BANNERS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.BANNER)
@Tag(CampTypeTag.TEXT)
public class ShowCampTurboLandingsTest {
    private static final String CLIENT = Logins.DEFAULT_CLIENT;

    private static final long ID_FOR_EXISTENT_TURBOLANDING = 777L;
    private static final long ID_FOR_UNEXISTENT_TURBOLANDING = 8888L;
    private static final String TEST_TURBOLANDING_NAME = "Тестовый турболендинг";
    private static final String DEFAULT_TURBOLANDING_METRICA_COUNTERS = "[]";
    private static final String UNKNOWN_TURBOLANDING_ERROR_TEXT = "Задан несуществующий турболендинг";

    private static final String TEST_SITELINK_TITLE = "Title 1111";
    private static final String TEST_SITELINK_URL = "ya.ru/?zzz=qqq";
    private static final String TEST_SITELINK_PROTOCOL_HTTP = "http://";

    private static final TurboLanding VALID_TURBOLANDING = getTurboLanding(ID_FOR_EXISTENT_TURBOLANDING);
    private static final TurboLanding INVALID_TURBOLANDING = getTurboLanding(ID_FOR_UNEXISTENT_TURBOLANDING);

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Before
    public void init() {
        cmdRule.cmdSteps().authSteps().authenticate(User.get(CLIENT));
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).turboLandingsSteps().addOrUpdate(
                new TurbolandingsRecord()
                        .setTlId(VALID_TURBOLANDING.getId())
                        .setHref(VALID_TURBOLANDING.getHref())
                        .setClientid(Long.valueOf(User.get(CLIENT).getClientID()))
                        .setName(TEST_TURBOLANDING_NAME)
                        .setMetrikaCountersJson(DEFAULT_TURBOLANDING_METRICA_COUNTERS)
        );
        TestEnvironment.newDbSteps().turboLandingsSteps().deleteTurboLanding(INVALID_TURBOLANDING.getId());
    }

    @Test
    @Description("Проверка - у исходного баннера турболендинг отсутствует")
    @TestCaseId("11011")
    public void turboLandingMissed() {
        TurboLanding turboLanding = bannersRule.getCurrentGroup().getBanners().get(0).getTurboLanding();

        assertThat("Турболендинг отсутствует", turboLanding, nullValue());

    }

    @Test
    @Description("Добавление турболендинга баннеру")
    @TestCaseId("11012")
    public void addBannerTurboLanding() {
        Group expectedGroup = bannersRule.getGroupForUpdate();
        expectedGroup.getBanners().get(0).setTurboLanding(VALID_TURBOLANDING);

        Group savedGroup = saveAdGroup(cmdRule, CLIENT, bannersRule.getCampaignId(), expectedGroup, bannersRule.getMediaType());
        Banner banner = savedGroup.getBanners().get(0);

        assertThat("Турболендинг баннера совпадает с сохраненным",
                banner.getTurboLanding(), BeanDifferMatcher.beanDiffer(VALID_TURBOLANDING)
        );
    }

    @Test
    @Description("При попытке добавить баннеру несуществующий турболендинг получаем ошибку")
    @TestCaseId("11013")
    public void addInvalidBannerTurboLanding() {
        Group expectedGroup = bannersRule.getGroupForUpdate();
        expectedGroup.getBanners().get(0).setTurboLanding(INVALID_TURBOLANDING);

        GroupErrorsResponse errors = saveInvalidAdGroup(cmdRule, CLIENT, bannersRule.getCampaignId(), expectedGroup, bannersRule.getMediaType());

        assertThat("Сохранение завершено с ошибкой",
                errors.getError(), equalTo(UNKNOWN_TURBOLANDING_ERROR_TEXT)
        );
    }

    @Test
    @Description("Добавление турболендинга сайтлинку")
    @TestCaseId("11014")
    public void addSiteLinkTurboLanding() {
        SiteLink emptySiteLink = new SiteLink().withUrlProtocol(TEST_SITELINK_PROTOCOL_HTTP).withHref("").withTitle("");

        Group expectedGroup = bannersRule.getGroupForUpdate();
        SiteLink siteLink = new SiteLink()
                .withTurboLanding(VALID_TURBOLANDING)
                .withTitle(TEST_SITELINK_TITLE)
                .withHref(TEST_SITELINK_URL)
                .withUrlProtocol(TEST_SITELINK_PROTOCOL_HTTP);

        List<SiteLink> siteLinks =  Arrays.asList(
                siteLink,
                emptySiteLink,
                emptySiteLink,
                emptySiteLink
        );

        expectedGroup.getBanners().get(0).setSiteLinks(siteLinks);

        Group savedGroup = saveAdGroup(cmdRule, CLIENT, bannersRule.getCampaignId(), expectedGroup,
                bannersRule.getMediaType());

        SiteLink receivedSitelink = savedGroup.getBanners().get(0).getSiteLinks().get(0);

        assertThat("Турболендинг сайтлинка совпадает с сохраненным",
                receivedSitelink.getTurboLanding(), BeanDifferMatcher.beanDiffer(VALID_TURBOLANDING)
        );
    }

    @Test
    @Description("Проверка флага наличия доступа к фиче")
    @TestCaseId("11015")
    public void checkIsFeatureTurboLandingEnabled() {
        Integer isFeatureEnabled = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(CLIENT, bannersRule.getCampaignId())
                .getIsFeatureTurboLandingEnabled();

        assertThat("Доступ к фиче есть",
                isFeatureEnabled, equalTo(1)
        );
    }

    @Test
    @Description("Проверка отсутствия доступа к фиче, если у пользователя нет турболендингов")
    @TestCaseId("11016")
    public void checkIsFeatureTurboLandingDisabled() {
        TestEnvironment.newDbSteps().turboLandingsSteps().deleteTurboLanding(VALID_TURBOLANDING.getId());
        Integer isFeatureEnabled = cmdRule.cmdSteps().campaignSteps().getShowCampMultiEdit(CLIENT, bannersRule.getCampaignId())
                .getIsFeatureTurboLandingEnabled();

        assertThat("Доступа к фиче нет",
                isFeatureEnabled, equalTo(0)
        );
    }

    private static TurboLanding getTurboLanding(long id) {
        return new TurboLanding()
                .withId(id)
                .withHref("https://yandex.ru/turbo?text=qlean&screen=welcome")
                .withIsDisabled(0);
    }

}
