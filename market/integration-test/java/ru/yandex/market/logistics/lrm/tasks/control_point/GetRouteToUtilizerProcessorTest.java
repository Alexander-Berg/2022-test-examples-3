package ru.yandex.market.logistics.lrm.tasks.control_point;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import yandex.market.combinator.v0.CombinatorGrpc;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.config.locals.UuidGenerator;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.queue.payload.GetRouteToUtilizerPayload;
import ru.yandex.market.logistics.lrm.queue.processor.GetRouteToUtilizerProcessor;
import ru.yandex.market.logistics.lrm.repository.ydb.converter.ReturnRouteHistoryConverter;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.repository.ydb.description.ReturnRouteHistoryTableDescription;
import ru.yandex.market.logistics.lrm.service.combinator.NoRouteException;
import ru.yandex.market.logistics.lrm.service.meta.DetachedTypedEntity;
import ru.yandex.market.logistics.lrm.service.meta.EntityMetaService;
import ru.yandex.market.logistics.lrm.service.meta.model.ReturnEntityUseBackwardGraphMeta;
import ru.yandex.market.logistics.lrm.utils.CombinatorRouteFactory;
import ru.yandex.market.logistics.lrm.utils.ProtobufMessagesUtils;
import ru.yandex.market.logistics.lrm.utils.YdbTestRepositoryUtils;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static yandex.market.combinator.v0.CombinatorOuterClass.ReturnRouteRequest;
import static yandex.market.combinator.v0.CombinatorOuterClass.ReturnRouteResponse;

@DisplayName("Получение возвратного маршрута в сторону утилайзера")
class GetRouteToUtilizerProcessorTest extends AbstractIntegrationYdbTest {

    private static final long DROPOFF_SEGMENT_ID = 3;
    private static final long UTILIZER_SEGMENT_ID = 4;

    private static final long DROPOFF_PARTNER_ID = 2003;
    private static final long RETURN_SC_PARTNER_ID = 500;
    private static final long UTILIZER_PARTNER_ID = 559;

    private static final long DROPOFF_LOGISTIC_POINT_ID = 1003;

    private static final Instant NOW_TIME = Instant.parse("2021-11-11T11:11:11.00Z");
    private static final Timestamp NOW_TIMESTAMP = Timestamp.newBuilder()
        .setSeconds(NOW_TIME.getEpochSecond())
        .setNanos(NOW_TIME.getNano())
        .build();

    @Autowired
    private GetRouteToUtilizerProcessor processor;

    @Autowired
    private CombinatorGrpc.CombinatorImplBase combinatorImplBase;

    @Autowired
    private ReturnRouteHistoryTableDescription routeHistoryTableDescription;

    @Autowired
    private EntityMetaTableDescription entityMetaTableDescription;

    @Autowired
    private ReturnRouteHistoryConverter returnRouteHistoryConverter;

    @Autowired
    private EntityMetaService entityMetaService;

    @Autowired
    private UuidGenerator uuidGenerator;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTableDescription, entityMetaTableDescription);
    }

    @BeforeEach
    void setUp() {
        // Если не валить этот вызов для неизвестных параметров, тест зависает, если есть ошибка в моках
        doAnswer(i -> {
            throw new RuntimeException("Unexpected request: " + i.getArgument(0));
        }).when(combinatorImplBase).getReturnRoute(any(), any());
        clock.setFixed(NOW_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Ошибка получения маршрута")
    @DatabaseSetup("/database/tasks/control_point/get-route-to-utilizer/before/success.xml")
    void errorGetRoute() throws Exception {
        ReturnRouteRequest returnRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestFromPoint(DROPOFF_PARTNER_ID, DROPOFF_LOGISTIC_POINT_ID))
            .setTo(CombinatorRouteFactory.requestToPoint(UTILIZER_PARTNER_ID))
            .build();

        try (var ignored = mockCombinatorError(returnRouteRequest)) {
            softly.assertThatThrownBy(() -> processor.execute(createPayload()))
                .isInstanceOf(RuntimeException.class)
                .getCause()
                .isInstanceOf(NoRouteException.class)
                .getCause()
                .hasMessage("UNKNOWN");
        }

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=ERROR\t\
                    format=json-exception\t\
                    code=GET_RETURN_ROUTE_ERROR\t\
                    """
            )
            .contains("payload={\\\"eventMessage\\\":\\\"Cannot get route from controlPoint to utilizer")
            .contains(
                """
                    request_id=test-request-id\t\
                    extra_keys=controlPointId\t\
                    extra_values=1
                    """
            );
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/database/tasks/control_point/get-route-to-utilizer/before/success.xml")
    @DatabaseSetup(
        value = "/database/tasks/control_point/get-route-to-utilizer/before/link_segment_to_history.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/control_point/get-route-to-utilizer/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() throws Exception {
        ReturnRouteRequest returnRouteRequest = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestFromPoint(DROPOFF_PARTNER_ID, DROPOFF_LOGISTIC_POINT_ID))
            .setTo(CombinatorRouteFactory.requestToPoint(UTILIZER_PARTNER_ID))
            .build();
        ReturnRouteResponse returnRouteResponse = CombinatorRouteFactory.routeResponse(
            CombinatorRouteFactory.routeScPoint(DROPOFF_SEGMENT_ID, "Second SC partner name"),
            CombinatorRouteFactory.routeScPoint(RETURN_SC_PARTNER_ID, "Return SC partner name"),
            CombinatorRouteFactory.routeFfPoint(UTILIZER_SEGMENT_ID, "Utilizer partner name")
        );

        try (var ignored = mockCombinatorRoute(returnRouteRequest, returnRouteResponse)) {
            processor.execute(createPayload());
        }
    }

    @Test
    @DisplayName("Успех, возвратный граф")
    @DatabaseSetup("/database/tasks/control_point/get-route-to-utilizer/before/success.xml")
    @DatabaseSetup(
        value = "/database/tasks/control_point/get-route-to-utilizer/before/link_segment_to_history.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/control_point/get-route-to-utilizer/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newGraph() throws Exception {
        entityMetaService.save(
            new DetachedTypedEntity(EntityType.RETURN, 1L),
            ReturnEntityUseBackwardGraphMeta.builder().enabled(true).build()
        );

        ReturnRouteRequest request = ReturnRouteRequest.newBuilder()
            .setFrom(CombinatorRouteFactory.requestFromPoint(DROPOFF_PARTNER_ID, DROPOFF_LOGISTIC_POINT_ID))
            .setTo(CombinatorRouteFactory.requestToPoint(UTILIZER_PARTNER_ID))
            .setRearrFactors("sbg_for_return_route=1")
            .build();
        ReturnRouteResponse response = CombinatorRouteFactory.routeResponse(
            CombinatorRouteFactory.buildPoint(
                "Dropoff partner name",
                PartnerType.DELIVERY,
                LogisticSegmentType.BACKWARD_WAREHOUSE,
                DROPOFF_SEGMENT_ID
            ),
            CombinatorRouteFactory.buildPoint(
                "Return SC partner name",
                PartnerType.SORTING_CENTER,
                LogisticSegmentType.BACKWARD_WAREHOUSE,
                RETURN_SC_PARTNER_ID
            ),
            CombinatorRouteFactory.buildPoint(
                "Utilizer partner name",
                PartnerType.SCRAP_DISPOSER,
                LogisticSegmentType.BACKWARD_WAREHOUSE,
                UTILIZER_SEGMENT_ID
            )
        );

        try (var ignored = mockCombinatorRoute(request, response)) {
            processor.execute(createPayload());
        }
    }

    @Test
    @DisplayName("Неправильное число сегментов в статусе TRANSIT_PREPARED")
    @DatabaseSetup("/database/tasks/control_point/get-route-to-utilizer/before/invalid-number-of-segments.xml")
    @DatabaseSetup(
        value = "/database/tasks/control_point/get-route-to-utilizer/before/link_segment_to_history.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/database/tasks/control_point/get-route-to-utilizer/after/invalid-number-of-segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void invalidNumberOfSegments() {
        softly.assertThatThrownBy(() ->
                processor.execute(createPayload())
            )
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Number of boxes (1) is not equal to number of prepared segments (0) on control point 1");
    }

    @Nonnull
    private AutoCloseable mockCombinatorError(ReturnRouteRequest request) {
        doThrow(new RuntimeException("error message"))
            .when(combinatorImplBase).getReturnRoute(eq(request), any());
        return verifyCombinatorRequest(request);
    }

    @Nonnull
    private GetRouteToUtilizerPayload createPayload() {
        return GetRouteToUtilizerPayload.builder()
            .controlPointId(1L)
            .build();
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
    private AutoCloseable verifyCombinatorRequest(ReturnRouteRequest request) {
        return () -> verify(combinatorImplBase).getReturnRoute(eq(request), any());
    }

    @SneakyThrows
    private void verifyReturnRouteSavedInYdb(ReturnRouteResponse returnRouteResponse) {
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains("Return route towards utilizer saved with uuid " + uuidGenerator.get());
        List<ReturnRouteHistoryTableDescription.ReturnRouteHistory> routesInYdb = YdbTestRepositoryUtils.findAll(
            ydbTemplate,
            routeHistoryTableDescription,
            returnRouteHistoryConverter.toListConverter()
        );
        JsonNode expectedJsonRoute = objectMapper.readTree(ProtobufMessagesUtils.getJson(returnRouteResponse));
        softly.assertThat(routesInYdb).containsExactly(
            new ReturnRouteHistoryTableDescription.ReturnRouteHistory(
                uuidGenerator.get().toString(),
                expectedJsonRoute,
                NOW_TIME
            )
        );
    }
}
