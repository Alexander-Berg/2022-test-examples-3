package ru.yandex.market.api.controller.v2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.VerticalNamespace;
import ru.yandex.market.api.common.client.InternalClientVersionInfo;
import ru.yandex.market.api.common.client.KnownMobileClientVersionInfo;
import ru.yandex.market.api.common.client.SemanticVersion;
import ru.yandex.market.api.domain.Model;
import ru.yandex.market.api.domain.Offer;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v1.ModelInfoField;
import ru.yandex.market.api.domain.v2.AddressV2;
import ru.yandex.market.api.domain.v2.FilterField;
import ru.yandex.market.api.domain.v2.GeoCoordinatesV2;
import ru.yandex.market.api.domain.v2.ImageWithThumbnails;
import ru.yandex.market.api.domain.v2.ModelListResult;
import ru.yandex.market.api.domain.v2.ModelResult;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferFieldV2;
import ru.yandex.market.api.domain.v2.OfferPromo;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.OutletV2;
import ru.yandex.market.api.domain.v2.RatingV2;
import ru.yandex.market.api.domain.v2.RecommendedListWithEncryptedIdsResult;
import ru.yandex.market.api.domain.v2.ResultContextV2;
import ru.yandex.market.api.domain.v2.ResultFieldV2;
import ru.yandex.market.api.domain.v2.ShopInfoFieldV2;
import ru.yandex.market.api.domain.v2.ShopInfoV2;
import ru.yandex.market.api.domain.v2.SpecificationGroup;
import ru.yandex.market.api.domain.v2.filters.EnumFilter;
import ru.yandex.market.api.domain.v2.filters.Filter;
import ru.yandex.market.api.domain.v2.filters.PhotoPickerEnumValue;
import ru.yandex.market.api.domain.v2.offers.GetOffersByModelResult;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.internal.common.DeviceType;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.PartnerInfo;
import ru.yandex.market.api.internal.common.Platform;
import ru.yandex.market.api.internal.computervision.CbirdResolver;
import ru.yandex.market.api.internal.djviewer.DjViewerTestClient;
import ru.yandex.market.api.internal.filters.Filters;
import ru.yandex.market.api.internal.filters.Filters.FilterType;
import ru.yandex.market.api.internal.report.ReportSort;
import ru.yandex.market.api.internal.report.ReportSortType;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.matchers.CriterionMatcher;
import ru.yandex.market.api.matchers.GiftOfferMatcher;
import ru.yandex.market.api.matchers.PhotoPickerEnumValueMatchers;
import ru.yandex.market.api.offer.GetOffersByModelRequest;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.Uuid;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.service.match.ModelMatchRequest;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.CommonPrimitiveCollections;
import ru.yandex.market.api.util.concurrent.ApiDeferredResult;
import ru.yandex.market.api.util.httpclient.clients.ComputerVisionTestClient;
import ru.yandex.market.api.util.httpclient.clients.LoyaltyTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;
import ru.yandex.market.api.util.httpclient.clients.UltraControllerTestClient;
import ru.yandex.market.ir.http.UltraController;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.internal.filters.Filters.FilterType.PHOTO_PICKER;
import static ru.yandex.market.api.matchers.ModelMatcher.id;
import static ru.yandex.market.api.matchers.ModelMatcher.model;
import static ru.yandex.market.api.matchers.ModelMatcher.offer;
import static ru.yandex.market.api.matchers.OfferMatcher.url;
import static ru.yandex.market.api.matchers.PhotoPickerEnumValueMatchers.photoPickerValue;

/**
 * Created by tesseract on 02.05.17.
 */
@WithContext
@ActiveProfiles(ModelsControllerV2Test.PROFILE)
public class ModelsControllerV2Test extends BaseTest {
    static final String PROFILE = "ModelsControllerV2Test";
    private static final String DEFAULT_DJ_VIEWER_EXPERIMENT = "default";

    @Profile(ModelsControllerV2Test.PROFILE)
    @org.springframework.context.annotation.Configuration
    public static class Configuration {
        @Primary
        @Bean
        public ClientHelper localHelper() {
            return Mockito.mock(ClientHelper.class);
        }
    }

    @Inject
    ModelsControllerV2 controller;

    @Inject
    ReportTestClient reportTestClient;

    @Inject
    LoyaltyTestClient loyaltyTestClient;

    @Inject
    ComputerVisionTestClient computerVisionTestClient;

    @Inject
    UltraControllerTestClient ultraControllerTestClient;

    @Inject
    DjViewerTestClient djViewerTestClient;

    @Inject
    ClientHelper clientHelper;

    private MockClientHelper mockClientHelper;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.mockClientHelper = new MockClientHelper(clientHelper);
    }

    private static void assertModelId(Long expected, GetOffersByModelResult result, int position) {
        OfferV2 offer = result.getOffers().get(position);
        Assert.assertEquals("Id модели оффера должен совпадать с ожидаемым", expected, offer.getModelId());
    }

    private static void assertWareMd5(String expected, GetOffersByModelResult result, int position) {
        OfferV2 offer = result.getOffers().get(position);
        Assert.assertEquals("wareMd5 оффера должен совпадать с ожидаемым", expected, offer.getWareMd5());
    }

    private static void assertPromo(String type, String key, Boolean offline, String price, GetOffersByModelResult result, int position) {
        OfferV2 offer = result.getOffers().get(position);

        if(type == null) {
            assertNull(offer.getPromo());
            return;
        }

        Assert.assertEquals("promo.type оффера должен совпадать с ожидаемым", type, offer.getPromo().getType());
        Assert.assertEquals("promo.key оффера должен совпадать с ожидаемым", key, offer.getPromo().getKey());
        Assert.assertEquals("promo.offline оффера должен совпадать с ожидаемым", offline, offer.getPromo().getOffline());
        Assert.assertEquals("promo.offlinePrice оффера должен совпадать с ожидаемым", price, offer.getPromo().getOfflinePrice());
    }

    /**
     * Проверка получения списка офферов модификации
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3731">MARKETAPI-3731: Проверка отдачи оферов модификаций v2</a>
     */
    @Test
    public void modificationOffers() {
        long id = 12299034L;
        // настройка системы
        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        reportTestClient.getModelOffers(id, "productoffers_12299034.json");
        // вызов системы
        GetOffersByModelResult result = controller.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(id)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Collections.emptyList())
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(new ReportSort(ReportSortType.RELEVANCY, SortOrder.DESC))
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(null)
                        .setFilterParameters(Collections.emptyMap())
                        .setWithModel(true)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        ).waitResult();
        // проверка утверждений
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOffers());
        assertModelId(12299043L, result, 0);
        assertWareMd5("o4fj1Ye5YSzd_aPfFMD3-A", result, 0);

        assertModelId(12299043L, result, 1);
        assertWareMd5("HikrivvKL6bSV8RRl5nhvg", result, 1);

        assertModelId(12299043L, result, 2);
        assertWareMd5("Vi1ah84S42a_fWn51xb1Cg", result, 2);

        assertModelId(12299043L, result, 3);
        assertWareMd5("h9mq3zZsW80DUlg7pM-6UQ", result, 3);
        assertPromo(null, null, null, null, result, 3);

        assertModelId(12299046L, result, 4);
        assertWareMd5("n1n7OWPdwUdKuGQfPln9-A", result, 4);
        assertPromo("test-offline-promo", "hE3uQiqFUjBKfXRufgapRw", true, "123.56", result, 4);

        assertModelId(12299043L, result, 5);
        assertWareMd5("Ke-gvWts-Xaq4VwQtsqSzw", result, 5);

        assertModelId(13114665L, result, 6);
        assertWareMd5("f6PbrAtPonfQqUQ_Q9KTjg", result, 6);

        assertModelId(13079140L, result, 7);
        assertWareMd5("-N7FwBsunXZtO8roxbrp6g", result, 7);

        assertModelId(12299046L, result, 8);
        assertWareMd5("DmpUPWwSulWLo_fDyV7_sQ", result, 8);

        assertModelId(13079140L, result, 9);
        assertWareMd5("mpoWFCmfig6d_2hYt0CB3g", result, 9);
    }

    /**
     * Проверка сортировки аутлетов по удаленности
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3565">MARKETAPI-3565: Сортировка аутлетов по удаленности v2</a>
     */
    @Test
    public void sortOffersByDistance() {
        long id = 11031665L;
        // настройка системы
        reportTestClient.getModelInfoById(id, "modelinfo_11031665.json");
        reportTestClient.getGeoModelOffers(id, "geoOffers_11031665.json");
        // вызов системы
        GetOffersByModelResult result = controller.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(id)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Collections.singletonList(OfferFieldV2.OUTLET))
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(new ReportSort(ReportSortType.DISTANCE, null))
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(null)
                        .setFilterParameters(Collections.emptyMap())
                        .setGpsCoords(new GeoCoordinatesV2(59.860437, 30.338092))
                        .setWithModel(true)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        ).waitResult();
        // проверка утверждений
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOffers());
        List<Double> actualDistances = result.getOffers().stream()
            .map(offer -> (OutletV2) (offer.getOutlet()))
            .map(outlet -> (AddressV2) (outlet.getAddress()))
            .map(address -> address.getGeoPoint().getDistance())
            .collect(Collectors.toList());
        List<Double> expectedDistances = new ArrayList<>(actualDistances);
        Collections.sort(expectedDistances);
        Assert.assertEquals(expectedDistances, actualDistances);
    }

    @Test
    public void radioFilters() {
        long id = 12299034L;
        // настройка системы
        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        reportTestClient.getModelOffers(id, "productoffers_12299034.json");
        // вызов системы
        GetOffersByModelResult result = controller.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(id)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Arrays.asList(ResultFieldV2.FILTERS, OfferFieldV2.ACTIVE_FILTERS))
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(null)
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(null)
                        .setFilterParameters(Collections.emptyMap())
                        .setWithModel(true)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        ).waitResult();
        // проверка утверждений
        Assert.assertNotNull(result);
        checkRadioFilters(result.getFilters());

        Assert.assertNotNull(result.getOffers());
        Assert.assertFalse(result.getOffers().isEmpty());
        result.getOffers().forEach(offer ->
            checkRadioFilters(offer.getActiveFilters())
        );
    }

    /**
     * Проверка, что обрезаются пустые value в блоке specification, ручка v2/models/recommended
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3804">MARKETAPI-3804: Пустые value в блоке specification, ручка v2/models/recommended</a>
     */
    @Test
    public void emptySpecificationsInRecommendedModels() {
        long id = 6100705L;
        IntCollection excludeHids = CommonPrimitiveCollections.asList(91491, 91033);
        User user = new User(null, null, new Uuid("1234qwer"), null);
        ContextHolder.update(ctx -> ctx.setUser(user));
        // настройка системы
        reportTestClient.recommendedModels(
            excludeHids.stream().map(hid -> "-" + hid).collect(Collectors.joining(",")),
            user.getUuid().getValue(),
            "nbo_category_products_6100705.json");
        reportTestClient.getModelInfoById(id, "modelinfo_6100705.json");
        // вызов системы
        ModelListResult result = ((ApiDeferredResult<ModelListResult>) controller.getRecommendedModels(
            excludeHids,
            Collections.singletonList(ModelInfoField.SPECIFICATION),
            PageInfo.DEFAULT,
            user,
            genericParams
        )).waitResult();
        // проверка утверждений
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getModels());
        Assert.assertFalse(result.getModels().isEmpty());

        ModelV2 model = (ModelV2) result.getModels().get(0);
        Assert.assertNotNull(model.getSpecificationGroups());
        Assert.assertFalse(model.getSpecificationGroups().isEmpty());

        List<SpecificationGroup.Feature> features = model.getSpecificationGroups().get(0).getFeatures();
        Assert.assertEquals(
            Arrays.asList("кварцевые наручные часы", "мужские", "отображение времени стрелками и цифрами",
                "корпус из стали и пластика", "браслет: каучук", "противоударные",
                "водонепроницаемость: WR200 (погружение под воду с аквалангом)", "будильник"),
            features.stream().map(SpecificationGroup.Feature::getValue).collect(Collectors.toList())
        );
    }

    @Test
    public void lookasModelByUrl() {
        String url = "img.png";
        long modelId = 1L;

        mockClientHelper.is(ClientHelper.Type.BIXBY, true);

        computerVisionTestClient.looksas(url, CbirdResolver.SAMSUNG_BIXBY_VISION_CBIRD, "success-computer-vision-response-model.json");

        reportTestClient.getModelInfoById(modelId, "modelinfo_1.json");
        ModelListResult result = controller.getLooksByImageUrlAsModels(Collections.emptyList(), url, genericParams).waitResult();
        List<? extends Model> models = result.getModels();

         Assert.assertEquals(1, models.size());

        ModelV2 model = (ModelV2) models.get(0);
        Assert.assertEquals(modelId, model.getId());

        Assert.assertTrue(result.getContext() instanceof ResultContextV2);
        ResultContextV2 contextV2 = (ResultContextV2) result.getContext();
        Assert.assertEquals("https://m.market.yandex.ru/picsearch?cbir_id=1&pp=37", contextV2.getLink());
    }

    @Test
    public void fullSpecification() {
        long modelId = 13485515L;

        reportTestClient.getModelInfoById(modelId, "modelinfo_13485515.json");

        ModelResult result = controller.getModel(
            modelId,
            Lists.newArrayList(ModelInfoField.SPECIFICATION),
            Collections.emptyMap(),
            genericParams,
                true,
                null
        ).waitResult();

        ModelV2 model = (ModelV2) result.getModel();

        assertThat(model.getSpecificationGroups(),  notNullValue());
        assertThat(model.getSpecificationGroups(),  not(empty()));
    }

    @Test
    public void filtersInImage() {
        long modelId = 1732171388L;

        reportTestClient.getModelInfoById(modelId, "modelinfo_1732171388.json");

        ModelResult result = controller.getModel(
                modelId,
                Lists.newArrayList(ModelInfoField.PHOTO, ModelInfoField.PHOTOS),
                Collections.emptyMap(),
                genericParams,
                false,
                null
        ).waitResult();

        ModelV2 model = (ModelV2) result.getModel();


        assertThat(
                ((ImageWithThumbnails) model.getPhoto()).getCriteria(),
                containsInAnyOrder(
                        CriterionMatcher.criterion("14871214", "15266392")
                )
        );

        assertThat(
                ((ImageWithThumbnails) model.getPhotos().get(0)).getCriteria(),
                containsInAnyOrder(
                        CriterionMatcher.criterion("14871214", "15266392")
                )
        );

    }

    @Test
    public void filtersInDefaultOffer() {
        long modelId = 12299034L;

        // настройка системы
        reportTestClient.getModelInfoById(modelId, "modelinfo_12299034.json");
        reportTestClient.getDefaultOffer_VendorRecommended(modelId, "defaultoffer_12299034.json");

        // вызов системы
        ModelResult result = controller.getModel(
            modelId,
            Lists.newArrayList(ModelInfoField.DEFAULT_OFFER),
            Collections.singletonMap(Filters.VENDOR_RECOMMENDED_FILTER_CODE, "1"),
            genericParams,
                false,
                null
        ).waitResult();

        ModelV2 model = (ModelV2) result.getModel();

        // проверка утверждений
        Assert.assertNotNull("Должны получить оффер рекоммендованный продавцом (фильтр -17)", model.getOffer());
    }

    @Test
    public void photoPickerTest() {
        long modelId = 1732171388L;

        reportTestClient.getModelInfoById(modelId, "modelinfo_1732171388.json");
        reportTestClient.getModelOffers(modelId, "productoffers_1732171388.json");

        GetOffersByModelResult result = controller.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(modelId)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Lists.newArrayList(FilterField.PHOTO_PICKER, ResultFieldV2.FILTERS))
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(new ReportSort(ReportSortType.DEFAULT, SortOrder.ASC))
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(null)
                        .setFilterParameters(Collections.emptyMap())
                        .setWithModel(true)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        ).waitResult();

        List<Filter> filters = result.getFilters();

        EnumFilter enumFilter = (EnumFilter) filters.stream()
            .filter(f -> PHOTO_PICKER.name().equals(f.getType()))
            .findAny()
            .get();

        List<PhotoPickerEnumValue> values = enumFilter.getValues().stream()
            .filter(PhotoPickerEnumValue.class::isInstance)
            .map(PhotoPickerEnumValue.class::cast)
            .map(PhotoPickerEnumValueMatchers::toStrWrap)
            .collect(Collectors.toList());

        assertThat(
            values,
            containsInAnyOrder(
                photoPickerValue(
                    PhotoPickerEnumValueMatchers.id("15266392"),
                    PhotoPickerEnumValueMatchers.color(null),
                    PhotoPickerEnumValueMatchers.photo("https://avatars.mds.yandex.net/get-mpic/466729/model_option-picker-1732171388-15266392--84c6daccab203dc43a44a5db6c56566a/1")
                ),
                photoPickerValue(
                    PhotoPickerEnumValueMatchers.id("14897638"),
                    PhotoPickerEnumValueMatchers.color("#C0C0C0"),
                    PhotoPickerEnumValueMatchers.photo("https://avatars.mds.yandex.net/get-mpic/397397/model_option-picker-1732171388-14897638--32188357156685ff3f046d6533e32d7c/1")
                ),
                photoPickerValue(
                    PhotoPickerEnumValueMatchers.id("15277521"),
                    PhotoPickerEnumValueMatchers.color("#808080"),
                    PhotoPickerEnumValueMatchers.photo("https://avatars.mds.yandex.net/get-mpic/397397/model_option-picker-1732171388-15277521--5b15e2df58874c91c61b6391df2aaf40/1")
                )
            )
        );

    }

    @Test
    public void modelAccessoriesHasOfferUrl_hotfix_marketapi_4270() {
        reportTestClient.getModelInfoById(
            1732181846L,
            "modelinfo_1732181846.json"
        );

        reportTestClient.getProductAccessories(
            1732181846L,
            "product_accessories_1732181846.json"
        );

        reportTestClient.getDefaultOffer(
            10710499L,
            "defaultoffer_10710499.json"
        );

        ModelListResult result = controller.getModelAccessories(
            1732181846L,
            "100131944800",
            null,
            Arrays.asList(ModelInfoField.DEFAULT_OFFER, OfferFieldV2.LINK),
            Collections.emptyMap(),
            PageInfo.DEFAULT,
            genericParams,
            null,
                null
        ).waitResult();

        ModelV2 model = (ModelV2) result.getModels().get(0);

        assertThat(
            model,
            model(
                id(10710499L),
                offer(
                    cast(url("https://market-click2.yandex.ru/redir/params"))
                )
            )
        );

    }

    @Test
    public void modelAccessoriesBlue() {
        setBlueClient();

        reportTestClient.getModelInfoById(
            1732181846L,
            "modelinfo_1732181846_blue.json"
        );

        reportTestClient.getProductAccessoriesBlue(
            1732181846L,
            "100131944800",
            "product_accessories_1732181846_blue.json"
        );

        ModelListResult result = controller.getModelAccessories(
            1732181846L,
            "100131944800",
            null,
            Collections.emptyList(),
            Collections.emptyMap(),
            PageInfo.DEFAULT,
            genericParams,
            null,
                null
        ).waitResult();

        assertThat(
            (List<ModelV2>) result.getModels(),
            Matchers.containsInAnyOrder(
                model(id(1721714804L)),
                model(id(14262592L)),
                model(id(12866265L))
            )
        );
    }

    @Test
    public void modelAccessoriesBlueWithoutMsku() {
        setBlueClient();

        reportTestClient.getModelInfoById(
            1732181846L,
            "modelinfo_1732181846_blue.json"
        );

        reportTestClient.getProductAccessoriesBlueWithoutMsku(
            1732181846L,
            "product_accessories_1732181846_blue.json"
        );

        controller.getModelAccessories(
            1732181846L,
            null,
            null,
            Collections.emptyList(),
            Collections.emptyMap(),
            PageInfo.DEFAULT,
            genericParams,
            null,
                null
        ).waitResult();
    }

    @Test
    public void modelAnalogsBlue() {
        setBlueClient();
        ContextHolder.update(ctx -> ctx.setUser(new User(new OauthUser(1), null, null, null)));

        long modelId = 14124403L;
        reportTestClient.getModelAnalogsBlue(
            modelId,
            "modelanalogs_14124403_blue.json"
        );
        loyaltyTestClient.checkStatus(213, 1, "loyalty_with_empty_type.json");

        ModelListResult result = controller.getModelAnalogs(
            modelId,
            Collections.emptyList(),
            Collections.emptyMap(),
            PageInfo.DEFAULT,
            genericParams,
            null,
                null
        ).waitResult();

        assertThat(
            (List<ModelV2>) result.getModels(),
            Matchers.containsInAnyOrder(
                model(id(14112311L)),
                model(id(1722996968L)),
                model(id(1720328396L)),
                model(id(14225483L))
            )
        );
    }

    private void setBlueClient() {
        ContextHolder.update(ctx -> {
            Client client = new Client();

            client.setType(Client.Type.MOBILE);

            ctx.setClient(client);
            ctx.setClientVersionInfo(
                new KnownMobileClientVersionInfo(
                    Platform.IOS,
                    DeviceType.TABLET,
                    SemanticVersion.MIN
                )
            );
        });

        mockClientHelper.is(ClientHelper.Type.BLUE_APP, true);
    }

    @Test
    public void matchModelViaUCTest() {
        String name = "LACOSTE L.12.12 White";
        String categoryName = "";
        String description = "";
        String locale = "RU_ru";
        String shopName = "";

        ultraControllerTestClient.enrichOffer(ultraControllerOffer(name, 100.00001f, categoryName, description, locale, shopName),
            "ultracontroller_enrich_single_offer.pb");
        reportTestClient.getModelInfoById(
            14275945L,
            "modelinfo-142759.json"
        );

        ModelMatchRequest matchRequest = modelMatchRequest(name, 3000f, categoryName, description, locale, shopName);
        ModelResult modelResult = controller
            .getMatchedModelFromUC(matchRequest, Collections.emptyList(), genericParams)
            .waitResult();

        Assert.assertEquals(14275945L, modelResult.getModel().getId());
    }

    @Test
    public void requestReportForBlueAttractiveModels() {
        setBlueClient();

        reportTestClient.getAttractiveModelsBlue("report__blue_attractive_models.json");

        controller.getAttractiveModels(
                Collections.emptyList(),
                Collections.emptyMap(),
                PageInfo.DEFAULT,
                genericParams,
                null
        ).waitResult();
    }

    @Test
    public void requestReportForAttractiveModels() {
        Map<String, String> filterParams = Collections.singletonMap(Filters.VENDOR_RECOMMENDED_FILTER_CODE, "1");

        reportTestClient.getAttractiveModels(filterParams, "report__attractive_models.json");

        controller.getAttractiveModels(
                Collections.emptyList(),
                filterParams,
                PageInfo.DEFAULT,
                genericParams,
                null
        ).waitResult();
    }

    @Test
    public void requestReportForBlueOMMFindings() {
        setBlueClient();

        reportTestClient.getBlueOmmFindings("report__blue_omm_findings.json");

        controller.getBlueOMMFindings(
            Collections.emptyList(),
            PageInfo.DEFAULT,
            genericParams,
            null
        ).waitResult();
    }

    /**
     * Если передан параметр local_delivery, то должны передавать в репорт min-delivery-priority=priority
     */
    @Test
    public void hasLocalDeliveryParameter() {
        long modelId = 1L;
        reportTestClient.getModelInfoById(
            modelId,
            "report_modelinfo_without_local_delivery_param.json"
        );

        reportTestClient.search("productoffers",
            x -> x
                .param("hyperid", String.valueOf(modelId))
                .param("min-delivery-priority", "priority"),
            "report_model_offers_local_delivery_param.json"
        );
        controller.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(modelId)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Collections.emptyList())
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(new ReportSort())
                        .setLocalDelivery(true)
                        .setDeliveryIncluded(false)
                        .setGroupBy(null)
                        .setFilterParameters(Collections.emptyMap())
                        .setWithModel(true)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        ).waitResult();
    }

    /**
     * Если НЕ передан параметр local_delivery, то не должны передавать в репорт min-delivery-priority
     */
    @Test
    public void hasNoLocalDeliveryParameter() {
        long modelId = 1L;
        reportTestClient.getModelInfoById(
            modelId,
            "report_modelinfo_local_delivery_param.json"
        );

        reportTestClient.search("productoffers",
            x -> x
                .param("hyperid", String.valueOf(modelId))
                .withoutParam("min-delivery-priority"),
            "report_model_offers_without_local_delivery_param.json"
        );

        controller.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(modelId)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Collections.emptyList())
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(new ReportSort())
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(null)
                        .setFilterParameters(Collections.emptyMap())
                        .setWithModel(true)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        ).waitResult();
    }

    @Test
    public void showRatingForNewShops() {
        long modelId = 1L;
        reportTestClient.getModelInfoById(
            modelId,
            "report_modelinfo_local_delivery_param.json"
        );

        reportTestClient.search("productoffers",
            x -> x
                .param("hyperid", String.valueOf(modelId)),
            "report_model_offers_new_shop.json"
        );

        reportTestClient.getShopsRatings(Collections.singleton(278913L), "report_shop_info.json");


        GetOffersByModelResult offersByModelResult = controller.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(modelId)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Arrays.asList(OfferFieldV2.SHOP, ShopInfoFieldV2.RATING))
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(new ReportSort())
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(null)
                        .setFilterParameters(Collections.emptyMap())
                        .setWithModel(true)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        ).waitResult();

        ShopInfoV2 shopInfo = (ShopInfoV2) offersByModelResult.getOffers().get(0).getShop();

        RatingV2 rating = (RatingV2) shopInfo.getRating();

        assertEquals(BigDecimal.valueOf(4.0), rating.getRating());
    }

    @Test
    public void sendRawClidForVertical() {
        ContextHolder.update(ctx -> ctx.setPartnerInfo(PartnerInfo.create("test-clid", "test-vid", 0L, 0L, null)));

        reportTestClient.getRecommendedModelsVerticalTouch("vertical_touch.json", "test-clid");

        controller.getRecommendedVerticalTouch(
                null,
                null,
                null,
                Collections.emptyList(),
                VerticalNamespace.DEFAULT,
                PageInfo.DEFAULT,
                GenericParams.DEFAULT
        ).waitResult();
    }

    @Test
    public void beruVerticalApp() {
        Client client = new Client();
        client.setType(Client.Type.INTERNAL);
        ContextHolder.update(ctx -> {
            ctx.setClient(client);
            ctx.setClientVersionInfo(InternalClientVersionInfo.INSTANCE);
            ctx.setPartnerInfo(PartnerInfo.create("907", "test-vid", 0L, 0L, null));
            ctx.setRequest(
                    MockRequestBuilder.start()
                            .param("rules", "blue")
                            .build()
            );
        });

        reportTestClient.getRecommendedModelsVerticalApp("vertical_app_blue.json");

        RecommendedListWithEncryptedIdsResult response = controller.getRecommendedVerticalApp(
                null,
                null,
                null,
                Collections.singleton(ModelInfoField.LINK),
                VerticalNamespace.DEFAULT,
                PageInfo.DEFAULT,
                GenericParams.DEFAULT).waitResult();

        ModelV2 model = (ModelV2) response.getModels().get(0);
        Assert.assertEquals("100515356211", model.getSku());
        Assert.assertEquals(
                "https://beru.ru/product/100515356211?pp=37&clid=907&vid=test-vid&mclid=0&distr_type=0",
                model.getLink()
        );
    }

    @Test
    public void parseMixedRecommendedVerticalItems() {
        reportTestClient.getRecommendedModelsVerticalApp("vertical_app_mixed_items.json");

        RecommendedListWithEncryptedIdsResult response = controller.getRecommendedVerticalApp(
                null,
                null,
                null,
                Collections.singleton(ModelInfoField.LINK),
                VerticalNamespace.MARKET_CAPI,
                PageInfo.DEFAULT,
                GenericParams.DEFAULT).waitResult();

        Assert.assertTrue(response.getModels().isEmpty());
        Assert.assertEquals(16, response.getItems().size());
    }

    @Test
    public void doNotRequestModelWithFlagForInternalClient() {
        long modelId = 1L;

        context.setClient(new Client() {{
            setType(Client.Type.INTERNAL);
        }});

        reportTestClient.search("productoffers",
            x -> x
                .param("hyperid", String.valueOf(modelId)),
            "report_model_offers_new_shop.json"
        );

        controller.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(modelId)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Collections.emptyList())
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(new ReportSort())
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(null)
                        .setFilterParameters(Collections.emptyMap())
                        .setWithModel(false)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        ).waitResult();
    }

    @Test
    public void requestModelWithFlagForVendor() {
        long modelId = 1L;

        context.setClient(new Client() {{
            setVendorId(153061L);
            setType(Client.Type.VENDOR);
        }});

        reportTestClient.getModelInfoById(
            modelId,
            "report_modelinfo_local_delivery_param.json"
        );

        reportTestClient.search("productoffers",
            x -> x
                .param("hyperid", String.valueOf(modelId)),
            "report_model_offers_new_shop.json"
        );

        controller.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(modelId)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Collections.emptyList())
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(new ReportSort())
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(null)
                        .setFilterParameters(Collections.emptyMap())
                        .setWithModel(false)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        ).waitResult();
    }

    @Test
    public void enrichOffersWithGifts() {
        long modelId = 1L;

        context.setClient(new Client() {{
            setType(Client.Type.INTERNAL);
        }});

        reportTestClient.search("productoffers",
                x -> x
                        .param("hyperid", String.valueOf(modelId)),
                "report_model_offers_gifts.json"
        );

        reportTestClient.getPromos(Arrays.asList("hE3uQiqFUjBKfXRufgapRw", "9q_ufkPhwJWMeMJF3gTrKw"), "promos_gifts" +
                ".json");

        GetOffersByModelResult result = controller.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(modelId)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Collections.singleton(OfferFieldV2.GIFT))
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(new ReportSort())
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(null)
                        .setFilterParameters(Collections.emptyMap())
                        .setWithModel(false)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        ).waitResult();

        assertThat(
            result.getOffers().get(0).getPromo().getGiftOffers(),
            containsInAnyOrder(
                GiftOfferMatcher.giftOffer("Чистящее средство Electrolux E6DMH104", "150", 1),
                GiftOfferMatcher.giftOffer("Чистящее средство Electrolux E6MCS104", "590", 1),
                GiftOfferMatcher.giftOffer("Лезвия для скребка Electrolux E6HUB102 10 шт", "250", 1)
            )
        );

        assertThat(
            result.getOffers().get(1).getPromo().getGiftsDescription(),
            containsInAnyOrder(
                "Сертификат на бесплатную утилизацию!"
            )
        );
    }


    @Test
    public void enrichOffersGenericBundle() {
        long modelId = 1L;

        context.setClient(new Client() {{
            setType(Client.Type.INTERNAL);
        }});

        reportTestClient.getModelOffers(modelId,
                "report_model_offers_generic_bundle.json");

        GetOffersByModelResult result = controller.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(modelId)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Collections.singleton(OfferFieldV2.GIFT))
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(new ReportSort())
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(null)
                        .setFilterParameters(Collections.emptyMap())
                        .setWithModel(false)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        ).waitResult();

        List<OfferPromo> genericBundles = result.getOffers().get(0).getPromos().stream()
                .filter(promo -> promo.getType().equals("generic-bundle")).collect(Collectors.toList());

        assertThat(genericBundles, hasSize(1));
        assertThat(genericBundles.get(0).getGiftOffers(), hasSize(1));
        assertThat(genericBundles.get(0).getGiftOffers(),
                contains(
                        GiftOfferMatcher.giftOffer("Беспроводные наушники BandRate Smart BRSS0606BBB, black", "1070", 1))
        );

        assertThat(result.getOffers().get(1).getPromos(), anyOf(nullValue(), empty()));
    }

    @Test
    public void getRecommendedModelsToloka() {
        reportTestClient.getRecommendedModelsToloka("toloka.json");

        ModelListResult result = controller.getRecommendedToloka(
            Collections.singleton(ModelInfoField.LINK),
            10,
            GenericParams.DEFAULT
        ).waitResult();

        Assert.assertEquals(2, result.getModels().size());
    }

    @Test
    public void getRecommendedTolokaDjViewer_pagination() {
        int count = 3;
        long[] ids = {217320567, 217317960, 14124920};

        djViewerTestClient.doRequest(x -> x.get().serverMethod("/api/toloka"))
                .ok()
                .body("djviewer_toloka_models_big_list.json");

        reportTestClient.doRequest("modelinfo",
                x -> x.param("numdoc", "" + count)
                        .param("hyperid", "" + ids[0])
                        .param("hyperid", "" + ids[1])
                        .param("hyperid", "" + ids[2])
        ).ok().body("modelinfo_toloka.json");

        ModelListResult result = controller.getRecommendedTolokaDjViewer(
                Collections.singleton(ModelInfoField.LINK),
                count,
                GenericParams.DEFAULT,
                DEFAULT_DJ_VIEWER_EXPERIMENT,
                null,
                false
        ).waitResult();

        List<? extends Model> models = result.getModels();
        Assert.assertEquals(count, models.size());
        Assert.assertArrayEquals(ids,
                models.stream().mapToLong(Model::getId).toArray());
    }

    @Test
    public void getRecommendedTolokaDjViewer_withSpecificParam() {
        djViewerTestClient.doRequest(
                x -> x.get().serverMethod("/api/toloka")
                        .param("test", "value")
        ).ok().body("djviewer_toloka_models_big_list.json");

        reportTestClient.doRequest("modelinfo", Function.identity())
                .ok().body("modelinfo_toloka.json");

        int count = 3;
        HttpServletRequest request = MockRequestBuilder.start()
                .param("dj_viewer_test", "value")
                .build();
        ContextHolder.update(x -> x.setRequest(request));
        ModelListResult result = controller.getRecommendedTolokaDjViewer(
                Collections.singleton(ModelInfoField.LINK),
                count,
                GenericParams.DEFAULT,
                DEFAULT_DJ_VIEWER_EXPERIMENT,
                null,
                false
        ).waitResult();

        Assert.assertFalse(result.getModels().isEmpty());
    }

    @Test
    public void getRecommendedTolokaDjViewer_result() {
        int count = 3;
        long id = 217320567;

        djViewerTestClient.doRequest(x -> x.get().serverMethod("/api/toloka"))
                .ok()
                .body("djviewer_toloka_models_big_list.json");

        reportTestClient.doRequest("modelinfo",
                x -> x.param("hyperid", "" + id)
        ).ok().body("modelinfo_toloka.json");

        ModelListResult result = controller.getRecommendedTolokaDjViewer(
                Arrays.asList(ModelInfoField.LINK, ModelInfoField.PHOTO),
                count,
                GenericParams.DEFAULT,
                DEFAULT_DJ_VIEWER_EXPERIMENT,
                null,
                false
        ).waitResult();

        List<? extends Model> models = result.getModels();
        Assert.assertFalse(models.isEmpty());
        Model model = models.get(0);
        Assert.assertTrue(model instanceof ModelV2);
        ModelV2 modelV2 = (ModelV2) model;
        Assert.assertEquals("Беспроводные наушники Apple AirPods Pro", modelV2.getName());
        Assert.assertEquals("http://market.yandex.ru/product/612787165", modelV2.getLink());
        Assert.assertEquals(
                "https://avatars.mds.yandex.net/get-mpic/1865543/img_id3628162816343983789.jpeg/orig",
                modelV2.getPhoto().getUrl());
    }

    private static UltraController.Offer ultraControllerOffer(String name, float price,
                                                       String categoryName, String description,
                                                       String locale, String shopName) {
        return UltraController.Offer.newBuilder()
            .setOffer(name)
            .setPrice(price)
            .setMarketCategory(categoryName)
            .setDescription(description)
            .setLocale(locale)
            .setShopName(shopName)
            .build();
    }

    private static ModelMatchRequest modelMatchRequest(String name, float price,
                                                       String categoryName, String description,
                                                       String locale, String shopName) {
        ModelMatchRequest matchRequest = new ModelMatchRequest();
        matchRequest.setName(name);
        matchRequest.setPrice(price);
        matchRequest.setCategoryName(categoryName);
        matchRequest.setDescription(description);
        matchRequest.setLocale(locale);
        matchRequest.setShopName(shopName);
        return matchRequest;
    }


    private void checkRadioFilters(List<Filter> filters) {
        Assert.assertNotNull(filters);
        List<EnumFilter> radioFilters = filters.stream()
            .filter(f -> FilterType.RADIO.name().equals(f.getType()))
            .map(f -> (EnumFilter) f)
            .collect(Collectors.toList());
        Assert.assertFalse(radioFilters.isEmpty());
        radioFilters.forEach(filter -> {
            Assert.assertNotNull(filter.getValues());
            Assert.assertFalse(filter.getValues().isEmpty());
            filter.getValues().forEach(value -> {
                Assert.assertNotNull(value.getId());
                Assert.assertNotNull(value.getName());
            });
        });
    }

    private Matcher<Offer> cast(Matcher<? extends Offer> matcher) {
        return (Matcher<Offer>) matcher;
    }
}
