package ru.yandex.market.checkout.checkouter.checkout;

import java.util.Arrays;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.config.pushapi.AsyncPushApiProperties;

import static ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentBooleanFeatureType.ASYNC_PUSH_API;
import static ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentBooleanFeatureType.ASYNC_PUSH_API_FBS;
import static ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentIntegerFeatureType.ASYNC_PUSH_API_MAX_ORDERS_COUNT_PER_SHOP;

public class AbstractPushApiTestBase extends AbstractWebTestBase {
    @Autowired
    private AsyncPushApiProperties asyncPushApiProperties;

    protected void setAsyncPushApi(boolean value) {
        checkouterFeatureWriter.writeValue(ASYNC_PUSH_API, value);
    }

    protected void setAsyncFBSPushApi(boolean value) {
        checkouterFeatureWriter.writeValue(ASYNC_PUSH_API_FBS, value);
    }

    protected void setEdaTimeout(int value) {
        asyncPushApiProperties.setEdaTimeout(value);
    }

    protected void setDbsTimeout(int value) {
        asyncPushApiProperties.setDbsTimeout(value);
    }

    protected void setExpressTimeout(int value) {
        asyncPushApiProperties.setExpressTimeout(value);
    }

    protected void setSyncShopIds(long shopId) {
        asyncPushApiProperties.setSyncShopIds(Collections.singleton(shopId));
    }

    protected void setReadTimeouts(Integer... timeouts) {
        asyncPushApiProperties.setReadTimeouts(Arrays.asList(timeouts));
    }

    protected void setRetryTimeouts(Integer... timeouts) {
        asyncPushApiProperties.setRetryTimeouts(Arrays.asList(timeouts));
    }

    protected void setMaxOrdersPerShop(int count) {
        checkouterFeatureWriter.writeValue(ASYNC_PUSH_API_MAX_ORDERS_COUNT_PER_SHOP, count);
    }
}
