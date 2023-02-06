package ru.yandex.market.mbi.api.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.outlet.Address;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.PhoneNumber;
import ru.yandex.market.core.outlet.Point;
import ru.yandex.market.core.outlet.ShopWarehouse;
import ru.yandex.market.core.outlet.db.DbShopWarehouseService;
import ru.yandex.market.core.outlet.db.ShopWarehouseTestUtils;
import ru.yandex.market.core.schedule.Schedule;
import ru.yandex.market.core.schedule.ScheduleLine;
import ru.yandex.market.mbi.api.client.entity.delivery.DeliveryWarehouseContainerDTO;
import ru.yandex.market.mbi.api.client.entity.delivery.DeliveryWarehouseDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.tags.Components;
import ru.yandex.market.tags.Tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author stani on 25.09.17.
 */
@Tags({
        @Tag(Components.MBI_API),
        @Tag(Tests.INTEGRATIONAL)
})
@DbUnitDataSet
class ShopWarehouseControllerTest extends FunctionalTest {

    public static final long SHOP_WAREHOUSE_ID = 1;
    private static final long DATASOURCE_ID = 10774L;

    @Autowired
    private DbShopWarehouseService dbShopWarehouseService;

    @Test
    void testGetWarehouse() {
        ShopWarehouse warehouse = dbShopWarehouseService.createShopWarehouse(
                ShopWarehouseTestUtils.getShopWarehouseForInsert(DATASOURCE_ID, SHOP_WAREHOUSE_ID)
        );
        assertNotNull(warehouse);

        ru.yandex.market.mbi.api.client.entity.outlets.ShopWarehouse warehouseApi
                = mbiApiClient.getShopWarehouse(warehouse.getId());
        compareWarehouses(warehouse, warehouseApi);
    }

    private void compareWarehouses(ShopWarehouse warehouse,
                                   ru.yandex.market.mbi.api.client.entity.outlets.ShopWarehouse warehouseApi) {

        assertEquals(warehouse.getId(), warehouseApi.getId());
        assertEquals(warehouse.getShopId(), warehouseApi.getShopId());
        assertEquals(warehouse.isForReturn(), warehouseApi.isForReturn());
        assertEquals(warehouse.isSupplierWarehouse(), warehouseApi.isSupplierWarehouse());
        comparePoints(warehouse.getPoint(), warehouseApi.getPoint());
    }

    private void comparePoints(Point point,
                               ru.yandex.market.mbi.api.client.entity.outlets.Point pointApi) {

        assertEquals(point.getContactName(), point.getContactName());
        assertEquals(point.getEmails().size(), pointApi.getEmails().size());
        assertTrue(point.getEmails().containsAll(pointApi.getEmails()));

        compareAddresses(point.getAddress(), pointApi.getAddress());
        compareGeoInfo(point.getGeoInfo(), pointApi.getGeoInfo());
        comparePhones(point.getPhones(), pointApi.getPhones());
        compareSchedules(point.getSchedule(), pointApi.getSchedule());
    }

    private void compareAddresses(Address address, ru.yandex.market.mbi.api.client.entity.outlets.Address addressApi) {

        assertEquals(address.getCity(), addressApi.getCity());
        assertEquals(address.getAddrAdditional(), addressApi.getAddrAdditional());
        assertEquals(address.getBlock(), addressApi.getBlock());
        assertEquals(address.getBuilding(), addressApi.getBuilding());
        assertEquals(address.getEstate(), addressApi.getEstate());
        assertEquals(address.getKm(), addressApi.getKm());
        assertEquals(address.getNumber(), addressApi.getNumber());
        assertEquals(address.getStreet(), addressApi.getStreet());
        assertEquals(address.getPostCode(), addressApi.getPostCode());
    }

    private void compareGeoInfo(GeoInfo geoInfo, ru.yandex.market.mbi.api.client.entity.outlets.GeoInfo geoInfoApi) {
        assertEquals(geoInfo.getRegionId(), geoInfoApi.getRegionId());
        assertEquals(geoInfo.getGpsCoords(), geoInfoApi.getGpsCoords());
    }

    private void comparePhones(List<PhoneNumber> phoneNumbers,
                               List<ru.yandex.market.mbi.api.client.entity.outlets.PhoneNumber> phoneNumbersApi) {

        assertEquals(phoneNumbers.size(), phoneNumbersApi.size());

        phoneNumbers = new ArrayList<>(phoneNumbers);
        phoneNumbersApi = new ArrayList<>(phoneNumbersApi);
        phoneNumbers.sort(Comparator.comparing(PhoneNumber::toString));
        phoneNumbersApi.sort(
                Comparator.comparing(ru.yandex.market.mbi.api.client.entity.outlets.PhoneNumber::toString));


        Iterator<PhoneNumber> phoneNumberIterator = phoneNumbers.iterator();
        Iterator<ru.yandex.market.mbi.api.client.entity.outlets.PhoneNumber> phoneNumberApiIterator =
                phoneNumbersApi.iterator();

        while (phoneNumberIterator.hasNext()) {
            PhoneNumber phoneNumber = phoneNumberIterator.next();
            ru.yandex.market.mbi.api.client.entity.outlets.PhoneNumber phoneNumberApi = phoneNumberApiIterator.next();
            assertEquals(phoneNumber.getCity(), phoneNumberApi.getCity());
            assertEquals(phoneNumber.getCountry(), phoneNumberApi.getCountry());
            assertEquals(phoneNumber.getNumber(), phoneNumberApi.getNumber());
            assertEquals(phoneNumber.getPhoneType(), phoneNumberApi.getPhoneType());
            assertEquals(phoneNumber.getPhoneTypeString(), phoneNumberApi.getPhoneTypeString());
            assertEquals(phoneNumber.getComments(), phoneNumberApi.getComments());
            assertEquals(phoneNumber.getExtension(), phoneNumberApi.getExtension());
        }

    }

    private void compareSchedules(Schedule schedule,
                                  ru.yandex.market.mbi.api.client.entity.outlets.Schedule scheduleApi) {
        assertEquals(schedule.getId(), scheduleApi.getId());

        Iterator<ScheduleLine> scheduleLinesIt = schedule.getLines().iterator();
        Iterator<ru.yandex.market.mbi.api.client.entity.outlets.ScheduleLine> scheduleApiLinesIt =
                scheduleApi.getLines().iterator();

        while (scheduleLinesIt.hasNext()) {
            ScheduleLine sl = scheduleLinesIt.next();
            ru.yandex.market.mbi.api.client.entity.outlets.ScheduleLine slApi = scheduleApiLinesIt.next();
            assertEquals(sl.getStartDay(), slApi.getStartDay());
            assertEquals(sl.startMinute(), slApi.getStartMinute());
            assertEquals(sl.days(), slApi.getDays());
            assertEquals(sl.minutes(), slApi.getMinutes());
        }
    }
}
