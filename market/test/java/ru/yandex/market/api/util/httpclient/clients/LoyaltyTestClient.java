package ru.yandex.market.api.util.httpclient.clients;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import ru.yandex.market.api.user.order.MarketUid;
import ru.yandex.market.api.util.ApiStrings;
import ru.yandex.market.api.util.httpclient.spi.HttpErrorType;
import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Created by fettsery on 23.05.18.
 */
@Service
public class LoyaltyTestClient extends AbstractFixedConfigurationTestClient {
    public LoyaltyTestClient() {
        super("Loyalty");
    }

    public HttpResponseConfigurer checkStatus(int regionId, long uid, String resultFileName) {
        return configure(b -> b.serverMethod("/perk/status/all")
            .param("regionId", String.valueOf(regionId))
            .param("uid", String.valueOf(uid))
            .get()
        ).ok().body(resultFileName);
    }

    public HttpResponseConfigurer checkStatusTimeout(int regionId, long uid, long timeout) {
        return configure(b -> b.serverMethod("/perk/status/all")
            .param("regionId", String.valueOf(regionId))
            .param("uid", String.valueOf(uid))
            .get()
        ).error(HttpErrorType.CONNECT_TIMEOUT).timeout(timeout);
    }

    public HttpResponseConfigurer checkStatusError(int regionId, long uid) {
        return configure(b -> b.serverMethod("/perk/status/all")
            .param("regionId", String.valueOf(regionId))
            .param("uid", String.valueOf(uid))
            .get()
        ).status(HttpResponseStatus.INTERNAL_SERVER_ERROR).emptyResponse();
    }

    public HttpResponseConfigurer getCoinsForOrder(long uid, long orderId, String resultFileName) {
        JSONObject request = new JSONObject();
        request.put("noAuth", false);
        request.put("orderId", orderId);
        request.put("uid", uid);

        return configure(b -> b.serverMethod("/coins/orderStatusUpdated")
            .body(x -> Arrays.equals(ApiStrings.getBytes(request.toString()), x), "body for checking loyalty for order")
            .post()
        ).ok().body(resultFileName);
    }

    public HttpResponseConfigurer getCoinsForUser(long uid, int limit, String filename) {
        return configure(
            b -> b.serverMethod("/coins/person")
            .get()
            .param("uid", String.valueOf(uid))
            .param("limitFutureCoins", String.valueOf(limit))
        ).ok().body(filename);
    }

    public HttpResponseConfigurer getCoinsForUnauthorisedUser(int limit, String filename) {
        return configure(
            b -> b.serverMethod("/coins/futureCoins")
                .get()
                .param("limitFutureCoins", String.valueOf(limit))
        ).ok().body(filename);
    }

    public HttpResponseConfigurer bindByMuid(long uid, MarketUid muid, String uuid, String ip, String filename) {
        return configure(
            b -> b.serverMethod("/coins/bindByMuid")
            .put()
            .param("ip", ip)
            .param("muid", String.valueOf(muid.getMuid()))
            .param("uid", String.valueOf(uid))
            .header(
                HttpHeaderNames.COOKIE.toString(),
                io.netty.handler.codec.http.cookie.ServerCookieEncoder.STRICT.encode(new DefaultCookie("muid", muid.getSignature()))
            )
            .header("User-Agent", uuid)
        ).ok().body(filename);
    }

    public HttpResponseConfigurer getCartCoins(long uid,
                                               Function<JsonNode, Boolean> bodyPredicate,
                                               String resultFileName) {
        return configure(b -> b.serverMethod("/coins/cart/v2")
            .param("uid", String.valueOf(uid))
            .jsonBody(bodyPredicate, "body matched")
            .post()
        ).ok().body(resultFileName);
    }

    public HttpResponseConfigurer getCartThreshold(int regionId,
                                                   String clientDeviceType,
                                                   String resultFileName) {
        return configure(b -> b.serverMethod("/discount/priceLeftForFreeDelivery/v2")
            .param("regionId", String.valueOf(regionId))
            .param("clientDeviceType", clientDeviceType)
            .post()
        ).ok().body(resultFileName);
    }

    public HttpResponseConfigurer getCartThresholdError(int regionId,
                                                        String clientDeviceType) {
        return configure(b -> b.serverMethod("/discount/priceLeftForFreeDelivery/v2")
                .param("regionId", String.valueOf(regionId))
                .param("clientDeviceType", clientDeviceType)
                .post()
        ).status(HttpResponseStatus.INTERNAL_SERVER_ERROR).emptyResponse();
    }
}
