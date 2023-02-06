package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import yandex.market.combinator.v0.CombinatorGrpc;
import yandex.market.combinator.v0.CombinatorOuterClass.ReturnRouteRequest;
import yandex.market.combinator.v0.CombinatorOuterClass.ReturnRouteRequest.Builder;
import yandex.market.combinator.v0.CombinatorOuterClass.ReturnRouteRequest.ReturnRoutePoint;
import yandex.market.combinator.v0.CombinatorOuterClass.ReturnRouteResponse;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.LocationDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.enums.SegmentType;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.config.locals.UuidGenerator;
import ru.yandex.market.logistics.lrm.config.properties.FeatureProperties;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.model.entity.enums.LogisticPointType;
import ru.yandex.market.logistics.lrm.model.exception.ResourceNotFoundException;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnSegmentGetRoutePayload;
import ru.yandex.market.logistics.lrm.queue.processor.ReturnSegmentGetRouteProcessor;
import ru.yandex.market.logistics.lrm.repository.ydb.converter.ReturnRouteHistoryConverter;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.repository.ydb.description.ReturnRouteHistoryTableDescription;
import ru.yandex.market.logistics.lrm.repository.ydb.description.ReturnRouteHistoryTableDescription.ReturnRouteHistory;
import ru.yandex.market.logistics.lrm.service.combinator.NoRouteException;
import ru.yandex.market.logistics.lrm.service.meta.DetachedTypedEntity;
import ru.yandex.market.logistics.lrm.service.meta.EntityMetaService;
import ru.yandex.market.logistics.lrm.service.meta.model.ReturnEntityUseBackwardGraphMeta;
import ru.yandex.market.logistics.lrm.utils.CombinatorRouteFactory;
import ru.yandex.market.logistics.lrm.utils.ProtobufMessagesUtils;
import ru.yandex.market.logistics.lrm.utils.YdbTestRepositoryUtils;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentServiceDto;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.point.ReturnPointInfoResponse;
import ru.yandex.market.logistics.management.entity.type.ActivityStatus;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Получение возвратного маршрута")
@DatabaseSetup("/database/tasks/return-segment/get-route/before/setup.xml")
class ReturnSegmentGetRouteProcessorTest extends AbstractIntegrationYdbTest {
    private static final long RETURN_ID = 1;
    private static final long PICKUP_SEGMENT_ID = 1;
    private static final long SORTING_CENTER_SEGMENT_ID = 2;
    private static final long DROPOFF_SEGMENT_ID = 3;
    private static final long SHOP_SEGMENT_ID = 4;
    private static final long FF_SEGMENT_ID = 7;
    private static final long UTILIZER_SEGMENT_ID = 7;

    private static final long SHOP_PARTNER_ID = 2004;
    private static final long SORTING_CENTER_PARTNER_ID = 2002;
    private static final long DROPOFF_PARTNER_ID = 2003;
    private static final long SHOP_WAREHOUSE_ID = 1004;
    private static final long DROPOFF_LOGISTIC_POINT_ID = 1003;
    private static final long SC_2_PARTNER_ID = 2006;
    private static final long FF_PARTNER_ID = 2007;
    private static final long SECOND_FF_PARTNER_ID = 2008;
    private static final long SC_2_WAREHOUSE_ID = 1006;

    private static final String ORDER_EXTERNAL_ID = "654987";

    private static final Instant NOW_TIME = Instant.parse("2021-11-11T11:11:11.00Z");
    private static final Timestamp NOW_TIMESTAMP = Timestamp.newBuilder()
        .setSeconds(NOW_TIME.getEpochSecond())
        .setNanos(NOW_TIME.getNano())
        .build();
    private static final String NEW_GRAPH_FLAG = "sbg_for_return_route=1";

    @Autowired
    private ReturnSegmentGetRouteProcessor processor;

    @Autowired
    private CombinatorGrpc.CombinatorImplBase combinatorImplBase;

    @Autowired
    private EntityMetaService entityMetaService;

    @Autowired
    private ReturnRouteHistoryTableDescription routeHistoryTableDescription;

    @Autowired
    private EntityMetaTableDescription entityMetaTableDescription;

    @Autowired
    private ReturnRouteHistoryConverter returnRouteHistoryConverter;

    @Autowired
    private UuidGenerator uuidGenerator;

    @Autowired
    private FeatureProperties featureProperties;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LomClient lomClient;

    @BeforeEach
    void setUp() {
        // Если не валить этот вызов для неизвестных параметров, тест зависает, если есть ошибка в моках
        doAnswer(i -> {
            throw new RuntimeException("Unexpected request: " + i.getArgument(0));
        }).when(combinatorImplBase).getReturnRoute(any(), any());
        clock.setFixed(NOW_TIME, DateTimeUtils.MOSCOW_ZONE);
        featureProperties.setEnableControlPointUseInGetRoute(false);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(combinatorImplBase, lmsClient);
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTableDescription, entityMetaTableDescription);
    }

    @Test
    @DisplayName("Успешное получение возвратного маршрута для сегмента дропофа")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/second_sc_route_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successSortingCenter() throws Exception {
        ReturnRouteRequest returnRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestFromPoint(DROPOFF_PARTNER_ID, DROPOFF_LOGISTIC_POINT_ID))
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();
        ReturnRouteResponse returnRouteResponse = CombinatorRouteFactory.routeResponse(
            CombinatorRouteFactory.routeScPoint(DROPOFF_SEGMENT_ID, "Second SC partner name"),
            CombinatorRouteFactory.routeFfPoint(SHOP_SEGMENT_ID, "Shop partner name")
        );

        try (var ignored = mockCombinatorRoute(returnRouteRequest, returnRouteResponse)) {
            execute(DROPOFF_SEGMENT_ID);
        }

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=INFO\t\
                    format=plain\t\
                    code=GET_RETURN_ROUTE_LOG\t\
                    payload=Got return route from combinator\t\
                    request_id=test-request-id\t\
                    entity_types=returnSegment\t\
                    entity_values=returnSegment:3\t\
                    extra_keys=request,response\t\
                    extra_values={\\"from\\":{\\"logistic_point_id\\":\\"1003\\",\\"partner_id\\":\\"2003\\"},\
                    \\"to\\":{\\"partner_id\\":\\"2004\\"},\\"start_time\\":\\"2021-11-11T11:11:11Z\\"},\
                    {\\"points\\":[{\\"segment_type\\":\\"WAREHOUSE\\",\
                    \\"segment_id\\":\\"3\\",\\"partner_type\\":\\"SORTING_CENTER\\",\
                    \\"partner_name\\":\\"Second SC partner name\\"},{\\"segment_type\\":\\"WAREHOUSE\\",\
                    \\"segment_id\\":\\"4\\",\\"partner_type\\":\\"FULFILLMENT\\",\
                    \\"partner_name\\":\\"Shop partner name\\"}]}
                    """
            );
    }

    @Test
    @DisplayName("Успешная обработка сегмента ВСЦ. Нет похода в комби")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/return_sorting_center.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/return_sc_create_in_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successReturnSortingCenter() throws Exception {
        try (
            var ignored1 = mockPartner(SHOP_PARTNER_ID, PartnerStatus.ACTIVE);
            var ignored2 = mockLogisticsPoint(SHOP_PARTNER_ID, PartnerType.DROPSHIP, SHOP_WAREHOUSE_ID)
        ) {
            execute(SORTING_CENTER_SEGMENT_ID);
        }
    }

    @Test
    @DisplayName("Сегмент ВСЦ. Следующая контрольная точка на утилизаторе, есть поход в комби")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/return_sorting_center.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/control_point_utilizer.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/return_sc_route_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnScCallGetRouteIfUtilizerNext() throws Exception {
        ReturnRouteRequest requestFromReturnScToUtilizer = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestFromPoint(550L, 1000550L))
            .setTo(CombinatorRouteFactory.requestRoutePoint(559L, 5590L))
            .setStartTime(NOW_TIMESTAMP)
            .build();

        ReturnRouteResponse response = CombinatorRouteFactory.routeResponse(
            routeSortingCenterPoint(),
            CombinatorRouteFactory.routeScPoint(SORTING_CENTER_SEGMENT_ID, "Second SC partner name"),
            CombinatorRouteFactory.routeScPoint(UTILIZER_SEGMENT_ID, "Utilizer partner name")
        );
        try (
            var ignored1 = mockCombinatorRoute(requestFromReturnScToUtilizer, response)
        ) {
            featureProperties.setEnableControlPointUseInGetRoute(true);
            execute(SORTING_CENTER_SEGMENT_ID);
        }
    }

    @Test
    @DisplayName("Сегмент ВСЦ. Следующая контрольная точка на утилизаторе, форсированный поход в комби")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/return_sorting_center.xml",
        type = DatabaseOperation.UPDATE
    )
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/control_point_utilizer.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/return_sc_route_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void returnScForceCallGetRouteIfUtilizerNext() throws Exception {
        ReturnRouteRequest requestFromReturnScToUtilizer = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestFromPoint(550L, 1000550L))
            .setTo(CombinatorRouteFactory.requestRoutePoint(559L, 5590L))
            .setStartTime(NOW_TIMESTAMP)
            .build();

        ReturnRouteResponse response = CombinatorRouteFactory.routeResponse(
            routeSortingCenterPoint(),
            CombinatorRouteFactory.routeScPoint(SORTING_CENTER_SEGMENT_ID, "Second SC partner name"),
            CombinatorRouteFactory.routeScPoint(UTILIZER_SEGMENT_ID, "Utilizer partner name")
        );
        try (
            var ignored1 = mockCombinatorRoute(requestFromReturnScToUtilizer, response)
        ) {
            featureProperties.setEnableControlPointUseInGetRoute(false);
            execute(SORTING_CENTER_SEGMENT_ID);
        }
    }

    @Test
    @DisplayName("Не найден возвратный сегмент")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void errorReturnSegmentNotFound() {
        softly.assertThatThrownBy(() -> execute(1394L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find RETURN_SEGMENT with ids [1394]");
    }

    @ParameterizedTest
    @MethodSource
    @DisplayName("Неизвестный тип сегмента")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void errorUnexpectedSegmentLogisticPointType(long segmentId, LogisticPointType type) {
        softly.assertThatThrownBy(() -> execute(segmentId))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Unexpected logisticPointType %s".formatted(type));
    }

    @Nonnull
    static Stream<Arguments> errorUnexpectedSegmentLogisticPointType() {
        return Stream.of(
            Arguments.of(PICKUP_SEGMENT_ID, LogisticPointType.PICKUP),
            Arguments.of(SHOP_SEGMENT_ID, LogisticPointType.SHOP),
            Arguments.of(FF_SEGMENT_ID, LogisticPointType.FULFILLMENT)
        );
    }

    @Test
    @DisplayName("Ошибка при обращении к комбинатору: точка назначения является финальной")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/destination_final.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/destination_final.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void errorCombinatorRequestFailedDestinationFinal() throws Exception {
        ReturnRouteRequest returnRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestFromPoint(DROPOFF_PARTNER_ID, DROPOFF_LOGISTIC_POINT_ID))
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();

        try (var ignored = mockCombinatorError(returnRouteRequest)) {
            execute(DROPOFF_SEGMENT_ID);
        }

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=INFO\t\
                    format=plain\t\
                    code=GET_RETURN_ROUTE_ERROR\t\
                    payload=Destination point is final, create task for SC\t\
                    request_id=test-request-id\t\
                    extra_keys=segmentId\t\
                    extra_values=3
                    """
            );
    }

    @Test
    @DisplayName("Ошибка при обращении к комбинатору: точка назначения не является финальной")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/before/setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void errorCombinatorRequestFailedDestinationNotFinal() throws Exception {
        ReturnRouteRequest returnRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(requestSortingCenterPoint())
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();

        try (
            var ignored1 = mockCombinatorError(returnRouteRequest);
            var ignored2 = mockGetOrder(createOrderReturnToDropoff());
            var ignored3 = mockPartner(SHOP_PARTNER_ID, PartnerStatus.ACTIVE);
            var ignored4 = mockLogisticSegment(
                null,
                SHOP_PARTNER_ID,
                LogisticSegmentType.WAREHOUSE,
                1L,
                null
            );
            var ignored5 = mockLogisticSegment(
                1L,
                null,
                LogisticSegmentType.MOVEMENT,
                null,
                1006360L
            )
        ) {
            assertCombinatorError();
        }

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=ERROR\t\
                    format=json-exception\t\
                    code=GET_RETURN_ROUTE_ERROR\t\
                    """
            )
            .contains("payload={\\\"eventMessage\\\":\\\"Cannot get return route from combinator")
            .contains(
                """
                    request_id=test-request-id\t\
                    entity_types=returnSegment\t\
                    entity_values=returnSegment:2\t\
                    extra_keys=request\t\
                    extra_values={\
                    \\"from\\":{\\"logistic_point_id\\":\\"1002\\",\\"partner_id\\":\\"2002\\"},\
                    \\"to\\":{\\"partner_id\\":\\"2004\\"},\
                    \\"start_time\\":\\"2021-11-11T11:11:11Z\\"\
                    }
                    """
            );
    }

    @Test
    @DisplayName("Ошибка при обращении к комбинатору, запрос через другой СЦ")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/another_sorting_center.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/first_sc_route_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void combinatorErrorSortingCenterFallback() throws Exception {
        ReturnRouteRequest firstRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestRoutePoint(2020, 1020))
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();
        ReturnRouteRequest secondRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestRoutePoint(2010, 1010))
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();

        ReturnRouteRequest successRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(requestSortingCenterPoint())
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();
        ReturnRouteResponse successRouteResponse = CombinatorRouteFactory.routeResponse(
            routeSortingCenterPoint(),
            CombinatorRouteFactory.routeScPoint(DROPOFF_SEGMENT_ID, "Second SC partner name"),
            CombinatorRouteFactory.routeFfPoint(SHOP_SEGMENT_ID, "Shop partner name")
        );

        try (
            var ignored1 = mockCombinatorError(firstRouteRequest);
            var ignored2 = mockLogisticsPoint(2010, PartnerType.SORTING_CENTER, 1010);
            var ignored3 = mockCombinatorError(secondRouteRequest);
            var ignored4 = mockLogisticsPoint(2002, PartnerType.SORTING_CENTER, 1002);
            var ignored5 = mockCombinatorRoute(successRouteRequest, successRouteResponse)
        ) {
            processor.execute(
                ReturnSegmentGetRoutePayload.builder()
                    .returnSegmentId(SORTING_CENTER_SEGMENT_ID)
                    .sortingCenterPartnerIds(List.of(2010L, 2002L))
                    .useStartTime(true)
                    .build()
            );
        }

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=WARN\t\
                    format=plain\t\
                    code=GET_RETURN_ROUTE_ERROR\t\
                    """
            )
            .contains("Using backup sorting center 2002 for segment 2");
    }

    @Test
    @DisplayName("Ошибка при обращении к комбинатору, запрос через другой СЦ взятый из ключа пропертей")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/another_sorting_center.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/first_sc_route_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void combinatorErrorSortingCenterFallbackFromPropertiesMapKey() throws Exception {
        featureProperties.getScDuplicatePartnerMap().put(2020L, Set.of(2002L, 2020L));
        ReturnRouteRequest firstRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestRoutePoint(2020, 1020))
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();

        ReturnRouteRequest successRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(requestSortingCenterPoint())
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();
        ReturnRouteResponse successRouteResponse = CombinatorRouteFactory.routeResponse(
            routeSortingCenterPoint(),
            CombinatorRouteFactory.routeScPoint(DROPOFF_SEGMENT_ID, "Second SC partner name"),
            CombinatorRouteFactory.routeFfPoint(SHOP_SEGMENT_ID, "Shop partner name")
        );

        try (
            var ignored1 = mockCombinatorError(firstRouteRequest);
            var ignored2 = mockLogisticsPoint(2002, PartnerType.SORTING_CENTER, 1002);
            var ignored3 = mockCombinatorRoute(successRouteRequest, successRouteResponse)
        ) {
            execute(SORTING_CENTER_SEGMENT_ID);
        }
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=WARN\t\
                    format=plain\t\
                    code=GET_RETURN_ROUTE_ERROR\t\
                    """
            )
            .contains("Using backup sorting center 2002 for segment 2");
        featureProperties.getScDuplicatePartnerMap().remove(2020L);
    }

    @Test
    @DisplayName("Ошибка при обращении к комбинатору, запрос через другой СЦ взятый из пропертей")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/another_sorting_center.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/first_sc_route_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void combinatorErrorSortingCenterFallbackFromPropertiesMap() throws Exception {
        featureProperties.getScDuplicatePartnerMap().put(404L, Set.of(2002L, 2020L));
        ReturnRouteRequest firstRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestRoutePoint(2020, 1020))
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();

        ReturnRouteRequest successRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(requestSortingCenterPoint())
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();
        ReturnRouteResponse successRouteResponse = CombinatorRouteFactory.routeResponse(
            routeSortingCenterPoint(),
            CombinatorRouteFactory.routeScPoint(DROPOFF_SEGMENT_ID, "Second SC partner name"),
            CombinatorRouteFactory.routeFfPoint(SHOP_SEGMENT_ID, "Shop partner name")
        );

        try (
            var ignored1 = mockCombinatorError(firstRouteRequest);
            var ignored2 = mockLogisticsPoint(2002, PartnerType.SORTING_CENTER, 1002);
            var ignored3 = mockCombinatorRoute(successRouteRequest, successRouteResponse)
        ) {
            execute(SORTING_CENTER_SEGMENT_ID);
        }
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=WARN\t\
                    format=plain\t\
                    code=GET_RETURN_ROUTE_ERROR\t\
                    """
            )
            .contains("Using backup sorting center 2002 for segment 2");
        featureProperties.getScDuplicatePartnerMap().remove(404L);
    }

    @Test
    @DisplayName("Не более одного похода в комбинатор от каждого виртуального СЦ")
    void doNotVisitSameSortingCenterTwice() {
        long firstScLogisticPointId = 1002;
        long firstScPartnerId = 2002;
        long secondScLogisticPointId = 1020;
        long secondScPartnerId = 2020;

        ReturnRouteRequest firstRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestRoutePoint(firstScPartnerId, firstScLogisticPointId))
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();

        ReturnRouteRequest secondRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestRoutePoint(secondScPartnerId, secondScLogisticPointId))
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();

        softly.assertThatThrownBy(() -> {
            try (
                var ignored1 = mockCombinatorError(firstRouteRequest);
                var ignored2 = mockLogisticsPoint(
                    firstScPartnerId,
                    PartnerType.SORTING_CENTER,
                    firstScLogisticPointId
                );
                var ignored3 = mockCombinatorError(secondRouteRequest);
                var ignored4 = mockLogisticsPoint(
                    secondScPartnerId,
                    PartnerType.SORTING_CENTER,
                    secondScLogisticPointId
                );
                var ignored5 = mockPartner(SHOP_PARTNER_ID, PartnerStatus.ACTIVE)
            ) {
                processor.execute(
                    ReturnSegmentGetRoutePayload.builder()
                        .returnSegmentId(SORTING_CENTER_SEGMENT_ID)
                        .sortingCenterPartnerIds(List.of(firstScPartnerId, secondScPartnerId))
                        .useStartTime(true)
                        .build()
                );
            }
        });

        verify(combinatorImplBase).getReturnRoute(eq(firstRouteRequest), any());
        verify(combinatorImplBase).getReturnRoute(eq(secondRouteRequest), any());
    }

    @Test
    @DisplayName("Ошибка получения машрута: фулфилмент")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/destination_fulfilment.xml",
        type = DatabaseOperation.UPDATE
    )
    void changeDestinationPartner_Fulfilment() throws Exception {
        ReturnRouteRequest request = sortingCenterFulfillmentRequest().setRearrFactors(NEW_GRAPH_FLAG).build();
        try (
            var ignored1 = mockCombinatorError(request);
            var ignored2 = mockGetOrder(createOrderReturnToDropoff())
        ) {
            assertCombinatorError();
        }
    }

    @ParameterizedTest
    @EnumSource(value = PartnerStatus.class, mode = EnumSource.Mode.INCLUDE, names = {"ACTIVE", "TESTING"})
    @DisplayName("Ошибка получения машрута: статус партнёра не позволяет менять точку назначения")
    void changeDestinationPartner_PartnerStatusNoNeedChangeDestination(PartnerStatus status) throws Exception {
        try (
            var ignored1 = mockCombinatorErrorFromScToShop();
            var ignored2 = mockGetOrder(createOrderReturnToDropoff());
            var ignored3 = mockPartner(SHOP_PARTNER_ID, status);
            var ignored4 = mockLogisticSegment(
                null,
                SHOP_PARTNER_ID,
                LogisticSegmentType.WAREHOUSE,
                1L,
                null
            );
            var ignored5 = mockLogisticSegment(
                1L,
                null,
                LogisticSegmentType.MOVEMENT,
                null,
                1006360L
            )
        ) {
            assertCombinatorError();
        }
    }

    @Test
    @DisplayName("Ошибка получения машрута: не найден заказ")
    void changeDestinationPartner_OrderNotFound() throws Exception {
        try (
            var ignored1 = mockCombinatorErrorFromScToShop();
            var ignored2 = mockGetOrder(null)
        ) {
            assertCombinatorError();
        }
    }

    @Test
    @DisplayName("Ошибка получения машрута: не найдено ни одного активного сегмента склада партнера")
    void changeDestinationPartner_NoActiveWarehouseSegments() throws Exception {
        try (
            var ignored1 = mockCombinatorErrorFromScToShop();
            var ignored2 = mockGetOrder(createOrderReturnToDropoff());
            var ignored3 = mockPartner(SHOP_PARTNER_ID, PartnerStatus.ACTIVE);
            var ignored4 = mockLogisticSegment(
                null,
                SHOP_PARTNER_ID,
                LogisticSegmentType.WAREHOUSE,
                List.of(),
                1L,
                null,
                true
            );
        ) {
            assertCombinatorError();
        }
    }

    @Test
    @DisplayName("Ошибка получения машрута: мерч перешел на экспресс схему")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/first_sc_route_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeDestinationPartner_Express() throws Exception {
        try (
            var ignored1 = mockCombinatorErrorFromScToShop();
            var ignored2 = mockPartner(SHOP_PARTNER_ID, PartnerStatus.ACTIVE);
            var ignored3 = mockGetOrder(createOrderReturnToSc(SC_2_PARTNER_ID));
            var ignored4 = mockLogisticSegment(
                null,
                SHOP_PARTNER_ID,
                LogisticSegmentType.WAREHOUSE,
                1L,
                null
            );
            var ignored5 = mockLogisticSegment(
                1L,
                null,
                LogisticSegmentType.MOVEMENT,
                List.of(ServiceCodeName.CALL_COURIER),
                null,
                1006360L,
                false
            );
            var ignored6 = mockCombinatorSuccessFromScToPartner(SC_2_PARTNER_ID, null)
        ) {
            execute(SORTING_CENTER_SEGMENT_ID);
        }
    }

    @Test
    @DisplayName("Ошибка получения машрута: мерч сменил точку сдачи с дропоффа")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/second_sc_create_in_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeDestinationPartner_ReturnPointForPartnerChanged() throws Exception {
        ReturnRouteRequest returnRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestFromPoint(DROPOFF_PARTNER_ID, DROPOFF_LOGISTIC_POINT_ID))
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();

        try (
            var ignored1 = mockCombinatorError(returnRouteRequest);
            var ignored2 = mockPartner(SHOP_PARTNER_ID, PartnerStatus.ACTIVE);
            var ignored3 = mockGetOrder(createOrderReturnToDropoff());
            var ignored4 = mockLogisticSegment(
                null,
                SHOP_PARTNER_ID,
                LogisticSegmentType.WAREHOUSE,
                1L,
                null
            );
            var ignored5 = mockLogisticSegment(
                1L,
                null,
                LogisticSegmentType.MOVEMENT,
                null,
                1006360L
            );
            var ignored6 = mockReturnPointForPartner(SHOP_PARTNER_ID, SC_2_WAREHOUSE_ID, SC_2_PARTNER_ID)
        ) {
            execute(DROPOFF_SEGMENT_ID);
        }
    }

    @Test
    @DisplayName("Ошибка получения машрута: возврат на дропофф")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/first_sc_route_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeDestinationPartner_DropoffHasWaybillSegment() throws Exception {
        try (
            var ignored1 = mockCombinatorErrorFromScToShop();
            var ignored2 = mockPartner(SHOP_PARTNER_ID, PartnerStatus.INACTIVE);
            var ignored3 = mockGetOrder(createOrderReturnToDropoff());
            var ignored4 = mockCombinatorSuccessFromScToPartner(DROPOFF_PARTNER_ID, DROPOFF_LOGISTIC_POINT_ID)
        ) {
            execute(SORTING_CENTER_SEGMENT_ID);
        }
    }

    @Test
    @DisplayName("Ошибка получения машрута: возврат на СЦ")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/first_sc_route_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeDestinationPartner_NotDropoffHasWaybillSegment() throws Exception {
        try (
            var ignored1 = mockCombinatorErrorFromScToShop();
            var ignored2 = mockPartner(SHOP_PARTNER_ID, PartnerStatus.INACTIVE);
            var ignored3 = mockGetOrder(createOrderReturnToSc(SC_2_PARTNER_ID));
            var ignored4 = mockCombinatorSuccessFromScToPartner(SC_2_PARTNER_ID, null)
        ) {
            execute(SORTING_CENTER_SEGMENT_ID);
        }
    }

    @Test
    @DisplayName("Ошибка получения машрута: возврат на СЦ, точка выдачи совпадает с этим СЦ")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/first_sc_create_in_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeDestinationPartner_SortingCenterIdEqualsOrderReturnSortingCenterId() throws Exception {
        try (
            var ignored1 = mockCombinatorErrorFromScToShop();
            var ignored2 = mockPartner(SHOP_PARTNER_ID, PartnerStatus.INACTIVE);
            var ignored3 = mockGetOrder(createOrderReturnToSc(SORTING_CENTER_PARTNER_ID));
        ) {
            execute(SORTING_CENTER_SEGMENT_ID);
        }
    }

    @Test
    @DisplayName("Логточка контрольной точки совпадает с логточкой возвратного сегмента")
    @DatabaseSetup("/database/tasks/return-segment/get-route/before/setup_without_shop.xml")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/control_point.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/shop_segment_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void controlPointEqualsToReturnSegment() throws Exception {
        when(featureProperties.isEnableControlPointUseInGetRoute()).thenReturn(true);

        try (
            var ignored1 = mockPartner(SHOP_PARTNER_ID, PartnerStatus.ACTIVE);
            var ignored2 = mockLogisticsPoint(SHOP_PARTNER_ID, PartnerType.DROPSHIP, SHOP_WAREHOUSE_ID)
        ) {
            execute(DROPOFF_SEGMENT_ID);
        }
    }

    @Test
    @DisplayName("Используем контрольную точку в качестве точки назначения")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/control_point.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/first_sc_route_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void routeToControlPoint() throws Exception {
        when(featureProperties.isEnableControlPointUseInGetRoute()).thenReturn(true);
        ReturnRouteRequest returnRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(requestSortingCenterPoint())
            .setTo(CombinatorRouteFactory.requestFromPoint(DROPOFF_PARTNER_ID, DROPOFF_LOGISTIC_POINT_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();
        ReturnRouteResponse returnRouteResponse = CombinatorRouteFactory.routeResponse(
            routeSortingCenterPoint(),
            CombinatorRouteFactory.routeScPoint(DROPOFF_SEGMENT_ID, "Second SC partner name")
        );

        try (var ignored = mockCombinatorRoute(returnRouteRequest, returnRouteResponse)) {
            execute(SORTING_CENTER_SEGMENT_ID);
            assertUseGraphMeta(false);
        }
    }

    @Test
    @DisplayName("Не используем контрольную точку в качестве точки назначения, когда флаг выключен")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/control_point.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/get-route/after/first_sc_route_saved.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void routeToControlPointFlagOff() throws Exception {
        ReturnRouteRequest returnRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(requestSortingCenterPoint())
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP)
            .build();
        ReturnRouteResponse returnRouteResponse = CombinatorRouteFactory.routeResponse(
            routeSortingCenterPoint(),
            CombinatorRouteFactory.routeScPoint(DROPOFF_SEGMENT_ID, "Second SC partner name"),
            CombinatorRouteFactory.routeFfPoint(SHOP_SEGMENT_ID, "Shop partner name")
        );

        try (var ignored = mockCombinatorRoute(returnRouteRequest, returnRouteResponse)) {
            featureProperties.setAllowUtilizationFlowReturnIds(Set.of());
            execute(SORTING_CENTER_SEGMENT_ID);
            featureProperties.setAllowUtilizationFlowReturnIds(Set.of(1L));
            assertUseGraphMeta(false);
        }
    }

    @Test
    @DisplayName("Не отправляем startTime комбинатору если его нет в payload")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/cancellation.xml",
        type = DatabaseOperation.REFRESH
    )
    void noStartTimeForFirstSegmentOfCancellationReturn() throws Exception {
        ReturnRouteRequest returnRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestFromPoint(2001L, 1001L))
            .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
            .build();
        ReturnRouteResponse returnRouteResponse = CombinatorRouteFactory.routeResponse(
            CombinatorRouteFactory.routeScPoint(PICKUP_SEGMENT_ID, "First SC partner name"),
            CombinatorRouteFactory.routeFfPoint(SORTING_CENTER_SEGMENT_ID, "Second SC partner name"),
            CombinatorRouteFactory.routeScPoint(DROPOFF_SEGMENT_ID, "Third SC partner name"),
            CombinatorRouteFactory.routeFfPoint(SHOP_SEGMENT_ID, "Shop partner name")
        );

        try (var ignored = mockCombinatorRoute(returnRouteRequest, returnRouteResponse)) {
            execute(PICKUP_SEGMENT_ID, false);
            assertUseGraphMeta(false);
        }
    }

    @Test
    @DisplayName("Используем возвратный граф по настройкам")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/destination_fulfilment.xml",
        type = DatabaseOperation.UPDATE
    )
    void newGraphWithoutMeta() throws Exception {
        ReturnRouteRequest request = sortingCenterFulfillmentRequest()
            .setRearrFactors(NEW_GRAPH_FLAG)
            .build();

        try (var ignored = mockCombinatorRoute(request, newSortingCenterFulfillmentResponse())) {
            execute(SORTING_CENTER_SEGMENT_ID);
            assertUseGraphMeta(true);
        }
    }

    @Test
    @DisplayName("Используем возвратный граф по глобальным настройкам")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/destination_fulfilment.xml",
        type = DatabaseOperation.UPDATE
    )
    void newGraphWithoutMetaByGlobalProperty() throws Exception {
        when(featureProperties.getUseBackwardGraphRoutes()).thenReturn(null);
        when(featureProperties.isAlwaysUseBackwardGraphRoutes()).thenReturn(true);

        ReturnRouteRequest request = sortingCenterFulfillmentRequest()
            .setRearrFactors(NEW_GRAPH_FLAG)
            .build();

        try (var ignored = mockCombinatorRoute(request, newSortingCenterFulfillmentResponse())) {
            execute(SORTING_CENTER_SEGMENT_ID);
            assertUseGraphMeta(true);
        }
    }

    @Test
    @DisplayName("Используем возвратный граф по настройкам без указания партнёра отправления")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/destination_second_fulfilment.xml",
        type = DatabaseOperation.UPDATE
    )
    void newGraphWithoutFromPartner() throws Exception {
        ReturnRouteRequest request = sortingCenterFulfillmentRequest()
            .setTo(CombinatorRouteFactory.requestToPoint(SECOND_FF_PARTNER_ID))
            .setRearrFactors(NEW_GRAPH_FLAG)
            .build();

        try (var ignored = mockCombinatorRoute(request, newSortingCenterFulfillmentResponse())) {
            execute(SORTING_CENTER_SEGMENT_ID);
            assertUseGraphMeta(true);
        }
    }

    @Test
    @DisplayName("Используем возвратный граф по существующей мете без настроек")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/destination_fulfilment.xml",
        type = DatabaseOperation.UPDATE
    )
    void newGraphWithMeta() throws Exception {
        when(featureProperties.getUseBackwardGraphRoutes()).thenReturn(null);
        entityMetaService.save(
            new DetachedTypedEntity(EntityType.RETURN, RETURN_ID),
            ReturnEntityUseBackwardGraphMeta.builder().enabled(true).build()
        );

        ReturnRouteRequest request = sortingCenterFulfillmentRequest()
            .setRearrFactors(NEW_GRAPH_FLAG)
            .build();

        try (var ignored = mockCombinatorRoute(request, newSortingCenterFulfillmentResponse())) {
            execute(SORTING_CENTER_SEGMENT_ID);
        }
    }

    @Test
    @DisplayName("Используем старый граф по существующей мете несмотря на настройки")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/get-route/before/destination_fulfilment.xml",
        type = DatabaseOperation.UPDATE
    )
    void oldGraphWithMeta() throws Exception {
        entityMetaService.save(
            new DetachedTypedEntity(EntityType.RETURN, RETURN_ID),
            ReturnEntityUseBackwardGraphMeta.builder().enabled(false).build()
        );

        ReturnRouteRequest request = sortingCenterFulfillmentRequest().build();
        ReturnRouteResponse response = CombinatorRouteFactory.routeResponse(
            CombinatorRouteFactory.routeScPoint(SORTING_CENTER_SEGMENT_ID, "SC partner name"),
            CombinatorRouteFactory.routeFfPoint(FF_SEGMENT_ID, "FF partner name")
        );
        try (var ignored = mockCombinatorRoute(request, response)) {
            execute(SORTING_CENTER_SEGMENT_ID);
        }
    }

    @Nonnull
    private OrderDto createOrderReturnToDropoff() {
        return new OrderDto()
            .setReturnSortingCenterId(DROPOFF_PARTNER_ID)
            .setWaybill(List.of(
                createSegment(
                    SegmentType.FULFILLMENT,
                    ru.yandex.market.logistics.lom.model.enums.PartnerType.DROPSHIP,
                    SHOP_PARTNER_ID,
                    SHOP_WAREHOUSE_ID
                ),
                createSegment(
                    SegmentType.SORTING_CENTER,
                    ru.yandex.market.logistics.lom.model.enums.PartnerType.DELIVERY,
                    DROPOFF_PARTNER_ID,
                    DROPOFF_LOGISTIC_POINT_ID
                )
            ));
    }

    @Nonnull
    private OrderDto createOrderReturnToSc(long returnSortingCenterId) {
        return new OrderDto()
            .setReturnSortingCenterId(returnSortingCenterId)
            .setWaybill(List.of(
                createSegment(
                    SegmentType.FULFILLMENT,
                    ru.yandex.market.logistics.lom.model.enums.PartnerType.DROPSHIP,
                    SHOP_PARTNER_ID,
                    SHOP_WAREHOUSE_ID
                ),
                createSegment(
                    SegmentType.SORTING_CENTER,
                    ru.yandex.market.logistics.lom.model.enums.PartnerType.SORTING_CENTER,
                    returnSortingCenterId,
                    SC_2_WAREHOUSE_ID
                )
            ));
    }

    @Nonnull
    WaybillSegmentDto createSegment(
        SegmentType segmentType,
        ru.yandex.market.logistics.lom.model.enums.PartnerType partnerType,
        long partnerId,
        long warehouseId
    ) {
        return WaybillSegmentDto.builder()
            .segmentType(segmentType)
            .partnerType(partnerType)
            .partnerId(partnerId)
            .warehouseLocation(LocationDto.builder().warehouseId(warehouseId).build())
            .build();
    }

    @Nonnull
    private AutoCloseable mockCombinatorErrorFromScToShop() {
        return mockCombinatorError(
            ReturnRouteRequest.newBuilder()
                .setFrom(requestSortingCenterPoint())
                .setTo(CombinatorRouteFactory.requestToPoint(SHOP_PARTNER_ID))
                .setStartTime(NOW_TIMESTAMP)
                .build()
        );
    }

    @Nonnull
    private AutoCloseable mockGetOrder(@Nullable OrderDto order) {
        OrderSearchFilter filter = OrderSearchFilter.builder()
            .barcodes(Set.of(ORDER_EXTERNAL_ID))
            .build();

        when(lomClient.searchOrders(filter, Pageable.unpaged()))
            .thenReturn(
                PageResult.of(
                    order == null ? Collections.emptyList() : List.of(order),
                    order == null ? 0 : 1,
                    0,
                    1
                )
            );
        return () -> verify(lomClient).searchOrders(filter, Pageable.unpaged());
    }

    @Nonnull
    private AutoCloseable mockPartner(long partnerId, PartnerStatus status) {
        when(lmsClient.getPartner(partnerId)).thenReturn(
            Optional.of(
                PartnerResponse.newBuilder()
                    .status(status)
                    .id(partnerId)
                    .name("partnerName-" + partnerId)
                    .build()
            )
        );
        return () -> verify(lmsClient).getPartner(partnerId);
    }

    @Nonnull
    private AutoCloseable mockLogisticsPoint(long partnerId, PartnerType partnerType, long logisticsPointId) {
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(partnerId))
            .partnerTypes(Set.of(partnerType))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        when(lmsClient.getLogisticsPoints(filter))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder()
                    .id(logisticsPointId)
                    .partnerId(partnerId)
                    .name("logisticPointName-" + logisticsPointId)
                    .build()
            ));
        return () -> verify(lmsClient).getLogisticsPoints(filter);
    }

    private AutoCloseable mockLogisticSegment(
        @Nullable Long segmentId,
        @Nullable Long partnerId,
        LogisticSegmentType type,
        @Nullable Long nextSegmentId,
        @Nullable Long returnPartnerId
    ) {
        return mockLogisticSegment(
            segmentId,
            partnerId,
            type,
            List.of(),
            nextSegmentId,
            returnPartnerId,
            false
        );
    }

    private AutoCloseable mockLogisticSegment(
        @Nullable Long segmentId,
        @Nullable Long partnerId,
        LogisticSegmentType type,
        List<ServiceCodeName> services,
        @Nullable Long nextSegmentId,
        @Nullable Long returnPartnerId,
        boolean emptyResult
    ) {
        LogisticSegmentFilter filter = LogisticSegmentFilter.builder()
            .setIds(Optional.ofNullable(segmentId).map(Set::of).orElse(null))
            .setPartnerIds(Optional.ofNullable(partnerId).map(Set::of).orElse(null))
            .setTypes(Set.of(type))
            .setServiceStatuses(Set.of(ActivityStatus.ACTIVE))
            .build();
        when(lmsClient.searchLogisticSegments(filter))
            .thenReturn(emptyResult ? List.of() : List.of(
                new LogisticSegmentDto()
                    .setNextSegmentIds(Optional.ofNullable(nextSegmentId).map(List::of).orElseGet(List::of))
                    .setPartnerId(returnPartnerId)
                    .setServices(
                        services.stream()
                            .map(
                                serviceCodeName -> LogisticSegmentServiceDto.builder()
                                    .setCode(serviceCodeName)
                                    .build()
                            )
                            .toList()
                    )
            ));
        return () -> verify(lmsClient).searchLogisticSegments(filter);
    }

    @Nonnull
    private AutoCloseable mockReturnPointForPartner(
        Long shopPartnerId,
        Long returnLogisticsPointId,
        Long returnPartnerId
    ) {
        when(lmsClient.getReturnPointForPartner(shopPartnerId))
            .thenReturn(Optional.of(new ReturnPointInfoResponse(
                returnLogisticsPointId,
                returnPartnerId,
                false,
                shopPartnerId,
                "shopName"
            )));
        return () -> verify(lmsClient).getReturnPointForPartner(shopPartnerId);
    }

    @Nonnull
    private AutoCloseable mockCombinatorSuccessFromScToPartner(long partnerId, @Nullable Long logisticPointId) {
        ReturnRoutePoint.Builder toPointBuilder = ReturnRoutePoint.newBuilder().setPartnerId(partnerId);

        if (logisticPointId != null) {
            toPointBuilder.setLogisticPointId(logisticPointId);
        }

        ReturnRouteRequest request = ReturnRouteRequest.newBuilder()
            .setFrom(requestSortingCenterPoint())
            .setTo(toPointBuilder.build())
            .setStartTime(NOW_TIMESTAMP)
            .build();

        doAnswer(invocation -> {
            StreamObserver<ReturnRouteResponse> returnRouteResponseStreamObserver = invocation
                .getArgument(1);

            returnRouteResponseStreamObserver.onNext(ReturnRouteResponse.newBuilder().build());
            returnRouteResponseStreamObserver.onCompleted();
            return null;
        }).when(combinatorImplBase).getReturnRoute(eq(request), any());

        return () -> verify(combinatorImplBase).getReturnRoute(eq(request), any());
    }

    @Nonnull
    private AutoCloseable mockCombinatorRoute(ReturnRouteRequest request, ReturnRouteResponse response) {
        doAnswer(invocation -> {
            StreamObserver<ReturnRouteResponse> returnRouteResponseStreamObserver = invocation
                .getArgument(1);

            returnRouteResponseStreamObserver.onNext(response);
            returnRouteResponseStreamObserver.onCompleted();
            return null;
        }).when(combinatorImplBase).getReturnRoute(eq(request), any());
        return () -> {
            verifyCombinatorRequest(request).close();
            verifyReturnRouteSavedInYdb(response);
        };
    }

    @Nonnull
    private AutoCloseable mockCombinatorError(ReturnRouteRequest request) {
        doThrow(new RuntimeException("error message"))
            .when(combinatorImplBase).getReturnRoute(eq(request), any());
        return verifyCombinatorRequest(request);
    }

    private void assertCombinatorError() {
        softly.assertThatThrownBy(() -> execute(SORTING_CENTER_SEGMENT_ID))
            .isInstanceOf(RuntimeException.class)
            .getCause()
            .isInstanceOf(NoRouteException.class)
            .getCause()
            .hasMessage("UNKNOWN");
    }

    @Nonnull
    private AutoCloseable verifyCombinatorRequest(ReturnRouteRequest request) {
        return () -> verify(combinatorImplBase).getReturnRoute(eq(request), any());
    }

    @Nonnull
    private ReturnRouteResponse.Point routeSortingCenterPoint() {
        return CombinatorRouteFactory.buildPoint(
            "SC partner name",
            PartnerType.SORTING_CENTER,
            LogisticSegmentType.WAREHOUSE,
            SORTING_CENTER_SEGMENT_ID
        );
    }

    @Nonnull
    private ReturnRoutePoint.Builder requestSortingCenterPoint() {
        return CombinatorRouteFactory.requestRoutePoint(SORTING_CENTER_PARTNER_ID, 1002);
    }

    @Nonnull
    private Builder sortingCenterFulfillmentRequest() {
        return ReturnRouteRequest.newBuilder()
            .setFrom(requestSortingCenterPoint())
            .setTo(CombinatorRouteFactory.requestToPoint(FF_PARTNER_ID))
            .setStartTime(NOW_TIMESTAMP);
    }

    @Nonnull
    private ReturnRouteResponse newSortingCenterFulfillmentResponse() {
        return CombinatorRouteFactory.routeResponse(
            CombinatorRouteFactory.buildPoint(
                "SC partner name",
                PartnerType.SORTING_CENTER,
                LogisticSegmentType.BACKWARD_WAREHOUSE,
                SORTING_CENTER_SEGMENT_ID
            ),
            CombinatorRouteFactory.buildPoint(
                "FF partner name",
                PartnerType.FULFILLMENT,
                LogisticSegmentType.BACKWARD_WAREHOUSE,
                FF_SEGMENT_ID
            )
        );
    }

    @SneakyThrows
    private void verifyReturnRouteSavedInYdb(ReturnRouteResponse returnRouteResponse) {
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Save return route with uuid " + uuidGenerator.get());
        List<ReturnRouteHistory> routesInYdb = YdbTestRepositoryUtils.findAll(
            ydbTemplate,
            routeHistoryTableDescription,
            returnRouteHistoryConverter.toListConverter()
        );
        JsonNode expectedJsonRoute = objectMapper.readTree(ProtobufMessagesUtils.getJson(returnRouteResponse));
        softly.assertThat(routesInYdb).containsExactly(
            new ReturnRouteHistory(
                uuidGenerator.get().toString(),
                expectedJsonRoute,
                NOW_TIME
            )
        );
    }

    private void assertUseGraphMeta(boolean expected) {
        softly.assertThat(getEntityMetaRecord(
                RETURN_1_HASH,
                "RETURN",
                RETURN_ID,
                "return-entity-use-backward-graph"
            ))
            .map(EntityMetaTableDescription.EntityMetaRecord::value)
            .map(v -> readValue(v, ReturnEntityUseBackwardGraphMeta.class))
            .map(ReturnEntityUseBackwardGraphMeta::isEnabled)
            .hasValue(expected);
    }

    private void execute(long segmentId) {
        execute(segmentId, true);
    }

    private void execute(long segmentId, boolean useStartTime) {
        processor.execute(
            ReturnSegmentGetRoutePayload.builder()
                .returnSegmentId(segmentId)
                .useStartTime(useStartTime)
                .build()
        );
    }
}
