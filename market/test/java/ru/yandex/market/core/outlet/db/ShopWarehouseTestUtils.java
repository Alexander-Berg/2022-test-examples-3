package ru.yandex.market.core.outlet.db;

import javax.annotation.Nonnull;

import ru.yandex.common.util.phone.PhoneType;
import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.outlet.Address;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.PhoneNumber;
import ru.yandex.market.core.outlet.Point;
import ru.yandex.market.core.outlet.ShopWarehouse;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleLine;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class ShopWarehouseTestUtils {

    @Nonnull
    public static ShopWarehouse getShopWarehouseForInsert(long datasourceId, long shopWarehouseId) {
        return new ShopWarehouse(
                null,
                datasourceId,
                new Point.Builder()
                        .setAddress(new Address.Builder()
                                .setCity("city1")
                                .setBlock("block1")
                                .setBuilding("building1")
                                .setEstate("estate1")
                                .setLaneKM(1234)
                                .setNumber("number1")
                                .setOther("other1")
                                .setStreet("street1")
                                .setPostCode("post_code1")
                                .setFlat("flat1")
                                .build())
                        .setGeoInfo(new GeoInfo(Coordinates.valueOf("1234,4321"), 213L))
                        .setEmails(asList("email@email.ru", "email1@email.ru"))
                        .setPhones(singletonList(
                                PhoneNumber.builder()
                                        .setNumber("01")
                                        .setPhoneType(PhoneType.PHONE)
                                        .setComments("phone_comments_1")
                                        .setExtension("1")
                                        .setCountry("7")
                                        .setCity("495")
                                        .build()))
                        .setSchedule(new Schedule(shopWarehouseId,
                                asList(
                                        new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 1, 11, 111),
                                        new ScheduleLine(ScheduleLine.DayOfWeek.WEDNESDAY, 2, 22, 222))
                        ))
                        .setContactName("contact name")
                        .build(),
                false,
                true);
    }

}
