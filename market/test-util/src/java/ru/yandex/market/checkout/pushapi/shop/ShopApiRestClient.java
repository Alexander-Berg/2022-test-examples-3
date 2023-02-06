package ru.yandex.market.checkout.pushapi.shop;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import ru.yandex.common.util.ApplicationUtil;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.shop.AuthType;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ShopApiRestClient implements ShopApi {

    private static final Logger log = Logger.getLogger(ShopApiRestClient.class);

    private RestTemplate restTemplate;
    private RestTemplateHandlers restTemplateHandlers;
    private ApiSelectorUtil apiSelectorUtil;

    @Required
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void setRestTemplateHandlers(RestTemplateHandlers restTemplateHandlers) {
        this.restTemplateHandlers = restTemplateHandlers;
    }

    public ApiSelectorUtil getApiSelectorUtil() {
        return apiSelectorUtil;
    }

    @Required
    public void setApiSelectorUtil(ApiSelectorUtil apiSelectorUtil) {
        this.apiSelectorUtil = apiSelectorUtil;
    }

    private <Req, Res> ShopApiResponse<Res> perform(
            final long shopId,
            boolean sandbox,
            Settings settings,
            Req cart,
            Class<Req> requestClass,
            Class<Res> responseClass,
            String path
    ) {
        log.info("requesting /" + path + " with " + settings);

        final HttpBodies httpBodies = new HttpBodies();
        ThreadLocalSettings.setSettings(settings);

        final long ts = System.currentTimeMillis();
        ApiSelectorUtil.ApiSelection apiSelection = apiSelectorUtil.getApiUrl(shopId, sandbox, settings, path, cart);

        if(log.isDebugEnabled()) {
            log.debug("Requesting: " + apiSelection.getUri());
        }

        final HttpMethod httpMethod = HttpMethod.POST;
        ShopApiResponse<Res> response;
        try {
            final Res responseObject = restTemplate.execute(
                    apiSelection.getUri(),
                httpMethod,
                restTemplateHandlers.requestCallback(restTemplate, cart, requestClass, httpBodies, settings),
                restTemplateHandlers.responseExtractor(restTemplate, responseClass, httpBodies)
            );

            if(responseObject instanceof CartResponse) {
                CartResponse cartResponse = (CartResponse) responseObject;
                cartResponse.setShopAdmin(apiSelection.isShopadmin());
            }

            response = ShopApiResponse.fromBody(responseObject);
        } catch(Exception e) {
            response = ShopApiResponse.fromException(e);
        }
        final long diff = System.currentTimeMillis() - ts;


        response.setUrl(apiSelection.getUrl());
        response.setArgs(apiSelection.getArgs());
        response.setHttpMethod(httpMethod.toString());
        response.populateBodies(httpBodies);
        response.setResponseTime(diff);
        response.setHost(ApplicationUtil.getHostName());
        return response;
    }

    @Override
    public ShopApiResponse<CartResponse> cart(final long shopId, Settings params, boolean sandbox, final ExternalCart cart) {
        return perform(shopId, sandbox, params, cart, ExternalCart.class, CartResponse.class, "cart");
    }

    @Override
    public ShopApiResponse<OrderResponse> orderAccept(final long shopId, Settings params, boolean sandbox, ShopOrder order) {
        return perform(shopId, sandbox, params, order, ShopOrder.class, OrderResponse.class, "order/accept");
    }

    @Override
    public ShopApiResponse<Void> changeOrderStatus(final long shopId, Settings params, boolean sandbox, ShopOrder statusChange) {
        return perform(shopId, sandbox, params, statusChange, ShopOrder.class, Void.class, "order/status");
    }

}
