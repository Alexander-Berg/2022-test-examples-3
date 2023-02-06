package ru.yandex.market.mbi.affiliate.promo.distribution;

import java.io.InputStream;
import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;

import ru.yandex.market.mbi.affiliate.promo.common.HttpSettings;
import ru.yandex.market.mbi.affiliate.promo.common.RestTemplateHelper;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;
import ru.yandex.market.mbi.affiliate.promo.service.PartnerService;
import ru.yandex.market.request.trace.Module;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
public class DistributionPlaceClientTest {

    private DistributionPlaceClient client;

    private MockRestServiceServer mockServer;

    @Before
    public void init() {
        var restTemplate = RestTemplateHelper.createRestTemplate(
                new HttpSettings(Module.DISTRIBUTION_REPORT, "http://someurl.me")
        );
        client = new DistributionPlaceClient(
                "http://someurl.me/api/v2/places/", restTemplate, 3);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void test() throws Exception {
        try (InputStream stream =
                     DistributionPlaceClientTest.class
                             .getClassLoader().getResourceAsStream("ru/yandex/market/mbi/affiliate/promo/distribution/place_response.json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            DistributionPlaceClient.Response response = objectMapper.readerFor(DistributionPlaceClient.Response.class).readValue(stream);

            mockServer.expect(ExpectedCount.once(), requestTo(new URI("http://someurl.me/api/v2/places/")))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withStatus(HttpStatus.OK)
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .body(new ObjectMapper().writeValueAsString(response))
                    );
            var clids = client.getClidsByPlaceTypes(PartnerService.FORBIDDEN_PLACE_TYPES);
            assertThat(clids, containsInAnyOrder(2352383L, 2352386L, 2352380L));

            mockServer.verify();
        }
    }

}
