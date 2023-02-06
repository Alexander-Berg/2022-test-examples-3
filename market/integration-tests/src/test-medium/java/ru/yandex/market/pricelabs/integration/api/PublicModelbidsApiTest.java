package ru.yandex.market.pricelabs.integration.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.api.api.PublicModelbidsApi;
import ru.yandex.market.pricelabs.api.api.PublicModelbidsApiInterfaces;
import ru.yandex.market.pricelabs.generated.server.pub.model.ModelsBidsRecommendationRequest;
import ru.yandex.market.pricelabs.generated.server.pub.model.RecommendationBid;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.MODEL_ID_1;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.ZERO_MODEL_ID;

public class PublicModelbidsApiTest extends AbstractApiTests {

    @Autowired
    private PublicModelbidsApi publicApiBean;
    private PublicModelbidsApiInterfaces publicApi;

    @BeforeEach
    void init() {
        publicApi = buildProxy(PublicModelbidsApiInterfaces.class, publicApiBean);
        super.init();
    }

    @Test
    public void modelbidsRecommendation() {
        ModelsBidsRecommendationRequest modelsBidsRecommendationRequest
                = new ModelsBidsRecommendationRequest();
        modelsBidsRecommendationRequest.setTimePeriodMillis(null);
        modelsBidsRecommendationRequest.setModels(singletonList(MODEL_ID_1));

        var ret = checkResponse(publicApi.modelbidsRecommendationPost(modelsBidsRecommendationRequest));
        List<Integer> modelIds = new ArrayList<>();
        List<Integer> estimatePositions = new ArrayList<>();
        Map<Integer, List<RecommendationBid>> modelIdPositions = new HashMap<>();
        ret.forEach(it -> {
            modelIds.add(it.getModelId());
            estimatePositions.add(it.getEstimatePosition());
            modelIdPositions.put(it.getModelId(), it.getPositions());
        });

        assertEquals(1, modelIdPositions.size());
        assertTrue(modelIds.contains(MODEL_ID_1));
        assertTrue(estimatePositions.contains(44));

        modelIdPositions.get(MODEL_ID_1).forEach(
                it -> {
                    if (it.getVbid() == 4 || it.getVbid() == 3) {
                        assertEquals(1, it.getPosition());
                    }
                    if (it.getVbid() == 14 || it.getVbid() == 13) {
                        assertEquals(2, it.getPosition());
                    }
                }
        );
    }

    @Test
    void zeroModelbidsRecommendation() {
        var ret = checkResponse(publicApi.zeroModelbidsRecommendationGet(225, null, 0, 1));
        ret.forEach(it -> {
            assertEquals(ZERO_MODEL_ID, it.getModelId());
            assertEquals(56, it.getEstimatePosition());
            assertEquals(3, it.getCategoryId());
            assertEquals(5, it.getBrandId());
        });

        assertEquals(1, ret.size());
    }

}
