package ru.yandex.market.vendors.analytics.platform.controller.billing;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.vendors.analytics.core.utils.DateUtils;
import ru.yandex.market.vendors.analytics.platform.FunctionalTest;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

/**
 * @author antipov93.
 */
public class BalanceFunctionalTest extends FunctionalTest {

    private static final String CS_BILLING_URL = "http://cs-billing-api.tst.vs.market.yandex.net:80";
    private static final String VENDOR_API_URL = "http://vendor-partner.tst.vs.market.yandex.net";

    @Autowired
    private RestTemplate csBillingRestTemplate;
    @Autowired
    private RestTemplate vendorApiRestTemplate;

    private MockRestServiceServer mockCsBillingServer;
    private MockRestServiceServer mockVendorApiServer;


    @BeforeEach
    void resetMocks() {
        mockCsBillingServer = MockRestServiceServer.createServer(csBillingRestTemplate);
        mockVendorApiServer = MockRestServiceServer.createServer(vendorApiRestTemplate);
    }

    @AfterEach
    void tearDown() {
        mockCsBillingServer.verify();
        mockVendorApiServer.verify();
    }

    protected void mockBalance(long datasourceId, long actualBalanceInRubles) {
        mockBalance(datasourceId, actualBalanceInRubles, LocalDateTime.now());
    }

    private void mockBalance(long datasourceId, long actualBalanceInRubles, LocalDateTime lastBilledTime) {
        mockCsBillingServer.expect(once(), requestTo(getBalanceRequestUrl(datasourceId)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(balanceResponse(actualBalanceInRubles, lastBilledTime))
                );
    }

    protected void mockVendorDatasource(long vendorId, long datasourceId) {
        mockVendorApiServer.expect(once(), requestTo(vendorDatasourceUrl(vendorId)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(vendorDatasourceResponse(vendorId, datasourceId))
                );
    }

    protected void mockChangeDynamicCost(long datasourceId, long uid, long newCostInRubles) {
        mockCsBillingServer.expect(once(), requestTo(changeDynamicCostUrl(datasourceId, uid)))
                .andExpect(MockRestRequestMatchers.content().json(dynamicCostRequest(newCostInRubles), true))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON));
    }

    protected void mockUserBalanceVendors(long userId, Collection<Long> vendors) {
        mockVendorApiServer.expect(once(), requestTo(userBalanceVendorsUrl(userId)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(balanceVendorsResponse(vendors))
                );
    }

    private static String getBalanceRequestUrl(long datasourceId) {
        return CS_BILLING_URL + "/service/206/datasource/" + datasourceId + "/balance";
    }

    private static String changeDynamicCostUrl(long datasourceId, long uid) {
        return CS_BILLING_URL + "/service/206/datasource/" + datasourceId + "/dynamicCost?"
                + "uid=" + uid;
    }

    private static String vendorDatasourceUrl(long vendorId) {
        return VENDOR_API_URL + "/vendors/datasources?"
                + "isOffer=true&"
                + "product=MARKET_ANALYTICS&"
                + "vendorId=" + vendorId;
    }

    private static String userBalanceVendorsUrl(long userId) {
        return VENDOR_API_URL + "/authoritiesByVendor/" + userId + "/analytics"
                + "?uid=" + userId;
    }

    private static String balanceResponse(long balanceInRubles, LocalDateTime lastBilledDate) {
        return ""
                + "{"
                + "  \"actualBalance\": " + balanceInRubles * 100 + ","
                + "  \"lastBilledDate\": " + DateUtils.toInstant(lastBilledDate).map(Instant::toEpochMilli).orElse(null)
                + "}";
    }

    private static String dynamicCostRequest(long newCost) {
        return "{\"dynamicCost\":" + newCost * 100 + ", \"poiMillis\": null}";
    }


    private static String vendorDatasourceResponse(long vendorId, long datasourceId) {
        return ""
                + "{\"result\": {\"items\": ["
                + "  {"
                + "    \"vendorId\": " + vendorId + ","
                + "    \"datasourceId\": " + datasourceId
                + "  }"
                + "]}}";
    }

    private static String balanceVendorsResponse(Collection<Long> vendors) {
        return ""
                + "{\"result\": {\"item\": {\"roles\": {"
                + "    \"admin_user\": [0], \"balanceVendors\": [1001012],"
                + "    \"balance_client_user\": " + StreamEx.of(vendors).map(String::valueOf).joining(", ", "[", "]")
                + "}}}}";
    }
}
