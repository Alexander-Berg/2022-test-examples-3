package ru.yandex.market.fulfillment.wrap.marschroute.functional;

import ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete.GetStocksBaseScenario;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete.GetStocksPositiveScenario;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetStocksResponse;

class PositiveGetStocksFunctionalTestImpl extends BaseGetStocksFunctionalTest<GetStocksResponse> {

    @Override
    protected GetStocksBaseScenario getGetStocksScenario() {
        return new GetStocksPositiveScenario(restTemplate, createMarschrouteApiUrl());
    }
}
