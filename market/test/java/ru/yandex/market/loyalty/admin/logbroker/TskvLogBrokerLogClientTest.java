package ru.yandex.market.loyalty.admin.logbroker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.model.PaymentType;
import ru.yandex.market.loyalty.api.model.coin.CoinStatus;
import ru.yandex.market.loyalty.api.model.discount.MultiCartWithBundlesDiscountRequest;
import ru.yandex.market.loyalty.core.logbroker.EmissionEvent;
import ru.yandex.market.loyalty.core.logbroker.TskvLogBrokerLogClient;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.trigger.TriggerMapper;
import ru.yandex.market.loyalty.core.utils.DeliveryRequestUtils;
import ru.yandex.market.loyalty.core.utils.DiscountRequestWithBundlesBuilder;
import ru.yandex.market.loyalty.core.utils.OperationContextFactory;
import ru.yandex.market.sdk.userinfo.service.UidConstants;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.MARKET_WAREHOUSE_ID;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.itemKey;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.orderRequestWithBundlesBuilder;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.price;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.warehouse;

public class TskvLogBrokerLogClientTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    @TriggerMapper
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("tskvObjectMapper")
    private ObjectMapper tskvObjectMapper;

    private TskvLogBrokerLogClient client;

    private Logger logMock;

    @Before
    public void init() {
        if (logMock == null) {
            logMock = mock(Logger.class);

            client = new TskvLogBrokerLogClient(clock, objectMapper, ImmutableMap.of(EmissionEvent.class,
                    "emissionEvents")) {
                @Override
                protected Logger getLogger(String name) {
                    return logMock;
                }
            };
        } else {
            reset(logMock);
        }
    }

    @Test
    public void testAllFieldsIncluded() {
        EmissionEvent event = EmissionEvent
                .builder()
                .setCoinType(CoreCoinType.FIXED)
                .setPromoId(100L)
                .setStatus(CoinStatus.ACTIVE)
                .setRequireAuth(false)
                .setCoinKey(new CoinKey(0L))
                .setTime(null)
                .setPlatform(CoreMarketPlatform.BLUE)
                .setReason(CoreCoinCreationReason.ORDER)
                .build();

        client.pushEvent(event);

        then(logMock).should(only()).info(eq("tskv\tdate=" + date() + "\tpromo_id=100\tstatus=ACTIVE\tcoin_id=0" +
                "\tcoin_type=FIXED\trequire_auth=false\treason=ORDER\tplatform=BLUE"));
    }

    @Test
    public void testNotOmmitEmptyValues() {
        EmissionEvent event = EmissionEvent
                .builder()
                .setCoinKey(null)
                .setCoinType(null)
                .setPromoId(null)
                .setStatus(null)
                .setTime(null)
                .setReason(CoreCoinCreationReason.ORDER)
                .setRequireAuth(null)
                .build();

        client.pushEvent(event);

        then(logMock).should(only()).info(eq("tskv\tdate=" + date() + "\tpromo_id=\tstatus=\tcoin_id=\tcoin_type" +
                "=\trequire_auth=\treason=ORDER\tplatform="));
    }

    @Test
    public void shouldPrintOnlySelectedDeliveryOption() throws JsonProcessingException {
        MultiCartWithBundlesDiscountRequest request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                        .withPaymentType(PaymentType.BANK_CARD)
                        .withDeliveries(DeliveryRequestUtils.courierDelivery())
                        .build())
                .withOperationContext(OperationContextFactory.withUidBuilder(UidConstants.NO_SIDE_EFFECTS_RANGE.lowerEndpoint()).buildOperationContext())
                .withBnplSelected(true)
                .build();

        String json = tskvObjectMapper.writeValueAsString(request);
        MultiCartWithBundlesDiscountRequest parsed = tskvObjectMapper.readValue(json,
                MultiCartWithBundlesDiscountRequest.class);
        assertThat(parsed.getOrders().get(0).getDeliveries().size(), equalTo(0));

        request = DiscountRequestWithBundlesBuilder
                .builder(orderRequestWithBundlesBuilder()
                        .withOrderItem(warehouse(MARKET_WAREHOUSE_ID), itemKey(DEFAULT_ITEM_KEY), price(100))
                        .withPaymentType(PaymentType.BANK_CARD)
                        .withDeliveries(DeliveryRequestUtils.courierDelivery(DeliveryRequestUtils.SELECTED))
                        .build())
                .withOperationContext(OperationContextFactory.withUidBuilder(UidConstants.NO_SIDE_EFFECTS_RANGE.lowerEndpoint()).buildOperationContext())
                .withBnplSelected(true)
                .build();
        json = tskvObjectMapper.writeValueAsString(request);
        parsed = tskvObjectMapper.readValue(json, MultiCartWithBundlesDiscountRequest.class);
        assertThat(parsed.getOrders().get(0).getDeliveries().size(), equalTo(1));

    }

    public String date() {
        return client.format(clock.instant().atZone(clock.getZone()));
    }
}
