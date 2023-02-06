package ru.yandex.market.api.util.httpclient.clients;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.ints.IntCollection;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import ru.yandex.market.api.common.client.rules.BlueRule;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.internal.filters.FiltersRegistry;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.user.order.ShopOfferId;
import ru.yandex.market.api.util.ApiCollections;
import ru.yandex.market.api.util.ApiStrings;
import ru.yandex.market.api.util.CommonPrimitiveCollections;
import ru.yandex.market.api.util.httpclient.spi.HttpRequestExpectationBuilder;
import ru.yandex.market.api.util.httpclient.spi.HttpResponseConfigurer;

@Service
public class ReportTestClient extends AbstractTestClient {
    private final BlueRule blueRule;

    public ReportTestClient(BlueRule blueRule) {
        this.blueRule = blueRule;
    }


    public void find(OfferId[] offerIds, String expectedResponseFileName) {
        String shoppingList = Arrays.stream(offerIds)
                .map(x -> String.format("o:%s:1", x.getWareMd5()))
                .collect(Collectors.joining(","));
        search("alt_same_shop",
                x -> x.param("shopping-list", shoppingList),
                expectedResponseFileName);
    }

    public HttpResponseConfigurer getCategoryModels(int categoryId, String excludeHids, String expectedResponseFileName) {
        return search("personalcategorymodels", x -> x.param("hid", categoryId + excludeHids), expectedResponseFileName);
    }

    public HttpResponseConfigurer getModelOffers(long modelId, String expectedResponseFileName) {
        return search("productoffers", x -> x.param("hyperid", String.valueOf(modelId)), expectedResponseFileName);
    }

    public HttpResponseConfigurer getModelOffers(Collection<Long> modelsIds, String expectedResponseFileName) {
        Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder> f =
                builder -> ApiCollections.reduce(builder, modelsIds, (x, id) -> x.param("hyperid", String.valueOf(id)))
                        .param("use_multiple_hyperid", "1");
        return search("productoffers", f, expectedResponseFileName);
    }


    public HttpResponseConfigurer getGeoModelOffers(long modelId, String expectedResponseFileName) {
        return search("geo", x -> x.param("hyperid", String.valueOf(modelId)), expectedResponseFileName);
    }

    public HttpResponseConfigurer getGeoOffer(String offerId, String expectedResponseFileName) {
        return search("geo", x -> x.param("offerid", offerId), expectedResponseFileName);
    }

    public HttpResponseConfigurer getModelModifications(long modelId, String expectedResponseFileName) {
        return search("model_modifications", x ->  x.param("hyperid", String.valueOf(modelId)), expectedResponseFileName);
    }

    public HttpResponseConfigurer getOfferInfo(OfferId offerId, String expectedResponseFileName) {
        return search("offerinfo", x -> x.param("offerid", offerId.getWareMd5()),
            expectedResponseFileName);
    }

    public HttpResponseConfigurer getProductAccessories(long modelId, String expectedResponseFileName) {
        return search("product_accessories", x -> x.param("hyperid", String.valueOf(modelId))
                .withoutParam("market-sku"),
            expectedResponseFileName);
    }

    public HttpResponseConfigurer getProductAccessoriesBlue(long modelId, String msku, String expectedResponseFileName) {
        return search("product_accessories", x -> x.param("hyperid", String.valueOf(modelId))
                .param("cpa", "real")
                .param("market-sku", msku),
            expectedResponseFileName);
    }

    public HttpResponseConfigurer getProductAccessoriesBlueWithoutMsku(long modelId, String expectedResponseFileName) {
        return search("product_accessories", x -> x.param("hyperid", String.valueOf(modelId))
                .param("cpa", "real")
                .withoutParam("market-sku"),
            expectedResponseFileName);
    }

    public HttpResponseConfigurer getModelAnalogsBlue(long modelId, String expectedResponseFileName) {
        return search("also_viewed", x -> x.param("hyperid", String.valueOf(modelId)),
            expectedResponseFileName);
    }

    public HttpResponseConfigurer getOfferInfoWithoutFilterByCpc(OfferId offerId, String expectedResponseFileName) {
        return search("offerinfo", x -> x.param("offerid", offerId.getWareMd5()).withoutParam("hide-offers-without-cpc-link"),
            expectedResponseFileName);
    }

    public HttpResponseConfigurer getOfferInfoWithCpc(OfferId offerId, String expectedResponseFileName) {
        return search("offerinfo", x -> x.param("offerid", offerId.getWareMd5())
                .param("hide-offers-without-cpc-link", "1"),
            expectedResponseFileName);
    }

    public HttpResponseConfigurer getOfferByShopOfferId(ShopOfferId shopOfferId, String expectedResponseFileName) {
        return search("offerinfo", x -> x.param("feed_shoffer_id_base64", Base64.encodeBase64String(shopOfferId.asString().getBytes())),
                expectedResponseFileName);
    }

    public void getOffersByShopOfferKeys(ShopOfferId[] offerKeys, String expectedResponseFileName) {
        search("offerinfo", x -> {
            for (ShopOfferId id : offerKeys) {
                x = x.param("feed_shoffer_id_base64", Base64.encodeBase64String(String.format("%s-%s", id.getFeedId(), id.getOfferId()).getBytes()));
            }
            return x;
        }, expectedResponseFileName);
    }

    public HttpResponseConfigurer getOffersV2(OfferId[] offerIds, String expectedResponseFileName) {
        return search("offerinfo", x -> {
            for (OfferId id : offerIds) {
                x = x.param("offerid", id.getWareMd5());
            }
            return x;
        }, expectedResponseFileName);
    }

    public void getModelInfoById(long modelId, String responseFileName) {
        search("modelinfo", x -> x.param("hyperid", String.valueOf(modelId)), responseFileName);
    }

    public void getModelStats(long modelId, String responseFileName) {
        search("model_stats", x -> x.param("hyperid", String.valueOf(modelId)), responseFileName);
    }

    public void productAnalogs(long modelId, String responseFileName) {
        search("also_viewed", x -> x.param("hyperid", String.valueOf(modelId)), responseFileName);
    }

    public void getModelInfoById(Collection<Long> modelIds, String responseFileName) {
        search("modelinfo", x -> {
            for (Long id : modelIds) {
                x = x.param("hyperid", String.valueOf(id));
            }
            return x;
        }, responseFileName);
    }

    public void clothes(String similarSig, String clothesSig, String responseFileName) {
        search("clothes", x -> x.param("similar-sig", similarSig)
                .param("clothes-sig", clothesSig), responseFileName);
    }

    public void getDefaultOffer(long modelId, String responseFileName) {
        search("defaultoffer", x -> x.param("hyperid", String.valueOf(modelId))
                .param("bsformat", "2"),
            responseFileName);
    }

    public void getDefaultOffer_VendorRecommended(long modelId, String responseFileName) {
        search("defaultoffer", x -> x.param("hyperid", String.valueOf(modelId))
                .param("bsformat", "2")
                .param("filter-vendor-recommended", "1"),
            responseFileName);
    }

    public HttpResponseConfigurer getModelInfoById(long modelId) {
        return configure(x -> buildRequest(x, "modelinfo", y -> y.param("hyperid", String.valueOf(modelId))));
    }

    public void searchOffersByModelId(int modelId, String responseFileName) {
        search("productoffers", x -> x.param("hyperid", String.valueOf(modelId)), responseFileName);
    }

    public void redirect(String text, String responseFileName) {
        search("prime", x -> x.param("cvredirect", "1").param("text", text), responseFileName);
    }

    public void redirect3(String text, String responseFileName) {
        search("prime", x -> x.param("cvredirect", "3").param("text", text), responseFileName);
    }

    public void searchV2(String text, String reponseFileName) {
        search("prime", x -> x.param("cvredirect", "0").param("text", text), reponseFileName);
    }

    public void searchV2withoutCvRedirect(String text, String reponseFileName) {
        search("prime", x -> x.withoutParam("cvredirect").param("text", text), reponseFileName);
    }

    public void recommendedModels(String excludeHids, String uuid, String responseFileName) {
        search("better_price", x ->
                        x.param("hid", excludeHids)
                                .param("uuid", uuid),
                responseFileName);
    }

    public void recommendedByHistory(String uuid, PageInfo pageInfo, String responseFileName) {
        search("products_by_history", x -> x.param("uuid", uuid)
                        .param("numdoc", String.valueOf(pageInfo.getCount()))
                        .param("page", String.valueOf(pageInfo.getPage())),
                responseFileName);
    }

    public void categorySearch(IntCollection hids, String responseFileName) {
        search("prime", x -> x.param("hid", CommonPrimitiveCollections.join(hids, ",")), responseFileName);
    }

    public void categorySearch(IntCollection hids,
                               Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder> modifier,
                               String responseFileName) {
        search(
            "prime",
            x -> x.param("hid", CommonPrimitiveCollections.join(hids, ","))
                .apply(modifier),
            responseFileName
        );
    }

    public void categorySearchLite(int hid, String responseFileName) {
        search(
            "prime",
            x -> x.param("hid", String.valueOf(hid)).param("prime-output", "1"),
            responseFileName
        );
    }

    public void popularProducts(String responseFileName) {
        search("popular_products", x -> x, responseFileName);
    }

    public void sku(String skuId,
                    String wareId,
                    String responseFileName) {
        search(
            "sku_offers",
            x -> x.param("market-sku", skuId).param("offerid", wareId),
            responseFileName
        );
    }

    public void sku(String skuId,
                    String responseFileName) {
        search(
            "sku_offers",
            x -> x.param("market-sku", skuId),
            responseFileName
        );
    }

    public void skus(List<String> skuIds,
                     String responseFileName) {
        search(
            "sku_offers",
            x -> x.param("market-sku", ApiStrings.COMMA_JOINER.join(skuIds)),
            responseFileName
        );
    }

    public HttpResponseConfigurer search(String place,
                                         Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder> modifier,
                                         String bodyResponseFile) {
        return configure(x -> buildRequest(x, place, modifier))
                .ok().body(bodyResponseFile);
    }

    public void getShopsRatings(Collection<Long> ids, String responseFilename) {
        search(
            "shop_info",
            x -> x.param("fesh", ApiStrings.COMMA_JOINER.join(ids)),
            responseFilename
        );
    }

    public HttpResponseConfigurer getOutlets(Collection<Long> outletIds, String responseFilename) {
        return search(
            "outlets",
            x -> x.param("outlets", ApiStrings.COMMA_JOINER.join(outletIds)),
            responseFilename
        );
    }

    public void getAttractiveModels(Map<String, String> filterParams, String responseFilename) {
        search("attractive_models",
            x -> {
                for(Map.Entry<String, String> filter : filterParams.entrySet()) {
                    x = x.param(FiltersRegistry.getById(filter.getKey()).getReportParamName(), filter.getValue());
                }
                return x;
            },
            responseFilename);
    }

    public void getAttractiveModelsBlue(String responseFilename) {
        search("blue_attractive_models",
            x -> x,
            responseFilename);
    }

    public void getBlueOmmFindings(String responseFilename) {
        search("blue_omm_findings",
            x -> x,
            responseFilename);
    }

    public void getRecommendedModelsVerticalTouch(String responseFilename, String pof) {
        doRequest("omm_market",
            x -> x.param("pof", pof)
                .withoutParam("trim-thumbs")
        )
            .ok()
            .body(responseFilename);
    }

    public void getRecommendedModelsVerticalApp(String responseFilename) {
        doRequest("omm_parallel", x -> x.withoutParam("trim-thumbs"))
            .ok()
            .body(responseFilename);
    }

    public void getRecommendedModelsToloka(String responseFilename) {
        doRequest("omm_toloka", Function.identity())
            .ok()
            .body(responseFilename);
    }

    public void isDeliveryAvailable(String responseFilename, int regionId) {
        doRequest("check_delivery_available",
            x -> x.param("rids", String.valueOf(regionId))
        )
            .ok()
            .body(responseFilename);
    }

    public void getOfflineShopFull(String responseFilename) {
        search("offline_promo",
            x -> x,
            responseFilename
        );
    }

    public void getOfflineShopOffers(long shopId, String responseFilename) {
        search("prime",
            x -> x.param("fesh", String.valueOf(shopId))
                .param("offline-promo-only", "1"),
            responseFilename
        );
    }

    public void getShopOffers(long shopId, String responseFilename) {
        search("prime",
            x -> x.param("fesh", String.valueOf(shopId)),
            responseFilename
        );
    }

    public void getPromos(Collection<String> keys, String responseFilename) {
        search("promo",
            x -> x.param("promoid", String.join(",", keys)),
            responseFilename
        );
    }

    public void checkFilters(int hid, List<String> glFilters, String responseFilename) {
        search("check_filters",
                x -> ApiCollections.reduce(
                        x.param("hid", String.valueOf(hid)),
                        glFilters,
                        (b, s) -> b.param("glfilter", s)
                ),
                responseFilename
        );
    }

    public HttpResponseConfigurer doRequest(String place,
                                            Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder> modifier) {
        return configure(x -> buildRequest(x, place, modifier));
    }

    private HttpRequestExpectationBuilder buildRequest(HttpRequestExpectationBuilder x,
                                                       String place,
                                                       Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder> modifier) {
        return x.get()
                .serverMethod("/yandsearch")
                .param("place", place)
                .apply(modifier);
    }

    @Override
    protected String resolveConfigurationName() {
        Context ctx = ContextHolder.get();
        boolean isBlue = null != ctx && blueRule.test(ctx);
        return isBlue ? "ReportBlue" : "Report";
    }
}
