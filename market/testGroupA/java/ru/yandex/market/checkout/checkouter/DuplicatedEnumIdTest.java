package ru.yandex.market.checkout.checkouter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import ru.yandex.market.checkout.common.db.HasIntId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static ru.yandex.market.checkout.checkouter.order.OrderSubstatus.DELIVERY_SERIVCE_UNDELIVERED;

public class DuplicatedEnumIdTest {

    // список исключений, допускающий дубликаты.
    private List<Object> excludes = List.of(DELIVERY_SERIVCE_UNDELIVERED);

    @Test
    public void checkDuplicatedEnumId() {
        Reflections reflections = new Reflections(
                "ru.yandex.market.checkout.checkouter",
                new SubTypesScanner()
        );

        Set<Class<? extends HasIntId>> subTypesOf = reflections.getSubTypesOf(HasIntId.class);

        for (Class<? extends HasIntId> enumClass : subTypesOf) {
            HasIntId[] enumConstants = enumClass.getEnumConstants();
            Map<Integer, Long> countById = Arrays.stream(enumConstants)
                    .filter(excludes::contains)
                    .collect(Collectors.groupingBy(HasIntId::getId, Collectors.counting()));

            Optional<Map.Entry<Integer, Long>> dublicatedId = countById.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() > 1)
                    .findFirst();

            assertFalse(dublicatedId.isPresent(), enumClass.getName() + " contains duplicated id");
        }
    }
}
