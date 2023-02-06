package ru.yandex.market.api.controller.v2;

import java.util.Collections;

import javax.inject.Inject;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.category.FilterService;
import ru.yandex.market.api.category.FilterSetType;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.FiltersResult;
import ru.yandex.market.api.domain.v2.GetModelReviewsResult;
import ru.yandex.market.api.domain.v2.ModelListResult;
import ru.yandex.market.api.domain.v2.ModelResult;
import ru.yandex.market.api.domain.v2.OfferResult;
import ru.yandex.market.api.domain.v2.model.GetModelSpecificationResult;
import ru.yandex.market.api.domain.v2.offers.GetOffersByModelResult;
import ru.yandex.market.api.domain.v2.opinion.GetOpinionsResult;
import ru.yandex.market.api.domain.v2.outlet.GetOutletsResult;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.internal.opinion.OpinionsSort;
import ru.yandex.market.api.internal.report.CommonReportOptions.OfferGroupType;
import ru.yandex.market.api.internal.report.ReportSort;
import ru.yandex.market.api.internal.report.ReportSortType;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.offer.GetOffersByModelRequest;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.exceptions.AccessDeniedException;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.concurrent.ApiDeferredResult;
import ru.yandex.market.api.util.httpclient.clients.LoyaltyTestClient;
import ru.yandex.market.api.util.httpclient.clients.ModelParamsTestClient;
import ru.yandex.market.api.util.httpclient.clients.PersStaticTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

/**
 * Created by tesseract on 12.07.17.
 *
 * @see <a href="https://st.yandex-team.ru/MARKETAPI-3849">MARKETAPI-3849: Добавить фильтрация по вендору в ручках
 * получения информации по модели и офферу</a>
 */
@WithContext
public class VendorAccessTest extends BaseTest {

    @Inject
    ModelsControllerV2 modelsController;
    @Inject
    OpinionsControllerV2 opinionsController;
    @Inject
    ReportTestClient reportTestClient;
    @Inject
    ModelParamsTestClient modelParamsTestClient;
    @Inject
    PersStaticTestClient persStaticTestClient;
    @Inject
    OutletControllerV2 outletController;
    @Inject
    OffersControllerV2 offersController;
    @Inject
    LoyaltyTestClient loyaltyTestClient;

    @Test(expected = AccessDeniedException.class)
    public void getDefaultOffer_Vendor_notOwner() {
        long id = 12299034L;
        long otherVendorId = 123456L;
        // настройка системы
        prepareVendorClient(otherVendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        // вызов системы
        ApiDeferredResult<OfferResult> result = modelsController.getDefaultOffer(id,
            Collections.emptyList(),
            Collections.emptyMap(),
            genericParams);
        // проверка утверждений
        result.waitResult();
    }

    @Test
    public void getDefaultOffer_Vendor_owner() {
        long id = 12299034L;
        long vendorId = 153043L;
        // настройка системы
        prepareVendorClient(vendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        reportTestClient.getDefaultOffer(id, "defaultoffer_12299034.json");
        // вызов системы
        ApiDeferredResult<OfferResult> result = modelsController.getDefaultOffer(id,
            Collections.emptyList(),
            Collections.emptyMap(),
            genericParams);
        // проверка утверждений
        Assert.assertNotNull("Не должно быть исключения", result.waitResult());
        Assert.assertNotNull(result.waitResult().getOffer());
    }

    @Test(expected = AccessDeniedException.class)
    public void getLooksAsModels_Vendor_notOwner() {
        long id = 12299034L;
        long otherVendorId = 123456L;
        // настройка системы
        prepareVendorClient(otherVendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        // вызов системы
        ApiDeferredResult<ModelListResult> result = modelsController.getLooksAsModels(id,
            Collections.emptyList(),
            Collections.emptyMap(),
            10,
            genericParams);
        // проверка утверждений
        result.waitResult();
    }

    @Test
    public void getLooksAsModels_Vendor_owner() {
        long id = 12299034L;
        long vendorId = 153043L;
        // настройка системы
        prepareVendorClient(vendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        reportTestClient.productAnalogs(id, "product_analogs_12299034.json");
        reportTestClient.getModelInfoById(Lists.newArrayList(10830297l, 10830298l, 10984311l, 10984317l, 10984360l, 10984361l, 10985953l, 10985958l, 10985961l, 11158483l), "modelinfo_analogs_12299034.json");
        // вызов системы
        ApiDeferredResult<ModelListResult> result = modelsController.getLooksAsModels(id,
            Collections.emptyList(),
            Collections.emptyMap(),
            10,
            genericParams);
        // проверка утверждений
        Assert.assertNotNull("Не должно быть исключения", result.waitResult());
    }

    @Test(expected = AccessDeniedException.class)
    public void getModelAccessories_Vendor_notOwner() {
        long id = 12299034L;
        long otherVendorId = 12345L;
        // настройка системы
        prepareVendorClient(otherVendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        // вызов системы
        ApiDeferredResult<ModelListResult> result = modelsController.getModelAccessories(id,
            null,
            91013,
            Collections.emptyList(),
            Collections.emptyMap(),
            PageInfo.DEFAULT,
            genericParams,
            null,
                null);
        // проверка утверждений
        result.waitResult();
    }

    @Test
    public void getModelAccessories_Vendor_owner() {
        long id = 12299034L;
        long vendorId = 153043L;
        // настройка системы
        prepareVendorClient(vendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        reportTestClient.getProductAccessories(id, "accessories_12299034.json");
        // вызов системы
        ApiDeferredResult<ModelListResult> result = modelsController.getModelAccessories(id,
            null,
            91013,
            Collections.emptyList(),
            Collections.emptyMap(),
            PageInfo.DEFAULT,
            genericParams,
            null,
                null);
        // проверка утверждений
        Assert.assertNotNull("Не должно быть исключения", result.waitResult());
    }

    @Test(expected = AccessDeniedException.class)
    public void getModelOffersFilters_Vendor_notOwner() {
        long id = 12299034L;
        long otherVendorId = 123456L;
        // настройка системы
        prepareVendorClient(otherVendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        // вызов системы
        ApiDeferredResult<FiltersResult> result = modelsController.getModelOffersFilters(id,
            Collections.emptyList(),
            FilterSetType.ALL,
            FilterService.FilterSort.NONE,
            genericParams);
        // проверка утверждений
        result.waitResult();
    }

    @Test
    public void getModelOffersFilters_Vendor_owner() {
        long id = 12299034L;
        long vendorId = 153043L;
        // настройка системы
        prepareVendorClient(vendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        reportTestClient.getModelOffers(id, "productoffers_12299034.json");
        reportTestClient.getModelModifications(id, "model_modifications_12299034.json");
        // вызов системы
        ApiDeferredResult<FiltersResult> result = modelsController.getModelOffersFilters(id,
            Collections.emptyList(),
            FilterSetType.ALL,
            FilterService.FilterSort.NONE,
            genericParams);
        // проверка утверждений
        Assert.assertNotNull("Не должно быть исключения", result.waitResult());
    }

    @Test(expected = AccessDeniedException.class)
    public void getModelOpinions_Vendor_notOwner() {
        long id = 12299034L;
        long otherVendorId = 123456L;
        // настройка системы
        prepareVendorClient(otherVendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        // вызов системы
        ApiDeferredResult<GetOpinionsResult> result = opinionsController.getModelOpinions(id,
            0,
            PageInfo.DEFAULT,
            new OpinionsSort(OpinionsSort.Type.DATE, SortOrder.DESC),
            null,
            Collections.emptyList(),
            null,
                null);
        // проверка утверждений
        result.waitResult();
    }

    @Test
    public void getModelOpinions_Vendor_owner() {
        long id = 12299034L;
        long vendorId = 153043L;
        // настройка системы
        prepareVendorClient(vendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        persStaticTestClient.getModelOpinion(id, "modelOpinions_12299034.json");
        // вызов системы
        ApiDeferredResult<GetOpinionsResult> result = opinionsController.getModelOpinions(id,
            0,
            PageInfo.DEFAULT,
            new OpinionsSort(OpinionsSort.Type.DATE, SortOrder.DESC),
            null,
            Collections.emptyList(),
            null,
                null);
        // проверка утверждений
        Assert.assertNotNull("Не должно быть исключения", result.waitResult());
    }

    @Test(expected = AccessDeniedException.class)
    public void getModelOutlets_Vendor_notOwner() {
        long id = 12299034L;
        long otherVendorId = 123456L;
        // настройка системы
        prepareVendorClient(otherVendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        // вызов системы
        ApiDeferredResult<GetOutletsResult> result = outletController.getModelOutlets(
            id,
            null,
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyMap(),
            new ReportSort(ReportSortType.DEFAULT, SortOrder.ASC),
            PageInfo.DEFAULT,
            genericParams
        );
        // проверка утверждений
        result.waitResult();
    }

    @Test
    public void getModelOutlets_Vendor_owner() {
        long id = 12299034L;
        long vendorId = 153043L;
        // настройка системы
        prepareVendorClient(vendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        reportTestClient.getGeoModelOffers(id, "geo_12299034.xml");
        // вызов системы
        ApiDeferredResult<GetOutletsResult> result = outletController.getModelOutlets(
            id,
            null,
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyMap(),
            new ReportSort(ReportSortType.DEFAULT, SortOrder.ASC),
            PageInfo.DEFAULT,
            genericParams
        );
        // проверка утверждений
        Assert.assertNotNull("Не должно быть исключения", result.waitResult());
    }

    @Test(expected = AccessDeniedException.class)
    public void getModelSpecification_Vendor_notOwner() {
        long id = 12299034L;
        long vendorId = 12345L;
        // настройка системы
        prepareVendorClient(vendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_specification_12299034.json");
        // вызов системы
        ApiDeferredResult<GetModelSpecificationResult> result = modelsController.getModelSpecification(id, genericParams);
        // проверка утверждений
        result.waitResult();
    }

    @Test
    public void getModelSpecification_Vendor_owner() {
        long id = 12299034L;
        long vendorId = 153043L;
        // настройка системы
        prepareVendorClient(vendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_specification_12299034.json");
        // вызов системы
        ApiDeferredResult<GetModelSpecificationResult> result = modelsController.getModelSpecification(id, genericParams);
        // проверка утверждений
        Assert.assertNotNull("Не должно быть исключения", result.waitResult());
    }

    @Test(expected = AccessDeniedException.class)
    public void getModelVendor_Vendor_notOwner() {
        long id = 12299034L;
        long otherVendorId = 12345L;
        // настройка системы
        prepareVendorClient(otherVendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        // вызов системы
        ApiDeferredResult<ModelResult> result = modelsController.getModel(id,
            Collections.emptyList(),
            Collections.emptyMap(),
            genericParams,
                false,
                null);
        // проверка утверждений
        result.waitResult();
    }

    @Test
    public void getModelVendor_Vendor_owner() {
        long id = 12299034L;
        long vendorId = 153043L;
        // настройка системы
        prepareVendorClient(vendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        // вызов системы
        ApiDeferredResult<ModelResult> result = modelsController.getModel(id,
            Collections.emptyList(),
            Collections.emptyMap(),
            genericParams,
                false,
                null);
        // проверка утверждений
        Assert.assertEquals(id, result.waitResult().getModel().getId());
    }

    @Test(expected = AccessDeniedException.class)
    public void getOfferOutlets_Vendor_notOwner() {
        String offerId = "GCTe_Le4WCV1ga9oMN4s6A";
        long otherVendorId = 123456L;
        // настройка системы
        prepareVendorClient(otherVendorId);

        reportTestClient.getOfferInfo(new OfferId(offerId, null), "offerinfo_GCTe_Le4WCV1ga9oMN4s6A.json");
        // вызов системы
        ApiDeferredResult<GetOutletsResult> result = outletController.getOfferOutlets(
            new OfferId(offerId, null),
            null,
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyMap(),
            null,
            PageInfo.DEFAULT,
            genericParams
        );
        // проверка утверждений
        result.waitResult();
    }

    @Test
    public void getOfferOutlets_Vendor_owner() {
        String offerId = "GCTe_Le4WCV1ga9oMN4s6A";
        long vendorId = 153043L;
        // настройка системы
        prepareVendorClient(vendorId);

        reportTestClient.getOfferInfo(new OfferId(offerId, null), "offerinfo_GCTe_Le4WCV1ga9oMN4s6A.json");
        reportTestClient.getGeoOffer(offerId, "geo_GCTe_Le4WCV1ga9oMN4s6A.json");
        // вызов системы
        ApiDeferredResult<GetOutletsResult> result = outletController.getOfferOutlets(
            new OfferId(offerId, null),
            null,
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyMap(),
            null,
            PageInfo.DEFAULT,
            genericParams
        );
        // проверка утверждений
        Assert.assertNotNull("Не должно быть исключения", result.waitResult());
    }

    @Test(expected = AccessDeniedException.class)
    public void getOffer_Vendor_notOwner() {
        String offerId = "GCTe_Le4WCV1ga9oMN4s6A";
        long otherVendorId = 123456L;
        // настройка системы
        prepareVendorClient(otherVendorId);

        reportTestClient.getOfferInfo(new OfferId(offerId, null), "offerinfo_GCTe_Le4WCV1ga9oMN4s6A.json");
        // вызов системы
        ApiDeferredResult<OfferResult> result = offersController.getOffer(
            new OfferId(offerId, null),
            false,
            Collections.emptyList(),
            genericParams
        );
        // проверка утверждений
        result.waitResult();
    }

    @Test
    public void getOffer_Vendor_owner() {
        String offerId = "GCTe_Le4WCV1ga9oMN4s6A";
        long vendorId = 153043L;
        // настройка системы
        prepareVendorClient(vendorId);

        reportTestClient.getOfferInfo(new OfferId(offerId, null), "offerinfo_GCTe_Le4WCV1ga9oMN4s6A.json");
        // вызов системы
        ApiDeferredResult<OfferResult> result = offersController.getOffer(
            new OfferId(offerId, null),
            false,
            Collections.emptyList(),
            genericParams
        );
        // проверка утверждений
        Assert.assertNotNull("Не должно быть исключения", result.waitResult());
    }

    @Test(expected = AccessDeniedException.class)
    public void getOffers_Vendor_notOwner() {
        long id = 12299034L;
        long otherVendorId = 12345L;
        // настройка системы
        prepareVendorClient(otherVendorId);
        ContextHolder.get().setUser(new User(new OauthUser(123L), null, null, null));

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        // вызов системы
        ApiDeferredResult<GetOffersByModelResult> result = modelsController.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(id)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Collections.emptyList())
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(new ReportSort())
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(OfferGroupType.NONE)
                        .setFilterParameters(Collections.emptyMap())
                        .setGpsCoords(null)
                        .setWithModel(true)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        );
        // проверка утверждений
        result.waitResult();
    }

    @Test
    public void getOffers_Vendor_owner() {
        long id = 12299034L;
        long vendorId = 153043L;
        // настройка системы
        prepareVendorClient(vendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        reportTestClient.getModelOffers(id, "productoffers_12299034.json");
        // вызов системы
        ApiDeferredResult<GetOffersByModelResult> result = modelsController.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(id)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Collections.emptyList())
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(new ReportSort())
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(OfferGroupType.NONE)
                        .setFilterParameters(Collections.emptyMap())
                        .setGpsCoords(null)
                        .setWithModel(true)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        );
        // проверка утверждений
        Assert.assertNotNull("Не должно быть исключения", result.waitResult());
    }

    @Test(expected = AccessDeniedException.class)
    public void getReviews_Vendor_notOwner() {
        long id = 12299034L;
        long otherVendorId = 123456L;
        // настройка системы
        prepareVendorClient(otherVendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        // вызов системы
        ApiDeferredResult<GetModelReviewsResult> result = modelsController.getReviews(id,
            PageInfo.DEFAULT,
            genericParams);
        // проверка утверждений
        result.waitResult();
    }

    @Test
    public void getReviews_Vendor_owner() {
        long id = 12299034L;
        long vendorId = 153043L;
        // настройка системы
        prepareVendorClient(vendorId);

        reportTestClient.getModelInfoById(id, "modelinfo_12299034.json");
        modelParamsTestClient.params(id, "modelparams_12299034.xml");
        // вызов системы
        ApiDeferredResult<GetModelReviewsResult> result = modelsController.getReviews(id,
            PageInfo.DEFAULT,
            genericParams);
        // проверка утверждений
        Assert.assertNotNull("Не должно быть исключения", result.waitResult());
    }

    private void prepareVendorClient(long vendorId) {
        Client client = new Client();
        client.setType(Client.Type.VENDOR);
        client.setVendorId(vendorId);
        context.setClient(client);
    }

}
