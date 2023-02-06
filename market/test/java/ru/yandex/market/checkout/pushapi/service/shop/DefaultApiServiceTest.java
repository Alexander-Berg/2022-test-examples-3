package ru.yandex.market.checkout.pushapi.service.shop;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.http.conn.ConnectTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.shop.MigrationMapping;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsService;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.PushApiOrder;
import ru.yandex.market.checkout.pushapi.client.entity.StocksRequest;
import ru.yandex.market.checkout.pushapi.client.entity.StocksResponse;
import ru.yandex.market.checkout.pushapi.client.entity.stock.Stock;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.client.error.ShopErrorException;
import ru.yandex.market.checkout.pushapi.config.AsyncEndpointsExperimentService;
import ru.yandex.market.checkout.pushapi.service.RandomizeTokenService;
import ru.yandex.market.checkout.pushapi.service.RequestDetailsService;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.checkout.pushapi.shop.ApiSelectorUtil;
import ru.yandex.market.checkout.pushapi.shop.ShopApi;
import ru.yandex.market.checkout.pushapi.shop.ShopApiResponse;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;
import ru.yandex.market.checkout.pushapi.warehouse.WarehouseMappingCache;
import ru.yandex.market.sdk.userinfo.service.UidConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DefaultApiServiceTest {

    private RequestPublishService requestPublishService = mock(RequestPublishService.class);
    private SettingsService settingsService = mock(SettingsService.class);
    private RequestDetailsService requestDetailsService = mock(RequestDetailsService.class);
    private ValidateService validateService = mock(ValidateService.class);
    private RandomizeTokenService randomizeTokenService = mock(RandomizeTokenService.class);
    private ApiSelectorUtil apiSelectorUtil = mock(ApiSelectorUtil.class);
    private ShopApi thickJsonShopApiAsyncRestClient = mock(ShopApi.class);
    private ShopApi thinJsonShopApiAsyncRestClient = mock(ShopApi.class);
    private CheckouterShopMetaDataGetter checkouterShopMetaDataGetter = mock(CheckouterShopMetaDataGetter.class);
    private WarehouseMappingCache warehouseMappingCache = mock(WarehouseMappingCache.class);
    private StubService stubServiceMock = mock(StubService.class);
    private AsyncEndpointsExperimentService asyncEndpointsExperimentService
            = mock(AsyncEndpointsExperimentService.class);

    private DefaultApiService apiService = spy(new DefaultApiService());

    private long UID = 1234567890;
    final long shopId = 1234;
    final Settings settings = Settings.builder().build();

    final Cart cart = mock(Cart.class);
    final StocksRequest stocksRequest = mock(StocksRequest.class);
    final ExternalCart externalCart = mock(ExternalCart.class);
    final CartResponse cartResponse = mock(CartResponse.class);
    final ShopApiResponse<CartResponse> shopCartResponse = ShopApiResponse.fromBody(cartResponse);

    final Order order = mock(Order.class);
    final PushApiOrder pushApiOrder = mock(PushApiOrder.class);
    final ShopOrder shopOrder = mock(ShopOrder.class);
    final OrderResponse orderResponse = mock(OrderResponse.class);
    final ShopApiResponse<OrderResponse> shopOrderResponse = ShopApiResponse.fromBody(orderResponse);

    final ApiSelectorUtil.ApiSelection selection = new ApiSelectorUtil
            .ApiSelection("asdg", null, DataType.JSON);

    final ShopApiResponse<CartResponse> forbiddenResponse = new ShopApiResponse<>(
            null, null, new HttpClientErrorException(HttpStatus.FORBIDDEN)
    );

    private final RequestContext prodContext = new RequestContext(shopId, false, Context.MARKET,
            ApiSettings.PRODUCTION, null, null);
    private final RequestContext sandboxContext = new RequestContext(shopId, true, Context.MARKET,
            ApiSettings.SANDBOX, null, null);
    private final ShopMetaData shopMetaData = ShopMetaDataBuilder.createTestDefault().build();

    @BeforeEach
    public void setUp() throws Exception {
        apiService.setRequestPublishService(requestPublishService);
        apiService.setSettingsService(settingsService);
        apiService.setRequestDetailsService(requestDetailsService);
        apiService.setValidateService(validateService);
        apiService.setRandomizeTokenService(randomizeTokenService);
        apiService.setApiSelectorUtil(apiSelectorUtil);
        apiService.setCartResponsePostprocessors(new ArrayList<>());
        apiService.setClock(TestableClock.getInstance());
        apiService.setCheckouterShopMetaDataGetter(checkouterShopMetaDataGetter);
        apiService.setWarehouseMappingCache(warehouseMappingCache);
        apiService.setStubService(stubServiceMock);
        apiService.setAsyncEndpointsExperimentService(asyncEndpointsExperimentService);
        apiService.setThickJsonShopApiAsyncRestClient(thickJsonShopApiAsyncRestClient);
        apiService.setThinJsonShopApiAsyncRestClient(thinJsonShopApiAsyncRestClient);

        when(asyncEndpointsExperimentService.isExperimentShop(anyLong())).thenReturn(true);
        when(apiSelectorUtil.getApiUrl(eq(settings), anyString(), any())).thenReturn(selection);
        when(settingsService.getSettings(eq(shopId), anyBoolean())).thenReturn(settings);
        when(requestDetailsService.createExternalCartWithPersonalData(eq(shopId), eq(cart), any()))
                .thenReturn(CompletableFuture.completedFuture(externalCart));

        doReturn(pushApiOrder).when(apiService).createPushApi(eq(order));
        when(requestDetailsService.createShopOrderEnrichedWithPersonalData(eq(shopId), eq(pushApiOrder)))
                .thenReturn(CompletableFuture.completedFuture(shopOrder));
        when(requestDetailsService.createShopOrderStatus(shopId, pushApiOrder))
                .thenReturn(CompletableFuture.completedFuture(shopOrder));
        mockShopApiCart(settings, shopCartResponse);
        mockShopApiOrderAccept(shopOrderResponse);
        when(checkouterShopMetaDataGetter.getMeta(shopId)).thenReturn(shopMetaData);
    }

    @Test
    public void testOrderAcceptRequestSettingsForProduction() {
        RequestContext context = new RequestContext(shopId, true, Context.MARKET, ApiSettings.PRODUCTION, null,
                order.getId());
        apiService.orderAccept(context, order, true);
        verify(settingsService).getSettings(shopId, false);
    }

    @Test
    public void testOrderAcceptRequestSettingsForSandbox() {
        RequestContext context = new RequestContext(shopId, true, Context.MARKET, ApiSettings.SANDBOX, null,
                order.getId());
        apiService.orderAccept(context, order, true);
        verify(settingsService).getSettings(shopId, true);
    }

    @Test
    public void testConnectionTimedOutError() {
        assertShopApiErrorHandle(
                new ResourceAccessException(
                        "higher level timed out",
                        new ConnectTimeoutException("timed out")
                ),
                ErrorSubCode.CONNECTION_TIMED_OUT
        );
    }

    @Test
    public void testConnectionRefusedError() {
        assertShopApiErrorHandle(
                new ResourceAccessException(
                        "higher level connection refused",
                        new ConnectException("connection refused")
                ),
                ErrorSubCode.CONNECTION_REFUSED
        );
    }

    @Test
    public void testReadTimedOutError() {
        assertShopApiErrorHandle(
                new ResourceAccessException(
                        "higher level read timed out",
                        new SocketTimeoutException("read timed out")
                ),
                ErrorSubCode.READ_TIMED_OUT
        );
    }

    @Test
    public void testMessageParseError() {
        assertShopApiErrorHandle(
                new HttpMessageConversionException("can't parse something"),
                ErrorSubCode.CANT_PARSE_RESPONSE
        );
    }

    @Test
    public void testValidateError() {
        doThrow(new ValidationException("can't validate something"))
                .when(validateService)
                .validateCartResponse(null, cartResponse);

        final ErrorSubCode errorSubCode = ErrorSubCode.INVALID_DATA;

        try {
            apiService.cart(prodContext, UID, cart, true);
        } catch (ShopErrorException e) {
            assertEquals(errorSubCode, e.getCode());
            verify(requestPublishService).publishError(
                    prodContext, ApiService.RESOURCE_CART, errorSubCode, anyString(), shopCartResponse, any());
        }
    }

    @Test
    public void testRegularOrderAcceptResponse() throws ExecutionException, InterruptedException {
        final OrderResponse actual = apiService.orderAccept(prodContext, order, true).get();

        verify(requestDetailsService).createShopOrderEnrichedWithPersonalData(anyLong(), any());
        assertEquals(actual, orderResponse);
    }

    @Test
    public void testRegularStatusChange() {
        ShopApiResponse<Void> shopApiResponse = ShopApiResponse.fromBody(null);
        mockShopApiOrderStatus(shopApiResponse);
        apiService.orderStatus(prodContext, pushApiOrder, true);

        verify(requestPublishService).publishSuccess(eq(prodContext), eq(ApiService.RESOURCE_ORDER_STATUS),
                eq(shopApiResponse), any(), eq(true));
        verify(requestDetailsService).createShopOrderStatus(anyLong(), any());
    }

    @Test
    public void testCancellationNotify() {
        ShopApiResponse<Void> shopApiResponse = ShopApiResponse.fromBody(null);
        mockCancellationNotify(shopApiResponse);
        apiService.orderCancellationNotify(prodContext, order);

        verify(requestPublishService).publishSuccess(eq(prodContext), eq(ApiService.RESOURCE_ORDER_CANCELLATION_NOTIFY),
                eq(shopApiResponse), any(), anyBoolean());
    }

    @Test
    public void testUpdateShopSettings() {
        apiService.settings(shopId, settings, false);

        verify(settingsService).updateSettings(shopId, settings, false);
    }

    @Test
    public void testWrongTokenCart() {
        mockShopApiWrongCart();
        apiService.wrongTokenCart(prodContext, UID, cart);
        verify(requestPublishService).publishSuccess(
                eq(prodContext), eq(ApiService.RESOURCE_CART_WRONG_TOKEN), any(), any(), eq(true));
    }

    @Test
    public void testThrowsShopErrorIfShopIgnoredWrongToken() {
        mockShopApiWrongCart();
        try {
            apiService.wrongTokenCart(prodContext, UID, cart);
        } catch (ShopErrorException e) {
            assertEquals(ErrorSubCode.IGNORED_WRONG_TOKEN, e.getCode());
            verify(requestPublishService).publishError(eq(prodContext), eq(ApiService.RESOURCE_CART_WRONG_TOKEN),
                    eq(ErrorSubCode.IGNORED_WRONG_TOKEN), anyString(), any(), any());
        }
    }

    @Test
    public void testErrorInCartMethodStoresErrorWithRightRequestParam() {
        final ShopApiResponse<CartResponse> response = new ShopApiResponse<>(
                "request", "response", new HttpClientErrorException(HttpStatus.ACCEPTED)
        );
        mockShopApiCart(settings, response);
        try {
            apiService.cart(prodContext, UID, cart, true);
        } catch (ShopErrorException e) {
            verify(requestPublishService).publishError(eq(prodContext), eq(ApiService.RESOURCE_CART),
                    any(ErrorSubCode.class), anyString(), eq(response), any());
        }
    }

    @Test
    public void testErrorInOrderAcceptMethodStoresErrorWithRightRequestParam() {
        final ShopApiResponse<OrderResponse> response = new ShopApiResponse<>(
                "request", "response", new HttpClientErrorException(HttpStatus.ACCEPTED)
        );
        mockShopApiOrderAccept(response);
        try {
            apiService.orderAccept(prodContext, order, true);
        } catch (ShopErrorException e) {
            verify(requestPublishService).publishError(eq(prodContext), eq(ApiService.RESOURCE_ORDER_ACCEPT),
                    any(ErrorSubCode.class), anyString(), eq(response), any());
        }
    }

    @Test
    public void testSuccessCartCallStoresSuccess() {
        apiService.cart(prodContext, UID, cart, true);
        verify(requestPublishService).publishSuccess(eq(prodContext), eq(ApiService.RESOURCE_CART), any(), any(),
                eq(true));
    }

    @Test
    public void testSuccessOrderAcceptCallStoresSuccess() {
        apiService.orderAccept(prodContext, order, true);
        verify(requestPublishService).publishSuccess(eq(prodContext), eq(ApiService.RESOURCE_ORDER_ACCEPT),
                eq(shopOrderResponse), any(), eq(true));
    }

    @Test
    public void testSandboxWrongTokenStoresSandboxFlag() {
        mockShopApiWrongCart();
        apiService.wrongTokenCart(sandboxContext, UID, cart);

        verify(requestPublishService).publishSuccess(
                eq(sandboxContext), eq(ApiService.RESOURCE_CART_WRONG_TOKEN), any(), any(), eq(true));
    }

    @Test
    public void testSandboxOrderStatusStoresSandboxFlag() {
        ShopApiResponse<Void> shopApiResponse = ShopApiResponse.fromBody(null);
        mockShopApiOrderStatus(shopApiResponse);
        apiService.orderStatus(sandboxContext, pushApiOrder, true);

        verify(requestPublishService).publishSuccess(eq(sandboxContext), eq(ApiService.RESOURCE_ORDER_STATUS),
                eq(shopApiResponse), any(), eq(true));
    }

    @Test
    public void testSandboxCartStoresSandboxFlag() {
        mockShopApiCart(settings, shopCartResponse);
        apiService.cart(sandboxContext, UID, cart, true);
        verify(requestPublishService).publishSuccess(eq(sandboxContext), eq(ApiService.RESOURCE_CART),
                any(), any(), eq(true));
    }

    @Test
    public void testSandboxOrderAcceptStoresSandboxFlagToSuccessStore() {
        apiService.orderAccept(sandboxContext, order, true);

        verify(requestPublishService).publishSuccess(eq(sandboxContext), eq(ApiService.RESOURCE_ORDER_ACCEPT),
                eq(shopOrderResponse), any(), eq(true));
    }

    @Test
    public void testCartNotLogResponseFlag() {
        apiService.cart(prodContext, UID, cart, false);
        verify(requestPublishService).publishSuccess(eq(prodContext), eq(ApiService.RESOURCE_CART), any(), any(),
                eq(false));
    }

    @Test
    public void testCartLogResponseFlagIfSandbox() {
        apiService.cart(sandboxContext, UID, cart, false);
        verify(requestPublishService).publishSuccess(eq(sandboxContext), eq(ApiService.RESOURCE_CART), any(), any(),
                eq(true));
    }

    @Test
    public void testCartLogResponseFlagIfCheckOrder() {
        when(requestDetailsService.createExternalCartWithPersonalData(eq(shopId), eq(cart), any()))
                .thenReturn(CompletableFuture.completedFuture(externalCart));
        RequestContext context = new RequestContext(shopId, false, Context.CHECK_ORDER, ApiSettings.PRODUCTION, null,
                null);
        apiService.cart(context, UID, cart, false);
        verify(requestPublishService).publishSuccess(eq(context), eq(ApiService.RESOURCE_CART), any(), any(), eq(true));
    }

    @Test
    public void testCartLogResponseFlagIfForce() {
        Settings settings = Settings.builder().forceLogResponseUntil(Timestamp.valueOf("2029-12-01 00:00:00")).build();
        when(
                apiSelectorUtil.getApiUrl(eq(settings), anyString(), any())).thenReturn(selection);
        when(settingsService.getSettings(eq(shopId), anyBoolean())).thenReturn(settings);
        mockShopApiCart(settings, shopCartResponse);

        apiService.cart(prodContext, UID, cart, false);
        verify(requestPublishService).publishSuccess(eq(prodContext), eq(ApiService.RESOURCE_CART), any(), any(),
                eq(true));
    }

    @Test
    public void testCartLogResponseFlagIfForcingExpired() {
        Settings settings = Settings.builder().forceLogResponseUntil(Timestamp.valueOf("2018-12-01 00:00:00")).build();
        when(apiSelectorUtil.getApiUrl(eq(settings), anyString(), any())).thenReturn(selection);
        when(settingsService.getSettings(eq(shopId), anyBoolean())).thenReturn(settings);
        mockShopApiCart(settings, shopCartResponse);

        apiService.cart(prodContext, UID, cart, false);
        verify(requestPublishService).publishSuccess(eq(prodContext), eq(ApiService.RESOURCE_CART), any(), any(),
                eq(false));
    }

    @Test
    public void testCartIfDisabledByShopMetaData() {
        when(apiSelectorUtil.getApiUrl(eq(settings), anyString(), any()))
                .thenReturn(new ApiSelectorUtil
                        .ApiSelection("stub-url", null, DataType.XML));
        when(settingsService.getSettings(eq(shopId), anyBoolean())).thenReturn(settings);
        when(checkouterShopMetaDataGetter.getMeta(shopId)).thenReturn(
                ShopMetaDataBuilder.createTestDefault().withCartRequestTurnedOff(true).build());

        mockShopApiCart(settings, ShopApiResponse.fromBody(null));

        apiService.cart(prodContext, UID, cart, false);
    }

    @Test
    public void testQueryStocks() throws ExecutionException, InterruptedException {
        Long donorWarehouseId = 1L;
        final Stock stock = new Stock();
        stock.setWarehouseId(donorWarehouseId);
        final StocksResponse stocksResponse = new StocksResponse();
        stocksResponse.setSkus(List.of(stock));
        ShopApiResponse<StocksResponse> shopApiResponse = ShopApiResponse.fromBody(stocksResponse);
        mockShopApiQueryStocks(shopApiResponse);
        final StocksResponse stocks = apiService.queryStocks(prodContext, stocksRequest, false).get();
        assertEquals(stocks.getSkus().get(0).getWarehouseId(), donorWarehouseId);
    }

    @Test
    public void testQueryStocksWithMappings() throws ExecutionException, InterruptedException {
        ShopMetaData mockMeta = mock(ShopMetaData.class);
        MigrationMapping migrationMapping = mock(MigrationMapping.class);
        Long donorWarehouseId = 1L;
        Long sourcePartnerId = 2L;
        Stock stock = new Stock();
        stock.setWarehouseId(donorWarehouseId);
        StocksResponse stocksResponse = new StocksResponse();
        stocksResponse.setSkus(List.of(stock));
        ShopApiResponse<StocksResponse> shopApiResponse = ShopApiResponse.fromBody(stocksResponse);
        mockShopApiQueryStocks(shopApiResponse);
        when(stocksRequest.getWarehouseId()).thenReturn(sourcePartnerId);
        when(mockMeta.getMigrationMapping()).thenReturn(migrationMapping);
        when(checkouterShopMetaDataGetter.getMeta(anyLong())).thenReturn(mockMeta);
        final StocksResponse stocks = apiService.queryStocks(prodContext, stocksRequest, false).get();
        assertEquals(stocks.getSkus().get(0).getWarehouseId(), sourcePartnerId);
    }

    @Test
    public void testSkipOnLoadTestOrderCart() throws ExecutionException, InterruptedException {
        RequestContext context = new RequestContext(shopId, false, Context.MARKET, ApiSettings.PRODUCTION, null, null);
        when(cart.getBuyer()).thenReturn(new Buyer(UidConstants.NO_SIDE_EFFECT_UID));
        when(stubServiceMock.cart(any(), any())).thenReturn(cartResponse);
        final CartResponse actual = apiService
                .cart(context, UidConstants.NO_SIDE_EFFECT_UID, cart, true).get();
        verifyNoInteractions(
                thickJsonShopApiAsyncRestClient,
                thinJsonShopApiAsyncRestClient
        );
        verify(stubServiceMock, times(1)).cart(any(), any());
        assertEquals(cartResponse, actual);
    }

    @Test
    public void testSkipOnLoadTestOrderAccept() throws ExecutionException, InterruptedException {
        when(order.getBuyer()).thenReturn(new Buyer(UidConstants.NO_SIDE_EFFECT_UID));
        when(stubServiceMock.orderAccept(anyLong(), any())).thenReturn(orderResponse);
        RequestContext context = new RequestContext(shopId, false, Context.MARKET, ApiSettings.PRODUCTION, null,
                order.getId());
        OrderResponse actual = apiService.orderAccept(context, order, true).get();
        verify(settingsService).getSettings(shopId, false);
        verifyNoInteractions(
                thickJsonShopApiAsyncRestClient,
                thinJsonShopApiAsyncRestClient
        );
        verify(stubServiceMock, times(1)).orderAccept(anyLong(), any());
        assertEquals(orderResponse, actual);
    }

    @Test
    public void testSkipOnLoadTestOrderStatus() {
        ShopApiResponse<Void> shopApiResponse = ShopApiResponse.fromBody(null);
        mockShopApiOrderStatus(shopApiResponse);
        when(pushApiOrder.getBuyer()).thenReturn(new Buyer(UidConstants.NO_SIDE_EFFECT_UID));
        apiService.orderStatus(prodContext, pushApiOrder, true);
        verifyNoInteractions(
                thickJsonShopApiAsyncRestClient,
                thinJsonShopApiAsyncRestClient
        );
        verify(stubServiceMock, times(1)).orderStatus(anyLong(), any());
    }

    private void assertShopApiErrorHandle(Exception exception, ErrorSubCode errorSubCode) {
        RequestContext context = new RequestContext(shopId, false, Context.MARKET, ApiSettings.PRODUCTION, null, null);
        try {
            apiService.cart(context, UID, cart, true);
        } catch (ShopErrorException e) {
            assertEquals(errorSubCode, e.getCode());
            verify(requestPublishService).publishError(
                    eq(context), anyString(), eq(errorSubCode), anyString(), isNull(ShopApiResponse.class), any());
        }
    }

    private void mockShopApiCart(Settings settings, ShopApiResponse<CartResponse> response) {
        when(thickJsonShopApiAsyncRestClient.cart(eq(settings), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(thinJsonShopApiAsyncRestClient.cart(eq(settings), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(response));
    }

    private void mockShopApiOrderAccept(ShopApiResponse<OrderResponse> response) {
        when(thickJsonShopApiAsyncRestClient.orderAccept(eq(settings), eq(shopOrder), eq(selection), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(response));
        when(thinJsonShopApiAsyncRestClient.orderAccept(eq(settings), eq(shopOrder), eq(selection), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(response));
    }

    private void mockShopApiOrderStatus(ShopApiResponse<Void> shopApiResponse) {
        when(thickJsonShopApiAsyncRestClient.changeOrderStatus(eq(settings), eq(shopOrder), eq(selection), any()))
                .thenReturn(CompletableFuture.completedFuture(shopApiResponse));
        when(thinJsonShopApiAsyncRestClient.changeOrderStatus(eq(settings), eq(shopOrder), eq(selection), any()))
                .thenReturn(CompletableFuture.completedFuture(shopApiResponse));
    }

    private void mockShopApiQueryStocks(ShopApiResponse<StocksResponse> shopApiResponse) {
        when(thickJsonShopApiAsyncRestClient.queryStocks(any(Settings.class), any(StocksRequest.class),
                any(ApiSelectorUtil.ApiSelection.class), any()))
                .thenReturn(CompletableFuture.completedFuture(shopApiResponse));
        when(thinJsonShopApiAsyncRestClient.queryStocks(any(Settings.class), any(StocksRequest.class),
                any(ApiSelectorUtil.ApiSelection.class), any()))
                .thenReturn(CompletableFuture.completedFuture(shopApiResponse));
    }

    private void mockShopApiWrongCart() {
        final Settings newSettings = mock(Settings.class);
        when(randomizeTokenService.randomizeToken(settings)).thenReturn(newSettings);
        mockShopApiCart(newSettings, forbiddenResponse);
    }

    private void mockCancellationNotify(ShopApiResponse<Void> shopApiResponse) {
        when(thickJsonShopApiAsyncRestClient.notifyOrderCancellation(eq(settings), eq(shopOrder), eq(selection), any()))
                .thenReturn(CompletableFuture.completedFuture(shopApiResponse));
        when(thinJsonShopApiAsyncRestClient.notifyOrderCancellation(eq(settings), eq(shopOrder), eq(selection), any()))
                .thenReturn(CompletableFuture.completedFuture(shopApiResponse));
    }
}
