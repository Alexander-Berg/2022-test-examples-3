package ru.yandex.direct.web.entity.banner.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.model.WebErrorResponse;
import ru.yandex.direct.web.core.model.WebResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.web.entity.banner.service.BannerHrefWithParamsWebService.INVALID_HREF_MESSAGE;
import static ru.yandex.direct.web.entity.banner.service.BannerHrefWithParamsWebService.INVALID_UID_OR_ADGROUP_ID_MESSAGE;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerHrefWithParamsWebServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private BannerHrefWithParamsWebService bannerHrefWithParamsWebService;

    private AdGroupInfo adGroupInfo;


    @Before
    public void setup() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        testAuthHelper.setSubjectUser(adGroupInfo.getUid());
    }

    @Test
    public void adGroupIdAndBannerHrefForInput_success() {
        WebResponse bannerHrefWithParamsResponse = bannerHrefWithParamsWebService.getBannerHrefWithParams(
                adGroupInfo.getAdGroupId(), null, "https://yandex.ru/");

        assertThat(bannerHrefWithParamsResponse.isSuccessful())
                .as("Result is successful")
                .isEqualTo(true);
    }

    @Test
    public void adGroupIdAndBannerIdForInput_success() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(adGroupInfo);
        WebResponse bannerHrefWithParamsResponse = bannerHrefWithParamsWebService.getBannerHrefWithParams(
                adGroupInfo.getAdGroupId(), bannerInfo.getBannerId(), null);

        assertThat(bannerHrefWithParamsResponse.isSuccessful())
                .as("Result is successful")
                .isEqualTo(true);
    }

    @Test
    public void onlyAdGroupIdForInput_error() {
        WebResponse bannerHrefWithParamsResponse = bannerHrefWithParamsWebService.getBannerHrefWithParams(
                adGroupInfo.getAdGroupId(), null, null);

        assertThat(bannerHrefWithParamsResponse.isSuccessful())
                .as("Result is not successful")
                .isEqualTo(false);

        assertThat(bannerHrefWithParamsResponse)
                .isInstanceOf(WebErrorResponse.class);
        WebErrorResponse errorResponse = (WebErrorResponse) bannerHrefWithParamsResponse;
        assertThat(errorResponse.getText())
                .as("Message is correct")
                .isEqualTo(INVALID_HREF_MESSAGE);
    }

    @Test
    public void invalidAdGroupIdForInput_error() {
        WebResponse bannerHrefWithParamsResponse = bannerHrefWithParamsWebService.getBannerHrefWithParams(
                0L, null, "https://yandex.ru/");

        assertThat(bannerHrefWithParamsResponse.isSuccessful())
                .as("Result is not successful")
                .isEqualTo(false);

        assertThat(bannerHrefWithParamsResponse)
                .isInstanceOf(WebErrorResponse.class);
        WebErrorResponse errorResponse = (WebErrorResponse) bannerHrefWithParamsResponse;
        assertThat(errorResponse.getText())
                .as("Message is correct")
                .isEqualTo(INVALID_UID_OR_ADGROUP_ID_MESSAGE);
    }

    @Test
    public void adGroupIdAndInvalidBannerHref_error() {
        WebResponse bannerHrefWithParamsResponse = bannerHrefWithParamsWebService.getBannerHrefWithParams(
                 adGroupInfo.getAdGroupId(), null, "definitely not an href");

        assertThat(bannerHrefWithParamsResponse.isSuccessful())
                .as("Result is not successful")
                .isEqualTo(false);

        assertThat(bannerHrefWithParamsResponse)
                .isInstanceOf(WebErrorResponse.class);
        WebErrorResponse errorResponse = (WebErrorResponse) bannerHrefWithParamsResponse;
        assertThat(errorResponse.getText())
                .as("Message is correct")
                .isEqualTo(INVALID_HREF_MESSAGE);
    }

    @Test
    public void adGroupIdAndInvalidBannerIdForInput_error() {
        WebResponse bannerHrefWithParamsResponse = bannerHrefWithParamsWebService.getBannerHrefWithParams(
                adGroupInfo.getAdGroupId(), 0L, null);

        assertThat(bannerHrefWithParamsResponse.isSuccessful())
                .as("Result is not successful")
                .isEqualTo(false);

        assertThat(bannerHrefWithParamsResponse)
                .isInstanceOf(WebErrorResponse.class);
        WebErrorResponse errorResponse = (WebErrorResponse) bannerHrefWithParamsResponse;
        assertThat(errorResponse.getText())
                .as("Message is correct")
                .isEqualTo(INVALID_HREF_MESSAGE);
    }
}
