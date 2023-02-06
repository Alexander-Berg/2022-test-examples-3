package ru.yandex.market.checkout.pushapi.web;

import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.pushapi.settings.DataType;

/**
 * @author mmetlov
 */
public class ParcelStatusJSONTest extends AbstractShopWebTestBase {

    public ParcelStatusJSONTest() {
        super(DataType.JSON);
    }

    @Test
    public void shouldSendShipmentToShop() throws Exception {
        performOrderShipmentStatusOldFormat(SHOP_ID);
        assertShopAdminRequestHasShipment();
    }

    @Test
    public void testSendShipmentToShopOnMultishipmentRequest() throws Exception {
        performOrderShipmentStatusNewFormat(SHOP_ID);
        assertShopAdminRequestHasShipment();
    }

    private void assertShopAdminRequestHasShipment() {
        shopadminStubMock.verify(
                RequestPatternBuilder.newRequestPattern()
                        .withUrl("/svn-shop/774/order/shipment/status")
                        .withRequestBody(new ContainsPattern("\"shipments\":[")));
    }
}
