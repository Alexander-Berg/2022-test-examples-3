package ru.yandex.direct.libs.collections;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.asynchttp.FetcherSettings;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.libs.collections.model.serpdata.CollectionSerpData;
import ru.yandex.direct.libs.collections.model.serpdata.CollectionSerpDataResult;
import ru.yandex.direct.tvm.TvmIntegrationStub;
import ru.yandex.direct.tvm.TvmService;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Тесты ходят в реальные сервисы, для тестирования нужно подложить TVM тикет. Для этого, например, можно:
 * <ol>
 * <li>Локально (или на бете) запустить {@link ru.yandex.direct.web.DirectWebApp}.</li>
 * <li>Сгенерировать запрос к Коллекциям через ручку
 * {@link ru.yandex.direct.grid.processing.service.contentpromotion.ContentPromotionGraphQlService#getContentPromotionMeta}
 * .</li>
 * <li>Поставить точку останова в {@link ru.yandex.direct.tvm.TvmIntegrationImpl#getTicket(TvmService)} и скопировать
 * полученный тикет (проверить, что тикет выдан для Коллекций).</li>
 * <li>Возвращать полученный тикет в {@link TvmIntegrationStub#getTicket(TvmService)}.</li>
 * <li>Возвращать {@code true} в {@link TvmIntegrationStub#isEnabled()}.</li>
 * <li>После всех проверок не забыть откатить в коде изменения из предыдущих пунктов (коммитить их не нужно)!</li>
 * </ol>
 *
 * <b>N.B.</b>
 * <ol>
 * <li>Для тестов {@link CollectionsClientTest#getCollectionId_CorrectIdReturned()} и
 * {@link CollectionsClientTest#getCollectionSerpData_CorrectCollectionSerpDataReturned()}
 * нужен тикет из продовых Коллекций.</li>
 * <li>Тест {@link CollectionsClientTest#getCollectionSerpDataResult_Testing_TvmError_CollectionNotFoundIsFalse()}
 * рассчитан на отсутсвие TVM тикета.</li>
 * <li>При отсутствии тикета Коллекции отвечают ошибкой 403.</li>
 * </ol>
 */
public class CollectionsClientTest {

    private static final String COLLECTION_URL = "https://yandex.ru/collections/user/assessors-pdb/bmw/";
    private static final String COLLECTION_ID = "5a9197470c1ed2230cb550cf";

    private static final String TEST_COLLECTION_URL = "https://l7test.yandex.ru/collections/user/yakudzablr/tupye-kartinochki/";
    private static final String TEST_COLLECTION_ID = "564c9d46d7208b001816e0ea";

    private static final String TEST_COLLECTION_SERP_DATA_FILENAME = "test_collection_serp_data.json";

    private String testCollectionSerpDataJson;

    private CollectionsClient collectionsClientTest;
    private CollectionsClient collectionsClient;

    @Before
    public void before() throws IOException {
        collectionsClientTest =
                new CollectionsClient("https://l7test.yandex.ru/collections/api", new ParallelFetcherFactory(
                        new DefaultAsyncHttpClient(), new FetcherSettings()), new TvmIntegrationStub(), TvmService.DIRECT_DEVELOPER);
        collectionsClient =
                new CollectionsClient("https://yandex.ru/collections/api", new ParallelFetcherFactory(
                        new DefaultAsyncHttpClient(), new FetcherSettings()), new TvmIntegrationStub(), TvmService.DIRECT_DEVELOPER);

        testCollectionSerpDataJson = IOUtils.toString(getClass().getResourceAsStream(TEST_COLLECTION_SERP_DATA_FILENAME), UTF_8);
    }

    @Test
    @Ignore("Ходит в реальную систему")
    public void getCollectionId_Testing_CorrectIdReturned() {
        String collectionId = collectionsClientTest.getCollectionId(TEST_COLLECTION_URL);

        assertThat(collectionId, is(TEST_COLLECTION_ID));
    }

    @Test
    @Ignore("Ходит в реальную систему")
    public void getCollectionSerpData_Testing_CorrectCollectionSerpDataReturned() {
        CollectionSerpData collectionSerpData = collectionsClientTest.getCollectionSerpData(TEST_COLLECTION_ID);

        assertThat(collectionSerpData, notNullValue());
        assertThat(collectionSerpData, is(CollectionSerpData.fromJson(testCollectionSerpDataJson)));
    }

    @Test
    @Ignore("Ходит в реальную систему")
    public void getCollectionSerpDataResult_Testing_TvmError_CollectionNotFoundIsTrue() {
        CollectionSerpDataResult result = collectionsClientTest.getCollectionSerpDataResult(TEST_COLLECTION_ID + "kek");

        assertThat(result.isSuccessfulResult(), is(false));
        assertThat(result.isCollectionNotFoundResult(), is(true));
    }

    @Test
    @Ignore("Ходит в реальную систему")
    public void getCollectionSerpDataResult_Testing_TvmError_CollectionNotFoundIsFalse() {
        CollectionSerpDataResult result = collectionsClientTest.getCollectionSerpDataResult(TEST_COLLECTION_ID);

        assertThat(result.isSuccessfulResult(), is(false));
        assertThat(result.isCollectionNotFoundResult(), is(false));
    }

    @Test
    @Ignore("Ходит в реальную систему")
    public void getCollectionId_CorrectIdReturned() {
        String collectionId = collectionsClient.getCollectionId(COLLECTION_URL);

        assertThat(collectionId, is(COLLECTION_ID));
    }

    @Test
    @Ignore("Ходит в реальную систему")
    public void getCollectionSerpData_CorrectCollectionSerpDataReturned() {
        CollectionSerpData collectionSerpData = collectionsClient.getCollectionSerpData(COLLECTION_ID);

        assertThat(collectionSerpData, notNullValue());
        assertThat(collectionSerpData.getId(), is(COLLECTION_ID));
    }
}
