package ru.yandex.market.api.util.httpclient.clients;

import com.google.common.base.Joiner;
import org.springframework.stereotype.Service;
import ru.yandex.market.api.util.httpclient.spi.HttpRequestExpectationBuilder;

import java.util.Collection;
import java.util.function.Function;

/**
 * Created by anton0xf on 24.01.17.
 */
@Service
public class BukerTestClient extends AbstractFixedConfigurationTestClient {

    public BukerTestClient() {
        super("Buker");
    }

    public void getVendor(Collection<Long> ids, String expectedResponseFileName) {
        getCards(ids, "vendors", expectedResponseFileName);
    }

    public void getVendorRecommendedShopsUrls(Collection<Long> ids, String expectedResponseFileName) {
        getCards(ids, "vendor-recommended-shops", expectedResponseFileName);
    }

    public void getModelRatingCards(Collection<Long> ids, String expectedResponseFileName) {
        getCards(ids, "models-rating", expectedResponseFileName);
    }

    public void getFilteredPageBySemanticId(String semanticId, String expectedResponseFileName) {
        configure(b -> b
            .param("semantic_id", semanticId)
            .apply(setCollection("filtered-pages")))
            .ok().body(expectedResponseFileName);
    }

    public void getFilterDescription(Collection<Long> ids, String expectedResponseFileName) {
        getCards(ids, "filters-description", expectedResponseFileName);
    }

    public void getGurulightFilterDescription(Collection<Long> ids, String expectedResponseFileName) {
        getCards(ids, "gurulight-filters-description", expectedResponseFileName);
    }


    private void getCards(Collection<Long> ids, String collection, String expectedResponseFileName) {
        configure(b -> b
            .param("ids", Joiner.on(",").join(ids))
            .apply(setCollection(collection)))
            .ok().body(expectedResponseFileName);
    }

    private Function<HttpRequestExpectationBuilder, HttpRequestExpectationBuilder>
                        setCollection(String collection) {
        return b -> b.serverMethod("buker/GetCards").get()
                .param("collection", collection);
    }

}
