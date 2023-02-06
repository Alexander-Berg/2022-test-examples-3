package ru.yandex.market.core.outlet.db;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.phone.PhoneType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.outlet.Address;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.OutletVisibility;
import ru.yandex.market.core.outlet.PhoneNumber;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleLine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DbUnitDataSet(before = "DbOutletInfoServiceTest.common.before.csv")
class DbOutletInfoServiceTest extends FunctionalTest {

    @Autowired
    private DbOutletInfoService outletInfoService;

    /**
     * Тест проверяет корректность удаления аутлета. В случае удаления должны удалиться все записи из таблиц,
     * завязанных на аутлет.
     */
    @Test
    @DbUnitDataSet(
            before = "DbOutletInfoServiceTest.before.csv",
            after = "DbOutletInfoServiceTest.after.csv"
    )
    void testDeleteOutletInfo() {
        outletInfoService.deleteOutletInfo(2L);
    }

    @Test
    @DisplayName("Проверка создания аутлета с одинаковыми datasource_id и shop_outlet_id")
    void testCreateSameOutletTwice() {
        OutletInfo outlet = getOutletInfo(1, 2, "8009009988");
        // первое создание аутлета - ок
        outletInfoService.createOutletInfo(100500, outlet);
        // повторное создание - ошибка
        assertThrows(IllegalArgumentException.class, () -> outletInfoService.createOutletInfo(2, outlet));
    }

    @Test
    @DisplayName("Проверка создания аутлета с одинаковыми datasource_id и shop_outlet_id после удаления предыдущего")
    void testCreateSameOutletAfterFirstDeleted() {
        OutletInfo outlet = getOutletInfo(1, 2, "8009009988");
        // первое создание аутлета - ок
        long outletId = outletInfoService.createOutletInfo(100500, outlet);
        // удаляем аутлет
        outletInfoService.markOutletAsDeleted(102, outletId);
        // повторное создание - ок
        long outletId2 = outletInfoService.createOutletInfo(103, outlet);
        assertThat(outletId, is(not(outletId2)));
    }

    @Test
    @DisplayName("После удаления аутлета, в entity_history должна появиться запись")
    @DbUnitDataSet(after = "DbOutletInfoServiceTest.testCheckEntityHistoryAfterOutletDeleted.after.csv")
    void testCheckEntityHistoryAfterOutletDeleted() {
        OutletInfo outlet = getOutletInfo(1, 2, "8009009988");
        long outletId = outletInfoService.createOutletInfo(100500, outlet);
        outletInfoService.markOutletAsDeleted(102, outletId);
    }

    @Test
    @DisplayName("Повторное удаление аутлета должно приводить к ошибке")
    void testCheckDeleteSameOutletTwice() {
        OutletInfo outlet = getOutletInfo(1, 2, "8009009988");
        long outletId = outletInfoService.createOutletInfo(100500, outlet);
        // первое удаление - ок
        outletInfoService.markOutletAsDeleted(102, outletId);
        // повторное удаление - ошибка
        assertThrows(IllegalArgumentException.class, () -> outletInfoService.markOutletAsDeleted(3, outletId));
    }

    private static OutletInfo getOutletInfo(long id, long datasourceId, String shopOutletId) {
        OutletInfo outlet = new OutletInfo(id, datasourceId, OutletType.MIXED, "some outlet", true, shopOutletId);
        outlet.setAddress(new Address.Builder().setCity("city").build());
        outlet.setEmails(List.of("test@test.ru"));
        outlet.setHidden(OutletVisibility.VISIBLE);

        PhoneNumber phone = PhoneNumber.builder()
                .setCountry("7")
                .setCity("495")
                .setNumber("345678")
                .setExtension("7")
                .setPhoneType(PhoneType.PHONE).build();
        outlet.addPhone(phone);

        ScheduleLine scheduleLine = new ScheduleLine(
                ScheduleLine.DayOfWeek.MONDAY,
                5, 0,
                (int) Duration.ofHours(8).toMinutes()
        );
        Schedule schedule = new Schedule(1, List.of(scheduleLine));
        outlet.setSchedule(schedule);

        outlet.setGeoInfo(new GeoInfo(new Coordinates(50, 50), 1L));

        return outlet;
    }
}
