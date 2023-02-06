package ru.yandex.market.deliverycalculator.searchengine.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.searchengine.FunctionalTest;
import ru.yandex.market.deliverycalculator.searchengine.JsonProtoHelper;
import ru.yandex.market.deliverycalculator.searchengine.service.FeedParserWorkflowService;

import static ru.yandex.market.deliverycalculator.searchengine.util.PrintProtoResultHandler.printProto;
import static ru.yandex.market.deliverycalculator.searchengine.util.ProtoResultMatcher.assertProtoAsJson;

@DbUnitDataSet(before = "db/MardoWhiteOptionsSearchTest.before.csv")
@DisplayName("Функциональный тест для проверки СиС на белом тарифами служб  из Я.До")
class MardoWhiteOptionsSearchTest extends FunctionalTest {

    @Autowired
    private FeedParserWorkflowService feedParserWorkflowService;

    @BeforeEach
    void setUp() {
        feedParserWorkflowService.updateActiveGenerationId();
        feedParserWorkflowService.importGenerations();
    }

    @Test
    @DisplayName("Флаг pickup выставлен в false - ожидаем, что доствки в ПВЗ служб нет")
    void offerPickupFlagSetToFalse() throws Exception {
        checkFeedOffers("request/offer-pickup-false.json", "response/expected-offer-pickup-false.json");
    }

    @Test
    @DisplayName("Флаг pickup не передается - ожидаем, что доствка в ПВЗ служб есть (поведение по умолчанию)")
    @Disabled
    void offerPickupFlagIsMissing() throws Exception {
        checkFeedOffers("request/offer-pickup-missing.json", "response/expected-offer-pickup-true.json");
    }

    @Test
    @DisplayName("Флаг pickup выставлен в true - ожидаем, что доствка в ПВЗ служб есть")
    void offerPickupFlagIsTrue() throws Exception {
        checkFeedOffers("request/offer-pickup-true.json", "response/expected-offer-pickup-true.json");
    }

    private void checkFeedOffers(String requestDataFile, String expectedDataFile) throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/shopOffers")
                        .content(JsonProtoHelper.getShopOffersReq(getClass(), requestDataFile)))
                .andDo(printProto())
                .andExpect(assertProtoAsJson(getClass(), expectedDataFile));
    }

}
