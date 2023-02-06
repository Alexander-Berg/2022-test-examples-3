package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.persistence.EntityManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.util.ScApiControllerCaller;
import ru.yandex.market.sc.core.domain.courier.model.ApiCourierDto;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.lot.model.ApiSortableDto;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.model.ApiCellWithLotDto;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundIdentifier;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundIdentifierType;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundRepository;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.route.model.ApiRouteStatus;
import ru.yandex.market.sc.core.domain.route.model.OutgoingCourierRouteType;
import ru.yandex.market.sc.core.domain.route.model.OutgoingRouteBaseDto;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.exception.ScErrorCode;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScApiControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@ExtendWith(DefaultScUserWarehouseExtension.class)
public class OutboundBindLotControllerTest {

    private final TestFactory testFactory;
    private final ScApiControllerCaller caller;
    private final XDocFlow flow;

    private final OutboundRepository outboundRepository;
    private final SortableRepository sortableRepository;
    private final PlaceRepository placeRepository;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final SortableLotService sortableLotService;

    @MockBean
    Clock clock;

    @BeforeEach
    void init() {
        TestFactory.setupMockClock(clock);
    }

    @SneakyThrows
    @DisplayName("простой сценарий отгрузки лота через outbound")
    @Test
    void simpleBindLotToOutbound() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);
        var lotForOutboundResponse = objectMapper.readValue(caller.getLotForOutbound(lot.getBarcode())
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), ApiSortableDto.class);
        assertThat(lotForOutboundResponse.getExternalId()).isEqualTo(lot.getBarcode());
        assertThat(lotForOutboundResponse.getStatus()).isEqualTo(LotStatus.READY);
        var outboundExternalId = caller.bindLotToOutbound(outbound.getExternalId(),
                        lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().isOk())
                .getResponseAsClass(String.class);
        assertThat(outboundExternalId).isEqualTo(outbound.getExternalId());
        var palletAsSortable = sortableRepository.findById(lot.getSortableId()).orElseThrow();
        outbound = outboundRepository.findByIdOrThrow(outbound.getId());
        order = testFactory.getOrder(order.getId());
        assertThat(palletAsSortable.getOutbound()).isEqualTo(outbound);
        assertThat(palletAsSortable.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        lot = sortableLotService.findByLotIdOrThrow(lot.getLotId());
        assertThat(lot.getOptLotStatus()).isEmpty();
        assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(testFactory.orderPlaces(order.getId()).stream()
                .map(Place::getStatus)
                .toList()).containsOnly(PlaceStatus.SHIPPED);
    }

    @DisplayName("Нельзя отгрузить уже отгруженный лот")
    @Test
    void cainBindShippedLot() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);
        var outboundExternalId = caller.bindLotToOutbound(outbound.getExternalId(),
                        lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().isOk())
                .getResponseAsClass(String.class);
        caller.bindLotToOutbound(outbound.getExternalId(),
                        lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is4xxClientError())
                .andExpect(content().json(String.format("""
                        {
                            "message": "Лот уже отгружен. Отгрузка: %s, Лот: %s"
                        }
                        """, outbound.getExternalId(), lot.getBarcode()), false));
    }

    @DisplayName("Нельзя отгрузить просроченный лот")
    @Test
    void cainBindLotToOutdatedOutbound() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(3, ChronoUnit.DAYS))
                .toTime(clock.instant().minus(2, ChronoUnit.DAYS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                            .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);
        var errmsg = String.format("Лот не может быть отгружен в рамках отгрузки %s. Направление маршрута: %d:%s.",
                outbound.getExternalId(), route.getCourierTo().getId(), route.getCourierTo().getName());
        caller.bindLotToOutbound(outbound.getExternalId(), lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value(errmsg));
    }

    @DisplayName("простой сценарий отгрузки лота через outbound. поиска отгрузки через гос номер авто")
    @Test
    void bindLotToOutboundByCarNumber() {
        long sortingCenterToId = 111746283111L; //random number
        String carNumber = "а123аа797";
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .carNumber(carNumber)
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);
        var outboundExternalId = caller.bindLotToOutbound(lot.getBarcode(),
                                    testFactory.getRouteIdForSortableFlow(route), carNumber)
                .andExpect(status().isOk())
                .getResponseAsClass(String.class);
        assertThat(outboundExternalId).isEqualTo(outbound.getExternalId());
        var palletAsSortable = sortableRepository.findById(lot.getSortableId()).orElseThrow();
        outbound = outboundRepository.findByIdOrThrow(outbound.getId());
        order = testFactory.getOrder(order.getId());
        assertThat(palletAsSortable.getOutbound()).isEqualTo(outbound);
        assertThat(palletAsSortable.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        lot = sortableLotService.findByLotIdOrThrow(lot.getLotId());
        assertThat(lot.getOptLotStatus()).isEmpty();
        assertThat(lot.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        assertThat(order.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(testFactory.orderPlaces(order.getId()).stream()
                .map(Place::getStatus)
                .toList()).containsOnly(PlaceStatus.SHIPPED);
    }

    @DisplayName("fail - попытка привязки к несуществующей отгрузке")
    @Test
    void lotBindingToWrongOutbound() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, true);
        testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setSortingCenterProperty(flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES, true);
        testFactory.increaseScOrderId();

        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("77777")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );

        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        var vehicleNumber = "ВЕ888ЗУ";
        var outbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .carNumber(vehicleNumber)
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);

        testFactory.sortOrderToLot(order, lot, flow.getUser());
        testFactory.prepareToShipLot(lot);

        var outboundId = new OutboundIdentifier(
                OutboundIdentifierType.EXTERNAL_ID,
                outbound.getExternalId() + "cant-find-it"
        );

        caller.bindLotToOutbound(lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route), outboundId)
                .andExpect(status().is4xxClientError());

        outboundId = new OutboundIdentifier(OutboundIdentifierType.VEHICLE_NUM, vehicleNumber + "cant-find-it");

        caller.bindLotToOutbound(lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route), outboundId)
                .andExpect(status().is4xxClientError());
    }

    @DisplayName("Нельзя отгружать маршрут по схеме ОПКО СЦ - СЦ, если служба доставки это не поддерживает")
    @Test
    void cantBindLotIfDeliveryServiceNotSupportIt() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                        .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);
        var errmsg = buildErrMsg(outbound, route.getCourierTo());
        caller.bindLotToOutbound(outbound.getExternalId(),
                        lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value(errmsg));
    }

    @DisplayName("нельзя отгружать маршрут по схеме ОПКО СЦ - СЦ если служба доставки это поддерживает на другом сц")
    @Test
    void cantBindLotIfDeliveryServiceSupportOnAnotherSc() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(37263917234234L)); //random sc id
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);
        var errmsg = buildErrMsg(outbound, route.getCourierTo());
        caller.bindLotToOutbound(outbound.getExternalId(), lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value(errmsg));
    }

    @DisplayName("нельзя отгружать маршрут по схеме ОПКО СЦ - СЦ если СЦ это не поддерживает")
    @Test
    void cantBindLotIfScNotSuportIt() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                        .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);
        var errmsg = buildErrMsg(outbound, route.getCourierTo());
        caller.bindLotToOutbound(outbound.getExternalId(), lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value(errmsg));
    }

    @DisplayName("Нельзя отгружать маршрут к чужой отгрузке (outbound)")
    @Test
    void cantBindLotToStrangeOutbound() {
        var sortingCenter = flow.getSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        long outboundSortingCenterToId = 111746283111L; //random number
        long routeSortingCenterToId = 726351273228313L; //another random number
        testFactory.increaseScOrderId();
        var outboundDeliveryService = testFactory
                .storedDeliveryService(String.valueOf(outboundSortingCenterToId));
        var routeDeliveryService = testFactory
                .storedDeliveryService(String.valueOf(routeSortingCenterToId));
        testFactory.setDeliveryServiceProperty(outboundDeliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        testFactory.setDeliveryServiceProperty(routeDeliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var outboundSortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(outboundSortingCenterToId)
                        .yandexId("5378264623")
                        .token("outbound_sc_to_token")
                        .partnerName("outbound_sc_to_partner_name")
                        .build()
        );
        var routeSortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(routeSortingCenterToId)
                        .yandexId("74282345623")
                        .token("route_sc_to_token")
                        .partnerName("route_sc_to_partner_name")
                        .build()
        );
        var scToScOutboundStrange = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(outboundSortingCenterTo.getYandexId())
                .build()
        );
        var scToScOutboundTarget = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound2")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(outboundSortingCenterTo.getYandexId())
                .build()
        );
        var targetOrder = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(routeDeliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var anotherOrder = testFactory.createForToday(
                order(flow.getSortingCenter(), "2").places("21", "22")
                        .deliveryService(outboundDeliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("21", "22").sortPlaces("21", "22").get();

        var route = testFactory.findOutgoingCourierRoute(targetOrder).orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, targetOrder);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(targetOrder, "11", lot, flow.getUser());
        testFactory.sortToLot(targetOrder, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);
        var errmsg = String.format("Лот не может быть отгружен в рамках отгрузки %s. Направление маршрута: %d:%s.",
                scToScOutboundStrange.getExternalId(), route.getCourierToId(), route.getCourierTo().getName()
        );
        caller.bindLotToOutbound(scToScOutboundStrange.getExternalId(),
                        lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value(errmsg));
    }

    @DisplayName("информация по маршруту для маршрута дропофф - СЦ должно приходить " +
            "с типом COURIER если все отгрузки закрыты")
    @Test
    void assertCourierTypeIfAllOutboundClosedOnDropoff() {
        var sortingCenter = flow.getSortingCenter();
        var user = flow.getUser();
        long sortingCenterToId = 77777;
        long logisticPointToId = 5378264623L;
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.increaseScOrderId();
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);
        testFactory.setSortingCenterProperty(sortingCenter,
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS, true);
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(sortingCenter.getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId(String.valueOf(logisticPointToId))
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(sortingCenter)
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        outbound.setStatus(OutboundStatus.CANCELLED_BY_SC);
        outboundRepository.save(outbound);
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);

        var courier = route.getCourier().orElseThrow();
        OutgoingRouteBaseDto expectedResult = new OutgoingRouteBaseDto(
                testFactory.getRouteIdForSortableFlow(route),
                new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(), courier.getDeliveryServiceId()),
                null,
                ApiRouteStatus.NOT_STARTED,
                List.of(TestFactory.cellDto(cell, false, 1)),
                1,
                OutgoingCourierRouteType.COURIER
        );

        caller.getApiV2RoutesByID(testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expectedResult)));

    }

    @DisplayName("информация по маршруту для маршрута ОПКО СЦ - СЦ должно приходить с типом MAGISTRAL")
    @Test
    void assertCourierRouteTypeIsMagistral() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );

        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);

        var courier = route.getCourier().orElseThrow();
        OutgoingRouteBaseDto expectedResult = new OutgoingRouteBaseDto(
                testFactory.getRouteIdForSortableFlow(route),
                new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(), courier.getDeliveryServiceId()),
                null,
                ApiRouteStatus.NOT_STARTED,
                List.of(TestFactory.cellDto(cell, false, 1)),
                1,
                OutgoingCourierRouteType.MAGISTRAL
        );

        caller.getApiV2RoutesByID(testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expectedResult)));
    }

    @DisplayName("информация по маршруту для маршрута ОПКО дропофф - СЦ " +
            "должна приходить с типом DROPOFF когда есть лоты")
    @Test
    void assertCourierRouteTypeIsDROPOFFForDropoff() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.IS_DROPOFF, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );

        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                    .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.addStampToSortableLotAndPrepare(lot.getBarcode(), "simpleStamp", flow.getUser());

        var courier = route.getCourier().orElseThrow();
        OutgoingRouteBaseDto expectedResult = new OutgoingRouteBaseDto(
                testFactory.getRouteIdForSortableFlow(route),
                new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(), courier.getDeliveryServiceId()),
                null,
                ApiRouteStatus.NOT_STARTED,
                List.of(TestFactory.cellDto(cell, false, 1)),
                1,
                OutgoingCourierRouteType.DROPOFF
        );

        caller.getApiV2RoutesByID(testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expectedResult)));
    }

    @DisplayName("информация по маршруту для маршрута ОПКО дропофф - СЦ " +
            "должна приходить с типом Courier когда есть только созданные и отгруженные лоты")
    @Test
    void assertCourierRouteTypeIsCourierForDropoff() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.IS_DROPOFF, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );

        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var order2 = testFactory.createForToday(
                order(flow.getSortingCenter(), "o3")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var lot2 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var courier = route.getCourier().orElseThrow();

        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());

        testFactory.addStampToSortableLotAndPrepare(lot.getBarcode(), "simpleStamp", flow.getUser());
        caller.bindLotToOutbound(outbound.getExternalId(),
                        lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful());
        OutgoingRouteBaseDto expectedResult = new OutgoingRouteBaseDto(
                testFactory.getRouteIdForSortableFlow(route),
                new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(), courier.getDeliveryServiceId()),
                null,
                ApiRouteStatus.IN_PROGRESS,
                List.of(TestFactory.cellDto(cell, false, 0)),
                0,
                OutgoingCourierRouteType.COURIER
        );

        caller.getApiV2RoutesByID(testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expectedResult)));

        testFactory.sortToLot(order2, "o3", lot2, flow.getUser());

        testFactory.addStampToSortableLotAndPrepare(lot2.getBarcode(), "simpleStamp2", flow.getUser());
        OutgoingRouteBaseDto expectedResult2 = new OutgoingRouteBaseDto(
                testFactory.getRouteIdForSortableFlow(route),
                new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(), courier.getDeliveryServiceId()),
                null,
                ApiRouteStatus.IN_PROGRESS,
                List.of(TestFactory.cellDto(cell, false, 1)),
                1,
                OutgoingCourierRouteType.DROPOFF
        );

        caller.getApiV2RoutesByID(testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expectedResult2)));


    }


    @DisplayName("информация по маршруту для маршрута ОПКО дропофф - СЦ " +
            "должна приходить с типом DROPOFF когда нет открытых лотов")
    @Test
    void assertCourierRouteTypeIsCOURIERWhenNoOpenLotsDropoff() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.IS_DROPOFF, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );

        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);

        var courier = route.getCourier().orElseThrow();
        OutgoingRouteBaseDto expectedResult = new OutgoingRouteBaseDto(
                testFactory.getRouteIdForSortableFlow(route),
                new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(), courier.getDeliveryServiceId()),
                null,
                ApiRouteStatus.NOT_STARTED,
                List.of(TestFactory.cellDto(cell, false, 0)),
                0,
                OutgoingCourierRouteType.COURIER
        );

        caller.getApiV2RoutesByID(testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expectedResult)));
    }

    @DisplayName("информация по маршруту для маршрута ОПКО СЦ - СЦ должно приходить с типом MAGISTRAL," +
            " если включено свойство ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES")
    @Test
    void assertCourierRouteTypeIsMagistralIfGlobalPropertyEnabled() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES,
                true
        );

        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);

        var courier = route.getCourier().orElseThrow();
        OutgoingRouteBaseDto expectedResult = new OutgoingRouteBaseDto(
                testFactory.getRouteIdForSortableFlow(route),
                new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(), courier.getDeliveryServiceId()),
                null,
                ApiRouteStatus.NOT_STARTED,
                List.of(TestFactory.cellDto(cell, false, 1)),
                1,
                OutgoingCourierRouteType.MAGISTRAL
        );

        caller.getApiV2RoutesByID(testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expectedResult)));
    }

    @DisplayName("информация по маршруту для маршрута ОПКО СЦ - СЦ должно приходить с типом COURIER " +
            "если настройка выключена")
    @Test
    void assertCourierRouteTypeIsCourierWhenPropertyIsOff() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES,
                false
        );

        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                        .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);

        var courier = route.getCourier().orElseThrow();

        OutgoingRouteBaseDto expectedResult = new OutgoingRouteBaseDto(
                testFactory.getRouteIdForSortableFlow(route),
                new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(), courier.getDeliveryServiceId()),
                null,
                ApiRouteStatus.NOT_STARTED,
                List.of(TestFactory.cellDto(cell, false, 1)),
                1,
                OutgoingCourierRouteType.COURIER
        );

        caller.getApiV2RoutesByID(testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expectedResult)));
    }

    @DisplayName("информация по маршруту для маршрута ОПКО СЦ - СЦ должно приходить с типом COURIER " +
            "если настройка включена, но отгрузка зафиксирована")
    @Test
    void assertCourierRouteTypeIsCourierWhenPropertyIsOnButOutboundIsShipped() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();

        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                    .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);

        var courier = route.getCourier().orElseThrow();

        caller.bindLotToOutbound(outbound.getExternalId(),
                        lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful());
        transactionTemplate.execute(ts -> {
            var outboundChange = outboundRepository.findByExternalId(outbound.getExternalId()).orElseThrow();
            outboundChange.setStatus(OutboundStatus.SHIPPED);
            entityManager.flush();
            return null;
        });
        OutgoingRouteBaseDto expectedResult = new OutgoingRouteBaseDto(
                testFactory.getRouteIdForSortableFlow(route),
                new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(), courier.getDeliveryServiceId()),
                null,
                ApiRouteStatus.SHIPPED,
                List.of(TestFactory.cellDto(cell, true, 0)),
                0,
                OutgoingCourierRouteType.COURIER
        );
        caller.getApiV2RoutesByID(testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expectedResult)));
    }

    @DisplayName("информация по маршруту для маршрута ОПКО СЦ - СЦ должно приходить с типом COURIER когда нет отгрузки")
    @Test
    void assertCourierRouteTypeIsCourierWhenNoOutbound() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                    .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);

        var courier = route.getCourier().orElseThrow();
        OutgoingRouteBaseDto expectedResult = new OutgoingRouteBaseDto(
                testFactory.getRouteIdForSortableFlow(route),
                new ApiCourierDto(courier.getId(), courier.getId(), courier.getName(), courier.getDeliveryServiceId()),
                null,
                ApiRouteStatus.NOT_STARTED,
                List.of(TestFactory.cellDto(cell, false, 1)),
                1,
                OutgoingCourierRouteType.COURIER
        );

        caller.getApiV2RoutesByID(testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expectedResult)));
    }

    @DisplayName("Нельзя отгружать уже отгруженный лот")
    @Test
    void cantBindShippedLot() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);
        var outboundExternalId = caller.bindLotToOutbound(outbound.getExternalId(),
                        lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().isOk());
        caller.getLotForOutbound(lot.getBarcode())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value(ScErrorCode.LOT_IN_WRONG_STATUS_FOR_OUTBOUND.getMessage()));
    }

    @DisplayName("Нельзя отгрузить не готовый к отгрузке лот")
    @Test
    void cantBindProcessingLot() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order)).orElseThrow();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        caller.getLotForOutbound(lot.getBarcode())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message")
                        .value(ScErrorCode.LOT_IN_WRONG_STATUS_FOR_OUTBOUND.getMessage()));
    }

    @DisplayName("Ячейка магистрального маршрута с лотами. простой сценарий")
    @Test
    void getCellWithLotInfo() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                    .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot, flow.getUser());
        testFactory.sortToLot(order, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);
        var outboundExternalId = caller.bindLotToOutbound(outbound.getExternalId(),
                        lot.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().isOk())
                .getResponseAsClass(String.class);

        ApiCellWithLotDto expected = new ApiCellWithLotDto(testFactory.getRouteIdForSortableFlow(route), route.getCourierTo().getName(),
                1L, 0L, 0L, cell.getScNumber(), cell.getId());

        caller.getCellWithLotInfo(cell.getId(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expected)));
    }

    @DisplayName("Ячейка магистрального маршрута с лотами. Есть собираемые лоты и готовые к отгрузке лоты")
    @Test
    void getCellWithLotInfoWithProcessingLots() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();

        var regularOrder1 = testFactory.createForToday(
                order(flow.getSortingCenter(), "r1")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();

        var regularOrder2 = testFactory.createForToday(
                order(flow.getSortingCenter(), "r2")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();

        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                        .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot1 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var lot2 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var lot3 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot1, flow.getUser());
        testFactory.sortToLot(order, "12", lot1, flow.getUser());
        testFactory.sortOrderToLot(regularOrder1, lot2, flow.getUser());
        testFactory.sortOrderToLot(regularOrder1, lot3, flow.getUser());
        testFactory.prepareToShipLot(lot1);
        testFactory.prepareToShipLot(lot3);
        var outboundExternalId = caller.bindLotToOutbound(outbound.getExternalId(),
                        lot1.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().isOk())
                .getResponseAsClass(String.class);

        ApiCellWithLotDto expected = new ApiCellWithLotDto(testFactory.getRouteIdForSortableFlow(route), route.getCourierTo().getName(),
                1L, 1L, 1L, cell.getScNumber(), cell.getId());

        caller.getCellWithLotInfo(cell.getId(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expected)));
    }

    @DisplayName("Все лоты готовятся")
    @Test
    void getCellWithLotInfoAllProcessing() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();

        var regularOrder1 = testFactory.createForToday(
                order(flow.getSortingCenter(), "r1")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();

        var regularOrder2 = testFactory.createForToday(
                order(flow.getSortingCenter(), "r2")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();

        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                    .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot1 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var lot2 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var lot3 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot1, flow.getUser());
        testFactory.sortToLot(order, "12", lot2, flow.getUser()); //second place to another lot
        testFactory.sortOrderToLot(regularOrder1, lot2, flow.getUser());
        testFactory.sortOrderToLot(regularOrder1, lot3, flow.getUser());

        ApiCellWithLotDto expected = new ApiCellWithLotDto(testFactory.getRouteIdForSortableFlow(route), route.getCourierTo().getName(),
                0L, 3L, 0L, cell.getScNumber(), cell.getId());

        caller.getCellWithLotInfo(cell.getId(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expected)));
    }

    @DisplayName("Все лоты готовы")
    @Test
    void getCellWithLotInfoAllReady() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant())
                .toTime(clock.instant())
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();

        var regularOrder1 = testFactory.createForToday(
                order(flow.getSortingCenter(), "r1")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();

        var regularOrder2 = testFactory.createForToday(
                order(flow.getSortingCenter(), "r2")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();

        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot1 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var lot2 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var lot3 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot1, flow.getUser());
        testFactory.sortToLot(order, "12", lot2, flow.getUser()); //second place to another lot
        testFactory.sortOrderToLot(regularOrder1, lot2, flow.getUser());
        testFactory.sortOrderToLot(regularOrder1, lot3, flow.getUser());

        testFactory.prepareToShipLot(lot1);
        testFactory.prepareToShipLot(lot2);
        testFactory.prepareToShipLot(lot3);

        ApiCellWithLotDto expected = new ApiCellWithLotDto(testFactory.getRouteIdForSortableFlow(route), route.getCourierTo().getName(),
                0L, 0L, 3L, cell.getScNumber(), cell.getId());

        caller.getCellWithLotInfo(cell.getId(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expected)));
    }

    @DisplayName("Все лоты отгружены")
    @Test
    void getCellWithLotInfoAllShipped() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var scToScOutbound = testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        var outbound = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();

        var regularOrder1 = testFactory.createForToday(
                order(flow.getSortingCenter(), "r1")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();

        var regularOrder2 = testFactory.createForToday(
                order(flow.getSortingCenter(), "r2")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();

        var route = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                    .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order);
        var lot1 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var lot2 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var lot3 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot1, flow.getUser());
        testFactory.sortToLot(order, "12", lot2, flow.getUser()); //second place to another lot
        testFactory.sortOrderToLot(regularOrder1, lot2, flow.getUser());
        testFactory.sortOrderToLot(regularOrder1, lot3, flow.getUser());

        testFactory.prepareToShipLot(lot1);
        testFactory.prepareToShipLot(lot2);
        testFactory.prepareToShipLot(lot3);

        caller.bindLotToOutbound(outbound.getExternalId(),
                        lot1.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(outbound.getExternalId())));
        caller.bindLotToOutbound(outbound.getExternalId(),
                        lot2.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(outbound.getExternalId())));
        caller.bindLotToOutbound(outbound.getExternalId(),
                        lot3.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(outbound.getExternalId())));

        ApiCellWithLotDto expected = new ApiCellWithLotDto(testFactory.getRouteIdForSortableFlow(route), route.getCourierTo().getName(),
                3L, 0L, 0L, cell.getScNumber(), cell.getId());

        caller.getCellWithLotInfo(cell.getId(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expected)));
    }

    @DisplayName("Статистика по разным направлениям не смешивается")
    @Test
    void getCellWithLotInfoDifferentRoutes() {
        long sortingCenterToId1 = 111746283111L; //random number
        long sortingCenterToId2 = 6253123726231L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService1 = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId1));
        var deliveryService2 = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId2));

        testFactory.setDeliveryServiceProperty(deliveryService1,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        testFactory.setDeliveryServiceProperty(deliveryService2,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));

        var sortingCenterTo1 = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId1)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        var sortingCenterTo2 = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId2)
                        .yandexId("582463923")
                        .token("sc_to_token_2")
                        .partnerName("sc_to_partner_name_2")
                        .build()
        );
        testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo1.getYandexId())
                .build()
        );
        testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound2")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo2.getYandexId())
                .build()
        );
        var outbound1 = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var outbound2 = outboundRepository.findByExternalId("outbound2").orElseThrow();
        var order = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService1).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();

        var regularOrder1 = testFactory.createForToday(
                order(flow.getSortingCenter(), "r1")
                        .deliveryService(deliveryService1).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();

        var regularOrder2 = testFactory.createForToday(
                order(flow.getSortingCenter(), "r2")
                        .deliveryService(deliveryService1).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();

        var route1 = testFactory.findOutgoingCourierRoute(testFactory.getOrderLikeForRouteLookup(order))
                                                                                        .orElseThrow().allowReading();
        var cell1 = testFactory.determineRouteCell(route1, order);
        var lot1 = testFactory.storedLot(flow.getSortingCenter(), cell1, LotStatus.CREATED);
        var lot2 = testFactory.storedLot(flow.getSortingCenter(), cell1, LotStatus.CREATED);
        var lot3 = testFactory.storedLot(flow.getSortingCenter(), cell1, LotStatus.CREATED);
        testFactory.sortToLot(order, "11", lot1, flow.getUser());
        testFactory.sortToLot(order, "12", lot2, flow.getUser()); //second place to another lot
        testFactory.sortOrderToLot(regularOrder1, lot2, flow.getUser());
        testFactory.sortOrderToLot(regularOrder1, lot3, flow.getUser());

        testFactory.prepareToShipLot(lot1);
        testFactory.prepareToShipLot(lot2);

        Long route1Id = testFactory.getRouteIdForSortableFlow(route1.getId());
        caller.bindLotToOutbound(outbound1.getExternalId(),
                        lot1.getBarcode(), route1Id)
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(outbound1.getExternalId())));

        ApiCellWithLotDto expected = new ApiCellWithLotDto(route1Id, route1.getCourierTo().getName(),
                1L, 1L, 1L, cell1.getScNumber(), cell1.getId());

        //второй маршрут
        var regularOrderToAnotherDestination1 = testFactory.createForToday(
                order(flow.getSortingCenter(), "d1")
                        .deliveryService(deliveryService2).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        var regularOrderToAnotherDestination2 = testFactory.createForToday(
                order(flow.getSortingCenter(), "d2")
                        .deliveryService(deliveryService2).dsType(DeliveryServiceType.TRANSIT).build()
        ).accept().sort().get();
        var mOrderToAnotherDestination1 = testFactory.createForToday(
                order(flow.getSortingCenter(), "d3").places("d11", "d12")
                        .deliveryService(deliveryService2).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("d11", "d12").sortPlaces("d11", "d12").get();
        var route2 = testFactory.findOutgoingCourierRoute(regularOrderToAnotherDestination1)
                                                                                .orElseThrow().allowReading();
        var cell2 = testFactory.determineRouteCell(route2, regularOrderToAnotherDestination1);
        var dlot1 = testFactory.storedLot(flow.getSortingCenter(), cell2, LotStatus.CREATED);
        var dlot2 = testFactory.storedLot(flow.getSortingCenter(), cell2, LotStatus.CREATED);
        var dlot3 = testFactory.storedLot(flow.getSortingCenter(), cell2, LotStatus.CREATED);
        testFactory.sortOrderToLot(regularOrderToAnotherDestination1, dlot1, flow.getUser());
        testFactory.sortOrderToLot(regularOrderToAnotherDestination2, dlot2, flow.getUser());
        testFactory.sortToLot(mOrderToAnotherDestination1, "d11", dlot3, flow.getUser());
        testFactory.prepareToShipLot(dlot1);
        testFactory.prepareToShipLot(dlot2);

        Long route2Id = testFactory.getRouteIdForSortableFlow(route2.getId());
        caller.bindLotToOutbound(outbound2.getExternalId(),
                        dlot1.getBarcode(), route2Id)
                .andExpect(status().isOk())
                .andExpect(content().string(JacksonUtil.toString(outbound2.getExternalId())));

        caller.getCellWithLotInfo(cell1.getId(), route1Id)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expected)));
    }

    @DisplayName("Две отгрузки на одно направление на сегодня")
    @Test
    void twoOtboundsForOneRoute() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound2")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        ); //такая отгрузка как и первая, только двумя часами позже
        var outbound1 = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var outbound2 = outboundRepository.findByExternalId("outbound2").orElseThrow();
        var order1 = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var order2 = testFactory.createForToday(
                order(flow.getSortingCenter(), "2").places("21", "22", "23", "24", "25")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("21", "22", "23", "24").sortPlaces("21", "22", "23").get(); //пойдет на вторую отгрузку
        var route = testFactory.findOutgoingCourierRoute(order1).orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(route, order1);
        var deletedLot = testFactory.storedLot(flow.getSortingCenter(), SortableType.PALLET,
                cell, LotStatus.CREATED, true);
        var lot0 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var lot1 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var lot2 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var lot3 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        var lot4 = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(order1, "11", lot1, flow.getUser());
        testFactory.sortToLot(order1, "12", lot1, flow.getUser());
        testFactory.sortToLot(order2, "21", lot2, flow.getUser());
        testFactory.sortToLot(order2, "23", lot3, flow.getUser());
        testFactory.sortToLot(order2, "24", lot4, flow.getUser());
        testFactory.prepareToShipLot(lot1);
        testFactory.prepareToShipLot(lot2);
        testFactory.prepareToShipLot(lot3);
        var outboundExternalId1 = caller.bindLotToOutbound(outbound1.getExternalId(),
                        lot1.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().isOk())
                .getResponseAsClass(String.class);
        var outboundExternalId2 = caller.bindLotToOutbound(outbound2.getExternalId(),
                        lot2.getBarcode(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().isOk())
                .getResponseAsClass(String.class);
        assertThat(outboundExternalId1).isEqualTo(outbound1.getExternalId());
        assertThat(outboundExternalId2).isEqualTo(outbound2.getExternalId());
        var palletAsSortable1 = sortableRepository.findById(lot1.getSortableId()).orElseThrow();
        var palletAsSortable2 = sortableRepository.findById(lot2.getSortableId()).orElseThrow();
        outbound1 = outboundRepository.findByIdOrThrow(outbound1.getId());
        order1 = testFactory.getOrder(order1.getId());
        outbound2 = outboundRepository.findByIdOrThrow(outbound2.getId());
        order2 = testFactory.getOrder(order2.getId());
        assertThat(palletAsSortable1.getOutbound()).isEqualTo(outbound1);
        assertThat(palletAsSortable1.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);

        assertThat(palletAsSortable2.getOutbound()).isEqualTo(outbound2);
        assertThat(palletAsSortable2.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);

        lot1 = testFactory.getLot(lot1.getLotId());
        lot2 = testFactory.getLot(lot2.getLotId());
        assertThat(lot1.getOptLotStatus()).isEmpty();
        assertThat(lot1.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        assertThat(lot2.getOptLotStatus()).isEmpty();
        assertThat(lot2.getStatus()).isEqualTo(SortableStatus.SHIPPED_DIRECT);
        assertThat(order1.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_SHIPPED_TO_SO_FF);
        assertThat(order2.getFfStatus()).isEqualTo(ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE);
        assertThat(testFactory.orderPlaces(order1.getId()).stream()
                .map(Place::getStatus)
                .toList()).containsOnly(PlaceStatus.SHIPPED);
        assertThat(placeRepository.findByOrderIdAndMainPartnerCode(order2.getId(), "21").orElseThrow()
                .getStatus()).isEqualTo(PlaceStatus.SHIPPED);
        assertThat(placeRepository.findByOrderIdAndMainPartnerCode(order2.getId(), "22").orElseThrow()
                .getStatus()).isEqualTo(PlaceStatus.SORTED);

        ApiCellWithLotDto expected = new ApiCellWithLotDto(testFactory.getRouteIdForSortableFlow(route), route.getCourierTo().getName(),
                2L, 1L, 1L, cell.getScNumber(), cell.getId());

        caller.getCellWithLotInfo(cell.getId(), testFactory.getRouteIdForSortableFlow(route))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expected)));
    }

    @DisplayName("Две отгрузки на одно направление на разные дни")
    @Test
    void twoOutboundsForOneDestinationInDifferentDays() {
        long sortingCenterToId = 111746283111L; //random number
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.setSortingCenterProperty(
                flow.getSortingCenter().getId(),
                SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS,
                true
        );
        testFactory.increaseScOrderId();
        var deliveryService = testFactory.storedDeliveryService(String.valueOf(sortingCenterToId));
        testFactory.setDeliveryServiceProperty(deliveryService,
                DeliveryServiceProperty.DS_SUPPORT_SC_TO_SC_TRANSPORTATIONS,
                String.valueOf(flow.getSortingCenter().getId()));
        var sortingCenterTo = testFactory.storedSortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(sortingCenterToId)
                        .yandexId("5378264623")
                        .token("sc_to_token")
                        .partnerName("sc_to_partner_name")
                        .build()
        );
        testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound1")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        );
        testFactory.createOutbound(TestFactory.CreateOutboundParams.builder()
                .externalId("outbound2")
                .type(OutboundType.DS_SC)
                .fromTime(clock.instant().plus(1, ChronoUnit.DAYS).minus(1, ChronoUnit.HOURS))
                .toTime(clock.instant().plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS))
                .sortingCenter(flow.getSortingCenter())
                .logisticPointToExternalId(sortingCenterTo.getYandexId())
                .build()
        ); //outbound for tomorrow
        var outboundToday = outboundRepository.findByExternalId("outbound1").orElseThrow();
        var outboundTomorrow = outboundRepository.findByExternalId("outbound2").orElseThrow();
        var orderToday = testFactory.createForToday(
                order(flow.getSortingCenter(), "1").places("11", "12")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).acceptPlaces("11", "12").sortPlaces("11", "12").get();
        var routeToday = testFactory.findOutgoingCourierRoute(orderToday)
                                                                    .orElseThrow().allowReading();
        var cell = testFactory.determineRouteCell(routeToday, orderToday);
        var lot = testFactory.storedLot(flow.getSortingCenter(), cell, LotStatus.CREATED);
        testFactory.sortToLot(orderToday, "11", lot, flow.getUser());
        testFactory.sortToLot(orderToday, "12", lot, flow.getUser());
        testFactory.prepareToShipLot(lot);

        Long routeTodayId = testFactory.getRouteIdForSortableFlow(routeToday.getId());
        ApiCellWithLotDto expected = new ApiCellWithLotDto(routeTodayId, routeToday.getCourierTo().getName(),
                0L, 0L, 1L, cell.getScNumber(), cell.getId());

        caller.getCellWithLotInfo(cell.getId(), routeTodayId)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        caller.bindLotToOutbound(outboundToday.getExternalId(),
                        lot.getBarcode(), routeTodayId)
                .andExpect(status().isOk());
        expected = new ApiCellWithLotDto(routeTodayId, routeToday.getCourierTo().getName(),
                1L, 0L, 0L, cell.getScNumber(), cell.getId());

        caller.getCellWithLotInfo(cell.getId(), routeTodayId)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expected)));
        tomorrowAthHour(2); //наступили новые сутки
        var orderTomorrow = testFactory.create(
                order(flow.getSortingCenter(), "2")
                        .places("21", "22")
                        .deliveryService(deliveryService).dsType(DeliveryServiceType.TRANSIT).build()
        ).updateShipmentDate(LocalDate.now(clock)).acceptPlaces("21", "22").sortPlaces("21", "22").get();
        var newRoute = testFactory.findOutgoingCourierRoute(orderTomorrow).orElseThrow().allowReading();
        var cell2 = testFactory.determineRouteCell(newRoute, orderTomorrow);
        assertThat(cell).isEqualTo(cell2);
        var lot2 = testFactory.storedLot(flow.getSortingCenter(), cell2, LotStatus.CREATED);
        testFactory.sortToLot(orderTomorrow, "21", lot2, flow.getUser());
        testFactory.sortToLot(orderTomorrow, "22", lot2, flow.getUser());
        testFactory.prepareToShipLot(lot2);

        Long newRouteId = testFactory.getRouteIdForSortableFlow(newRoute.getId());
        expected = new ApiCellWithLotDto(newRouteId, routeToday.getCourierTo().getName(),
                0L, 0L, 1L, cell.getScNumber(), cell.getId());
        caller.getCellWithLotInfo(cell.getId(), newRouteId)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expected)));

        caller.getCellWithLotInfo(cell.getId(), newRouteId)
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string(JacksonUtil.toString(expected)));
    }

    private void tomorrowAthHour(int hour) {
        ZoneId systemZone = ZoneId.systemDefault();
        TestFactory.setupMockClock(clock, DateTimeUtil.tomorrowAtHour(hour,
                clock,
                systemZone.getRules().getOffset(Instant.now(clock))));
    }
    private String buildErrMsg(Outbound scToScOutboundStrange, Courier courier) {
        return String.format("Лот не может быть отгружен в рамках отгрузки %s. Направление маршрута: %d:%s.",
                scToScOutboundStrange.getExternalId(), courier.getId(), courier.getName());
    }

}
