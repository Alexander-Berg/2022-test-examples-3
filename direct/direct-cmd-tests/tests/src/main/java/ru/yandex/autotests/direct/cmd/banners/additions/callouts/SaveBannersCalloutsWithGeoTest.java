package ru.yandex.autotests.direct.cmd.banners.additions.callouts;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.banners.GroupsParameters;
import ru.yandex.autotests.direct.cmd.data.savecamp.SaveCampRequest;
import ru.yandex.autotests.direct.cmd.data.showcamp.ShowCampResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.utils.campaigns.CampaignTypeEnum;
import ru.yandex.autotests.irt.testutils.allure.AssumptionException;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.direct.cmd.util.BeanLoadHelper.loadCmdBean;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/*
* todo javadoc
*/
@Aqua.Test
@Description("Прявязка дополнений к баннерам с различными языками и регионами")
@Stories(TestFeatures.Banners.BANNERS_CALLOUTS)
@Features(TestFeatures.BANNERS)
@RunWith(Parameterized.class)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
public class SaveBannersCalloutsWithGeoTest {
    @ClassRule
    public static DirectCmdRule defaultClassRuleChain = DirectCmdRule.defaultClassRule();

    public String ulogin = "at-direct-banners-callouts-9";

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();
    @Parameterized.Parameter(value = 0)
    public String callout;
    @Parameterized.Parameter(value = 1)
    public Geo geo;
    private CalloutsTestHelper helper;
    private String cid;

    @Parameterized.Parameters(name = "Уточнение: {0} gео: {1}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"Türkçe", Geo.TURKEY},
                {"українська", Geo.CRIMEA},
                {"українська", Geo.UKRAINE},
                {"русский", Geo.UKRAINE},
                {"русский", Geo.CRIMEA},
                {"русский", Geo.BELORUSSIA},
                {"қазақ", Geo.KAZAKHSTAN}
        });
    }

    @Before
    public void setUp() {
        SaveCampRequest campRequest = loadCmdBean(CmdBeans.SAVE_NEW_TEXT_CAMP_DEFAULT, SaveCampRequest.class)
                .withGeo(geo.getGeo());
        campRequest.setMediaType(CampaignTypeEnum.TEXT.getValue());
        campRequest.setUlogin(ulogin);

        cid = cmdRule.cmdSteps().campaignSteps().saveNewCampaign(campRequest).toString();

        helper = new CalloutsTestHelper(ulogin, cmdRule.cmdSteps(), cid);

        helper.clearCalloutsForClient();
    }

    @Test
    @ru.yandex.qatools.allure.annotations.TestCaseId("9086")
    public void saveCallouts() {
        GroupsParameters request = helper.getRequestFor(helper.newGroupAndSet(callout).withGeo(geo.getGeo()));
        helper.saveCallouts(request);

        ShowCampResponse response = cmdRule.cmdSteps().campaignSteps().getShowCamp(ulogin, cid);

        String actualCallout = helper.getCalloutsList(response)
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssumptionException("Дополнение не сохранилось"));

        assertThat("дополнения сохранились", actualCallout, equalTo(callout));

    }

    @After
    public void deleteCamp() {
        if (cid != null) {
            cmdRule.cmdSteps().campaignSteps().deleteCampaign(ulogin, Long.valueOf(cid));
        }
    }
}
