package ru.yandex.market.api.util.httpclient.clients;

import org.springframework.stereotype.Service;
import ru.yandex.market.api.util.httpclient.spi.HttpRequestExpectationBuilder;

import javax.inject.Inject;
import java.util.Collection;

/**
 * @author dimkarp93
 */
@Service
public class PersStaticTestClient extends AbstractFixedConfigurationTestClient {

    @Inject
    public PersStaticTestClient() {
        super("PersStatic");
    }

    public void getShopOpinion(long shopId, String expectedFileName) {
        httpExpectations.configure(persStatic()
            .serverMethod("/api/opinion/shop/" + String.valueOf(shopId)))
            .body(expectedFileName)
            .ok();
    }

    public void getModelOpinion(long modelId, String expectedFileName) {
        httpExpectations.configure(persStatic()
            .serverMethod("/api/opinion/model/" + String.valueOf(modelId)))
            .body(expectedFileName)
            .ok();
    }

    public void getShopRatingDistribution(Collection<Long> shopIds, String expectedFileName) {
        getRatingDistribution("shop", "shopid", shopIds, expectedFileName);
    }

    public void getModelRatingDistribution(Collection<Long> modelIds, String expectedFileName) {
        getRatingDistribution("model", "modelid", modelIds, expectedFileName);
    }

    private void getRatingDistribution(String distributionScope, String distributionScopeItem,
                                       Collection<Long> ids, String expectedFileName) {
        HttpRequestExpectationBuilder builder = persStatic().serverMethod("/api/distribution/" + distributionScope);
        ids.stream().map(String::valueOf).forEach(id -> builder.param(distributionScopeItem, id));
        httpExpectations.configure(builder)
                .body(expectedFileName)
                .ok();
    }

    private HttpRequestExpectationBuilder persStatic() {
        return new HttpRequestExpectationBuilder()
                .get();
    }
}
