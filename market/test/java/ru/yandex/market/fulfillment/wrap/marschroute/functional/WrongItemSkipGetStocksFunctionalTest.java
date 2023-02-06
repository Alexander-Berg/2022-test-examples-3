package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.MarschrouteTestConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete.GetStocksBaseScenario;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete.GetStocksPositiveScenario;

@SpringBootTest(classes = {MarschrouteTestConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "fulfillment.services.stock.failOnWrongItemId=false")
class WrongItemSkipGetStocksFunctionalTest extends PositiveGetStocksFunctionalTestImpl {
    @Override
    protected GetStocksBaseScenario getGetStocksScenario() {
        return new GetStocksPositiveScenario("functional/get_stocks/wrong_item_id/response.json", restTemplate, createMarschrouteApiUrl());
    }
}
