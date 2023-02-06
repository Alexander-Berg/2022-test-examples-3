package ru.yandex.market.checkout.pushapi.service.shop;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.rest.InvalidRequestException;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.shop.DataType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.client.error.ShopErrorException;
import ru.yandex.market.checkout.pushapi.service.RandomizeTokenService;
import ru.yandex.market.checkout.pushapi.service.RequestDetailsService;
import ru.yandex.market.checkout.pushapi.service.shop.settings.SettingsService;
import ru.yandex.market.checkout.pushapi.shop.ApiSelectorUtil;
import ru.yandex.market.checkout.pushapi.shop.ShopApi;
import ru.yandex.market.checkout.pushapi.shop.ShopApiResponse;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import java.io.PrintWriter;
import java.io.StringWriter;

@Component
public class DefaultApiService implements ApiService {

    private static final Logger log = Logger.getLogger(DefaultApiService.class);
    
    private ShopApi thickXmlShopApiRestClient;
    private ShopApi thinXmlShopApiRestClient;
    private ShopApi thickJsonShopApiRestClient;
    private ShopApi thinJsonShopApiRestClient;
    private ErrorsStoreService errorsStoreService;
    private SettingsService settingsService;
    private RequestDetailsService requestDetailsService;
    private ValidateService validateService;
    private RandomizeTokenService randomizeTokenService;
    private ApiSelectorUtil apiSelectorUtil;

    @Autowired
    public void setRandomizeTokenService(RandomizeTokenService randomizeTokenService) {
        this.randomizeTokenService = randomizeTokenService;
    }

    @Autowired
    public void setThickXmlShopApiRestClient(ShopApi thickXmlShopApiRestClient) {
        this.thickXmlShopApiRestClient = thickXmlShopApiRestClient;
    }

    @Autowired
    public void setThinXmlShopApiRestClient(ShopApi thinXmlShopApiRestClient) {
        this.thinXmlShopApiRestClient = thinXmlShopApiRestClient;
    }

    @Autowired
    public void setThickJsonShopApiRestClient(ShopApi thickJsonShopApiRestClient) {
        this.thickJsonShopApiRestClient = thickJsonShopApiRestClient;
    }

    @Autowired
    public void setThinJsonShopApiRestClient(ShopApi thinJsonShopApiRestClient) {
        this.thinJsonShopApiRestClient = thinJsonShopApiRestClient;
    }

    @Autowired
    public void setErrorsStoreService(ErrorsStoreService errorsStoreService) {
        this.errorsStoreService = errorsStoreService;
    }

    @Autowired
    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Autowired
    public void setRequestDetailsService(RequestDetailsService requestDetailsService) {
        this.requestDetailsService = requestDetailsService;
    }

    @Autowired
    public void setValidateService(ValidateService validateService) {
        this.validateService = validateService;
    }

    @Autowired
    public void setApiSelectorUtil(ApiSelectorUtil apiSelectorUtil) {
        this.apiSelectorUtil = apiSelectorUtil;
    }

    private <T, V> V handleShopApiRequest(
        long shopId, T request, boolean sandbox, ShopRequest<T, V> shopRequest
    ) {
        final Settings settings = settingsService.getSettings(shopId);
        ApiSelectorUtil.ApiSelection apiSelection = apiSelectorUtil.getApiUrl(shopId, sandbox, settings, shopRequest.getResource(), request);

        final ShopApiResponse<V> result = shopRequest.request(
            selectShopMediaType(shopRequest.getResource(), settings, apiSelection), settings, request
        );
        if(result.isError()) {
            final Exception ex = result.getException();
            final ErrorSubCode errorSubCode = result.getErrorSubCode();

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            log.error("ShopApiRequest error: " + ex.getClass().getName() + "\n" + sw.toString());
            log.error("ShopApiRequest error request: " + request);
            log.error("ShopApiRequest error response: \n" +
                    "args: " + result.getArgs() + "\n" +
                    "host: " + result.getHost() + "\n" +
                    "rq headers: " + result.getRequestHeaders() + "\n" +
                    "rq body: " + result.getRequestBody() + "\n" +
                    "resp headers: " + result.getResponseHeaders() + "\n" +
                    "resp body: " + result.getResponseBody());
            errorsStoreService.storeError(
                shopId, shopRequest.getResource(), errorSubCode, ex.getMessage(), sandbox, result
            );
            throw new ShopErrorException(errorSubCode, ex.getMessage(), ex);
        } else {
            errorsStoreService.storeSuccess(shopId, shopRequest.getResource(), sandbox, result);
            return result.getBody();
        }
    }
    
    @Override
    public CartResponse cart(final long shopId, final long uid, final Cart cart, final boolean sandbox) {
        return handleShopApiRequest(
            shopId, cart, sandbox, new ShopRequest<Cart, CartResponse>(RESOURCE_CART) {
                @Override
                public ShopApiResponse<CartResponse> request(ShopApi shopApi, Settings settings, Cart request) {
                    final ShopApiResponse<CartResponse> response = shopApi.cart(
                        shopId, settings, sandbox, requestDetailsService.createExternalCart(request)
                    );
                    response.setUid(uid);
                    if(!response.isError()) {
                        try {
                            final CartResponse cartResponse = response.getBody();
                            validateService.validateCartResponse(cart, cartResponse);

                            requestDetailsService.fixCartResponse(cartResponse);
                        } catch(ValidationException e) {
                            return ShopApiResponse.copyAndSetError(e, response);
                        }
                    }

                    return response;
                }
            }
        );
    }

    @Override
    public OrderResponse orderAccept(final long shopId, final Order order, final boolean sandbox) {
        return handleShopApiRequest(
            shopId, order, sandbox, new ShopRequest<Order, OrderResponse>(RESOURCE_ORDER_ACCEPT) {
                @Override
                public ShopApiResponse<OrderResponse> request(ShopApi shopApi, Settings settings, Order request) {
                    final Long uidFromOrder = getUidFromOrder(order);
                    final ShopApiResponse<OrderResponse> orderResponse = shopApi.orderAccept(
                            shopId, settings, sandbox, requestDetailsService.createShopOrder(request)
                    );
                    orderResponse.setUid(uidFromOrder);

                    if(!orderResponse.isError()) {
                        try {
                            validateService.validateOrderResponse(orderResponse.getBody());
                        } catch(ValidationException|InvalidRequestException|IllegalArgumentException e) {
                            return ShopApiResponse.copyAndSetError(e, orderResponse);
                        }
                    }

                    return orderResponse;
                }
            }
        );
    }

    private Long getUidFromOrder(Order order) {
        final Buyer buyer = order.getBuyer();
        if(buyer != null) {
            return buyer.getUid();
        } else {
            return null;
        }
    }

    @Override
    public void orderStatus(final long shopId, final Order statusChange, final boolean sandbox) {
        handleShopApiRequest(
            shopId, statusChange, sandbox, new ShopRequest<Order, Void>(RESOURCE_ORDER_STATUS) {
                @Override
                public ShopApiResponse<Void> request(ShopApi shopApi, Settings settings, Order request) {
                    final Long uidFromOrder = getUidFromOrder(statusChange);
                    final ShopOrder shopOrder = requestDetailsService.createShopOrderStatus(request);
                    final ShopApiResponse<Void> response = shopApi.changeOrderStatus(
                            shopId, settings, sandbox, shopOrder)
                    ;
                    response.setUid(uidFromOrder);
                    return response;
                }
            }
        );
    }

    @Override
    public void settings(long shopId, Settings settings) {
        settingsService.updateSettings(shopId, settings);
    }

    @Override
    public Settings getSettings(long shopId) {
        return settingsService.getSettings(shopId);
    }

    @Override
    public void wrongTokenCart(final long shopId, final long uid, Cart cart, final boolean sandbox) {
        handleShopApiRequest(shopId, cart, sandbox, new ShopRequest<Cart, CartResponse>(RESOURCE_CART_WRONG_TOKEN) {
            @Override
            ShopApiResponse<CartResponse> request(ShopApi shopApi, Settings settings, Cart request) {
                final Settings wrongTokenSettings = randomizeTokenService.randomizeToken(settings);

                final ShopApiResponse<CartResponse> response = shopApi.cart(
                        shopId, wrongTokenSettings, sandbox, requestDetailsService.createExternalCart(request)
                );
                response.setUid(uid);
                if(response.isError() && response.getErrorSubCode() == ErrorSubCode.HTTP) {
                    final HttpStatusCodeException exception = (HttpStatusCodeException) response.getException();
                    if(exception.getStatusCode() == HttpStatus.FORBIDDEN) {
                        log.info("caught httpStatusError");
                        return ShopApiResponse.copyAndSetBody(null, response);
                    }
                }

                return ShopApiResponse.copyAndSetError(
                    new ShopErrorException(ErrorSubCode.IGNORED_WRONG_TOKEN, "ignored wrong token"),
                    response
                );
            }
        });
    }

    private ShopApi selectShopMediaType(String resource, Settings settings, ApiSelectorUtil.ApiSelection apiSelection) {
        DataType dataType;

        if(apiSelection.isShopadmin()) {
            dataType = DataType.XML;
        }
        else {
            dataType = settings.getDataType();
        }

        switch(dataType) {
            case XML:
                switch(resource) {
                    case RESOURCE_CART:
                    case RESOURCE_CART_WRONG_TOKEN:
                        return thinXmlShopApiRestClient;
                    case RESOURCE_ORDER_ACCEPT:
                    case RESOURCE_ORDER_STATUS:
                        return thickXmlShopApiRestClient;
                }
            case JSON:
                switch(resource) {
                    case RESOURCE_CART:
                    case RESOURCE_CART_WRONG_TOKEN:
                        return thinJsonShopApiRestClient;
                    case RESOURCE_ORDER_ACCEPT:
                    case RESOURCE_ORDER_STATUS:
                        return thickJsonShopApiRestClient;
                }
        }

        throw new RuntimeException("unknown dataType: " + settings.getDataType());
    }

    private abstract class ShopRequest<T, V> {
        private String resource;

        protected ShopRequest(String resource) {
            this.resource = resource;
        }

        abstract ShopApiResponse<V> request(ShopApi shopApi, Settings settings, T request);

        String getResource() {
            return resource;
        }
    }
}
