package ru.yandex.market.abo.core.storage.json.express.moderation;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.express.moderation.ExpressModerationInfo;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PhoneType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author i-shunkevich
 * @date 28.04.2021
 */
class JsonExpressModerationInfoServiceTest extends EmptyTest {

    private static final long ticketId = 1L;

    @Autowired
    JsonExpressModerationInfoService jsonExpressModerationInfoService;

    @Test
    void serializationTest() {
        var phone1 = new Phone("84444444444", "2222", "Основной телефон", PhoneType.PRIMARY);
        var phone2 = new Phone("85555555555", null, null, PhoneType.PRIMARY);
        var phones = List.of(
                new ExpressModerationInfo.LogisticsPoint.Phone(phone1),
                new ExpressModerationInfo.LogisticsPoint.Phone(phone2)
        );

        var monday = new ScheduleDayResponse(-1L, 1, LocalTime.MIN, LocalTime.MAX, true);
        var tuesday = new ScheduleDayResponse(-2L, 1, LocalTime.NOON, LocalTime.MAX, true);
        var schedule = List.of(
                new ExpressModerationInfo.LogisticsPoint.ScheduleDay(monday),
                new ExpressModerationInfo.LogisticsPoint.ScheduleDay(tuesday)
        );

        Address address = Address.newBuilder()
                .country("Россия")
                .region("Москва")
                .subRegion("Москва")
                .settlement("Москва")
                .addressString("111111 Москва, Тверская, 1")
                .shortAddressString("Москва, Тверская, 1")
                .comment("Адрес")
                .street("Тверская")
                .house("1")
                .housing("1")
                .building("1")
                .apartment("1")
                .latitude(BigDecimal.valueOf(55.55))
                .longitude(BigDecimal.valueOf(55.55))
                .locationId(111)
                .exactLocationId(111)
                .postCode("111111")
                .build();

        var logisticsPoint = new ExpressModerationInfo.LogisticsPoint(address, phones, schedule);
        var logisticsPoints = List.of(logisticsPoint);
        var expressModerationInfo = new ExpressModerationInfo(logisticsPoints);
        jsonExpressModerationInfoService.save(ticketId, expressModerationInfo);
        flushAndClear();

        assertEquals(expressModerationInfo,
                jsonExpressModerationInfoService.getExpressModerationInfo(ticketId)
                        .map(JsonExpressModerationInfo::getStoredEntity)
                        .orElse(null)
        );
    }
}
