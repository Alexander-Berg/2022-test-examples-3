package ru.yandex.market.vendor;


import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.brand.MemoryBrandInfoService;

import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/VendorPartnerTest/before.csv",
        dataSource = "vendorDataSource"
)
class VendorPartnerTest extends AbstractVendorPartnerFunctionalTest {
    @Autowired
    private MemoryBrandInfoService brandInfoService;

    /**
     * Тест проверяет, что приложение vendor-partner стартует и отвечает на пинг стандартным способом.
     */
    @Test
    void shouldLoadSpringContextWithoutAnyErrors() {
        String OK = "0;OK";

        String response;
        do {
            try {
                response = FunctionalTestHelper.get(baseUrl + "/ping");
                assertThat(response, equalTo(OK));
            } catch (HttpServerErrorException e) {
                response = e.getResponseBodyAsString();
                assertThat(response, not(equalTo(OK)));
            }
            brandInfoService.brandById(14473967L);
        } while (!OK.equals(response));
    }

    /**
     * Проверяет, что неизвестный метод с UID-ом возвращает 404
     */
    @Test
    void testUnknownMethodWithUidShouldReturn404() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/blablabla?uid=100500")
        );
        String expected = getStringResource("/testUnknownMethodWithUidShouldReturn404/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), when(IGNORING_ARRAY_ORDER));
    }

    /**
     * Проверяет, что неизвестный метод без UID-а возвращает 404
     */
    @Test
    void testUnknownMethodWithoutUidShouldReturn404() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/blablabla")
        );
        String expected = getStringResource("/testUnknownMethodWithoutUidShouldReturn404/expected.json");
        JsonAssert.assertJsonEquals(expected, exception.getResponseBodyAsString(), when(IGNORING_ARRAY_ORDER));
    }

}
