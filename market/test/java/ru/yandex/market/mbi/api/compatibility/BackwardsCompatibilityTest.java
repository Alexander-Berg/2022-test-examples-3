package ru.yandex.market.mbi.api.compatibility;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.core.abo.AboCutoff;
import ru.yandex.market.mbi.api.client.entity.abo.AboCutoffInfo;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.ExpectedCount.twice;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

/**
 * Проверка обратной совместимости клиента mbi-api
 */
public class BackwardsCompatibilityTest extends FunctionalTest {

    private static final String GET_ABO_CUTOFFS_URL_TEMPLATE = "http://localhost:%d/shop-abo-cutoffs/%d";

    @Autowired
    private RestTemplate mbiApiRestTemplate;
    private ClientHttpRequestFactory originalRequestFactory;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        originalRequestFactory = mbiApiRestTemplate.getRequestFactory();
        mockServer = MockRestServiceServer.createServer(mbiApiRestTemplate);
    }

    @AfterEach
    void tearDown() {
        mbiApiRestTemplate.setRequestFactory(originalRequestFactory);
    }

    @Test
    @DisplayName("Проверка десериализации новых значений в AboCutoff в старой версии клиента")
    void getAboCutoffsTest() {
        mockServer.expect(twice(), requestTo(String.format(GET_ABO_CUTOFFS_URL_TEMPLATE, port, 774L)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(getString(this.getClass(), "data/GetAboCutoffsResponse.xml"), MediaType.TEXT_XML));

        assertThatCode(() -> mbiApiClient.getAboCutoffs(774L)).doesNotThrowAnyException();
        assertThat(mbiApiClient.getAboCutoffs(774L).getAboCutoffs())
                .hasSize(4)
                .extracting(AboCutoffInfo::getAboCutoff)
                .filteredOn(AboCutoff.UNKNOWN::equals)
                .hasSize(2);
    }
}
