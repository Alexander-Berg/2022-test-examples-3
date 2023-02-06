package ru.yandex.market.fulfillment.wrap.marschroute.model.response.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.List;
import java.util.Map;

class TransportResponseErrorResponseParsingTest extends ParsingTest<TransportResponse> {
    TransportResponseErrorResponseParsingTest() {
        super(new ObjectMapper(), TransportResponse.class, "transport/error_response.json");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.of(
            "success", false,
            "code", 999,
            "comment", "Ошибка сервиса");
    }
}
