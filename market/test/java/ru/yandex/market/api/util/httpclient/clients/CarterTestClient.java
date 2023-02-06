package ru.yandex.market.api.util.httpclient.clients;

import java.util.function.Function;

import com.google.common.base.Functions;
import org.springframework.stereotype.Service;

import ru.yandex.market.api.internal.carter.domain.AddItemResult;
import ru.yandex.market.api.util.ApiStrings;
import ru.yandex.market.api.util.httpclient.spi.HttpRequestExpectationBuilder;
import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;
import ru.yandex.market.api.util.json.JsonSerializer;
import ru.yandex.market.common.Serializer;

import static ru.yandex.market.checkout.common.web.CheckoutHttpParameters.EXPERIMENTS_HEADER;

@Service
public class CarterTestClient extends AbstractFixedConfigurationTestClient {

    private final Serializer jsonSerializer;

    public CarterTestClient(JsonSerializer jsonSerializer) {
        super("Carter");
        this.jsonSerializer = jsonSerializer;
    }

    public HttpResponseConfigurer getCart(long userId, String expectedResponseFileName) {
        return configure(x -> x
            .get()
            .serverMethod(String.format("cart/UID/%d/list", userId)))
            .ok()
            .body(expectedResponseFileName);
    }

    public HttpResponseConfigurer getCartWithExperiments(long userId, int regionId, String experiments,
                                                         String expectedResponseFileName) {
        return getRawCart(userId, regionId, x -> x.header(EXPERIMENTS_HEADER, experiments), expectedResponseFileName);
    }

    public HttpResponseConfigurer getCartWithPromoId(long userId, int regionId, String promoId,
                                                         String expectedResponseFileName) {
        return getRawCart(userId, regionId, x -> x.param("perkPromoId", promoId), expectedResponseFileName);

    }

    public HttpResponseConfigurer getRawCart(long userId, int regionId, Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder> fn, String expectedResponseFileName) {
        return configure(x -> fn.apply(x)
                    .get()
                    .serverMethod(String.format("cart/UID/%d/list", userId))
                    .param("rid", String.valueOf(regionId))
        )
                .ok()
                .body(expectedResponseFileName);
    }


    public HttpResponseConfigurer getCart(long userId, int regionId, String expectedResponseFileName) {
        return getRawCart(userId, regionId, Functions.identity(), expectedResponseFileName);
    }

    public HttpResponseConfigurer getCartWithConsolidate(long userId, int regionId, boolean consolidate,
                                                         String expectedResponseFileName) {
        return getRawCart(userId, regionId, x -> x.param("consolidate", String.valueOf(consolidate)), expectedResponseFileName);
    }

    public void addItem(long userId, long itemId, long timestamp) {

        AddItemResult addItemResult = new AddItemResult();
        addItemResult.setResult(itemId);
        addItemResult.setStatus("success");
        addItemResult.setTimestamp(timestamp);

        String response = jsonSerializer.writeObject(addItemResult);
        configure(x -> x
            .post()
            .serverMethod(String.format("cart/UID/%s/list/-1/item", userId)))
            .ok()
            .body(response.getBytes(ApiStrings.UTF8));
    }

    public void editItem(long userId, String itemId, int count, String filename) {
        configure(x -> x
            .put()
            .serverMethod(String.format("cart/UID/%d/list/-1/item/%s", userId, itemId))
            .param("count", String.valueOf(count)))
            .ok()
            .body(filename);
    }

    public void removeItem(long userId, String itemId, String filename) {
        configure(x -> x
            .delete()
            .serverMethod(String.format("cart/UID/%d/list/-1/item/%s", userId, itemId)))
            .ok()
            .body(filename);
    }

    public void removeItem(long userId, String itemId) {
        configure(x -> x
            .delete()
            .serverMethod(String.format("cart/UID/%d/list/-1/item/%s", userId, itemId)))
            .ok()
            .emptyResponse();
    }

    public void merge(String userFrom, long userTo, boolean enableMultiOffers) {
        configure(
            x -> x.patch()
                .serverMethod("cart/UUID/" + userFrom + "/list")
                .param("idTo", String.valueOf(userTo))
                .param("enableMultiOffers", String.valueOf(enableMultiOffers))
        ).ok().emptyResponse();
    }
}
