package ru.yandex.market.abo.core.checkorder.scenario.runner;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioError;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioErrorType;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioType;
import ru.yandex.market.abo.core.checkorder.CheckOrderCreationException;
import ru.yandex.market.abo.core.checkorder.CheckOrderOffersProvider;
import ru.yandex.market.abo.core.checkorder.CheckOrderService;
import ru.yandex.market.abo.core.checkorder.model.CheckOrderScenario;
import ru.yandex.market.abo.core.checkorder.scenario.runner.blue.RejectedByPartnerScenarioRunner;
import ru.yandex.market.abo.core.checkorder.scenario.runner.blue.WarehouseRegionService;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeatureListItem;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerIndexState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioErrorType.NO_OFFERS;
import static ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioErrorType.ORDER_NOT_REJECTED;

/**
 * @author artemmz
 * @date 11/11/2019.
 */
class RejectedByApiScenarioRunnerTest {
    private static final Long SHOP_ID = 3424234L;

    RejectedByPartnerScenarioRunner runner;

    @Mock
    CheckOrderService checkOrderService;
    @Mock
    CheckOrderOffersProvider checkOrderOffersProvider;
    @Mock
    CheckOrderScenarioError scenarioError;
    @Mock
    CheckOrderScenario scenario;
    @Mock
    MultiCart cart;
    @Mock
    Order orderMock;
    @Mock
    OrderItem orderItem;
    @Mock
    MbiApiService mbiApiService;
    @Mock
    WarehouseRegionService warehouseRegionService;
    @Mock
    ShopFeatureListItem shopFeature;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        runner = new RejectedByPartnerScenarioRunner(
                checkOrderService, mbiApiService, warehouseRegionService,
                new RejectedByApiStrategy(checkOrderService), null, checkOrderOffersProvider, null);
        when(scenario.getType()).thenReturn(CheckOrderScenarioType.REJECTED_BY_PARTNER);
        when(scenario.getStatus()).thenReturn(CheckOrderScenarioStatus.NEW);
        when(scenario.getShopId()).thenReturn(SHOP_ID);

        when(mbiApiService.getShopWithFeature(SHOP_ID, FeatureType.DROPSHIP)).thenReturn(shopFeature);
        when(shopFeature.isCpaPartnerInterface()).thenReturn(false);

        when(cart.getCarts()).thenReturn(List.of(orderMock));
        when(orderMock.getItems()).thenReturn(List.of(orderItem));
        when(mbiApiService.getIndexState(SHOP_ID)).thenReturn(new PartnerIndexState(SHOP_ID, false, true));
        when(warehouseRegionService.chooseRegionForCheckOrder(anyLong())).thenReturn(Optional.empty());
    }

    @Test
    void cartApiError() throws CheckOrderCreationException {
        doThrow(new CheckOrderCreationException(scenarioError))
                .when(checkOrderService).createCartAndValidate(any(), any());
        when(scenarioError.getErrorType()).thenReturn(CheckOrderScenarioErrorType.CART_PROBLEMS);
        assertEquals(CheckOrderScenarioStatus.SUCCESS, runner.initOrder(scenario));
    }

    @Test
    void cartOtherError() throws CheckOrderCreationException {
        doThrow(new CheckOrderCreationException(scenarioError))
                .when(checkOrderService).createCartAndValidate(any(), any());
        when(scenarioError.getErrorType()).thenReturn(NO_OFFERS);

        assertEquals(CheckOrderScenarioStatus.FAIL, runner.initOrder(scenario));
        verify(scenario).setErrorType(scenarioError.getErrorType());
        verify(scenario).withErrorDetails(scenarioError.getErrorDetails());
    }

    @Test
    void cartActualizedWithNonZeroCount() throws CheckOrderCreationException {
        doReturn(cart).when(checkOrderService).createCartAndValidate(any(), any());
        when(orderItem.getCount()).thenReturn(1);

        assertEquals(CheckOrderScenarioStatus.FAIL, runner.initOrder(scenario));
        verify(scenario).setErrorType(ORDER_NOT_REJECTED);
    }

    @Test
    void cartActualizedWithZeroCount() throws CheckOrderCreationException {
        doReturn(cart).when(checkOrderService).createCartAndValidate(any(), any());
        when(orderItem.getCount()).thenReturn(0);

        assertEquals(CheckOrderScenarioStatus.SUCCESS, runner.initOrder(scenario));
    }

    @Test
    void cartActualizedWithEmptyItemsCount() throws CheckOrderCreationException {
        doReturn(cart).when(checkOrderService).createCartAndValidate(any(), any());
        when(orderMock.getItems()).thenReturn(List.of());

        assertEquals(CheckOrderScenarioStatus.SUCCESS, runner.initOrder(scenario));
    }

    @Test
    void cartActualizedWithNullItemsCount() throws CheckOrderCreationException {
        doReturn(cart).when(checkOrderService).createCartAndValidate(any(), any());
        when(orderMock.getItems()).thenReturn(null);

        assertEquals(CheckOrderScenarioStatus.SUCCESS, runner.initOrder(scenario));
    }
}
