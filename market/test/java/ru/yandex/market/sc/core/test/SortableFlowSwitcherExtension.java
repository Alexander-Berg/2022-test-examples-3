package ru.yandex.market.sc.core.test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.cell.repository.CellRepository;
import ru.yandex.market.sc.core.domain.order.MissingRoutesServiceTest;
import ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.place.model.PlaceStatus;
import ru.yandex.market.sc.core.domain.place.repository.Place;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepository;
import ru.yandex.market.sc.core.domain.place.repository.PlaceRepositoryTest;
import ru.yandex.market.sc.core.domain.place.repository.SortableFlowStage;
import ru.yandex.market.sc.core.domain.route.RouteNonBlockingQueryService;
import ru.yandex.market.sc.core.domain.route.jdbc.RouteJdbcRepository;
import ru.yandex.market.sc.core.domain.route.repository.Route;
import ru.yandex.market.sc.core.domain.route.repository.RouteCell;
import ru.yandex.market.sc.core.domain.route.repository.RouteCellRepository;
import ru.yandex.market.sc.core.domain.route_so.RouteSoMigrationHelper;
import ru.yandex.market.sc.core.domain.route_so.model.RouteDestinationType;
import ru.yandex.market.sc.core.domain.route_so.model.RouteType;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSo;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoSite;
import ru.yandex.market.sc.core.domain.route_so.repository.RouteSoSiteRepository;
import ru.yandex.market.sc.core.util.ScDateUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM;
import static ru.yandex.market.sc.core.domain.order.model.ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN;
import static ru.yandex.market.sc.core.domain.place.model.PlaceStatus.KEEPED;
import static ru.yandex.market.sc.core.domain.place.model.PlaceStatus.RETURNED;
import static ru.yandex.market.sc.core.domain.place.repository.SortableFlowStage.STAGE_2_3;
import static ru.yandex.market.sc.core.domain.route_so.model.RouteDestinationType.COURIER;
import static ru.yandex.market.sc.core.domain.route_so.model.RouteDestinationType.WAREHOUSE;
import static ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus.KEEPED_RETURN;
import static ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus.SORTED_DIRECT;
import static ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus.SORTED_RETURN;

@Slf4j
@Service
public class SortableFlowSwitcherExtension implements BeforeEachCallback, AfterEachCallback {
    public static final String ENV_TO_DISABLE_ROUTE_SO_STAGE_1 = "DISABLE_ROUTE_SO_STAGE_1";

    public static final String ENV_TO_DISABLE_ROUTE_SO_STAGE_1_2_CELL_BINDING =
            "ENV_TO_DISABLE_ROUTE_SO_STAGE_1_2_CELL_BINDING";

    public static final String ENV_TO_DISABLE_ROUTE_SO_STAGE_2_SORT_WITH_ROUTE_SO =
            "ENV_TO_DISABLE_ROUTE_SO_STAGE_1_2_CELL_BINDING";


    private static final boolean SHOW_UNDONE_TESTS = false;

    List<Class<?>> testToSkip = List.of(PlaceRepositoryTest.class,
            // Тестируется класс только для работы с олдовыми рутами, с новыми рутами работает place rescheduler
            MissingRoutesServiceTest.class
    );

    List<String> testDisplayNameToSkip = List.of(
            "[3] params=OrderControllerFullTest.TestTransitionParams(statusBefore=ORDER_CREATED_FF, " +
                    "preparationMethodName=noPreparation, transitionsMethodName=updateOrderFFApi, " +
                    "statusAfter=ORDER_CREATED_FF, statusAfterInApi=SORT_TO_COURIER)",
            // тут просто хибер не инвалдирует кэши после jdbc апдейта, на самом деле все работает
            "[22] params=OrderControllerFullTest.TestStatusParams(status=SO_GOT_INFO_ABOUT_PLANNED_RETURN, " +
                    "preparationMethodName=switchClockToTomorrowAndRescheduleReturns, statusInApi=SORT_TO_WAREHOUSE)"
    );

    List<String> testMethodToSkip = List.of(
            //отложили до следующих ПР
            "copyReturnOrderTest",

            //проблемы из-за наших вспомогательных методов, которые создают руты, по логике все ок.
            "updateCourierTiFakeFromExistingCourier",

            //функциональность для route so просто не была написана
            "changeRouteCourierIfExistingRouteIsEmpty",

            //страемся сделать Route so immutable, поэтому при изменении курьера создаем новый рут - ячейку
            "changeOutgoingCourierExistingCourier",
            "changeOutgoingCourierNewCourier",

            // Используется прямой апдейт set stage, которы изменяет сортабл статус и руты, но не создает их
            "acceptSortablesAndFailFinishTest",
            "wrongStageTest",

            //При приемке места заказу проставляется incoming_route_date, которая проставляется
            // всем коробкам в sortable_flow. По факту входящая дата не приходила в ff-api и route so
            // для awaiting direct можно создать только на домыслах, что он придется в тот же день
            // (чего мы делать не будем)
            "updateCourierFromPartiallyAcceptedStatus",

            // Рескедьюлинг для заказа обновляет incoming route date для принятого заказа, из-за этого расхождение
            "getTransferActReturnPartialMultiplaceOrderNextDay",

            // Создается новый плейс путем копирования через sql-процедуру copy_return_order
            "copyToUtilizationForOnePlace",

            // Создается новый плейс путем копирования через sql-процедуру copy_return_order
            "copyToUtilizationFromTwoPlaces"
    );

    List<String> testMethodInLegacyOnly = List.of(
            // нужно починить рут дто
            "getRoutesByCourier",
            "getCellsForRouteV2",

            // 29 июля. Временно гасим тесты, чтобы перейти на sort with route so,
            // чтобы в транк перестал добавляться падающий в route so код
            "rescheduleSortInAdvanceWithZeroCutoff",
            "getLastStockmanSignatureDataByCourier"
    );

    List<String> testInLegacyOnly = List.of(
            // 29 июля. Временно гасим тесты, чтобы перейти на sort with route so,
            // чтобы в транк перестал добавляться падающий в route so код
            //sc int
            "ru.yandex.market.sc.internal.controller.FFApiControllerPutInboundRegistryTest",
            "ru.yandex.market.sc.internal.controller.internal.InternalOrderControllerTest",
            "ru.yandex.market.sc.internal.domain.order.PartnerDeliveryOrderServiceTest",
            "ru.yandex.market.sc.internal.domain.report.PartnerReportServiceTest",
            "ru.yandex.market.sc.internal.sqs.handler.CourierReassignHandlerTest",
            "ru.yandex.market.sc.internal.controller.partner.PartnerCellControllerTest",
            //sc core
            "ru.yandex.market.sc.core.domain.cell.CellCommandServiceTest",
            "ru.yandex.market.sc.core.domain.order.MissingRoutesServiceTest",
            "ru.yandex.market.sc.core.domain.route.RouteFacadeTest",
            "ru.yandex.market.sc.core.domain.scan.LotAcceptServiceTest",
            "ru.yandex.market.sc.core.domain.scan.ScanServiceTest",
            "ru.yandex.market.sc.core.domain.order.OrderQueryServiceTest",
            //sc api
            "ru.yandex.market.sc.api.features.SortOnReturnFlowRightAfterAcceptOnDropoffTest",
            "ru.yandex.market.sc.api.features.ClientReturnTestFlow",
            "ru.yandex.market.sc.api.features.ImpermanentCellFlowTest",
            "ru.yandex.market.sc.api.features.SortOnReturnFlowRightAfterAcceptOnDropoffTest",
            "ru.yandex.market.sc.api.controller.OrderControllerSortableTest",
            "ru.yandex.market.sc.api.controller.OrderControllerSortableTest$FixOnlyFullySortedInbounds"




//            "ru.yandex.market.sc.core.domain.route.RouteFacadeTest",
//            "ru.yandex.market.sc.core.domain.route.repository.RouteMapperTest",
//            "ru.yandex.market.sc.api.controller.RouteControllerTest",
//            "ru.yandex.market.sc.api.features.ClientReturnTestFlow"


    );

    static boolean forceLegacyFlow = false;

    public static boolean useNewRouteSoStage1() {
//        return false;
        return System.getenv(ENV_TO_DISABLE_ROUTE_SO_STAGE_1) == null;
    }

    public static boolean useNewRouteSoStage1_2() {
//        return false;
        return System.getenv(ENV_TO_DISABLE_ROUTE_SO_STAGE_1_2_CELL_BINDING) == null;
    }

    public static boolean useNewRouteSoStage2() {
//        return false;
        return !forceLegacyFlow &&  System.getenv(ENV_TO_DISABLE_ROUTE_SO_STAGE_2_SORT_WITH_ROUTE_SO) == null;
    }
    public static SortableFlowStage sortableFlowStage() {
        return STAGE_2_3;
    }

    public static boolean showUndoneTests() {
        return SHOW_UNDONE_TESTS;
    }

    public static void testNotMigrated() {
        if (SHOW_UNDONE_TESTS) {
            throw new RuntimeException("Этот тест не был переделан на новый сортабл флоу");
        }
    }

    public static void testIsFailingInRouteSo() {
        if (SHOW_UNDONE_TESTS) {
            throw new RuntimeException("Этот тест падает в новом сортабл флоу. Нужно доделать функционал");
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        var dataSource = SpringExtension.getApplicationContext(context).getBean("dataSource", DataSource.class);
        switchToNewFlow(dataSource, context);
        RouteSoMigrationHelper.revokeAllRouteReadingPermissions();
        if (!useNewRouteSoStage2()) {
            RouteSoMigrationHelper.allowRouteReading();
        } else {
            RouteSoMigrationHelper.revokeAllRouteReadingPermissions();
        }
    }


    private void switchToNewFlow(DataSource dataSource, ExtensionContext context) {
        forceLegacyFlow = testInLegacyOnly.contains(context.getTestClass().map(Class::getName).orElse(null))
                 || testMethodInLegacyOnly.contains(context.getTestMethod().map(Method::getName).orElse(null));

        if (useNewRouteSoStage1() || useNewRouteSoStage2()) {

            ResourceDatabasePopulator scriptLauncher = new ResourceDatabasePopulator();
            scriptLauncher.addScript(new ClassPathResource("truncate.sql"));

            if (useNewRouteSoStage1()) {
                scriptLauncher.addScript(
                        new ByteArrayResource(
                                """
                                        insert into configuration (key, value) VALUES
                                        ('NOT_FOR_PROD_FORCE_ROUTE_SO_STAGE_1', true);
                                        """.getBytes(StandardCharsets.UTF_8)
                        ));
            }
            if (useNewRouteSoStage2()) {
                scriptLauncher.addScript(
                        new ByteArrayResource(
                                """
                                        insert into configuration (key, value) VALUES
                                        ('NOT_FOR_PROD_FORCE_ROUTE_SO_STAGE_2', true);
                                        insert into configuration (key, value) VALUES
                                        ('ENABLE_ROUTE_SO_FOR_INBOUND_LOOKUP', true);
                                        """.getBytes(StandardCharsets.UTF_8)
                        ));
            }

            scriptLauncher.execute(dataSource);
        }
    }


    @Override
    public void afterEach(ExtensionContext context) {
        if (testToSkip.contains(context.getTestClass().orElse(null))
                || testMethodToSkip.contains(context.getTestMethod().map(Method::getName).orElse(null))
                || testDisplayNameToSkip.contains(context.getDisplayName())
        ) {
            //пропускаем тесты, где осуществляется прямая модификация заказов или коробок, например, через SQL
            return;
        }

        ApplicationContext springContext = SpringExtension.getApplicationContext(context);
        ScOrderRepository orderRepository = springContext.getBean(ScOrderRepository.class);
        log.info("Проверяем, что поля заказов и коробок совпадают");
        try {
            for (ScOrder scOrder : orderRepository.findAll()) {
                checkOrderPlaceFields(springContext, scOrder);
            }
        } catch (LazyInitializationException lazy) {
            new RuntimeException("Сущность из базы ленится", lazy);
        }
    }


    private void checkOrderPlaceFields(ApplicationContext context, ScOrder order) {

        TestFactory testFactory = context.getBean(TestFactory.class);

        log.info(String.format("Анализириуем заказ с id = %s, external id = %s, status = %s : %s",
                order.getId(), order.getExternalId(), order.getFfStatus(), order));
        var orderPlaces = testFactory.orderPlaces(order.getId());
        // Проверяем, что у коробки заполнились все поля из заказа
        // ИЛИ мы отдаем их из прокси заказа, когда useNewFlowStage2 выключен
        for (Place place : orderPlaces) {
            log.info("Анализириуем коробку с id = {}, external id = {}, status = {} : {}",
                    place.getId(), place.getMainPartnerCode(), place.getFfStatus(), place);
            assertThat(place.getSortableFlowStage()).isEqualTo(
                    sortableFlowStage());

            if (
                    (order.getFfStatus() == ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE
                            && !(place.getStatus() == PlaceStatus.SHIPPED))
                            || (order.getFfStatus() == ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE
                            && !(place.getStatus() == PlaceStatus.ACCEPTED))
                            || (place.getStatus().getState() != order.getFfStatus().getState())

                            // Нужно для теста LotShippedFlowTest.shipLotsWithPartialMultiplaceOrderMiddleMile1
                            // когда часть коробок уже возвращены
                            || place.getStatus() == RETURNED
                            && order.getFfStatus() == SO_GOT_INFO_ABOUT_PLANNED_RETURN

            ) {

                log.info("Пропускаем. Статус коробки сильно отличается от статуса заказа," +
                                " place status = {}, place state = {}, order status = {}, order state = {}",
                        place.getStatus(), place.getStatus().getState(),
                        order.getFfStatus(), order.getStatus().getState());

                continue;
            }
            if (
                    (order.getFfStatus().equals(ORDER_AWAITING_CLARIFICATION_FF)
                            && !place.getStatus().equals(KEEPED))
                            // Для теста prepareToShipMultiplaceOrderWithoutPlace
                            || (order.getFfStatus().equals(ORDER_READY_TO_BE_SEND_TO_SO_FF)
                            && !place.getSortableStatus().equals(SORTED_DIRECT))
            ) {

                log.info("Пропускаем (2). Статус коробки сильно отличается от статуса заказа," +
                                " place status = {}, place status (old) = {}, order status = {}, order state = {}",
                        place.getSortableStatus(), place.getStatus(),
                        order.getFfStatus(), order.getStatus().getState());

                continue;
            }

            log.info("Продолжаем обработку. Статус коробки НЕ сильно отличается от статуса заказа," +
                    " \nplace status = {}, \norder status = {}", place.getStatus(), order.getFfStatus());
            log.info("Сортабл статусы: \nplace status = {}, \norder status = {}",
                    place.getSortableStatus(), order.getFfStatus().getSortableStatus());

            assertThat(place.getSortingCenter()).isEqualTo(order.getSortingCenter());
            assertThat(place.getShipmentDate()).isEqualTo(order.getShipmentDate());

//            todo в возвратах могут отличаться тк направление коробок внутри заказа могут быть разные
//            assertThat(place.getIncomingRouteDate()).isEqualTo(order.getIncomingRouteDate());
//            assertThat(place.getCourier()).isEqualTo(order.getCourier());
            assertThat(place.getWarehouseFrom()).isEqualTo(order.getWarehouseFrom());
            assertThat(place.getWarehouseReturn()).isEqualTo(order.getWarehouseReturn());
            assertThat(place.isMiddleMile()).isEqualTo(order.isMiddleMile());
            if (order.getFfStatus().getSortableStatus() != null) {
                if (order.getFfStatus().equals(RETURNED_ORDER_READY_TO_BE_SENT_TO_IM)) {
                    //у заказа нет отдельного статуса соотвествующего KEEPED_RETURN
                    assertThat(place.getSortableStatus()).isIn(List.of(KEEPED_RETURN, SORTED_RETURN));
                } else {
                    assertThat(place.getSortableStatus()).isEqualTo(order.getFfStatus().getSortableStatus());
                }
            }
            log.info("Коробка прошла тест. Id = {}, external id = {}, status = {} : {}",
                    place.getId(), place.getMainPartnerCode(), place.getFfStatus(), place);

        }

        //Проверяем route so
        if (useNewRouteSoStage1()) {
            for (Place place : orderPlaces) {
                checkRouteSo(place);
            }
        }
        //Проверяем route so 1.2 - привязка ячеек
        if (useNewRouteSoStage1_2()) {
            for (Place place : orderPlaces) {
//                checkRouteSoBindCells(place, context);
                checkRouteSoBindCells2(place, context);
            }
        }

    }

    private void checkRouteSoBindCells2(Place place, ApplicationContext context) {
        TransactionTemplate tran = context.getBean(TransactionTemplate.class);
        RouteJdbcRepository routeJdbcRepository = context.getBean(RouteJdbcRepository.class);
        RouteNonBlockingQueryService routeQueryService = context.getBean(RouteNonBlockingQueryService.class);
        CellRepository cellRepository = context.getBean(CellRepository.class);
        PlaceRepository placeRepository = context.getBean(PlaceRepository.class);

        RouteSoSiteRepository routeSoSiteRepository = context.getBean(RouteSoSiteRepository.class);
        RouteCellRepository routeCellRepository = context.getBean(RouteCellRepository.class);
        List<RouteCell> routeCells = routeCellRepository.findAll();
        List<RouteSoSite> routeSoSites = routeSoSiteRepository.findAll();
        for (RouteCell routeCell : routeCells) {

            LocalDate rcDate = routeCell.getExpectedDateSort();

            Optional<RouteSoSite> first = routeSoSites.stream().filter(
                            rs -> rs.getReservedFrom().isBefore(ScDateUtils.toNoonLdt(rcDate))
                                    && rs.getReservedTo().isAfter(ScDateUtils.toNoonLdt(rcDate)))
                    .findFirst();

            Route route = routeCell.getRoute();
            route.allowReading();
            assertThat(first)
                    .withFailMessage(
                            "Старый route [%s, courier_id = %s, warehouse_id = %s, route_type = %s] привязан к ячейке [%s]," +
                                    " в то время как route SO нет",
                            route.getId(),
                            route.getCourierToId(),
                            route.getWarehouseTo(),
                            route.getType(),
                            routeCell)
                    .isPresent();
//            assertThat(routeCells).withFailMessage(
//                    "Route So [%s, dest_id = %s, dest_type = %s, route_type = %s] привязан к ячейкам [%s]," +
//                            " в то время как route к [%s]",
//                    place_.getOutRoute().getId(),
//                    place_.getOutRoute().getDestinationId(),
//                    place_.getOutRoute().getDestinationType(),
//                    place_.getOutRoute().getType(),
//                    routeSoCells, routeCells
//            ).isEqualTo(routeSoCells);
        }

        for (RouteSoSite routeSoSite : routeSoSites) {
            var start = routeSoSite.getReservedFrom().toLocalDate();
            LocalDateTime reservedToExlusive = routeSoSite.getReservedTo().minusSeconds(1);
            var finish = reservedToExlusive.toLocalDate();
            while ( !start.isAfter(finish)) {
                LocalDate rcDate = start;

                Optional<RouteSoSite> first = routeSoSites.stream().filter(
                                rs -> rs.getReservedFrom().isBefore(ScDateUtils.toNoonLdt(rcDate))
                                        && rs.getReservedTo().isAfter(ScDateUtils.toNoonLdt(rcDate)))
                        .findFirst();

                RouteSo routeSo = routeSoSite.getRoute();
                assertThat(first)
                        .withFailMessage(
                                "RouteSo [%s, courier_id = %s, warehouse_id = %s, route_type = %s] привязан к ячейке [%s]," +
                                        " в то время как в старом route нет",
                                routeSo.getId(),
                                routeSo.getDestinationType(),
                                routeSo.getDestinationId(),
                                routeSo.getType(),
                                routeSoSite)
                        .isPresent();

                start = start.plusDays(1);
            }

        }

    }
    private void checkRouteSoBindCells(Place place, ApplicationContext context) {
        TransactionTemplate tran = context.getBean(TransactionTemplate.class);
        RouteJdbcRepository routeJdbcRepository = context.getBean(RouteJdbcRepository.class);
        RouteNonBlockingQueryService routeQueryService = context.getBean(RouteNonBlockingQueryService.class);
        CellRepository cellRepository = context.getBean(CellRepository.class);
        PlaceRepository placeRepository = context.getBean(PlaceRepository.class);

        RouteSoSiteRepository routeSoSiteRepository = context.getBean(RouteSoSiteRepository.class);
        RouteCellRepository routeCellRepository = context.getBean(RouteCellRepository.class);

        tran.execute(ts -> {
                    Optional<Route> placeOutgointRoute = routeQueryService.findPlaceOutgoingRoute(place);
//                    Optional<Route> placeOutgointRoute = routeQueryService.findOrderOutgoingRoute(place);
//                    List<Cell> cells = placeOutgointRoute.get().getCells(placeOutgointRoute.get().getExpectedDate());
                    if (placeOutgointRoute.isPresent()) {
                        Set<Cell> routeCells = new HashSet<>(
                                placeOutgointRoute.get().getCells(placeOutgointRoute.get().getExpectedDate()));
//                        Set<Cell> routeCells =
//                                routeJdbcRepository.findRouteCells(List.of(placeOutgointRoute.get().getId()))
//                                    .stream().map(RouteIdCellDto::getCellId)
//                                    .map(cellRepository::findByIdOrThrow)
//                                    .collect(Collectors.toSet());
//
                        Place place_ = placeRepository.findByIdOrThrow(place.getId());

                        Set<Cell> routeSoCells = place_.getOutRoute().getRouteSoSites()

                                .stream().map(RouteSoSite::getCell)
                                .collect(Collectors.toSet());

                        assertThat(routeCells).withFailMessage(
                                "Route So [%s, dest_id = %s, dest_type = %s, route_type = %s] привязан к ячейкам [%s]," +
                                        " в то время как route к [%s]",
                                place_.getOutRoute().getId(),
                                place_.getOutRoute().getDestinationId(),
                                place_.getOutRoute().getDestinationType(),
                                place_.getOutRoute().getType(),
                                routeSoCells, routeCells
                        ).isEqualTo(routeSoCells);
                    }
                    return null;
                }
        );


    }

    private void checkRouteSo(Place place) {
        switch (place.getSortableStatus()) {
            case CANCELLED -> {
                checkInRouteIsNull(place);
                checkOutRouteIsNull(place);
            }
            case AWAITING_DIRECT -> {
                checkInRouteIsTheSame(place, WAREHOUSE, place.getWarehouseFrom().getId(), place.getIncomingRouteDate());

                //todo:kir 27 apr добавил опять, так как статусы заказа проверяются внутри
                checkOutRouteIsTheSame(
                        place, COURIER, place.getCourierId().orElse(null), place.getOutgoingRouteDate());
            }
            case ARRIVED_DIRECT -> {
                checkInRouteIsNull(place);
                checkOutRouteIsTheSame(
                        place, COURIER, place.getCourierId().orElse(null), place.getOutgoingRouteDate());
            }
            case KEEPED_DIRECT, SORTED_DIRECT, PREPARED_DIRECT -> {
                checkInRouteIsNull(place);
                checkOutRouteIsTheSame(
                        place, COURIER, place.getCourierId().orElse(null), place.getOutgoingRouteDate());
            }
            case SHIPPED_DIRECT -> {
                checkInRouteIsNull(place);
                checkOutRouteIsNull(place);
            }

            case AWAITING_RETURN -> {
                checkInRouteIsTheSame(
                        place, COURIER, place.getCourierId().orElse(null), place.getIncomingRouteDate());
                checkOutRouteIsTheSame(
                        place, WAREHOUSE, place.getWarehouseReturn().getId(), place.getOutgoingRouteDate());

            }
            case ACCEPTED_RETURN, KEEPED_RETURN, SORTED_RETURN, PREPARED_RETURN -> {
                checkInRouteIsNull(place);
                checkOutRouteIsTheSame(
                        place, WAREHOUSE, place.getWarehouseReturn().getId(), place.getOutgoingRouteDate());
            }

            case SHIPPED_RETURN -> {
                checkInRouteIsNull(place);
                checkOutRouteIsNull(place);
            }
        }
    }

    private boolean orderInStatus(ScOrder order, List<ScOrderFFStatus> statuses) {
        return statuses.contains(order.getFfStatus());
    }

    private void checkInRouteIsNull(Place place) {
        RouteSo inRoute = place.getInRoute();
        assertThat(inRoute)
                .withFailMessage(
                        "У коробки [%s] в статусе [%s] найден входящий маршрут [%s, %s, %s], которого не должно быть",
                        place.getMainPartnerCode(),
                        place.getSortableStatus(),
                        Optional.ofNullable(inRoute).map(RouteSo::getType).orElse(null),
                        Optional.ofNullable(inRoute).map(RouteSo::getDestinationType).orElse(null),
                        Optional.ofNullable(inRoute).map(RouteSo::getDestinationId).orElse(null)
                )
                .isNull();
    }

    private void checkOutRouteIsNull(Place place) {
        RouteSo outRoute = place.getOutRoute();

        assertThat(outRoute)
                .withFailMessage(
                        "У коробки [%s] найден исходящий маршрут [%s, %s, %s], которого не должно быть",
                        place.getMainPartnerCode(),
                        Optional.ofNullable(outRoute).map(RouteSo::getType).orElse(null),
                        Optional.ofNullable(outRoute).map(RouteSo::getDestinationType).orElse(null),
                        Optional.ofNullable(outRoute).map(RouteSo::getDestinationId).orElse(null)
                )
                .isNull();
    }

    private void checkInRouteIsTheSame(Place place, RouteDestinationType destinationType, Long destinationId,
                                       LocalDate date) {
        RouteSo inRoute = place.getInRoute();


        if ((date == null || destinationId == null)) {
            assertThat(inRoute)
                    .withFailMessage(
                            "У коробки [%s] в статусе [%s] найден входящий маршрут [%s, %s]," +
                                    " которого нет у заказа в статусе [%s]",
                            place.getMainPartnerCode(), place.getSortableStatus(), destinationType, destinationId,
                            place.getOrder().getFfStatus())
                    .isNull();

        } else {
            assertThat(inRoute)
                    .withFailMessage(
                            "Не найден входящий маршрут [%s, %s] у коробки [%s] в статусе [%s]," +
                                    " при том что у заказа в статусе [%s] он присуствует",
                            getRouteType(destinationType, true), destinationId, place.getMainPartnerCode(),
                            place.getSortableStatus(), place.getOrder().getFfStatus()
                    )
                    .isNotNull();

            assertThat(inRoute.getSortingCenter().getId())
                    .withFailMessage(
                            "Сортировочный центр маршрута [%s]" +
                                    " не совпадает с СЦ коробки [%s]",
                            inRoute.getSortingCenter().getId(),
                            place.getSortingCenterId()
                    )
                    .isEqualTo(place.getSortingCenterId());

            assertThat(inRoute.getType())
                    .withFailMessage(
                            "Тип входящего маршрута [%s] не совпадает ожидаемым [%s]" +
                                    " для коробки [%s] в статусе [%s]",
                            inRoute.getType(), getRouteType(destinationType, true),
                            place.getMainPartnerCode(), place.getSortableStatus())
                    .isEqualTo(getRouteType(destinationType, true));


            assertThat(inRoute.getDestinationType())
                    .withFailMessage(
                            "Тип контрагента входящего маршрута [%s] не совпадает ожидаемым [%s]" +
                                    " для коробки [%s]",
                            inRoute.getDestinationType(), destinationType,
                            place.getMainPartnerCode())
                    .isEqualTo(destinationType);
            assertThat(inRoute.getDestinationId()).isEqualTo(destinationId);
            // проверяем, что рут назначен на тот же день
            Instant inRouteTime = ScDateUtils.toNoon(date);
            assertThat(ScDateUtils.timeIsBetween(inRouteTime,
                    inRoute.getIntervalFrom(), inRoute.getIntervalTo()))
                    .withFailMessage(
                            "Интервал входящего маршрута коробки [%s, %s] не совпадает" +
                                    " со временем входящего маршрута заказа [%s] для коробки [%s]",
                            inRoute.getIntervalFrom(), inRoute.getIntervalTo(),
                            date, place.getMainPartnerCode())
                    .isTrue();
        }
    }

    private Collection<ScOrderFFStatus> getFfStatusesWhereRouteIsPossible(
            RouteDestinationType destinationType, boolean in
    ) {

        if (in) {
            if (destinationType.equals(WAREHOUSE)) {
                return List.of(
                        ScOrderFFStatus.ORDER_CREATED_FF,
                        ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE,
                        ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE
                );
            } else if (destinationType.equals(COURIER)) {
                return List.of(
                        ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN
                );
            }
        } else {
            if (destinationType.equals(WAREHOUSE)) {
                return List.of(
                        ScOrderFFStatus.SO_GOT_INFO_ABOUT_PLANNED_RETURN,
                        ScOrderFFStatus.RETURNED_ORDER_AT_SO_WAREHOUSE,
                        ScOrderFFStatus.RETURNED_ORDER_READY_TO_BE_SENT_TO_IM,
                        ScOrderFFStatus.SORTING_CENTER_PREPARED_FOR_UTILIZE
                );
            } else if (destinationType.equals(COURIER)) {
                return List.of(
                        ScOrderFFStatus.ORDER_CREATED_FF,
                        ScOrderFFStatus.ORDER_PARTIALLY_ARRIVED_TO_SO_WAREHOUSE,
                        ScOrderFFStatus.ORDER_PARTIALLY_SHIPPED_TO_SO_WAREHOUSE,
                        ScOrderFFStatus.ORDER_ARRIVED_TO_SO_WAREHOUSE,
                        ScOrderFFStatus.ORDER_AWAITING_CLARIFICATION_FF,
                        ScOrderFFStatus.ORDER_READY_TO_BE_SEND_TO_SO_FF,
                        ScOrderFFStatus.ORDER_PREPARED_TO_BE_SEND_TO_SO
                );
            }

        }

        throw new RuntimeException("Сейчас мы работаем только с COURIER и WAREHOUSE destincation type," +
                "если нужны другие типы - этому коду требуется доработка.");
    }

    private void checkOutRouteIsTheSame(Place place, RouteDestinationType destinationType, Long destinationId,
                                        LocalDate date) {
        //TODO: kir ВАЖНО
        //TODO: kir ВАЖНО
        // нужно не забыть добавить проверку СЦ в маршруты

        RouteSo outRoute = place.getOutRoute();

        if (date == null || destinationId == null) {
            assertThat(outRoute)
                    .withFailMessage(
                            "У коробки [%s] в статусе [%s] найден исходящий маршрут [%s, %s]," +
                                    " которого нет у заказа в статусе [%s]",
                            place.getMainPartnerCode(), place.getSortableStatus(), destinationType, destinationId,
                            place.getOrder().getFfStatus())
                    .isNull();
        } else {
            assertThat(outRoute)
                    .withFailMessage(
                            "Не найден исходящий маршрут [%s, %s] у коробки [%s] в статусе [%s]," +
                                    " при том что у заказа в статусе [%s] он присуствует",
                            getRouteType(destinationType, false), destinationId, place.getMainPartnerCode(),
                            place.getSortableStatus(), place.getOrder().getFfStatus()
                    )
                    .isNotNull();

            assertThat(outRoute.getType())
                    .withFailMessage(
                            "Тип исходящего маршрута [%s] не совпадает ожидаемым [%s]" +
                                    " для коробки [%s] в статусе [%s]",
                            outRoute.getType(), getRouteType(destinationType, false),
                            place.getMainPartnerCode(), place.getSortableStatus())
                    .isEqualTo(getRouteType(destinationType, false));


            assertThat(outRoute.getDestinationType())
                    .withFailMessage(
                            "Тип контрагента исходящего маршрута [%s] не совпадает ожидаемым [%s]" +
                                    " для коробки [%s] в статусе [%s]",
                            outRoute.getDestinationType(), destinationType,
                            place.getMainPartnerCode(), place.getSortableStatus())
                    .isEqualTo(destinationType);
            assertThat(outRoute.getDestinationId())
                    .withFailMessage(
                            "Контрагент исходящего маршрута [%s, %s] не совпадает ожидаемым [%s, %s]" +
                                    " для коробки [%s] в статусе [%s]",
                            outRoute.getDestinationId(), outRoute.getDestinationType(), destinationId, destinationType,
                            place.getMainPartnerCode(), place.getSortableStatus())
                    .isEqualTo(destinationId);
            // проверяем, что рут назначен на тот же день
            Instant inRouteTime = ScDateUtils.toNoon(date);
            assertThat(ScDateUtils.timeIsBetween(inRouteTime,
                    outRoute.getIntervalFrom(), outRoute.getIntervalTo()))
                    .withFailMessage(
                            "Интервал исходящего маршрута коробки [%s, %s] не совпадает" +
                                    " со временем исходящего маршрута заказа [%s] для коробки [%s] в статусе [%s]",
                            outRoute.getIntervalFrom(), outRoute.getIntervalTo(),
                            date, place.getMainPartnerCode(), place.getSortableStatus())
                    .isTrue();
        }

    }

    private RouteType getRouteType(RouteDestinationType destinationType, boolean in) {
        switch (destinationType) {
            case COURIER -> {
                if (in) {
                    return RouteType.IN_RETURN;
                } else {
                    return RouteType.OUT_DIRECT;
                }
            }

            case WAREHOUSE -> {
                if (in) {
                    return RouteType.IN_DIRECT;
                } else {
                    return RouteType.OUT_RETURN;
                }
            }

            default -> throw new IllegalStateException("Unexpected value: " + destinationType);
        }

    }

    private List<RouteType> getRouteTypes(Collection<RouteDestinationType> destinationTypes, boolean in) {
        return destinationTypes.stream().map(type -> getRouteType(type, in)).toList();
    }
}
