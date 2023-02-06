package ru.yandex.market.logistic.api.model.common.request;

import ru.yandex.market.logistic.api.utils.ParsingTest;

class RequestStateParsingTest extends ParsingTest<RequestState> {

    RequestStateParsingTest() {
        super(RequestState.class, "fixture/request/request_state.xml");
    }
}
