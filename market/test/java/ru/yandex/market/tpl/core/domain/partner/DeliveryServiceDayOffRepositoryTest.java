package ru.yandex.market.tpl.core.domain.partner;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.partner.DayOffTestDataFactory.DS_1_DAY_OFFS;
import static ru.yandex.market.tpl.core.domain.partner.DayOffTestDataFactory.DS_2_DAY_OFFS;
import static ru.yandex.market.tpl.core.domain.partner.DayOffTestDataFactory.DS_ID_1;
import static ru.yandex.market.tpl.core.domain.partner.DayOffTestDataFactory.DS_ID_2;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DeliveryServiceDayOffRepositoryTest {

    private final DayOffTestDataFactory dayOffTestDataFactory;
    private final DeliveryServiceDayOffRepository deliveryServiceDayOffRepository;

    @BeforeEach
    public void init() {
        dayOffTestDataFactory.createTestData();
    }

    @Test
    void testGetDayOffsByDeliveryServiceId() {
        var dayOffsDs1 = deliveryServiceDayOffRepository.findByDeliveryServiceId(DS_ID_1);
        assertThat(
                dayOffsDs1.stream()
                        .map(DeliveryServiceDayOff::getDate)
                        .collect(Collectors.toList())
        )
                .containsExactlyInAnyOrderElementsOf(DS_1_DAY_OFFS);

        var dayOffsDs2 = deliveryServiceDayOffRepository.findByDeliveryServiceId(DS_ID_2);
        assertThat(
                dayOffsDs2.stream()
                        .map(DeliveryServiceDayOff::getDate)
                        .collect(Collectors.toList())
        )
                .containsExactlyInAnyOrderElementsOf(DS_2_DAY_OFFS);
    }
}
