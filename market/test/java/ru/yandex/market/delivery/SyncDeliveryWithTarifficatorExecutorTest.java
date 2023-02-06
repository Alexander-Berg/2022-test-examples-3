package ru.yandex.market.delivery;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

@DbUnitDataSet(
        before = "syncDeliveryTariffWithTarifficatorExecutor.before.csv"
)
public class SyncDeliveryWithTarifficatorExecutorTest extends FunctionalTest {

    @Autowired
    private SyncDeliveryWithTarifficatorExecutor syncDsbsDeliveryTariffsWithTarificator;
    @Autowired
    private WireMockServer tarificatorWireMockServer;

    @BeforeEach
    private void beforeEach() {
        tarificatorWireMockServer.resetMappings();
        tarificatorWireMockServer.removeServeEventsMatching(RequestPattern.everything());
    }

    @Test
    @DbUnitDataSet(after = "syncDeliveryTariffWithTarifficatorExecutor.success.after.csv")
    void testSuccessfulExecution() {
        prepareMockForShop(1000L, true);
        prepareMockForShop(2000L, true);

        syncDsbsDeliveryTariffsWithTarificator.doJob(null);
    }

    @Test
    @DbUnitDataSet(after = "syncDeliveryTariffWithTarifficatorExecutor.partial.after.csv")
    void testErrorOnExecution() {
        prepareMockForShop(1000L, true);
        prepareMockForShop(2000L, false);

        assertThatThrownBy(() -> syncDsbsDeliveryTariffsWithTarificator.doJob(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to sync delivery for 1 shops");
    }

    private void prepareMockForShop(long shopId, boolean successful) {
        ResponseDefinitionBuilder metaDateResponse = aResponse().withStatus(successful ? 200 : 400)
                .withBody(getString(this.getClass(), "metaData.response.json"));

        tarificatorWireMockServer.stubFor(get("/v2/shops/" + shopId + "/region-groups")
                .willReturn(aResponse().withStatus(200).withBody(getString(this.getClass(), "getRegionGroups.response" +
                        ".json"))));
        tarificatorWireMockServer.stubFor(post("/v2/shops/" + shopId + "/meta?_user_id=11").willReturn(metaDateResponse));
    }

}
