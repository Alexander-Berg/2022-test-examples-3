package ru.yandex.market.tpl.core.demo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.manual.CreateClientReturnRoutePointRequestDto;
import ru.yandex.market.tpl.api.model.manual.CreateRoutePointRequestDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalStoreApi;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.GpsCoord;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeStoreResponseItem;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeStoreResponse;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.pickup.generator.PickupPointGenerator;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.task.TaskOrderDeliveryRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.service.demo.ManualService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.RETURN_CREATED;

@RequiredArgsConstructor
public class ManualServiceTest extends TplAbstractTest {
    private final ManualService manualService;
    private final TestUserHelper testUserHelper;
    private final Clock clock;
    private final TaskOrderDeliveryRepository taskOrderDeliveryRepository;
    private final ClientReturnRepository clientReturnRepository;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final UserShiftRepository userShiftRepository;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DefaultPersonalStoreApi personalStoreApi;

    private User user;
    private UserShift userShift;

    @BeforeEach
    public void init() {
        user = testUserHelper.findOrCreateUser(1);
        userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
    }

    @AfterEach
    public void after() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMART_CONSOLIDATION_ORDERS_FOR_LAVKA, false);
    }

    @Test
    void successfulCreateLockerDeliveryTasks() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.SMART_CONSOLIDATION_ORDERS_FOR_LAVKA, true);
        User user = testUserHelper.findOrCreateUser(456743456);
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

        //Должны объединиться, так как лавка и одинаковые адреса 1
        PickupPoint pickupPointAddressA1 = PickupPointGenerator.generatePickupPoint(546467L);
        pickupPointAddressA1.setAddress("ADDRESS.  A,");
        pickupPointAddressA1.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointAddressA1);
        Order orderAddressA1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointAddressA1)
                .build());
        CreateRoutePointRequestDto dtoAddressA1 = getDto(new BigDecimal("15"), new BigDecimal("16"));

        PickupPoint pickupPointAddressA2 = PickupPointGenerator.generatePickupPoint(546468L);
        pickupPointAddressA2.setAddress("ADDRESSA");
        pickupPointAddressA2.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointAddressA2);
        Order orderAddressA2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointAddressA2)
                .build());
        CreateRoutePointRequestDto dtoAddressA2 = getDto(new BigDecimal("15"), new BigDecimal("17"));

        //Нет объединения, адрес лавки уникален 2
        PickupPoint pickupPointAddressB1 = PickupPointGenerator.generatePickupPoint(546469L);
        pickupPointAddressB1.setAddress("ADDRESSB");
        pickupPointAddressB1.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointAddressB1);
        Order orderAddressB1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointAddressB1)
                .build());
        CreateRoutePointRequestDto dtoAddressB1 = getDto(new BigDecimal("15"), new BigDecimal("18"));

        //Нулевой адрес лавки, нет объединения, так как единственная привязка к PickupPoint 4
        PickupPoint pickupPointAddressNull1 = PickupPointGenerator.generatePickupPoint(546470L);
        pickupPointAddressNull1.setAddress(null);
        pickupPointAddressNull1.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointAddressNull1);
        Order orderAddressNull1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointAddressNull1)
                .build());
        CreateRoutePointRequestDto dtoAddressNull1 = getDto(new BigDecimal("15"), new BigDecimal("19"));

        PickupPoint pickupPointAddressNull2 = PickupPointGenerator.generatePickupPoint(546471L);
        pickupPointAddressNull2.setAddress(null);
        pickupPointAddressNull2.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointAddressNull2);
        Order orderAddressNull2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointAddressNull2)
                .build());
        CreateRoutePointRequestDto dtoAddressNull2 = getDto(new BigDecimal("15"), new BigDecimal("20"));

        //Не лавка, должны объединиться по id PickupPoint 5
        PickupPoint pickupPointForNotLavka = PickupPointGenerator.generatePickupPoint(546472L);
        pickupPointForNotLavka.setAddress(null);
        pickupPointForNotLavka.setPartnerSubType(PartnerSubType.PVZ);
        pickupPointRepository.save(pickupPointForNotLavka);
        Order orderNotLavka1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointForNotLavka)
                .build());
        Order orderNotLavka2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointForNotLavka)
                .build());
        CreateRoutePointRequestDto dtoNotLavka1 = getDto(new BigDecimal("15"), new BigDecimal("21"));
        CreateRoutePointRequestDto dtoNotLavka2 = getDto(new BigDecimal("15"), new BigDecimal("22"));


        //Лавка, должны объединиться по id PickupPoint, так как адрес нулевой и общий PickupPoint 6
        PickupPoint pickupPointForLavka = PickupPointGenerator.generatePickupPoint(546473L);
        pickupPointForLavka.setAddress(null);
        pickupPointForLavka.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointForLavka);
        Order orderForLavka1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointForLavka)
                .build());
        Order orderForLavka2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPointForLavka)
                .build());
        CreateRoutePointRequestDto dtoForLavka1 = getDto(new BigDecimal("15"), new BigDecimal("23"));
        CreateRoutePointRequestDto dtoForLavka2 = getDto(new BigDecimal("15"), new BigDecimal("24"));

        //Не лавка, одинаковые адреса, разные id. Не объединяются 8
        PickupPoint pickupPointForNotLavkaSameAddress1 = PickupPointGenerator.generatePickupPoint(546474L);
        pickupPointForNotLavkaSameAddress1.setAddress("ADDRESS");
        pickupPointForNotLavkaSameAddress1.setPartnerSubType(PartnerSubType.PVZ);
        pickupPointRepository.save(pickupPointForNotLavkaSameAddress1);
        Order orderForNotLavkaSameAddress1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam
                .builder()
                .pickupPoint(pickupPointForNotLavkaSameAddress1)
                .build());
        CreateRoutePointRequestDto dtoForNotLavkaSameAddress1
                = getDto(new BigDecimal("15"), new BigDecimal("25"));

        PickupPoint pickupPointForNotLavkaSameAddress2 = PickupPointGenerator.generatePickupPoint(546475L);
        pickupPointForNotLavkaSameAddress2.setAddress("ADDRESS");
        pickupPointForNotLavkaSameAddress2.setPartnerSubType(PartnerSubType.PVZ);
        pickupPointRepository.save(pickupPointForNotLavkaSameAddress2);
        Order orderForNotLavkaSameAddress2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam
                .builder()
                .pickupPoint(pickupPointForNotLavkaSameAddress2)
                .build());
        CreateRoutePointRequestDto dtoForNotLavkaSameAddress2
                = getDto(new BigDecimal("15"), new BigDecimal("26"));

        //Лавка, разные адреса, общие координаты. Объединяются 9
        PickupPoint pickupPointForLavkaSameCoordinates1 = PickupPointGenerator.generatePickupPoint(546479L);
        pickupPointForLavkaSameCoordinates1.setAddress("ADDRESS_COORD1");
        pickupPointForLavkaSameCoordinates1.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointForLavkaSameCoordinates1);
        Order orderForLavkaSameCoordinates1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam
                .builder()
                .pickupPoint(pickupPointForLavkaSameCoordinates1)
                .build());

        PickupPoint pickupPointForLavkaSameCoordinates2 = PickupPointGenerator.generatePickupPoint(546476L);
        pickupPointForLavkaSameCoordinates2.setAddress("ADDRESS_COORD2");
        pickupPointForLavkaSameCoordinates2.setPartnerSubType(PartnerSubType.LAVKA);
        pickupPointRepository.save(pickupPointForLavkaSameCoordinates2);
        Order orderForLavkaSameCoordinates2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam
                .builder()
                .pickupPoint(pickupPointForLavkaSameCoordinates2)
                .build());

        CreateRoutePointRequestDto dtoForLavkaSameCoordinates
                = getDto(new BigDecimal("15"), new BigDecimal("26"));

        //Не лавка, разные адреса, общие координаты. Не объединяются 11
        PickupPoint pickupPointForNotLavkaSameCoordinates1 = PickupPointGenerator.generatePickupPoint(546477L);
        pickupPointForNotLavkaSameCoordinates1.setAddress("ADDRESS_COORD_NOT_LAVKA1");
        pickupPointForNotLavkaSameCoordinates1.setPartnerSubType(PartnerSubType.PVZ);
        pickupPointRepository.save(pickupPointForNotLavkaSameCoordinates1);
        Order orderForNotLavkaSameCoordinates1 =
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam
                        .builder()
                        .pickupPoint(pickupPointForNotLavkaSameCoordinates1)
                        .build());

        PickupPoint pickupPointForNotLavkaSameCoordinates2 = PickupPointGenerator.generatePickupPoint(546478L);
        pickupPointForNotLavkaSameCoordinates2.setAddress("ADDRESS_COORD_NOT_LAVKA2");
        pickupPointForNotLavkaSameCoordinates2.setPartnerSubType(PartnerSubType.PVZ);
        pickupPointRepository.save(pickupPointForNotLavkaSameCoordinates2);
        Order orderForNotLavkaSameCoordinates2 =
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam
                        .builder()
                        .pickupPoint(pickupPointForNotLavkaSameCoordinates2)
                        .build());

        CreateRoutePointRequestDto dtoForNotLavkaSameCoordinates
                = getDto(new BigDecimal("15"), new BigDecimal("27"));

        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoAddressA1, orderAddressA1);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoAddressA2, orderAddressA2);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoAddressB1, orderAddressB1);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoAddressNull1, orderAddressNull1);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoAddressNull2, orderAddressNull2);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoNotLavka1, orderNotLavka1);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoNotLavka2, orderNotLavka2);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoForLavka1, orderForLavka1);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoForLavka2, orderForLavka2);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoForNotLavkaSameAddress1,
                orderForNotLavkaSameAddress1);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoForNotLavkaSameAddress2,
                orderForNotLavkaSameAddress2);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoForLavkaSameCoordinates,
                orderForLavkaSameCoordinates1);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoForLavkaSameCoordinates,
                orderForLavkaSameCoordinates2);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoForNotLavkaSameCoordinates,
                orderForNotLavkaSameCoordinates1);
        manualService.createLockerDeliveryTask(user.getUid(), userShift.getId(), dtoForNotLavkaSameCoordinates,
                orderForNotLavkaSameCoordinates2);

        transactionTemplate.execute(status -> {
            UserShift userShiftUpdate = userShiftRepository.getById(userShift.getId());
            long lockerTasksCount = userShiftUpdate.streamLockerDeliveryTasks().count();
            assertThat(lockerTasksCount).isEqualTo(11);
            return status;
        });
    }

    private CreateRoutePointRequestDto getDto(BigDecimal longitude, BigDecimal latitude) {
        CreateRoutePointRequestDto dto = new CreateRoutePointRequestDto();
        dto.setCity("CITY");
        dto.setStreet("STREET");
        dto.setHouse("HOUSE");
        dto.setBuilding("BUILD");
        dto.setHouse("HOUSE");
        dto.setExpectedDeliveryTime(Instant.now());
        dto.setLatitude(latitude);
        dto.setLongitude(longitude);
        dto.setType(RoutePointType.LOCKER_DELIVERY);
        return dto;
    }

    @Test
    void successfulCreateClientReturnTask() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.MANUAL_STORE_PERSONAL_ADDRESS, true);

        var request = CreateClientReturnRoutePointRequestDto.builder()
                .city("Moscow")
                .street("Tverskaya")
                .house("1")
                .expectedArriveTime(LocalDateTime.now(clock))
                .latitude(BigDecimal.ONE)
                .longitude(BigDecimal.TEN)
                .build();

        PersonalMultiTypeStoreResponse personalResponse = new PersonalMultiTypeStoreResponse().items(
                List.of(
                        new MultiTypeStoreResponseItem().id("123").value(new CommonType().address(Map.of())),
                        new MultiTypeStoreResponseItem().id("321").value(new CommonType().gpsCoord(new GpsCoord()))
                )
        );
        Mockito.when(personalStoreApi.v1MultiTypesStorePost(Mockito.any())).thenReturn(personalResponse);

        var routePointDto =
                assertDoesNotThrow(() ->
                        manualService.generateClientReturnOrderDeliveryTaskAndAssign(
                                user.getUid(), userShift.getId(), request
                        ));

        assertThat(routePointDto).isNotNull();
        var todList = taskOrderDeliveryRepository.findAll();
        assertThat(todList).hasSize(1);
        var tod = todList.get(0);
        assertThat(tod.getClientReturnId()).isNotNull();
        var clientReturn = clientReturnRepository.findByIdOrThrow(tod.getClientReturnId());
        assertThat(clientReturn.getLogisticRequestPointFrom()).isNotNull();
        assertThat(clientReturn.getArriveIntervalFrom()).isNotNull();
        assertThat(clientReturn.getStatus()).isEqualTo(RETURN_CREATED);
        assertThat(clientReturn.getLogisticRequestPointFrom().getAddressPersonalId()).isEqualTo("123");
        assertThat(clientReturn.getLogisticRequestPointFrom().getGpsPersonalId()).isEqualTo("321");
    }
}
