package ru.yandex.market.tpl.core.domain.partner;

import java.time.LocalDate;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DayOffTestDataFactory {
    public static final long DS_ID_1 = 1001;
    public static final long DS_ID_2 = 1002;

    public static final Set<LocalDate> DS_1_DAY_OFFS = Set.of(
            LocalDate.of(2020, 12, 20),
            LocalDate.of(2020, 12, 21),
            LocalDate.of(2020, 12, 22)
    );

    public static final Set<LocalDate> DS_2_DAY_OFFS = Set.of(
            LocalDate.of(2020, 12, 20),
            LocalDate.of(2020, 12, 21),
            LocalDate.of(2020, 12, 22),
            LocalDate.of(2020, 12, 23)
    );

    private final DeliveryServiceDayOffRepository deliveryServiceDayOffRepository;

    public void createTestData() {
        DS_1_DAY_OFFS.stream()
                .map(date -> new DeliveryServiceDayOff(DS_ID_1, date))
                .forEach(deliveryServiceDayOffRepository::save);
        DS_2_DAY_OFFS.stream()
                .map(date -> new DeliveryServiceDayOff(DS_ID_2, date))
                .forEach(deliveryServiceDayOffRepository::save);
    }
}
