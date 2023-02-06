package ru.yandex.market.shopadminstub.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.common.report.indexer.model.OfferDetails;
import ru.yandex.market.shopadminstub.errors.ReportException;
import ru.yandex.market.shopadminstub.model.CartParameters;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.shopadminstub.model.Item;
import ru.yandex.market.shopadminstub.services.report.GeoReportService;
import ru.yandex.market.shopadminstub.services.report.OfferInfoService;
import ru.yandex.market.shopadminstub.test.AbstractMockedUnitTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static ru.yandex.common.util.currency.Currency.RUR;

/**
 * todo refactor + use builders
 *
 * @author Nicolai Iusiumbeli <armor@yandex-team.ru>
 * date: 09/02/2017
 */
public class CartServiceImplTest extends AbstractMockedUnitTestBase {

    private CartService cartService;
    private OfferInfoService offerInfoService;
    private GeoReportService geoReportService;

    private final long shopId = 123L;
    private final int regionId = 3;
    private Item item;
    private Map<OfferItemKey, Item> items;

    @BeforeEach
    public void setUp() throws Exception {
        offerInfoService = createMock(OfferInfoService.class);
        geoReportService = createMock(GeoReportService.class);
        var executorService = Executors.newFixedThreadPool(10);
        cartService = new CartServiceImpl(offerInfoService, geoReportService, executorService);

        doNothing().when(offerInfoService).fetchReport(anyInt(), any());
        doNothing().when(geoReportService).fetchGeo(anyInt(), any(), any());

        item = buildItem();
        items = new HashMap<>();
        items.put(item.getOfferItemKey(), item);
    }

    private Item buildItem() {
        Item item = new Item();
        item.setPrice(new BigDecimal("888.77"));
        item.setCount(10);
        item.setFeedId(11L);
        item.setOfferId("12");
        return item;
    }

    @Test
    public void actualReportTest() {
        OfferDetails offerDetails = new OfferDetails(null, null, 888.77, false, item.getFeedId().intValue(),
                item.getOfferId());
        offerDetails.setAvailable(true);

        CartParameters cartParameters = cartService.actualizeCartParameters(shopId, buildRequest(items, 2, "RUR"));

        for (Map.Entry<OfferItemKey, Item> entry : cartParameters.getItems().entrySet()) {
            assertThat(entry.getValue().getCount(), equalTo(10));
            assertThat(entry.getValue().getPrice(), equalTo(new BigDecimal("888.77")));
        }
    }

    @Test
    public void outdatedAvailabilityTest() {
        OfferDetails offerDetails = new OfferDetails(null, null, 888.77, false, item.getFeedId().intValue(),
                item.getOfferId());
        offerDetails.setAvailable(false);

        CartParameters cartParameters = cartService.actualizeCartParameters(shopId, buildRequest(items, 2, "RUR"));

        for (Map.Entry<OfferItemKey, Item> entry : cartParameters.getItems().entrySet()) {
            assertThat(entry.getValue().getCount(), equalTo(10));
            assertThat(entry.getValue().getPrice(), equalTo(new BigDecimal("888.77")));
        }
    }

    @Test
    public void brokenFeedDispatcherTest() {
        CartParameters cartParameters = cartService.actualizeCartParameters(shopId, buildRequest(items, 2, "RUR"));

        for (Map.Entry<OfferItemKey, Item> entry : cartParameters.getItems().entrySet()) {
            assertThat(entry.getValue().getCount(), equalTo(10));
            assertThat(entry.getValue().getPrice(), equalTo(new BigDecimal("888.77")));
        }
    }

    @Test
    public void brokenReportTest() {
        Assertions.assertThrows(ReportException.class, () -> {
            doThrow(IOException.class).when(offerInfoService).fetchReport(anyInt(), any());

            CartParameters cartParameters = cartService.actualizeCartParameters(shopId, buildRequest(items, 2, "RUR"));

            for (Map.Entry<OfferItemKey, Item> entry : cartParameters.getItems().entrySet()) {
                assertThat(entry.getValue().getCount(), equalTo(10));
                assertThat(entry.getValue().getPrice(), equalTo(new BigDecimal("888.77")));
            }
        });
    }

    private CartRequest buildRequest(Map<OfferItemKey, Item> items, Integer deliveryRegionId, String currency) {
        CartRequest cartRequest = new CartRequest();
        cartRequest.setRegionId(regionId);
        cartRequest.setDeliveryRegionId(deliveryRegionId);
        cartRequest.setCurrency(currency);
        cartRequest.getItems().putAll(items);
        return cartRequest;
    }
}
