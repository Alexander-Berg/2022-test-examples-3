package ru.yandex.market.api.integration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import javax.inject.Inject;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.junit.Test;

import ru.yandex.market.api.domain.Category;
import ru.yandex.market.api.domain.HasIntId;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v1.MatchedCategoryV1;
import ru.yandex.market.api.domain.v2.MatchedCategoryV2;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.model.Source;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.service.match.MatchService;
import ru.yandex.market.api.service.match.MatchType;
import ru.yandex.market.api.service.match.ModelMatchRequest;
import ru.yandex.market.api.service.match.ModelMatchResult;
import ru.yandex.market.api.util.CommonPrimitiveCollections;
import ru.yandex.market.api.util.PrimitiveCollections;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.ClassifierTestClient;
import ru.yandex.market.api.util.httpclient.clients.MatcherTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;
import ru.yandex.market.api.util.httpclient.spi.HttpErrorType;
import ru.yandex.market.ir.http.Classifier;
import ru.yandex.market.ir.http.Matcher;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by tesseract on 01.10.16.
 */
public class MatchServiceTest extends BaseTest {

    @Inject
    private MatchService matchService;

    @Inject
    private MatcherTestClient matcherTestClient;

    @Inject
    private ClassifierTestClient classifierTestClient;

    @Inject
    private ReportTestClient reportTestClient;

    /**
     * Проверяем плучение категории от классификатора
     * <p>
     * Проверяем:
     * <ol>
     * <li>Получили ожидаемый набор категорий</li>
     * <li>У категории получили ожидаемый rank</li>
     * <li>Получили категории соответствующие запросщенной версии</li>
     * </ol>
     */
    @Test
    public void matchCategories_V1_0_0() {
        context.setVersion(Version.V1_0_0);

        String locale = "RU_RU";
        String name = "Canon 5D body";

        classifierTestClient.classify(
            classifierRequest(locale, name, 30),
            "classifier_match_categories_v1_canon.pb"
        );

        List<Category> result = Futures.waitAndGet(matchService.matchCategories(request(locale, name),
                PageInfo.fromTotalPages(1, 30, 1), genericParams));

        assertCategories(result, MatchedCategoryV1.class, c -> ((MatchedCategoryV1) c).getRank());
    }

    /**
     * Проверяем плучение категории от классификатора
     * <p>
     * Проверяем:
     * <ol>
     * <li>Получили ожидаемый набор категорий</li>
     * <li>У категории получили ожидаемый rank</li>
     * <li>Получили категории соответствующие запросщенной версии</li>
     * </ol>
     */
    @Test
    public void matchCategories_V2_0_1() {
        context.setVersion(Version.V2_0_1);

        String locale = "RU_RU";
        String name = "Canon 5D body";

        classifierTestClient.classify(
            classifierRequest(locale, name, 30),
            "classifier_match_categories_v1_canon.pb"
        );

        List<Category> result = Futures.waitAndGet(matchService.matchCategories(request(locale, name),
                PageInfo.fromTotalPages(1, 30, 1),genericParams));

        assertCategories(result, MatchedCategoryV2.class, c -> ((MatchedCategoryV2) c).getRank());
    }

    /**
     * Проверяем плучение категории от классификатора (ошибка соединения)
     * <p>
     * Проверяем:
     * <ol>
     * <li>Было выброшено исключение (спятисотили)</li>
     * </ol>
     */
    @Test(expected = RuntimeException.class)
    public void matchCategories_error_V2_0_1() {

        context.setVersion(Version.V2_0_1);

        String locale = "RU_RU";
        String name = "raise_error";

        classifierTestClient
            .classify(classifierRequest(locale, name, 30))
            .error(HttpErrorType.CONNECT_TIMEOUT);

        Futures.waitAndGet(matchService.matchCategories(request(locale, name),
                PageInfo.fromTotalPages(1, 30, 1), genericParams));
    }

    /**
     * Проверяем работоспособность match-модели в матчере методом BATCH
     *
     * Вызов системы: v2.0.1/models/match?name=canon 5d body&method=batch
     *
     * Проверки:
     * <ol>
     *     <li>Получили результат из матчера</li>
     *     <li>Получили одну модель</li>
     *     <li>Модель совпала с ответом матчера</li>
     * </ol>
     */
    @Test
    public void matchModels_BATCH_V2_0_1() {
        String locale = "RU_RU";
        String name = "Canon 5D body B";

        classifierTestClient.classify(
            classifierRequest(locale, name, 1),
            "classifier_match_models_batch_v2.pb"
        );

        matcherTestClient.matchBatch(
            matcherOfferBatch(matcherOffer(locale, name, 91148)),
            "matcher_match_models_batch_v2.pb"
        );

        reportTestClient.getModelInfoById(
            779332,
            "report_model_info_match_models_batch_v2.json"
        );

        matchModels(
            request(locale, name),
            1,
            Collections.singleton(MatchType.BATCH),
            Source.MATCHER,
            new long[]{779332}
        );
    }

    /**
     * Проверяем работоспособность match-модели в матчере методом MULTY
     *
     * Вызов системы: v2.0.1/models/match?name=canon 5d body&method=multy
     *
     * Проверки:
     * <ol>
     *     <li>Получили результат из матчера</li>
     *     <li>Получили одну модель</li>
     *     <li>Модель совпала с ответом матчера</li>
     * </ol>
     */
    @Test
    public void matchModels_MULTI_V2_0_1() {
        String locale = "RU_RU";
        String name = "Canon 5D body m";

        classifierTestClient.classify(
            classifierRequest(locale, name, 1),
            "classifier_match_models_multi_v2.pb"
        );

        matcherTestClient.multiMatch(
            matcherOffer(locale, name, 91148),
            "matcher_match_models_multi_v2.pb"
        );

        reportTestClient.getModelInfoById(
            779332,
            "report_model_info_match_models_mutli.json"
        );

        matchModels(
            request(locale, name),
            1,
            Collections.singleton(MatchType.MULTI),
            Source.MATCHER,
            new long[]{779332}
        );
    }

    /**
     * Проверяем работоспособность match-модели в матчере методом STRING
     *
     * Вызов системы: v2.0.1/models/match?name=canon 5d body&method=string
     *
     * Проверки:
     * <ol>
     *     <li>Получили результат из матчера</li>
     *     <li>Получили одну модель</li>
     *     <li>Модель совпала с ответом матчера</li>
     * </ol>
     */
    @Test
    public void matchModels_STRING_V2_0_1() {
        String locale = "RU_RU";
        String name = "Canon 5D body s";

        matcherTestClient.matchString(
            text(locale, name),
            "matcher_match_models_string_v2.pb"
        );

        reportTestClient.getModelInfoById(
            779332,
            "report_model_info_match_models_string_v2.json"
        );

        matchModels(
            request(locale, name),
            1,
            Collections.singleton(MatchType.STRING),
            Source.MATCHER,
            new long[]{779332}
        );
    }

    /**
     * Проверяем работоспособность match-модели в матчере методом MULTI_STRING
     *
     * Вызов системы: v2.0.1/models/match?name=canon 5d body&method=multi_string
     *
     * Проверки:
     * <ol>
     *     <li>Получили результат из матчера</li>
     *     <li>Получили одну модель</li>
     *     <li>Модель совпала с ответом матчера</li>
     * </ol>
     */
    @Test
    public void matchModels_MULTI_STRING_V2_0_1() {
        String locale = "RU_RU";
        String name = "Canon 5D body ms";

        matcherTestClient.multiMatchString(
            text(locale, name),
            "matcher_match_models_multi_string_v2.pb"
        );

        reportTestClient.getModelInfoById(
            779332,
            "report_model_info_match_models_mutli_string_v2.json"
        );

        matchModels(
            request(locale, name),
            1,
            Collections.singleton(MatchType.MULTI_STRING),
            Source.MATCHER,
            new long[]{779332}
        );
    }

    /**
     * Проверяем работоспособность match-модели в матчере методом REPORT
     *
     * Вызов системы: v2.0.1/models/match?name=canon 5d body&method=report
     *
     * Проверки:
     * <ol>
     *     <li>Получили результат из матчера</li>
     *     <li>Получили одну модель</li>
     *     <li>Модель совпала с ответом матчера</li>
     * </ol>
     */
    @Test
    public void matchModels_REPORT_V2_0_1() {
        String locale = "RU_RU";
        String matchText = "Canon 5D body r";
        int categoriesCount = 1;
        long[] modelIds = new long[]{2638137, 7888705, 779332, 14202707, 2638143};

        classifierTestClient.classify(
            classifierRequest(locale, matchText, categoriesCount),
            "classifier_match_models_report_v2.pb"
        );

        reportTestClient.searchV2(matchText, "report_prime_match_models_report_v2.json");

        reportTestClient.getModelInfoById(
            new LongArrayList(modelIds),
            "report_model_info_match_models_report_v2.json"
        );

        matchModels(
            request(locale, matchText),
            categoriesCount,
            Collections.singleton(MatchType.REPORT),
            Source.REPORT,
            modelIds
        );
    }


    private Classifier.ClassificationRequest classifierRequest(String locale,
                                                               String name,
                                                               int categoriesCount) {
        return Classifier.ClassificationRequest.newBuilder()
            .setResultCategoriesCount(categoriesCount)
            .addOffer(
                Classifier.Offer.newBuilder()
                    .setLocale(locale)
                    .setTitle(name)
            )
            .build();
    }

    private Matcher.OfferBatch matcherOfferBatch(Matcher.Offer... offers) {
        Matcher.OfferBatch.Builder batchBuilder = Matcher.OfferBatch.newBuilder();
        Arrays.stream(offers).forEach(batchBuilder::addOffer);
        return batchBuilder.build();

    }

    private Matcher.Offer matcherOffer(String locale, String name, int hid) {
        return Matcher.Offer.newBuilder()
            .setHid(hid)
            .setLocale(locale)
            .setTitle(name)
            .build();
    }

    private Matcher.LocalizedText text(String locale, String name) {
        return Matcher.LocalizedText.newBuilder()
            .setLocale(locale)
            .setText(name)
            .build();
    }

    private ModelMatchRequest request(String locale, String name) {
        ModelMatchRequest request = new ModelMatchRequest();
        request.setLocale(locale);
        request.setName(name);
        return request;
    }

    private void matchModels(ModelMatchRequest request,
                             int checkCategories,
                             Set<MatchType> matchTypes,
                             Source expectedSource,
                             long[] expectedModels) {

        context.setVersion(Version.V2_0_1);
        context.setPpList(IntLists.EMPTY_LIST);

        ModelMatchResult<ModelV2> result = Futures.waitAndGet(
            matchService.matchModels(request, null,
                matchTypes, checkCategories, Collections.emptyList(), genericParams)
        );

        assertEquals(
            "Должны получить модель из " + expectedSource,
            expectedSource,
            result.getSource()
        );

        LongList actualModels = CommonPrimitiveCollections.transform(result.getModels(), ModelV2::getId);
        System.out.println("!! " + actualModels);
        assertArrayEquals(
            "Модель должна совпадать с ответом матчера",
            expectedModels,
            actualModels.toLongArray()
        );

    }

    private void assertCategories(List<Category> result, Class<?> clazz, ToDoubleFunction<Category> rankExtractor) {
        assertNotNull(result);

        assertTrue(
            "В ответе должны содержаться только " + clazz.getSimpleName(),
            Iterables.all(result, Predicates.instanceOf(clazz))
        );

        int[] expectedIds = new int[]{91148, 91153, 91031, 91042, 90613, 90635, 138608,
            91526, 90617, 8475955, 91156, 308025, 7766764, 91524, 91176, 90434, 984941, 984940, 10983253};
        IntList actualsIds = CommonPrimitiveCollections.transform(result, HasIntId::getId);
        assertArrayEquals(
            "Список категории должен совпадать с ответом от классификатора",
            expectedIds,
            actualsIds.toIntArray()
        );

        double[] expectedRanks = new double[]{-0.8738041813132762, -2.569717566799069, -3.2915656626424328,
            -3.677472255174113, -3.7844842661983344, -3.8042652086760382, -4.142398501654421,
            -4.32234824400321, -4.467902477635581, -4.649725801199532, -4.734350067265173,
            -4.8871822032797425, -5.028089464204164, -5.174595584325422, -5.390119047245684,
            -5.423422559346355, -5.804638595178778, -6.015501863744683, -8.100748348991958};
        DoubleList actualRanks = CommonPrimitiveCollections.transform(result, (ToDoubleFunction) rankExtractor);
        assertArrayEquals(
            "Список rank-ов должен совпадать с ответом от классификатора",
            expectedRanks,
            actualRanks.toDoubleArray(),
            0.00001
        );
    }
}
