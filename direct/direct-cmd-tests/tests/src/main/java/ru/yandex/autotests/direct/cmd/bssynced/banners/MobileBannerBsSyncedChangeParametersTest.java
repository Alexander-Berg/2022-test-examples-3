package ru.yandex.autotests.direct.cmd.bssynced.banners;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.common.AppMetricaHrefs;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.commons.banner.Banner;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersStatusbssynced;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.ReflectedAttributes;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper.makeCampSynced;

@Aqua.Test
@Description("Проверка сброса статуса bsSynced при изменении параметров баннера в мобильной кампании")
@Features(TestFeatures.BANNERS)
@Stories(TestFeatures.Banners.STATUS_BS_SYNCED)
@Tag(CampTypeTag.MOBILE)
public class MobileBannerBsSyncedChangeParametersTest {

    protected static final String CLIENT = "at-direct-bssync-banners1";
    protected static final String ANOTHER_HREF = AppMetricaHrefs.HREF_THREE;

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new MobileBannersRule().withUlogin(CLIENT);
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
    @Description("Изменение заголовка  должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9304")
    public void changeTitle() {
        createdBanner.withTitle("some new title");
        editGroup();
        check();
    }

    @Test
    @Description("Изменение заголовка  должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9301")
    public void changeBody() {
        createdBanner.withBody("some new body");
        editGroup();
        check();
    }

    @Test
    @Description("Изменение ссылки  должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9305")
    public void changeHref() {
        createdBanner.withHref(ANOTHER_HREF);
        editGroup();
        check();
    }

    @Test
    @Description("Добавление geoflag в ppc.banners.opts должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9302")
    public void setGeoFlag() {
        createdGroup.setGeo(Geo.BALASHIHA.getGeo());
        editGroup();

        check();
    }

    @Test
    @Description("Изменение данных из магазинов приложений должно сбрасывать statusBsSynced баннера")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9306")
    public void changeReflectedAttrs() {
        List<String> reflectedAttrs = new ArrayList<>();
        reflectedAttrs.add(ReflectedAttributes.RATING.toString());
        reflectedAttrs.add(ReflectedAttributes.ICON.toString());
        createdBanner.setReflectedAttrs(reflectedAttrs);
        editGroup();
        check();
    }

    private void editGroup() {
        cmdRule.cmdSteps().groupsSteps().prepareGroupForUpdate(createdGroup, bannersRule.getMediaType());
        bannersRule.saveGroup(GroupsParameters.forExistingCamp(CLIENT, bannersRule.getCampaignId(), createdGroup));
    }

    private void check() {
        BsSyncedHelper.checkBannerBsSynced(CLIENT, bannersRule.getBannerId(), BannersStatusbssynced.No);
    }

}
