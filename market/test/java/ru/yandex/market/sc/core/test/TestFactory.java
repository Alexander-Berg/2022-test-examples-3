package ru.yandex.market.sc.core.test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.mockito.MemCachedClientFactoryMock;
import ru.yandex.market.logistic.api.client.LogisticApiClientFactory;
import ru.yandex.market.logistic.api.model.common.OrderTransferCode;
import ru.yandex.market.logistic.api.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.CargoType;
import ru.yandex.market.logistic.api.model.fulfillment.Delivery;
import ru.yandex.market.logistic.api.model.fulfillment.DeliveryType;
import ru.yandex.market.logistic.api.model.fulfillment.Email;
import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.logistic.api.model.fulfillment.PartnerCode;
import ru.yandex.market.logistic.api.model.fulfillment.PartnerInfo;
import ru.yandex.market.logistic.api.model.fulfillment.PaymentType;
import ru.yandex.market.logistic.api.model.fulfillment.Person;
import ru.yandex.market.logistic.api.model.fulfillment.Phone;
import ru.yandex.market.logistic.api.model.fulfillment.PhysicalPersonSender;
import ru.yandex.market.logistic.api.model.fulfillment.Recipient;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.ReturnType;
import ru.yandex.market.logistic.api.model.fulfillment.request.CreateOrderRequest;
import ru.yandex.market.logistic.api.model.fulfillment.request.entities.restricted.CreateOrderRestrictedData;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.sc.core.dbqueue.ScQueueType;
import ru.yandex.market.sc.core.domain.cargo.Cargo;
import ru.yandex.market.sc.core.domain.cargo.CargoCommandService;
import ru.yandex.market.sc.core.domain.cell.CellCommandService;
import ru.yandex.market.sc.core.domain.cell.CellCreator;
import ru.yandex.market.sc.core.domain.cell.CellField;
import ru.yandex.market.sc.core.domain.cell.event.CellCreatedEvent;
import ru.yandex.market.sc.core.domain.cell.model.ApiCellForRouteBaseDto;
import ru.yandex.market.sc.core.domain.cell.model.CellStatus;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.policy.CellPolicy;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.cell.repository.CellRepository;
import ru.yandex.market.sc.core.domain.client_return.ClientReturnService;
import ru.yandex.market.sc.core.domain.client_return.repository.ClientReturnBarcodePrefix;
import ru.yandex.market.sc.core.domain.courier.CourierCommandService;
import ru.yandex.market.sc.core.domain.courier.CourierQueryService;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.courier.repository.CourierMapper;
import ru.yandex.market.sc.core.domain.courier.repository.CourierRepository;
import ru.yandex.market.sc.core.domain.courier.shift.repository.CourierShift;
import ru.yandex.market.sc.core.domain.courier.shift.repository.CourierShiftRepository;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryService;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceIntakeSchedule;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServicePropertyRepository;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServicePropertySource;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceRepository;
import ru.yandex.market.sc.core.domain.flow.repository.Flow;
import ru.yandex.market.sc.core.domain.flow.repository.FlowRepository;
import ru.yandex.market.sc.core.domain.flow.repository.FlowSystemName;
import ru.yandex.market.sc.core.domain.flow_operation.repository.FlowOperation;
import ru.yandex.market.sc.core.domain.inbound.InboundCommandService;
import ru.yandex.market.sc.core.domain.inbound.InboundFacade;
import ru.yandex.market.sc.core.domain.inbound.model.CreateInboundRegistryOrderRequest;
import ru.yandex.market.sc.core.domain.inbound.model.CreateInboundRegistrySortableRequest;
import ru.yandex.market.sc.core.domain.inbound.model.InboundAvailableApiAction;
import ru.yandex.market.sc.core.domain.inbound.model.InboundCreateRequest;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.model.LinkToInboundRequestDto;
import ru.yandex.market.sc.core.domain.inbound.model.PutCarInfoRequest;
import ru.yandex.market.sc.core.domain.inbound.model.RegistryUnitType;
import ru.yandex.market.sc.core.domain.inbound.repository.BoundRegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus;
import ru.yandex.market.sc.core.domain.inbound.repository.Registry;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryOrder;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortable;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistrySortableRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.RegistryType;
import ru.yandex.market.sc.core.domain.location.LocationCreateRequest;
import ru.yandex.market.sc.core.domain.location.repository.Location;
import ru.yandex.market.sc.core.domain.location.repository.LocationRepository;
import ru.yandex.market.sc.core.domain.logistic_point.model.TargetLogisticPoint;
import ru.yandex.market.sc.core.domain.logistic_point.repository.TargetLogisticPointRepository;
import ru.yandex.market.sc.core.domain.lot.LotCommandService;
import ru.yandex.market.sc.core.domain.lot.model.ApiSortableDto;
import ru.yandex.market.sc.core.domain.lot.model.CreateLotRequest;
import ru.yandex.market.sc.core.domain.lot.repository.LotRepository;
import ru.yandex.market.sc.core.domain.lot.repository.LotSize;
import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.measurements.repository.Measurements;
import ru.yandex.market.sc.core.domain.measurements.repository.MeasurementsRepository;
import ru.yandex.market.sc.core.domain.movement_courier.model.MovementCourierRequest;
import ru.yandex.market.sc.core.domain.movement_courier.repository.MovementCourier;
import ru.yandex.market.sc.core.domain.movement_courier.repository.MovementCourierRepository;
import ru.yandex.market.sc.core.domain.operation.repository.Operation;
import ru.yandex.market.sc.core.domain.operation.repository.OperationRepository;
import ru.yandex.market.sc.core.domain.operation.repository.OperationSystemName;
import ru.yandex.market.sc.core.domain.operation_log.event.OperationLogEvent;
import ru.yandex.market.sc.core.domain.operation_log.model.OperationLogRequest;
import ru.yandex.market.sc.core.domain.operation_log.model.OperationLogResult;
import ru.yandex.market.sc.core.domain.order.AcceptService;
import ru.yandex.market.sc.core.domain.order.OrderCommandService;
import ru.yandex.market.sc.core.domain.order.OrderNonBlockingQueryService;
import ru.yandex.market.sc.core.domain.order.OrderQueryService;
import ru.yandex.market.sc.core.domain.order.PreShipService;
import ru.yandex.market.sc.core.domain.order.SortService;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderDto;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.order.model.ApiPlaceDto;
import ru.yandex.market.sc.core.domain.order.model.CreateClientReturnRequest;
import ru.yandex.market.sc.core.domain.order.model.CreateReturnRequest;
import ru.yandex.market.sc.core.domain.order.model.OrderCreateRequest;
import ru.yandex.market.sc.core.domain.order.model.OrderIdResponse;
import ru.yandex.market.sc.core.domain.order.model.OrderLike;
import ru.yandex.market.sc.core.domain.order.model.OrderReturnType;
import ru.yandex.market.sc.core.domain.order.model.OrderScRequest;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.model.UpdateReturnWarehouseRequestDto;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderFFStatusHistoryItem;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderFFStatusHistoryItemRepository;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderItem;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderMapper;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.order.sender_verification.OrderSenderVerificationRepository;
import ru.yandex.market.sc.core.domain.outbound.OutboundCommandService;
import ru.yandex.market.sc.core.domain.outbound.OutboundFacade;
import ru.yandex.market.sc.core.domain.outbound.OutboundQueryService;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundCreateRequest;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundIdentifier;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundIdentifierType;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundType;
import ru.yandex.market.sc.core.domain.outbound.model.partner.OutboundPartnerDto;
import ru.yandex.market.sc.core.domain.outbound.repository.Outbound;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundRepository;
import ru.yandex.market.sc.core.domain.outbound.repository.OutboundStatus;
import ru.yandex.market.sc.core.domain.place.FfApiPlaceService;
import ru.yandex.market.sc.core.domain.place.PlaceCommandService;
import ru.yandex.market.sc.core.domain.place.PlaceNonBlockingQueryService;
import ru.yandex.market.sc.core.domain.place.PlaceRouteSoService;
import ru.yandex.market.sc.core.domain.place.model.PlaceId;
import ru.yandex.market.sc.core.domain.place.model.PlaceScRequest;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceHistory;
import ru.yandex.market.sc.core.domain.place.repository.PlaceHistoryRepository;
import ru.yandex.market.sc.core.domain.place.repository.PlacePartnerCode;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.place.repository.SortableFlowStage;
import ru.yandex.market.sc.core.domain.pool.repository.Pool;
import ru.yandex.market.sc.core.domain.pool.repository.PoolRepository;
import ru.yandex.market.sc.core.domain.process.repository.Process;
import ru.yandex.market.sc.core.domain.process.repository.ProcessFlow;
import ru.yandex.market.sc.core.domain.process.repository.ProcessRepository;
import ru.yandex.market.sc.core.domain.route.RouteCommandService;
import ru.yandex.market.sc.core.domain.route.RouteNonBlockingQueryService;
import ru.yandex.market.sc.core.domain.route.RouteQueryService;
import ru.yandex.market.sc.core.domain.route.jdbc.RouteFinishJdbcRepository;
import ru.yandex.market.sc.core.domain.route.model.RouteDocumentType;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishByCellsRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishLotPalletRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishOrderCreateRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishPlaceCreateRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteFinishRequest;
import ru.yandex.market.sc.core.domain.route.model.RouteOrdersFilter;
import ru.yandex.market.sc.core.domain.route.model.RouteType;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.route.repository.RouteFinish;
import ru.yandex.market.sc.core.domain.route.repository.RouteFinishOrder;
import ru.yandex.market.sc.core.domain.route.repository.RouteFinishPlace;
import ru.yandex.market.sc.core.domain.route.repository.RouteFinishPlaceRepository;
import ru.yandex.market.sc.core.domain.route.repository.RouteRepository;
import ru.yandex.market.sc.core.domain.route_so.Routable;
import ru.yandex.market.sc.core.domain.route_so.RouteSoCommandService;
import ru.yandex.market.sc.core.domain.route_so.RouteSoMigrationHelper;
import ru.yandex.market.sc.core.domain.route_so.model.RouteDestinationType;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSo;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoRepository;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoSite;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoSiteRepository;
import ru.yandex.market.sc.core.domain.scan.ScanService;
import ru.yandex.market.sc.core.domain.scan.model.AcceptLotRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SaveVGHRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogContext;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLotService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableBarcodeSeq;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.CrossDockMapping;
import ru.yandex.market.sc.core.domain.sorting_center.repository.CrossDockMappingRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.PartnerMappingGroup;
import ru.yandex.market.sc.core.domain.sorting_center.repository.PartnerMappingGroupRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPartner;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPartnerRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterProperty;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertyRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertySource;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.stage.StageLoader;
import ru.yandex.market.sc.core.domain.user.UserCommandService;
import ru.yandex.market.sc.core.domain.user.model.UserRole;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.user.repository.UserRepository;
import ru.yandex.market.sc.core.domain.warehouse.model.WarehouseType;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseProperty;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehousePropertyRepository;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehousePropertySource;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseRepository;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseSchedule;
import ru.yandex.market.sc.core.domain.zone.ZoneCommandService;
import ru.yandex.market.sc.core.domain.zone.ZoneType;
import ru.yandex.market.sc.core.domain.zone.model.PartnerZoneRequestDto;
import ru.yandex.market.sc.core.domain.zone.repository.Zone;
import ru.yandex.market.sc.core.domain.zone.repository.ZoneRepository;
import ru.yandex.market.sc.core.resolver.dto.ScContext;
import ru.yandex.market.sc.core.util.ScDateUtils;
import ru.yandex.market.sc.core.util.ScMetrics;
import ru.yandex.market.sc.internal.model.CourierDto;
import ru.yandex.market.sc.internal.model.LocationDto;
import ru.yandex.market.sc.internal.model.WarehouseDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.sc.core.domain.client_return.ClientReturnService.DELIVERY_SERVICE_YA_ID;
import static ru.yandex.market.sc.core.domain.process.ProcessQueryService.COMMON_PROCESS_SYSTEM_NAME;

/**
 * @author valter
 */
@AllowRouteFieldReading
public class TestFactory {

    public static final ObjectMapper SC_OBJECT_MAPPER = LogisticApiClientFactory.createXmlMapper()
            .setSerializationInclusion(NON_EMPTY);

    public static final long SC_ID = 12L;
    public static final long SC_ID_2 = 13L;
    public static final String SC_PARTNER_ID = "21312983";
    public static final String SC_PARTNER_ID_2 = "21312984";
    public static final String SC_YANDEX_ID = "456439232";
    public static final String SC_YANDEX_ID_2 = "456439233";
    public static final long USER_UID_LONG = 123L;
    public static final String USER_UID = Long.toString(USER_UID_LONG);
    public static final String WAREHOUSE_YANDEX_ID = "324234234-2";

    @Autowired
    OrderNonBlockingQueryService orderNonBlockingQueryService;
    @Autowired
    PlaceNonBlockingQueryService placeNonBlockingQueryService;
    @Autowired
    RouteSoRepository routeSoRepository;
    @Autowired
    RouteNonBlockingQueryService routeNonBlockingQueryService;
    @Autowired
    RouteQueryService routeQueryService;
    @Autowired
    SortingCenterPartnerRepository sortingCenterPartnerRepository;
    @Autowired
    SortingCenterRepository sortingCenterRepository;
    @Autowired
    SortingCenterPropertyRepository sortingCenterPropertyRepository;
    @Autowired
    ZoneCommandService zoneCommandService;
    @Autowired
    DeliveryServiceRepository deliveryServiceRepository;
    @Autowired
    DeliveryServicePropertyRepository deliveryServicePropertyRepository;
    @Autowired
    DeliveryServicePropertySource deliveryServicePropertySource;
    @Autowired
    SortingCenterPropertySource sortingCenterPropertySource;
    @Autowired
    WarehouseRepository warehouseRepository;
    @Autowired
    CourierRepository courierRepository;
    @Autowired
    CourierShiftRepository courierShiftRepository;
    @Autowired
    CourierQueryService courierQueryService;
    @Autowired
    CourierCommandService courierCommandService;
    @Autowired
    CellCommandService cellCommandService;
    @Autowired
    CellCreator cellCreator;
    @Autowired
    CellRepository cellRepository;
    @Autowired
    OrderQueryService orderQueryService;
    @Autowired
    OrderCommandService orderCommandService;
    @Autowired
    PlaceRouteSoService placeRouteSoService;
    @Autowired
    PreShipService preShipService;
    @Autowired
    FfApiPlaceService ffApiPlaceService;
    @Autowired
    AcceptService acceptService;
    @Autowired
    SortService sortService;
    @Autowired
    RouteCommandService routeCommandService;
    @Autowired
    RouteSoCommandService routeSoCommandService;
    @Autowired
    RouteSoSiteRepository routeSoSiteRepository;
    @Autowired
    PlaceCommandService placeCommandService;
    @Autowired
    RouteRepository routeRepository;


    @Autowired
    RouteFinishJdbcRepository routeFinishJdbcRepository;
    @Autowired
    RouteFinishPlaceRepository routeFinishPlaceRepository;
    @Autowired
    PlaceRepository placeRepository;
    @Autowired
    ScOrderRepository scOrderRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserCommandService userCommandService;
    @Autowired
    Clock clock;
    @Autowired
    WarehousePropertyRepository warehousePropertyRepository;
    @Autowired
    CellPolicy cellPolicy;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    InboundRepository inboundRepository;
    @Autowired
    OutboundRepository outboundRepository;
    @Autowired
    BoundRegistryRepository boundRegistryRepository;
    @Autowired
    RegistryRepository registryRepository;
    @Autowired
    SortableRepository sortableRepository;
    @Autowired
    RegistrySortableRepository registrySortableRepository;
    @Autowired
    ZoneRepository zoneRepository;
    @Autowired
    LocationRepository locationRepository;
    @Autowired
    MeasurementsRepository measurementsRepository;
    @Autowired
    InboundCommandService inboundCommandService;
    @Autowired
    InboundFacade inboundFacade;
    @Autowired
    OutboundCommandService outboundCommandService;
    @Autowired
    OutboundQueryService outboundQueryService;
    @Autowired
    LotCommandService lotCommandService;
    @Autowired
    SortableLotService sortableLotService;
    @Autowired
    RouteSoMigrationHelper routeSoMigrationHelper;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    LotRepository lotRepository;
    @Autowired
    MovementCourierRepository movementCourierRepository;
    @Autowired
    EntityManager entityManager;
    @Autowired
    MemCachedClientFactoryMock memCachedClientFactoryMock;
    @Autowired
    TargetLogisticPointRepository targetLogisticPointRepository;
    @Autowired
    ScOrderFFStatusHistoryItemRepository statusHistoryItemRepository;
    @Autowired
    OutboundFacade outboundFacade;
    @Autowired
    ScanService scanService;
    @Autowired
    WarehousePropertySource warehousePropertySource;
    @Autowired
    PoolRepository poolRepository;
    @Autowired
    SortableBarcodeSeq sortableBarcodeSeq;
    @Autowired
    ProcessRepository processRepository;
    @Autowired
    FlowRepository flowRepository;
    @Autowired
    OperationRepository operationRepository;
    @Autowired
    PartnerMappingGroupRepository partnerMappingGroupRepository;
    @Autowired
    CrossDockMappingRepository crossDockMappingRepository;
    @Autowired
    OrderSenderVerificationRepository orderSenderVerificationRepository;
    @Autowired
    private ObjectMapper jacksonObjectMapper;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private PlaceHistoryRepository placeHistoryRepository;
    @Autowired
    private CargoCommandService cargoCommandService;


    public static final LocationDto MOCK_WAREHOUSE_LOCATION = LocationDto.builder()
            .country("Россия")
            .region("Москва и Московская область")
            .locality("Котельники")
            .build();

    private static long tokenId = 0;

    public static Logger mockScMetricsLogger() {
        var mock = mock(Logger.class);
        setPrivateStaticField(ScMetrics.class, "keyValueMetric", mock);
        return mock;
    }

    public static void unmockScMetricsLogger() {
        setPrivateStaticField(ScMetrics.class, "keyValueMetric",
                LoggerFactory.getLogger("KeyValueMetricLogger"));
    }

    @SuppressWarnings("SameParameterValue")
    @SneakyThrows
    private static synchronized <T> void setPrivateStaticField(Class<T> c, String fieldName, Object value) {
        var field = c.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(c, value);
        field.setAccessible(false);
    }

    private static String generateToken() {
        return "secret_token" + tokenId++;
    }

    public static Warehouse warehouse() {
        return warehouse(null);
    }

    public static Warehouse warehouse(@Nullable String yandexId) {
        return warehouse(yandexId, yandexId, WarehouseType.SORTING_CENTER);
    }

    public static Warehouse warehouse(@Nullable String yandexId, boolean isDropoff) {
        return warehouse(yandexId, yandexId, isDropoff ? WarehouseType.DROPOFF : WarehouseType.SORTING_CENTER);
    }

    public static Warehouse warehouse(@Nullable String yandexId, String partnerId, WarehouseType whType) {
        return warehouse(yandexId, partnerId, whType, "123");
    }

    public static boolean bindCellsToRouteSo() {
        return SortableFlowSwitcherExtension.useNewRouteSoStage1_2();
    }

    public static boolean sortWithRouteSo() {
        return SortableFlowSwitcherExtension.useNewRouteSoStage2();
    }

    public Routable convertToRouteSoIfneeded(Route route) {
        RouteSoMigrationHelper.allowRouteReading();

        Routable routable;
        if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
            routable = getRouteSo(route);
        } else {
            routable = route;
        }

        RouteSoMigrationHelper.revokeRouteReadingPermission();
        return routable;
    }


    public long getRouteIdForSortableFlow(Route route) {
        RouteSoMigrationHelper.allowRouteReading();

        Long id;
        if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
            id = getRouteSo(route).getId();
        } else {
            id = route.getId();
        }

        RouteSoMigrationHelper.revokeRouteReadingPermission();
        return id;
    }

    /**
     * Отдает RouteSo или Route в зависимости от ключеного флоу
     */
    public Routable getRoutable(Route route) {
        RouteSoMigrationHelper.allowRouteReading();

        Routable routable;
        if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
            routable = getRouteSo(route);
        } else {
            routable = routeRepository.findById(route.getId()).orElse(null);
        }

        RouteSoMigrationHelper.revokeRouteReadingPermission();
        return routable;
    }

    public Routable getRoutable(Long routeId, SortingCenter sortingCenter) {
        return routeSoMigrationHelper.getRoutable(routeId, sortingCenter);
    }

    public long getRouteIdForSortableFlow(long routeId) {
        if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
            return getRouteSo(routeRepository.findByIdOrThrow(routeId)).getId();
        } else {
            return routeId;
        }
    }

    public RouteSo getRouteSo(Route route) {
        ru.yandex.market.sc.core.domain.route_so.model.RouteType type = null;
        Long destinationId = null;
        RouteSoMigrationHelper.allowRouteReading();
        switch (route.getType()) {
            case INCOMING_WAREHOUSE -> {
                type = ru.yandex.market.sc.core.domain.route_so.model.RouteType.IN_DIRECT;
                destinationId = route.getWarehouseFrom().getId();
            }
            case OUTGOING_WAREHOUSE -> {
                type = ru.yandex.market.sc.core.domain.route_so.model.RouteType.OUT_RETURN;
                destinationId = route.getWarehouseTo().getId();
            }
            case INCOMING_COURIER -> {
                type = ru.yandex.market.sc.core.domain.route_so.model.RouteType.IN_RETURN;
                destinationId = route.getCourierFrom().getId();
            }
            case OUTGOING_COURIER -> {
                type = ru.yandex.market.sc.core.domain.route_so.model.RouteType.OUT_DIRECT;
                destinationId = route.getCourierTo().getId();
            }
        }

        SortingCenter sortingCenter = route.getSortingCenter();
        Instant expectedTime = ScDateUtils.toNoon(route.getExpectedDate());
        RouteSoMigrationHelper.revokeRouteReadingPermission();


        return routeSoRepository
                .getIntersecting(
                        sortingCenter,
                        type,
                        destinationId,
                        expectedTime,
                        expectedTime
                ).stream().findAny().orElseThrow();
    }

    public Set<RouteSo> getRoutesSo(Route route) {
        ru.yandex.market.sc.core.domain.route_so.model.RouteType type = null;
        Long destinationId = null;
        RouteSoMigrationHelper.allowRouteReading();
        switch (route.getType()) {
            case INCOMING_WAREHOUSE -> {
                type = ru.yandex.market.sc.core.domain.route_so.model.RouteType.IN_DIRECT;
                destinationId = route.getWarehouseFrom().getId();
            }
            case OUTGOING_WAREHOUSE -> {
                type = ru.yandex.market.sc.core.domain.route_so.model.RouteType.OUT_RETURN;
                destinationId = route.getWarehouseTo().getId();
            }
            case INCOMING_COURIER -> {
                type = ru.yandex.market.sc.core.domain.route_so.model.RouteType.IN_RETURN;
                destinationId = route.getCourierFrom().getId();
            }
            case OUTGOING_COURIER -> {
                type = ru.yandex.market.sc.core.domain.route_so.model.RouteType.OUT_DIRECT;
                destinationId = route.getCourierTo().getId();
            }
        }

        SortingCenter sortingCenter = route.getSortingCenter();
        Instant expectedTime = ScDateUtils.toNoon(route.getExpectedDate());
        RouteSoMigrationHelper.revokeRouteReadingPermission();


        return routeSoRepository
                .getIntersecting(
                        sortingCenter,
                        type,
                        destinationId,
                        expectedTime,
                        expectedTime
                );
    }


    public static Warehouse warehouse(@Nullable String yandexId, String partnerId, WarehouseType whType,
                                      String logisticPointId) {
        var isDropoff = WarehouseType.DROPOFF.equals(whType);
        var timeFrom = OffsetTime.ofInstant(Instant.ofEpochMilli(1L), ZoneId.of("UTC"));
        var timeTo = OffsetTime.ofInstant(Instant.ofEpochMilli(1L), ZoneId.of("UTC"));
        return new Warehouse(
                yandexId == null ? "324234234-2" : yandexId,
                logisticPointId,
                partnerId,
                yandexId == null ? "324234234-2" : yandexId,
                isDropoff,
                location(),
                "ООО Ромашка-Склад",
                "Вася Васечкин",
                List.of("+72349230432", "+7239473294"),
                "от последнего хутора по дороге на юг прямо и налево на втором повороте. А потом назад",
                List.of(
                        new WarehouseSchedule(DayOfWeek.MONDAY, timeFrom, timeTo),
                        new WarehouseSchedule(DayOfWeek.TUESDAY, timeFrom, timeTo)
                ),
                whType
        );
    }

    public static ApiCellForRouteBaseDto cellDto(Cell cell, boolean emptyCell, long lotCount) {
        return new ApiCellForRouteBaseDto(cell, emptyCell, lotCount);
    }

    public void setLotStatus(long lotId, LotStatus status) {
        jdbcTemplate.update("update lot set status = ? where id = ?", status.name(), lotId);
    }

    public static SortingCenterPartner sortingCenterPartner() {
        return sortingCenterPartner(150L, generateToken());
    }

    public static SortingCenterPartner sortingCenterPartner(long id, String token) {
        return new SortingCenterPartner(id, token);
    }

    public static SortingCenter sortingCenter() {
        return sortingCenter(SC_ID, SC_PARTNER_ID, "ООО Яндекс.Маркет", generateToken());
    }

    public static SortingCenter sortingCenter2() {
        return sortingCenter(
                TestFactory.SortingCenterParams.builder()
                        .id(SC_ID_2)
                        .partnerName(SC_PARTNER_ID_2)
                        .yandexId(SC_YANDEX_ID_2)
                        .sortingCenterPartnerId(null)
                        .build()
        );
    }

    public static SortingCenter sortingCenter(Long sortingCenterPartnerId) {
        return sortingCenter(SC_ID, SC_PARTNER_ID, "ООО Яндекс.Маркет", generateToken(), sortingCenterPartnerId);
    }

    public static Zone zone(SortingCenter sortingCenter) {
        return zone(sortingCenter, "danger");
    }

    public static Zone zone(SortingCenter sortingCenter, String name) {
        return zone(sortingCenter, name, emptyList());
    }

    public static Zone zone(SortingCenter sortingCenter, String name, List<Process> proccesses) {
        return new Zone(sortingCenter, name, ZoneType.DEFAULT, proccesses, null);
    }

    public static Zone workstation(SortingCenter sortingCenter, String name, Long zoneParentId) {
        return new Zone(sortingCenter, name, ZoneType.WORKSTATION, emptyList(), zoneParentId);
    }

    public static SortingCenter sortingCenter(long id, String partnerId, String partnerName, String token) {
        return sortingCenter(id, partnerId, SC_YANDEX_ID + id, partnerName, token, null);
    }

    public static SortingCenter sortingCenter(long id, String partnerId, String partnerName,
                                              String token, Long sortingCenterPartnerId) {
        return sortingCenter(id, partnerId, SC_YANDEX_ID + id, partnerName, token, sortingCenterPartnerId);
    }

    public static SortingCenter sortingCenter(long id, String partnerId, String partnerName,
                                              String token, Long sortingCenterPartnerId, String regionTagSuffix,
                                              String yandexId) {
        return sortingCenter(id, partnerId, yandexId, partnerName, token, sortingCenterPartnerId, regionTagSuffix);
    }

    public static SortingCenter sortingCenter(long id, String partnerId, String yandexId,
                                              String partnerName, String token, Long sortingCenterPartnerId) {
        return new SortingCenter(id, "Ул Ленина, 4385/3", token, partnerId, yandexId,
                partnerName, "СЦ " + partnerId, null, sortingCenterPartnerId);
    }

    public static SortingCenter sortingCenter(long id, String partnerId, String yandexId,
                                              String partnerName, String token, Long sortingCenterPartnerId,
                                              String regionTagSuffix) {
        return new SortingCenter(id, "Ул Ленина, 4385/3", token, partnerId, yandexId,
                partnerName, "СЦ " + partnerId, regionTagSuffix, sortingCenterPartnerId);
    }

    public static SortingCenter sortingCenter(SortingCenterParams params) {
        var partnerId = "partner-" + params.getId();
        return new SortingCenter(
                params.getId(),
                "Ул Ленина, 4385/3",
                generateToken(),
                partnerId,
                params.getYandexId(),
                params.getPartnerName(),
                "СЦ " + partnerId,
                null,
                params.getSortingCenterPartnerId()
        );
    }

    public static MovementCourier movementCourier(
            String externalId,
            String name,
            String legalName,
            @Nullable String carNumber,
            @Nullable Long uid
    ) {
        return new MovementCourier(externalId, name, legalName, carNumber, uid);
    }

    public static MovementCourier movementCourier() {
        return movementCourier("ext_9", "Vasya", "legal_name", "А 511АА 777", 212_85_06L);
    }

    public static MovementCourier movementCourier(Long uid) {
        return movementCourier("ext" + uid, "Vasya" + uid, "legal_name" + uid, "А 511АА 777" + uid, uid);
    }

    public MovementCourier getMovementCourier(Long uid) {
        return movementCourierRepository.findByUid(uid).stream().findFirst().orElse(null);
    }

    public MovementCourier storedMovementCourier(Long uid) {
        return movementCourierRepository.save(movementCourier(uid));
    }

    public MovementCourier storedMovementCourier(MovementCourier movementCourier) {
        return movementCourierRepository.save(movementCourier);
    }

    public static LocationCreateRequest locationCreateRequest() {
        return new LocationCreateRequest(
                "Россия", "Рус", "Ярославская обл", "Ярославская область",
                "Ярославский р-н", "Ярославль", "ул Ленина", "11", "2", "3",
                "12", "124133242", "23434", 11L, "метро", BigDecimal.ONE, BigDecimal.valueOf(1.34d), 1L
        );
    }

    public static Location location() {
        return new Location(
                "Россия", "Рус", "Ярославская обл", "Ярославская область",
                "Ярославский р-н", "Ярославль", "ул Ленина", "11", "2", "3",
                "12", "124133242", "23434", 11L, "метро", BigDecimal.ONE, BigDecimal.valueOf(1.34d),
                1L
        );
    }

    public static Measurements measurements() {
        return new Measurements(
                121L, 56756L, 32432L, BigDecimal.TEN, BigDecimal.valueOf(435345.123d),
                BigDecimal.valueOf(0.1d)
        );
    }

    public static DeliveryService deliveryService() {
        return deliveryService("32423");
    }

    public static DeliveryService deliveryService(String yandexId) {
        return new DeliveryService(
                yandexId, "sadae", "Лучшая служба доставки в мире, мы доставим все вовсюда",
                List.of("+72349230432", "+7239473294"), "Петр Петров"
        );
    }

    private static ScOrderItem scOrderItem(String name) {
        return new ScOrderItem(
                name,
                213L,
                BigDecimal.valueOf(123312.2312d),
                CargoType.AEROSOLS_AND_GASES,
                "213123",
                49357834L,
                name + "_id"
        );
    }

    public static Order ffOrder(String token) {
        return ffOrder("ff_create_order.xml", token);
    }

    @SneakyThrows
    public static Order ffOrder(String fileName, String token) {
        String rawInput = String.format(IOUtils.toString(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(fileName)
                ),
                StandardCharsets.UTF_8
        ), token);

        RequestWrapper<CreateOrderRequest> createOrderRequest = SC_OBJECT_MAPPER.readValue(rawInput,
                new TypeReference<>() {
                });

        return createOrderRequest.getRequest().getOrder();
    }

    @SneakyThrows
    public static Order ffOrderWithParams(String fileName, String... values) {
        String rawInput = String.format(IOUtils.toString(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(fileName)
                ),
                StandardCharsets.UTF_8
        ), values);

        RequestWrapper<CreateOrderRequest> createOrderRequest = SC_OBJECT_MAPPER.readValue(rawInput,
                new TypeReference<>() {
                });

        return createOrderRequest.getRequest().getOrder();
    }

    public Cell cell(SortingCenter sortingCenter) {
        return new Cell(sortingCenter, "1", CellType.BUFFER);
    }

    private Cell cell(SortingCenter sortingCenter, String scNumber) {
        return new Cell(sortingCenter, scNumber, CellType.BUFFER);
    }

    private Cell cell(SortingCenter sortingCenter, CellStatus status) {
        return new Cell(sortingCenter, "1", status, CellType.BUFFER, CellSubType.DEFAULT, 1L);
    }

    private Cell cell(SortingCenter sortingCenter, Zone zone, boolean deleted) {
        return cell(sortingCenter, CellType.BUFFER,
                CellSubType.DEFAULT, zone, deleted);
    }

    private Cell cell(SortingCenter sortingCenter, CellType type, CellSubType subType, Zone zone, boolean deleted) {
        return cell(sortingCenter, type, subType, zone, deleted, "1");
    }

    private Cell cell(
            SortingCenter sortingCenter, CellType type, CellSubType subType, Zone zone, boolean deleted,
            String scNumber) {
        return new Cell(sortingCenter, scNumber, CellStatus.ACTIVE, type,
                subType, deleted, null, null, zone, null);
    }

    private Cell cell(SortingCenter sortingCenter, CellType type, Warehouse warehouse, String scNumber) {
        return new Cell(sortingCenter, scNumber, CellStatus.ACTIVE, type, CellSubType.DEFAULT,
                false, warehouse.getYandexId(), null, null, null);
    }

    @Transactional
    public Cell cell() {
        return cell(sortingCenterRepository.save(sortingCenter()));
    }

    @Transactional
    public Courier courier() {
        return courier(11L);
    }

    public List<MovementCourier> getMovementCouriers() {
        return movementCourierRepository.findAll();
    }

    private Courier courier(long id) {
        return new Courier(
                id,
                "Иван Иванов", "234234", "Лада седан, баклажан", "+7123456789", "Рога и копыта", null
        );
    }

    private Courier courier(long id, String name) {
        return new Courier(
                id,
                name, "234234", "Лада седан, баклажан", "+7123456789", "Рога и копыта", null
        );
    }

    private Courier courier(long id, long deliveryServiceId) {
        return new Courier(
                id,
                "Иван Иванов", "234234", "Лада седан, баклажан", "+7123456789", "Рога и копыта",
                deliveryServiceId
        );
    }

    private Courier emptyCourier(long id, String name) {
        return new Courier(id, name, null, null, null, null, null);
    }

    @Transactional
    public DeliveryServiceIntakeSchedule deliveryServiceIntakeSchedule() {
        return new DeliveryServiceIntakeSchedule(
                storedDeliveryService(),
                storedSortingCenter(),
                DayOfWeek.SUNDAY,
                OffsetTime.ofInstant(clock.instant(), ZoneId.of("UTC")),
                OffsetTime.ofInstant(clock.instant().plus(8, ChronoUnit.HOURS), ZoneId.of("UTC"))
        );
    }

    @Transactional
    public Place place() {
        return place(storedSortingCenter());
    }

    @Transactional
    public Place place(SortingCenter sortingCenter) {
        return place(sortingCenter, true);
    }

    @Transactional
    public Place place(SortingCenter sortingCenter, boolean korobyte) {
        var order = createOrderForToday(sortingCenter).get();
        return new Place(
                Collections.emptyList(),
                List.of(new PlacePartnerCode("123", "12345")),
                order.getOrder(),
                "place-1-" + order.getExternalId(),
                "123", "12345",
                korobyte ? measurements() : null,
                sortingCenter,
                order.getShipmentDate(),
                order.getIncomingRouteDate(),
                order.getOutgoingRouteDate(),
                order.getCourier(),
                order.getWarehouseFrom(), order.getWarehouseReturn(), order.isMiddleMile(),
                order.getSegmentUid(),
                order.getCargoUnitId(),
                false,
                SortableStatus.AWAITING_DIRECT,
                SortableFlowStage.STAGE_2_3,
                getOrCreateAnyUser(sortingCenter),
                null,
                null
        );
    }

    public List<Place> orderPlaces(ScOrder order) {
        return placeRepository.findAllByOrderIdOrderById(order.getId());
    }

    public Place anyOrderPlace(ScOrder order) {
        return orderPlaces(order).get(0);
    }

    public Place placeById(List<Place> places, String mainPartnerCode) {
        return places.stream()
                .filter(p -> p.getMainPartnerCode().equals(mainPartnerCode))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Нет плейса с mainPartnerCode = " + mainPartnerCode));
    }

    /**
     * Возвращает коробку по данному заказу или коробки, если включен сортировочный поток в стадии 2.3
     */
    public Place getOrderLikeForRouteLookup(OrderLike placeOrOrder) {
        if (placeOrOrder.isPlace()) {
            return placeOrOrder.getPlace();
        }

        return orderPlaces(placeOrOrder.getOrder()).get(0);
    }

    public ScOrder getOrder(long id) {
        return scOrderRepository.findByIdOrThrow(id);
    }

    public ScOrderWithPlaces getOrderWithPlaces(long id) {
        return new ScOrderWithPlaces(
                scOrderRepository.findByIdOrThrow(id),
                placeRepository.findAllByOrderIdOrderById(id)
        );
    }

    public ScOrder updated(ScOrder order) {
        return scOrderRepository.findByIdOrThrow(order.getId());
    }

    public Place getPlace(long id) {
        return placeRepository.findByIdOrThrow(id);
    }

    public Place updated(Place place) {
        return placeRepository.findByIdOrThrow(place.getId());
    }

    public ScOrder findOrder(String externalId, SortingCenter sc) {
        return scOrderRepository.findAllByExternalId(externalId).stream().filter(o -> o.getSortingCenterId() ==
                sc.getId()).findAny().orElseThrow();
    }

    public Place findPlace(String orderExternalId, String placeExternalId, SortingCenter sc) {
        ScOrder order = findOrder(orderExternalId, sc);
        return orderPlace(order, placeExternalId);
    }

    public List<Place> orderPlaces(long orderId) {
        return placeRepository.findAllByOrderIdOrderById(orderId);
    }

    public Place orderPlace(long orderId) {
        List<Place> places = placeRepository.findAllByOrderIdOrderById(orderId);
        assertThat(places).hasSize(1);
        return places.get(0);
    }

    public Place orderPlace(ScOrder order, String mainPartnerCode) {
        return orderPlaces(order).stream()
                .filter(p -> Objects.equals(p.getMainPartnerCode(), mainPartnerCode))
                .findAny().orElseThrow();
    }

    public Place orderPlace(ScOrder order) {
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places).hasSize(1);
        return orderPlace(order.getId());
    }

    public PlaceScRequest placeScRequest(Place place, User user) {
        return new PlaceScRequest(PlaceId.of(place), user);
    }

    public PlaceScRequest placeScRequest(ScOrder order, User user) {
        Place place = orderPlace(order);
        return placeScRequest(place, user);
    }

    public PlaceScRequest placeScRequest(OrderScRequest orderScRequest) {
        return new PlaceScRequest(new PlaceId(orderScRequest.getId(), orderScRequest.getExternalId()),
                orderScRequest.getUser());
    }


    public Inbound getInbound(String externalId) {
        return inboundRepository.findByExternalId(externalId).orElseThrow();
    }

    public Inbound getInbound(Long id) {
        return inboundRepository.findById(id).orElseThrow();
    }

    public Cell getCell(long id) {
        return cellRepository.findByIdOrThrow(id);
    }

    @SuppressWarnings("unused")
    public RegistryOrder getRegistryOrder(String registryOrderExternalId) {
        return boundRegistryRepository.findByExternalId(registryOrderExternalId).orElseThrow();
    }

    public List<RegistryOrder> getRegistryOrdersByRegistryExternalId(Long registryId) {
        return boundRegistryRepository.findAllByRegistryId(registryId);
    }

    public Registry getRegistryById(Long registryId) {
        return registryRepository.findByIdOrThrow(registryId);
    }

    public List<Registry> getRegistryByInboundId(Long inboundId) {
        return registryRepository.findAllByInboundId(inboundId);
    }

    public List<Registry> getRegistryByOutboundId(Long outboundId) {
        return registryRepository.findAllByOutboundId(outboundId);
    }

    public List<RegistryOrder> getAllOrdersByInboundId(Long inboundId) {
        var registryList = getRegistryByInboundId(inboundId);
        List<RegistryOrder> result = new ArrayList<>();
        registryList.forEach(registry -> result.addAll(
                boundRegistryRepository.findAllByRegistryId(registry.getId())));
        return result;
    }

    public Outbound getOutbound(String externalId) {
        return outboundRepository.findByExternalId(externalId).orElseThrow();
    }

    @Transactional
    public ScOrder scOrder() {
        var sortingCenter = storedSortingCenter(13);
        var user = storedUser(123);
        var warehouse = storedWarehouse();
        var deliveryService = storedDeliveryService();
        return new ScOrder(
                List.of(
                        scOrderItem("тушенка"),
                        scOrderItem("макароны")
                ),
                sortingCenter,
                "239847234",
                ScOrderFFStatus.ORDER_CREATED_FF,
                user,
                warehouse, warehouse,
                deliveryService, courierRepository.save(courier()),
                LocalDate.now(clock),
                LocalDate.now(clock),
                LocalDateTime.now(clock),
                BigDecimal.valueOf(32423.3232d), BigDecimal.ZERO,
                PaymentType.CARD, DeliveryType.COURIER,
                location(), measurements(),
                BigDecimal.ONE, BigDecimal.TEN,
                null,
                null,
                "1312312", "23423435",
                "234234235", "my_tariff",
                "my_comment",
                LocalDate.now(clock), LocalDate.now(clock),
                Instant.now(clock),
                false,
                false,
                1,
                null,
                null,
                null,
                null,
                deliveryService,
                "cargo_unit_test",
                "route_segment_test"
        );
    }

    @Transactional
    public Zone storedZone(SortingCenter sortingCenter) {
        return zoneRepository.save(zone(sortingCenter));
    }

    @Transactional
    public Zone storedZone(SortingCenter sortingCenter, String name) {
        return zoneRepository.save(zone(sortingCenter, name));
    }

    @Transactional
    public Zone storedZone(SortingCenter sortingCenter, String name, List<Process> processes) {
        return zoneRepository.save(zone(sortingCenter, name, processes));
    }

    @Transactional
    public Zone storedZone(SortingCenter sortingCenter, String name, Process... processes) {
        return zoneRepository.save(zone(sortingCenter, name, Arrays.asList(processes)));
    }

    @Transactional
    public Zone storedWorkstation(SortingCenter sortingCenter, String name, Long zoneParentId, Process... processes) {
        List<Process> ps = Arrays.asList(processes);
        return zoneRepository.save(new Zone(sortingCenter, name, ZoneType.WORKSTATION, ps, zoneParentId));
    }

    @Transactional
    public Zone storedDeletedZone(SortingCenter sortingCenter) {
        Zone zone = zoneRepository.save(zone(sortingCenter));
        zone.setDeleted(true);
        return zone;
    }

    @Transactional
    public Zone storedDeletedZone(SortingCenter sortingCenter, String name) {
        Zone zone = zoneRepository.save(zone(sortingCenter, name));
        zone.setDeleted(true);
        return zone;
    }

    @Transactional
    public Zone storedDeletedWorkstation(SortingCenter sortingCenter, String name, Long zoneParentId) {
        Zone zone = zoneRepository.save(workstation(sortingCenter, name, zoneParentId));
        zone.setDeleted(true);
        return zone;
    }

    @Transactional
    public SortingCenterPartner storedSortingCenterPartner() {
        return sortingCenterPartnerRepository.save(sortingCenterPartner());
    }

    @Transactional
    public SortingCenterPartner storedSortingCenterPartner(long id, String token) {
        return sortingCenterPartnerRepository.save(sortingCenterPartner(id, token));
    }

    public List<ScOrderFFStatusHistoryItem> findOrderStatusHistoryItems(long orderId) {
        return statusHistoryItemRepository.findByOrderId(orderId);
    }

    public List<PlaceHistory> findPlaceHistory(long placeId) {
        return placeHistoryRepository.findByPlaceId(placeId);
    }

    public String bindLotToOutbound(String outboundExtId, String lotExtId, Long routeId, User user) {
        OutboundIdentifier identifier = new OutboundIdentifier(OutboundIdentifierType.EXTERNAL_ID, outboundExtId);
        return outboundFacade.bindLotToOutbound(identifier, lotExtId, routeId, new ScContext(user));
    }

    public ApiSortableDto addStampToSortableLotAndPrepare(String lotExternalId, String stampId, User user) {
        return scanService.addStampToSortableLotAndPrepare(lotExternalId, stampId, new ScContext(user,
                ScanLogContext.PALLETIZATION)
        );
    }

    public ApiSortableDto deleteStamp(String lotExternalId, String stampId, User user) {
        return scanService.deleteStamp(lotExternalId, stampId, new ScContext(user,
                ScanLogContext.PALLETIZATION)
        );
    }

    public boolean getCellFullness(long cellId) {
        return cellRepository.findByIdOrThrow(cellId).isFull();
    }

    @Data
    @Builder
    public static class SortingCenterParams {

        private long id;

        @Builder.Default
        private String partnerName = "ООО Яндекс.Маркет";

        @Builder.Default
        private String token = generateToken();

        @Builder.Default
        private Long sortingCenterPartnerId = null;

        @Builder.Default
        private String regionSuffix = null;

        @Builder.Default
        private String yandexId = SC_YANDEX_ID_2;

        private PartnerZoneRequestDto request(String name) {
            return new PartnerZoneRequestDto(name, emptyList());
        }

    }

    @Transactional
    public SortingCenter getSortingCenterById(long scId) {
        return sortingCenterRepository.findByIdOrThrow(scId);
    }

    @Transactional
    public SortingCenter storedSortingCenter() {
        var defaultSortCenter = sortingCenter();
        return sortingCenterRepository.findById(defaultSortCenter.getId())
                .orElseGet(() -> sortingCenterRepository.save(defaultSortCenter));
    }

    @Transactional
    public SortingCenter storedSortingCenter2() {
        var defaultSortCenter = sortingCenter2();
        return sortingCenterRepository.findById(defaultSortCenter.getId())
                .orElseGet(() -> sortingCenterRepository.save(defaultSortCenter));
    }

    @Transactional
    public SortingCenter storedSortingCenter(long id) {
        Optional<SortingCenter> existing = sortingCenterRepository.findById(id);
        return existing.orElseGet(() -> sortingCenterRepository.save(
                sortingCenter(id, "partner-" + id, "ООО Яндекс.Маркет", generateToken()))
        );
    }

    @Transactional
    public SortingCenter storedSortingCenterWithYandexId(long id, String yandexId) {
        return sortingCenterRepository.save(sortingCenter(
                id, "partner-" + id, yandexId, "ООО Яндекс.Маркет", generateToken(), null));
    }

    @Transactional
    public SortingCenter storedSortingCenter(long id, String partnerName) {
        return sortingCenterRepository.save(sortingCenter(id, "partner-" + id, partnerName, generateToken()));
    }

    public SortingCenter storedSortingCenter(SortingCenterParams params) {
        return sortingCenterRepository.save(
                sortingCenter(params.getId(), "partner-" + params.getId(), params.getPartnerName(),
                        params.getToken(), params.getSortingCenterPartnerId(), params.getRegionSuffix(),
                        params.getYandexId()));
    }

    @Transactional
    public User findUserByUid(long uid) {
        return userRepository.findByUid(uid).orElseThrow();
    }

    @Transactional
    public User storedUser(SortingCenter sortingCenter, long uid) {
        return storedUser(sortingCenter, uid, UserRole.STOCKMAN);
    }

    @Transactional
    public User storedUser(long uid) {
        return storedUser(storedSortingCenter(), uid, UserRole.STOCKMAN);
    }

    @Transactional
    public User storedUser(SortingCenter sortingCenter, long uid, UserRole role) {
        return storedUser(sortingCenter, uid, role, null, "Иван Иванов");
    }

    @Transactional
    public User storedUser(SortingCenter sortingCenter, long uid, UserRole role, String staffLogin) {
        return storedUser(sortingCenter, uid, role, staffLogin, "Иван Иванов");
    }

    @Transactional
    public User storedUser(SortingCenter sortingCenter, long uid, UserRole role, String staffLogin, String name) {
        var user = userRepository.findByUid(uid);

        return user.orElseGet(() ->
                userRepository.save(
                        new User(sortingCenter, uid, "valter@yandex-team.ru", name, role, staffLogin)
                ));
    }

    @Transactional
    public User getOrCreateAnyUser(SortingCenter sortingCenter) {
        return userRepository.findAllBySortingCenter(sortingCenter).stream().findAny().orElse(
                userRepository.save(new User(sortingCenter, new Random().nextLong(),
                        "anyUser@yandex-team.ru", "Any User", UserRole.ADMIN,
                        "anyUser"))
        );
    }

    @Transactional
    public User getOrCreateAnyUser(Long scId) {
        SortingCenter sortingCenter = sortingCenterRepository.findById(scId).get();
        return userRepository.findAllBySortingCenter(sortingCenter).stream().findAny().orElse(
                userRepository.save(new User(sortingCenter, new Random().nextLong(),
                        "anyUser@yandex-team.ru", "Any User", UserRole.ADMIN,
                        "anyUser"))
        );
    }

//    @Transactional
//    // Не очень валидно, но это проще, чем пробросить SC в тысячи вызовов методов, которые его используют
//    // Не должно влиять не тесты практически, наверняка
//    public User getOrCreateAnyUser() {
//        var sortingCenter = storedSortingCenter();
//        return userRepository.findAllBySortingCenter(sortingCenter).stream().findAny().orElse(
//                userRepository.save(new User(sortingCenter, new Random().nextLong(),
//                        "anyUser@yandex-team.ru", "Any User", UserRole.ADMIN,
//                        "anyUser"))
//        );
//    }


    @Transactional
    public User storedSupportUser(SortingCenter sortingCenter, long uid) {
        return storedUser(sortingCenter, uid, UserRole.SUPPORT);
    }

    @Transactional
    public SortableLot storedLot(SortingCenter sortingCenter, Cell parentCell, LotStatus status) {
        return storedLot(sortingCenter, SortableType.PALLET, parentCell, status, false, null);
    }

    @Transactional
    public SortableLot storedLot(SortingCenter sortingCenter, Cell parentCell, LotStatus status, Inbound inbound,
                                 boolean newArrived, boolean crossDock) {
        return storedLot(
                sortingCenter,
                SortableType.PALLET,
                parentCell,
                status,
                false,
                sortableBarcodeSeq.barcodeFrom(SortableType.PALLET),
                null,
                null,
                null,
                inbound,
                newArrived,
                crossDock
        );
    }

    @Transactional
    public SortableLot storedLot(String barcode, SortingCenter sortingCenter, User user, Long stageId,
                                 boolean crossDock, @Nullable Inbound inbound, @Nullable Cell parentCell) {
        var sortableLot = lotCommandService.createLot(
                new CreateLotRequest(
                        SortableType.PALLET,
                        parentCell,
                        sortingCenter,
                        false,
                        LotStatus.READY,
                        barcode,
                        null,
                        null,
                        null,
                        user,
                        inbound,
                        true,
                        crossDock)
        );
        sortableLot.setStage(StageLoader.getById(stageId), user);
        entityManager.flush();

        return sortableLot;
    }

    @Transactional
    public SortableLot storedLot(SortingCenter sortingCenter, SortableType sortableType, Cell parentCell) {
        return storedLot(sortingCenter, sortableType, parentCell, LotStatus.CREATED, false);
    }

    @Transactional
    public SortableLot storedLot(
            SortingCenter sortingCenter,
            SortableType sortableType,
            Cell parentCell,
            LotStatus status,
            boolean deleted
    ) {
        return storedLot(
                sortingCenter,
                sortableType,
                parentCell,
                status,
                deleted,
                sortableBarcodeSeq.barcodeFrom(sortableType)
        );
    }

    @Transactional
    public SortableLot storedLot(
            SortingCenter sortingCenter,
            SortableType sortableType,
            Cell parentCell,
            LotStatus status,
            boolean deleted,
            String barcode
    ) {
        return storedLot(sortingCenter, sortableType, parentCell, status, deleted, barcode, null, null, null, null,
                false, false);
    }

    @Transactional
    @SuppressWarnings("checkstyle:ParameterNumber")
    public SortableLot storedLot(
            SortingCenter sortingCenter,
            SortableType sortableType,
            Cell parentCell,
            LotStatus status,
            boolean deleted,
            String barcode,
            TargetLogisticPoint targetLogisticPoint,
            Courier courier,
            User user,
            Inbound inbound,
            boolean newArrived,
            boolean crossDock
    ) {
        if (parentCell != null) {
            validateCellType(parentCell);
        }

        var savedLot = lotCommandService.createLot(
                new CreateLotRequest(sortableType,
                        parentCell,
                        sortingCenter,
                        deleted,
                        LotStatus.CREATED,
                        barcode,
                        null,
                        targetLogisticPoint,
                        courier,
                        user,
                        inbound,
                        newArrived,
                        crossDock)
        );
        savedLot.setLotStatus(status, user);
        entityManager.flush();
        return savedLot;
    }

    @Transactional
    public void deleteLot(SortableLot lot) {
        lotCommandService.deleteLot(lot.getSortingCenter(), lot.getLotId());
    }

    private void validateCellType(Cell cell) {
        switch (cell.getType()) {
            case COURIER:
            case RETURN:
                // все ок, для них можно создавать ячейки
                break;
            case BUFFER:
                if (cell.getSubtype() != CellSubType.BUFFER_XDOC) {
                    throw new InvalidTestParameters("BUFFER ячейку можно создавать только с подтипом BUFFER_XDOC");
                }
                break;
            default:
                throw new InvalidTestParameters("Для лотов доступны только ячейки с типами COURIER, RETURN и" +
                        " BUFFER(с подтипом BUFFER_XDOC)");
        }
    }

    @Transactional
    public SortableLot getLot(long id) {
        return sortableLotService.findByLotId(id).orElse(null);
    }

    @Transactional
    public SortableLot getLot(Sortable sortable) {
        return sortableLotService.findBySortableId(sortable.getId()).orElseThrow();
    }

    @Transactional
    public void sortToLot(ScOrder order, String placeExternalId, SortableLot lot, User user) {
        scanService.sortSortable(
                new SortableSortRequestDto(order.getExternalId(), placeExternalId, lot.getBarcode()),
                new ScContext(user, ScanLogContext.SORT_LOT));
    }

    @Transactional
    public void sortToLot(Place place, SortableLot lot, User user) {
        scanService.sortSortable(
                new SortableSortRequestDto(place, lot),
                new ScContext(user, ScanLogContext.SORT_LOT));
    }

    @Transactional
    public void acceptLot(String stampId, User user) {
        ScContext ctx = ScContext.builder()
                .user(user)
                .build();
        scanService.acceptLotWithPlaces(new AcceptLotRequestDto(stampId), ctx);
    }

    @Transactional
    public SortableLot preAcceptLotWithPlaces(String barcode, SortingCenter sortingCenter, User user) {
        var lot = sortableLotService.findByExternalIdAndSortingCenter(barcode, sortingCenter).orElseThrow();
        acceptService.preAcceptLotWithPlaces(lot,  user);

        return lot;
    }

    @Transactional
    public SortableLot finishAcceptLotWithPlaces(String barcode, SortingCenter sortingCenter, User user) {
        var lot = sortableLotService.findByExternalIdAndSortingCenter(barcode, sortingCenter).orElseThrow();
        var places = placeNonBlockingQueryService.findPlacesInLot(lot);

        return acceptService.acceptLotWithPlaces(lot, places, user);
    }

    @Transactional
    public TargetLogisticPoint storedTargetLogisticPoint(Long id, String name, String address) {
        return targetLogisticPointRepository.saveAndFlush(new TargetLogisticPoint(id, name, address));
    }

    @Transactional
    public User getOrCreateStoredUser(SortingCenter sortingCenter) {
        long uid = sortingCenter.getId() * 1000000L + 321L;
        return userRepository.findAllBySortingCenter(sortingCenter).stream().filter(u -> u.getUid() == uid).findAny()
                .orElseGet(() -> storedUser(sortingCenter, uid));
    }

    @Transactional
    public User getOrCreateSupportStoredUser(SortingCenter sortingCenter) {
        return userRepository.findAllBySortingCenter(sortingCenter).stream()
                .filter(u -> u.getUid() == 4321L && u.getRole() == UserRole.SUPPORT).findAny()
                .orElseGet(() -> storedSupportUser(sortingCenter, 4321L));
    }

    @Transactional
    public Location storedLocation() {
        return locationRepository.save(location());
    }

    @Transactional
    public Measurements storedMeasurements() {
        return measurementsRepository.save(measurements());
    }

    @Transactional
    public Warehouse findWarehouseBy(String yandexId) {
        return warehouseRepository.findByYandexId(yandexId).orElseThrow();
    }

    @Transactional
    public Warehouse findWarehouseBy() {
        return warehouseRepository.findByYandexId(WAREHOUSE_YANDEX_ID).orElseThrow();
    }

    @Transactional
    public Warehouse storedWarehouse() {
        return storedWarehouse((String) null);
    }

    @Transactional
    public Warehouse storedWarehouse(String yandexId) {
        return storedWarehouse(yandexId, yandexId, WarehouseType.SORTING_CENTER);
    }

    @Transactional
    public Warehouse storedWarehouse(String yandexId, WarehouseType whType) {
        return storedWarehouse(yandexId, yandexId, whType);
    }

    @Transactional
    public Warehouse storedWarehouse(String yandexId, boolean isDropoff) {
        return storedWarehouse(yandexId, yandexId, isDropoff ? WarehouseType.DROPOFF : WarehouseType.SORTING_CENTER);
    }

    @Transactional
    public Warehouse storedWarehouse(String yandexId, String partnerId, WarehouseType whType) {
        Optional<Warehouse> existing = yandexId != null
                ? warehouseRepository.findByYandexId(yandexId)
                : warehouseRepository.findByYandexId("324234234-2");
        return existing.orElseGet(() -> warehouseRepository.save(warehouse(yandexId, partnerId, whType)));
    }

    @Transactional
    public Warehouse storedWarehouse(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }

    @Transactional
    public void updateWarehouseType(String yandexId, WarehouseType whType) {
        jdbcTemplate.update("update warehouse set type = ? where yandex_id = ?", whType.name(), yandexId);
    }

    @Transactional
    public DeliveryService storedDeliveryService() {
        return storedDeliveryService("123", true);
    }

    @SuppressWarnings("UnusedReturnValue")
    @Transactional
    public DeliveryService storedFakeReturnDeliveryService() {
        return storedDeliveryService(DELIVERY_SERVICE_YA_ID);
    }

    @Transactional
    public DeliveryService storedDeliveryService(String yandexId) {
        return deliveryServiceRepository.save(deliveryService(yandexId));
    }

    @Transactional
    public DeliveryService storedDeliveryService(String yandexId, boolean isLastMile) {
        return storedDeliveryService(yandexId, storedSortingCenter().getId(), isLastMile);
    }

    @Transactional
    public DeliveryService storedDeliveryService(String yandexId, long sortingCenterId, boolean isLastMile) {
        var listCurrentDs = deliveryServiceRepository.findByYandexId(yandexId);
        if (listCurrentDs.isPresent()) {
            return listCurrentDs.get();
        }
        DeliveryService deliveryService = deliveryServiceRepository.save(deliveryService(yandexId));
        if (isLastMile) {
            deliveryServicePropertyRepository.save(new DeliveryServiceProperty(
                    deliveryService.getYandexId(),
                    DeliveryServiceProperty.TYPE_ON_SC_PREFIX + sortingCenterId,
                    DeliveryServiceType.LAST_MILE_COURIER.name()
            ));
        }
        return deliveryService;
    }

    @Transactional
    public Courier storedCourier() {
        return courierRepository.save(courier());
    }

    @Transactional
    public Courier storedCourier(long id) {
        return courierRepository.save(courier(id));
    }

    @Transactional
    public CourierWithDs magistralCourier() {
        return magistralCourier("999888777");
    }

    @Transactional
    public CourierWithDs magistralCourier(String dsYandexId) {
        var ds = storedDeliveryService(dsYandexId, false);
        return new CourierWithDs(storedCourierFromDs(ds), ds);
    }

    public record CourierWithDs(Courier courier, DeliveryService deliveryService) {

    }

    @Transactional
    public Courier storedCourierFromDs(DeliveryService deliveryService) {
        return courierRepository.save(courier(CourierMapper.mapDeliveryServiceIdToCourierId(
                Long.parseLong(Objects.requireNonNull(deliveryService.getYandexId()))
        ), Long.parseLong(deliveryService.getYandexId())));
    }

    @Transactional
    public Courier storedCourierFromDs(long deliveryServiceId) {
        return courierRepository.save(courier(CourierMapper.mapDeliveryServiceIdToCourierId(deliveryServiceId)));
    }

    @Transactional
    public Courier storedCourier(long id, long deliveryServiceId) {
        return courierRepository.save(courier(id, deliveryServiceId));
    }

    @Transactional
    public Courier storedCourier(long id, String name) {
        return courierRepository.save(courier(id, name));
    }

    @Transactional
    public Courier storedEmptyCourier(long id, String name) {
        return courierRepository.save(emptyCourier(id, name));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter) {
        return cellCreator.createCell(cell(sortingCenter));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, String scNumber) {
        return cellCreator.createCell(cell(sortingCenter, scNumber));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, String number, CellField.CellFieldBuilder cellFieldBuilder) {
        var cellField = cellFieldBuilder.build();
        CellStatus status = Optional.ofNullable(cellField.status())
                .orElse(number == null ? CellStatus.NOT_ACTIVE : CellStatus.ACTIVE);
        return cellCreator.createCell(
                new Cell(sortingCenter, number, status,
                        cellField.type(),
                        cellField.subType(),
                        cellField.deleted(),
                        cellField.warehouseYandexId(),
                        cellField.courier(),
                        cellField.zone(),
                        cellField.sequenceNumber(),
                        cellField.alleyNumber(),
                        cellField.sectionNumber(),
                        cellField.levelNumber(),
                        cellField.lotsCapacity(),
                        cellField.cargoType()
                )
        );
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, CellStatus status) {
        return cellCreator.createCell(cell(sortingCenter, status));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, Zone zone) {
        return cellCreator.createCell(cell(sortingCenter, zone, false));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, CellType type, Warehouse warehouse, String scNumber) {
        return cellCreator.createCell(cell(sortingCenter, type, warehouse, scNumber));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, CellType type, @Nullable Zone zone) {
        return cellCreator.createCell(cell(sortingCenter, type, CellSubType.DEFAULT, zone, false));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, CellType type, CellSubType cellSubType, @Nullable Zone zone) {
        return cellCreator.createCell(cell(sortingCenter, type, cellSubType, zone, false));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, CellType type, CellSubType cellSubType, @Nullable Zone zone,
                           String scNumber) {
        return cellCreator.createCell(cell(sortingCenter, type, cellSubType, zone, false, scNumber));
    }

    @Transactional
    public Cell storedCell(
            SortingCenter sortingCenter, CellType type, CellSubType cellSubType, @Nullable Zone zone, boolean deleted
    ) {
        return cellCreator.createCell(cell(sortingCenter, type, cellSubType, zone, deleted));
    }

    @Transactional
    public Cell storedCell(Cell cell) {
        return cellCreator.createCell(cell);
    }

    @SuppressWarnings("UnusedReturnValue")
    @Transactional
    public Cell storedDeletedCell(SortingCenter sortingCenter, Zone zone) {
        return cellCreator.createCell(cell(sortingCenter, zone, true));
    }

    @SuppressWarnings("UnusedReturnValue")
    @Transactional
    public Cell storedDeletedCell(SortingCenter sortingCenter, String number, CellType cellType) {
        Cell cell = new Cell(sortingCenter, number, cellType);
        cell.setDeleted(true);
        return cellCreator.createCell(cell);
    }

    @Transactional
    public Cell storedNotActiveCell(SortingCenter sortingCenter, String number, CellType type) {
        Cell cell = new Cell(sortingCenter, number, CellStatus.NOT_ACTIVE, type, CellSubType.DEFAULT);
        return cellCreator.createCell(cell);
    }

    @Transactional
    public Cell findCell(SortingCenter sortingCenter, String number) {
        return cellRepository.findBySortingCenterAndScNumber(sortingCenter, number).stream().findFirst().orElseThrow();
    }

    @Transactional
    public Cell findCell(long id) {
        return cellRepository.findByIdOrThrow(id);
    }

    @Transactional
    public void setFullnessToCell(long cellId, boolean isFull) {
        var cell = cellRepository.findByIdOrThrow(cellId);
        cellCommandService.setFullness(cell, isFull);
    }

    @Transactional
    public List<Cell> findAllCells() {
        return cellRepository.findAll();
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, String number, CellType type) {
        return cellCreator.createCell(new Cell(sortingCenter, number, type, CellSubType.DEFAULT));
    }

    @Transactional
    public void deleteCellForce(Cell cell) {
        cellCommandService.deleteCell(cell.getSortingCenter(), cell.getId(), true);
    }

    @Transactional
    public void markCellDeleted(Cell cellToDelete) {
        var cell = cellRepository.findByIdOrThrow(cellToDelete.getId());
        cell.setDeleted(true);
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, String number, CellType type, long courierId) {
        return cellCreator.createCell(new Cell(sortingCenter, number, CellStatus.ACTIVE, type,
                CellSubType.DEFAULT,
                false, null, courierId, null, null));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, String number, DeliveryService deliveryService) {
        long courierId = CourierMapper.mapDeliveryServiceIdToCourierId(Long.parseLong(
                Objects.requireNonNull(deliveryService.getYandexId())
        ));
        return cellCreator.createCell(new Cell(sortingCenter, number, CellStatus.ACTIVE, CellType.COURIER,
                CellSubType.DEFAULT,
                false, null, courierId, null, null));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, String number, CellType type, CellSubType subtype) {
        return cellCreator.createCell(new Cell(sortingCenter, number, type, subtype));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, String number,
                           CellType type, CellSubType subtype, Long sequenceNumber) {
        return cellCreator.createCell(new Cell(sortingCenter, number, type, subtype, sequenceNumber));
    }

    @Transactional
    public Cell storedMagistralCell(SortingCenter sortingCenter, String number, CellSubType subtype, Long courierId) {
        return cellCreator.createCell(new Cell(sortingCenter, number,
                CellStatus.ACTIVE, CellType.COURIER, subtype, false, null, courierId, null, null));
    }

    @Transactional
    public Cell storedMagistralCell(SortingCenter sortingCenter, Long courierId) {
        return storedMagistralCell(sortingCenter, "test-c1", CellSubType.DEFAULT, courierId);
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, String number, CellType type, CellSubType subType,
                           String warehouseYandexId) {
        return cellCreator.createCell(new Cell(sortingCenter, number, CellStatus.ACTIVE, type,
                subType, false, warehouseYandexId, null, null, null));
    }

    @Transactional
    public Cell storedShipBufferCell(SortingCenter sortingCenter, long courierId, Zone zone, long alleyNumber,
                                     long sectionNumber, long levelNumber, long lotsCapacity) {
        var cellName = zone.getName() + alleyNumber + sectionNumber + levelNumber;
        var newCell = cellCreator.createCell(new Cell(
                sortingCenter, cellName, CellStatus.ACTIVE, CellType.COURIER, CellSubType.SHIP_BUFFER, false, null,
                courierId, zone, null, alleyNumber, sectionNumber, levelNumber, lotsCapacity, null));
        eventPublisher.publishEvent(new CellCreatedEvent(newCell, sortingCenter));
        return newCell;
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, String number, CellType type, CellSubType subType,
                           String warehouseYandexId, Zone zone) {
        CellStatus status = number == null ? CellStatus.NOT_ACTIVE : CellStatus.ACTIVE;
        return cellCreator.createCell(new Cell(sortingCenter, number, status, type,
                subType, false, warehouseYandexId, null, zone, null));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, String number, CellType type, CellSubType subType,
                           String warehouseYandexId, Zone zone, Long sequenceNumber) {
        CellStatus status = number == null ? CellStatus.NOT_ACTIVE : CellStatus.ACTIVE;
        return cellCreator.createCell(new Cell(sortingCenter, number, status, type,
                subType, false, warehouseYandexId, null, zone, sequenceNumber));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, String number, CellStatus status, CellType type, Zone zone) {
        return cellCreator.createCell(new Cell(sortingCenter, number, status, type,
                CellSubType.DEFAULT, false, null, null, zone, null));
    }

    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, String number, CellType type, String warehouseYandexId) {
        return cellCreator.createCell(new Cell(
                sortingCenter, number, CellStatus.ACTIVE, type, CellSubType.DEFAULT,
                false, warehouseYandexId, null, null, null));
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @Transactional
    public Cell storedCell(SortingCenter sortingCenter, String number, CellType type, CellSubType subType,
                           Courier courier, Zone zone, Long alleyNumber, Long sectionNumber, Long levelNumber,
                           Long lotsCapacity) {
        CellStatus status = number == null ? CellStatus.NOT_ACTIVE : CellStatus.ACTIVE;
        return cellCreator.createCell(new Cell(sortingCenter, number, status, type, subType, false, null,
                courier.getId(), zone, null, alleyNumber, sectionNumber, levelNumber, lotsCapacity,
                null));
    }

    @Transactional
    public Cell storedActiveCell(SortingCenter sortingCenter) {
        return cellCreator.createCell(new Cell(sortingCenter, "123", CellStatus.ACTIVE, CellType.COURIER,
                CellSubType.DEFAULT));
    }

    @Transactional
    public Cell storedActiveCell(SortingCenter sortingCenter, CellType type) {
        return cellCreator.createCell(new Cell(sortingCenter, "123", CellStatus.ACTIVE, type,
                CellSubType.DEFAULT));
    }

    @Transactional
    public Cell storedActiveCell(SortingCenter sortingCenter, CellType type, CellSubType subType, String scNumber) {
        return cellCreator.createCell(new Cell(sortingCenter, scNumber, CellStatus.ACTIVE, type,
                subType));
    }

    @Transactional
    public Cell storedActiveCell(SortingCenter sortingCenter, CellType type, String scNumber) {
        return cellCreator.createCell(
                new Cell(sortingCenter, scNumber, CellStatus.ACTIVE, type, CellSubType.DEFAULT));
    }

    public List<Place> findPlacesInCell(Cell cell) {
        return placeRepository.findAllByMutableStateCell(cell);
    }

    public int placesCount(Cell cell) {
        return placeRepository.countByCellId(cell.getId());
    }

    @Transactional
    public void shipOrderRouteAndDisableCellDistribution(OrderLike order) {
        var route = findOutgoingCourierRoute(order).orElseThrow();
        //Запоминаем маршрут СО до того как он отцепится от коробки
        var place = order.getPlace();
        RouteSo routeSo;
        if (place == null) {
            routeSo = orderPlace(order.getOrder()).getOutRoute();
        } else {
            routeSo = place.getOutRoute();
        }

        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                getRouteIdForSortableFlow(route),
                new ScContext(getOrCreateStoredUser(order.getSortingCenter())),
                List.of(Objects.requireNonNull(determineRouteCell(route, order)).getId()),
                null,
                false
        ));

        routeCommandService.cleanRoutesCellAndDisableCellDistribution(List.of(route.getId()));
        if (routeSo != null) {
            routeSoCommandService.cleanRoutesCellAndDisableCellDistribution(List.of(routeSo.getId()));
        }


    }

    private void cleanRouteSo(OrderLike order) {
        RouteSoMigrationHelper.allowRouteReading();
        var place = order.getPlace();
        RouteSo routeSo;
        if (place == null) {
            routeSo = orderPlace(order.getOrder()).getOutRoute();
        } else {
            routeSo = place.getOutRoute();
        }
        if (routeSo != null) {
            routeSoCommandService.cleanRoutesCellAndDisableCellDistribution(List.of(routeSo.getId()));
        }
        RouteSoMigrationHelper.revokeRouteReadingPermission();
    }

    @Transactional
    @VisibleForTesting // только для легаси маршрутов
    public Optional<Route> findPossibleIncomingWarehouseRoute(OrderLike order) {
        // В потоке сортируемых 2_3 возвращает место, чтобы искать по нему маршрут
        OrderLike placeOrOrder = getOrderLikeForRouteLookup(order);
        if (placeOrOrder.isPlace()) {
            return findPossibleIncomingWarehouseRouteByPlaceId(placeOrOrder.getId());
        } else {
            return findPossibleIncomingWarehouseRouteByOrderId(placeOrOrder.getId());
        }
    }

    @Transactional
    public Optional<Route> findPossibleIncomingWarehouseRouteByOrderId(long orderId) {
        var order = scOrderRepository.findByIdOrThrow(orderId);
        return routeNonBlockingQueryService.findWarehouseRoute(
                order.getIncomingRouteDate(), order.getSortingCenter(), RouteType.INCOMING_WAREHOUSE,
                order.getWarehouseFrom()
        );
    }

    @Transactional
    public Optional<Route> findPossibleIncomingWarehouseRouteByPlaceId(long placeId) {
        var place = placeRepository.findByIdOrThrow(placeId);
        return routeNonBlockingQueryService.findWarehouseRoute(
                place.getIncomingRouteDate(), place.getSortingCenter(), RouteType.INCOMING_WAREHOUSE,
                place.getWarehouseFrom()
        );
    }

    @Transactional
    public Optional<Route> findPossibleOutgoingWarehouseRoute(OrderLike order) {
        // В потоке сортируемых 2_3 возвращает место, чтобы искать по нему маршрут
        OrderLike placeOrOrder = getOrderLikeForRouteLookup(order);
        if (placeOrOrder.isPlace()) {
            return findPossibleOutcomingWarehouseRouteByPlaceId(placeOrOrder.getId()).map(Route::allowReading);
        } else {
            return findPossibleOutcomingWarehouseRouteByOrderId(placeOrOrder.getId()).map(Route::allowReading);
        }
    }

    @Transactional
    public Optional<Route> findPossibleOutcomingWarehouseRouteByOrderId(long orderId) {
        var order = scOrderRepository.findByIdOrThrow(orderId);
        return routeNonBlockingQueryService.findWarehouseRoute(
                order.getOutgoingRouteDate(), order.getSortingCenter(), RouteType.OUTGOING_WAREHOUSE,
                order.getWarehouseReturn()
        );
    }

    @Transactional
    public Optional<Route> findPossibleOutcomingWarehouseRouteByPlaceId(long placeId) {
        var place = placeRepository.findByIdOrThrow(placeId);
        return routeNonBlockingQueryService.findWarehouseRoute(
                place.getOutgoingRouteDate(), place.getSortingCenter(), RouteType.OUTGOING_WAREHOUSE,
                place.getWarehouseReturn()
        );
    }

    @Transactional
    public Optional<Route> findPossibleRouteForCancelledOrder(OrderLike order) {
        // В потоке сортируемых 2_3 возвращает место, чтобы искать по нему маршрут
        OrderLike placeOrOrder = getOrderLikeForRouteLookup(order);
        if (placeOrOrder.isPlace()) {
            return findPossibleRouteForCancelledOrderByPlaceId(placeOrOrder.getId());
        } else {
            return findPossibleRouteForCancelledOrderByOrderId(placeOrOrder.getId());
        }
    }

    @Transactional
    public Optional<Route> findPossibleRouteForCancelledOrderByOrderId(long orderId) {
        var order = scOrderRepository.findByIdOrThrow(orderId);
        return routeNonBlockingQueryService.findPossibleRouteForCancelledOrder(order);
    }

    @Transactional
    public Optional<Route> findPossibleRouteForCancelledOrderByPlaceId(long placeId) {
        var place = placeRepository.findByIdOrThrow(placeId);
        return routeNonBlockingQueryService.findPossibleRouteForCancelledOrder(place);
    }

    public Optional<Route> findOutgoingWarehouseRoute(OrderLike placeOrOrder) {
        var anyPlace = placeOrOrder.isPlace() ? placeOrOrder.getPlace() : anyOrderPlace(placeOrOrder.getOrder());

        return transactionTemplate.execute(ts ->
                routeNonBlockingQueryService.findPlaceRoute(anyPlace, RouteType.OUTGOING_WAREHOUSE)
                        .map(Route::allowReading)   // этот экземпляр не передается внутрь в тестируемые методы,
                                                    // в самих тестах читать его можно
        );
    }

    @Transactional
    public Optional<Route> findOutgoingWarehouseRoute(Long orderId) {
        var order = scOrderRepository.findByIdOrThrow(orderId);

        Place place = getOrderLikeForRouteLookup(order);

        return routeNonBlockingQueryService.findPlaceRoute(place, RouteType.OUTGOING_WAREHOUSE)
                .map(Route::allowReading); // Используется только в тестах, не нужно передавать
                                           // в сами тестируемые классы
    }

    @Transactional
    public Optional<Route> findPossibleIncomingCourierRoute(OrderLike orderObj) {
        return findPossibleIncomingCourierRoute(orderObj, null);
    }

    @Transactional
    public Optional<Route> findPossibleIncomingCourierRoute(OrderLike order, @Nullable Courier courier) {
        // В потоке сортируемых 2_3 возвращает место, чтобы искать по нему маршрут
        OrderLike placeOrOrder = getOrderLikeForRouteLookup(order);

        return routeNonBlockingQueryService.findCourierRoute(
                placeOrOrder.getIncomingRouteDate(), placeOrOrder.getSortingCenter(), RouteType.INCOMING_COURIER,
                Objects.requireNonNullElse(courier, placeOrOrder.getCourier())
        );
    }

    @Transactional
    public Optional<Route> findOutgoingRoute(OrderLike order) {
        // В потоке сортируемых 2_3 возвращает место, чтобы искать по нему маршрут
        OrderLike placeOrOrder = getOrderLikeForRouteLookup(order);
        return findOutgoingCourierRoute(placeOrOrder).or(() -> findOutgoingWarehouseRoute(placeOrOrder));
    }

    public Optional<Route> findOutgoingCourierRoute(OrderLike order) {
        Place place = getOrderLikeForRouteLookup(order);
        return transactionTemplate.execute(ts ->
                routeNonBlockingQueryService.findPlaceRoute(place, RouteType.OUTGOING_COURIER)
        ).map(Route::allowReading); //Нельзя передавать в тестируемые методы
    }

    @Transactional
    public void addRouteCell(Route route, Cell cell, LocalDate date) {
        route = routeRepository.findByIdOrThrow(route.getId());
        route.addCell(cell, date);
        if (SortableFlowSwitcherExtension.useNewRouteSoStage1_2()) {
            Long destincationId = getDestinationId(route);
            Set<RouteSo> routeSo =
                    routeSoRepository.getRouteAtTime(
                    route.getSortingCenter(),
                    routeTypeToRouteSoType(route.getType()),
                    destincationId,
                            ScDateUtils.toNoon(route.getExpectedDate()));
            RouteSo routeSo1 = routeSo.stream().findAny().get();
            routeSo1.getRouteSoSites().add(
                    new RouteSoSite(routeSo1, cell,
                            LocalDateTime.ofInstant(routeSo1.getIntervalFrom(), DateTimeUtil.DEFAULT_ZONE_ID),
                            LocalDateTime.ofInstant(routeSo1.getIntervalTo(), DateTimeUtil.DEFAULT_ZONE_ID))
            );

        }


        entityManager.flush();
    }

    private Long getDestinationId(Route route) {
        RouteSoMigrationHelper.allowRouteReading();
        var res = switch (route.getType()) {
            case INCOMING_WAREHOUSE -> route.getWarehouseFromId();
            case OUTGOING_WAREHOUSE -> route.getWarehouseToId();
            case INCOMING_COURIER -> route.getCourierFromId();
            case OUTGOING_COURIER -> route.getCourierToId();
        };
        RouteSoMigrationHelper.revokeRouteReadingPermission();
        return res;
    }

    public ru.yandex.market.sc.core.domain.route_so.model.RouteType routeTypeToRouteSoType(RouteType routeType) {
        return switch (routeType) {
            case INCOMING_WAREHOUSE -> ru.yandex.market.sc.core.domain.route_so.model.RouteType.IN_DIRECT;
            case OUTGOING_WAREHOUSE -> ru.yandex.market.sc.core.domain.route_so.model.RouteType.OUT_RETURN;
            case INCOMING_COURIER -> ru.yandex.market.sc.core.domain.route_so.model.RouteType.IN_RETURN;
            case OUTGOING_COURIER -> ru.yandex.market.sc.core.domain.route_so.model.RouteType.OUT_DIRECT;
        };
    }

    public RouteDestinationType routeTypeToRouteSoDestinationType(RouteType routeType) {
        return switch (routeType) {
            case INCOMING_WAREHOUSE -> RouteDestinationType.WAREHOUSE;
            case OUTGOING_WAREHOUSE -> RouteDestinationType.WAREHOUSE;
            case INCOMING_COURIER -> RouteDestinationType.COURIER;
            case OUTGOING_COURIER -> RouteDestinationType.COURIER;
        };
    }

//    @Transactional
    public Route findCellActiveRoute(Long cellId) {
        return routeNonBlockingQueryService.findCellActiveRouteForToday(cellId).orElseThrow();
    }

    @Transactional
    public Routable findCellActiveRoute(Long cellId, SortingCenter sortingCenter) {
        Cell cell = cellRepository.findByIdOrThrow(cellId);
        return routeQueryService.findActiveRouteByCellForToday(cell, sortingCenter).orElseThrow();
    }

    @Transactional
    public Cell determineRouteCell(Route route, OrderLike order) {
        route = routeRepository.findByIdOrThrow(route.getId());
        Routable routable = getRoutable(route);

        Cell cell = cellPolicy.findAndValidateOutgoingCell(
                getOrderLikeForRouteLookup(order), null,
                routable
        ).orElseThrow();

        return cell;
    }

    @Transactional
    public Optional<Cell> findRouteCell(Route route, OrderLike order) {
        route = routeRepository.findByIdOrThrow(route.getId());
        Routable routable = getRoutable(route);
//        Routable routable = chooseRoutable(route, getRouteSo(route));

        return cellPolicy.findAndValidateOutgoingCell(order.getOrder(), null, routable);
    }

//    //todo:kir hmmm..
//    public Routable chooseRoutable(Route route, RouteSo routeSo){
//        return routeSoMigrationHelper.chooseRoutable(route, getRouteSo(route));
//    }

    @Transactional
    public void assertApiPlaceDto(Place place, ApiOrderStatus expectedStatus, Cell expectedCelToSort) {
        ApiOrderDto orderForApi = orderQueryService.getOrderForApi(
                place.getSortingCenter(), place.getExternalId(), place.getMainPartnerCode());
        ApiPlaceDto apiPlaceDto = orderForApi.getPlaces().stream()
                .filter(p -> p.getExternalId().equals(place.getMainPartnerCode()))
                .findFirst().orElseThrow();
        assertThat(apiPlaceDto.getStatus())
                .isEqualTo(expectedStatus);
        assertThat(apiPlaceDto.getAvailableCells())
                .anyMatch(cell -> cell.getId() == expectedCelToSort.getId());
    }

    public TestFactory updateSegment(long orderId, Warehouse warehouseReturn, String segmentUid, String cargoUnitId,
                                     User user) {
        UpdateReturnWarehouseRequestDto requestBuiler =
                UpdateReturnWarehouseRequestDto.builder()
                        .orderId(orderId)
                        .cargoUnitId(cargoUnitId)
                        .segmentUuid(segmentUid)
                        .returnWarehouse(WarehouseDto.builder()
                                .yandexId(warehouseReturn.getYandexId())
                                .build())
                        .timeOut(Instant.now(clock))
                        .timeIn(Instant.now(clock))
                        .build();
        orderCommandService.updateReturnWarehouse(requestBuiler,  user);
        return this;
    }

    public TestFactory cancelOrder(OrderLike orderLike) {
        orderCommandService.cancelOrder(orderLike.getId(), null, false,
                userCommandService.findOrCreateFFApiRobotUser(orderLike.getSortingCenter())
        );
        return this;
    }

    public TestFactory cancelOrder(long orderId) {
        orderCommandService.cancelOrder(orderId, null, false, storedUser(storedSortingCenter(), 1000000L + orderId));
        return this;
    }

    public TestFactory cancelOrder(SortingCenter sortingCenter, String externalOrderId) {
        orderCommandService.cancelOrder(sortingCenter, externalOrderId,
                null, false, storedUser(storedSortingCenter(), 1000000L));
        return this;
    }

    @Transactional
    public TestFactory acceptPlace(Place place) {
        acceptService.acceptPlace(new PlaceScRequest(
                PlaceId.of(place), getOrCreateStoredUser(place.getSortingCenter())
        ));
        return this;
    }

    @Transactional
    public TestFactory acceptPlace(ScOrder order, String placeExternalId) {
        acceptService.acceptPlace(new PlaceScRequest(
                new PlaceId(order.getId(), placeExternalId), getOrCreateStoredUser(order.getSortingCenter())
        ));
        return this;
    }

    @Transactional
    public TestFactory acceptPlace(ScOrder order, String placeExternalId, User user) {
        acceptService.acceptPlace(new PlaceScRequest(new PlaceId(order.getId(), placeExternalId), user));
        return this;
    }

    @Transactional
    public TestFactory shipPlace(ScOrder order, String placeExternalId) {
        return shipPlace(
                placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), placeExternalId).orElseThrow()
        );
    }

    @Transactional
    public TestFactory shipPlace(Place place) {
        Route route;
        Cell cell;

        route = findOutgoingCourierRoute(place)
                .or(() -> findOutgoingWarehouseRoute(place))
                .orElseThrow();
        cell = determineRouteCell(route, place);

        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                getRouteIdForSortableFlow(route),
                new ScContext(getOrCreateStoredUser(place.getSortingCenter())),
                List.of(Objects.requireNonNull(cell).getId()),
                null,
                place.getExternalId(),
                place.getMainPartnerCode(),
                null,
                false
        ));
        return this;
    }

    @Transactional
    @SuppressWarnings("UnusedReturnValue")
    public TestFactory sortPlace(ScOrder order, String placeExternalId) {
        PlaceScRequest request = new PlaceScRequest(
                new PlaceId(order.getId(), placeExternalId), getOrCreateStoredUser(order.getSortingCenter())
        );
        long cellId = findOutgoingCellId(
                placeRepository.findByOrderIdAndMainPartnerCode(order.getId(), placeExternalId).orElseThrow()
        );

        placeCommandService.sortPlace(request, cellId, false);
        return this;
    }

    @Transactional
    @SuppressWarnings("UnusedReturnValue")
    public TestFactory sortPlace(ScOrder order, long cellId) {
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places).hasSize(1);

        return sortPlace(places.get(0), cellId);
    }

    @Transactional
    @SuppressWarnings("UnusedReturnValue")
    public TestFactory sortPlace(Place place) {
        return sortPlace(place, findOutgoingCellId(place));
    }

    @Transactional
    @SuppressWarnings("UnusedReturnValue")
    public TestFactory sortPlace(Place place, long cellId) {
        placeCommandService.sortPlace(new PlaceScRequest(
                PlaceId.of(place),
                getOrCreateStoredUser(place.getSortingCenter())
        ), cellId, false);
        return this;
    }

    private long findOutgoingCellId(OrderLike order) {
        RouteSoMigrationHelper.allowRouteReading();
        Route route = findOutgoingWarehouseRoute(order)
                .or(() -> findOutgoingCourierRoute(order))
                .orElseThrow();
        Cell cell = determineRouteCell(route, order);
        RouteSoMigrationHelper.revokeRouteReadingPermission();
        return cell.getId();
    }

    @Transactional
    public void rescheduleSortReturns(SortingCenter sortingCenter) {
        List<Long> orderIds = orderNonBlockingQueryService.findReturnOrderIdsForReschedule(
                sortingCenter, LocalDate.now(clock));
        User user = storedUser(sortingCenter, 123);

        orderCommandService.rescheduleSortDateReturns(orderIds, LocalDate.now(clock), sortingCenter,
                user);

        List<Long> placeIds = placeNonBlockingQueryService.findByOrderIds(orderIds).stream()
                .map(Place::getId).toList();

        placeRouteSoService.rescheduleSortDate(placeIds, Instant.now(clock), user);

    }

    @Transactional
    public User user() {
        return user(sortingCenter());
    }

    @Transactional
    public User user(SortingCenter sortingCenter) {
        return new User(sortingCenter, 123123L, "mail@mail.mail", "Иван Иванов");
    }

    @Transactional
    public Route route() {
        SortingCenter sortingCenter = sortingCenterRepository.save(sortingCenter());
        Cell cell = cellCreator.createCell(cell(sortingCenter));
        return route(LocalDate.now(clock), sortingCenter, cell);
    }

    public Route route(LocalDate date, SortingCenter sortingCenter, Cell cell) {
        Route route = new Route(
                sortingCenter, RouteType.INCOMING_WAREHOUSE,
                date, OffsetTime.now(clock),
                storedWarehouse(), null, null, null
        );
        route.addCell(cell, LocalDate.now(clock));
        return route;
    }

    public RouteFinish storedEmptyRouteFinish(
            Route route,
            User user
    ) {
        return storedEmptyRouteFinish(route, user, Instant.now(clock));
    }

    public RouteFinish storedEmptyRouteFinish(
            Route route,
            User user,
            Instant time
    ) {
        route.allowNextRead();
        Long routeId = getRouteIdForSortableFlow(route);

        routeFinishJdbcRepository.finishRoute(new RouteFinishRequest(
                 SortableFlowSwitcherExtension.useNewRouteSoStage2() ? getRouteSo(route) : route,
                new ScContext(user),
                Collections.emptyList(),
                Collections.emptyList(),
                time,
                "test route finish",
                "test barcode",
                true
        ));
        if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
            return transactionTemplate.execute(
                    ts -> routeSoRepository.getById(routeId).getRouteFinishes().stream()
                            .findFirst()
                            .orElseThrow()
            );

        } else {
            return transactionTemplate.execute(
                    ts -> routeRepository.getById(routeId).getRouteFinishes().stream()
                            .findFirst()
                            .orElseThrow()
            );
        }
    }

    @Transactional
    public RouteFinishOrder storedRouteFinishOrder(
            RouteFinish routeFinish,
            long orderId,
            String externalOrderId,
            ScOrderFFStatus orderFfStatus,
            @Nullable Long cellId
    ) {
        routeFinishJdbcRepository.createRouteFinishOrders(
                routeFinish.getId(),
                List.of(new RouteFinishOrderCreateRequest(
                        orderId,
                        externalOrderId,
                        orderFfStatus,
                        cellId
                ))
        );
        var actualRouteFinish = routeRepository.getById(routeFinish.getRouteId())
                .getRouteFinishes().stream()
                .filter(rf -> Objects.equals(rf.getId(), routeFinish.getId()))
                .findFirst().orElseThrow();
        return actualRouteFinish.getRouteFinishOrders().stream()
                .filter(p -> p.getOrderId() == orderId)
                .findFirst().orElseThrow();
    }

    @Transactional
    public RouteFinishPlace storedRouteFinishPlace(
            RouteFinish routeFinish,
            long placeId,
            String externalPlaceId,
            long orderId,
            PlaceStatus finishedPlaceStatus,
            SortableStatus finishedSortableStatus,
            Long cellId,
            Long lotId,
            Long lotCellId
    ) {
        routeFinishJdbcRepository.createRouteFinishPlaces(
                routeFinish.getId(),
                List.of(new RouteFinishPlaceCreateRequest(
                        placeId,
                        orderId,
                        externalPlaceId,
                        finishedPlaceStatus,
                        finishedSortableStatus,
                        cellId,
                        lotId,
                        lotCellId,
                        null
                )));
        var actualRouteFinish = routeRepository.getById(routeFinish.getRouteId())
                .getRouteFinishes().stream()
                .filter(rf -> Objects.equals(rf.getId(), routeFinish.getId()))
                .findFirst().orElseThrow();
        return actualRouteFinish.getRouteFinishPlaces().stream()
                .filter(p -> p.getPlaceId() == placeId)
                .findFirst().orElseThrow();
    }

    public RouteFinishPlace findRouteFinishPlaceByPlaceId(long placeId) {
        return routeFinishPlaceRepository.findAll().stream()
                .filter(rfp -> rfp.getPlaceId() == placeId)
                .findFirst()
                .orElseThrow();
    }

    public List<RouteFinishPlace> findRouteFinishPlacesByPlaceId(long placeId) {
        return routeFinishPlaceRepository.findAll().stream()
                .filter(rfp -> rfp.getPlaceId() == placeId)
                .toList();
    }

    @Transactional
    public Route storedOutgoingCourierRoute(LocalDate date, SortingCenter sortingCenter,
                                            Courier courier, Cell... cells) {
        return storedOutgoingCourierRoute(date, sortingCenter, courier, false, cells).allowReading();
    }

    @Transactional
    public Route storedOutgoingCourierRoute(LocalDate date, SortingCenter sortingCenter,
                                            Courier courier, boolean bindCellsOnToday, Cell... cells) {
        var routeOptional = routeRepository.findByExpectedDateAndSortingCenterAndCourierToId(date, sortingCenter,
                courier.getId()
        );
        if (routeOptional.isPresent()) {
            return routeOptional.get();
        }

        var route = new Route(
                sortingCenter, RouteType.OUTGOING_COURIER,
                date, OffsetTime.now(clock),
                null, null, null, courier
        );
        RouteSoMigrationHelper.allowRouteReading();
        for (Cell cell : cells) {
            if (bindCellsOnToday) {
                route.addCell(cell, LocalDate.now(clock));
            } else {
                route.addCell(cell, date);
            }
        }
        RouteSoMigrationHelper.revokeRouteReadingPermission();
        Route persistedRoute = routeRepository.save(route);

        if (SortableFlowSwitcherExtension.useNewRouteSoStage1_2()) {
            RouteSo routeSo = new RouteSo(
                    sortingCenter,
                    ru.yandex.market.sc.core.domain.route_so.model.RouteType.OUT_DIRECT,
                    courier.getId(),
                    RouteDestinationType.COURIER,
                    ScDateUtils.toBeginningOfDay(date), ScDateUtils.toEndOfDay(date),
                    null
            );

            for (Cell cell : cells) {
                if (bindCellsOnToday) {
                    routeSo.bindCell(cell,
                            ScDateUtils.toLocalDateTime(ScDateUtils.beginningOfDay(clock)),
                            ScDateUtils.toLocalDateTime(ScDateUtils.endOfDay(clock)));

                } else {
                    routeSo.bindCell(cell,
                            ScDateUtils.toLocalDateTime(ScDateUtils.toBeginningOfDay(date)),
                            ScDateUtils.toLocalDateTime(ScDateUtils.toEndOfDay(date)));
                }

            }
            routeSoRepository.save(routeSo);

        }
        return persistedRoute;
    }


    @Transactional
    public Route storedOutgoingCourierRoute(SortingCenter sortingCenter, @Nullable Cell cell, Courier courier,
                                            LocalDate expectedDate, LocalDate reserveDateSort) {
        Route route = new Route(
                sortingCenter, RouteType.OUTGOING_COURIER,
                expectedDate, OffsetTime.now(clock),
                null, null, null, courier
        );
        if (cell != null) {
            route.addCell(cell, reserveDateSort);
        }
        Route routePersisted = routeRepository.save(route);


        Instant intervalFrom = ScDateUtils.toBeginningOfDay(expectedDate);
        Instant intervalTo = ScDateUtils.toEndOfDay(expectedDate);
        RouteSo routeSo = new RouteSo(sortingCenter,
                ru.yandex.market.sc.core.domain.route_so.model.RouteType.OUT_DIRECT,
                courier.getId(), RouteDestinationType.COURIER,
                intervalFrom, intervalTo, null);
        RouteSo routeSoPersisted = routeSoRepository.save(routeSo);
        if (cell != null) {
            routeSoPersisted.bindCell(cell, ScDateUtils.toLocalDateTime(intervalFrom),
                    ScDateUtils.toLocalDateTime(intervalTo));
        }
        return routePersisted;
    }

    public Route storedOutgoingWarehouseRoute(LocalDate date, SortingCenter sortingCenter,
                                              Warehouse warehouse, Cell... cells) {
        var route = new Route(
                sortingCenter, RouteType.OUTGOING_WAREHOUSE,
                date, OffsetTime.now(clock),
                null, warehouse, null, null
        );
        route.allowReading();
        for (Cell cell : cells) {
            route.addCell(cell, LocalDate.now(clock));
        }

        Instant intervalFrom = ScDateUtils.toBeginningOfDay(date);
        Instant intervalTo = ScDateUtils.toEndOfDay(date);
        RouteSo routeSo = new RouteSo(sortingCenter,
                ru.yandex.market.sc.core.domain.route_so.model.RouteType.OUT_RETURN,
                warehouse.getId(), RouteDestinationType.WAREHOUSE,
                intervalFrom, intervalTo, null);
        RouteSo routeSoPersisted = routeSoRepository.save(routeSo);

        for (Cell cell : cells) {
            routeSoPersisted.bindCell(cell, ScDateUtils.beginningOfDayDt(clock), ScDateUtils.endOfDayDt(clock));
        }
        return routeRepository.save(route).allowReading();
    }

    @Transactional
    public Route storedIncomingCourierDropOffRoute(
            LocalDate date,
            SortingCenter sortingCenter,
            Courier courier
    ) {
        Route route = routeRepository.save(incomingCourierDropOffRoute(date, sortingCenter, courier));

        Instant intervalFrom = ScDateUtils.toBeginningOfDay(date);
        Instant intervalTo = ScDateUtils.toEndOfDay(date);
        RouteSo routeSo = new RouteSo(sortingCenter,
                ru.yandex.market.sc.core.domain.route_so.model.RouteType.IN_RETURN,
                courier.getId(), RouteDestinationType.COURIER,
                intervalFrom, intervalTo, null);
        RouteSo routeSoPersisted = routeSoRepository.save(routeSo);

        return route;
    }

    public Route incomingCourierDropOffRoute(
            LocalDate date,
            SortingCenter sortingCenter,
            Courier courier
    ) {
        var route = new Route(
                sortingCenter, RouteType.INCOMING_COURIER,
                date, OffsetTime.now(clock),
                null, null, courier, null
        );

        return route;
    }


    public void invalidateMemcached() {
        memCachedClientFactoryMock.close();
    }

    public static void setupMockClockToSystemTime(Clock clock) {
        setupMockClock(clock, Instant.now());
    }

    public static void setupMockClock(Clock clock) {
        setupMockClock(clock, Instant.ofEpochSecond(1587033394L));
    }

    public static void setupMockClock(Clock clock, Instant newTime) {
        doReturn(newTime).when(clock).instant();
        doReturn(ZoneId.systemDefault()).when(clock).getZone();
    }

    @Transactional
    public TestOrderBuilder createOrderForToday(SortingCenter sortingCenter) {
        return createOrder(sortingCenter).updateShipmentDate(LocalDate.now(clock)).updateCourier(defaultCourier());
    }

    @Transactional
    public TestOrderBuilder createForToday(CreateOrderParams params) {
        boolean isDropoff = getSortingCenterProperty(params.getSortingCenter(),
                SortingCenterPropertiesKey.IS_DROPOFF, false);

        if (params.dsType.equals(DeliveryServiceType.TRANSIT) || params.isClientReturn() || isDropoff) {
            return create(params).updateShipmentDate(LocalDate.now(clock));
        } else {
            return create(params).updateCourier(defaultCourier()).updateShipmentDate(LocalDate.now(clock));
        }
    }

    @Transactional
    public TestOrderBuilder createForToday(CreateOrderParams params, CourierDto courierDto) {
        if (params.dsType.equals(DeliveryServiceType.TRANSIT)) {
            throw new UnsupportedOperationException("Cant' create order with pre-defined courier and set new courier");
        }
        return create(params).updateCourier(courierDto).updateShipmentDate(LocalDate.now(clock));
    }

    @Transactional
    public TestOrderBuilder createOrder(SortingCenter sortingCenter, DeliveryService deliveryService) {
        return new TestOrderBuilder().create(order(sortingCenter, deliveryService).build());
    }

    @Transactional
    public TestOrderBuilder createOrder(SortingCenter sortingCenter) {
        return new TestOrderBuilder().create(order(sortingCenter).build());
    }

    @Transactional
    public TestOrderBuilder createClientReturnForToday(Long scId, String token, String yandexId, CourierDto courierDto,
                                                       String barcode) {
        return new TestOrderBuilder()
                .createClientReturn(scId, token, yandexId, courierDto, barcode, LocalDate.now(clock));
    }

    @Transactional
    public TestOrderBuilder createClientReturnForToday(SortingCenter sortingCenter,
                                                       CourierDto courierDto,
                                                       String barcode) {
        return new TestOrderBuilder()
                .createClientReturn(sortingCenter.getId(), sortingCenter.getToken(), sortingCenter.getYandexId(),
                        courierDto, barcode, LocalDate.now(clock));
    }

    @Transactional
    public TestOrderBuilder createOrder(CreateOrderParams params) {
        return new TestOrderBuilder().create(params);
    }

    @Transactional
    public TestOrderBuilder create(CreateOrderParams params) {
        return new TestOrderBuilder().create(params);
    }

    public ScOrder rescheduleReturn(ScOrder order, LocalDate date) {
        orderCommandService.rescheduleSortDateReturns(List.of(order.getId()), date, order.getSortingCenter(),
                getOrCreateAnyUser(order.getSortingCenter()));
        placeRouteSoService.rescheduleSortDate(orderPlaces(order).stream().map(Place::getId).toList(),
                ScDateUtils.toNoon(date), null);
        return scOrderRepository.findByIdOrThrow(order.getId());
    }


    public ScOrder updateForTodayDelivery(OrderLike order) {
        long orderId = order.getOrder().getId();
        LocalDate now = LocalDate.now(clock);

        orderCommandService.updateShipmentDate(orderId, now, getOrCreateAnyUser(order.getSortingCenterId()));
        orderCommandService.updateCourier(orderId, defaultCourier(),
                getOrCreateAnyUser(order.getSortingCenter()));

        transactionTemplate.execute(ts -> {
                    List<Place> places = placeRepository.findAllByOrderIdOrderById(orderId);
                    ScOrder updatedOrder = scOrderRepository.findByIdOrThrow(orderId);
                    Courier updatedCourier = updatedOrder.getCourier();

                    for (Place place : places) {
                        place.getHistory().size();
                        Instant shipmentTime = ScDateUtils.toNoon(now);
                        placeRouteSoService.updatePlaceRoutes(place, updatedCourier,
                                shipmentTime,
                                getOrCreateAnyUser(place.getSortingCenter()));

                    }
                    return null;
                }
        );
        return scOrderRepository.findByIdOrThrow(orderId);
    }

    public ScOrder accept(OrderLike order) {
        placeRepository.findAllByOrderIdOrderById(order.getId()).forEach(
                place -> acceptService.acceptPlace(new PlaceScRequest(
                        new PlaceId(order.getId(), place.getMainPartnerCode()),
                        getOrCreateStoredUser(order.getSortingCenter()))));
        return scOrderRepository.findByIdOrThrow(order.getId());
    }

    @Transactional
    public ScOrder sortOrderToLot(OrderLike order, SortableLot lot, User user) {
        List<Place> places = placeRepository.findAllByOrderIdOrderById(order.getId());
        assertThat(places).hasSize(order.getPlaceCount()); //Текущий флоу расчитана на работу одноместных
        // multiplace заказов

        PlaceId placeId = new PlaceId(order.getId(), places.get(0).getMainPartnerCode());
        PlaceScRequest request = new PlaceScRequest(placeId, user);
        placeCommandService.sortPlaceToLot(request, lot.getLotId());
        return scOrderRepository.findByIdOrThrow(order.getId());
    }

    @Transactional
    public Place sortPlaceToLot(Place place, SortableLot lot, User user) {
        placeCommandService.sortPlaceToLot(new PlaceScRequest(
                new PlaceId(place.getOrderId(), place.getMainPartnerCode()),
                user), lot.getLotId());
        return placeRepository.findByIdOrThrow(place.getId());
    }

    public SortableLot prepareToShipLot(SortableLot lot) {
        lotCommandService.prepareToShipLot(lot.getLotId(), SortableAPIAction.READY_FOR_SHIPMENT,
                getOrCreateStoredUser(lot.getSortingCenter()));
        return sortableLotService.findByLotIdOrThrow(lot.getLotId());
    }

    public SortableLot prepareToShipLot(Sortable parent) {
        SortableLot lot = sortableLotService.findBySortableId(parent.getId()).orElseThrow();
        return prepareToShipLot(lot);
    }

    @Transactional
    @SuppressWarnings("UnusedReturnValue")
    public ScOrder sortOrder(OrderLike order) {
        return sortOrder(order, findOutgoingCellId(anyOrderPlace(order.getOrder())));
    }

    @Transactional
    @SuppressWarnings("UnusedReturnValue")
    public ScOrder sortOrder(OrderLike order, Long cellId) {
        sortPlace(order.getOrder(), cellId);
        return scOrderRepository.findByIdOrThrow(order.getId());
    }

    @SuppressWarnings("UnusedReturnValue")
    @Transactional
    public ScOrder shipOrderRoute(OrderLike order) {
        return shipOrderRoute(order, getOrCreateStoredUser(order.getSortingCenter()));
    }

    public void setupWarehouseAndDeliveryServiceForClientReturns(
            String warehouseId
    ) {
        storedWarehouse(warehouseId);
        storedDeliveryService(ClientReturnService.DELIVERY_SERVICE_YA_ID);
    }

    public void setupWarehouseAndDeliveryServiceForClientReturns() {
        setupWarehouseAndDeliveryServiceForClientReturns(
                ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId());
    }

    @Transactional
    public SortingCenterProperty setSortingCenterProperty(SortingCenter sortingCenter,
                                                          SortingCenterPropertiesKey key,
                                                          boolean value) {
        return setSortingCenterProperty(sortingCenter.getId(), key, value);
    }

    @Transactional
    public Boolean getSortingCenterProperty(SortingCenter sortingCenter,
                                            SortingCenterPropertiesKey key,
                                            boolean value) {
        return sortingCenterPropertyRepository
                .findBySortingCenterIdAndKey(sortingCenter.getId(), key).map(SortingCenterProperty::getValue)
                .map(Boolean::valueOf)
                .orElse(value);
    }


    @Transactional
    public void setSortingCenterPropertyMap(SortingCenter sortingCenter,
                                            Map<SortingCenterPropertiesKey, Boolean> sortingCenterPropertiesMap) {
        sortingCenterPropertiesMap.forEach((prop, value) -> setSortingCenterProperty(sortingCenter, prop, value));
    }

    @Transactional
    public SortingCenterProperty setSortingCenterProperty(SortingCenter sortingCenter,
                                                          SortingCenterPropertiesKey key,
                                                          String value) {
        return setSortingCenterProperty(sortingCenter.getId(), key, value);
    }

    @Transactional
    public SortingCenterProperty setSortingCenterProperty(long scId,
                                                          SortingCenterPropertiesKey key,
                                                          boolean value) {
        return setSortingCenterProperty(scId, key, Boolean.toString(value));
    }


    @Transactional
    public SortingCenterProperty setSortingCenterProperty(long scId,
                                                          SortingCenterPropertiesKey key,
                                                          String value) {
        var propertyO = sortingCenterPropertyRepository.findBySortingCenterIdAndKey(scId, key);
        if (propertyO.isPresent()) {
            var property = propertyO.get();
            property.setValue(value);
            SortingCenterProperty save = sortingCenterPropertyRepository.save(property);
            return save;
        }
        SortingCenterProperty save = sortingCenterPropertyRepository.save(new SortingCenterProperty(
                scId, key, value
        ));
        return save;
    }

    @Transactional
    public void storedPartnerMappingGroup(Long... items) {
        if (items.length == 0) {
            return;
        }
        partnerMappingGroupRepository.save(PartnerMappingGroup.builder()
                .items(Sets.newHashSet(items))
                .build()
        );
    }

    public void storedCrossDockMapping(long sourceSortingCenterId, long firstHopSortingCenterId,
                                       long destSortingCenterId) {
        crossDockMappingRepository.save(CrossDockMapping.builder()
                .sourceSortingCenterId(sourceSortingCenterId)
                .destSortingCenterId(destSortingCenterId)
                .firstHopSortingCenterId(firstHopSortingCenterId)
                .build()
        );
    }

    @Transactional
    public void setDeliveryServiceProperty(DeliveryService deliveryService,
                                           String key, String value) {
        deliveryServicePropertyRepository.save(new DeliveryServiceProperty(
                deliveryService.getYandexId(), key, value
        ));
    }

    public void changeDeliveryServiceOn(SortingCenter sortingCenter,
                                        DeliveryService deliveryService,
                                        DeliveryServiceType dsType) {
        var currentDsType =
                StreamEx.of(deliveryServicePropertyRepository.findAllByDeliveryServiceYandexId(
                                deliveryService.getYandexId()))
                        .findAny(p -> Objects.equals(p.getKey(),
                                DeliveryServiceProperty.TYPE_ON_SC_PREFIX + sortingCenter.getId()));
        var newDsProperty = new DeliveryServiceProperty(
                deliveryService.getYandexId(),
                DeliveryServiceProperty.TYPE_ON_SC_PREFIX + sortingCenter.getId(),
                dsType.name()
        );
        if (currentDsType.isPresent() && !currentDsType.get().equals(newDsProperty)) {
            deliveryServicePropertyRepository.delete(currentDsType.get());
            deliveryServicePropertyRepository.save(newDsProperty);
        }
        if (!currentDsType.isPresent()) {
            deliveryServicePropertyRepository.save(newDsProperty);
        }
    }

    @Transactional
    public ScOrder shipOrderRoute(OrderLike order, User dispatcherPerson) {
        var route = findOutgoingCourierRoute(order)
                .or(() -> findOutgoingWarehouseRoute(order))
                .or(() -> findPossibleOutgoingWarehouseRoute(order))
                .orElseThrow();
        Long routeId = getRouteIdForSortableFlow(route);
        routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                routeId,
                new ScContext(dispatcherPerson),
                List.of(Objects.requireNonNull(determineRouteCell(route, order)).getId()),
                null,
                false
        ));
        return scOrderRepository.findByIdOrThrow(order.getOrder().getId());
    }

    public ScOrder updateCourier(OrderLike order, Courier newCourier) {
        orderCommandService.updateCourier(order.getId(), new CourierDto(newCourier.getId()),
                getOrCreateAnyUser(order.getSortingCenter()));

        transactionTemplate.execute(ts -> {
                    placeRepository.findAllByOrderIdOrderById(order.getId()).forEach(
                            p -> {
                                Instant shipmentTime = p.getShipmentDate() == null
                                        ? Instant.now(clock)
                                        : ScDateUtils.toNoon(p.getShipmentDate());
                                placeRouteSoService.updatePlaceRoutes(p, newCourier,
                                        shipmentTime,
                                        getOrCreateAnyUser(p.getSortingCenter()));
                            }

                    );
                    return null;
                }
        );

        return scOrderRepository.findByIdOrThrow(order.getId());
    }

    public CourierDto defaultCourier() {
        return new CourierDto(1L, "Иван Пивовар Таранов", null, null, null, null, null, false);
    }

    public CourierDto fakeCourier() {
        return new CourierDto(404L, "UNKNOWN_COURIER", null);
    }

    public static CreateOrderParams.CreateOrderParamsBuilder order(SortingCenter sortingCenter) {
        return order(sortingCenter, deliveryService());
    }

    public static CreateOrderParams.CreateOrderParamsBuilder order(SortingCenter sortingCenter,
                                                                   DeliveryService deliveryService) {
        return CreateOrderParams.builder()
                .sortingCenter(sortingCenter)
                .deliveryService(deliveryService);
    }

    public static CreateOrderParams.CreateOrderParamsBuilder order(SortingCenter sortingCenter, String externalId) {
        return CreateOrderParams.builder()
                .sortingCenter(sortingCenter)
                .deliveryService(deliveryService())
                .externalId(externalId);
    }

    @Transactional
    public void setWarehouseProperty(String warehouseId, String key, String value) {
        var properties = warehousePropertyRepository.findAllByWarehouseYandexId(warehouseId);
        properties.stream()
                .filter(p -> Objects.equals(p.getKey(), key))
                .forEach(p -> warehousePropertyRepository.delete(p));
        warehousePropertyRepository.flush();
        warehousePropertyRepository.save(new WarehouseProperty(warehouseId, key, value));
        warehousePropertySource.refreshForce();
    }


    @SuppressWarnings("checkstyle:ParameterNumber")
    public Outbound createOutbound(
            String externalId,
            OutboundStatus status,
            OutboundType type,
            Instant fromTime,
            Instant toTime,
            String partnerYandexId,
            @Nullable SortingCenter sortingCenter,
            @Nullable MovementCourier courier
    ) {
        return createOutbound(externalId, status, type, fromTime, toTime, partnerYandexId, sortingCenter, courier,
                null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public Outbound createOutbound(
            String externalId,
            OutboundStatus status,
            OutboundType type,
            Instant fromTime,
            Instant toTime,
            String partnerYandexId,
            @Nullable SortingCenter sortingCenter,
            @Nullable MovementCourier courier,
            @Nullable String partnerToYandexId
    ) {
        Outbound outbound = new Outbound()
                .setExternalId(externalId)
                .setLocationFrom(location())
                .setMovementCourier(Optional.ofNullable(courier)
                        .orElseGet(() -> movementCourierRepository.save(movementCourier())
                        ))
                .setSortingCenter(Optional.ofNullable(sortingCenter)
                        .orElseGet(() -> sortingCenterRepository.save(sortingCenter())))
                .setFromTime(fromTime)
                .setToTime(toTime)
                .setStatus(status)
                .setLogisticPointToExternalId(partnerYandexId)
                .setPartnerToExternalId(partnerToYandexId)
                .setType(type);
        return outboundRepository.save(outbound);
    }

    public Inbound createInbound(CreateInboundParams params) {
        MovementCourierRequest courierRequest;
        MovementCourier movementCourier = params.movementCourier;
        if (movementCourier != null) {
            courierRequest = new MovementCourierRequest(
                    movementCourier.getExternalId(),
                    movementCourier.getLegalEntityName(),
                    movementCourier.getLegalEntityLegalName(),
                    movementCourier.getCarNumber(),
                    movementCourier.getUid(),
                    params.courierPhone
            );
        } else {
            Long randomNumber = ThreadLocalRandom.current().nextLong(1_000_000);
            courierRequest = new MovementCourierRequest(
                    randomNumber.toString(),
                    "name",
                    "legalName",
                    null,
                    randomNumber,
                    params.courierPhone
            );
        }

        transactionTemplate.execute(ts -> {

            var request = InboundCreateRequest.builder()
                    .externalId(params.getInboundExternalId())
                    .warehouseFromId(params.getWarehouseFromExternalId())
                    .inboundType(params.getInboundType())
                    .fromDate(params.getFromDate())
                    .toDate(params.getToDate())
                    .courierRequest(courierRequest)
                    .locationCreateRequest(params.getLocationCreateRequest())
                    .comment("test inbound")
                    .sortingCenter(params.getSortingCenter())
                    .nextLogisticPointId(params.getNextLogisticPointId())
                    .externalRequestId(params.getInformationListBarcode())
                    .confirmed(params.isConfirmed())
                    .transportationId(params.getTransportationId())
                    .realSupplierName(params.getRealSupplierName())
                    .build();
            inboundCommandService.putInbound(request);

            params.getRegistryMap().keySet().forEach(regKey -> {
                //regKey - внешний Id реестра
                List<CreateInboundRegistryOrderRequest> registryRequests =
                        new ArrayList<>(params.getRegistryMap().get(regKey).size());
                //количество заказов в реестре
                params.getRegistryMap().get(regKey).forEach(order -> registryRequests.add(
                        new CreateInboundRegistryOrderRequest(params.getInboundExternalId(),
                                order.getFirst(),
                                order.getSecond(),
                                params.placeInPallets == null ? "pallet_external_id" :
                                        params.placeInPallets.getOrDefault(order.getSecond(), "pallet_external_id"))));
                params.getPlainOrders().forEach(plainOrder -> {
                    //добавим еще заказов из plainOrders если они есть
                    registryRequests.add(new CreateInboundRegistryOrderRequest(params.getInboundExternalId(),
                            plainOrder.getFirst(),
                            plainOrder.getSecond(),
                            params.placeInPallets == null ? "pallet_external_id" :
                                    params.placeInPallets.getOrDefault(plainOrder.getSecond(), "pallet_external_id")));
                });

                List<CreateInboundRegistrySortableRequest> sortableRequests = new ArrayList<>();

                if (params.placeInPallets != null) {
                    Set<String> pallets = new HashSet<>(params.placeInPallets.values());

                    pallets.forEach(
                            pallet -> sortableRequests.add(
                                    new CreateInboundRegistrySortableRequest(
                                            params.inboundExternalId,
                                            pallet,
                                            RegistryUnitType.PALLET,
                                            Optional.ofNullable(params.palletToStamp)
                                                    .map(it -> it.get(pallet))
                                                    .orElse(null),
                                            Optional.ofNullable(params.crossDockPalletDestinations)
                                                    .map(cdpd -> cdpd.get(pallet))
                                                    .orElse(null)
                                    )
                            )
                    );
                }

                inboundCommandService.createInboundRegistry(
                        registryRequests,
                        sortableRequests,
                        params.getInboundExternalId(),
                        regKey,
                        getOrCreateAnyUser(params.getSortingCenter())
                );
                Inbound inbound = inboundRepository.findByExternalId(params.inboundExternalId).orElseThrow();
                if (params.getInboundStatus() != null) {
                    inbound.setInboundStatus(params.getInboundStatus());
                    inbound.setStatusUpdatedAt(Instant.now(clock));
                    entityManager.flush();
                }
                lotCommandService.createLotsAndPutPlaces(sortableRequests, registryRequests, inbound, null);
            });
            return this;
        });
        return inboundRepository.findByExternalId(params.inboundExternalId).orElseThrow();
    }

    public void linkSortableToInbound(Inbound inbound, String barcode, SortableType type, User user) {
        linkSortableToInbound(inbound.getExternalId(), barcode, type, user);
    }

    public void linkSortableToInbound(String inboundExternalId, String barcode, SortableType type, User user) {
        inboundFacade.linkToInbound(
                inboundExternalId,
                new LinkToInboundRequestDto(barcode, type),
                ScanLogContext.XDOC_ACCEPTANCE,
                user
        );
    }

    public void acceptInbound(long inboundId) {
        transactionTemplate.execute(ts -> {
            inboundCommandService.acceptInbound(inboundId);
            return null;
        });
    }

    /**
     * ТМ проставляет confirmed совершая putInboundRequest
     */
    public void confirmInbound(String externalId) {
        transactionTemplate.execute(ts -> {
            var inbound = inboundRepository.findByExternalId(externalId).orElseThrow();
            confirmInbound(inbound);
            return null;
        });
    }

    public void confirmInbound(Inbound inbound) {
        inboundCommandService.putInbound(
                from(inbound)
                        .withConfirmed(true)
        );
    }

    public InboundCreateRequest from(Inbound inbound) {
        return transactionTemplate.execute(ts -> {
            var inboundWithSession = inboundRepository.findByIdOrThrow(inbound.getId());
            return InboundCreateRequest.builder()
                    .externalId(inboundWithSession.getExternalId())
                    .warehouseFromId(inboundWithSession.getWarehouseFromId())
                    .inboundType(inboundWithSession.getType())
                    .fromDate(inboundWithSession.getFromDate())
                    .toDate(inboundWithSession.getToDate())
                    .courierRequest(from(inboundWithSession.getMovementCourier()))
                    .locationCreateRequest(from(inboundWithSession.getLocationTo()))
                    .comment(inboundWithSession.getComment())
                    .sortingCenter(inboundWithSession.getSortingCenter())
                    .nextLogisticPointId(inboundWithSession.getNextLogisticPointId())
                    .externalRequestId(inboundWithSession.getInformationListCode())
                    .confirmed(inboundWithSession.isConfirmed())
                    .build();
        });
    }

    public static MovementCourierRequest from(MovementCourier courier) {
        return new MovementCourierRequest(
                courier.getExternalId(),
                courier.getLegalEntityName(),
                courier.getLegalEntityLegalName(),
                courier.getCarNumber(),
                courier.getUid(),
                null
        );
    }

    public static LocationCreateRequest from(Location location) {
        if (location == null) {
            return null;
        }
        return LocationCreateRequest.builder()
                .country(location.getCountry())
                .locality(location.getLocality())
                .region(location.getRegion())
                .federalDistrict(location.getFederalDistrict())
                .subRegion(location.getSubRegion())
                .settlement(location.getSettlement())
                .street(location.getStreet())
                .house(location.getHouse())
                .building(location.getBuilding())
                .housing(location.getHousing())
                .room(location.getRoom())
                .zipCode(location.getZipCode())
                .porch(location.getPorch())
                .floor(
                        Optional.ofNullable(location.getFloor())
                                .orElse(null)
                )
                .metro(location.getMetro())
                .lat(location.getLat())
                .lng(location.getLng())
                .locationId(Optional.ofNullable(location.getLocationId()).orElse(null))
                .build();
    }

    public void inboundCarArrived(Inbound inbound, PutCarInfoRequest request) {
        inboundCommandService.inboundCarArrived(inbound.getId(), request);
    }

    public void inboundSaveVgh(SortingCenter sortingCenter, SaveVGHRequestDto request, String barcode) {
        inboundFacade.saveVGH(sortingCenter, request, barcode);
    }

    public void updateShipmentDate(Long orderId, LocalDate date, SortingCenter sortingCenter) {
        orderCommandService.updateShipmentDate(orderId, date, getOrCreateAnyUser(sortingCenter), true);
        transactionTemplate.execute(ts -> {
                    List<Place> places = placeRepository.findAllByOrderIdOrderById(orderId);
                    for (Place place : places) {
                        ScOrder order = scOrderRepository.findByIdOrThrow(orderId);
                        place.getHistory().size();
                        Instant shipmentTime = ScDateUtils.toNoon(date);
                        placeRouteSoService.updatePlaceRoutes(place, order.getCourier(),
                                shipmentTime,
                                getOrCreateAnyUser(place.getSortingCenter()));

                    }
                    return null;
                }
        );
    }


    public void updateCourier(Long orderId, CourierDto courierDto, SortingCenter sortingCenter) {
        orderCommandService.updateCourier(orderId, courierDto, getOrCreateAnyUser(sortingCenter));
        Courier courier = courierCommandService.findOrCreateCourier(courierDto, false);
        transactionTemplate.execute(ts -> {
                    placeRepository.findAllByOrderIdOrderById(orderId).forEach(
                            p -> {
                                if (
                                        p.getShipmentDate() == null
                                                && !SortableFlowSwitcherExtension.useNewRouteSoStage1()
                                ) {
                                    return;
                                }

                                Instant shipmentTime = p.getShipmentDate() == null
                                        ? null
                                        : ScDateUtils.toNoon(p.getShipmentDate());
                                placeRouteSoService.updatePlaceRoutes(p, courier,
                                        shipmentTime,
                                        getOrCreateAnyUser(p.getSortingCenter()));
                            }

                    );
                    entityManager.flush();
                    return null;
                }
        );
    }

    public void finishAcceptance(User user, String externalId) {
        inboundFacade.performActionWithScan(
                externalId,
                null,
                user,
                InboundAvailableApiAction.FINISH_INBOUND_ACCEPTANCE);
    }

    public void readyToReceiveInbound(Inbound inbound) {
        inboundCommandService.readyToReceiveInbound(inbound.getId());
    }

    public void finishInbound(Inbound inbound) {
        finishInbound(inbound, null);
    }

    public void finishInbound(Inbound inbound, User user) {
        inboundCommandService.fixInboundById(inbound.getId(), user);
    }

    public Outbound createOutbound(SortingCenter sortingCenter) {
        return transactionTemplate.execute(ts -> createOutbound(
                CreateOutboundParams.builder()
                        .externalId("111")
                        .fromTime(Instant.parse("2021-04-22T10:00:00Z"))
                        .toTime(Instant.parse("2021-04-22T12:00:00Z"))
                        .locationCreateRequest(locationCreateRequest())
                        .sortingCenter(sortingCenter)
                        .partnerToExternalId("")
                        .build()
        ));
    }

    public Outbound createOutbound(CreateOutboundParams params) {
        outboundCommandService.put(OutboundCreateRequest.builder()
                .externalId(params.getExternalId())
                .type(params.getType())
                .fromTime(params.getFromTime())
                .toTime(params.getToTime())
                .courierRequest(params.getCourierExternalId() == null ? null :
                        new MovementCourierRequest(params.getCourierExternalId(), "name",
                                "legalName",
                                Optional.ofNullable(params.carNumber).orElse(null),
                                212_85_06L, "phone2345"))
                .locationCreateRequest(locationCreateRequest())
                .comment("test outbound")
                .sortingCenter(params.getSortingCenter())
                .partnerToExternalId(params.getPartnerToExternalId())
                .logisticPointToExternalId(params.getLogisticPointToExternalId())
                .build());
        return outboundQueryService.getByExternalId(params.externalId);
    }

    public Registry bindRegistry(String outboundExternalId, RegistryType registryType) {
        return bindRegistry(outboundExternalId, null, registryType);
    }

    public Registry bindRegistry(String outboundExternalId, String regExtId, RegistryType registryType) {
        return transactionTemplate.execute(ts -> {
            var outbound = getOutbound(outboundExternalId);
            var registry = Registry.outboundRegistry(regExtId, outbound, registryType, null);
            var newRegistries = Optional.ofNullable(outbound.getRegistries())
                    .map(ArrayList::new)
                    .orElseGet(ArrayList::new);
            newRegistries.add(registry);
            outbound.setRegistries(newRegistries);

            return registry;
        });
    }

    public Registry bindRegistry(Inbound inbound, String registryExternalId, RegistryType registryType) {
        return transactionTemplate.execute(ts -> {
            var registry = Registry.inboundRegistry(registryExternalId, inbound, registryType);
            registryRepository.save(registry);
            return registry;
        });
    }

    public RegistryOrder bindInboundOrder(Inbound inbound, Registry registry, String placeId, String palletId) {
        return transactionTemplate.execute(ts -> {
            var inboundRegistryOrder = new RegistryOrder(
                    inbound.getExternalId(),
                    placeId,
                    registry.getId(),
                    palletId
            );
            boundRegistryRepository.save(inboundRegistryOrder);
            return inboundRegistryOrder;
        });
    }

    public RegistryOrder bindOrder(Registry registry, String orderExternalId, String placeId, String palletId) {
        return transactionTemplate.execute(ts -> {
            var inboundRegistryOrder = new RegistryOrder(
                    orderExternalId,
                    placeId,
                    registry.getId(),
                    palletId
            );
            boundRegistryRepository.save(inboundRegistryOrder);
            return inboundRegistryOrder;
        });
    }

    public RegistrySortable bindRegistrySortable(Registry registry, Sortable sortable) {
        return bindRegistrySortable(registry, sortable.getRequiredBarcodeOrThrow(), sortable.getType());
    }

    public RegistrySortable bindRegistrySortable(Registry registry, String externalId, SortableType sortableType) {
        return transactionTemplate.execute(ts -> {
            var registrySortable = new RegistrySortable(
                    registry,
                    externalId,
                    sortableType.getUnitType()
            );
            registrySortableRepository.save(registrySortable);
            return registrySortable;
        });
    }

    @Data
    @Builder
    public static class CreateInboundParams {

        private SortingCenter sortingCenter;

        @Builder.Default
        private String inboundExternalId = "inboundExternalId";

        @Builder.Default
        private InboundType inboundType = InboundType.DEFAULT;

        private OffsetDateTime fromDate;

        private OffsetDateTime toDate;

        private String warehouseFromExternalId;

        @Builder.Default
        private String courierExternalId = "inboundCourierExternalId";

        @Builder.Default
        private MovementCourier movementCourier = null;

        @Builder.Default
        private String courierPhone = null;

        @Builder.Default
        private InboundStatus inboundStatus = InboundStatus.CREATED;

        @Builder.Default
        private LocationCreateRequest locationCreateRequest = locationCreateRequest();

        private String nextLogisticPointId;

        private String informationListBarcode;

        private boolean confirmed;

        private String transportationId;

        private String realSupplierName;

        @Builder.Default
        private List<Pair<String, String>> plainOrders = Collections.emptyList();

        /**
         * Словарь <внешний Id реестра> -> <Список из пар (Внешний Id заказа, Внешний Id посылки)>
         */
        @Builder.Default
        private Map<String, List<Pair<String, String>>> registryMap =
                Map.of("registry_1", List.of(Pair.of("order_ext_id_1", "order_ext_id_1")));

        /**
         * Словарь <заказ> -> <паллета>
         */
        @Builder.Default
        private Map<String, String> placeInPallets = null;

        /**
         * Словарь <паллета> -> <пломба>
         */
        @Builder.Default
        private Map<String, String> palletToStamp = null;

        @Builder.Default
        private Map<String, Long> crossDockPalletDestinations = null;

    }

    @Data
    @Builder
    public static class CreateOutboundParams {

        private SortingCenter sortingCenter;

        @Builder.Default
        private String externalId = "outboundExternalId";

        @Builder.Default
        private OutboundType type = OutboundType.ORDERS_RETURN;

        private Instant fromTime;

        private Instant toTime;

        @Builder.Default
        private String courierExternalId = "outboundCourierExternalId";

        @Builder.Default
        private LocationCreateRequest locationCreateRequest = locationCreateRequest();

        private String partnerToExternalId;

        private String logisticPointToExternalId;

        @Nullable
        private String carNumber;

    }

    @Data
    @Builder
    public static class CreateOrderParams {

        private SortingCenter sortingCenter;

        @Builder.Default
        private DeliveryService deliveryService = null;

        @Builder.Default
        private String externalId = null;

        @Builder.Default
        private DeliveryServiceType dsType = DeliveryServiceType.LAST_MILE_COURIER;

        @Builder.Default
        private boolean createTwoPlaces = false;

        @Builder.Default
        private boolean isPackageRequired = false;

        @Builder.Default
        private LocalDate deliveryDate = null;

        @Builder.Default
        private LocalDate shipmentDate = null;

        @Builder.Default
        private LocalDate shipmentDateTime = null;

        @Builder.Default
        private String warehouseFromId = null;

        @Builder.Default
        private String warehouseFromName = null;

        @Builder.Default
        private String warehouseReturnId = null;

        @Builder.Default
        private String warehouseReturnName = null;

        @Builder.Default
        private ReturnType warehouseReturnType = ReturnType.WAREHOUSE;

        private Order request;

        @Builder.Default
        private Boolean warehouseCanProcessDamagedOrders = null;

        @Builder.Default
        private boolean isClientReturn = false;

        @Builder.Default
        private boolean isFashion = false;

        private List<String> places;

        @Builder.Default
        private String recipientEmail = null;

        @Builder.Default
        private boolean isC2c = false;

        public static class CreateOrderParamsBuilder {

            public CreateOrderParamsBuilder() {
                this.places = Collections.emptyList();
            }

            public CreateOrderParamsBuilder sortingCenter(SortingCenter sortingCenter) {
                this.sortingCenter = sortingCenter;
                this.request = ffOrder(sortingCenter.getToken());
                return this;
            }

            public CreateOrderParamsBuilder places(List<String> places) {
                this.places = places;
                return this;
            }

            public CreateOrderParamsBuilder places(String... places) {
                this.places = Arrays.asList(places);
                return this;
            }

        }

    }

    public CourierShift createCourierShiftForScOrder(OrderLike scOrder, LocalDate shiftDate, LocalTime shiftStartTime) {
        return courierShiftRepository.save(
                new CourierShift(
                        scOrder.getSortingCenter(),
                        scOrder.getCourier(),
                        shiftDate,
                        shiftStartTime
                )
        );
    }

    public static boolean useNewSortableFlow() {
        return true;
    }

    public static RouteOrdersFilter ordersRegistryFilter(RouteDocumentType documentType) {
        return RouteOrdersFilter.builder()
                .routeDocumentType(documentType)
                .build();
    }

    @Transactional
    public Route getRoute(long routeId) {
        return routeNonBlockingQueryService.findWithRouteFinish(routeId)
                .allowReading(); //В тех местах где этот экземпляр передается в тестируемые методы использовано
                                 // route.disallowRead()
    }



    @Transactional
    public List<RouteCell> findRoutesCell(Cell cell) {
        return StreamEx.of(routeRepository.findAll())
                .flatMap(r -> r.allowNextRead().getRouteCells().stream())
                .filter(rc -> rc.getCell().equals(cell))
                .toList();
    }

    @Transactional
    public List<RouteSoSite> findRouteSoSites(Cell cell) {
        return StreamEx.of(routeSoSiteRepository.findAll())
                .filter(rc -> rc.getCell().equals(cell))
                .toList();
    }

    @Transactional
    public SortableLot shipLotRouteByParentCell(SortableLot lot) {
        var route = findCellActiveRoute(lot.getParentCellId(), lot.getSortingCenter());
        routeCommandService.finishOutgoingRouteWithLot(route.getId(), new RouteFinishLotPalletRequest(
                new ScContext(getOrCreateStoredUser(lot.getSortingCenter())), lot.getLotId(), null, null)
        );
        return sortableLotService.findByLotIdOrThrow(lot.getLotId());
    }

    @Transactional
    public void shipLots(Long routeId, SortingCenter sortingCenter) {
        routeCommandService.finishRouteWithLots(routeId, "<car-barcode>", sortingCenter);
    }

    @Transactional
    public void shipLotWithFilter(Long routeId, SortableLot lot) {
        SortingCenter sortingCenter = lot.getSortingCenter();
        var user = getOrCreateAnyUser(sortingCenter);
        routeCommandService.finishRouteWithLots(routeId, "<car-barcode>",
                                                Set.of(lot), new ScContext(user), sortingCenter);
    }

    public List<String> findTask(ScQueueType scQueueType) {
        return jdbcTemplate.queryForList(
                        "select task from queue_task where queue_name = ?",
                        scQueueType.name()
                ).stream()
                .map(stringObjectMap -> String.valueOf(stringObjectMap.get("task")))
                .toList();
    }

    public void setConfiguration(String name, Object value) {
        jdbcTemplate.update("INSERT INTO configuration(key, value) " +
                "values(?, ?)", name, value);
    }

    public OutboundPartnerDto shipOutbound(String outboundExternalId) {
        return outboundFacade.shipOutboundByPIAdmin(outboundExternalId, null);
    }

    /**
     * смещение ScOrder.id, что бы они не пересекались с Sortable.id
     * <p>
     * существуют ручки, которые ориентируются на id что опознать чем является груз
     * Sortable или ScOrder
     * На проде ScOrder.id значительно превосходит Sortable.id
     * А в тестах они стартуют с одинаковым id
     * Вызов этого метода позволяет эмулировать ситуацию на проде
     */
    public void increaseScOrderId() {
        jdbcTemplate.queryForObject("SELECT setval('seq_order', 100, true)", Long.class);
    }



    @AllowRouteFieldReading
    public class TestOrderBuilder {

        private long orderId;
        private CreateOrderParams params;

        public ScOrder get() {
            // for non-transactional tests
            return scOrderRepository.findByIdOrThrow(orderId);
        }

        public ScOrderWithPlaces getOrderWithPlaces() {
            // for non-transactional tests
            return new ScOrderWithPlaces(
                    scOrderRepository.findByIdOrThrow(orderId),
                    placeRepository.findAllByOrderIdOrderById(orderId)
            );
        }

        public Map<String, Place> getPlaces() {
            // for non-transactional tests
            return placeRepository.findAllByOrderIdOrderById(orderId).stream()
                    .collect(Collectors.toMap(p -> p.getMainPartnerCode(), p -> p));
        }

        public Place getPlace(String mainPartnerCode) {
            // for non-transactional tests
            Place place = getPlaces().get(mainPartnerCode);
            if (place != null) {
                return place;
            }
            throw new IllegalArgumentException("Order doesn't have place with barcode '" + mainPartnerCode + "'");
        }

        public List<Place> getPlacesList() {
            // for non-transactional tests
            return placeRepository.findAllByOrderIdOrderById(orderId);
        }

        public Place getPlace() {
            // for non-transactional tests
            List<Place> places = getPlacesList();
            assertThat(places).hasSize(1);
            return places.get(0);
        }

        public Order orderRequest(CreateOrderParams params) {
            return orderRequestBuilder(params).build();
        }

        public Order.OrderBuilder orderRequestBuilder(CreateOrderParams params) {
            this.params = params;
            var orderRequest = params.getRequest();
            ResourceId externalId = params.getExternalId() == null
                    ? orderRequest.getOrderId()
                    : new ResourceId(params.getExternalId(), null);
            String yandexId = externalId.getYandexId();
            return new Order.OrderBuilder(
                    externalId,
                    orderRequest.getLocationTo(),
                    orderRequest.getItems(),
                    orderRequest.getKorobyte(),
                    orderRequest.getCargoCost(),
                    orderRequest.getAssessedCost(),
                    orderRequest.getPaymentMethod(),
                    params.getDeliveryService() == null
                            ? orderRequest.getDelivery()
                            : deliveryWithIdAndName(
                            orderRequest.getDelivery(),
                            params.getDeliveryService().getYandexId(),
                            params.getDeliveryService().getName()
                    ),
                    orderRequest.getDeliveryType(),
                    orderRequest.getDeliveryCost(),
                    orderRequest.getDocumentData(),
                    warehouseWithIdAndName(
                            orderRequest.getWarehouse(),
                            params.getWarehouseReturnId(),
                            Optional.ofNullable(params.getWarehouseReturnName())
                                    .orElse(orderRequest.getWarehouse().getIncorporation())
                    ),
                    warehouseWithIdAndName(
                            orderRequest.getWarehouseFrom(),
                            params.getWarehouseFromId(),
                            Optional.ofNullable(params.getWarehouseFromName())
                                    .orElse(orderRequest.getWarehouseFrom().getIncorporation())
                    ),
                    recipientWithEmail(orderRequest.getRecipient(), params.getRecipientEmail()),
                    orderRequest.getWeight(),
                    orderRequest.getAmountPrepaid()
            )
                    .setSender(params.getRequest().getSender())
                    .setPlaces(!params.createTwoPlaces && params.getPlaces().isEmpty()
                            ? Collections.emptyList()
                            : places(yandexId, orderRequest, params)
                    )
                    .setDeliveryDate(
                            params.getDeliveryDate() == null
                                    ? orderRequest.getDeliveryDate()
                                    : DateTime.fromLocalDateTime(params.getDeliveryDate().atStartOfDay())
                    )
                    .setShipmentDate(
                            params.getShipmentDate() == null
                                    ? orderRequest.getShipmentDate()
                                    : DateTime.fromLocalDateTime(params.getShipmentDate().atStartOfDay())
                    )
                    .setReturnInfo(
                            returnInfoWithTypeAndName(
                                    orderRequest.getReturnInfo(),
                                    params.getWarehouseReturnType(),
                                    params.warehouseReturnName)
                    )
                    .setTags(params.isC2c() ? Set.of(ScOrderMapper.C2C_TAG) : Set.of())
                    .setPhysicalPersonSender(params.isC2c() ? new PhysicalPersonSender(
                            new Person("Имя", "Фамилия", null, null),
                            new Phone("+7 (999) 123-45-67", null)
                    ) : null);
        }

        private OrderIdResponse createClientReturn(CreateOrderParams params) {
            return orderCommandService.createClientReturn(new CreateClientReturnRequest(
                    params.getSortingCenter().getId(),
                    params.getSortingCenter().getToken(),
                    params.getSortingCenter().getYandexId(),
                    null,
                    "VOZVRAT_SF_PVZ_" + Objects.requireNonNullElse(params.getExternalId(), "123"),
                    params.getShipmentDate(),
                    null,
                    null,
                    null,
                    null,
                    null
            ), getOrCreateAnyUser(params.getSortingCenter()));
        }

        private OrderIdResponse createFashion(CreateOrderParams params) {
            return orderCommandService.createClientReturn(new CreateClientReturnRequest(
                    params.getSortingCenter().getId(),
                    params.getSortingCenter().getToken(),
                    params.getSortingCenter().getYandexId(),
                    null,
                    "HLP_PVZ_FSN_RET_" + Objects.requireNonNullElse(params.getExternalId(), "123"),
                    params.getShipmentDate(),
                    null,
                    null,
                    null,
                    null,
                    null
            ), storedUser(params.getSortingCenter(), params.getSortingCenter().getId() + 10000));
        }

        public TestOrderBuilder create(CreateOrderParams params) {
            transactionTemplate.execute(ts -> {
                var orderRequest = orderRequest(params);
                if (deliveryServicePropertyRepository.findAllByDeliveryServiceYandexId(
                                orderRequest.getDelivery().getDeliveryId().getYandexId()
                        ).stream()
                        .noneMatch(p -> Objects.equals(p.getKey(),
                                DeliveryServiceProperty.TYPE_ON_SC_PREFIX + params.getSortingCenter().getId()))) {
                    deliveryServicePropertyRepository.save(new DeliveryServiceProperty(
                            orderRequest.getDelivery().getDeliveryId().getYandexId(),
                            DeliveryServiceProperty.TYPE_ON_SC_PREFIX + params.getSortingCenter().getId(),
                            params.getDsType().name()
                    ));
                }
                CreateOrderRestrictedData restrictedData = null;
                if (params.isC2c()) {
                    restrictedData = new CreateOrderRestrictedData(
                            new OrderTransferCodes.OrderTransferCodesBuilder()
                                    .setReturnOutbound(new OrderTransferCode.OrderTransferCodeBuilder()
                                            .setVerification("00000")
                                            .build())
                                    .build()
                    );
                }
                if (params.isClientReturn() || params.isFashion()) {
                    storedWarehouse(
                            params.isFashion()
                                    ? ClientReturnBarcodePrefix.CLIENT_RETURN_FSN.getWarehouseReturnId()
                                    : ClientReturnBarcodePrefix.CLIENT_RETURN_PVZ.getWarehouseReturnId()
                    );
                    storedDeliveryService("ds_for_client_return", false);
                    storedCourier(-1, ClientReturnService.CLIENT_RETURN_COURIER);
                    setWarehouseProperty(
                            orderRequest.getWarehouse().getWarehouseId().getYandexId(),
                            WarehouseProperty.CAN_PROCESS_CLIENT_RETURNS,
                            "true");
                    this.orderId = params.isFashion()
                            ? createFashion(params).getId()
                            : createClientReturn(params).getId();
                } else {
                    // Создание заказа ЗДЕСЬ
                    this.orderId = orderCommandService.createOrder(new OrderCreateRequest(
                            params.getSortingCenter(), orderRequest, restrictedData
                    ), getOrCreateAnyUser(params.getSortingCenter())).getId();
                }
                if (warehousePropertyRepository.findAllByWarehouseYandexId(
                                orderRequest.getWarehouse().getWarehouseId().getYandexId()).stream()
                        .noneMatch(p -> Objects.equals(p.getKey(), WarehouseProperty.CAN_PROCESS_DAMAGED_ORDERS))
                        && params.getWarehouseCanProcessDamagedOrders() != null
                ) {
                    setWarehouseProperty(
                            orderRequest.getWarehouse().getWarehouseId().getYandexId(),
                            WarehouseProperty.CAN_PROCESS_DAMAGED_ORDERS,
                            Objects.toString(params.getWarehouseCanProcessDamagedOrders()));
                }
                entityManager.flush();
                return null;
            });
            return this;
        }

        private List<ru.yandex.market.logistic.api.model.fulfillment.Place> places(
                String yandexId,
                ru.yandex.market.logistic.api.model.fulfillment.Order orderRequest,
                CreateOrderParams params
        ) {
            if (params.getPlaces().isEmpty()) {
                return List.of(
                        new ru.yandex.market.logistic.api.model.fulfillment.Place(
                                new ResourceId(yandexId + "-1", null),
                                orderRequest.getKorobyte(),
                                List.of(
                                        new PartnerCode("123", "pk-" + yandexId + "-1")
                                ),
                                List.of()
                        ),
                        new ru.yandex.market.logistic.api.model.fulfillment.Place(
                                new ResourceId(yandexId + "-2", null),
                                orderRequest.getKorobyte(),
                                List.of(
                                        new PartnerCode("123", "pk-" + yandexId + "-2")
                                ),
                                List.of()
                        )
                );
            } else {
                return params.getPlaces().stream()
                        .map(placeId -> new ru.yandex.market.logistic.api.model.fulfillment.Place(
                                new ResourceId(placeId, null),
                                orderRequest.getKorobyte(),
                                List.of(
                                        new PartnerCode("123", placeId),
                                        new PartnerCode("124", placeId)
                                ),
                                List.of()
                        ))
                        .toList();
            }
        }

        private ru.yandex.market.logistic.api.model.fulfillment.Delivery deliveryWithIdAndName(
                ru.yandex.market.logistic.api.model.fulfillment.Delivery delivery, String id, String name
        ) {
            var resourceId = new ResourceId(id, id);
            return new Delivery(
                    resourceId,
                    name,
                    delivery.getPhones(), delivery.getContract(), delivery.getDocs(), delivery.getPriority(),
                    delivery.getIntakeTime(), delivery.getCourier()
            );
        }

        private ru.yandex.market.logistic.api.model.fulfillment.Warehouse warehouseWithIdAndName(
                ru.yandex.market.logistic.api.model.fulfillment.Warehouse warehouse,
                @Nullable String id,
                @Nullable String name
        ) {
            if (id == null) {
                return warehouse;
            }
            var resourceId = new ResourceId(id, id);
            return new ru.yandex.market.logistic.api.model.fulfillment.Warehouse(
                    resourceId, resourceId,
                    warehouse.getAddress(), warehouse.getInstruction(), warehouse.getSchedule(),
                    warehouse.getContact(), warehouse.getPhones(), name
            );
        }

        private ru.yandex.market.logistic.api.model.fulfillment.ReturnInfo returnInfoWithTypeAndName(
                ru.yandex.market.logistic.api.model.fulfillment.ReturnInfo returnInfo,
                @Nullable ReturnType type,
                @Nullable String name
        ) {
            PartnerInfo srcPartnerTo = returnInfo.getPartnerTo();
            return new ru.yandex.market.logistic.api.model.fulfillment.ReturnInfo(
                    new PartnerInfo(
                            srcPartnerTo.getPartnerId(),
                            Optional.ofNullable(name).orElse(srcPartnerTo.getIncorporation())
                    ),
                    returnInfo.getPartnerTransporter(),
                    Optional.ofNullable(type).orElse(returnInfo.getType())
            );
        }

        private Recipient recipientWithEmail(Recipient recipient, @Nullable String recipientEmail) {
            if (recipientEmail == null) {
                return recipient;
            }
            return new Recipient(
                    recipient.getFio(),
                    recipient.getPhones(),
                    Optional.ofNullable(recipientEmail)
                            .map(email -> new Email(email))
                            .orElse(recipient.getEmail()));
        }

        public TestOrderBuilder createClientReturn(
                Long scId, String token, String yandexId, CourierDto courierDto, String barcode, LocalDate returnDate
        ) {
            transactionTemplate.execute(ts -> {
                orderCommandService.createClientReturn(new CreateClientReturnRequest(
                        scId, token, yandexId, courierDto, barcode, returnDate, null,
                        null, null, null, null
                ), getOrCreateAnyUser(scId));
                var scOrder = scOrderRepository.findBySortingCenterAndExternalId(
                        sortingCenterRepository.findByIdOrThrow(scId), barcode
                ).orElseThrow();
                this.orderId = scOrder.getId();
                return null;
            });
            return this;
        }

        public TestOrderBuilder updateShipmentDate(LocalDate date) {
            orderCommandService.updateShipmentDate(orderId, date, getOrCreateAnyUser(get().getSortingCenterId()), true);
            transactionTemplate.execute(ts -> {
                        List<Place> places = placeRepository.findAllByOrderIdOrderById(orderId);
                        for (Place place : places) {
                            ScOrder order = scOrderRepository.findByIdOrThrow(orderId);
                            place.getHistory().size();
                            Instant shipmentTime = ScDateUtils.toNoon(date);
                            placeRouteSoService.updatePlaceRoutes(place, order.getCourier(),
                                    shipmentTime,
                                    getOrCreateAnyUser(place.getSortingCenter()));

                        }
                        return null;
                    }
            );
            return this;
        }

        public TestOrderBuilder updateShipmentDateTime(LocalDateTime dateTime) {
            orderCommandService.updateShipmentDateTime(orderId, dateTime,
                    getOrCreateAnyUser(get().getSortingCenterId()));
            return this;
        }

        public TestOrderBuilder updateCourier(Courier courier) {
            return updateCourier(new CourierDto(courier.getId(), courier.getName(),
                    courier.getCarNumber(), courier.getCarDescription(), courier.getPhone(), courier.getCompanyName(),
                    courier.getDeliveryServiceId(), courier.isDeliveryService()));
        }

        public TestOrderBuilder updateCourier(CourierDto courierDto) {
            orderCommandService.updateCourier(orderId, courierDto, getOrCreateAnyUser(params.getSortingCenter()));
            Courier courier = courierCommandService.findOrCreateCourier(courierDto, false);
            transactionTemplate.execute(ts -> {
                        placeRepository.findAllByOrderIdOrderById(orderId).forEach(
                                p -> {
                                    if (
                                            p.getShipmentDate() == null
                                                    && !SortableFlowSwitcherExtension.useNewRouteSoStage1()
                                    ) {
                                        return;
                                    }

                                    Instant shipmentTime = p.getShipmentDate() == null
                                            ? null
                                            : ScDateUtils.toNoon(p.getShipmentDate());
                                    placeRouteSoService.updatePlaceRoutes(p, courier,
                                            shipmentTime,
                                            getOrCreateAnyUser(p.getSortingCenter()));
                                }

                        );
                        entityManager.flush();
                        return null;
                    }
            );
            return this;
        }

        public TestOrderBuilder cancel() {
            orderCommandService.cancelOrder(orderId, "1", false, getOrCreateAnyUser(params.getSortingCenter()));
            return this;
        }

        public TestOrderBuilder cancelWithPosponed() {
            orderCommandService.cancelOrder(orderId, "1", true, getOrCreateAnyUser(params.getSortingCenter()));
            return this;
        }

        public TestOrderBuilder accept() {
            accept(getOrCreateStoredUser(get().getSortingCenter()));
            return this;
        }

        public TestOrderBuilder accept(User user) {
            return acceptPlaces();
        }

        public TestOrderBuilder acceptPlaces() {
            return acceptPlaces(getPlaces().keySet().toArray(String[]::new));
        }

        public TestOrderBuilder acceptPlaces(String... placeExternalIds) {
            Arrays.stream(placeExternalIds).forEach(this::acceptPlace);
            return this;
        }

        public TestOrderBuilder acceptPlaces(List<String> placeExternalIds) {
            placeExternalIds.forEach(this::acceptPlace);
            return this;
        }

        public TestOrderBuilder acceptPlace(String placeExternalId) {
            acceptService.acceptPlace(
                    placeRequest(getOrCreateStoredUser(get().getSortingCenter()), placeExternalId)
            );
            return this;
        }

        public TestOrderBuilder sort() {
            return sortPlaces();
        }

        public TestOrderBuilder sort(User user) {
            return sortByUser(request(user));
        }

        public TestOrderBuilder sortPlaces() {
            return sortPlaces(getPlaces().keySet().toArray(String[]::new));
        }

        public TestOrderBuilder sortPlace(String placeExternalId) {
            return sortPlaceByUser(placeRequest(getOrCreateStoredUser(get().getSortingCenter()), placeExternalId));
        }

        public TestOrderBuilder sortPlaces(String... placeExternalIds) {
            Arrays.stream(placeExternalIds).forEach(this::sortPlace);
            return this;
        }

        public TestOrderBuilder sortPlaces(long cellId, String... placeExternalIds) {
            Arrays.stream(placeExternalIds).forEach(placeExternalId -> sortPlace(placeExternalId, cellId));
            return this;
        }

        public TestOrderBuilder sortPlaces(List<String> placeExternalIds) {
            placeExternalIds.forEach(this::sortPlace);
            return this;
        }

        public TestOrderBuilder sortPlace(String placeExternalId, User user) {
            return sortPlaceByUser(placeRequest(user, placeExternalId));
        }

        public TestOrderBuilder sort(long cellId) {
            sortPlaces(cellId, getPlaces().keySet().toArray(String[]::new));
            return this;
        }

        public TestOrderBuilder sortPlace(String placeExternalId, long cellId) {
            return sortPlace(placeExternalId, cellId, false);
        }

        private TestOrderBuilder sortPlace(String placeExternalId, long cellId, boolean ignoreTodayRouteOnKeep) {
            placeCommandService.sortPlace(
                    placeRequest(getOrCreateStoredUser(get().getSortingCenter()), placeExternalId), cellId,
                    ignoreTodayRouteOnKeep
            );
            return this;
        }

        public TestOrderBuilder keepPlaces(String... placeExternalIds) {
            return keepPlaces(false, placeExternalIds);
        }

        public TestOrderBuilder keepPlaces(@Nullable Long cellId, String... placeExternalIds) {
            return keepPlaces(false, cellId, placeExternalIds);
        }

        public TestOrderBuilder keepPlacesIgnoreTodayRoute(String... placeExternalIds) {
            return keepPlaces(true, placeExternalIds);
        }

        public TestOrderBuilder keepPlaces(boolean ignoreTodayRoute,
                                           @Nullable Long cellId, String... placeExternalIds) {
            transactionTemplate.execute(ts -> {
                Arrays.stream(placeExternalIds).forEach(
                        pid -> sortPlace(
                                pid,
                                cellPolicy.findAndValidateOrCreateBufferCell(get(), cellId).getId(),
                                ignoreTodayRoute
                        )
                );
                return null;
            });
            return this;
        }

        public TestOrderBuilder keepPlaces(boolean ignoreTodayRoute, String... placeExternalIds) {
            return keepPlaces(ignoreTodayRoute, null, placeExternalIds);
        }

        public TestOrderBuilder keep() {
            return keepPlaces(orderPlace(get()).getMainPartnerCode());
        }

        public TestOrderBuilder keep(boolean ignoreTodayRoute) {
            return keepPlaces(ignoreTodayRoute, getPlaces().keySet().toArray(String[]::new));
        }

        public TestOrderBuilder keep(long cellId) {
            return keepPlaces(cellId, getPlaces().keySet().toArray(String[]::new));
        }

        public TestOrderBuilder prepare() {
            String[] places = getPlaces().keySet().toArray(String[]::new);
            if (places.length != 1) {
                throw new RuntimeException("Данный флоу работает только с однометными заказами");
            }
            return preparePlace(places[0]);
        }

        public TestOrderBuilder preparePlace(String placeExternalId) {
            transactionTemplate.execute(ts -> {
                ScOrder order = get();
                Long routeId;
                if (SortableFlowSwitcherExtension.useNewRouteSoStage2()) {
                    routeId = getRouteSo(findOutgoingCourierRoute(order).orElseThrow()).getId();
                } else {
                    routeId = findOutgoingCourierRoute(order).orElseThrow().getId();
                }
                preShipService.prepareToShipPlace(new PlaceScRequest(
                                new PlaceId(orderId, placeExternalId), getOrCreateStoredUser(order.getSortingCenter())),
                        routeId,
                        findOutgoingCellId(anyOrderPlace(order))
                );
                return null;
            });
            return this;
        }

        public TestOrderBuilder prepareToShipLot() {
            return prepareToShipLot(1);
        }

        public TestOrderBuilder prepareToShipLot(long lotId) {
            lotCommandService.prepareToShipLot(lotId, SortableAPIAction.READY_FOR_SHIPMENT,
                    getOrCreateStoredUser(this.get().getSortingCenter()));
            return this;
        }

        public TestOrderBuilder changeRouteId(long newId) {
            transactionTemplate.execute(ts -> {
                var route = routeNonBlockingQueryService.findPlaceOutgoingRoute(getPlacesList().get(0)).orElseThrow();
                jdbcTemplate.update("ALTER TABLE route DISABLE TRIGGER ALL;");
                jdbcTemplate.update("ALTER TABLE route_cell DISABLE TRIGGER ALL;");
                jdbcTemplate.update("ALTER TABLE route_finish DISABLE TRIGGER ALL;");
                jdbcTemplate.update("update route set id = ? where id = ?", newId, route.getId());
                jdbcTemplate.update("update route_cell set route_id = ? where route_id = ?",
                        newId, route.getId());
                jdbcTemplate.update("update route_finish set route_id = ? where route_id = ?",
                        newId, route.getId());
                jdbcTemplate.update("ALTER TABLE route_finish ENABLE TRIGGER ALL;");
                jdbcTemplate.update("ALTER TABLE route_cell ENABLE TRIGGER ALL;");
                jdbcTemplate.update("ALTER TABLE route ENABLE TRIGGER ALL;");
                return null;
            });
            return this;
        }

        /**
         * Шипает маршрут любой посылки заказа
         */
        public TestOrderBuilder ship() {
            Place place = getPlacesList().get(0);
            transactionTemplate.execute(ts -> {
                Route route = null;
                RouteSo routeSo = null;
                boolean sortRouteSo = TestFactory.sortWithRouteSo();
                if (sortRouteSo) {
                    routeSo = place.getOutRoute();
                } else {
                    route = routeNonBlockingQueryService.findPlaceOutgoingRoute(place).orElseThrow(() ->
                            new IllegalArgumentException("Place " + place.getId() + " has no outgoing route - can't ship")
                    );
                }


                routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                        sortRouteSo ? routeSo.getId() : route.getId(),
                        new ScContext(getOrCreateStoredUser(place.getSortingCenter())),
                        place.getCellId().stream().toList(),
                        place.getExternalId(),
                        false
                ));
                return null;
            });
            return this;
        }

        public TestOrderBuilder shipPlaces() {
            getPlacesList().forEach(p -> shipPlaceInternal(p, null));
            return this;
        }

        public TestOrderBuilder shipLot() {
            return shipLot(1);
        }

        public TestOrderBuilder shipLot(long lotId) {
            transactionTemplate.execute(ts -> {
                ScOrder order = get();
                Route route = routeNonBlockingQueryService.findPlaceOutgoingRoute(anyOrderPlace(order))
                        .orElseThrow();
                routeCommandService.finishOutgoingRouteWithLot(getRouteIdForSortableFlow(route), new RouteFinishLotPalletRequest(
                        new ScContext(getOrCreateStoredUser(order.getSortingCenter())), lotId, null, null));
                return null;
            });
            return this;
        }

        public TestOrderBuilder shipPlace(String placeExternalId) {
            Place place = placeRepository.findByOrderIdAndMainPartnerCode(get().getId(), placeExternalId)
                    .orElseThrow();
            shipPlaceInternal(place, null);
            return this;
        }

        public TestOrderBuilder shipPlaces(String... placeExternalIds) {
            Arrays.stream(placeExternalIds).forEach(this::shipPlace);
            return this;
        }

        public TestOrderBuilder shipPlaces(List<String> placeExternalIds) {
            placeExternalIds.forEach(this::shipPlace);
            return this;
        }

        public TestOrderBuilder shipWithBarCode(String transportBarCode) {
            getPlacesList().forEach(p -> shipPlaceInternal(p, transportBarCode));
            return this;
        }

        private void shipPlaceInternal(Place place, @Nullable String transportBarCode) {
            User user = getOrCreateStoredUser(place.getSortingCenter());
            transactionTemplate.execute(ts -> {
                boolean sortRouteSo = TestFactory.sortWithRouteSo();
                Route route = null;
                RouteSo routeSo = null;
                if (sortRouteSo) {
                    routeSo = place.getOutRoute();
                } else {
                    route = routeNonBlockingQueryService.findPlaceOutgoingRoute(place)
                            .orElseThrow(() -> new IllegalArgumentException("Place " + place.getId() +
                                    " has no outgoing route - can't ship"));
                }

                routeCommandService.finishOutgoingRouteWithCell(new RouteFinishByCellsRequest(
                        sortRouteSo ? routeSo.getId() : route.getId(),
                        new ScContext(user),
                        place.getCellId().stream().toList(),
                        transportBarCode,
                        place.getExternalId(),
                        place.getMainPartnerCode(),
                        null,
                        false
                ));
                return null;
            });
        }

        public TestOrderBuilder markOrderAsDamaged() {
            orderCommandService.markOrderAsDamaged(request(getOrCreateStoredUser(get().getSortingCenter())));
            return this;
        }

        public TestOrderBuilder makeReturn() {
            ScOrder order = scOrderRepository.findById(orderId).orElseThrow();
            orderCommandService.returnOrdersByIds(List.of(orderId), getOrCreateAnyUser(order.getSortingCenter()),
                    false);
            return this;
        }


        public TestOrderBuilder setupClockPlusDays(int days) {
            setupMockClock(clock, Instant.now(clock).plus(days, ChronoUnit.DAYS));
            return this;
        }

        private TestOrderBuilder sortByUser(OrderScRequest request) {
            placeCommandService.sortPlace(placeScRequest(request), findOutgoingCellId(get()), false);
            return this;
        }

        private TestOrderBuilder sortPlaceByUser(PlaceScRequest request) {
            transactionTemplate.execute(ts -> {
                placeCommandService.sortPlace(request,
                        findOutgoingCellId(placeRepository.findByOrderIdAndMainPartnerCode(
                                request.getPlaceId().getOrderId(), request.getPlaceId().getExternalId()).orElseThrow()
                        ),
                        false);
                return null;
            });
            return this;
        }

        public TestOrderBuilder sortToLot() {
            return sortToLot("SC_LOT_100000", SortableType.PALLET);
        }

        public TestOrderBuilder sortToLot(String lotExternalId, SortableType lotType) {
            Place place = orderPlace(orderId);
            return sortPlaceToLot(lotExternalId, lotType, place.getMainPartnerCode());
        }

        public TestOrderBuilder sortPlacesToLot(String... placeExternalIds) {
            return sortPlaceToLot("SC_LOT_100000", SortableType.PALLET, placeExternalIds);
        }

        public TestOrderBuilder sortPlaceToLot(
                String lotExternalId,
                SortableType lotType,
                String... placeExternalIds
        ) {
            Place place = getPlace(placeExternalIds[0]);
            var lot = findOrCreateLotToSort(lotExternalId, lotType, place);

            StreamEx.of(placeExternalIds)
                    .forEach(placeExternalId -> {
                        PlaceScRequest placeRequest = new PlaceScRequest(
                                new PlaceId(orderId, placeExternalId),
                                getOrCreateStoredUser(place.getSortingCenter()));
                        placeCommandService.sortPlaceToLot(placeRequest, lot.getLotId());
                    });
            return this;
        }

        private SortableLot findOrCreateLotToSort(String lotExternalId, SortableType lotType, Place place) {
            var lot = sortableLotService.findByExternalIdAndSortingCenter(lotExternalId, place.getSortingCenter());
            if (lot.isPresent()) {
                return lot.get();
            }
            Cell cell = getCellToCreateLotIn(place);
            CreateLotRequest createLotRequest = new CreateLotRequest(
                    lotType,
                    cell, place.getSortingCenter(),
                    false, LotStatus.CREATED, lotExternalId, null, null, null, null, null, false);
            return lotCommandService.createLot(createLotRequest);
        }

        private Cell getCellToCreateLotIn(Place place) {
            if (place.getCell() != null) {
                // посылка уже лежит в ячейке
                return place.getCell();
            }
            return transactionTemplate.execute(ts -> findCell(findOutgoingCellId(place)));
        }

        public TestOrderBuilder sortToLot(long lotId) {
            for (Place place : getPlacesList()) {
                sortPlaceToLot(lotId, place.getMainPartnerCode());
            }
            return this;
        }

        public TestOrderBuilder sortPlaceToLot(long lotId, String placeExternalId) {
            placeCommandService.sortPlaceToLot(
                    placeRequest(getOrCreateStoredUser(get().getSortingCenter()), placeExternalId), lotId
            );
            return this;
        }

        private OrderScRequest request(User user) {
            return new OrderScRequest(orderId, get().getExternalId(), user);
        }

        private PlaceScRequest placeRequest(User user, String externalId) {
            return new PlaceScRequest(new PlaceId(orderId, externalId), user);
        }

        public TestOrderBuilder enableSortMiddleMileToLot() {
            var order = get();
            setSortingCenterProperty(order.getSortingCenterId(),
                    SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, true);
            return this;
        }

    }

    public Pool storedPool(long id, SortingCenter sortingCenter, String name, RouteDestinationType destinationType,
                           long destinationId, Set<Cell> cells) {
        var cellIds = cells.stream()
                .map(Cell::getId)
                .collect(Collectors.toSet());
        return poolRepository.save(new Pool(id, sortingCenter.getId(), name, destinationType, destinationId, cellIds));
    }

    public Pool storedPool(long id, SortingCenter sortingCenter, Set<Cell> cells) {
        var randomDSId = System.nanoTime();
        return storedPool(id, sortingCenter,
                "pool" + randomDSId,
                RouteDestinationType.DELIVERY_SERVICE,
                randomDSId,
                cells
        );
    }

    public Process storedCommonProcess() {
        var operation1 = storedOperation(OperationSystemName.ZONE_CHECK_IN.name());
        var operation2 = storedOperation(OperationSystemName.ZONE_LEAVE.name());
        var operation3 = storedOperation(OperationSystemName.FLOW_CHECK_IN.name());
        var operation4 = storedOperation(OperationSystemName.FLOW_LEAVE.name());
        var operation5 = storedOperation(OperationSystemName.NEXT_OPERATION.name());
        var flow = storedFlow(
                FlowSystemName.COMMON.name(),
                "Общий флоу",
                List.of(operation1, operation2, operation3, operation4, operation5)
        );

        return storedProcess(COMMON_PROCESS_SYSTEM_NAME, "Общий процесс", List.of(flow));
    }

    public Process storedProcess(String systemName, String displayName) {
        return processRepository.save(new Process().setDisplayName(displayName).setSystemName(systemName));
    }

    public Process storedProcess(String name) {
        return storedProcess(name, name);
    }

    public Process storedProcess(String systemName, String displayName, List<Flow> flows) {
        var process = new Process().setDisplayName(displayName)
                .setSystemName(systemName);
        var processFlows = IntStream.range(0, flows.size())
                .mapToObj(i -> new ProcessFlow()
                        .setProcess(process)
                        .setFlow(flows.get(i))
                        .setOrderNumber(i)
                ).collect(Collectors.toList());
        process.setProcessFlows(processFlows);

        return processRepository.save(process);
    }

    public Process storedCheckInAndLeaveOperation() {
        var ops = List.of(
                storedOperation(OperationSystemName.ZONE_CHECK_IN.name()),
                storedOperation(OperationSystemName.ZONE_LEAVE.name())
        );
        var flow = storedFlow(FlowSystemName.COMMON.name(), FlowSystemName.COMMON.name(), ops);
        return storedProcess(COMMON_PROCESS_SYSTEM_NAME, COMMON_PROCESS_SYSTEM_NAME, List.of(flow));
    }

    public Flow storedFlow(String systemName, String displayName, List<Operation> operations) {
        var flow = new Flow().setDisplayName(displayName).setSystemName(systemName);
        var flowOperations = IntStream.range(0, operations.size())
                .mapToObj(i -> new FlowOperation().setOperation(operations.get(i)).setOrdinal(i).setFlow(flow))
                .collect(Collectors.toList());
        flow.setFlowOperations(flowOperations);

        return flowRepository.save(flow);
    }

    public Flow storedFlow(String systemName, String displayName, Map<Operation, JsonNode> operationToConfigMap) {
        var flow = new Flow().setDisplayName(displayName).setSystemName(systemName);
        var operations = operationToConfigMap.entrySet().stream().toList();
        var flowOperations = IntStream.range(0, operationToConfigMap.size())
                .mapToObj(i -> new FlowOperation()
                        .setOperation(operations.get(i).getKey())
                        .setOrdinal(i)
                        .setFlow(flow)
                        .setConfig(operations.get(i).getValue()))
                .collect(Collectors.toList());
        flow.setFlowOperations(flowOperations);

        return flowRepository.save(flow);
    }

    @SneakyThrows
    public JsonNode toJsonNode(String s) {
        return jacksonObjectMapper.readTree(s);
    }

    public Operation storedOperation(String systemName, String displayName) {
        return operationRepository.save(new Operation().setDisplayName(displayName).setSystemName(systemName));
    }

    public Operation storedOperation(String systemName) {
        return operationRepository.save(new Operation().setDisplayName(systemName).setSystemName(systemName));
    }

    public SortableLot switchLotSize(long lotId, LotSize lotSize) {
        return sortableLotService.switchLotSize(lotId, lotSize);
    }

    public void checkInZone(Zone zone, User user) {
        var logRequest = OperationLogRequest.builder()
                .sortingCenter(zone.getSortingCenter())
                .user(user)
                .zoneId(zone)
                .workstationId(zone)
                .process(COMMON_PROCESS_SYSTEM_NAME)
                .flow(FlowSystemName.COMMON)
                .operation(OperationSystemName.ZONE_CHECK_IN)
                .result(OperationLogResult.OK)
                .fixedAt(Instant.now(clock))
                .build();

        eventPublisher.publishEvent(new OperationLogEvent(logRequest));
    }

    public void leaveZone(Zone zone, User user) {
        var logRequest = OperationLogRequest.builder()
                .sortingCenter(zone.getSortingCenter())
                .user(user)
                .zoneId(zone)
                .workstationId(zone)
                .process(COMMON_PROCESS_SYSTEM_NAME)
                .flow(FlowSystemName.COMMON)
                .operation(OperationSystemName.ZONE_LEAVE)
                .result(OperationLogResult.OK)
                .fixedAt(Instant.now(clock))
                .build();

        eventPublisher.publishEvent(new OperationLogEvent(logRequest));
    }

    public void createReturn(Cargo cargo, SortingCenter sortingCenter, User user) {
        var fromWarehouseDto = WarehouseDto.builder()
                .type(WarehouseType.SORTING_CENTER.name())
                .yandexId("123123")
                .logisticPointId("123123")
                .incorporation("ООО фром мерчант")
                .location(TestFactory.MOCK_WAREHOUSE_LOCATION)
                .build();
        var returnWarehouseDto = WarehouseDto.builder()
                .yandexId(cargo.warehouseReturnYandexId())
                .logisticPointId("log_point-" + cargo.warehouseReturnYandexId())
                .type(WarehouseType.SORTING_CENTER.getName())
                .incorporation(cargo.warehouseReturnYandexId())
                .location(TestFactory.MOCK_WAREHOUSE_LOCATION)
                .build();
        cargoCommandService.createReturn(CreateReturnRequest.builder()
                .sortingCenter(sortingCenter)
                .orderBarcode(cargo.orderBarcode())
                .placeBarcode(cargo.placeBarcode())
                .returnDate(LocalDate.now())
                .returnWarehouse(returnWarehouseDto)
                .fromWarehouse(fromWarehouseDto)
                .segmentUuid(cargo.segmentUuid())
                .cargoUnitId(cargo.cargoUnitId())
                .timeIn(Instant.now(clock))
                .timeOut(Instant.now(clock))
                .orderReturnType(OrderReturnType.CANCELLATION_RETURN)
                .assessedCost(new BigDecimal(10_000))
                .build(), user);
    }
}
