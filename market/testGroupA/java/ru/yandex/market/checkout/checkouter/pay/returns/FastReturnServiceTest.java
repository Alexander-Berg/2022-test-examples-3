package ru.yandex.market.checkout.checkouter.pay.returns;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.SupplierType;
import ru.yandex.market.checkout.checkouter.pay.AbstractPaymentTestBase;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.helpers.ReturnHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.PickupOption;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.test.providers.ActualDeliveryProvider.PICKUP_PRICE;

public class FastReturnServiceTest extends AbstractPaymentTestBase {

    @Autowired
    private ReturnHelper returnHelper;

    private Return ret;

    @BeforeEach
    public void setUp() {
        freezeTime();
        returnHelper.mockShopInfo();
        returnHelper.mockSupplierInfo();
        mockActualDelivery();
    }

    @AfterEach
    public void tearDown() {
        clearFixed();
    }

    @Test
    public void shouldAllowFastReturn() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder());
        order.get().getItems().stream()
                .map(OrderItem::getSupplierId)
                .forEach(shopId -> shopService.updateMeta(shopId, ShopMetaDataBuilder.createTestDefault()
                        .withSupplierFastReturnEnabled(true)
                        .build()));
        assertThat("Сумма возврата должна быть меньше StorageReturnService.FAST_RETURN_THRESHOLD",
                order.get().getBuyerTotal(), lessThan(BigDecimal.valueOf(2000L)));
        Return returnRequest = ReturnProvider.generateReturn(order.get());
        ret = returnHelper.createReturn(order.get().getId(), returnRequest);
        assertTrue(ret.isFastReturn());
    }

    @Test
    public void shouldAllowFastReturnFor1p() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder(o -> o.getItems()
                .forEach(item -> item.setSupplierType(SupplierType.FIRST_PARTY))));
        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        assertTrue(ret.isFastReturn());
    }

    @Test
    public void shouldNotAllowFastReturnForExpensiveItemsTotal() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder(o -> {
            var item = o.getItems().iterator().next();
            item.setBuyerPrice(BigDecimal.valueOf(10000L));
            item.setQuantPrice(BigDecimal.valueOf(10000L));
        }));
        order.get().getItems().stream()
                .map(OrderItem::getSupplierId)
                .forEach(shopId -> shopService.updateMeta(shopId, ShopMetaDataBuilder.createTestDefault()
                        .withSupplierFastReturnEnabled(true)
                        .build()));
        assertThat("Сумма возврата должна быть больше StorageReturnService.FAST_RETURN_THRESHOLD",
                order.get().getBuyerTotal(), greaterThan(BigDecimal.valueOf(2000L)));
        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        assertFalse(ret.isFastReturn());
    }

    @Test
    public void shouldNotAllowFastReturnForUnsupportedMerchant() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder(o -> o.getItems().iterator().next()
                .setSupplierType(SupplierType.THIRD_PARTY)));
        order.get().getItems().stream()
                .map(OrderItem::getSupplierId)
                .forEach(shopId -> shopService.updateMeta(shopId, ShopMetaDataBuilder.createTestDefault()
                        .withSupplierFastReturnEnabled(false)
                        .build()));
        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        assertFalse(ret.isFastReturn());
    }

    @Test
    public void shouldNotAllowFastReturnForReturnReason() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder(o -> o.getItems()
                .forEach(item -> item.setSupplierType(SupplierType.FIRST_PARTY))));
        Return returnRequest = ReturnProvider.generateReturn(order.get());
        returnRequest.getItems().forEach(returnItem ->
                returnItem.setReasonType(ReturnReasonType.DO_NOT_FIT));
        ret = returnHelper.createReturn(order.get().getId(), returnRequest);
        assertFalse(ret.isFastReturn());
    }

    @Test
    public void shouldNotAllowFastReturnForUntrustedBuyer() {
        order.set(orderServiceTestHelper.createDeliveredBlueOrder(o -> o.getItems()
                .forEach(item -> item.setSupplierType(SupplierType.FIRST_PARTY))));
        mstatAntifraudConfigurer.mockUntrustedUser();

        ret = returnHelper.createReturn(order.get().getId(), ReturnProvider.generateReturn(order.get()));
        assertFalse(ret.isFastReturn());
    }

    private void mockActualDelivery() {
        Parameters parameters = new Parameters();
        PickupOption pickupOption = new PickupOption();
        pickupOption.setDeliveryServiceId(321L);
        pickupOption.setMarketPartner(true);
        pickupOption.setPrice(PICKUP_PRICE);
        pickupOption.setCurrency(Currency.RUR);
        parameters.getReportParameters().setActualDelivery(
                ActualDeliveryProvider.builder()
                        .addPickup(pickupOption)
                        .addDelivery(123L)
                        .addLargeSize(false)
                        .build());
        reportConfigurer.mockReportPlace(MarketReportPlace.ACTUAL_DELIVERY, parameters.getReportParameters());
    }
}
