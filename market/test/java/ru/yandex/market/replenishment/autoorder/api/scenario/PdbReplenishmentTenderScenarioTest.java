package ru.yandex.market.replenishment.autoorder.api.scenario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mboc.http.DeliveryParams;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.replenishment.autoorder.api.dto.EditItemsCountInTenderResultDTO;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.config.ExcelTestingHelper;
import ru.yandex.market.replenishment.autoorder.dto.AxCreatePurchasePriceDto;
import ru.yandex.market.replenishment.autoorder.model.AxHandlerResponse;
import ru.yandex.market.replenishment.autoorder.model.ShopSkuKeyWithStatus;
import ru.yandex.market.replenishment.autoorder.model.SskuStatus;
import ru.yandex.market.replenishment.autoorder.model.TenderStatus;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationDTO;
import ru.yandex.market.replenishment.autoorder.model.dto.AdjustedRecommendationsDTO;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;
import ru.yandex.market.replenishment.autoorder.service.PdbReplenishmentService;
import ru.yandex.market.replenishment.autoorder.service.client.DeepMindClient;
import ru.yandex.market.replenishment.autoorder.service.tender.TenderService;
import ru.yandex.market.replenishment.autoorder.utils.TestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.replenishment.autoorder.utils.AxHandlerResponseUtils.getMockedResponseEntity;
import static ru.yandex.market.replenishment.autoorder.utils.TestUtils.dtoToString;

@DbUnitDataBaseConfig({
    @DbUnitDataBaseConfig.Entry(
        name = "tableType",
        value = "TABLE,MATERIALIZED VIEW")
})
@WithMockLogin
public class PdbReplenishmentTenderScenarioTest extends ControllerTest {

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    PdbReplenishmentService pdbReplenishmentService;

    @Autowired
    TenderService tenderService;

    @Autowired
    @Qualifier("axRestTemplate")
    private RestTemplate axRestTemplate;

    @Value("${ax.handlers}")
    private String axaptaServerUrl;

    private static final long DEMAND_ID = 11;
    private static final int DEMAND_VERSION = 1;

    final ExcelTestingHelper excelTestingHelper = new ExcelTestingHelper(this);
    final DeepMindClient deepMindClient = Mockito.mock(DeepMindClient.class);

    @Before
    public void prepareMocks() throws Exception {
        LocalDateTime now = LocalDateTime.of(2021, 8, 10, 18, 0, 0);

        var sskuStatuses = List.of(
            new ShopSkuKeyWithStatus(10L, "100", SskuStatus.ACTIVE),
            new ShopSkuKeyWithStatus(20L, "100", SskuStatus.ACTIVE),
            new ShopSkuKeyWithStatus(20L, "200", SskuStatus.ACTIVE)
        );

        setTestTime(now);

        when(deepMindClient.getSskusStatuses(any())).thenReturn(sskuStatuses);
        when(deepMindClient.updateSskusStatuses(any())).thenReturn(true);

        ReflectionTestUtils.setField(tenderService, "deepMindClient", deepMindClient);

        var mockParams = Mockito.mock(DeliveryParams.class);
        var builder = MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
            .addFulfilmentInfo(
                MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setAvailability(SupplierOffer.Availability.ACTIVE)
                    .setShopSku("100")
                    .setSupplierId(10)
                    .build()
            )
            .addFulfilmentInfo(
                MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setAvailability(SupplierOffer.Availability.ACTIVE)
                    .setShopSku("100")
                    .setSupplierId(20)
                    .build()
            ).addFulfilmentInfo(
                MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                    .setAvailability(SupplierOffer.Availability.ACTIVE)
                    .setShopSku("200")
                    .setSupplierId(20)
                    .build()
            );
        when(mockParams.searchFulfillmentSskuParamsForInterval(any())).thenReturn(builder.build());
        ReflectionTestUtils.setField(tenderService, "deliveryParams", mockParams);
        mockAxRestTemplateForCreatePurchasePrice(Set.of(
            Pair.of("5", "0001"),
            Pair.of("6", "0002")
        ));
    }

    /*
    Редактировать рекомендации -> Начать тендер -> Импорт ответов поставщиков -> Предложения собраны -> Зафиксировать
     победителей -> Завершить тендер / Отправить выбраннные -> Отправить в AX
     */
    @Test
    @DbUnitDataSet(before = "PdbReplenishmentTenderScenarioTest_testScenario1.before.csv",
        after = "PdbReplenishmentTenderScenarioTest_testScenario1.after.csv")
    @DbUnitDataSet(
        dataSource = "pdbDataSource",
        after = "PdbReplenishmentTenderScenarioTest_testScenario1.pdb.after.csv")
    public void testScenario1() throws Exception {
        //редактируем рекомендации
        {
            AdjustedRecommendationsDTO adjustedRecommendationsDTO = new AdjustedRecommendationsDTO();
            adjustedRecommendationsDTO.setAdjustedRecommendations(
                List.of(AdjustedRecommendationDTO.builder().id(1001L).adjustedPurchQty(30).correctionReason(1L).build()));
            adjustedRecommendationsDTO.setDemandId(DEMAND_ID);
            adjustedRecommendationsDTO.setDemandVersion(DEMAND_VERSION);

            mockMvc.perform(put("/api/v1/recommendations/adjust?demandType=TENDER")
                    .contentType(APPLICATION_JSON_UTF8)
                    .content(dtoToString(adjustedRecommendationsDTO)))
                .andExpect(status().isOk());
        }

        //начинаем тендер
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/status")
                .contentType(APPLICATION_JSON_UTF8)
                .content(String.format("{\"status\":\"%s\"}", TenderStatus.STARTED)))
            .andExpect(status().isOk());

        //импортируем ответы поставщика 1
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/" + DEMAND_ID + "/10/excel",
                "PdbReplenishmentTenderScenarioTest_testScenario_importSupplierResponses.xlsx")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("true"));


        //импортируем ответы поставщика 2
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/" + DEMAND_ID + "/20/excel",
                "PdbReplenishmentTenderScenarioTest_testScenario_importSupplier2Responses.xlsx")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("true"));

        //Предложения собраны
        {
            //Выявление победителя
            mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/calculate")
                    .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/status")
                    .contentType(APPLICATION_JSON_UTF8)
                    .content(String.format("{\"status\":\"%s\"}", TenderStatus.OFFERS_COLLECTED)))
                .andExpect(status().isOk());
        }

        //Зафиксируем победителей
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/status")
                .contentType(APPLICATION_JSON_UTF8)
                .content(String.format("{\"status\":\"%s\"}", TenderStatus.WINNER_FIXED)))
            .andExpect(status().isOk());

        //Завершаем тендер
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/export")
                .content("[10, 20]")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        //Отправляем в AX
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@org.jetbrains.annotations.NotNull TransactionStatus status) {
                pdbReplenishmentService.exportToPdb();
            }
        });

    }

    /*
    Редактировать рекомендации -> Начать тендер -> Импорт ответов поставщиков -> Предложения собраны -> Зафиксировать
     победителей -> Редактировать результаты тендера -> Завершить тендер / Отправить выбраннные -> Отправить в AX
     */
    @Test
    @DbUnitDataSet(before = "PdbReplenishmentTenderScenarioTest_testScenario1.before.csv",
        after = "PdbReplenishmentTenderScenarioTest_testScenario2.after.csv")
    @DbUnitDataSet(
        dataSource = "pdbDataSource",
        after = "PdbReplenishmentTenderScenarioTest_testScenario2.pdb.after.csv")
    public void testScenario2() throws Exception {
        //редактируем рекомендации
        {
            AdjustedRecommendationsDTO adjustedRecommendationsDTO = new AdjustedRecommendationsDTO();
            adjustedRecommendationsDTO.setAdjustedRecommendations(
                List.of(AdjustedRecommendationDTO.builder().id(1001L).adjustedPurchQty(30).correctionReason(1L).build()));
            adjustedRecommendationsDTO.setDemandId(DEMAND_ID);
            adjustedRecommendationsDTO.setDemandVersion(DEMAND_VERSION);

            mockMvc.perform(put("/api/v1/recommendations/adjust?demandType=TENDER")
                    .contentType(APPLICATION_JSON_UTF8)
                    .content(dtoToString(adjustedRecommendationsDTO)))
                .andExpect(status().isOk());
        }

        //начинаем тендер
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/status")
                .contentType(APPLICATION_JSON_UTF8)
                .content(String.format("{\"status\":\"%s\"}", TenderStatus.STARTED)))
            .andExpect(status().isOk());


        //импортируем ответы поставщика 1
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/" + DEMAND_ID + "/10/excel",
                "PdbReplenishmentTenderScenarioTest_testScenario_importSupplierResponses.xlsx")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("true"));


        //импортируем ответы поставщика 2
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/" + DEMAND_ID + "/20/excel",
                "PdbReplenishmentTenderScenarioTest_testScenario_importSupplier2Responses.xlsx")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("true"));


        //Предложения собраны
        {
            //Выявление победителя
            mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/calculate")
                    .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/status")
                    .contentType(APPLICATION_JSON_UTF8)
                    .content(String.format("{\"status\":\"%s\"}", TenderStatus.OFFERS_COLLECTED)))
                .andExpect(status().isOk());
        }

        //Зафиксируем победителей
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/status")
                .contentType(APPLICATION_JSON_UTF8)
                .content(String.format("{\"status\":\"%s\"}", TenderStatus.WINNER_FIXED)))
            .andExpect(status().isOk());

        //Редактируем результаты тендера
        String testJson = TestUtils.dtoToString(new EditItemsCountInTenderResultDTO("0002.100", 0L));
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/result/edit")
                .content(testJson)
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        //Завершаем тендер
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/export")
                .content("[10, 20]")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        //Отправляем в AX
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@org.jetbrains.annotations.NotNull TransactionStatus status) {
                pdbReplenishmentService.exportToPdb();
            }
        });

    }

    /*
   Редактировать рекомендации -> Начать тендер -> Импорт ответов поставщиков -> Предложения собраны -> Зафиксировать
     победителей -> Отправить выбраннные 1 -> Отправить выбраннные 2 -> Отправить в AX
    */
    @Test
    @DbUnitDataSet(before = "PdbReplenishmentTenderScenarioTest_testScenario1.before.csv",
        after = "PdbReplenishmentTenderScenarioTest_testScenario3.after.csv")
    @DbUnitDataSet(
        dataSource = "pdbDataSource",
        after = "PdbReplenishmentTenderScenarioTest_testScenario3.pdb.after.csv")
    public void testScenario3() throws Exception {
        //редактируем рекомендации
        {
            AdjustedRecommendationsDTO adjustedRecommendationsDTO = new AdjustedRecommendationsDTO();
            adjustedRecommendationsDTO.setAdjustedRecommendations(
                List.of(AdjustedRecommendationDTO.builder().id(1001L).adjustedPurchQty(30).correctionReason(1L).build()));
            adjustedRecommendationsDTO.setDemandId(DEMAND_ID);
            adjustedRecommendationsDTO.setDemandVersion(DEMAND_VERSION);

            mockMvc.perform(put("/api/v1/recommendations/adjust?demandType=TENDER")
                    .contentType(APPLICATION_JSON_UTF8)
                    .content(dtoToString(adjustedRecommendationsDTO)))
                .andExpect(status().isOk());
        }

        //начинаем тендер
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/status")
                .contentType(APPLICATION_JSON_UTF8)
                .content(String.format("{\"status\":\"%s\"}", TenderStatus.STARTED)))
            .andExpect(status().isOk());

        //импортируем ответы поставщика 1
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/" + DEMAND_ID + "/10/excel",
                "PdbReplenishmentTenderScenarioTest_testScenario_importSupplierResponses.xlsx")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("true"));


        //импортируем ответы поставщика 2
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/" + DEMAND_ID + "/20/excel",
                "PdbReplenishmentTenderScenarioTest_testScenario_importSupplier2Responses.xlsx")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("true"));


        //Предложения собраны
        {
            //Выявление победителя
            mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/calculate")
                    .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/status")
                    .contentType(APPLICATION_JSON_UTF8)
                    .content(String.format("{\"status\":\"%s\"}", TenderStatus.OFFERS_COLLECTED)))
                .andExpect(status().isOk());
        }

        //Зафиксируем победителей
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/status")
                .contentType(APPLICATION_JSON_UTF8)
                .content(String.format("{\"status\":\"%s\"}", TenderStatus.WINNER_FIXED)))
            .andExpect(status().isOk());


        //Отправляем поставщика 1
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/export")
                .content("[10]")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        //Отправляем поставщика 2
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/export")
                .content("[20]")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        //Отправляем в AX
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@org.jetbrains.annotations.NotNull TransactionStatus status) {
                pdbReplenishmentService.exportToPdb();
            }
        });
    }

    /*
   Редактировать рекомендации -> Начать тендер -> Импорт ответов поставщиков -> Предложения собраны -> Зафиксировать
     победителей -> Отправить выбраннные 1 -> Отправить в AX
    */
    @Test
    @DbUnitDataSet(before = "PdbReplenishmentTenderScenarioTest_testScenario1.before.csv",
        after = "PdbReplenishmentTenderScenarioTest_testScenario4.after.csv")
    @DbUnitDataSet(
        dataSource = "pdbDataSource",
        after = "PdbReplenishmentTenderScenarioTest_testScenario4.pdb.after.csv")
    public void testScenario4() throws Exception {
        //редактируем рекомендации
        {
            AdjustedRecommendationsDTO adjustedRecommendationsDTO = new AdjustedRecommendationsDTO();
            adjustedRecommendationsDTO.setAdjustedRecommendations(
                List.of(AdjustedRecommendationDTO.builder().id(1001L).adjustedPurchQty(30).correctionReason(1L).build()));
            adjustedRecommendationsDTO.setDemandId(DEMAND_ID);
            adjustedRecommendationsDTO.setDemandVersion(DEMAND_VERSION);

            mockMvc.perform(put("/api/v1/recommendations/adjust?demandType=TENDER")
                    .contentType(APPLICATION_JSON_UTF8)
                    .content(dtoToString(adjustedRecommendationsDTO)))
                .andExpect(status().isOk());
        }

        //начинаем тендер
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/status")
                .contentType(APPLICATION_JSON_UTF8)
                .content(String.format("{\"status\":\"%s\"}", TenderStatus.STARTED)))
            .andExpect(status().isOk());

        //импортируем ответы поставщика 1
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/" + DEMAND_ID + "/10/excel",
                "PdbReplenishmentTenderScenarioTest_testScenario_importSupplierResponses.xlsx")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("true"));

        //импортируем ответы поставщика 2
        excelTestingHelper.upload(
                "POST",
                "/api/v1/tender/" + DEMAND_ID + "/20/excel",
                "PdbReplenishmentTenderScenarioTest_testScenario_importSupplier2Responses.xlsx")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("true"));

        //Предложения собраны
        {
            mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/status")
                    .contentType(APPLICATION_JSON_UTF8)
                    .content(String.format("{\"status\":\"%s\"}", TenderStatus.OFFERS_COLLECTED)))
                .andExpect(status().isOk());

            //Выявление победителя
            mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/calculate")
                    .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
        }

        //Зафиксируем победителей
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/status")
                .contentType(APPLICATION_JSON_UTF8)
                .content(String.format("{\"status\":\"%s\"}", TenderStatus.WINNER_FIXED)))
            .andExpect(status().isOk());

        //Отправляем поставщика 1
        mockMvc.perform(post("/api/v1/tender/" + DEMAND_ID + "/export")
                .content("[10]")
                .contentType(APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        //Отправляем в AX
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@org.jetbrains.annotations.NotNull TransactionStatus status) {
                pdbReplenishmentService.exportToPdb();
            }
        });

    }
    //endregion

    private void mockAxRestTemplateForCreatePurchasePrice(@NotNull Set<Pair<String, String>> responseIdAndRsId) {
        final String createPurchPriceUrl = axaptaServerUrl + "create-purch-price";
        responseIdAndRsId.forEach(pair -> {
                String responseId = pair.first;
                String rsId = pair.second;
                if (rsId != null && responseId != null) {
                    Mockito.doReturn(getMockedResponseEntity(responseId))
                        .when(axRestTemplate).exchange(
                            eq(createPurchPriceUrl),
                            eq(HttpMethod.POST),
                            ArgumentMatchers.argThat((HttpEntity<AxCreatePurchasePriceDto> argument) ->
                                argument.getBody() != null
                                    && rsId.equals(argument.getBody().getRsId())),
                            eq(AxHandlerResponse.class)
                        );
                }
            }
        );
    }
}
