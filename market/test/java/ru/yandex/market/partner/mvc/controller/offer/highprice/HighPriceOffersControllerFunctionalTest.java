package ru.yandex.market.partner.mvc.controller.offer.highprice;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferPrice;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import NMarketIndexer.Common.Common;
import com.amazonaws.util.StringInputStream;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.parser.LiteInputStreamParser;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.datacamp.DataCampUtil;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.mvc.controller.offer.highprice.dto.HighPriceProblemTypeDto;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.mvc.controller.util.ResponseJsonUtil.getResult;

@DbUnitDataSet(before = "HighPriceOffersControllerFunctionalTest.before.csv")
class HighPriceOffersControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    @Qualifier("marketReportService")
    private AsyncMarketReportService marketReportService;

    @Autowired
    private DataCampClient dataCampShopClient;

    @BeforeEach
    void init() {
        mockEmptyMainReportResponse();
        mockNotEmptyDataCampResponse();
        mockNotEmptyPicturesReportRequest();
    }

    /**
     * Тест проверяет получение офферов из репорта для таба.
     */
    @Test
    void getReportOffersSimpleTest() {
        mockNotEmptyMainReportResponse();

        ResponseEntity<String> response = getHighPriceOffers(22L, 1, 20);
        String expected = StringTestUtil.getString(getClass(), "get-report-offers-simple-test-response.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    /**
     * Тест проверяет фильтр по проблемам репорта (скрытия в ответ не попадают).
     */
    @Test
    @DbUnitDataSet(before = "getHighPriceOffersWithHidingsTest.before.csv")
    void getResponsesFromHidingsAndReportBothWithFilterByReportProblemsTypeTest() {
        mockNotEmptyMainReportResponse();

        ResponseEntity<String> response = getHighPriceOffers(11L, 1, 20, Collections.emptyList(),
                Collections.emptyList(), Arrays.asList(HighPriceProblemTypeDto.BUYBOX, HighPriceProblemTypeDto.MINREF), null);
        String expected = StringTestUtil.getString(getClass(), "get-report-offers-simple-test-response.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    /**
     * Тест проверяет что оффера, у которых в репорте нет листовой категории, в качестве категории будут
     * содержать любую другую, если таковая имеется.
     */
    @Test
    void getOffersWithoutLeafCategoryTest() {
        mockNotEmptyMainReportResponse("getOffersWithoutLeafCategoryTest.reportResponse.json");

        ResponseEntity<String> response = getHighPriceOffers(22L, 1, 20);
        System.out.println(response);
        String expected = StringTestUtil.getString(getClass(), "get-offers-without-leaf-category-test-response.json");
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    private void mockEmptyMainReportResponse() {
        String reportResponse = StringTestUtil.getString(getClass(), "emptyReportResponse.json");
        when(marketReportService.async(any(), any()))
                .then(invocation -> {
                    LiteInputStreamParser<?> parser = invocation.getArgument(1);
                    CompletableFuture<Object> future = new CompletableFuture<>();
                    Object result = parser == null ? null : parser.parse(new StringInputStream(reportResponse));
                    future.complete(result);
                    return future;
                });
    }

    private void mockNotEmptyMainReportResponse() {
        mockNotEmptyMainReportResponse("getReportOffersSimpleTest.reportResponse.json");
    }

    private void mockNotEmptyMainReportResponse(String reportResponseFileName) {
        String reportResponse = StringTestUtil.getString(getClass(), reportResponseFileName);
        when(marketReportService.async(
                argThat(request -> request != null && request.getPlace() == MarketReportPlace.SUPPLIER_HIGH_PRICES),
                any()
        )).then(invocation -> {
            LiteInputStreamParser<?> parser = invocation.getArgument(1);
            CompletableFuture<Object> future = new CompletableFuture<>();
            Object result = parser == null ? null : parser.parse(new StringInputStream(reportResponse));
            future.complete(result);
            return future;
        });
    }

    private void mockNotEmptyPicturesReportRequest() {
        String reportResponse = StringTestUtil.getString(getClass(), "getReportPictures.reportResponse.json");
        when(marketReportService.async(
                argThat(request -> request != null && request.getPlace() == MarketReportPlace.SKU_SEARCH),
                any()
        )).then(invocation -> {
            LiteInputStreamParser<?> parser = invocation.getArgument(1);
            CompletableFuture<Object> future = new CompletableFuture<>();
            Object result = parser == null ? null : parser.parse(new StringInputStream(reportResponse));
            future.complete(result);
            return future;
        });
    }

    private void mockNotEmptyDataCampResponse() {
        List<DataCampOffer.Offer> offers = Arrays.asList(
                createDataCampOfferInfo("111_offer_id", 670),
                createDataCampOfferInfo("112_offer_id", 480),
                createDataCampOfferInfo("113_offer_id", 398),
                createDataCampOfferInfo("114_offer_id", 378),
                createDataCampOfferInfo("115_offer_id", 379),
                createDataCampOfferInfo("116_offer_id", 378),
                createDataCampOfferInfo("117_offer_id", 378),
                createDataCampOfferInfo("46783RTVS7405", 500),
                createDataCampOfferInfo("4777QQQ684", 836),
                createDataCampOfferInfo("10855364", 89263),
                createDataCampOfferInfo("221_offer_id", 3764),
                createDataCampOfferInfo("222_offer_id", 4666),
                createDataCampOfferInfo("223_offer_id", 600),
                createDataCampOfferInfo("push2pull13729383", 2293),
                createDataCampOfferInfo("push2pull1712539807", 14492)
        );

        doReturn(
                SyncChangeOffer.FullOfferResponse.newBuilder()
                        .addAllOffer(offers)
                        .build()
        ).when(dataCampShopClient).getOffers(anyLong(), any(), any(SyncChangeOffer.ChangeOfferRequest.class));
    }

    private DataCampOffer.Offer createDataCampOfferInfo(String offerId, long price) {
        return DataCampOffer.Offer.newBuilder()
                .setIdentifiers(
                        DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId(offerId)
                                .build()
                )
                .setPrice(
                        DataCampOfferPrice.OfferPrice.newBuilder()
                                .setBasic(
                                        DataCampOfferPrice.PriceBundle.newBuilder()
                                                .setBinaryPrice(
                                                        Common.PriceExpression.newBuilder()
                                                                .setPrice(DataCampUtil.powToIdx(BigDecimal.valueOf(price)))
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    private ResponseEntity<String> getHighPriceOffers(long campaignId, int page, int count) {
        return getHighPriceOffers(campaignId, page, count, null, null, null, null);
    }

    private ResponseEntity<String> getHighPriceOffers(
            long campaignId,
            int page,
            int count,
            @Nullable List<Long> categoryIds,
            @Nullable List<String> vendors,
            @Nullable List<HighPriceProblemTypeDto> problemTypes,
            @Nullable String textQueryString
    ) {
        String requestCategories = "";
        if (CollectionUtils.isNotEmpty(categoryIds)) {
            StringJoiner categoriesJoiner = new StringJoiner(",", "&categoryId=", "");
            categoryIds.forEach(categoryId -> categoriesJoiner.add(categoryId.toString()));
            requestCategories = categoriesJoiner.toString();
        }

        String requestVendors = "";
        if (CollectionUtils.isNotEmpty(vendors)) {
            StringJoiner vendorsJoiner = new StringJoiner(",", "&vendor=", "");
            vendors.forEach(vendorsJoiner::add);
            requestVendors = vendorsJoiner.toString();
        }

        String requestProblemTypes = "";
        if (CollectionUtils.isNotEmpty(problemTypes)) {
            StringJoiner problemTypesJoiner = new StringJoiner(",", "&problem_type=", "");
            problemTypes.forEach(problemType -> problemTypesJoiner.add(problemType.getId()));
            requestProblemTypes = problemTypesJoiner.toString();
        }

        String requestQ = textQueryString == null ? "" : "&q=" + textQueryString;

        return FunctionalTestHelper.get(
                baseUrl + "/high-price/shop-skus?" +
                        "campaign_id=" + campaignId +
                        "&page=" + page +
                        "&count=" + count +
                        requestCategories +
                        requestVendors  +
                        requestProblemTypes +
                        requestQ
        );
    }
}
