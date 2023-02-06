package ru.yandex.market.ff.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.collection.Tuple3;
import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.config.ServiceConfiguration;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;
import static ru.yandex.market.ff.client.enums.RequestStatus.ACCEPTED_BY_SERVICE;
import static ru.yandex.market.ff.client.enums.RequestStatus.ACCEPTED_BY_XDOC_SERVICE;
import static ru.yandex.market.ff.client.enums.RequestStatus.ARRIVED_TO_SERVICE;
import static ru.yandex.market.ff.client.enums.RequestStatus.ARRIVED_TO_XDOC_SERVICE;
import static ru.yandex.market.ff.client.enums.RequestStatus.AWAITING_FOR_UNLOADING;
import static ru.yandex.market.ff.client.enums.RequestStatus.CANCELLATION_REJECTED;
import static ru.yandex.market.ff.client.enums.RequestStatus.CANCELLATION_REQUESTED;
import static ru.yandex.market.ff.client.enums.RequestStatus.CANCELLATION_SENT_TO_SERVICE;
import static ru.yandex.market.ff.client.enums.RequestStatus.CANCELLED;
import static ru.yandex.market.ff.client.enums.RequestStatus.CREATED;
import static ru.yandex.market.ff.client.enums.RequestStatus.FINISHED;
import static ru.yandex.market.ff.client.enums.RequestStatus.INITIAL_ACCEPTANCE_COMPLETED;
import static ru.yandex.market.ff.client.enums.RequestStatus.INITIAL_ACCEPTANCE_COMPLETED_BY_XDOC_SERVICE;
import static ru.yandex.market.ff.client.enums.RequestStatus.INITIAL_ACCEPTANCE_DETAILS_LOADED;
import static ru.yandex.market.ff.client.enums.RequestStatus.INVALID;
import static ru.yandex.market.ff.client.enums.RequestStatus.INVALID_CONFIRMATION;
import static ru.yandex.market.ff.client.enums.RequestStatus.IN_PROGRESS;
import static ru.yandex.market.ff.client.enums.RequestStatus.PLAN_REGISTRY_ACCEPTED;
import static ru.yandex.market.ff.client.enums.RequestStatus.PLAN_REGISTRY_CREATED;
import static ru.yandex.market.ff.client.enums.RequestStatus.PLAN_REGISTRY_SENT;
import static ru.yandex.market.ff.client.enums.RequestStatus.PREPARED_FOR_CREATION;
import static ru.yandex.market.ff.client.enums.RequestStatus.PROCESSED;
import static ru.yandex.market.ff.client.enums.RequestStatus.READY_TO_WITHDRAW;
import static ru.yandex.market.ff.client.enums.RequestStatus.RECEIPT_REJECTED;
import static ru.yandex.market.ff.client.enums.RequestStatus.REJECTED_BY_SERVICE;
import static ru.yandex.market.ff.client.enums.RequestStatus.REJECTED_BY_XDOC_SERVICE;
import static ru.yandex.market.ff.client.enums.RequestStatus.SENT_TO_SERVICE;
import static ru.yandex.market.ff.client.enums.RequestStatus.SENT_TO_XDOC_SERVICE;
import static ru.yandex.market.ff.client.enums.RequestStatus.SHIPPED_TO_SERVICE;
import static ru.yandex.market.ff.client.enums.RequestStatus.UNLOADING;
import static ru.yandex.market.ff.client.enums.RequestStatus.VALIDATED;
import static ru.yandex.market.ff.client.enums.RequestStatus.WAITING_FOR_CONFIRMATION;
import static ru.yandex.market.ff.client.enums.RequestType.ADDITIONAL_SUPPLY;
import static ru.yandex.market.ff.client.enums.RequestType.CROSSDOCK;
import static ru.yandex.market.ff.client.enums.RequestType.CUSTOMER_RETURN;
import static ru.yandex.market.ff.client.enums.RequestType.CUSTOMER_RETURN_SUPPLY;
import static ru.yandex.market.ff.client.enums.RequestType.DROPOFF_RETURN_WITHDRAW;
import static ru.yandex.market.ff.client.enums.RequestType.EXPENDABLE_MATERIALS;
import static ru.yandex.market.ff.client.enums.RequestType.FIX_LOST_INVENTORYING_WITHDRAW;
import static ru.yandex.market.ff.client.enums.RequestType.INVALID_UNREDEEMED;
import static ru.yandex.market.ff.client.enums.RequestType.INVENTORYING_SUPPLY;
import static ru.yandex.market.ff.client.enums.RequestType.MOVEMENT_SUPPLY;
import static ru.yandex.market.ff.client.enums.RequestType.MOVEMENT_WITHDRAW;
import static ru.yandex.market.ff.client.enums.RequestType.OPER_LOST_INVENTORYING_WITHDRAW;
import static ru.yandex.market.ff.client.enums.RequestType.ORDERS_RETURN_WITHDRAW;
import static ru.yandex.market.ff.client.enums.RequestType.ORDERS_SUPPLY;
import static ru.yandex.market.ff.client.enums.RequestType.ORDERS_WITHDRAW;
import static ru.yandex.market.ff.client.enums.RequestType.SHADOW_SUPPLY;
import static ru.yandex.market.ff.client.enums.RequestType.SHADOW_WITHDRAW;
import static ru.yandex.market.ff.client.enums.RequestType.SUPPLY;
import static ru.yandex.market.ff.client.enums.RequestType.TRANSFER;
import static ru.yandex.market.ff.client.enums.RequestType.UPDATING_REQUEST;
import static ru.yandex.market.ff.client.enums.RequestType.UTILIZATION_WITHDRAW;
import static ru.yandex.market.ff.client.enums.RequestType.VALID_UNREDEEMED;
import static ru.yandex.market.ff.client.enums.RequestType.WITHDRAW;
import static ru.yandex.market.ff.client.enums.RequestType.XDOC_TRANSPORT_SUPPLY;
import static ru.yandex.market.ff.client.enums.RequestType.XDOC_TRANSPORT_WITHDRAW;
import static ru.yandex.market.ff.client.enums.RequestType.X_DOC_PARTNER_SUPPLY_TO_DISTRIBUTION_CENTER;
import static ru.yandex.market.ff.client.enums.RequestType.X_DOC_PARTNER_SUPPLY_TO_FF;
import static ru.yandex.market.ff.config.utils.StatusPriorityUtils.STATUSES_NOT_FROM_WAREHOUSE;

public class StatusMovementUnitTest extends SoftAssertionSupport {
    private static final Map<RequestType, Map<RequestStatus, List<RequestStatus>>> ALLOWED_MOVES =
            new ServiceConfiguration().allowedRequestStatusMoves();

    @SafeVarargs
    private static <B> Stream<B> combine(Stream<B>... streams) {
        return Stream.of(streams).flatMap(Function.identity());
    }

    private static Stream<Tuple3<RequestType, RequestStatus, RequestStatus>> forType(
            RequestType type,
            Stream<Tuple2<RequestStatus, RequestStatus>> allowedMoves
    ) {
        return allowedMoves.map(o -> cartesianProduct(type, o));
    }

    private static <A, B> Tuple2<A, B> cartesianProduct(A a, B b) {
        return Tuple2.tuple(a, b);
    }

    private static Stream<Tuple2<RequestStatus, RequestStatus>> from(RequestStatus from, Stream<RequestStatus> to) {
        return cartesianProduct(from, to);
    }

    private static Stream<RequestStatus> to(RequestStatus... statuses) {
        return Stream.of(statuses);
    }

    private static <A, B> Stream<Tuple2<A, B>> cartesianProduct(A a, Stream<B> stream) {
        return stream.map(b -> cartesianProduct(a, b));
    }

    private static <A, B, C> Tuple3<A, B, C> cartesianProduct(A a, Tuple2<B, C> tuple) {
        return Tuple3.tuple(a, tuple._1, tuple._2);
    }

    @Test
    public void testAllowedStatusMovements() {
        Map<RequestType, Map<RequestStatus, Set<RequestStatus>>> allowedDestinationsByTypeByStatusFrom =
                allAllowedMovements().collect(
                        groupingBy(Tuple3::get1,
                                groupingBy(Tuple3::get2,
                                        mapping(Tuple3::get3, toSet()))));

        for (RequestType type : RequestType.values()) {
            for (RequestStatus status : RequestStatus.values()) {
                List<RequestStatus> actual =
                        Optional.ofNullable(ALLOWED_MOVES.get(type))
                                .map(map -> map.get(status))
                                .orElse(Collections.emptyList());

                Set<RequestStatus> expected =
                        allowedDestinationsByTypeByStatusFrom
                                .getOrDefault(type, Collections.emptyMap())
                                .getOrDefault(status, Collections.emptySet());

                assertions.assertThat(actual)
                        .as("testing allowed movements for type [%s] from status [%s]", type, status)
                        .containsExactlyInAnyOrder(expected.toArray(new RequestStatus[]{}));
            }
        }
    }

    /**
     * Проверка отсутствия цикла в графе переходов статусов, относящихся к статусам склада.
     * Для проверки используется dfs (обход в глубину) по графу из статусов, считающихся начальными складскими,
     * то есть {@link RequestStatus#ACCEPTED_BY_SERVICE} и {@link RequestStatus#ACCEPTED_BY_XDOC_SERVICE}.
     */
    @Test
    public void testNoWarehouseCyclesInTransitionGraph() {
        for (RequestType requestType : RequestType.values()) {
            dfsToCheckCycleInWarehouseStatusesGraph(requestType, ALLOWED_MOVES.get(requestType),
                    List.of(ACCEPTED_BY_SERVICE, ACCEPTED_BY_XDOC_SERVICE),
                    Set.of(List.of(ACCEPTED_BY_SERVICE, IN_PROGRESS, ACCEPTED_BY_SERVICE)));
        }
    }

    private List<RequestStatus> dfs(Map<RequestStatus, List<RequestStatus>> graph,
                                    RequestStatus vertex,
                                    List<RequestStatus> currentPath,
                                    Set<RequestStatus> visited) {
        currentPath.add(vertex);
        visited.add(vertex);
        List<RequestStatus> toVertexes = graph.get(vertex);
        if (toVertexes != null) {
            for (RequestStatus toVertex : toVertexes) {
                if (STATUSES_NOT_FROM_WAREHOUSE.contains(toVertex)) {
                    continue;
                }
                if (!visited.contains(toVertex)) {
                    List<RequestStatus> maybeCycle = dfs(graph, toVertex, currentPath, visited);
                    if (maybeCycle != null) {
                        return maybeCycle;
                    }
                } else {
                    if (currentPath.contains(toVertex)) {
                        currentPath.add(toVertex);
                        return currentPath;
                    }
                }
            }
        }
        currentPath.remove(vertex);
        return null;
    }

    private void dfsToCheckCycleInWarehouseStatusesGraph(RequestType type,
                                                         Map<RequestStatus, List<RequestStatus>> graph,
                                                         List<RequestStatus> initialStatuses,
                                                         Set<List<RequestStatus>> allowedCycles) {
        for (RequestStatus initialStatus : initialStatuses) {
            if (!graph.containsKey(initialStatus)) {
                continue;
            }
            List<RequestStatus> maybeCycle = dfs(graph, initialStatus, new ArrayList<>(), new HashSet<>());
            if (maybeCycle != null && !allowedCycles.contains(maybeCycle)) {
                assertions.fail("There is cycle in transition graph for " + type + ", cycle: " + maybeCycle);
            }
        }
    }

    private Stream<Tuple3<RequestType, RequestStatus, RequestStatus>> allAllowedMovements() {
        Set<Tuple2<RequestStatus, RequestStatus>> commonAllowedMoves = getCommonAllowedMoves();

        Set<Tuple2<RequestStatus, RequestStatus>> commonAllowedMovesForMovements = getCommonAllowedMovesForMovements();

        Set<Tuple2<RequestStatus, RequestStatus>> inventoryingSupplyAllowedMoves = Stream.of(
                from(CREATED, to(VALIDATED, INVALID, CANCELLED, WAITING_FOR_CONFIRMATION, SENT_TO_SERVICE,
                        ACCEPTED_BY_SERVICE, PROCESSED, FINISHED)),
                from(WAITING_FOR_CONFIRMATION, to(VALIDATED, CANCELLATION_REQUESTED)),
                from(VALIDATED, to(SENT_TO_SERVICE, CANCELLATION_REQUESTED)),
                from(REJECTED_BY_SERVICE, to(CREATED)),
                from(INVALID, to(CREATED)),
                from(SENT_TO_SERVICE, to(REJECTED_BY_SERVICE, ACCEPTED_BY_SERVICE)),
                from(CANCELLATION_REQUESTED,
                        to(ACCEPTED_BY_SERVICE, REJECTED_BY_SERVICE, CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(CANCELLATION_SENT_TO_SERVICE, to(CANCELLED, ACCEPTED_BY_SERVICE, CANCELLATION_REJECTED)),
                from(CANCELLATION_REJECTED, to(ACCEPTED_BY_SERVICE)),
                from(ACCEPTED_BY_SERVICE,
                        to(ARRIVED_TO_SERVICE, INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED,
                                FINISHED, CANCELLED, CANCELLATION_REQUESTED,
                                CANCELLATION_SENT_TO_SERVICE, SENT_TO_XDOC_SERVICE, SENT_TO_SERVICE,
                                PLAN_REGISTRY_CREATED)),
                from(SENT_TO_XDOC_SERVICE,
                        to(REJECTED_BY_XDOC_SERVICE, ACCEPTED_BY_XDOC_SERVICE, ARRIVED_TO_SERVICE,
                                INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS,
                                PROCESSED, CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE)),
                from(ACCEPTED_BY_XDOC_SERVICE,
                        to(ARRIVED_TO_XDOC_SERVICE, SHIPPED_TO_SERVICE, ARRIVED_TO_SERVICE,
                                INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED,
                                CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE)),
                from(REJECTED_BY_XDOC_SERVICE, to(CANCELLED)),
                from(ARRIVED_TO_XDOC_SERVICE,
                        to(SHIPPED_TO_SERVICE, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(SHIPPED_TO_SERVICE,
                        to(ARRIVED_TO_SERVICE, INITIAL_ACCEPTANCE_COMPLETED,
                                IN_PROGRESS, PROCESSED, CANCELLATION_REQUESTED,
                                CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(ARRIVED_TO_SERVICE, to(INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED, CANCELLED)),
                from(INITIAL_ACCEPTANCE_COMPLETED, to(INITIAL_ACCEPTANCE_DETAILS_LOADED)),
                from(INITIAL_ACCEPTANCE_DETAILS_LOADED, to(IN_PROGRESS, PROCESSED, CANCELLED)),
                from(IN_PROGRESS, to(PROCESSED)),
                from(PROCESSED, to(FINISHED)),
                from(PLAN_REGISTRY_CREATED, to(PLAN_REGISTRY_SENT, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE
                )),
                from(PLAN_REGISTRY_SENT, to(PLAN_REGISTRY_ACCEPTED, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE
                )),
                from(PLAN_REGISTRY_ACCEPTED, to(AWAITING_FOR_UNLOADING, ARRIVED_TO_SERVICE,
                        INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED,
                        CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE,
                        REJECTED_BY_SERVICE
                ))
        ).flatMap(Function.identity()).collect(toSet());

        Set<Tuple2<RequestStatus, RequestStatus>> withdrawStatuses = Stream.of(
                from(CREATED, to(VALIDATED, INVALID, CANCELLED, PLAN_REGISTRY_CREATED, PLAN_REGISTRY_SENT,
                        PLAN_REGISTRY_ACCEPTED)),
                from(VALIDATED, to(SENT_TO_SERVICE, CANCELLATION_REQUESTED)),
                from(REJECTED_BY_SERVICE, to(CREATED)),
                from(INVALID, to(CREATED)),
                from(SENT_TO_SERVICE, to(REJECTED_BY_SERVICE, ACCEPTED_BY_SERVICE, PLAN_REGISTRY_ACCEPTED, IN_PROGRESS,
                        READY_TO_WITHDRAW)),
                from(CANCELLATION_REQUESTED,
                        to(REJECTED_BY_SERVICE, ACCEPTED_BY_SERVICE, CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(CANCELLATION_SENT_TO_SERVICE, to(CANCELLED, ACCEPTED_BY_SERVICE, CANCELLATION_REJECTED)),
                from(CANCELLATION_REJECTED, to(ACCEPTED_BY_SERVICE)),
                from(ACCEPTED_BY_SERVICE,
                        to(SENT_TO_SERVICE, PLAN_REGISTRY_ACCEPTED, IN_PROGRESS, READY_TO_WITHDRAW, PROCESSED,
                                CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE,
                                PLAN_REGISTRY_CREATED)),
                from(PLAN_REGISTRY_CREATED, to(INVALID, PLAN_REGISTRY_SENT, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE)),
                from(PLAN_REGISTRY_SENT, to(PLAN_REGISTRY_ACCEPTED, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE)),
                from(PLAN_REGISTRY_ACCEPTED, to(SENT_TO_SERVICE, IN_PROGRESS, READY_TO_WITHDRAW, PROCESSED, CANCELLED,
                        CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE)),
                from(IN_PROGRESS, to(SENT_TO_SERVICE, ACCEPTED_BY_SERVICE, READY_TO_WITHDRAW, PROCESSED, CANCELLED)),
                from(READY_TO_WITHDRAW, to(SENT_TO_SERVICE, PROCESSED, CANCELLED)),
                from(PROCESSED, to(FINISHED))
        ).flatMap(Function.identity()).collect(toSet());

        Set<Tuple2<RequestStatus, RequestStatus>> withdrawStatusesForMovements = getWithdrawStatusesForMovements();

        Set<Tuple2<RequestStatus, RequestStatus>> inventoryingWithdrawStatuses = Stream.of(
                from(CREATED, to(VALIDATED, INVALID, SENT_TO_SERVICE, ACCEPTED_BY_SERVICE, PROCESSED, FINISHED)),
                from(VALIDATED, to(SENT_TO_SERVICE)),
                from(REJECTED_BY_SERVICE, to(CREATED)),
                from(INVALID, to(CREATED)),
                from(SENT_TO_SERVICE, to(REJECTED_BY_SERVICE, ACCEPTED_BY_SERVICE)),
                from(ACCEPTED_BY_SERVICE, to(IN_PROGRESS, READY_TO_WITHDRAW, PROCESSED, FINISHED)),
                from(IN_PROGRESS, to(READY_TO_WITHDRAW, PROCESSED)),
                from(READY_TO_WITHDRAW, to(PROCESSED)),
                from(PROCESSED, to(FINISHED))
        ).flatMap(Function.identity()).collect(toSet());

        Set<Tuple2<RequestStatus, RequestStatus>> customerReturnsStatuses = getCustomerReturnsAllowedMoves();

        return combine(
                forType(CROSSDOCK, commonAllowedMoves.stream()),
                forType(CUSTOMER_RETURN, customerReturnsStatuses.stream()),
                forType(CUSTOMER_RETURN_SUPPLY, commonAllowedMoves.stream()),
                forType(INVALID_UNREDEEMED, commonAllowedMoves.stream()),
                forType(VALID_UNREDEEMED, commonAllowedMoves.stream()),
                forType(SUPPLY, commonAllowedMoves.stream()),
                forType(EXPENDABLE_MATERIALS, commonAllowedMoves.stream()),
                forType(ORDERS_SUPPLY, commonAllowedMoves.stream()),
                forType(ORDERS_WITHDRAW, withdrawStatuses.stream()),
                forType(SHADOW_SUPPLY, combine(
                        from(CREATED, to(VALIDATED, INVALID, WAITING_FOR_CONFIRMATION, CANCELLED)),
                        from(WAITING_FOR_CONFIRMATION, to(FINISHED, CANCELLATION_REQUESTED)),
                        from(VALIDATED, to(FINISHED, CANCELLATION_REQUESTED)),
                        from(CANCELLATION_REQUESTED, to(CANCELLED)))),
                forType(WITHDRAW, withdrawStatuses.stream()),
                forType(OPER_LOST_INVENTORYING_WITHDRAW, inventoryingWithdrawStatuses.stream()),
                forType(FIX_LOST_INVENTORYING_WITHDRAW, inventoryingWithdrawStatuses.stream()),
                forType(UTILIZATION_WITHDRAW, withdrawStatuses.stream()),
                forType(INVENTORYING_SUPPLY, inventoryingSupplyAllowedMoves.stream()),
                forType(MOVEMENT_SUPPLY, commonAllowedMovesForMovements.stream()),
                forType(MOVEMENT_WITHDRAW, withdrawStatusesForMovements.stream()),
                forType(ORDERS_RETURN_WITHDRAW, withdrawStatuses.stream()),
                forType(DROPOFF_RETURN_WITHDRAW, withdrawStatuses.stream()),
                forType(TRANSFER, combine(
                        from(PREPARED_FOR_CREATION, to(CREATED, CANCELLED)),
                        from(CREATED, to(VALIDATED, INVALID)),
                        from(VALIDATED,
                                to(SENT_TO_SERVICE, ACCEPTED_BY_SERVICE, REJECTED_BY_SERVICE, PROCESSED, IN_PROGRESS)),
                        from(REJECTED_BY_SERVICE, to(CREATED)),
                        from(INVALID, to(CREATED)),
                        from(SENT_TO_SERVICE, to(ACCEPTED_BY_SERVICE, REJECTED_BY_SERVICE, PROCESSED, IN_PROGRESS)),
                        from(ACCEPTED_BY_SERVICE, to(IN_PROGRESS, PROCESSED, REJECTED_BY_SERVICE)),
                        from(IN_PROGRESS, to(PROCESSED, REJECTED_BY_SERVICE)),
                        from(PROCESSED, to(FINISHED)))),
                forType(UPDATING_REQUEST, combine(
                        from(CREATED, to(VALIDATED, INVALID)),
                        from(VALIDATED, to(FINISHED)))),
                forType(SHADOW_WITHDRAW, combine(
                        from(CREATED, to(VALIDATED, INVALID, CANCELLED)),
                        from(VALIDATED, to(FINISHED, CANCELLATION_REQUESTED)),
                        from(CANCELLATION_REQUESTED, to(CANCELLED)))),
                forType(X_DOC_PARTNER_SUPPLY_TO_FF, getXdocToFfSupplyAllowedMoves().stream()),
                forType(X_DOC_PARTNER_SUPPLY_TO_DISTRIBUTION_CENTER, commonAllowedMoves.stream()),
                forType(XDOC_TRANSPORT_SUPPLY, commonAllowedMovesForMovements.stream()),
                forType(XDOC_TRANSPORT_WITHDRAW, withdrawStatusesForMovements.stream()),
                forType(ADDITIONAL_SUPPLY, commonAllowedMoves.stream()));
    }

    @NotNull
    private Set<Tuple2<RequestStatus, RequestStatus>> getCommonAllowedMoves() {
        return Stream.of(
                from(AWAITING_FOR_UNLOADING, to(UNLOADING, RECEIPT_REJECTED)),
                from(UNLOADING, to(ARRIVED_TO_SERVICE, RECEIPT_REJECTED)),
                from(CREATED, to(VALIDATED, INVALID, CANCELLED, WAITING_FOR_CONFIRMATION, PLAN_REGISTRY_CREATED,
                        PLAN_REGISTRY_SENT, PLAN_REGISTRY_ACCEPTED)),
                from(WAITING_FOR_CONFIRMATION, to(VALIDATED, CANCELLATION_REQUESTED, INVALID_CONFIRMATION)),
                from(INVALID_CONFIRMATION, to(WAITING_FOR_CONFIRMATION, CANCELLATION_REQUESTED)),
                from(VALIDATED, to(SENT_TO_SERVICE, CANCELLATION_REQUESTED)),
                from(REJECTED_BY_SERVICE, to(CREATED)),
                from(INVALID, to(CREATED)),
                from(SENT_TO_SERVICE, to(REJECTED_BY_SERVICE, ACCEPTED_BY_SERVICE)),
                from(CANCELLATION_REQUESTED,
                        to(ACCEPTED_BY_SERVICE, REJECTED_BY_SERVICE, CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(CANCELLATION_SENT_TO_SERVICE, to(CANCELLED, ACCEPTED_BY_SERVICE, CANCELLATION_REJECTED)),
                from(CANCELLATION_REJECTED, to(ACCEPTED_BY_SERVICE)),
                from(ACCEPTED_BY_SERVICE,
                        to(ARRIVED_TO_SERVICE, INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS,
                                PROCESSED, CANCELLED, CANCELLATION_REQUESTED,
                                CANCELLATION_SENT_TO_SERVICE, SENT_TO_XDOC_SERVICE, SENT_TO_SERVICE,
                                AWAITING_FOR_UNLOADING, PLAN_REGISTRY_CREATED, PLAN_REGISTRY_ACCEPTED)),
                from(PLAN_REGISTRY_CREATED, to(PLAN_REGISTRY_SENT, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE)),
                from(PLAN_REGISTRY_SENT, to(PLAN_REGISTRY_ACCEPTED, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE)),
                from(PLAN_REGISTRY_ACCEPTED, to(AWAITING_FOR_UNLOADING, ARRIVED_TO_SERVICE,
                        INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED,
                        CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE,
                        SENT_TO_SERVICE)),
                from(AWAITING_FOR_UNLOADING, to(UNLOADING)),
                from(UNLOADING, to(ARRIVED_TO_SERVICE, INITIAL_ACCEPTANCE_COMPLETED)),
                from(SENT_TO_XDOC_SERVICE,
                        to(REJECTED_BY_XDOC_SERVICE, ACCEPTED_BY_XDOC_SERVICE, ARRIVED_TO_SERVICE,
                                INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS,
                                PROCESSED, CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE)),
                from(ACCEPTED_BY_XDOC_SERVICE,
                        to(ARRIVED_TO_XDOC_SERVICE, SHIPPED_TO_SERVICE, ARRIVED_TO_SERVICE,
                                INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED,
                                CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE)),
                from(REJECTED_BY_XDOC_SERVICE, to(CANCELLED)),
                from(ARRIVED_TO_XDOC_SERVICE,
                        to(SHIPPED_TO_SERVICE, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(SHIPPED_TO_SERVICE,
                        to(ARRIVED_TO_SERVICE, INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS,
                                PROCESSED, CANCELLATION_REQUESTED,
                                CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(ARRIVED_TO_SERVICE, to(INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED, CANCELLED)),
                from(INITIAL_ACCEPTANCE_COMPLETED, to(INITIAL_ACCEPTANCE_DETAILS_LOADED)),
                from(INITIAL_ACCEPTANCE_DETAILS_LOADED, to(IN_PROGRESS, PROCESSED, CANCELLED)),
                from(IN_PROGRESS, to(PROCESSED)),
                from(PROCESSED, to(FINISHED))
        ).flatMap(Function.identity()).collect(toSet());
    }

    @NotNull
    private Set<Tuple2<RequestStatus, RequestStatus>> getCustomerReturnsAllowedMoves() {
        return Stream.of(
                from(PREPARED_FOR_CREATION, to(CREATED, INVALID, CANCELLED)),
                from(AWAITING_FOR_UNLOADING, to(UNLOADING, RECEIPT_REJECTED)),
                from(UNLOADING, to(ARRIVED_TO_SERVICE, RECEIPT_REJECTED)),
                from(CREATED, to(VALIDATED, INVALID, CANCELLED, WAITING_FOR_CONFIRMATION, PLAN_REGISTRY_CREATED,
                        PLAN_REGISTRY_SENT, PLAN_REGISTRY_ACCEPTED)),
                from(WAITING_FOR_CONFIRMATION, to(VALIDATED, CANCELLATION_REQUESTED, INVALID_CONFIRMATION)),
                from(INVALID_CONFIRMATION, to(WAITING_FOR_CONFIRMATION, CANCELLATION_REQUESTED)),
                from(VALIDATED, to(SENT_TO_SERVICE, CANCELLATION_REQUESTED)),
                from(REJECTED_BY_SERVICE, to(CREATED)),
                from(INVALID, to(CREATED)),
                from(SENT_TO_SERVICE, to(REJECTED_BY_SERVICE, ACCEPTED_BY_SERVICE)),
                from(CANCELLATION_REQUESTED,
                        to(ACCEPTED_BY_SERVICE, REJECTED_BY_SERVICE, CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(CANCELLATION_SENT_TO_SERVICE, to(CANCELLED, ACCEPTED_BY_SERVICE, CANCELLATION_REJECTED)),
                from(CANCELLATION_REJECTED, to(ACCEPTED_BY_SERVICE)),
                from(ACCEPTED_BY_SERVICE,
                        to(ARRIVED_TO_SERVICE, INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS,
                                PROCESSED, CANCELLED, CANCELLATION_REQUESTED,
                                CANCELLATION_SENT_TO_SERVICE, SENT_TO_XDOC_SERVICE, SENT_TO_SERVICE,
                                AWAITING_FOR_UNLOADING, PLAN_REGISTRY_CREATED, PLAN_REGISTRY_ACCEPTED)),
                from(PLAN_REGISTRY_CREATED, to(PLAN_REGISTRY_SENT, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE)),
                from(PLAN_REGISTRY_SENT, to(PLAN_REGISTRY_ACCEPTED, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE, FINISHED)),
                from(PLAN_REGISTRY_ACCEPTED, to(AWAITING_FOR_UNLOADING, ARRIVED_TO_SERVICE,
                        INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED,
                        CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE,
                        SENT_TO_SERVICE)),
                from(AWAITING_FOR_UNLOADING, to(UNLOADING)),
                from(UNLOADING, to(ARRIVED_TO_SERVICE, INITIAL_ACCEPTANCE_COMPLETED)),
                from(SENT_TO_XDOC_SERVICE,
                        to(REJECTED_BY_XDOC_SERVICE, ACCEPTED_BY_XDOC_SERVICE, ARRIVED_TO_SERVICE,
                                INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS,
                                PROCESSED, CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE)),
                from(ACCEPTED_BY_XDOC_SERVICE,
                        to(ARRIVED_TO_XDOC_SERVICE, SHIPPED_TO_SERVICE, ARRIVED_TO_SERVICE,
                                INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED,
                                CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE)),
                from(REJECTED_BY_XDOC_SERVICE, to(CANCELLED)),
                from(ARRIVED_TO_XDOC_SERVICE,
                        to(SHIPPED_TO_SERVICE, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(SHIPPED_TO_SERVICE,
                        to(ARRIVED_TO_SERVICE, INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS,
                                PROCESSED, CANCELLATION_REQUESTED,
                                CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(ARRIVED_TO_SERVICE, to(INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED, CANCELLED)),
                from(INITIAL_ACCEPTANCE_COMPLETED, to(INITIAL_ACCEPTANCE_DETAILS_LOADED)),
                from(INITIAL_ACCEPTANCE_DETAILS_LOADED, to(IN_PROGRESS, PROCESSED, CANCELLED)),
                from(IN_PROGRESS, to(PROCESSED)),
                from(PROCESSED, to(FINISHED)),
                from(REJECTED_BY_SERVICE, to(CREATED))
        ).flatMap(Function.identity()).collect(toSet());
    }

    private Set<Tuple2<RequestStatus, RequestStatus>> getWithdrawStatusesForMovements() {
        return Stream.of(
                from(CREATED, to(VALIDATED, INVALID, CANCELLED, WAITING_FOR_CONFIRMATION, SENT_TO_SERVICE,
                        ACCEPTED_BY_SERVICE, PROCESSED, FINISHED, PLAN_REGISTRY_CREATED, PLAN_REGISTRY_SENT,
                        PLAN_REGISTRY_ACCEPTED)),
                from(VALIDATED, to(SENT_TO_SERVICE, CANCELLATION_REQUESTED)),
                from(REJECTED_BY_SERVICE, to(CREATED)),
                from(INVALID, to(CREATED)),
                from(SENT_TO_SERVICE, to(REJECTED_BY_SERVICE, ACCEPTED_BY_SERVICE)),
                from(CANCELLATION_REQUESTED,
                        to(REJECTED_BY_SERVICE, ACCEPTED_BY_SERVICE, CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(CANCELLATION_SENT_TO_SERVICE, to(CANCELLED, ACCEPTED_BY_SERVICE, CANCELLATION_REJECTED)),
                from(CANCELLATION_REJECTED, to(ACCEPTED_BY_SERVICE)),
                from(ACCEPTED_BY_SERVICE,
                        to(IN_PROGRESS, PLAN_REGISTRY_ACCEPTED, READY_TO_WITHDRAW, PROCESSED, CANCELLED,
                                CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE,
                                PLAN_REGISTRY_CREATED)),
                from(PLAN_REGISTRY_CREATED, to(INVALID, PLAN_REGISTRY_SENT, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE)),
                from(PLAN_REGISTRY_SENT, to(PLAN_REGISTRY_ACCEPTED, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE)),
                from(PLAN_REGISTRY_ACCEPTED, to(PLAN_REGISTRY_CREATED, PLAN_REGISTRY_SENT, IN_PROGRESS,
                        READY_TO_WITHDRAW, PROCESSED, CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE,
                        REJECTED_BY_SERVICE)),
                from(IN_PROGRESS, to(ACCEPTED_BY_SERVICE, READY_TO_WITHDRAW, PROCESSED, CANCELLED)),
                from(READY_TO_WITHDRAW, to(PROCESSED, CANCELLED)),
                from(PROCESSED, to(FINISHED))
        ).flatMap(Function.identity()).collect(toSet());
    }

    private Set<Tuple2<RequestStatus, RequestStatus>> getCommonAllowedMovesForMovements() {
        return Stream.of(
                from(AWAITING_FOR_UNLOADING, to(UNLOADING, RECEIPT_REJECTED)),
                from(UNLOADING, to(ARRIVED_TO_SERVICE, RECEIPT_REJECTED)),
                from(CREATED, to(VALIDATED, INVALID, CANCELLED, WAITING_FOR_CONFIRMATION, SENT_TO_SERVICE,
                        ACCEPTED_BY_SERVICE, PROCESSED, FINISHED, PLAN_REGISTRY_CREATED, PLAN_REGISTRY_SENT,
                        PLAN_REGISTRY_ACCEPTED, ARRIVED_TO_SERVICE)),
                from(WAITING_FOR_CONFIRMATION, to(VALIDATED, CANCELLATION_REQUESTED, INVALID_CONFIRMATION)),
                from(INVALID_CONFIRMATION, to(WAITING_FOR_CONFIRMATION, CANCELLATION_REQUESTED)),
                from(VALIDATED, to(SENT_TO_SERVICE, CANCELLATION_REQUESTED)),
                from(REJECTED_BY_SERVICE, to(CREATED)),
                from(INVALID, to(CREATED)),
                from(SENT_TO_SERVICE, to(REJECTED_BY_SERVICE, ACCEPTED_BY_SERVICE)),
                from(CANCELLATION_REQUESTED,
                        to(ACCEPTED_BY_SERVICE, REJECTED_BY_SERVICE, CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(CANCELLATION_SENT_TO_SERVICE, to(CANCELLED, ACCEPTED_BY_SERVICE, CANCELLATION_REJECTED)),
                from(CANCELLATION_REJECTED, to(ACCEPTED_BY_SERVICE)),
                from(ACCEPTED_BY_SERVICE,
                        to(INITIAL_ACCEPTANCE_COMPLETED, ARRIVED_TO_SERVICE, IN_PROGRESS,
                                PROCESSED, CANCELLED, CANCELLATION_REQUESTED,
                                CANCELLATION_SENT_TO_SERVICE, SENT_TO_XDOC_SERVICE, SENT_TO_SERVICE,
                                AWAITING_FOR_UNLOADING, REJECTED_BY_SERVICE, PLAN_REGISTRY_CREATED)),
                from(PLAN_REGISTRY_CREATED, to(PLAN_REGISTRY_SENT, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE)),
                from(PLAN_REGISTRY_SENT, to(PLAN_REGISTRY_ACCEPTED, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE)),
                from(PLAN_REGISTRY_ACCEPTED, to(AWAITING_FOR_UNLOADING, ARRIVED_TO_SERVICE,
                        INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED,
                        CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE)),
                from(AWAITING_FOR_UNLOADING, to(UNLOADING)),
                from(UNLOADING, to(ARRIVED_TO_SERVICE, INITIAL_ACCEPTANCE_COMPLETED)),
                from(SENT_TO_XDOC_SERVICE,
                        to(REJECTED_BY_XDOC_SERVICE, ACCEPTED_BY_XDOC_SERVICE, ARRIVED_TO_SERVICE,
                                INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS,
                                PROCESSED, CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE)),
                from(ACCEPTED_BY_XDOC_SERVICE,
                        to(ARRIVED_TO_XDOC_SERVICE, SHIPPED_TO_SERVICE, ARRIVED_TO_SERVICE,
                                INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED,
                                CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE)),
                from(REJECTED_BY_XDOC_SERVICE, to(CANCELLED)),
                from(ARRIVED_TO_XDOC_SERVICE,
                        to(SHIPPED_TO_SERVICE, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(SHIPPED_TO_SERVICE,
                        to(ARRIVED_TO_SERVICE, INITIAL_ACCEPTANCE_COMPLETED,
                                IN_PROGRESS, PROCESSED, CANCELLATION_REQUESTED,
                                CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(ARRIVED_TO_SERVICE, to(INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED, CANCELLED)),
                from(INITIAL_ACCEPTANCE_COMPLETED, to(INITIAL_ACCEPTANCE_DETAILS_LOADED)),
                from(INITIAL_ACCEPTANCE_DETAILS_LOADED, to(IN_PROGRESS, PROCESSED, CANCELLED)),
                from(IN_PROGRESS, to(PROCESSED)),
                from(PROCESSED, to(FINISHED))
        ).flatMap(Function.identity()).collect(toSet());
    }


    @NotNull
    private Set<Tuple2<RequestStatus, RequestStatus>> getXdocToFfSupplyAllowedMoves() {
        return Stream.of(
                from(AWAITING_FOR_UNLOADING, to(UNLOADING, RECEIPT_REJECTED)),
                from(UNLOADING, to(ARRIVED_TO_SERVICE, RECEIPT_REJECTED)),
                from(CREATED, to(VALIDATED, INVALID, CANCELLED, WAITING_FOR_CONFIRMATION, PLAN_REGISTRY_CREATED,
                        PLAN_REGISTRY_SENT, PLAN_REGISTRY_ACCEPTED)),
                from(WAITING_FOR_CONFIRMATION, to(VALIDATED, CANCELLATION_REQUESTED, INVALID_CONFIRMATION)),
                from(INVALID_CONFIRMATION, to(WAITING_FOR_CONFIRMATION, CANCELLATION_REQUESTED)),
                from(VALIDATED, to(SENT_TO_SERVICE, CANCELLATION_REQUESTED, FINISHED)),
                from(REJECTED_BY_SERVICE, to(CREATED)),
                from(INVALID, to(CREATED)),
                from(SENT_TO_SERVICE, to(REJECTED_BY_SERVICE, ACCEPTED_BY_SERVICE)),
                from(CANCELLATION_REQUESTED,
                        to(ACCEPTED_BY_SERVICE, REJECTED_BY_SERVICE, CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(CANCELLATION_SENT_TO_SERVICE, to(CANCELLED, ACCEPTED_BY_SERVICE, CANCELLATION_REJECTED)),
                from(CANCELLATION_REJECTED, to(ACCEPTED_BY_SERVICE)),
                from(ACCEPTED_BY_SERVICE,
                        to(ARRIVED_TO_SERVICE, INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED,
                                CANCELLED, CANCELLATION_REQUESTED,
                                CANCELLATION_SENT_TO_SERVICE, SENT_TO_XDOC_SERVICE, SENT_TO_SERVICE,
                                AWAITING_FOR_UNLOADING, PLAN_REGISTRY_CREATED, ARRIVED_TO_XDOC_SERVICE,
                                ACCEPTED_BY_XDOC_SERVICE, REJECTED_BY_XDOC_SERVICE, PLAN_REGISTRY_ACCEPTED)),
                from(PLAN_REGISTRY_CREATED, to(PLAN_REGISTRY_SENT, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE)),
                from(PLAN_REGISTRY_SENT, to(PLAN_REGISTRY_ACCEPTED, CANCELLED, CANCELLATION_REQUESTED,
                        CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE)),
                from(PLAN_REGISTRY_ACCEPTED, to(AWAITING_FOR_UNLOADING, ARRIVED_TO_SERVICE,
                        INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED,
                        CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_SERVICE,
                        INITIAL_ACCEPTANCE_COMPLETED_BY_XDOC_SERVICE,
                        SENT_TO_SERVICE, ACCEPTED_BY_XDOC_SERVICE, ARRIVED_TO_XDOC_SERVICE,
                        REJECTED_BY_XDOC_SERVICE)),
                from(AWAITING_FOR_UNLOADING, to(UNLOADING)),
                from(UNLOADING, to(ARRIVED_TO_SERVICE, INITIAL_ACCEPTANCE_COMPLETED)),
                from(SENT_TO_XDOC_SERVICE,
                        to(REJECTED_BY_XDOC_SERVICE, ACCEPTED_BY_XDOC_SERVICE, ARRIVED_TO_SERVICE,
                                INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS,
                                PROCESSED, CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE)),
                from(ACCEPTED_BY_XDOC_SERVICE,
                        to(ARRIVED_TO_XDOC_SERVICE, SHIPPED_TO_SERVICE, ARRIVED_TO_SERVICE,
                                INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED,
                                INITIAL_ACCEPTANCE_COMPLETED_BY_XDOC_SERVICE,
                                CANCELLED, CANCELLATION_REQUESTED, CANCELLATION_SENT_TO_SERVICE)),
                from(REJECTED_BY_XDOC_SERVICE, to(CANCELLED)),
                from(INITIAL_ACCEPTANCE_COMPLETED_BY_XDOC_SERVICE,
                        to(ARRIVED_TO_XDOC_SERVICE, CANCELLED, CANCELLATION_REQUESTED,
                                CANCELLATION_SENT_TO_SERVICE, REJECTED_BY_XDOC_SERVICE)),
                from(ARRIVED_TO_XDOC_SERVICE,
                        to(SHIPPED_TO_SERVICE, ARRIVED_TO_SERVICE,
                                INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED, CANCELLATION_REQUESTED,
                                CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(SHIPPED_TO_SERVICE,
                        to(ARRIVED_TO_SERVICE, INITIAL_ACCEPTANCE_COMPLETED,
                                IN_PROGRESS, PROCESSED, CANCELLATION_REQUESTED,
                                CANCELLATION_SENT_TO_SERVICE, CANCELLED)),
                from(ARRIVED_TO_SERVICE, to(INITIAL_ACCEPTANCE_COMPLETED, IN_PROGRESS, PROCESSED, CANCELLED)),
                from(INITIAL_ACCEPTANCE_COMPLETED, to(INITIAL_ACCEPTANCE_DETAILS_LOADED)),
                from(INITIAL_ACCEPTANCE_DETAILS_LOADED, to(IN_PROGRESS, PROCESSED, CANCELLED)),
                from(IN_PROGRESS, to(PROCESSED)),
                from(PROCESSED, to(FINISHED))
        ).flatMap(Function.identity()).collect(toSet());
    }

}
