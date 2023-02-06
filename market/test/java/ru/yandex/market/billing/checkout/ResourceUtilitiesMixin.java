package ru.yandex.market.billing.checkout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;

/**
 * Чтение событий по заказам из json файла. Копия из проекта mbi.
 */
public interface ResourceUtilitiesMixin extends ResourceUtilities {
    default List<OrderHistoryEvent> events(String jsonPath) throws IOException {
        return new ArrayList<>(
                getObjectMapper().readValue(
                        getResourceAsInputStream(jsonPath),
                        OrderHistoryEvents.class
                ).getContent()
        );
    }

    ObjectMapper getObjectMapper();
}
