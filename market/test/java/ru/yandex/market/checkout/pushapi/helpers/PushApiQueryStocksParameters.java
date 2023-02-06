package ru.yandex.market.checkout.pushapi.helpers;

import java.time.Clock;
import java.util.function.Function;

import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.pushapi.client.entity.StocksRequest;
import ru.yandex.market.checkout.pushapi.client.entity.StocksResponse;
import ru.yandex.market.checkout.pushapi.providers.StocksRequestProvider;
import ru.yandex.market.checkout.pushapi.providers.StocksResponseProvider;
import ru.yandex.market.checkout.pushapi.settings.DataType;

public class PushApiQueryStocksParameters {
    private static final long DEFAULT_SHOP_ID = 242102L;
    private static final boolean DEFAULT_SANDBOX = false;

    private final StocksRequest request;
    private final StocksResponse response;
    private String content;
    private Function<PushApiQueryStocksParameters, String> contentSerializer;
    private long shopId = DEFAULT_SHOP_ID;
    private boolean sandbox = DEFAULT_SANDBOX;
    private Context context;
    private ApiSettings apiSettings;
    private boolean partnerInterface;
    private DataType dataType = DataType.JSON;
    private Integer responseDelay;

    public PushApiQueryStocksParameters(Clock clock, Function<PushApiQueryStocksParameters, String> contentSerializer) {
        this(StocksRequestProvider.buildStocksRequest(), StocksResponseProvider.buildResponse(clock));
        this.contentSerializer = contentSerializer;
    }

    public PushApiQueryStocksParameters(StocksRequest request, StocksResponse response) {
        this.request = request;
        this.response = response;
    }

    public StocksRequest getRequest() {
        return request;
    }

    public StocksResponse getResponse() {
        return response;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public long getShopId() {
        return shopId;
    }

    public void setShopId(long shopId) {
        this.shopId = shopId;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public ApiSettings getApiSettings() {
        return apiSettings;
    }

    public void setApiSettings(ApiSettings apiSettings) {
        this.apiSettings = apiSettings;
    }

    public boolean isPartnerInterface() {
        return partnerInterface;
    }

    public void setPartnerInterface(boolean partnerInterface) {
        this.partnerInterface = partnerInterface;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public String callContentSerializer() {
        return contentSerializer.apply(this);
    }

    public Integer getResponseDelay() {
        return responseDelay;
    }

    public void setResponseDelay(Integer responseDelay) {
        this.responseDelay = responseDelay;
    }
}
