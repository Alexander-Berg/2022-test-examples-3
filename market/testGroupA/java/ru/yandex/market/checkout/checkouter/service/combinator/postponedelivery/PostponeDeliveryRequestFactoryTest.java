package ru.yandex.market.checkout.checkouter.service.combinator.postponedelivery;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataRetrieveResult;
import ru.yandex.market.checkout.checkouter.service.personal.PersonalDataService;
import ru.yandex.market.checkout.checkouter.service.personal.model.PersGps;
import ru.yandex.market.checkout.checkouter.storage.shipment.ParcelRecordMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

public class PostponeDeliveryRequestFactoryTest {

    @Test
    public void buildRequestTest() {
        final PersonalDataService personalDataService = Mockito.mock(PersonalDataService.class);

        PersGps persGps = new PersGps();
        persGps.setLongitude("37.90647953857668");
        persGps.setLatitude("55.70417927289191");
        Mockito.when(personalDataService.retrieve(any()))
                .thenReturn(new PersonalDataRetrieveResult(null, null, null, null, persGps));

        Mockito.when(personalDataService.getPersGps(any())).thenReturn(persGps);

        // Assign
        var order = new Order();
        order.setId(1234567L);
        Set<Integer> cargoTypes = new LinkedHashSet<>();
        cargoTypes.add(300);
        cargoTypes.add(310);
        order.setItems(List.of(
                createOrderItem(1L, 2, 10000L, 15L, 20L, 20L, null),
                createOrderItem(2L, 4, 25000L, 30L, 50L, 50L, cargoTypes)
        ));
        var delivery = new Delivery();
        delivery.setOutletId(10000123123L);
        delivery.setRegionId(213L);
        var address = new AddressImpl();
        address.setGps("37.90647953857668,55.70417927289191");
        address.setPreciseRegionId(120539L);
        delivery.setBuyerAddress(address);
        delivery.setDeliveryServiceId(322L);
        delivery.setDeliveryDates(new DeliveryDates(
                createDate(11, 11, 2021),
                createDate(13, 11, 2021),
                LocalTime.of(10, 0, 0),
                LocalTime.of(14, 0, 0)
        ));
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setFeatures(Set.of(DeliveryFeature.DEFERRED_COURIER));
        var parcel = new Parcel();
        var route = ParcelRecordMapper.parseRoute("{\"this\":\"is\",\"route\":111}");
        parcel.setRoute(route);
        delivery.addParcel(parcel);
        order.setDelivery(delivery);
        order.setProperty(OrderPropertyType.EXPERIMENTS, "use_some_feature=1;use_other_feature=0");

        var expectedRequest = PostponeDeliveryDummyUtils.getRequest();

        // Act
        var request = PostponeDeliveryRequestFactory.buildRequest(order, personalDataService::getPersGps);

        // Assert
        assertEquals(expectedRequest, request);
    }

    private static OrderItem createOrderItem(Long id, int count, long weight, long height, long width, long depth,
                                             Set<Integer> cargoTypes) {
        var item = new OrderItem();
        item.setOfferItemKey(new OfferItemKey(id.toString(), id, id.toString()));
        item.setCount(count);
        item.setWeight(weight);
        item.setHeight(height);
        item.setWidth(width);
        item.setDepth(depth);
        item.setCargoTypes(cargoTypes);
        return item;
    }

    private static Date createDate(int day, int month, int year) {
        return Date.from(LocalDate.of(year, month, day)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());
    }
}
