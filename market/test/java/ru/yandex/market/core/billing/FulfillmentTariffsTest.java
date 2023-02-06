package ru.yandex.market.core.billing;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.Factory;
import org.apache.commons.collections4.map.LazyMap;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.common.util.csv.CSVReader;
import ru.yandex.market.core.date.Period;
import ru.yandex.market.core.util.DateTimes;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тест для фулфиллмент тарифов. Проверяет отсутствие пересечений в тарифах и корректность типов услуг и заказов.
 */
public class FulfillmentTariffsTest {

    private static final String NULL = "null";

    /**
     * Список разрешеных serviceType.
     */
    private final Set<String> ALLOWED_SERVICE_TYPES = new HashSet<>(Arrays.asList(
            "ff_processing", "ff_withdraw", "ff_storage_billing", "delivery_to_customer", "delivery_to_customer_return"
    ));

    /**
     * Список разрешенных orderType.
     */
    private final Set<String> ALLOWED_ORDER_TYPES = new HashSet<>(Arrays.asList(
            "fulfillment", "crossdock", "drop_ship"
    ));

    @Test
    void testVerifyFulfillmentTariffPeriods() throws IOException {
        final ClassPathResource classPathResource = new ClassPathResource(
                "production/market_billing/fulfillment_tariff/fulfillment_tariff_periods.csv"
        );

        Map<String, List<Period>> map = LazyMap.lazyMap(new HashMap<>(), (Factory<List<Period>>) ArrayList::new);

        try (CSVReader r = new CSVReader(classPathResource.getInputStream())) {
            r.readHeaders();
            while (r.readRecord()) {
                if (r.getFieldsCount() == 0 || r.getField(0).startsWith("#")) {
                    continue;
                }

                final String supplierId = r.getField(5);
                final String serviceType = r.getField(1);
                final String orderType = r.getField(4);
                final String dateTimeFrom = r.getField(2);
                final String dateTimeTo = r.getField(3);

                assertTrue(ALLOWED_SERVICE_TYPES.contains(serviceType), serviceType);
                assertTrue(ALLOWED_ORDER_TYPES.contains(orderType), orderType);

                final LocalDate from = LocalDate.from(DateTimeFormatter.ISO_DATE.parse(dateTimeFrom));
                final LocalDate to;
                if (NULL.equals(dateTimeTo)) {
                    to = LocalDate.MAX;
                } else {
                    to = LocalDate.from(DateTimeFormatter.ISO_DATE.parse(dateTimeTo));
                }

                final String key =
                        "SupplierId: " + supplierId + ", serviceType: " + serviceType + ", orderType: " + orderType;

                map.get(key).add(new Period(
                        DateTimes.toInstantAtDefaultTz(from.atStartOfDay()),
                        DateTimes.toInstantAtDefaultTz(to.atStartOfDay())
                ));
            }
        }

        map.forEach(this::check);
    }

    private void check(String key, List<Period> list) {
        list.sort(Comparator.comparing(Period::getFrom));

        for (int i = 0; i < list.size() - 1; i++) {
            final Period periodOne = list.get(i);
            final Period periodTwo = list.get(i + 1);
            if (periodOne.intersection(periodTwo)) {
                throw new IllegalStateException(
                        "There is an intersection in periods: key: " + key +
                                "; period 1: " + periodOne + "; period 2: " + periodTwo
                );
            }
        }
    }
}
