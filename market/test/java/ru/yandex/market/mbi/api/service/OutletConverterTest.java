package ru.yandex.market.mbi.api.service;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.phone.PhoneType;
import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.outlet.Address;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletLegalInfo;
import ru.yandex.market.core.outlet.OutletType;
import ru.yandex.market.core.outlet.PhoneNumber;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleLine;
import ru.yandex.market.mbi.api.client.entity.outlets.LegalInfo;
import ru.yandex.market.mbi.api.client.entity.outlets.Outlet;
import ru.yandex.market.mbi.api.client.entity.outlets.OutletConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class OutletConverterTest {
    private static final long OUTLET_ID = 1;
    private static final String OUTLET_NAME = "outlet";
    private static final String DELIVERY_SERVICE_OUTLET_ID = "3";
    private static final String DELIVERY_SERVICE_OUTLET_CODE = "33";
    private static final String GPS_COORDS = "1234.0,4321.0";
    private static final long GEO_REGION_ID = 213L;
    private static final String EMAIL1 = "email1";
    private static final String EMAIL2 = "email2";
    private static final ScheduleLine.DayOfWeek DAY_OF_WEEK1 = ScheduleLine.DayOfWeek.SATURDAY;
    private static final int DAYS1 = 1;
    private static final int START_MINUTE1 = 60;
    private static final int MINUTES1 = 120;
    private static final ScheduleLine.DayOfWeek DAY_OF_WEEK2 = ScheduleLine.DayOfWeek.MONDAY;
    private static final int DAYS2 = 2;
    private static final int START_MINUTE2 = 0;
    private static final int MINUTES2 = 60;
    private static final String PHONE_CITY1 = "495";
    private static final String PHONE_COUNTRY1 = "+7";
    private static final String PHONE_EXTENSION1 = "1234";
    private static final String PHONE_COMMENTS1 = "phone comments 1";
    private static final PhoneType PHONE_TYPE1 = PhoneType.PHONE;
    private static final String PHONE_CITY2 = "499";
    private static final String PHONE_COUNTRY2 = "+4";
    private static final String PHONE_EXTENSION2 = "4321";
    private static final String PHONE_COMMENTS2 = "phone comments 2";
    private static final PhoneType PHONE_TYPE2 = PhoneType.PHONE_FAX;
    private static final String CITY = "city";
    private static final String STREET = "street";
    private static final String NUMBER = "number";
    private static final String BLOCK = "block";
    private static final String BUILDING = "building";
    private static final String ESTATE = "estate";
    private static final int LANE_KM = 4;
    private static final String POST_CODE = "123456";
    private static final String OTHER = "other";
    private static final String PHONE_NUMBER1 = "223322223322";
    private static final String PHONE_NUMBER2 = "02";

    @Test
    public void convert() {
        Outlet outlet = OutletConverter.convert(getOutletInfo());
        assertEquals(getExpectedOutlet(), outlet);
    }

    private Outlet getExpectedOutlet() {
        return new Outlet(
                OUTLET_ID, OUTLET_NAME, DELIVERY_SERVICE_OUTLET_ID, DELIVERY_SERVICE_OUTLET_CODE,
                new ru.yandex.market.mbi.api.client.entity.outlets.Address(
                        CITY, STREET, NUMBER, BUILDING, ESTATE, BLOCK, LANE_KM, POST_CODE, OTHER),
                new ru.yandex.market.mbi.api.client.entity.outlets.GeoInfo(GPS_COORDS, GEO_REGION_ID),
                Arrays.asList(EMAIL1, EMAIL2),
                Arrays.asList(
                        new ru.yandex.market.mbi.api.client.entity.outlets.PhoneNumber(
                                PHONE_COUNTRY1, PHONE_CITY1, PHONE_NUMBER1,
                                PHONE_EXTENSION1, PHONE_COMMENTS1, PHONE_TYPE1),
                        new ru.yandex.market.mbi.api.client.entity.outlets.PhoneNumber(
                                PHONE_COUNTRY2, PHONE_CITY2, PHONE_NUMBER2,
                                PHONE_EXTENSION2, PHONE_COMMENTS2, PHONE_TYPE2)),
                Arrays.asList(
                        new ru.yandex.market.mbi.api.client.entity.outlets.ScheduleLine(DAY_OF_WEEK1, DAYS1, START_MINUTE1, MINUTES1),
                        new ru.yandex.market.mbi.api.client.entity.outlets.ScheduleLine(DAY_OF_WEEK2, DAYS2, START_MINUTE2, MINUTES2)),
                new LegalInfo(OrganizationType.OOO, "org name",
                        "123", "jur addr", "fact addr")
        );
    }

    private OutletInfo getOutletInfo() {
        OutletInfo outletInfo = new OutletInfo(OUTLET_ID, 2, OutletType.DEPOT, OUTLET_NAME, false, "1");
        outletInfo.setDeliveryServiceOutletId(DELIVERY_SERVICE_OUTLET_ID);
        outletInfo.setDeliveryServiceOutletCode(DELIVERY_SERVICE_OUTLET_CODE);
        outletInfo.setAddress(getAddress());
        outletInfo.setGeoInfo(new GeoInfo(Coordinates.valueOf(GPS_COORDS), GEO_REGION_ID));
        outletInfo.setPhones(Arrays.asList(getPhone1(), getPhone2()));
        outletInfo.setEmails(Arrays.asList(EMAIL1, EMAIL2));
        outletInfo.setSchedule(new Schedule(1, Arrays.asList(getScheduleLine1(), getScheduleLine2())));
        outletInfo.setLegalInfo(getOutletLegalInfo());
        return outletInfo;
    }

    private ScheduleLine getScheduleLine1() {
        return new ScheduleLine(DAY_OF_WEEK1, DAYS1, START_MINUTE1, MINUTES1);
    }

    private ScheduleLine getScheduleLine2() {
        return new ScheduleLine(DAY_OF_WEEK2, DAYS2, START_MINUTE2, MINUTES2);
    }

    private OutletLegalInfo getOutletLegalInfo() {
        return new OutletLegalInfo.Builder()
                .setOutletId(OUTLET_ID)
                .setOrganizationType(OrganizationType.OOO)
                .setOrganizationName("org name")
                .setRegistrationNumber("123")
                .setJuridicalAddress("jur addr")
                .setFactAddress("fact addr")
                .build();
    }

    private PhoneNumber getPhone1() {
        return PhoneNumber.builder()
                .setCity(PHONE_CITY1)
                .setCountry(PHONE_COUNTRY1)
                .setNumber(PHONE_NUMBER1)
                .setExtension(PHONE_EXTENSION1)
                .setComments(PHONE_COMMENTS1)
                .setPhoneType(PHONE_TYPE1)
                .build();
    }

    private PhoneNumber getPhone2() {
        return PhoneNumber.builder()
                .setCity(PHONE_CITY2)
                .setCountry(PHONE_COUNTRY2)
                .setNumber(PHONE_NUMBER2)
                .setExtension(PHONE_EXTENSION2)
                .setComments(PHONE_COMMENTS2)
                .setPhoneType(PHONE_TYPE2)
                .build();
    }

    private Address getAddress() {
        return new Address.Builder()
                .setCity(CITY)
                .setStreet(STREET)
                .setNumber(NUMBER)
                .setBlock(BLOCK)
                .setBuilding(BUILDING)
                .setEstate(ESTATE)
                .setLaneKM(LANE_KM)
                .setOther(OTHER)
                .setPostCode(POST_CODE)
                .build();
    }
}
