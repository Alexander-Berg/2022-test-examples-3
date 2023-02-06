package ru.yandex.market.deliverycalculator.searchengine.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.PbSnUtils;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffProgramType;
import ru.yandex.market.deliverycalculator.searchengine.FunctionalTest;
import ru.yandex.market.deliverycalculator.searchengine.service.FeedParserWorkflowService;
import ru.yandex.market.deliverycalculator.workflow.solomon.BoilingSolomonService;
import ru.yandex.market.deliverycalculator.workflow.solomon.BoilingSolomonTestUtil;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.BoilingStageType;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.GenerationCachingBoilingKey;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты для {@link IdxReportController}.
 */
class IdxReportControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private FeedParserWorkflowService feedParserWorkflowService;

    @Autowired
    private BoilingSolomonService boilingSolomonService;

    @BeforeEach
    void before() {
        feedParserWorkflowService.updateActiveGenerationId();
        feedParserWorkflowService.importGenerations();
    }

    @Test
    @DbUnitDataSet(before = "classpath:idxreport/getShopDeliveryMetaTest.before.csv")
    void getShopDeliveryMetaTest() throws Exception {
        getShopDeliveryMetaRequest(99, null)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(shopDeliveryMetaErrorResponse(99)));

        getShopDeliveryMetaRequest(100, null)
                .andExpect(status().isOk())
                .andExpect(content().json(shopDeliveryMetaResponse(2L, true, "RUR")));

        getShopDeliveryMetaRequest(101, null)
                .andExpect(status().isOk())
                .andExpect(content().json(shopDeliveryMetaResponse(6L, 2., 13, 10, 15, "RUR")));

        getShopDeliveryMetaRequest(102, Collections.singletonList("market_delivery"))
                .andExpect(status().isOk())
                .andExpect(content().json(shopDeliveryMetaResponse(8L, "RUR")));

        getShopDeliveryMetaRequest(103, null)
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(shopDeliveryMetaErrorResponse(103)));

        getShopDeliveryMetaRequest(104, null)
                .andExpect(status().isOk())
                .andExpect(content().json(shopDeliveryMetaResponse(13L, "RUR")));

        //белый магазин с включенным региональным авторасчетом СиС
        getShopDeliveryMetaRequest(105, null)
                .andExpect(status().isOk())
                .andExpect(content().json(shopDeliveryMetaResponse(17L, "RUR")));

        getShopDeliveryMetaRequest(106, null)
                .andExpect(status().isOk())
                .andExpect(content().json(shopDeliveryMetaResponse(17L, "BYN")));

        getShopDeliveryMetaRequest(107, null)
                .andExpect(status().isOk())
                .andExpect(content().json(shopDeliveryMetaResponse(18L, "BYN")));

        final GenerationCachingBoilingKey key1010 = GenerationCachingBoilingKey.of(1010L, 4L, 4L, DeliveryTariffProgramType.WHITE_MARKET_DELIVERY, BoilingStageType.GENERATION_CACHING_STAGE);
        final GenerationCachingBoilingKey key2020 = GenerationCachingBoilingKey.of(2020L, 5L, 5L, DeliveryTariffProgramType.DAAS, BoilingStageType.GENERATION_CACHING_STAGE);
        BoilingSolomonTestUtil.checkStageEvents(boilingSolomonService, key1010, key2020);
    }

    @Test
    @DbUnitDataSet(before = {
            "classpath:idxreport/getShopOffersDeliveryInfoTest.before.csv",
            "classpath:idxreport/getShopOffersDeliveryInfo_YaDoWithoutModifiersTest.before.csv"
    })
    void getShopOffersDeliveryInfo_YaDoWithoutModifiersTest() throws Exception {
        getShopOffersDeliveryInfoRequest(
                createShopOffersPbSnRequest(
                        DeliveryCalcProtos.ShopOffersReq.newBuilder()
                                .setGenerationId(5L)
                                .setShopId(1000)
                                .setFeedId(3000)
                                .addAllOffers(
                                        Arrays.asList(
                                                createOfferWithCustomWeight(15.).build(),
                                                createOfferWithCustomWeight(70.).build()
                                        )
                                )
                                .build()
                ))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        StringTestUtil.getString(
                                IdxReportControllerFunctionalTest.class,
                                "/idxreport/response/shop-without-modifiers.json"
                        )
                ));

        GenerationCachingBoilingKey key567354 =
                GenerationCachingBoilingKey.of(567354L, 1L, 1L, DeliveryTariffProgramType.DAAS, BoilingStageType.GENERATION_CACHING_STAGE);
        GenerationCachingBoilingKey key894563 =
                GenerationCachingBoilingKey.of(894563L, 2L, 2L, DeliveryTariffProgramType.DAAS, BoilingStageType.GENERATION_CACHING_STAGE);
        GenerationCachingBoilingKey key6725 =
                GenerationCachingBoilingKey.of(6725L, 3L, 3L, DeliveryTariffProgramType.DAAS, BoilingStageType.GENERATION_CACHING_STAGE);

        BoilingSolomonTestUtil.checkStageEvents(boilingSolomonService, key567354, key894563, key6725);
    }

    @Test
    @DbUnitDataSet(before = {
            "classpath:idxreport/getShopOffersDeliveryInfoTest.before.csv",
            "classpath:idxreport/getShopOffersDeliveryInfo_YaDoWithModifiersTest.before.csv"
    })
    void getShopOffersDeliveryInfo_YaDoWithModifiersTest() throws Exception {
        getShopOffersDeliveryInfoRequest(
                createShopOffersPbSnRequest(
                        DeliveryCalcProtos.ShopOffersReq.newBuilder()
                                .setGenerationId(5L)
                                .setShopId(1001)
                                .setFeedId(3001)
                                .addAllOffers(
                                        Arrays.asList(
                                                createOfferWithCustomWeight(15.).build(),
                                                createOfferWithCustomWeight(30.).build(),
                                                createOfferWithCustomWeight(80.).build()
                                        )
                                )
                                .build()
                ))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        StringTestUtil.getString(
                                IdxReportControllerFunctionalTest.class,
                                "/idxreport/response/shop-with-modifiers.json"
                        )
                ));
    }

    @Test
    @DisplayName("Магазин доставляет из нескольких регионов, учитываем эти настройки")
    @DbUnitDataSet(before = {
        "classpath:idxreport/getShopOffersDeliveryInfoTest.before.csv",
        "classpath:idxreport/getShopOffersDeliveryInfo_YaDoAsMappingTest.before.csv"
    })
    void getShopOffersDeliveryInfo_YaDoAsMappingTest() throws Exception {
        getShopOffersDeliveryInfoRequest(
            createShopOffersPbSnRequest(
                DeliveryCalcProtos.ShopOffersReq.newBuilder()
                    .setGenerationId(5L)
                    .setShopId(1001)
                    .setFeedId(3001)
                    .addAllOffers(
                        Arrays.asList(
                            createOfferWithCustomWeight(15.).build(),
                            createOfferWithCustomWeight(30.).build(),
                            createOfferWithCustomWeight(80.).build()
                        )
                    )
                    .build()
            ))
            .andExpect(status().isOk())
            .andExpect(result -> {
                JSONCompareResult compareResult = JSONCompare.compareJSON(
                    StringTestUtil.getString(
                        IdxReportControllerFunctionalTest.class,
                        "/idxreport/response/shop-several-regions.json"
                    ),
                    result.getResponse().getContentAsString(),
                    JSONCompareMode.NON_EXTENSIBLE
                );

                Assertions.assertThat(compareResult.failed())
                    .as(compareResult.getMessage())
                    .isFalse();
            });


    }

    @Test
    @DbUnitDataSet(before = {
            "classpath:idxreport/getShopOffersDeliveryInfoTest.before.csv",
            "classpath:idxreport/getShopOffersDeliveryInfo_WhiteDeliveryCourierWithYaDo_checkCostModifiers.before.csv"
    })
    @DisplayName("Проверка применения модификатора, изменяющего цену доставки: должен примениться для офферов, цена " +
            "которых больше 600 рублей")
    void getShopOffersDeliveryInfo_WhiteDeliveryCourierWithYaDo_checkCostModifiers() throws Exception {
        getShopOffersDeliveryInfoRequest(
                createShopOffersPbSnRequest(
                        DeliveryCalcProtos.ShopOffersReq.newBuilder()
                                .setGenerationId(5L)
                                .setShopId(111)
                                .setFeedId(222)
                                .addAllOffers(
                                        Arrays.asList(
                                                setOfferPrice(createOfferWithCustomWeight(15.), 500.).build(),
                                                setOfferPrice(createOfferWithCustomWeight(15.), 700.).build()
                                        )
                                )
                                .build()
                ))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        StringTestUtil.getString(
                                IdxReportControllerFunctionalTest.class,
                                "/idxreport/response/white_delivery_courier_yado_cost_modifiers.json"
                        ), true
                ));
    }

    @Test
    @DbUnitDataSet(before = {
            "classpath:idxreport/getShopOffersDeliveryInfoTest.before.csv",
            "classpath:idxreport/getShopOffersDeliveryInfo_Supplier_RegularDeliveryTest.before.csv"
    })
    @DisplayName("Проверка своих тарифов для поставщиков: courier & pickup на первой версии бакетов")
    void getShopOffersDeliveryInfo__Supplier_RegularDeliveryTest() throws Exception {
        checkRegularTariffsResponse(1002, 3002, 6, "regular-shop.json");
    }

    @Test
    @DbUnitDataSet(before = {
            "classpath:idxreport/getShopOffersDeliveryInfoTest.before.csv",
            "classpath:idxreport/getShopOffersDeliveryInfo_RegularDeliveryTest.before.csv"
    })
    @DisplayName("Проверка своих тарифов: использование второй версии бакетов для courier & pickup")
    void getShopOffersDeliveryInfo_RegularDeliveryTest_newBuckets() throws Exception {
        checkRegularTariffsResponse(1002, 3002, 6, "regular-shop-new-buckets.json");
    }

    @Test
    @DbUnitDataSet(before = {
            "classpath:idxreport/getShopOffersDeliveryInfoTest.before.csv",
            "classpath:idxreport/getShopOffersDeliveryInfo_RegularDeliveryTest.before.csv"
    })
    @DisplayName("Проверка своих тарифов: использование второй версии, но у pickup еще нет поколения с новыми бакетами")
    void getShopOffersDeliveryInfo_RegularDeliveryTest_useNewBuckets_noNewPickupBuckets() throws Exception {
        checkRegularTariffsResponse(3000, 5002, 8, "regular-shop-new-buckets-without-pickup.json");
    }

    @Test
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfo_RegularDeliveryTest.NoCampaignType.before.csv")
    @DisplayName("Проверка своих тарифов: использование второй версии, но у courier & pickup не указан тип кампании")
    void getShopOffersDeliveryInfo_RegularDeliveryTest_useNewBuckets_NoCampaignType() throws Exception {
        checkRegularTariffsResponse(5000, 5002, 3, "regular-shop-new-buckets-no-campaign-type.json");
    }

    @Test
    @DisplayName("Проверка курьерского авторасчета")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryCourierTest() throws Exception {
        checkWhiteCourierResponse("white_delivery.json", createOfferWithCustomWeight(5.));
    }

    @Test
    @DisplayName("Проверка курьерского авторасчета для оффера без габаритов")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryCourier_NoDimensionsTest() throws Exception {
        checkWhiteCourierResponse("white_delivery.json", createOfferWithoutDimensions(5.));
    }

    @Test
    @DisplayName("Проверка курьерского авторасчета для оффера c весом меньше диапазона тарифа")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryCourier_TooLowWeightTariffTest() throws Exception {
        checkWhiteCourierResponse("white_delivery_empty_response.json", createOfferWithCustomWeight(0.5));
    }

    @Test
    @DisplayName("Проверка курьерского авторасчета для оффера c весом меньше диапазона рула")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryCourier_TooLowWeightTest() throws Exception {
        checkWhiteCourierResponse("white_delivery_empty_response.json", createOfferWithCustomWeight(1.));
    }

    @Test
    @DisplayName("Проверка курьерского авторасчета для оффера c весом на нижней границе диапазона рула")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryCourier_LowerBoundTest() throws Exception {
        checkWhiteCourierResponse("white_delivery_empty_response.json", createOfferWithCustomWeight(2.));
    }


    @Test
    @DisplayName("Проверка курьерского авторасчета для оффера c весом на верхней границе диапазона рула")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryCourier_UpperBoundTest() throws Exception {
        checkWhiteCourierResponse("white_delivery.json", createOfferWithCustomWeight(15.));
    }

    @Test
    @DisplayName("Проверка курьерского авторасчета для оффера c весом больше диапазона рула")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryCourier_TooHeavyTariffTest() throws Exception {
        checkWhiteCourierResponse("white_delivery_empty_response.json", createOfferWithCustomWeight(15.1));
    }

    @Test
    @DisplayName("Проверка курьерского авторасчета для оффера c весом больше диапазона тарифа")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryCourier_TooHeavyTest() throws Exception {
        checkWhiteCourierResponse("white_delivery_empty_response.json", createOfferWithCustomWeight(50.1));
    }

    @Test
    @DisplayName("Проверка курьерского авторасчета для оффера c граничным весом среди нескольких рулов")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryManyRulesTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryCourier_ManyRulesTest() throws Exception {
        checkWhiteCourierResponse("white_delivery.json", createOfferWithCustomWeight(3.));
    }

    @Test
    @DisplayName("Проверка авторасчета самовывоза, если есть модификаторы для другой службы")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryPickupTest_ShopAutoCalculated_OtherModifiers.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryPickup_ShopAutoCalculated_OtherModifiersTest() throws Exception {
        checkWhitePickupResponse("white_delivery_pickup_shop_auto_calc_no_offers.json");
    }

    @Test
    @DisplayName("Проверка авторасчета самовывоза, если у магазина нет модификаторов")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryPickupTest_ShopAutoCalculated_WithoutModifiers.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryPickup_ShopAutoCalculated_WithoutModifiersTest() throws Exception {
        checkWhitePickupResponse("white_delivery_pickup_shop_auto_calc_no_offers.json");
    }

    @Test
    @DisplayName("Проверка случая, когда одна СД имеет курьерский и ПВЗ тариф, бакеты должны соответствовать типу тарифа и настройкам авторасчета")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfo_ShopAutoCalculatedWithSameCarrierAndPickupDeliveryService.before.csv")
    void getShopOffersDeliveryInfo_ShopAutoCalculatedWithSameCarrierAndPickupDeliveryService() throws Exception {
        checkWhitePickupResponse("white_delivery_pickup_currier_shop_auto_calc.json");
    }

    @Test
    @DisplayName("Проверка авторасчета самовывоза")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryPickupTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryPickupTest() throws Exception {
        checkWhitePickupResponse("white_delivery_pickup.json");
    }

    @Test
    @DisplayName("Проверка авторасчета самовывоза для оффера без габаритов")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryPickupTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryPickup_NoDimensionsTest() throws Exception {
        checkWhitePickupResponse("white_delivery_pickup.json", createOfferWithoutDimensions(5.));
    }

    @Test
    @DisplayName("Проверка авторасчета самовывоза для оффера c весом меньше диапазона тарифа")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryPickupTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryPickup_TooLowWeightTariffTest() throws Exception {
        checkWhitePickupResponse("white_delivery_empty_response.json", createOfferWithCustomWeight(0.1));
    }

    @Test
    @DisplayName("Проверка авторасчета самовывоза для оффера c весом меньше диапазона рула")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryPickupTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryPickup_TooLowWeightTest() throws Exception {
        checkWhitePickupResponse("white_delivery_empty_response.json", createOfferWithCustomWeight(2.));
    }

    @Test
    @DisplayName("Проверка авторасчета самовывоза для оффера c весом на нижней границе диапазона рула")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryPickupTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryPickup_LowerBoundTest() throws Exception {
        checkWhitePickupResponse("white_delivery_empty_response.json", createOfferWithCustomWeight(3.));
    }

    @Test
    @DisplayName("Проверка авторасчета самовывоза для оффера c весом на верхней границе диапазона рула")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryPickupTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryPickup_UpperBoundTest() throws Exception {
        checkWhitePickupResponse("white_delivery_pickup.json", createOfferWithCustomWeight(50.));
    }

    @Test
    @DisplayName("Проверка авторасчета самовывоза для оффера c весом больше диапазона рула")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryPickupTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryPickup_TooHeavyTest() throws Exception {
        checkWhitePickupResponse("white_delivery_empty_response.json", createOfferWithCustomWeight(50.1));
    }

    @Test
    @DisplayName("Проверка авторасчета самовывоза для оффера c весом больше диапазона тарифа")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryPickupTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryPickup_TooHeavyTariffTest() throws Exception {
        checkWhitePickupResponse("white_delivery_empty_response.json", createOfferWithCustomWeight(120.1));
    }

    @Test
    @DisplayName("Проверка авторасчета самовывоза для оффера c граничным весом среди нескольких рулов")
    @DbUnitDataSet(before = "classpath:idxreport/getShopOffersDeliveryInfoWhiteDeliveryPickupManyRulesTest.before.csv")
    void getShopOffersDeliveryInfo_WhiteDeliveryPickup_ManyRulesTest() throws Exception {
        checkWhitePickupResponse("white_delivery_pickup.json", createOfferWithCustomWeight(3.));
    }

    private void checkWhitePickupResponse(String responseFile) throws Exception {
        checkWhiteResponse(responseFile, createOfferWithCustomWeight(5.), true);
    }

    private void checkWhitePickupResponse(String responseFile, DeliveryCalcProtos.Offer.Builder offer) throws Exception {
        checkWhiteResponse(responseFile, offer, true);
    }

    private void checkWhiteCourierResponse(String responseFile, DeliveryCalcProtos.Offer.Builder offer) throws Exception {
        checkWhiteResponse(responseFile, offer, false);
    }

    private void checkWhiteResponse(String responseFile, DeliveryCalcProtos.Offer.Builder offer, boolean pickup) throws Exception {
        getShopOffersDeliveryInfoRequest(
                createShopOffersPbSnRequest(
                        DeliveryCalcProtos.ShopOffersReq.newBuilder()
                                .setGenerationId(6L)
                                .setShopId(111)
                                .setFeedId(222)
                                .addAllOffers(
                                        Collections.singletonList(
                                                offer.setPickup(pickup).build()
                                        )
                                )
                                .build()
                ))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(content().json(
                        StringTestUtil.getString(
                                IdxReportControllerFunctionalTest.class,
                                "/idxreport/response/" + responseFile
                        )
                ));
    }

    private DeliveryCalcProtos.Offer.Builder createOfferWithCustomWeight(double weight) {
        return createOfferWithoutDimensions(weight)
                .setWidth(40.)
                .setLength(40.)
                .setHeight(40.);
    }

    private DeliveryCalcProtos.Offer.Builder createOfferWithoutDimensions(double weight) {
        return DeliveryCalcProtos.Offer.newBuilder()
                .setOfferId("Offer name")
                .setWeight(weight)
                .addProgramType(DeliveryCalcProtos.ProgramType.REGULAR_PROGRAM)
                .addPriceMap(
                        DeliveryCalcProtos.OfferPrice.newBuilder()
                                .setCurrency("RUR")
                                .setValue(100.)
                                .build()
                );
    }

    private byte[] createShopOffersPbSnRequest(DeliveryCalcProtos.ShopOffersReq request) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PbSnUtils.writePbSnMessage("DCSR", request, outputStream);
        return outputStream.toByteArray();
    }

    private String shopDeliveryMetaResponse(Long generationId, String... currencies) {
        return shopDeliveryMetaResponse(generationId, false, currencies);
    }

    private String shopDeliveryMetaResponse(Long generationId, boolean useYmlDelivery, String... currencies) {
        return "{" +
                "\"generationId\":" + generationId + "," +
                "\"currencies\":[" + Arrays.stream(currencies).collect(Collectors.joining("\",\"", "\"", "\"")) + "]," +
                "\"useYmlDelivery\":" + useYmlDelivery +
                "}";
    }

    private String shopDeliveryMetaResponse(
            Long generationId,
            double weight, int length, int width, int height,
            String... currencies
    ) {
        return "{" +
                "\"generationId\":" + generationId + "," +
                "\"shopOffersAverageWeightDimensions\":" +
                "{\"length\":" + length + "," +
                "\"width\":" + width + "," +
                "\"height\":" + height + "," +
                "\"weight\":" + weight + "}," +
                "\"currencies\":[" + Arrays.stream(currencies).collect(Collectors.joining("\",\"", "\"", "\""))+ "]," +
                "\"useYmlDelivery\":false" +
                "}";
    }

    private String shopDeliveryMetaErrorResponse(long shopId) {
        return "{\"message\":\"No one shop-related generation was found for shopId=" + shopId + "\"}";
    }

    private ResultActions getShopDeliveryMetaRequest(long shopId, List<String> programs) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/shopDeliveryMeta", shopId, programs)
                .param("shopId", String.valueOf(shopId));
        if (programs != null) {
            requestBuilder.param("program", programs.toArray(new String[0]));
        }
        return mockMvc.perform(requestBuilder);
    }

    private ResultActions getShopOffersDeliveryInfoRequest(byte[] body) throws Exception {
        return mockMvc.perform(post("/shopOffers")
                .accept(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void checkRegularTariffsResponse(long shopId, long feedId, long generationId, String expectedFile) throws Exception {
        getShopOffersDeliveryInfoRequest(
                createShopOffersPbSnRequest(
                        DeliveryCalcProtos.ShopOffersReq.newBuilder()
                                .setGenerationId(generationId)
                                .setShopId(shopId)
                                .setFeedId(feedId)
                                .addAllOffers(
                                        Arrays.asList(
                                                createOfferWithCustomWeight(5.)
                                                        .addProgramType(DeliveryCalcProtos.ProgramType.REGULAR_PROGRAM)
                                                        .setPickup(true)
                                                        .addPriceMap(
                                                                DeliveryCalcProtos.OfferPrice.newBuilder()
                                                                        .setCurrency("RUR")
                                                                        .setValue(500.)
                                                                        .build()
                                                        )
                                                        .build(),
                                                createOfferWithCustomWeight(100.)
                                                        .addProgramType(DeliveryCalcProtos.ProgramType.REGULAR_PROGRAM)
                                                        .setPickup(true)
                                                        .addPriceMap(
                                                                DeliveryCalcProtos.OfferPrice.newBuilder()
                                                                        .setCurrency("RUR")
                                                                        .setValue(500.)
                                                                        .build()
                                                        )
                                                        .build()
                                        )
                                )
                                .build()
                ))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        StringTestUtil.getString(
                                IdxReportControllerFunctionalTest.class,
                                "/idxreport/response/" + expectedFile
                        )
                ));
    }

    private DeliveryCalcProtos.Offer.Builder setOfferPrice(DeliveryCalcProtos.Offer.Builder offer, double price) {
        return offer.clearPriceMap()
                .addPriceMap(DeliveryCalcProtos.OfferPrice.newBuilder()
                        .setCurrency("RUR")
                        .setValue(price)
                        .build()
                );
    }
}
