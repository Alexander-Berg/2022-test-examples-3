package ru.yandex.autotests.direct.cmd.campaigns.copycamp;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.StatusModerate;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.CampaignV2;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.extendedgeo.ExtendedGeoItem;
import ru.yandex.autotests.direct.cmd.data.savecamp.GeoCharacteristic;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.passport.api.core.matchers.common.IsNot.not;

@Aqua.Test
@Description("Проверка копирования текстовой кампании с auto video (copyCamp)")
@Stories(TestFeatures.Campaigns.COPY_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.COPY_CAMP)
@Tag(ObjectTag.CAMPAIGN)
@Tag(ObjectTag.BANNER)
@Tag(ObjectTag.AUTO_VIDEO)
@Tag(CampTypeTag.TEXT)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class CopyCampWithGeoChangesAndMultipliersTest {
    private static final String CLIENT_1 = "at-direct-geo-changes-1";
    private static final String CLIENT_2 = "at-direct-geo-changes-2";

    public static final String PCT = "640";
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();
    private Long newCid;
    private String newCidOwner;

    private TextBannersRule bannersRule = new TextBannersRule()
            .withUlogin(CLIENT_1)
            .overrideCampTemplate(campRequest());

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    @After
    public void deleteCampaignCopy() {
        if (newCidOwner != null && newCid != null) {
            cmdRule.apiAggregationSteps().deleteActiveCampaignQuietly(newCidOwner, newCid);
        }
    }

    private SaveCampRequest campRequest() {
        Map<String, GeoCharacteristic> geoCharacteristicMap = new HashMap<>();
        geoCharacteristicMap.put(Geo.RUSSIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));

        Map<String, String> geoMultipliers = new HashMap<>();
        geoMultipliers.put(Geo.RUSSIA.getGeo(), PCT);

        return new SaveCampRequest().withGeoChanges(geoCharacteristicMap).withGeo("")
                .withGeoMultipliers(geoMultipliers);
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10886")
    public void copyCampWithExtendedGeo() {
        newCidOwner = CLIENT_1;
        newCid = cmdRule.cmdSteps().copyCampSteps().copyCampWithinClient(CLIENT_1, bannersRule.getCampaignId());
        CampaignV2 campaign = cmdRule.cmdSteps().campaignSteps().getEditCamp(newCid, CLIENT_1).getCampaign();

        assumeThat("у кампании есть настройки геотаргетинга", campaign.getExtendedGeoItemsMap(),
                not(nullValue()));

        Map<String, ExtendedGeoItem> expectedExtendedGeoItemMap = new HashMap<>();
        expectedExtendedGeoItemMap.put(Geo.RUSSIA.getGeo(), new ExtendedGeoItem().withMultiplierPct(PCT).withAll("1"));
        assertThat("после копирования настройки корректировок верные", campaign.getExtendedGeoItemsMap(),
                beanDiffer(expectedExtendedGeoItemMap));
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("10885")
    public void copyCampWithExtendedGeoOtherClient() {
        newCidOwner = CLIENT_2;
        newCid = cmdRule.cmdSteps().copyCampSteps().copyCamp(
                CLIENT_1,
                CLIENT_2,
                bannersRule.getCampaignId(),
                StatusModerate.YES.toString()
        );
        CampaignV2 campaign = cmdRule.cmdSteps().campaignSteps().getEditCamp(newCid, CLIENT_2).getCampaign();

        Map<String, ExtendedGeoItem> expectedExtendedGeoItemMap = new HashMap<>();
        expectedExtendedGeoItemMap.put(Geo.RUSSIA.getGeo(), new ExtendedGeoItem().withMultiplierPct(PCT).withAll("1"));
        assertThat("после копирования настройки корректировок верные", campaign.getExtendedGeoItemsMap(),
                beanDiffer(expectedExtendedGeoItemMap));
    }
}
