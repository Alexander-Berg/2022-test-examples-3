package ru.yandex.market.mbi.affiliate.promo.distribution;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import ru.yandex.market.mbi.affiliate.promo.model.Partner;
import ru.yandex.market.request.trace.Module;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
public class DistributionReportClientTest {

    private DistributionReportClient client;

    private MockRestServiceServer mockServer;

    @Before
    public void init() {
        var restTemplate = RestTemplateHelper.createRestTemplate(
                new HttpSettings(Module.DISTRIBUTION_REPORT, "http://someurl.me"));
        client = new DistributionReportClient(
                "http://someurl.me/api/v2/products/clids/report/",
                restTemplate);
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testParse() throws IOException {
        DistributionReportClient.Response response = loadResponse();
        List<Partner> clidData = client.resolveClidData(response);
        assertThat(clidData, iterableWithSize(6));
        Map<Long, String> packDomainsByClid =
                clidData.stream().collect(Collectors.toMap(Partner::getClid, Partner::getPackDomain));
        Map<Long, String> userLoginsByClid =
                clidData.stream().collect(Collectors.toMap(Partner::getClid, Partner::getUserLogin));
        assertThat(packDomainsByClid.get(2496500L), is("t.me/marketaff"));
        assertThat(packDomainsByClid.get(2490761L), is("promocodes.marketaff.ru"));
        assertThat(userLoginsByClid.get(2496500L), is("the_partner_login4"));
    }

    @Test
    public void testGetByUserLogin() throws Exception {
        DistributionReportClient.Response response = loadResponse();
        mockServer.expect(ExpectedCount.once(), requestTo(new URI("http://someurl.me/api/v2/products/clids/report/")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(
                        "{\"dimensions\":[\"clid\",\"set_id\",\"pack_id\",\"set_pack_id\",\"pack_domain\",\"user_login\"]," +
                        "\"filters\":[\"AND\",[" +
                            "[\"user_login\",\"=\",\"the_partner_login\"]," +
                            "[\"clid_type_id\",\"=\",100021]," +
                            "[\"contract_id\",\"<>\",0]," +
                            "[\"soft_id\",\"=\",1046]" +
                        "]]}"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(new ObjectMapper().writeValueAsString(response))
                );
        List<Partner> clids = client.getClidsByUserLogin("the_partner_login");
        assertThat(clids, iterableWithSize(6));
    }

    @Test
    public void testGetAll() throws Exception {
        DistributionReportClient.Response response = loadResponse();
        mockServer.expect(ExpectedCount.once(), requestTo(new URI("http://someurl.me/api/v2/products/clids/report/")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(
                        "{\"dimensions\":[\"clid\",\"set_id\",\"pack_id\",\"set_pack_id\",\"pack_domain\",\"user_login\"]," +
                                "\"filters\":[\"AND\",[" +
                                "[\"clid_type_id\",\"=\",100021]," +
                                "[\"contract_id\",\"<>\",0]," +
                                "[\"soft_id\",\"=\",1046]" +
                                "]]}"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(new ObjectMapper().writeValueAsString(response))
                );
        List<Partner> partners = client.getAllPartners();
        assertThat(partners, iterableWithSize(6));
        assertThat(partners.stream().map(Partner::getClid).collect(Collectors.toList()),
                containsInAnyOrder(2496765L, 2487083L, 2496499L, 2490761L, 2496500L, 2490762L));
    }

    private DistributionReportClient.Response loadResponse() throws IOException {
        try (InputStream stream =
                     DistributionReportClientTest.class
                             .getClassLoader().getResourceAsStream("ru/yandex/market/mbi/affiliate/promo/distribution/report_response.json")) {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readerFor(DistributionReportClient.Response.class).readValue(stream);
        }
    }
}