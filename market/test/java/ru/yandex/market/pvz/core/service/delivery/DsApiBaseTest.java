package ru.yandex.market.pvz.core.service.delivery;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import ru.yandex.market.logistic.api.model.common.request.AbstractRequest;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.response.AbstractResponse;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.tpl.common.util.StringFormatter;

import static org.assertj.core.api.Assertions.assertThat;

public class DsApiBaseTest {

    private static final ObjectMapper MAPPER = PvzDsApiProcessingConfiguration.DEFAULT_MAPPER;

    @SneakyThrows
    protected <T extends AbstractRequest> T readRequest(String fileName, Class<T> clazz, Map<String, Object> vars) {
        String rawInput = IOUtils.toString(this.getClass().getResourceAsStream(fileName), StandardCharsets.UTF_8);

        RequestWrapper<T> requestWrapper = MAPPER.readValue(StringFormatter.formatVars(rawInput, vars),
                MAPPER.getTypeFactory().constructParametricType(RequestWrapper.class, clazz));

        assertThat(requestWrapper.getRequest()).isNotNull();
        return requestWrapper.getRequest();
    }

    @SneakyThrows
    protected <T extends AbstractResponse> T readResponse(String fileName, Class<T> clazz, Map<String, Object> vars) {
        String rawInput = IOUtils.toString(this.getClass().getResourceAsStream(fileName), StandardCharsets.UTF_8);

        ResponseWrapper<T> responseWrapper = MAPPER.readValue(StringFormatter.formatVars(rawInput, vars),
                MAPPER.getTypeFactory().constructParametricType(ResponseWrapper.class, clazz));

        assertThat(responseWrapper.getResponse()).isNotNull();
        return responseWrapper.getResponse();
    }
}
