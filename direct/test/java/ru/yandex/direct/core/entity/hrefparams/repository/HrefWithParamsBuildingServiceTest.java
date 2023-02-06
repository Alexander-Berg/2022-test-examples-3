package ru.yandex.direct.core.entity.hrefparams.repository;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.hrefparams.service.HrefWithParamsBuildingService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.data.TestGroups;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.hrefparams.service.HrefWithParamsBuildingService.buildHrefWithParams;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class HrefWithParamsBuildingServiceTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private HrefWithParamsBuildingService hrefWithParamsBuildingService;

    @Autowired
    private Steps steps;

    private Object[] parameters() {
        return new Object[][]{
                {"p=v", null, null, null, "Null href does not change"},
                {null, null, "https://yandex.ru/",
                        "https://yandex.ru/",
                        "Href with no params does not change"},
                {null, null, "https://yandex.ru/?some_param=val",
                        "https://yandex.ru/?some_param=val",
                        "Href with params on it does not change"},
                {"p2=new&p3=v3", null, "https://yandex.ru/?p1=v1&p2=old",
                        "https://yandex.ru/?p1=v1&p2=new&p3=v3",
                        "Params on campaign are merged"},
                {null, "p2=new&p3=v3", "https://yandex.ru/?p1=v1&p2=old",
                        "https://yandex.ru/?p1=v1&p2=new&p3=v3",
                        "Params on ad group are merged"},
                {"p2=from_camp&p_camp", "p2=from_group&p_group", "https://yandex.ru/?p1=v1&p2=old",
                        "https://yandex.ru/?p1=v1&p2=from_group&p_group",
                        "Params on ad group are chosen and params from campaign are ignored"},
        };
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("{4}")
    public void testBuildByAdGroupId(String paramsOnCampaign, String paramsOnAdGroup, String bannerHref,
                                               String expectedResult, String testCase) {
        var campaignInfo = steps.dynamicCampaignSteps().createCampaign(
                TestCampaigns.defaultDynamicCampaignWithSystemFields().withBannerHrefParams(paramsOnCampaign));
        var adGroupInfo = steps.adGroupSteps()
                .createAdGroup(TestGroups.activeDynamicTextAdGroup(campaignInfo.getCampaignId())
                        .withTrackingParams(paramsOnAdGroup), campaignInfo);

        assertThat(hrefWithParamsBuildingService.buildHrefWithParamsByAdGroupId(adGroupInfo.getShard(),
                adGroupInfo.getAdGroupId(), bannerHref))
                .isEqualTo(expectedResult);
    }

    @Test
    @Parameters(method = "parameters")
    @TestCaseName("{4}")
    public void testBuild(String paramsOnCampaign, String paramsOnAdGroup, String bannerHref,
                                  String expectedResult, String testCase) {
        assertThat(buildHrefWithParams(bannerHref, paramsOnAdGroup, paramsOnCampaign))
                .isEqualTo(expectedResult);
    }
}
