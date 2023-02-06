package ru.yandex.autotests.direct.cmd.bssynced.banners;

import java.util.Collections;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.BannersMinusGeoType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.PhrasesStatusbssynced;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.model.PerlBoolean;
import ru.yandex.autotests.directapi.darkside.model.Status;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper.makeCampSynced;

@Aqua.Test
@Description("Проверка сброса статуса bsSynced группы при изменении статусов баннеров с минус регионами")
@Features(TestFeatures.GROUPS)
@Stories(TestFeatures.Groups.STATUS_BS_SYNCED)
@Tag(CampTypeTag.TEXT)
public class ChangingGroupStatusesBsSyncedWhenSetBannersStatusShowTest {

    private static final String CLIENT = "at-direct-bssync-groups1";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private TextBannersRule bannersRule = new TextBannersRule()
            .overrideGroupTemplate(new Group().withGeo(Geo.RUSSIA.getGeo() + "," + Geo.UKRAINE.getGeo()))
            .withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @Before
    public void before() {
        makeCampSynced(cmdRule, bannersRule.getCampaignId());
    }

    private void setBannerUkraineMinusGeo(Long bid) {
        TestEnvironment.newDbSteps().useShardForLogin(CLIENT).bannersSteps()
                .saveBannersMinusGeo(bid, BannersMinusGeoType.current, Geo.UKRAINE.getGeo());
    }

    @Test
    @Description("Сброс bsSynced у группы при остановке баннера с минус регионами")
    @TestCaseId("11018")
    public void bannerWithMinusGeoStopped_resetGroupBsSynced() {
        setBannerUkraineMinusGeo(bannersRule.getBannerId());
        cmdRule.cmdSteps().bannerSteps().setBannersStatusShow(bannersRule.getGroupId(),
                Collections.singletonList(bannersRule.getBannerId()), PerlBoolean.NO);

        BsSyncedHelper.checkGroupBsSynced(CLIENT, bannersRule.getGroupId(), PhrasesStatusbssynced.No);
    }

    @Test
    @Description("Сброс bsSynced у группы при возобновлении баннера с минус регионами")
    @TestCaseId("11019")
    public void bannerWithMinusGeoResumed_resetGroupBsSynced() {
        setBannerUkraineMinusGeo(bannersRule.getBannerId());
        cmdRule.apiSteps().bannersFakeSteps().setStatusShow(bannersRule.getBannerId(), Status.NO);

        cmdRule.cmdSteps().bannerSteps().setBannersStatusShow(bannersRule.getGroupId(),
                Collections.singletonList(bannersRule.getBannerId()), PerlBoolean.YES);

        BsSyncedHelper.checkGroupBsSynced(CLIENT, bannersRule.getGroupId(), PhrasesStatusbssynced.No);
    }

    @Test
    @Description("Не должен сбрасываться bsSynced у группы при остановке баннера без минус регионов")
    @TestCaseId("11020")
    public void bannerWithoutMinusGeoStopped_notResetGroupBsSynced() {
        cmdRule.cmdSteps().bannerSteps().setBannersStatusShow(bannersRule.getGroupId(),
                Collections.singletonList(bannersRule.getBannerId()), PerlBoolean.NO);

        BsSyncedHelper.checkGroupBsSynced(CLIENT, bannersRule.getGroupId(), PhrasesStatusbssynced.Yes);
    }

    @Test
    @Description("Не должен сбрасываться  bsSynced у группы при возобновлении баннера без минус регионов")
    @TestCaseId("11021")
    public void bannerWithoutMinusGeoResumed_notResetGroupBsSynced() {
        cmdRule.apiSteps().bannersFakeSteps().setStatusShow(bannersRule.getBannerId(), Status.NO);

        cmdRule.cmdSteps().bannerSteps().setBannersStatusShow(bannersRule.getGroupId(),
                Collections.singletonList(bannersRule.getBannerId()), PerlBoolean.YES);

        BsSyncedHelper.checkGroupBsSynced(CLIENT, bannersRule.getGroupId(), PhrasesStatusbssynced.Yes);
    }
}
