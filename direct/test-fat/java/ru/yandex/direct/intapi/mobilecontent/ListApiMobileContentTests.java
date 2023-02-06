package ru.yandex.direct.intapi.mobilecontent;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.intapi.entity.mobilecontent.controller.MobileAppApiController;
import ru.yandex.direct.intapi.entity.mobilecontent.model.ApiMobileContentRequest;
import ru.yandex.direct.intapi.entity.mobilecontent.model.ApiMobileContentResponse;
import ru.yandex.direct.intapi.fatconfiguration.FatIntApiTest;
import ru.yandex.direct.intapi.mobilecontent.utils.ApiMobileContentYTRecord;
import ru.yandex.direct.intapi.mobilecontent.utils.ApiMobileTableUtils;
import ru.yandex.direct.web.core.entity.mobilecontent.model.ApiMobileContent;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.ytwrapper.model.YtCluster.YT_LOCAL;

@FatIntApiTest
@RunWith(SpringRunner.class)
public class ListApiMobileContentTests {
    private static final String APP_ID1 = "testApp1";
    private static final String APP_ID2 = "testApp2";
    private static final String LANG_RU = "ru";
    private static final String LANG_US = "us";
    private static final String BUNDLE_NAME = "first bundle";
    private static final String APP_NAME = "test app";
    private static final String ICON = "dummyIconAddress";
    private static final Long RATING_COUNT = 10L;
    private static final String CURRENCY_RU = "RUB";
    private static final String CURRENCY_US = "USD";
    private static final String PUBLISHER = "Test, Inc";
    private static final String WEBSITE = "dummyWebsiteAddress";

    private static final String STORE_NAME = "gplay";

    private static final ApiMobileContent API_MOBILE_CONTENT1_RU = new ApiMobileContent()
            .withStore(STORE_NAME)
            .withAppId(APP_ID1)
            .withLang(LANG_RU)
            .withBundle(BUNDLE_NAME)
            .withName(APP_NAME)
            .withIcon(ICON)
            .withRatingCount(RATING_COUNT)
            .withCurrency(CURRENCY_RU)
            .withPublisher(PUBLISHER)
            .withWebsite(WEBSITE);
    private static final ApiMobileContent API_MOBILE_CONTENT1_US = new ApiMobileContent()
            .withStore(STORE_NAME)
            .withAppId(APP_ID1)
            .withLang(LANG_US)
            .withBundle(BUNDLE_NAME)
            .withName(APP_NAME)
            .withIcon(ICON)
            .withRatingCount(RATING_COUNT)
            .withCurrency(CURRENCY_US)
            .withPublisher(PUBLISHER)
            .withWebsite(WEBSITE);

    private static final Map<String, ApiMobileContent> API_MOBILE_CONTENT1_MAP = Map.of(
            LANG_RU, API_MOBILE_CONTENT1_RU,
            LANG_US, API_MOBILE_CONTENT1_US);

    @Autowired
    private MobileAppApiController controller;

    @Autowired
    private ApiMobileTableUtils apiMobileTablesUtils;

    @Before
    public void before() {
        List<ApiMobileContentYTRecord> mobileApps = asList(
                new ApiMobileContentYTRecord()
                        .withAppId(APP_ID1)
                        .withLang(LANG_RU)
                        .withBundle(BUNDLE_NAME)
                        .withName(APP_NAME)
                        .withIcon(ICON)
                        .withRatingCount(RATING_COUNT)
                        .withCurrency(CURRENCY_RU)
                        .withPublisher(PUBLISHER)
                        .withWebsite(WEBSITE),
                new ApiMobileContentYTRecord()
                        .withAppId(APP_ID1)
                        .withLang(LANG_US)
                        .withBundle(BUNDLE_NAME)
                        .withName(APP_NAME)
                        .withIcon(ICON)
                        .withRatingCount(RATING_COUNT)
                        .withCurrency(CURRENCY_US)
                        .withPublisher(PUBLISHER)
                        .withWebsite(WEBSITE),
                new ApiMobileContentYTRecord()
                        .withAppId(APP_ID2)
                        .withLang(LANG_RU)
                        .withBundle(BUNDLE_NAME)
                        .withName(APP_NAME)
                        .withIcon(ICON)
                        .withRatingCount(RATING_COUNT)
                        .withCurrency(CURRENCY_RU)
                        .withPublisher(PUBLISHER)
                        .withWebsite(WEBSITE)
        );
        apiMobileTablesUtils.createMobileContentTable(STORE_NAME, YT_LOCAL, mobileApps);
    }

    @Test
    public void listApiMobileContentTest() {
        var request = new ApiMobileContentRequest(singletonList(new ApiMobileContentRequest.App(STORE_NAME, APP_ID1)));
        var result = (ApiMobileContentResponse) controller.list(request);
        var resultApps = result.getApps();

        assertThat(result.isSuccessful()).isTrue();
        assertThat(resultApps.size()).isEqualTo(2);
        for (var resultApiMobileContent : resultApps) {
            var apiMobileContent = API_MOBILE_CONTENT1_MAP.get(resultApiMobileContent.getLang());
            assertThat(resultApiMobileContent).isEqualToComparingFieldByField(apiMobileContent);
        }
    }
}
