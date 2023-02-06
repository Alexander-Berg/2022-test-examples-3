package ru.yandex.market.core.abo._public.impl;

import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.api.entity.rating.operational.RatingPartnerType;
import ru.yandex.market.abo.api.entity.spark.data.ReportInfo;
import ru.yandex.market.abo.api.entity.spark.data.ResponseSparkStatus;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.abo.AboServiceException;
import ru.yandex.market.core.abo._public.AboPublicService;
import ru.yandex.market.core.abo._public.ShopCloneInfo;
import ru.yandex.market.core.util.http.UnitTestMarketHttpClient;
import ru.yandex.market.mbi.http.MarketHttpClient;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author sergey-fed
 * Функционалтные тесты для {@link HttpAboPublicService}.
 */
class HttpAboPublicServiceTest extends FunctionalTest {

    @Autowired
    private MarketHttpClient httpClient;

    @Autowired
    private AboPublicService aboPublicService;

    private static void stubClientResponse(MarketHttpClient client, String responseText) {
        ((UnitTestMarketHttpClient) client).setResponseText(responseText);
    }

    private static Matcher<ShopCloneInfo> cloneInfoMatcher(long shopId, long clusterId) {
        return new TypeSafeMatcher<ShopCloneInfo>() {
            @Override
            protected boolean matchesSafely(ShopCloneInfo item) {
                return item.getShopId() == shopId && item.getCloneClusterId() == clusterId;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ShopCloneInfo{shopId=").appendValue(shopId)
                        .appendText(", cloneClusterId=").appendValue(clusterId).appendText("}");
            }
        };
    }

    /**
     * Проверяет, что вызов {@code /api/shop/down}, возвращающий идентификаторы магазинов
     * отрабатывает корректно
     */
    @Test
    void testGetDisabledByPingerShopIdsWithShops() {
        stubClientResponse(httpClient, "[3,1,2147483650]");

        Set<Long> ids = aboPublicService.getDisabledByPingerShopIds();

        assertThat(ids, containsInAnyOrder(3L, 1L, 2147483650L));
    }

    /**
     * Проверяет, что вызов {@code /api/shop/down} с пустым ответом отрабатывает корректно
     */
    @Test
    void testGetDisabledByPingerShopIdsWithEmptyOutput() {
        stubClientResponse(httpClient, "[]");

        Set<Long> ids = aboPublicService.getDisabledByPingerShopIds();

        assertTrue("Set of ids is not empty for empty abo-public response", ids.isEmpty());
    }

    /**
     * Проверяет, что в случае недоступности abo-public получается внятная ошибка
     */
    @Test
    void testGetDisabledByPingerShopIdsWithAboPublicUnavailability() {
        String errorJettyResponse =
                "<html>\n" +
                        "<head>\n" +
                        "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=ISO-8859-1\"/>\n" +
                        "<title>Error 503 </title>\n" +
                        "</head>\n" +
                        "</html>";
        stubClientResponse(httpClient, errorJettyResponse);

        assertThrows(
                AboServiceException.class,
                () -> aboPublicService.getDisabledByPingerShopIds()
        );
    }

    /**
     * Проверяет, что вызов {@code /api/shop/clones}, возвращающий данные, отрабатывает корректно
     */
    @Test
    void testGetAllShopsCloneInfoWithShops() {
        stubClientResponse(httpClient,
                "[{\"clusterId\": 2147483651,\"shopId\": 89},{\"clusterId\": 4520349,\"shopId\": 2147483650}]");

        Set<ShopCloneInfo> shopCloneInfos = aboPublicService.getAllShopsCloneInfo();

        assertThat(shopCloneInfos, containsInAnyOrder(
                cloneInfoMatcher(89L, 2147483651L),
                cloneInfoMatcher(2147483650L, 4520349L)
        ));
    }

    /**
     * Проверяет, что вызов {@code /api/shop/clones} с пустым ответом отрабатывает корректно
     */
    @Test
    void testGetAllShopsCloneInfoWithEmptyOutput() {
        stubClientResponse(httpClient, "[]");

        Set<ShopCloneInfo> shopCloneInfos = aboPublicService.getAllShopsCloneInfo();

        assertTrue("Set of shop clones is not empty for empty abo-public response", shopCloneInfos.isEmpty());
    }

    /**
     * Проверяет, что retry-и клиента отрабатывают корректно.
     */
    @Test
    void testRetries() throws Exception {
        stubClientResponse(httpClient, "I'm not a JSON or XML response.");

        assertThrows(AboServiceException.class, () -> aboPublicService.getAllShopsCloneInfo());

        verify(httpClient, times(3)).get(anyString(), any());
    }

    /**
     * Проверяет, что вызов {@code /api/cpa/partner-model-settings}, возвращающий данные, отрабатывает корректно.
     */
    @Test
    void testGetPartnerModelSettings() {
        int newbieOrdersLimitPerDay = 40;
        int newbieLimitUntilOrdersCount = 100;
        int showInPiOrdersCount = 10;
        String responseText = "{\"newbieOrdersLimitPerDay\":" + newbieOrdersLimitPerDay +
                              ",\"newbieLimitUntilOrdersCount\":" + newbieLimitUntilOrdersCount +
                              ",\"showInPiOrdersCount\":" + showInPiOrdersCount + "}";
        stubClientResponse(httpClient, responseText);
        var partnerModelSettings = aboPublicService.getPartnerModelSettings(RatingPartnerType.DROPSHIP,true);
        boolean checkPartnerModelSettings = partnerModelSettings.getNewbieOrdersLimitPerDay() == newbieOrdersLimitPerDay &&
                                            partnerModelSettings.getNewbieLimitUntilOrdersCount() == newbieLimitUntilOrdersCount &&
                                            partnerModelSettings.getShowInPiOrdersCount() == showInPiOrdersCount;
        assertTrue("partnerModelSettings must be equals responseText", checkPartnerModelSettings);
    }

    /**
     * Проверяет, что не запрашиваем пустой ОГРН.
     */
    @Test
    void testGetOgrnEmpty() throws Exception {
        stubClientResponse(httpClient, "{}");

        assertEquals(aboPublicService.getOgrnInfo(null, 11L).getReportInfo(),
                new ReportInfo(ResponseSparkStatus.OGRN_INVALID_FORMAT));

        verify(httpClient, times(0)).get(anyString(), any());
    }

    /**
     * Проверяет, что запрашиваем ОГРН.
     */
    @Test
    void testGetOgrn() throws Exception {
        stubClientResponse(httpClient, "{\"ReportInfo\":{\"sparkStatus\":\"OK\"}}");

        assertEquals(aboPublicService.getOgrnInfo("1033289584680", 11L).getReportInfo(),
                new ReportInfo(ResponseSparkStatus.OK));

        verify(httpClient, times(1))
                .get(eq("market.abo.public.url/api/spark/ogrnInfo?ogrn=1033289584680&uid=11"), any());
    }

    /**
     * Проверяет, что запрашиваем ИНН.
     */
    @Test
    void testGetOgrnInn() throws Exception {
        stubClientResponse(httpClient, "{\"ReportInfo\":{\"sparkStatus\":\"OK\"}}");

        assertEquals(aboPublicService.getOgrnInfo("7090892806", 11L).getReportInfo(),
                new ReportInfo(ResponseSparkStatus.OK));

        verify(httpClient, times(1))
                .get(eq("market.abo.public.url/api/spark/ogrnInfo?inn=7090892806&uid=11"), any());
    }
}
