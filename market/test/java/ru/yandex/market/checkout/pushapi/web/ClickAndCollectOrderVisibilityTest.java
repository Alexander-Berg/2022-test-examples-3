package ru.yandex.market.checkout.pushapi.web;

import java.io.IOException;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.shop.OrderVisibility;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderAcceptHelper;
import ru.yandex.market.checkout.pushapi.helpers.PushApiOrderParameters;
import ru.yandex.market.checkout.util.shopapi.ShopApiConfigurer;
import ru.yandex.market.request.trace.RequestContextHolder;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_UID;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES;

public class ClickAndCollectOrderVisibilityTest extends AbstractOrderVisibilityTestBase {

    @Autowired
    private PushApiOrderAcceptHelper orderAcceptHelper;
    @Autowired
    private ShopApiConfigurer shopApiConfigurer;

    private static final DeliveryType deliveryType = DeliveryType.DELIVERY;
    private PushApiOrderParameters parameters;
    private Map<OrderVisibility, Boolean> orderVisibilityMap;

    @BeforeEach
    public void setUpLocal() {
        parameters = new PushApiOrderParameters();
        prepareDelivery(parameters.getOrder().getDelivery(), deliveryType);
        prepareClickAndCollectOrder(parameters.getOrder());
        RequestContextHolder.createNewContext();
        shopApiConfigurer.mockOrderResponse(parameters);
    }

    @Test
    public void orderAcceptWithNullVisibilityMap() throws Exception {
        // Arrange
        orderVisibilityMap = null;
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        // Act
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        // Assert
        checkOrderVisibility(false);
    }

    @Test
    public void orderAcceptWithEmptyVisibilityMap() throws Exception {
        // Arrange
        orderVisibilityMap = emptyMap();
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        // Act
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        // Assert
        checkOrderVisibility(false);
    }

    @Test
    public void orderAcceptWithBuyerInVisibilityMap() throws Exception {
        // Arrange
        orderVisibilityMap = singletonMap(BUYER, true);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        // Act
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        // Assert
        checkOrderVisibility(false);
    }

    @Test
    public void orderAcceptWithIgnoreDataHiding() throws Exception {
        // Arrange
        orderVisibilityMap = ImmutableMap.of(BUYER, true, IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES, true);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        // Act
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        // Assert
        checkOrderVisibility(true);
    }

    @Test
    public void orderAcceptWithoutBuyerUid() throws Exception {
        // Arrange
        orderVisibilityMap = ImmutableMap.of(
                BUYER, true,
                IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES, true,
                BUYER_UID, false);
        prepareShopMetaData(parameters.getShopId(), orderVisibilityMap);

        // Act
        mockSettingsForDifferentParameters(parameters);
        orderAcceptHelper.orderAcceptForActions(parameters);

        // Assert
        checkOrderVisibility(true);
    }

    private void checkOrderVisibility(boolean showBuyer) throws IOException {
        checkOrderVisibility(false, showBuyer, deliveryType, orderVisibilityMap);
    }
}
