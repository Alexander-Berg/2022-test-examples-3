package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.MarschrouteTestConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete.GetStocksBaseScenario;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete.GetStocksNegativeScenario;

@SpringBootTest(classes = {MarschrouteTestConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = "fulfillment.services.stock.failOnWrongItemId=true")
class WrongItemFailGetStocksFunctionalTest extends BaseGetStocksFunctionalTest<String> {

    @Override
    protected GetStocksBaseScenario getGetStocksScenario() {
        return new GetStocksNegativeScenario(restTemplate, createMarschrouteApiUrl());
    }
}
