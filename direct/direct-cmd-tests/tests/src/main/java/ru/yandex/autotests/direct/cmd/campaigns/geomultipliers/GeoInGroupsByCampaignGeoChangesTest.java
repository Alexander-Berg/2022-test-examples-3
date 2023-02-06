package ru.yandex.autotests.direct.cmd.campaigns.geomultipliers;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.savecamp.GeoCharacteristic;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.assertj.core.api.Assertions.assertThat;

@Aqua.Test
@Description("Изменение гео на группах при изменении их на кампании" )
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SAVE_CAMP)
public class GeoInGroupsByCampaignGeoChangesTest {

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();
    protected static String ULOGIN = "at-direct-geo-changes-1";

    private TextBannersRule bannersRule1 = new TextBannersRule()
            .overrideCampTemplate(new SaveCampRequest().withGeo(Geo.AUSTRIA + "," + Geo.RUSSIA));

    private TextBannersRule bannersRule2 = new TextBannersRule()
            .forExistingCampaign(bannersRule1)
            .overrideCampTemplate(new SaveCampRequest().withGeo(Geo.AUSTRIA + "," + Geo.RUSSIA));

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule1, bannersRule2).as(ULOGIN);

    @Test
    @Description("Гео изменяется на группах")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10892")
    public void groupGeoShouldBeChanged() {
        SaveCampRequest request = bannersRule1.getSaveCampRequest();
        Map<String, GeoCharacteristic> geoChanges = new HashMap<>();
        geoChanges.put(Geo.RUSSIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));
        geoChanges.put(Geo.AUSTRIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));
        geoChanges.put(Geo.SIBERIA.getGeo(), new GeoCharacteristic().withIsNegative("1"));

        request.withGeoChanges(geoChanges).withCid(bannersRule1.getCampaignId().toString());

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

        Group group1 = bannersRule1.getCurrentGroup();
        Group group2 = bannersRule2.getCurrentGroup();
        String[] expected = new String[3];
        expected[0] = Geo.RUSSIA.getGeo();
        expected[1] = Geo.AUSTRIA.getGeo();
        expected[2] = "-" + Geo.SIBERIA.getGeo();
        checkGeo(group1, group2, expected);
    }

    @Step("Проверяем гео регионы на группах")
    private void checkGeo(Group group1, Group group2, String[] expected) {
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(group1.getGeo().split(",")).containsExactlyInAnyOrder(expected);
        softly.assertThat(group2.getGeo().split(",")).containsExactlyInAnyOrder(expected);
        softly.assertAll();
    }

    @Test
    @Description("Добавление нового региона в кампании добавляет его на группу")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10891")
    public void addedToGroupGeo() {
        bannersRule1.updateCurrentGroupWith(new Group().withGeo(Geo.AUSTRIA.getGeo()));

        SaveCampRequest request = bannersRule1.getSaveCampRequest();
        Map<String, GeoCharacteristic> geoChanges = new HashMap<>();
        geoChanges.put(Geo.RUSSIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));

        request.withGeoChanges(geoChanges).withCid(bannersRule1.getCampaignId().toString());

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

        Group group1 = bannersRule1.getCurrentGroup();

        String[] expected = new String[2];

        expected[0] = Geo.RUSSIA.getGeo();
        expected[1] = Geo.AUSTRIA.getGeo();

        assertThat(group1.getGeo().split(",")).containsExactlyInAnyOrder(expected);
    }

    @Test
    @Description("Выставление более высокого уровня в дереве регионов на кампании перекрывает"
            + " выставленный более низкий на группе")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10890")
    public void geoCoversSubTrees() {
        bannersRule1.updateCurrentGroupWith(new Group().withGeo(Geo.SIBERIA.getGeo()));

        SaveCampRequest request = bannersRule1.getSaveCampRequest();
        Map<String, GeoCharacteristic> geoChanges = new HashMap<>();
        geoChanges.put(Geo.RUSSIA.getGeo(), new GeoCharacteristic().withIsNegative("0"));

        request.withGeoChanges(geoChanges).withCid(bannersRule1.getCampaignId().toString());

        cmdRule.cmdSteps().campaignSteps().postSaveCamp(request);

        Group group1 = bannersRule1.getCurrentGroup();

        assertThat(group1.getGeo().split(",")).containsExactlyInAnyOrder(Geo.RUSSIA.getGeo());

    }
}
