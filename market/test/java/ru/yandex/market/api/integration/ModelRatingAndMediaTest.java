package ru.yandex.market.api.integration;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.domain.Model;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.Rating;
import ru.yandex.market.api.domain.v1.ModelInfo;
import ru.yandex.market.api.domain.v1.ModelInfoField;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.RatingV2;
import ru.yandex.market.api.model.AbstractModelV1;
import ru.yandex.market.api.model.ModelService;
import ru.yandex.market.api.server.version.ModelVersion;
import ru.yandex.market.api.util.CommonPrimitiveCollections;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.BukerTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

/**
 * Created by vdorogin on 06.06.17.
 */
public class ModelRatingAndMediaTest extends BaseTest {

    private static final List<Long> POPULAR_IDS = Arrays.asList(
        13485515L, 11136761L, 12372911L, 6189220L, 12206903L, 13964505L, 10476846L, 13781933L, 10853126L,
        1724912762L, 1711072152L, 13321558L, 4679555L, 14211948L, 12320741L, 14229140L, 10476849L, 1712956714L,
        12506314L, 10567419L, 12833785L, 12880557L, 13576873L, 12180463L, 1723842096L, 7085897L, 5071668L,
        13910985L, 1722356245L, 11614581L, 10727884L, 1720941533L, 10833015L);

    @Inject
    private ModelService modelService;

    @Inject
    private ReportTestClient reportTestClient;

    @Inject
    private BukerTestClient bukerTestClient;

    /**
     * Проверка рейтинга модели при отсутствии рейтинга в букере
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3619">MARKETAPI-3619: Лишние походы в букер за рейтингом</a>
     */
    @Test
    public void modelRatingAbsenceV1() {
        Long id = 10608843L;
        // настройка системы
        reportTestClient.getModelInfoById(id, "modelinfo_10608843.json");
        bukerTestClient.getModelRatingCards(Collections.singleton(id), "buker_empty_result.xml");
        // вызов системы
        AbstractModelV1 model = Futures.waitAndGet(modelService.getModelV1(
            id,
            false,
            Collections.emptyMap(),
            genericParams,
            Collections.emptySet(),
            true
        ));
        // проверка утверждений
        Assert.assertNotNull(model);
        Assert.assertEquals(-1, model.getRating(), 0.1);
        Assert.assertEquals(0, model.getGradeCount());
    }

    /**
     * Проверка рейтинга модели при отсутствии рейтинга в букере
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3619">MARKETAPI-3619: Лишние походы в букер за рейтингом</a>
     */
    @Test
    public void modelRatingPresenceV1() {
        Long id = 12320741L;
        // настройка системы
        reportTestClient.getModelInfoById(id, "modelinfo_12320741.json");
        bukerTestClient.getModelRatingCards(Collections.singleton(id), "buker_models_rating_12320741.xml");
        // вызов системы
        AbstractModelV1 model = Futures.waitAndGet(modelService.getModelV1(
            id,
            false,
            Collections.emptyMap(),
            genericParams,
            Collections.emptySet(),
            true
        ));
        // проверка утверждений
        Assert.assertNotNull(model);
        Assert.assertEquals(5.0d, model.getRating(), 0.1);
        Assert.assertEquals(9, model.getGradeCount());
    }

    /**
     * Проверка рейтинга модели в популярных моделях v1
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3619">MARKETAPI-3619: Лишние походы в букер за рейтингом</a>
     */
    @Test
    public void modelRatingInPopularModelsV1() {
        // настройка системы
        reportTestClient.popularProducts("report_popular_products.json");
        bukerTestClient.getModelRatingCards(POPULAR_IDS, "buker_popular_models_rating.xml");
        // вызов системы
        List<ModelInfo> models = Futures.waitAndGet(modelService.getPopularModelsV1(
            false,
            new PageInfo(1, 30),
            Arrays.asList(ModelInfoField.RATING),
            genericParams
        ));
        // проверка утверждений
        Assert.assertNotNull(models);
        checkModelRating(models, 13485515L, 4.0f, 22);
        checkModelRating(models, 10853126L, -1.0f, 0);
        // для модификации значения должны браться из родителя
        checkModelRating(models, 4679555L, 2.5f, 7);
    }

    /**
     * Проверка media в популярных моделях v1
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3619">MARKETAPI-3619: Лишние походы в букер за рейтингом</a>
     */
    @Test
    public void modelMediaInPopularModelsV1() {
        // настройка системы
        reportTestClient.popularProducts("report_popular_products.json");
        bukerTestClient.getModelRatingCards(POPULAR_IDS, "buker_popular_models_rating.xml");
        // вызов системы
        List<ModelInfo> models = Futures.waitAndGet(modelService.getPopularModelsV1(
            false,
            new PageInfo(1, 30),
            Arrays.asList(ModelInfoField.MEDIA),
            genericParams
        ));
        // проверка утверждений
        Assert.assertNotNull(models);
        checkModelMedia(models, 13485515L, 28, 12);
        checkModelMedia(models, 10853126L, 0, 0);
        // для модификации значения должны браться из родителя
        checkModelMedia(models, 4679555L, 0, 6);
    }

    /**
     * Проверка рейтинга в моделях v2
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3619">MARKETAPI-3619: Лишние походы в букер за рейтингом</a>
     */
    @Test
    public void modelsRatingV2() {
        List<Long> ids = Arrays.asList(13485515L, 10853126L, 4679555L);
        List<Long> bukerIds = Arrays.asList(13485515L, 5071668L, 10853126L, 4679555L);
        // настройка системы
        reportTestClient.getModelInfoById(ids, "report_models_rating_v2.json");
        // вызов системы
        List<Model> models = Futures.waitAndGet(modelService.getModels(
            CommonPrimitiveCollections.asList(13485515L, 10853126L, 4679555L),
            Collections.emptyMap(),
            Arrays.asList(ModelInfoField.RATING),
            genericParams,
            ModelVersion.V2
        ));
        // проверка утверждений
        Assert.assertNotNull(models);
        checkModelRatingV2(models, 13485515L, new BigDecimal("4"),new BigDecimal("5"), 22);
        checkModelRatingV2(models, 10853126L, BigDecimal.ONE.negate(),BigDecimal.ONE.negate(), 0);
        // для модификации значения должны браться из родителя
        checkModelRatingV2(models, 4679555L, new BigDecimal("2.5"), new BigDecimal("3.5"), 7);
    }

    /**
     * Проверка media в моделях v2
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3619">MARKETAPI-3619: Лишние походы в букер за рейтингом</a>
     */
    @Test
    public void modelsMediaV2() {
        List<Long> ids = Arrays.asList(13485515L, 10853126L);
        // настройка системы
        reportTestClient.getModelInfoById(ids, "report_models_rating_v2.json");
        // вызов системы
        List<Model> models = Futures.waitAndGet(modelService.getModels(
            CommonPrimitiveCollections.asList(13485515L, 10853126L),
            Collections.emptyMap(),
            Arrays.asList(ModelInfoField.MEDIA),
            genericParams,
            ModelVersion.V2
        ));
        // проверка утверждений
        Assert.assertNotNull(models);
        checkModelMediaV2(models, 13485515L, 28, 12);
        checkModelMediaV2(models, 10853126L, 0, 0);
    }

    /**
     * Проверка media в моделях (модификациях) v2
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3619">MARKETAPI-3619: Лишние походы в букер за рейтингом</a>
     */
    @Test
    public void modificationsMediaV2() {
        Long id = 4679555L;
        // настройка системы
        reportTestClient.getModelInfoById(id, "report_models_rating_v2.json");
        // вызов системы
        List<Model> models = Futures.waitAndGet(modelService.getModels(
            CommonPrimitiveCollections.asList(id),
            Collections.emptyMap(),
            Arrays.asList(ModelInfoField.MEDIA),
            genericParams,
            ModelVersion.V2
        ));
        // проверка утверждений
        Assert.assertNotNull(models);
        // для модификации значения должны браться из родителя
        checkModelMediaV2(models, 4679555L, 0, 6);
    }

    private void checkModelRating(List<ModelInfo> models, Long id, float value, int count) {
        ModelInfo model = models.stream()
            .filter(m -> id == m.getId())
            .findFirst()
            .orElseThrow(() -> new AssertionError("В выдаче отсутствует модель " + id));
        Assert.assertNotNull(model.getRating());
        Assert.assertEquals(value, model.getRating().getRating(), 0.01);
        Assert.assertEquals(count, model.getRating().getCount());
    }

    private void checkModelMedia(List<ModelInfo> models, Long id, int articles, int reviews) {
        ModelInfo model = models.stream()
            .filter(m -> id == m.getId())
            .findFirst()
            .orElseThrow(() -> new AssertionError("В выдаче отсутствует модель " + id));
        Assert.assertNotNull(model.getMedia());
        Assert.assertEquals(articles, model.getMedia().getArticles());
        Assert.assertEquals(reviews, model.getMedia().getReviews());
    }

    private void checkModelRatingV2(List<Model> models, Long id, BigDecimal value, BigDecimal preciseValue, int count) {
        Model model = models.stream()
            .filter(m -> id == m.getId())
            .findFirst()
            .orElseThrow(() -> new AssertionError("В выдаче отсутствует модель " + id));
        Rating rating = ((ModelV2) model).getRating();
        Assert.assertNotNull(rating);
        RatingV2 ratingV2 = (RatingV2) rating;
        Assert.assertEquals(value, ratingV2.getRating());
        Assert.assertEquals(preciseValue, ratingV2.getPreciseRating());
        Assert.assertEquals(count, ratingV2.getCount());
    }

    private void checkModelMediaV2(List<Model> models, Long id, Integer articles, Integer reviews) {
        Model model = models.stream()
            .filter(m -> id == m.getId())
            .findFirst()
            .orElseThrow(() -> new AssertionError("В выдаче отсутствует модель " + id));
        ModelV2 modelV2 = (ModelV2) model;
        Assert.assertEquals(reviews, modelV2.getOpinionCount());
        Assert.assertEquals(articles, modelV2.getReviewCount());
    }
}
