package ru.yandex.market.checkout.pushapi.service.shop;

import org.apache.http.conn.ConnectTimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.client.entity.shop.SettingsBuilder;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.client.error.ShopErrorException;
import ru.yandex.market.checkout.pushapi.service.RandomizeTokenService;
import ru.yandex.market.checkout.pushapi.service.RequestDetailsService;
import ru.yandex.market.checkout.pushapi.service.shop.settings.SettingsService;
import ru.yandex.market.checkout.pushapi.shop.ShopApiResponse;
import ru.yandex.market.checkout.pushapi.shop.ShopApiRestClient;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.notNull;

public class DefaultApiServiceTest {
    
    private ShopApiRestClient xmlShopApiRestClient = mock(ShopApiRestClient.class);
    private ShopApiRestClient jsonShopApiRestClient = mock(ShopApiRestClient.class);
    private ErrorsStoreService errorsStoreService = mock(ErrorsStoreService.class);
    private SettingsService settingsService = mock(SettingsService.class);
    private RequestDetailsService requestDetailsService = mock(RequestDetailsService.class);
    private ValidateService validateService = mock(ValidateService.class);
    private RandomizeTokenService randomizeTokenService = mock(RandomizeTokenService.class);
    
    private DefaultApiService apiService = new DefaultApiService();

    private long UID = 1234567890;

    @Before
    public void setUp() throws Exception {
        apiService.setErrorsStoreService(errorsStoreService);
//        apiService.setJsonShopApiRestClient(jsonShopApiRestClient);
//        apiService.setXmlShopApiRestClient(xmlShopApiRestClient);
        apiService.setSettingsService(settingsService);
        apiService.setRequestDetailsService(requestDetailsService);
        apiService.setValidateService(validateService);
        apiService.setRandomizeTokenService(randomizeTokenService);

    }

    @Test
    public void testXmlShopReturnsRegularCartResponse() throws Exception {
        final long shopId = 1234;
        final Settings settings = new SettingsBuilder().build();

        final Cart cart = mock(Cart.class);
        final ExternalCart externalCart = mock(ExternalCart.class);
        final CartResponse cartResponse = mock(CartResponse.class);

        when(settingsService.getSettings(shopId)).thenReturn(settings);
        when(requestDetailsService.createExternalCart(cart)).thenReturn(externalCart);

        when(xmlShopApiRestClient.cart(settings, externalCart)).thenReturn(
            new ShopApiResponse<>(cartResponse, null, null)
        );

        final CartResponse actual = apiService.cart(shopId, 1, cart, false);
        assertEquals(cartResponse, actual);
    }

    @Test
    public void testJsonShopReturnsRegularCartResponse() throws Exception {
        final long shopId = 1234;
        final Settings settings = new SettingsBuilder().build();

        final Cart cart = mock(Cart.class);
        final ExternalCart externalCart = mock(ExternalCart.class);
        final CartResponse cartResponse = mock(CartResponse.class);

        when(settingsService.getSettings(shopId)).thenReturn(settings);
        when(requestDetailsService.createExternalCart(cart)).thenReturn(externalCart);

        when(jsonShopApiRestClient.cart(settings, externalCart)).thenReturn(
            new ShopApiResponse<>(cartResponse, null, null)
        );

        final CartResponse actual = apiService.cart(shopId, 1, cart, false);
        assertEquals(cartResponse, actual);
    }

    @Test
    public void testConnectionTimedOutError() throws Exception {
        assertShopApiErrorHandle(
            new ResourceAccessException(
                "higher level timed out",
                new ConnectTimeoutException("timed out")
            ),
            ErrorSubCode.CONNECTION_TIMED_OUT
        );
    }

    @Test
    public void testConnectionRefusedError() throws Exception {
        assertShopApiErrorHandle(
            new ResourceAccessException(
                "higher level connection refused",
                new ConnectException("connection refused")
            ),
            ErrorSubCode.CONNECTION_REFUSED
        );
    }

    @Test
    public void testReadTimedOutError() throws Exception {
        assertShopApiErrorHandle(
            new ResourceAccessException(
                "higher level read timed out",
                new SocketTimeoutException("read timed out")
            ),
            ErrorSubCode.READ_TIMED_OUT
        );
    }

    @Test
    public void testMessageParseError() throws Exception {
        assertShopApiErrorHandle(
            new HttpMessageConversionException("can't parse something"),
            ErrorSubCode.CANT_PARSE_RESPONSE
        );
    }

    @Test
    public void testValidateError() throws Exception {
        final long shopId = 1234;

        final Cart cart = mock(Cart.class);
        final ExternalCart externalCart = mock(ExternalCart.class);
        final CartResponse cartResponse = mock(CartResponse.class);

        when(settingsService.getSettings(shopId)).thenReturn(new SettingsBuilder().build());
        when(requestDetailsService.createExternalCart(cart)).thenReturn(externalCart);
        when(xmlShopApiRestClient.cart(any(Settings.class), eq(externalCart))).thenReturn(
            new ShopApiResponse<>(cartResponse, null, null)
        );
        doThrow(new ValidationException("can't validate something"))
            .when(validateService)
            .validateCartResponse(null, cartResponse);

        final ErrorSubCode errorSubCode = ErrorSubCode.INVALID_DATA;
        
        try {
            apiService.cart(shopId, 1, cart, false);
            fail();
        } catch(ShopErrorException e) {
            assertEquals(errorSubCode, e.getCode());
            verify(errorsStoreService).storeError(
                eq(shopId), any(String.class), eq(errorSubCode), anyString(), eq(false), any(ShopApiResponse.class));
        }
    }

    @Test
    public void testRegularOrderAcceptResponse() throws Exception {
        final long shopId = 1234;
        final Settings settings = new SettingsBuilder().build();

        final Order order = mock(Order.class);
        final ShopOrder shopOrder = mock(ShopOrder.class);
        final OrderResponse orderResponse = mock(OrderResponse.class);
        
        when(settingsService.getSettings(shopId)).thenReturn(settings);
        when(requestDetailsService.createShopOrder(order)).thenReturn(shopOrder);

        when(jsonShopApiRestClient.orderAccept(eq(settings), eq(shopOrder))).thenReturn(
            new ShopApiResponse<>(orderResponse, null, null)
        );

        final OrderResponse actual = apiService.orderAccept(shopId, order, false);
        assertEquals(actual, orderResponse);
    }

    @Test
    public void testRegularStatusChange() throws Exception {
        final long shopId = 1234;
        final Settings settings = new SettingsBuilder().build();

        final ShopOrder statusChange = mock(ShopOrder.class);

        when(settingsService.getSettings(shopId)).thenReturn(settings);
        when(xmlShopApiRestClient.changeOrderStatus(eq(settings), eq(statusChange))).thenReturn(
            new ShopApiResponse<>((Void) null, null, null)
        );

        apiService.orderStatus(shopId, statusChange, false);
        
        verify(xmlShopApiRestClient).changeOrderStatus(eq(settings), eq(statusChange));
    }

    @Test
    public void testUpdateShopSettings() throws Exception {
        final Settings settings = mock(Settings.class);
        final int shopId = 1234;
        apiService.settings(shopId, settings);
        
        verify(settingsService).updateSettings(shopId, settings);
    }

    @Test
    public void testWrongTokenCart() throws Exception {
        final Settings settings = new SettingsBuilder().build();

        final long shopId = 1234;
        final Cart cart = mock(Cart.class);
        final ExternalCart externalCart = mock(ExternalCart.class);

        when(settingsService.getSettings(shopId)).thenReturn(settings);
        final Settings newSettings = mock(Settings.class);
        when(randomizeTokenService.randomizeToken(settings)).thenReturn(newSettings);
        when(requestDetailsService.createExternalCart(cart)).thenReturn(externalCart);

        final ShopApiResponse<CartResponse> response = new ShopApiResponse<>(
            null, null, new HttpClientErrorException(HttpStatus.FORBIDDEN)
        );
        when(xmlShopApiRestClient.cart(eq(newSettings), eq(externalCart))).thenReturn(response);

        apiService.wrongTokenCart(shopId, UID, cart, false);

        verify(errorsStoreService).storeSuccess(
            eq(shopId), eq(ApiService.RESOURCE_CART_WRONG_TOKEN), eq(false), notNull(ShopApiResponse.class)
        );
    }

    @Test
    public void testThrowsShopErrorIfShopIgnoredWrongToken() throws Exception {
        final Settings settings = new SettingsBuilder().build();

        final long shopId = 1234l;
        final Cart cart = mock(Cart.class);
        final ExternalCart externalCart = mock(ExternalCart.class);

        when(settingsService.getSettings(shopId)).thenReturn(settings);
        final Settings newSettings = mock(Settings.class);
        when(randomizeTokenService.randomizeToken(settings)).thenReturn(newSettings);
        when(requestDetailsService.createExternalCart(cart)).thenReturn(externalCart);

        final CartResponse cartResponse = mock(CartResponse.class);
        when(xmlShopApiRestClient.cart(eq(newSettings), eq(externalCart))).thenReturn(
            new ShopApiResponse<>(cartResponse, null, null)
        );

        try {
            apiService.wrongTokenCart(shopId, UID, cart, false);
            fail();
        } catch(ShopErrorException e) {
            assertEquals(ErrorSubCode.IGNORED_WRONG_TOKEN, e.getCode());
            verify(errorsStoreService).storeError(
                eq(shopId), eq(ApiService.RESOURCE_CART_WRONG_TOKEN),
                eq(ErrorSubCode.IGNORED_WRONG_TOKEN), anyString(), eq(false), any(ShopApiResponse.class)
            );
        }
    }

    @Test
    public void testErrorInCartMethodStoresErrorWithRightRequestParam() throws Exception {
        final long shopId = 1234l;
        when(settingsService.getSettings(shopId)).thenReturn(new SettingsBuilder().build());
        final ShopApiResponse<CartResponse> response = new ShopApiResponse<>(
            "request", "response", new HttpClientErrorException(HttpStatus.ACCEPTED)
        );
        when(jsonShopApiRestClient.cart(any(Settings.class), any(ExternalCart.class)))
            .thenReturn(response);

        try {
            apiService.cart(shopId, 1, mock(Cart.class), false);
            fail();
        } catch(ShopErrorException e) {
            verify(errorsStoreService).storeError(
                eq(shopId), eq(ApiService.RESOURCE_CART), any(ErrorSubCode.class), anyString(), eq(false),
                eq(response)
            );
        }
    }

    @Test
    public void testErrorInOrderAcceptMethodStoresErrorWithRightRequestParam() throws Exception {
        final long shopId = 1234l;
        when(settingsService.getSettings(shopId)).thenReturn(new SettingsBuilder().build());
        final ShopApiResponse<OrderResponse> response = new ShopApiResponse<>(
            "request", "response", new HttpClientErrorException(HttpStatus.ACCEPTED)
        );
        when(jsonShopApiRestClient.orderAccept(any(Settings.class), any(ShopOrder.class)))
            .thenReturn(response);

        try {
            apiService.orderAccept(shopId, mock(Order.class), false);
            fail();
        } catch(ShopErrorException e) {
            verify(errorsStoreService).storeError(
                eq(shopId), eq(ApiService.RESOURCE_ORDER_ACCEPT), any(ErrorSubCode.class),
                anyString(), eq(false), eq(response)
            );
        }
    }

    @Test
    public void testErrorInOrderStatusMethodStoresErrorWithRightRequestParam() throws Exception {
        final long shopId = 1234l;
        when(settingsService.getSettings(shopId)).thenReturn(new SettingsBuilder().build());
        when(jsonShopApiRestClient.changeOrderStatus(any(Settings.class), any(ShopOrder.class)))
            .thenReturn(errResponse(new HttpServerErrorException(HttpStatus.BAD_GATEWAY)));

        try {
            apiService.orderStatus(shopId, mock(Order.class), false);
            fail();
        } catch(ShopErrorException e) {
            verify(errorsStoreService).storeError(
                eq(shopId), eq(ApiService.RESOURCE_ORDER_STATUS), any(ErrorSubCode.class),
                anyString(), eq(false), any(ShopApiResponse.class)
            );
        }
    }

    private ShopApiResponse normResponse(Object object) {
        return new ShopApiResponse(object, "request", "response");
    }

    private ShopApiResponse errResponse(HttpStatusCodeException ex) {
        return new ShopApiResponse("request", "response", ex);
    }

    @Test
    public void testSuccessCartCallStoresSuccessToErrorStore() throws Exception {
        final long shopId = 1234l;
        when(settingsService.getSettings(shopId)).thenReturn(new SettingsBuilder().build());
        final ShopApiResponse<CartResponse> response = normResponse(new CartResponse());
        when(jsonShopApiRestClient.cart(any(Settings.class), any(ExternalCart.class))).thenReturn(response);
        apiService.cart(shopId, 1, mock(Cart.class), false);

        verify(errorsStoreService).storeSuccess(shopId, ApiService.RESOURCE_CART, false, response);
    }

    @Test
    public void testSuccessOrderAcceptCallStoresSuccessToErrorStore() throws Exception {
        final long shopId = 1234l;
        when(settingsService.getSettings(shopId)).thenReturn(new SettingsBuilder().build());
        final ShopApiResponse<OrderResponse> response = normResponse(new OrderResponse());
        when(jsonShopApiRestClient.orderAccept(any(Settings.class), any(ShopOrder.class))).thenReturn(response);
        apiService.orderAccept(shopId, mock(Order.class), false);

        verify(errorsStoreService).storeSuccess(shopId, ApiService.RESOURCE_ORDER_ACCEPT, false, response);
    }

    @Test
    public void testSuccessOrderStatusCallStoresSuccessToErrorStore() throws Exception {
        final long shopId = 1234l;
        when(settingsService.getSettings(shopId)).thenReturn(new SettingsBuilder().build());
        final ShopApiResponse<Void> response = normResponse(null);
        when(jsonShopApiRestClient.changeOrderStatus(any(Settings.class), any(ShopOrder.class))).thenReturn(response);
        apiService.orderStatus(shopId, mock(Order.class), false);

        verify(errorsStoreService).storeSuccess(shopId, ApiService.RESOURCE_ORDER_STATUS, false, response);
    }

    @Test
     public void testSandboxOrderStatusStoresSandboxFlagToErrorStore() throws Exception {
        final long shopId = 1234l;
        when(settingsService.getSettings(shopId)).thenReturn(new SettingsBuilder().build());
        final ShopApiResponse<Void> response = normResponse(null);
        when(jsonShopApiRestClient.changeOrderStatus(any(Settings.class), any(ShopOrder.class))).thenReturn(response);
        apiService.orderStatus(shopId, mock(Order.class), true);

        verify(errorsStoreService).storeSuccess(eq(shopId), any(String.class), eq(true), eq(response));
    }

    @Test
    public void testSandboxCartStoresSandboxFlagToErrorStore() throws Exception {
        final long shopId = 1234l;
        when(settingsService.getSettings(shopId)).thenReturn(new SettingsBuilder().build());
        final ShopApiResponse<CartResponse> response = normResponse(new CartResponse());
        when(jsonShopApiRestClient.cart(any(Settings.class), any(ExternalCart.class))).thenReturn(response);
        apiService.cart(shopId, 1, mock(Cart.class), true);

        verify(errorsStoreService).storeSuccess(eq(shopId), any(String.class), eq(true), eq(response));
    }

    @Test
    public void testSandboxOrderAcceptStoresSandboxFlagToErrorStore() throws Exception {
        final long shopId = 1234l;
        when(settingsService.getSettings(shopId)).thenReturn(new SettingsBuilder().build());
        final ShopApiResponse<OrderResponse> response = normResponse(new OrderResponse());
        when(jsonShopApiRestClient.orderAccept(any(Settings.class), any(ShopOrder.class))).thenReturn(response);
        apiService.orderAccept(shopId, mock(Order.class), true);

        verify(errorsStoreService).storeSuccess(eq(shopId), any(String.class), eq(true), eq(response));
    }

    @Test
    public void testSandboxWrongTokenStoresSandboxFlagToErrorStore() throws Exception {
        final long shopId = 1234l;
        when(settingsService.getSettings(shopId)).thenReturn(new SettingsBuilder().build());
        final ShopApiResponse<CartResponse> response = new ShopApiResponse<>(
            "request", "response", new HttpClientErrorException(HttpStatus.FORBIDDEN));
        when(jsonShopApiRestClient.cart(any(Settings.class), any(ExternalCart.class))).thenReturn(response);
        apiService.wrongTokenCart(shopId, UID, mock(Cart.class), true);

        verify(errorsStoreService).storeSuccess(eq(shopId), any(String.class), eq(true), notNull(ShopApiResponse.class));
    }

    private void assertShopApiErrorHandle(Exception exception, ErrorSubCode errorSubCode) {
        final long shopId = 1234;

        final Cart cart = mock(Cart.class);
        final ExternalCart externalCart = mock(ExternalCart.class);

        when(settingsService.getSettings(shopId)).thenReturn(new SettingsBuilder().build());
        when(requestDetailsService.createExternalCart(cart)).thenReturn(externalCart);
        when(xmlShopApiRestClient.cart(any(Settings.class), eq(externalCart))).thenThrow(exception);

        try {
            apiService.cart(shopId, 1, cart, false);
            fail();
        } catch(ShopErrorException e) {
            assertEquals(errorSubCode, e.getCode());
            verify(errorsStoreService).storeError(
                eq(shopId), anyString(), eq(errorSubCode), anyString(), eq(false), isNull(ShopApiResponse.class)
            );
        }
    }

}
