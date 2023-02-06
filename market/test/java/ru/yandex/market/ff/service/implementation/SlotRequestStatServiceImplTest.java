package ru.yandex.market.ff.service.implementation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.SlotRequestStatService;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsForDayResponse;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.TimeSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseFreeSlotsResponse;

@ParametersAreNonnullByDefault
class SlotRequestStatServiceImplTest extends IntegrationTest {

    @Autowired
    SlotRequestStatService slotRequestStatService;

    @Test
    @ExpectedDatabase(
        value = "classpath:service/slot-request-stat-service/saveStat/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void saveStat() {
        ShopRequest request = new ShopRequest();
        request.setId(1000000L);
        FreeSlotsResponse freeSlots = new FreeSlotsResponse(
            List.of(
                new WarehouseFreeSlotsResponse(
                    300L,
                    List.of(
                        // MINIMAL_AVAILABLE_DATE
                        mockSlot(2022, 3, 23, 5, 30, 6, 30),
                        // MINIMAL_AVAILABLE_DAYLIGHT_DATE по нижней границе диапазона "daylight" (6-21)
                        mockSlot(2022, 3, 24, 6, 0, 7, 0)
                    )
                ),
                new WarehouseFreeSlotsResponse(
                    301L,
                    List.of(
                        // MINIMAL_AVAILABLE_DATE
                        mockSlot(2022, 3, 23, 20, 30, 21, 30),
                        // MINIMAL_AVAILABLE_DAYLIGHT_DATE по верхней границе диапазона "daylight" (6-21)
                        mockSlot(2022, 3, 24, 20, 0, 21, 0)
                    )
                )
            )
        );
        slotRequestStatService.saveStat(request, freeSlots);
    }

    @Nonnull
    private FreeSlotsForDayResponse mockSlot(
        int year, int month, int dayOfMonth,
        int fromHour, int fromMinute,
        int tillHour, int tillMinute
    ) {
        return new FreeSlotsForDayResponse(
            LocalDate.of(year, month, dayOfMonth),
            ZoneOffset.of("+5"),
            List.of(
                new TimeSlotResponse(LocalTime.of(fromHour, fromMinute), LocalTime.of(tillHour, tillMinute))
            )
        );
    }
}
