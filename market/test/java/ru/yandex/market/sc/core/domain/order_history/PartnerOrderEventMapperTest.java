package ru.yandex.market.sc.core.domain.order_history;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.order.model.ConvertibleToHistoryEvent;
import ru.yandex.market.sc.core.domain.order_history.model.HistoryEvent;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class PartnerOrderEventMapperTest {

    @Autowired
    private final PartnerOrderEventMapper mapper = new PartnerOrderEventMapper();

    @Test
    void convertPlaceStatusToHistoryEvent() {
        Arrays.stream(PlaceStatus.values()).forEach(
                placeStatus -> assertDoesNotThrow(() -> mapper.convertPlaceStatusToHistoryEvent(placeStatus))
        );
    }

    @Test
    void convertEventToConvertibleToHistoryEvent() {
        Reflections reflections = new Reflections("ru.yandex.market.sc.core");
        Set<Class<? extends ConvertibleToHistoryEvent>> impls =
                reflections.getSubTypesOf(ConvertibleToHistoryEvent.class);
        Map<ConvertibleToHistoryEvent, HistoryEvent> historyEventMap =
                Arrays.stream(HistoryEvent.values())
                        .collect(Collectors.toMap(
                                mapper::convertEventToConvertibleToHistoryEvent,
                                Function.identity()
                        ));
        impls.stream().map(Class::getEnumConstants)
                .flatMap(Arrays::stream)
                .forEach(
                        convertibleToHistoryEvent -> {
                            log.info("Test status {}.{}", convertibleToHistoryEvent.getClass().getSimpleName() ,
                                    convertibleToHistoryEvent);
                            assertTrue(historyEventMap.containsKey(convertibleToHistoryEvent));
                        }
                );

    }

}
