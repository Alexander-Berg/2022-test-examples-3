package ru.yandex.autotests.direct.cmd.bssynced.campaigns;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.savecamp.GeoCharacteristic;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.FeaturesTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper.checkCampaignBsSynced;
import static ru.yandex.autotests.direct.cmd.bssynced.BsSyncedHelper.checkGroupBsSynced;

@Aqua.Test
@Description("Проверка поведения statusBsSynced при работе с геокорректировками" )
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SAVE_CAMP)
@Tag(FeaturesTag.BS_SYNCED)
public class StatusBsSyncedOnGeoMultipliersChangeTest {
    static final String NEW_RUSSIA_MULTIPLIER = "600";
    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static String ULOGIN = "at-direct-geo-changes-1";

    private TextBannersRule bannersRule = new TextBannersRule()
            .overrideGroupTemplate(new Group().withGeo(Geo.SIBERIA.getGeo()))
            .overrideCampTemplate(new SaveCampRequest().withGeo(null));

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule).as(ULOGIN);

    @Before
    public void setUp() {
        BsSyncedHelper.setCampaignBsSynced(cmdRule, bannersRule.getCampaignId(), StatusBsSynced.YES);
        BsSyncedHelper.setGroupBsSynced(cmdRule, bannersRule.getGroupId(), StatusBsSynced.YES);
        BsSyncedHelper.moderateCamp(cmdRule, bannersRule.getCampaignId());
    }

    @Test
    @Description("StatusBsSynced сбрасывается на кампании при изменении регионов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10879")
    public void campaignStatusBsSyncedChangedOnGeoChanges() {
        produceGeoChanges();
        checkCampaignBsSynced(ULOGIN, bannersRule.getCampaignId(), StatusBsSynced.NO, StatusBsSynced.SENDING);
    }

    @Test
    @Description("StatusBsSynced сбрасывается на группе при изменении регионов на кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10880")
    public void groupStatusBsSyncedChangedOnGeoChanges() {
        produceGeoChanges();
        checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO, StatusBsSynced.SENDING);
    }

    @Test
    @Description("StatusBsSynced сбрасывается на кампании при изменении процентов для регионов")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10881")
    public void campaignStatusBsSyncedChangedOnMultiprierChanges() {
        produceMultipliersChanges();
        checkCampaignBsSynced(ULOGIN, bannersRule.getCampaignId(), StatusBsSynced.NO, StatusBsSynced.SENDING);
    }

    @Test
    @Description("StatusBsSynced сбрасывается на группе при изменении процентов для регионов на кампании")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10882")
    public void groupStatusBsSyncedChangedOnMultiprierChanges() {
        produceMultipliersChanges();
        checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.NO, StatusBsSynced.SENDING);
    }

    @Test
    @Description("StatusBsSynced не сбрасывается на кампании при сохранении без изменений")
    @Ignore
    @ru.yandex.qatools.allure.annotations.TestCaseId("10883")
    public void campaignStatusBsSyncedNotChangedWithNoChanges() {
        produceNoChanges();
        checkCampaignBsSynced(ULOGIN, bannersRule.getCampaignId(), StatusBsSynced.YES);
    }

    @Test
    @Description("StatusBsSynced не сбрасывается на группе при сохранении кампании без изменений")
    @Ignore
    @ru.yandex.qatools.allure.annotations.TestCaseId("10884")
    public void groupStatusBsSyncedNotChangedWithNoChanges() {
        produceNoChanges();
        checkGroupBsSynced(bannersRule.getCurrentGroup(), StatusBsSynced.YES);
    }

    private void produceGeoChanges() {
        SaveCampRequest request = bannersRule.getSaveCampRequest();

        Map<String, GeoCharacteristic> geoChanges = new HashMap<>();
        geoChanges.put(Geo.RUSSIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));

        request.withGeoChanges(geoChanges).withCid(bannersRule.getCampaignId().toString());
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);
    }

    private void produceMultipliersChanges() {
        SaveCampRequest request = bannersRule.getSaveCampRequest();

        Map<String, String> geoMultipliers = new HashMap<>();
        geoMultipliers.put(Geo.RUSSIA.getGeo(), NEW_RUSSIA_MULTIPLIER);

        request.withGeoMultipliers(geoMultipliers);

        request.withCid(bannersRule.getCampaignId().toString());
        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

    }

    private void produceNoChanges() {
        cmdRule.cmdSteps().campaignSteps()
                .postSaveCamp(bannersRule.getSaveCampRequest().withCid(bannersRule.getCampaignId().toString()));

    }
}
