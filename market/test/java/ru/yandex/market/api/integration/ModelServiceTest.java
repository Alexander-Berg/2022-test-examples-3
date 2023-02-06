package ru.yandex.market.api.integration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.netty.handler.codec.http.HttpResponseStatus;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.longs.LongList;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.common.client.KnownMobileClientVersionInfo;
import ru.yandex.market.api.common.client.SemanticVersion;
import ru.yandex.market.api.domain.Category;
import ru.yandex.market.api.domain.Model;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v1.CategoryInfo;
import ru.yandex.market.api.domain.v1.ModelInfoField;
import ru.yandex.market.api.domain.v2.CategoryV2;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.RatingV2;
import ru.yandex.market.api.domain.v2.SpecificationGroup;
import ru.yandex.market.api.domain.v2.filters.Filter;
import ru.yandex.market.api.error.NotFoundException;
import ru.yandex.market.api.internal.common.DeviceType;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.Platform;
import ru.yandex.market.api.internal.guru.ModelType;
import ru.yandex.market.api.model.ModelService;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.Uuid;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.CommonPrimitiveCollections;
import ru.yandex.market.api.util.PagedResult;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.BukerTestClient;
import ru.yandex.market.api.util.httpclient.clients.PersStaticTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;
import ru.yandex.market.api.util.httpclient.spi.HttpErrorType;

/**
 * Created by tesseract on 27.09.16.
 */
@WithContext
@ActiveProfiles(ru.yandex.market.api.integration.RedirectControllerV2Test.PROFILE)
public class ModelServiceTest extends BaseTest {

    private static final long MODEL_1_ID = 13485518;
    private static final long[] MODEL_1_LOOKSAS_IDS = {13485515, 14123351, 12790257, 12859245, 13769875, 12911822,
        12259780, 13934014, 12788838, 13521876};


    static final String PROFILE = "ModelServiceTest";

    @Configuration
    @Profile(PROFILE)
    public static class Config {
        @Bean
        @Primary
        public ClientHelper localHelper() {
            return Mockito.mock(ClientHelper.class);
        }
    }

    @Inject
    private ModelService modelService;

    @Inject
    private ReportTestClient reportTestClient;

    @Inject
    private BukerTestClient bukerTestClient;

    @Inject
    private PersStaticTestClient persStaticTestClient;

    @Inject
    private ClientHelper clientHelper;

    private MockClientHelper mockClientHelper;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mockClientHelper = new MockClientHelper(clientHelper);
    }

    /**
     * Тест получения модели по ее id (id=13485518)
     * <p>
     * Запрашиваем модель с минимальным кол-вом полей (fields-пусто)
     * <p>
     * Проверяем:
     * <ol>
     * <li>Получили модель по id</li>
     * <li>Получили модель с нужным id</li>
     * <li>Названием модели совпадает с ожидаемым</li>
     * <li>Тип модели совпадает с ожидаемым</li>
     * <li>Не заполнены поля: AlternatePrice, Category, Facts, NavigationNode, Offer, Photo, Photos</li>
     * </ol>
     * <p>
     */
    @Test
    public void getModelById_V2_0_1() {
        // настройка системы
        context.setVersion(Version.V2_0_1);

        reportTestClient.getModelInfoById(MODEL_1_ID, "modelinfo_13485518.json");
        // вызов системы
        ModelV2 model = Futures.waitAndGet(modelService.getModel(MODEL_1_ID, Collections.emptyList(), genericParams));
        // Проверка утверждений
        Assert.assertEquals("id модели должен соответствовать запрошенному", MODEL_1_ID, model.getId());
        Assert.assertEquals("Название модели не совпадает", "Samsung Galaxy S7 Edge 32Gb", model.getName());
        Assert.assertEquals("Тип модели не совпадает", ModelType.MODEL, model.getType());

        Assert.assertNull("AlternatePrice", model.getAlternatePrice());
        Assert.assertNull("Category", model.getCategory());
        Assert.assertNull("Facts", model.getFacts());
        Assert.assertNull("Filters", model.getFilters());
        Assert.assertNull("NavigationNode", model.getNavigationNode());
        Assert.assertNull("Offer", model.getOffer());
        Assert.assertNull("Photo", model.getPhoto());
        Assert.assertNull("Photos", model.getPhotos());
        // Очистка
    }

    /**
     * Тест получения модели по ее id (id=123 - не существует)
     * <p>
     * Запрашиваем модель с минимальным кол-вом полей (fields-пусто)
     * <p>
     * Проверяем:
     * <ol>
     * <li>модель не найдена</li>
     * </ol>
     * <p>
     */
    @Test(expected = NotFoundException.class)
    public void getModelById_NotFound_V2_0_1() {
        // настройка системы
        context.setVersion(Version.V2_0_1);

        reportTestClient.getModelInfoById(123, "modelinfo_123.json");
        // вызов системы
        Futures.waitAndGet(modelService.getModel(123, Collections.emptyList(), genericParams));
        // Проверка утверждений
        // Очистка
    }

    /**
     * Тест получения модели по ее id  в случае ошибки соединения с репортом
     * <p>
     * Запрашиваем модель с минимальным кол-вом полей (fields-пусто)
     * <p>
     * Проверяем:
     * <ol>
     * <li>модель не найдена</li>
     * </ol>
     * <p>
     */
    @Test(expected = NotFoundException.class)
    public void getModelById_Timeout_V2_0_1() {
        // настройка системы
        reportTestClient.getModelInfoById(MODEL_1_ID)
            .timeout(10)
            .error(HttpErrorType.CONNECT_TIMEOUT)
            .status(HttpResponseStatus.BAD_GATEWAY)
            .body("modelinfo_123.json");

        context.setVersion(Version.V2_0_1);
        // вызов системы
        Futures.waitAndGet(modelService.getModel(MODEL_1_ID, Collections.emptyList(), genericParams));
        // Проверка утверждений
        // Очистка
    }

    /**
     * Тест получения модели по ее id  в случае неответа репорта за укакзанное время
     * <p>
     * Запрашиваем модель с минимальным кол-вом полей (fields-пусто)
     * <p>
     * Проверяем:
     * <ol>
     * <li>модель не найдена</li>
     * </ol>
     * <p>
     */
    @Test(expected = NotFoundException.class)
    public void getModelById_Cancel_V2_0_1() {
        // настройка системы
        context.setVersion(Version.V2_0_1);
        // вызов системы
        Futures.waitAndGet(modelService.getModel(13485518, Collections.emptyList(), genericParams));
        // Проверка утверждений
        // Очистка
    }

    /**
     * Тест получения модели по ее id (id=13485518)
     * <p>
     * Запрашиваем модель c fields=CATEGORY
     * <p>
     * Проверяем:
     * <ol>
     * <li>Получили модель по id</li>
     * <li>Модель содержит категорию и эта категория является категорией "Мобильные телефоны"</li>
     * </ol>
     * <p>
     */
    @Test
    public void getModelByIdWithCategory_V2_0_1() {
        // настройка системы
        context.setVersion(Version.V2_0_1);

        reportTestClient.getModelInfoById(MODEL_1_ID, "modelinfo_13485518.json");
        // вызов системы
        ModelV2 model = Futures.waitAndGet(modelService.getModel(MODEL_1_ID, Lists.newArrayList(ModelInfoField.CATEGORY), genericParams));
        // Проверка утверждений
        Assert.assertEquals("id модели должен соответствовать запрошенному", MODEL_1_ID, model.getId());
        Assert.assertNotNull("Должны получить категорию", model.getCategory());
        Assert.assertEquals("Должны полчить модель из категории 'мобильные телефоны'", 91491, model.getCategoryId());
        Assert.assertEquals("Данные должны быть консистентны", model.getCategoryId(), model.getCategory().getId());
        // Очистка
    }

    /**
     * Тест получения модели по ее id (id=13485518)
     * <p>
     * Запрашиваем модель fields=PHOTO
     * <p>
     * Проверяем:
     * <ol>
     * <li>Получили модель по id</li>
     * <li>Модель содержит фотографию, и она совпадает с фотографией из ответа репорта</li>
     * </ol>
     * <p>
     */
    @Test
    public void getModelByIdWithPhoto_V2_0_1() {
        // настройка системы
        context.setVersion(Version.V2_0_1);

        reportTestClient.getModelInfoById(MODEL_1_ID, "modelinfo_13485518.json");
        // вызов системы
        ModelV2 model = Futures.waitAndGet(modelService.getModel(MODEL_1_ID, Lists.newArrayList(ModelInfoField.PHOTO), genericParams));
        // Проверка утверждений
        Assert.assertEquals("id модели должен соответствовать запрошенному", MODEL_1_ID, model.getId());
        Assert.assertNotNull("Должны получить фотографию", model.getPhoto());
        Assert.assertEquals("URL фотографии должен совпадать с ответом репорта", "https://mdata.yandex.net/i?path=b0222094034_img_id9187668351168946175.jpeg", model.getPhoto().getUrl());
        // Очистка
    }

    /**
     * Тест получения модели по ее id (id=13485518)
     * <p>
     * Запрашиваем модель с fields=PHOTO
     * <p>
     * Проверяем:
     * <ol>
     * <li>Получили модель по id</li>
     * <li>Модель содержит рейтинг и рейтинг совпадает с информацией из букера</li>
     * </ol>
     * <p>
     */
    @Test
    public void getModelByIdWithRating_V2_0_1() {
        // настройка системы
        context.setVersion(Version.V2_0_1);

        reportTestClient.getModelInfoById(MODEL_1_ID, "modelinfo_13485518.json");
        // вызов системы
        ModelV2 model = Futures.waitAndGet(modelService.getModel(MODEL_1_ID, Lists.newArrayList(ModelInfoField.RATING), genericParams));
        // Проверка утверждений
        Assert.assertEquals("id модели должен соответствовать запрошенному", MODEL_1_ID, model.getId());
        Assert.assertNotNull("Должны получить рейтинг модели", model.getRating());
        RatingV2 rating = (RatingV2) model.getRating();
        Assert.assertEquals("Рейтинг должен браться из репорта (в ответе репорта значение 0, значит используем значение -1)", BigDecimal.ONE.negate(), rating.getRating());
        Assert.assertEquals("Кол-во оценок должно браться из репорта", 0, rating.getCount());
        // Очистка
    }

    /**
     * Тест получения модели по ее id (id=13485518)
     * <p>
     * Запрашиваем модель с fields=DEFAULT_OFFER
     * <p>
     * Проверяем:
     * <ol>
     * <li>Получили модель по id</li>
     * <li>Модель содержит оффер и id оффера соответствует ожидаемому</li>
     * </ol>
     * <p>
     */
    @Test
    public void getModelByIdWithDefaultOffer_V2_0_1() {
        // настройка системы
        context.setVersion(Version.V2_0_1);

        reportTestClient.getModelInfoById(MODEL_1_ID, "modelinfo_13485518.json");
        reportTestClient.getDefaultOffer(MODEL_1_ID, "defaultoffer_13485518.json");
        // вызов системы
        ModelV2 model = Futures.waitAndGet(modelService.getModel(MODEL_1_ID, Lists.newArrayList(ModelInfoField.DEFAULT_OFFER), genericParams));
        // Проверка утверждений
        Assert.assertEquals("id модели должен соответствовать запрошенному", MODEL_1_ID, model.getId());
        Assert.assertNotNull("Должны получить оффер", model.getOffer());
        OfferV2 offer = (OfferV2) model.getOffer();
        Assert.assertEquals("wareMD5 должно совпадать с ответом репорта", "sJZqnw4Ii6ep6-HexkevjA", offer.getWareMd5());
        // Очистка
    }

    /**
     * Тест получения модели по ее id (id=13485518)
     * <p>
     * Запрашиваем модель с fields=FILTERS
     * <p>
     * Проверяем:
     * <ol>
     * <li>Получили модель по id</li>
     * <li>Модель содержит оффер и id оффера соответствует ожидаемому</li>
     * </ol>
     * <p>
     */
    @Test
    public void getModelByIdWithFilters_V2_0_1() {
        // настройка системы
        context.setVersion(Version.V2_0_1);

        reportTestClient.getModelInfoById(MODEL_1_ID, "modelinfo_13485518.json");
        reportTestClient.getModelOffers(MODEL_1_ID, "offersreport_13485518.json");
        // вызов системы
        ModelV2 model = Futures.waitAndGet(modelService.getModel(MODEL_1_ID, Lists.newArrayList(ModelInfoField.FILTERS), genericParams));
        // Проверка утверждений
        Assert.assertEquals("id модели должен соответствовать запрошенному", MODEL_1_ID, model.getId());
        Assert.assertNotNull("Должны получить фильтры", model.getFilters());
        Assert.assertEquals("В ответе репорта содержатся два фильтра", 2, model.getFilters().size());
        Filter filter0 = model.getFilters().get(0);
        Filter filter1 = model.getFilters().get(1);
        Assert.assertEquals("Ожидаем увидеть фильтр с id", "12782797", filter0.getId());
        Assert.assertEquals("Ожидаем увидеть фильтр с id", "13354415", filter1.getId());
        // Очистка
    }

    /**
     * Проверяем работоспособность ручки поиска похожих для моделей в V2 (model/{id}/looksas) для гуру-модели
     * <p>
     * Проверки:
     * <ol>
     * <li>Все модели результата имеею версию ModelV2</li>
     * <li>В результате содержаться модели с ожидаемыми id (ответ гуры LookalikeTable) и в нужном порядке</li>
     * </ol>
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-2574">MARKETAPI-2574: Internal error в v1/model/id/looksas</a>
     */
    @Test
    public void getLooksAsModels_GURU_V2_0_1() {
        // настройка системы
        context.setVersion(Version.V2_0_1);

        reportTestClient.getModelInfoById(MODEL_1_ID, "modelinfo_13485518.json");

        reportTestClient.getModelInfoById(
            Arrays.stream(MODEL_1_LOOKSAS_IDS).mapToObj(Long::valueOf).collect(Collectors.toList()),
            "modelinfo_for_analogs_13485518.json"
        );
        reportTestClient.productAnalogs(MODEL_1_ID, "product_analogs_13485518.json");
        // вызов системы
        ArrayList<ModelInfoField> fields = Lists.newArrayList();
        List<Model> models = Futures.waitAndGet(modelService.getLooksAsModels(MODEL_1_ID, 10, fields, Collections.emptyMap(), genericParams));
        // Проверка утверждений
        Assert.assertTrue("В V2 в результате должны быть модели второй версии", Iterables.all(models, Predicates.instanceOf(ModelV2.class)));
        LongList ids = CommonPrimitiveCollections.transform(models, Model::getId);
        Assert.assertArrayEquals("Список моделей и их порядок должны сопадать с ответом репорта", MODEL_1_LOOKSAS_IDS, ids.toLongArray());
        // Очистка
    }

    /**
     * Проверяем правильность получения характеристик гуру-модели
     */
    @Test
    public void getGuruModelByIdWithSpecification() {
        // настройка системы
        context.setVersion(Version.V2_0_5);

        reportTestClient.getModelInfoById(12559706, "modelinfo_12559706.json");

        // вызов системы
        Model model = Futures.waitAndGet(modelService.getModel(12559706, Collections.singleton(ModelInfoField.SPECIFICATION), genericParams));

        // проверка утверждений
        Assert.assertNotNull("Должны получить модель по id", model);
        Assert.assertTrue("Модель должна быть второй версии", model instanceof ModelV2);

        ModelV2 v2 = (ModelV2) model;
        Assert.assertEquals(12559706, v2.getId());
        Assert.assertEquals("Модель должна быть гуризованной", ModelType.MODEL, v2.getType());

        List<SpecificationGroup> groups = v2.getSpecificationGroups();
        Assert.assertNotNull("Должны получить характеристики модели т.к. запрашивали через fields", groups);
        Assert.assertEquals("Должна быть одна группа т.к. это соновные хараткеристики модели", 1, groups.size());

        SpecificationGroup group = groups.get(0);
        Assert.assertEquals("Общие характеристики", group.getName());
        Assert.assertEquals(5, group.getFeatures().size());

        Assert.assertEquals("ЖК-телевизор, 720p HD", group.getFeatures().get(0).getValue());
        Assert.assertEquals("диагональ 32\" (81 см)", group.getFeatures().get(1).getValue());
        Assert.assertEquals("HDMI x2, USB", group.getFeatures().get(2).getValue());
        Assert.assertEquals("тип подсветки: Edge LED", group.getFeatures().get(3).getValue());
        Assert.assertEquals("картинка в картинке", group.getFeatures().get(4).getValue());
    }

    /**
     * Выкидываем NotFoundException несмотря на то, что в ответе репорта модель есть
     * Это происходит т.к. кластер был пересобран и возвращается модель с другим id
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3483">MARKETAPI-3483: Не находим модель по id если это пересобранный кластер</a>
     */
    @Test
    public void getModelForReassembledCluster() {
        // настройка системы
        context.setVersion(Version.V2_0_9);

        reportTestClient.getModelInfoById(1713490428, "modelinfo_1713490428.json");

        // вызов системы
        Model model = Futures.waitAndGet(modelService.getModel(1713490428, Collections.emptyList(), genericParams));

        // проверка утверждений
        Assert.assertNotNull("Должны получить модель по id", model);
        Assert.assertEquals("Должны получить модель с id который запрашивали", 1713490428L, model.getId());

    }

    /**
     * Выкидываем NotFoundException несмотря на то, что в ответе репорта модель есть
     * Это происходит т.к. кластер был пересобран и возвращается модель с другим id
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3483">MARKETAPI-3483: Не находим модель по id если это пересобранный кластер</a>
     */
    @Test
    public void getModelForReassembledClusteNewLogic() {
        // настройка системы
        context.setVersion(Version.V2_0_9);

        mockClientHelper.is(ClientHelper.Type.BLUE_APP, true);

        Client mobileBlue = new Client();
        mobileBlue.setType(Client.Type.MOBILE);

        context.setClient(mobileBlue);
        context.setClientVersionInfo(
                new KnownMobileClientVersionInfo(
                        Platform.IOS,
                        DeviceType.TABLET,
                        new SemanticVersion(2, 0, 0)
                )
        );

        reportTestClient.getModelInfoById(1713490428, "modelinfo_1713490428.json");



        // вызов системы
        ModelV2 model = (ModelV2) Futures.waitAndGet(modelService.getModel(1713490428, Collections.emptyList(), genericParams));

        // проверка утверждений
        Assert.assertNotNull("Должны получить модель по id", model);

        Assert.assertEquals( 1713490428L, (long) model.getDeletedId());
        Assert.assertEquals( 1721354031L, model.getId());

    }

    /**
     * Проверяем правильность получения характеристик групповой-модели
     */
    @Test
    public void getGroupModelByIdWithSpecification() {
        int id = 12299034;

        // настройка системы
        context.setVersion(Version.V2_0_5);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");

        // вызов системы
        Model model = Futures.waitAndGet(modelService.getModel(id,
            Collections.singleton(ModelInfoField.SPECIFICATION), genericParams));

        // проверка утверждений
        Assert.assertNotNull("Должны получить модель по id", model);
        Assert.assertTrue("Модель должна быть второй версии", model instanceof ModelV2);

        ModelV2 v2 = (ModelV2) model;
        Assert.assertEquals(id, v2.getId());
        Assert.assertEquals("Модель должна быть групповой", ModelType.GROUP, v2.getType());

        List<SpecificationGroup> groups = v2.getSpecificationGroups();
        Assert.assertNotNull("Должны получить характеристики модели т.к. запрашивали через fields", groups);
        Assert.assertEquals("Должна быть одна группа т.к. это соновные хараткеристики модели", 1, groups.size());

        SpecificationGroup group = groups.get(0);
        Assert.assertEquals("Общие характеристики", group.getName());
        Assert.assertEquals(11, group.getFeatures().size());

        Assert.assertEquals("Процессор: Core i5 / Core i7", group.getFeatures().get(0).getValue());
        Assert.assertEquals("Частота процессора: 2600...3100 МГц", group.getFeatures().get(1).getValue());
        Assert.assertEquals("Объем оперативной памяти: 8...16 Гб", group.getFeatures().get(2).getValue());
        Assert.assertEquals("Объем жесткого диска: 128...1000 Гб", group.getFeatures().get(3).getValue());
        Assert.assertEquals("Размер экрана: 13.3 \"", group.getFeatures().get(4).getValue());
        Assert.assertEquals("Видеокарта: Intel Iris Graphics 6100", group.getFeatures().get(5).getValue());
        Assert.assertEquals("Вес: 1.58 кг", group.getFeatures().get(6).getValue());
        Assert.assertEquals("Оптический привод: DVD нет", group.getFeatures().get(7).getValue());
        Assert.assertEquals("4G LTE: нет", group.getFeatures().get(8).getValue());
        Assert.assertEquals("Bluetooth: есть", group.getFeatures().get(9).getValue());
        Assert.assertEquals("Wi-Fi: есть", group.getFeatures().get(10).getValue());
    }

    /**
     * Проверяем правильность получения характеристик книги
     */
    @Test
    public void getBookModelByIdWithSpecification() {
        int id = 12838810;

        // настройка системы
        context.setVersion(Version.V2_0_5);

        reportTestClient.getModelInfoById(id, "modelinfo_12838810.json");

        // вызов системы
        Model model = Futures.waitAndGet(modelService.getModel(id, Collections.singleton(ModelInfoField.SPECIFICATION), genericParams));

        // проверка утверждений
        Assert.assertNotNull("Должны получить модель по id", model);
        Assert.assertTrue("Модель должна быть второй версии", model instanceof ModelV2);

        ModelV2 v2 = (ModelV2) model;
        Assert.assertEquals(id, v2.getId());
        Assert.assertEquals("Модель должна быть групповой", ModelType.BOOK, v2.getType());

        List<SpecificationGroup> groups = v2.getSpecificationGroups();
        Assert.assertNotNull("Должны получить характеристики модели т.к. запрашивали через fields", groups);
        Assert.assertEquals("Должна быть одна группа т.к. это соновные хараткеристики модели", 1, groups.size());

        SpecificationGroup group = groups.get(0);
        Assert.assertEquals("Общие характеристики", group.getName());
        Assert.assertEquals(3, group.getFeatures().size());

        Assert.assertEquals("ISBN: 5170913982 9785170913985", group.getFeatures().get(0).getValue());
        Assert.assertEquals("Автор: Леонардо да Винчи", group.getFeatures().get(1).getValue());
        Assert.assertEquals("Издательство: АСТ", group.getFeatures().get(2).getValue());
    }

    /**
     * Проверяем правильность получения характеристик книги
     */
    @Test
    public void getModificationByIdWithSpecification() {
        int id = 9253479;

        // настройка системы
        context.setVersion(Version.V2_0_5);

        reportTestClient.getModelInfoById(id, "modelinfo_9253479.json");

        // вызов системы
        Model model = Futures.waitAndGet(modelService.getModel(id, Collections.singleton(ModelInfoField.SPECIFICATION), genericParams));

        // проверка утверждений
        Assert.assertNotNull("Должны получить модель по id", model);
        Assert.assertTrue("Модель должна быть второй версии", model instanceof ModelV2);

        ModelV2 v2 = (ModelV2) model;
        Assert.assertEquals(id, v2.getId());
        Assert.assertEquals("Модель должна быть модификацией", ModelType.MODIFICATION, v2.getType());

        List<SpecificationGroup> groups = v2.getSpecificationGroups();
        Assert.assertNotNull("Должны получить характеристики модели т.к. запрашивали через fields", groups);
        Assert.assertEquals("Должна быть одна группа т.к. это соновные хараткеристики модели", 1, groups.size());

        SpecificationGroup group = groups.get(0);
        Assert.assertEquals("Общие характеристики", group.getName());
        Assert.assertEquals(6, group.getFeatures().size());

        Assert.assertEquals("зимние шины, с шипами", group.getFeatures().get(0).getValue());
        Assert.assertEquals("для внедорожника", group.getFeatures().get(1).getValue());
        Assert.assertEquals("размер 205/55 R16", group.getFeatures().get(2).getValue());
        Assert.assertEquals("индекс скорости T (до 190 км/ч)", group.getFeatures().get(3).getValue());
        Assert.assertEquals("индекс нагрузки 94 (670 кг)", group.getFeatures().get(4).getValue());
        Assert.assertEquals("шины для северной зимы", group.getFeatures().get(5).getValue());
    }

    /**
     * Проверяем правильность получения характеристик класстера
     */
    @Test
    public void getClusterModelByIdWithSpecification() {
        int id = 1365570183;

        // настройка системы
        context.setVersion(Version.V2_0_5);

        reportTestClient.getModelInfoById(id, "modelinfo_1365570183.json");

        // вызов системы
        Model model = Futures.waitAndGet(modelService.getModel(id, Collections.singleton(ModelInfoField.SPECIFICATION), genericParams));

        // проверка утверждений
        Assert.assertNotNull("Должны получить модель по id", model);
        Assert.assertTrue("Модель должна быть второй версии", model instanceof ModelV2);

        ModelV2 v2 = (ModelV2) model;
        Assert.assertEquals(id, v2.getId());
        Assert.assertEquals("Модель должна быть кластером", ModelType.CLUSTER, v2.getType());

        List<SpecificationGroup> groups = v2.getSpecificationGroups();
        Assert.assertNotNull("Должны получить характеристики модели т.к. запрашивали через fields", groups);
        Assert.assertEquals("Должна быть одна группа т.к. это соновные хараткеристики модели", 1, groups.size());

        SpecificationGroup group = groups.get(0);
        Assert.assertEquals("Общие характеристики", group.getName());
        Assert.assertEquals(4, group.getFeatures().size());

        Assert.assertEquals("темно-синий", group.getFeatures().get(0).getValue());
        Assert.assertEquals("Marc & Andre", group.getFeatures().get(1).getValue());
        Assert.assertEquals("синтетика", group.getFeatures().get(2).getValue());
        Assert.assertEquals("закрытый", group.getFeatures().get(3).getValue());
    }

    /**
     * Проверяем получение моделей, рекомендованных к следующей покупке
     */
    @Test
    public void testRecommendedModels() {
        User user = new User(null, null, new Uuid("1234qwer"), null);
        ContextHolder.update(ctx -> ctx.setUser(user));

        reportTestClient.recommendedModels("-123321", user.getUuid().getValue(), "models.json");

        PagedResult<ModelV2> page = Futures.waitAndGet(modelService.getRecommendedModels(IntLists.singleton(123321),
                user, Collections.emptyList(), PageInfo.DEFAULT, GenericParams.DEFAULT));

        Assert.assertNotNull(page);

        PageInfo pageInfo = page.getPageInfo();
        Assert.assertNull(pageInfo.getTotal());
        Assert.assertNull(pageInfo.getTotalElements());
        Assert.assertEquals(1, pageInfo.getNumber());
        Assert.assertEquals(10, pageInfo.getCount());

        List<ModelV2> models = page.getElements();
        Assert.assertEquals(7, models.size());
        assertModel(models.get(0), 13953515, "Xiaomi Redmi 3S 16Gb");
        assertModel(models.get(1), 9330604,  "Panasonic TX-L(R)42ET60");
        assertModel(models.get(2), 10505774, "Miele KM 6395");
        assertModel(models.get(3), 13584123, "Apple iPhone SE 64Gb");
        assertModel(models.get(4), 8348791,  "Sharp LC-22LE240");
        assertModel(models.get(5), 13584121, "Apple iPhone SE 16Gb");
        assertModel(models.get(6), 8366943,  "Sharp LC-24LE240");
    }

    /**
     * Проверка получения рекоммендованных моделей (на основании истории просмотров пользователя)
     */
    @Test
    public void testGetRecommendedByHistory() {
        User user = new User(null, null, new Uuid("1234qwer"), null);
        ContextHolder.update(ctx -> ctx.setUser(user));

        reportTestClient.recommendedByHistory(user.getUuid().getValue(), PageInfo.DEFAULT, "models.json");

        List<ModelV2> models = Futures.waitAndGet(modelService.getRecommendedByHistory(PageInfo.DEFAULT,
                Collections.emptyList(), GenericParams.DEFAULT)).getModels().getElements();

        Assert.assertEquals(7, models.size());
        assertModel(models.get(0), 13953515, "Xiaomi Redmi 3S 16Gb");
        assertModel(models.get(1), 9330604,  "Panasonic TX-L(R)42ET60");
        assertModel(models.get(2), 10505774, "Miele KM 6395");
        assertModel(models.get(3), 13584123, "Apple iPhone SE 64Gb");
        assertModel(models.get(4), 8348791,  "Sharp LC-22LE240");
        assertModel(models.get(5), 13584121, "Apple iPhone SE 16Gb");
        assertModel(models.get(6), 8366943,  "Sharp LC-24LE240");
    }


    @Test
    public void testCategory() {
        ContextHolder.update(ctx -> ctx.setVersion(Version.V2_1_5));
        ModelV2 model = new ModelV2();
        model.setId(1968987605L);
        model.setCategoryId(91157);
        ModelV2 modelV2 = new ModelV2();
        modelV2.setId(196891235L);
        modelV2.setCategoryId(0);
        Futures.waitAndGet(modelService.enrichModels(Arrays.asList(modelV2, model), false,  Collections.singleton(ModelInfoField.CATEGORY), GenericParams.DEFAULT));

        CategoryV2 category = (CategoryV2) model.getCategory();
        CategoryV2 categoryV2 = (CategoryV2) modelV2.getCategory();
        Assert.assertEquals("Косметика, парфюмерия и уход", category.getName());
        Assert.assertNull(categoryV2);
    }

    private void assertModel(ModelV2 model, long expectedId, String expectedName) {
        Assert.assertNotNull(model);
        Assert.assertEquals(expectedId, model.getId());
        Assert.assertEquals(expectedName, model.getName());
    }
}
