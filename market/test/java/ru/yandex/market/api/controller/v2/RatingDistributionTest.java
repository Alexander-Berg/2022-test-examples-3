package ru.yandex.market.api.controller.v2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import it.unimi.dsi.fastutil.ints.IntLists;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.domain.Model;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.Rating;
import ru.yandex.market.api.domain.v1.ModelInfoField;
import ru.yandex.market.api.domain.v2.ModelListResult;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferFieldV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.RatingV2;
import ru.yandex.market.api.domain.v2.ShopInfoFieldV2;
import ru.yandex.market.api.domain.v2.ShopInfoV2;
import ru.yandex.market.api.domain.v2.offers.GetOffersByModelResult;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.offer.GetOffersByModelRequest;
import ru.yandex.market.api.pers.ReviewRatingDistributionItem;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.util.concurrent.ApiDeferredResult;
import ru.yandex.market.api.util.httpclient.clients.BukerTestClient;
import ru.yandex.market.api.util.httpclient.clients.PersStaticTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

/**
 * Created by vdorogin on 25.05.17.
 */
public class RatingDistributionTest extends BaseTest {

    @Inject
    CategoryControllerV2 categoryController;
    @Inject
    ModelsControllerV2 modelsController;
    @Inject
    PersStaticTestClient persTestClient;
    @Inject
    BukerTestClient bukerTestClient;
    @Inject
    ReportTestClient reportTestClient;

    /**
     * Проверка получения распределения оценок по моделям
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3860">MARKETAPI-3860: Перейти на пакетные ручки получения распределения оценок по моделям и магазинам</a>
     */
    @Test
    public void modelRatingDistribution() {
        int categoryId = 91491;
        List<Long> modelIds = Arrays.asList(
                13485515L, 12859246L, 13517608L, 13814762L, 13778007L,
                10495456L, 13188749L, 13485518L, 13584121L, 13188751L
        );
        // настройка системы
        context.setVersion(Version.V2_0_3);
        reportTestClient.getCategoryModels(categoryId, "", "personalcategorymodels_91491.json");
        persTestClient.getModelRatingDistribution(modelIds, "models_rating_distribution.json");
        // вызов системы
        ModelListResult result = ((ApiDeferredResult<ModelListResult>) categoryController.getPopularModels(
                null,
                categoryId,
                Collections.singletonList(ModelInfoField.RATING),
                Collections.emptyMap(),
                PageInfo.DEFAULT,
                genericParams,
                null
        )).waitResult();
        // проверка утверждений
        Assert.assertNotNull(result);
        List<? extends Model> models = result.getModels();
        Assert.assertNotNull(models);

        Map<Long, Rating> ratings = models.stream()
                .map(m -> (ModelV2) m)
                .collect(Collectors.toMap(ModelV2::getId, ModelV2::getRating));

        checkRatingDistribution(ratings, 13485515L,
                new RatingItem(1.0f, 0, 0),
                new RatingItem(2.0f, 0, 0),
                new RatingItem(3.0f, 5, 42),
                new RatingItem(4.0f, 3, 25),
                new RatingItem(5.0f, 4, 33)
        );
        checkRatingDistribution(ratings, 13814762L,
                new RatingItem(1.0f, 0, 0),
                new RatingItem(2.0f, 0, 0),
                new RatingItem(3.0f, 0, 0),
                new RatingItem(4.0f, 0, 0),
                new RatingItem(5.0f, 0, 0)
        );
        checkRatingDistribution(ratings, 10495456L,
                new RatingItem(1.0f, 3, 14),
                new RatingItem(2.0f, 2, 9),
                new RatingItem(3.0f, 4, 18),
                new RatingItem(4.0f, 2, 9),
                new RatingItem(5.0f, 11, 50)
        );
    }

    /**
     * Проверка получения распределения оценок по магазинам
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3860">MARKETAPI-3860: Перейти на пакетные ручки получения распределения оценок по моделям и магазинам</a>
     */
    @Test
    public void shopRatingDistribution() {
        long modelId = 12568033L;
        List<Long> shopIds = Arrays.asList(
                255L, 378051L, 228797L, 10204663L, 86195L, 186505L, 211L, 8185L, 288733L, 145969L
        );
        // настройка системы
        context.setVersion(Version.V2_0_3);
        reportTestClient.getModelInfoById(modelId, "modelinfo_12568033.json");
        reportTestClient.getModelOffers(modelId, "productoffers_12568033.json");
        persTestClient.getShopRatingDistribution(shopIds, "shops_rating_distribution.json");
        reportTestClient.getShopsRatings(shopIds, "report_shops_ratings.json");
        // вызов системы
        GetOffersByModelResult result = modelsController.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(modelId)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Arrays.asList(OfferFieldV2.SHOP, ShopInfoFieldV2.RATING))
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(null)
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(null)
                        .setFilterParameters(Collections.emptyMap())
                        .setGpsCoords(null)
                        .setWithModel(true)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        ).waitResult();
        // проверка утверждений
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOffers());

        Map<Long, Rating> ratings = result.getOffers().stream()
                .map(OfferV2::getShop)
                .map(ShopInfoV2.class::cast)
                .filter(shopInfoV2 -> shopInfoV2.getRating() != null)
                .collect(Collectors.toMap(ShopInfoV2::getId, ShopInfoV2::getRating));

        checkRatingDistribution(ratings, 10204663L,
                new RatingItem(1.0f, 0, 0),
                new RatingItem(2.0f, 0, 0),
                new RatingItem(3.0f, 0, 0),
                new RatingItem(4.0f, 1, 100),
                new RatingItem(5.0f, 0, 0)
        );
        checkRatingDistribution(ratings, 288733L,
                new RatingItem(1.0f, 0, 0),
                new RatingItem(2.0f, 0, 0),
                new RatingItem(3.0f, 0, 0),
                new RatingItem(4.0f, 0, 0),
                new RatingItem(5.0f, 0, 0)
        );
        checkRatingDistribution(ratings, 255L,
                new RatingItem(1.0f, 109, 2),
                new RatingItem(2.0f, 56, 1),
                new RatingItem(3.0f, 72, 1),
                new RatingItem(4.0f, 574, 11),
                new RatingItem(5.0f, 4623, 85)
        );
    }

    private void checkRatingDistribution(Map<Long, Rating> ratings, Long modelId, RatingItem... ratingItems) {
        Assert.assertTrue(ratings.containsKey(modelId));
        RatingV2 rating = (RatingV2) ratings.get(modelId);
        List<ReviewRatingDistributionItem> distribution = rating.getRatings();
        Assert.assertNotNull(distribution);
        Assert.assertEquals(ratingItems.length, distribution.size());
        for (int i = 0; i < ratingItems.length; i++) {
            Assert.assertEquals(ratingItems[i].getValue(), distribution.get(i).getValue(), 0.0d);
            Assert.assertEquals(ratingItems[i].getCount(), distribution.get(i).getCount());
            Assert.assertEquals(ratingItems[i].getPercent(), distribution.get(i).getPercent());
        }
    }

    private static class RatingItem {
        private float value;
        private long count;
        private int percent;

        public RatingItem(float value, long count, int percent) {
            this.value = value;
            this.count = count;
            this.percent = percent;
        }

        public float getValue() {
            return value;
        }

        public long getCount() {
            return count;
        }

        public int getPercent() {
            return percent;
        }
    }
}
