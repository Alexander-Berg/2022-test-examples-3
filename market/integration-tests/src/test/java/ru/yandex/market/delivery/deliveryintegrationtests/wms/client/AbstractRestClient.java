package ru.yandex.market.delivery.deliveryintegrationtests.wms.client;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import ru.yandex.market.logistic.api.model.common.request.AbstractRequest;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;

@Slf4j
public abstract class AbstractRestClient {

    protected XmlMapper mapper;

    public AbstractRestClient() {
        mapper = new XmlMapper();
        mapper.registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    protected String createReqBody(RequestWrapper<? extends AbstractRequest> req) {
        String body = "";

        try {
            body = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(req);
        } catch (JsonProcessingException e) {
            log.info("Ошибка при создании тела запроса: \n{}", e.getMessage());
            e.printStackTrace();
        }

        return body;
    }

    protected  <T> T createWrappedResponse(String res, TypeReference<T> typeReference) {
        T wrappedResponse = null;
        try {
            wrappedResponse = mapper.readValue(res, typeReference);
        } catch (IOException e) {
            log.info("Ошибка при парсинге ответа: \n{}", e.getMessage());
            e.printStackTrace();
        }

        return wrappedResponse;
    }
}
