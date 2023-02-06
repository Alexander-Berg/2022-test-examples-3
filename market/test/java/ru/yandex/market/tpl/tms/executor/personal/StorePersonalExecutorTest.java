package ru.yandex.market.tpl.tms.executor.personal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalStoreApi;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.FullName;
import ru.yandex.market.tpl.common.personal.client.model.GpsCoord;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeStoreResponseItem;
import ru.yandex.market.tpl.common.personal.client.model.PersonalAddressKeys;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeStoreRequest;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeStoreResponse;
import ru.yandex.market.tpl.common.personal.client.tpl.PersonalExternalService;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderDeliveryRepository;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

@RequiredArgsConstructor
class StorePersonalExecutorTest extends TplTmsAbstractTest {

    private final OrderGenerateService orderGenerateService;
    private final OrderRepository orderRepository;
    private final DefaultPersonalStoreApi personalStoreApi;
    private final OrderDeliveryRepository orderDeliveryRepository;
    private final PersonalExternalService personalExternalService;
    private final Clock clock;
    private final TestUserHelper testUserHelper;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private StorePersonalExecutor executor;

    private Order order1;
    private Order order2;
    private Order order3;

    @BeforeEach
    void init() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.STORE_PERSONAL_JOB_ENABLED, true);
        executor = new StorePersonalExecutor(
                orderDeliveryRepository,
                personalExternalService,
                transactionTemplate,
                configurationProviderAdapter,
                Clock.fixed(clock.instant().plus(200, ChronoUnit.DAYS), ZoneId.systemDefault())
        );
        User user1 = testUserHelper.createUser(
                TestUserHelper.UserGenerateParam.builder()
                        .workdate(LocalDate.now(clock).plusDays(40))
                        .userId(123L).build()
        );
        User user2 = testUserHelper.createUser(
                TestUserHelper.UserGenerateParam.builder()
                        .workdate(LocalDate.now(clock))
                        .userId(321L).build()
        );
        OrderGenerateService.OrderGenerateParam param1 = OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("11")
                .recipientPhone("phone1")
                .recipientFio("Иван Иванов")
                .addressGenerateParam(
                        AddressGenerator.AddressGenerateParam.builder()
                                .country("country1")
                                .region("region1")
                                .city("city1")
                                .street("street1")
                                .house("1")
                                .apartment("apartment1")
                                .entrance("entrance1")
                                .entryPhone("entryPhone1")
                                .floor(1)
                                .geoPoint(GeoPoint.ofLatLon(11, 12))
                                .build()
                )
                .build();
        OrderGenerateService.OrderGenerateParam param2 = OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("12")
                .recipientPhone("phone2")
                .recipientFio("Алексей  Алексеев ")
                .addressGenerateParam(
                        AddressGenerator.AddressGenerateParam.builder()
                                .country("country2")
                                .region("region2")
                                .city("city2")
                                .street("street2")
                                .house("1")
                                .apartment("apartment2")
                                .entrance("entrance2")
                                .entryPhone("entryPhone2")
                                .floor(1)
                                .geoPoint(GeoPoint.ofLatLon(12, 11))
                                .build()
                )
                .build();
        order1 = orderGenerateService.createOrder(param1);
        order2 = orderGenerateService.createOrder(param2);
        order3 = orderGenerateService.createOrder("13");

        order1.getDelivery().setRecipientPhonePersonalId(null);
        order1.getDelivery().setRecipientEmailPersonalId(null);
        order1.getDelivery().setRecipientFioPersonalId(null);
        order1.getDelivery().getDeliveryAddress().setAddressPersonalId(null);
        order1.getDelivery().getDeliveryAddress().setGpsPersonalId(null);

        order1 = orderRepository.save(order1);

        order2.getDelivery().setRecipientPhonePersonalId(null);
        order2.getDelivery().setRecipientEmailPersonalId(null);
        order2.getDelivery().setRecipientFioPersonalId(null);
        order2.getDelivery().getDeliveryAddress().setAddressPersonalId(null);
        order2.getDelivery().getDeliveryAddress().setGpsPersonalId(null);

        order2 = orderRepository.save(order2);

        order3.getDelivery().setRecipientPhonePersonalId(null);
        order3.getDelivery().setRecipientEmailPersonalId(null);
        order3.getDelivery().setRecipientFioPersonalId(null);
        order3.getDelivery().getDeliveryAddress().setAddressPersonalId(null);
        order3.getDelivery().getDeliveryAddress().setGpsPersonalId(null);

        order3 = orderRepository.save(order3);

        testUserHelper.createOpenedShift(user1, List.of(order1, order2), LocalDate.now(clock).plusDays(40));

        testUserHelper.createOpenedShift(user2, List.of(order3), LocalDate.now(clock));

        List<CommonType> requestItem = List.of(
                new CommonType().email(order1.getDelivery().getRecipientEmail()),
                new CommonType().phone(order2.getDelivery().getRecipientPhone()),
                new CommonType().phone(order1.getDelivery().getRecipientPhone()),
                new CommonType().fullName(new FullName().forename("Алексей").surname("Алексеев")),
                new CommonType().fullName(new FullName().forename("Иван").surname("Иванов")),
                new CommonType().address(toRequest(param2.getAddressGenerateParam())),
                new CommonType().address(toRequest(param1.getAddressGenerateParam())),
                new CommonType().gpsCoord(new GpsCoord()
                        .longitude(param1.getAddressGenerateParam().getGeoPoint().getLongitude().stripTrailingZeros()
                                .setScale(1))
                        .latitude(param1.getAddressGenerateParam().getGeoPoint().getLatitude().stripTrailingZeros()
                                .setScale(1))
                ),
                new CommonType().gpsCoord(new GpsCoord()
                        .longitude(param2.getAddressGenerateParam().getGeoPoint().getLongitude().stripTrailingZeros()
                                .setScale(1))
                        .latitude(param2.getAddressGenerateParam().getGeoPoint().getLatitude().stripTrailingZeros()
                                .setScale(1))
                )

        );

        PersonalMultiTypeStoreRequest request = new PersonalMultiTypeStoreRequest()
                .items(requestItem);

        PersonalMultiTypeStoreResponse response = new PersonalMultiTypeStoreResponse().items(
                List.of(
                        new MultiTypeStoreResponseItem()
                                .id("123")
                                .value(requestItem.get(0)),
                        new MultiTypeStoreResponseItem()
                                .id("456")
                                .value(requestItem.get(1)),
                        new MultiTypeStoreResponseItem()
                                .id("789")
                                .value(requestItem.get(2)),
                        new MultiTypeStoreResponseItem()
                                .id("22")
                                .value(requestItem.get(3)),
                        new MultiTypeStoreResponseItem()
                                .id("11")
                                .value(requestItem.get(4)),
                        new MultiTypeStoreResponseItem()
                                .id("321")
                                .value(requestItem.get(5)),
                        new MultiTypeStoreResponseItem()
                                .id("654")
                                .value(requestItem.get(6)),
                        new MultiTypeStoreResponseItem()
                                .id("987")
                                .value(requestItem.get(7)),
                        new MultiTypeStoreResponseItem()
                                .id("0")
                                .value(requestItem.get(8))
                )
        );

        Mockito.when(personalStoreApi.v1MultiTypesStorePost(request)).thenReturn(response);
    }

    @Test
    void storePersonalIds() {
        executor.doRealJob(null);
        order1 = orderRepository.findByIdOrThrow(order1.getId());
        order2 = orderRepository.findByIdOrThrow(order2.getId());
        order3 = orderRepository.findByIdOrThrow(order3.getId());

        Assertions.assertThat(order1.getDelivery().getRecipientEmailPersonalId()).isEqualTo("123");
        Assertions.assertThat(order1.getDelivery().getRecipientPhonePersonalId()).isEqualTo("789");
        Assertions.assertThat(order1.getDelivery().getRecipientFioPersonalId()).isEqualTo("11");
        Assertions.assertThat(order1.getDelivery().getDeliveryAddress().getAddressPersonalId()).isEqualTo("654");
        Assertions.assertThat(order1.getDelivery().getDeliveryAddress().getGpsPersonalId()).isEqualTo("987");

        Assertions.assertThat(order2.getDelivery().getRecipientEmailPersonalId()).isEqualTo("123");
        Assertions.assertThat(order2.getDelivery().getRecipientPhonePersonalId()).isEqualTo("456");
        Assertions.assertThat(order2.getDelivery().getRecipientFioPersonalId()).isEqualTo("22");
        Assertions.assertThat(order2.getDelivery().getDeliveryAddress().getAddressPersonalId()).isEqualTo("321");
        Assertions.assertThat(order2.getDelivery().getDeliveryAddress().getGpsPersonalId()).isEqualTo("0");

        Assertions.assertThat(order3.getDelivery().getRecipientEmailPersonalId()).isNull();
        Assertions.assertThat(order3.getDelivery().getRecipientPhonePersonalId()).isNull();
        Assertions.assertThat(order3.getDelivery().getRecipientPhonePersonalId()).isNull();
        Assertions.assertThat(order3.getDelivery().getDeliveryAddress().getAddressPersonalId()).isNull();
        Assertions.assertThat(order3.getDelivery().getDeliveryAddress().getGpsPersonalId()).isNull();

    }

    private Map<String, String> toRequest(AddressGenerator.AddressGenerateParam param) {
        return Map.of(
                PersonalAddressKeys.COUNTRY.getName(),
                param.getCountry(),
                PersonalAddressKeys.REGION.getName(),
                param.getRegion(),
                PersonalAddressKeys.LOCALITY.getName(),
                param.getCity(),
                PersonalAddressKeys.STREET.getName(),
                param.getStreet(),
                PersonalAddressKeys.HOUSE.getName(),
                param.getHouse(),
                PersonalAddressKeys.ROOM.getName(),
                param.getApartment(),
                PersonalAddressKeys.PORCH.getName(),
                param.getEntrance(),
                PersonalAddressKeys.INTERCOM.getName(),
                param.getEntryPhone(),
                PersonalAddressKeys.FLOOR.getName(),
                param.getFloor().toString());
    }

}
