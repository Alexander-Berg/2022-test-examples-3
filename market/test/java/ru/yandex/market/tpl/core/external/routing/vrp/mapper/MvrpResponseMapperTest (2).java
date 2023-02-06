package ru.yandex.market.tpl.core.external.routing.vrp.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.util.TplObjectMappers;
import ru.yandex.market.tpl.core.domain.routing.api.RoutingRequestItemTestBuilder;
import ru.yandex.market.tpl.core.external.routing.RoutingCommonTestUtils;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResponseItem;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResponseRoutePoint;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResult;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultShift;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.external.routing.vrp.VrpTaskResult;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpResponse;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpResponseRoutes;
import ru.yandex.market.tpl.core.external.routing.vrp.model.RouteNodeNode;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.HardcodedVrpSettingsProvider;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.VrpSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettings;
import ru.yandex.market.tpl.core.external.routing.vrp.settings.depot.DepotSettingsProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author ungomma
 */
@SuppressWarnings("ConstantConditions")
class MvrpResponseMapperTest {

    public static final long USER_ID = 123L;
    private DepotSettingsProvider depotSettingsProvider = mock(DepotSettingsProvider.class);
    private final MvrpResponseMapper mapper = new MvrpResponseMapper(depotSettingsProvider);
    private final ObjectMapper objectMapper = TplObjectMappers.TPL_DB_OBJECT_MAPPER;

    private MvrpResponse response;
    private RoutingResult result;
    private Instant startOfDay;

    void setUp() throws Exception {
        when(
                depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                        any(), any())
        ).thenReturn(false);
        response = readResponse("/vrp/vrp_mvrp_response.json");
        assertThat(response).isNotNull();
        assertThat(response.getMetrics()).isNotNull();

        RoutingRequest dummyRequest = new RoutingApiDataHelper()
                .getRoutingRequest(USER_ID, LocalDate.parse("2019-11-19"), 0);

        prepareRoutingRequest(dummyRequest, response);

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);
        result = mapper.mapResult(new VrpTaskResult(dummyRequest, settings, response, null));
        startOfDay = dummyRequest.getRoutingDate().atStartOfDay(dummyRequest.getZoneId()).toInstant();
    }

    @Test
    void shouldMapArrivalTimes() throws Exception {
        setUp();
        assertThat(result.getShiftsByUserId()).hasSameSizeAs(response.getRoutes());

        for (var route : response.getRoutes()) {
            long userId = route.getVehicleId();
            RoutingResultShift userShift = result.getShiftsByUserId().get(userId);
            assertThat(userShift).isNotNull();

            Map<String, Instant> mappedTimes = StreamEx.of(userShift.getRoutePoints())
                    .mapToEntry(RoutingResponseRoutePoint::getItems, RoutingResponseRoutePoint::getExpectedArrivalTime)
                    .flatMapKeys(l -> l.stream().map(RoutingResponseItem::getTaskId))
                    .toMap();

            Map<String, Instant> responseTimes = StreamEx.of(route.getRoute())
                    .removeBy(rn -> rn.getNode().getType(), RouteNodeNode.TypeEnum.DEPOT)
                    .mapToEntry(rn -> rn.getNode().getLocation().getId(),
                            rn -> startOfDay.plusSeconds(rn.getArrivalTimeS()).plusSeconds(rn.getWaitingDurationS())
                    )
                    .toMap();

            assertThat(mappedTimes).hasSameSizeAs(responseTimes);
            assertThat(mappedTimes).containsAllEntriesOf(responseTimes);
        }

    }

    @Test
    void shouldNotLoseOrderIds() throws Exception {
        setUp();
        Set<Long> responseOrderIds = StreamEx.of(response.getRoutes())
                .flatCollection(MvrpResponseRoutes::getRoute)
                .removeBy(rn -> rn.getNode().getType(), RouteNodeNode.TypeEnum.DEPOT)
                // в качестве location#id может быть multiOrderId
                .map(rn -> rn.getNode().getLocation().getId())
                .map(Long::parseLong)
                .toSet();

        Set<Long> mappedOrderIds = StreamEx.ofValues(result.getShiftsByUserId())
                .flatCollection(RoutingResultShift::getRoutePoints)
                .flatCollection(RoutingResponseRoutePoint::getItems)
                .flatMap(i -> i.getSubTaskIds().stream())
                .toSet();

        assertThat(mappedOrderIds).hasSameElementsAs(responseOrderIds);
    }

    @Test
    void fullResultCheck() throws Exception {
        when(
                depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                        any(), any())
        ).thenReturn(false);

        MvrpResponse response = readResponse("/vrp/vrp_mvrp_response.json");

        RoutingRequest dummyRequest = readRequest("/vrp/vrp_mvrp_response_mapped_request.json");
        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);
        RoutingResult result = mapper.mapResult(new VrpTaskResult(dummyRequest, settings, response, null));

        RoutingResult correctResult = RoutingCommonTestUtils.mapFromResource("/vrp/vrp_mvrp_response_mapped.json",
                RoutingResult.class);

        RoutingResult resultSerialized = objectMapper.treeToValue(objectMapper.valueToTree(result),
                RoutingResult.class);

        assertThat(resultSerialized.getShiftsByUserId().get(148L).getRoutePoints())
                .containsExactlyElementsOf(correctResult.getShiftsByUserId().get(148L).getRoutePoints());

        assertThat(resultSerialized).isEqualToIgnoringGivenFields(correctResult,
                "requestId", "profileType", "droppedItems", "routingRequest");
    }

    /**
     * Точки 5, 7 идут не подряд https://courier.yandex.ru/mvrp-map#57989fd6-1cd02062-188a4f16-db7649ce .
     */
    @Test
    void shouldUseLocationGroupsWhenMapResult() throws Exception {
        setUp();
        MvrpResponse response = readResponse("/vrp/vrp_svrp_response_with_loc_groups.json");
        assertThat(response).isNotNull();

        RoutingRequest request = readRequest("/vrp/vrp_svrp_request_with_loc_groups.json");
        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.REROUTE);
        RoutingResult result = mapper.mapResult(new VrpTaskResult(request, settings, response, null));

        assertThat(result.getShiftsByUserId().get(USER_ID).getRoutePoints().stream()
                .filter(rp -> rp.getItems().stream()
                        .map(RoutingResponseItem::getTaskId)
                        .collect(Collectors.toSet())
                        .containsAll(Set.of("281013", "281014"))
                ).findFirst()).isPresent();
    }

    @Test
    void mergeLocationsWithDisabledGroupingByHouse() throws Exception {
        when(
                depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                        DepotSettings.MVRP_GROUP_LOCATIONS_BY_HOUSE_DISABLED, 50598L)
        ).thenReturn(true);
        when(
                depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                        DepotSettings.MVRP_GROUP_LOCATIONS_BY_BUILDING_ENABLED, 50598L)
        ).thenReturn(true);

        RoutingRequest request = readRequest("/vrp/merge-locations-by-house/vrp-request.json");
        MvrpResponse response = readResponse("/vrp/merge-locations-by-house/vrp-response.json");

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);
        RoutingResult result = mapper.mapResult(new VrpTaskResult(request, settings, response, null));

        RoutingResultShift shift = result.getShiftsByUserId().values().iterator().next();
        assertThat(shift.getRoutePoints().size()).isEqualTo(1);
    }

    @Test
    void mergeLocationsWithGroupingByHouse() throws Exception {
        when(
                depotSettingsProvider.findPropertyValueForSortingCenterOrDefault(
                        any(), any())
        ).thenReturn(false);
        var taskIdWithSeparateHouse = 54440L;

        RoutingRequest request = readRequest("/vrp/merge-locations-by-house/vrp-request.json");
        MvrpResponse response = readResponse("/vrp/merge-locations-by-house/vrp-response.json");

        VrpSettings settings = new HardcodedVrpSettingsProvider().getVrpSettings(RoutingProfileType.GROUP);
        RoutingResult result = mapper.mapResult(new VrpTaskResult(request, settings, response, null));

        RoutingResultShift shift = result.getShiftsByUserId().values().iterator().next();
        assertThat(shift.getRoutePoints().size()).isEqualTo(2);
        assertThat(
                shift.getRoutePoints().stream()
                        .filter(point ->
                                point.getItems().stream()
                                        .anyMatch(it -> it.getSubTaskIds().contains(taskIdWithSeparateHouse))
                        )
                        .allMatch(it -> it.getItems().size() == 1)
        ).isEqualTo(true);
    }

    private void prepareRoutingRequest(RoutingRequest routingRequest, MvrpResponse mvrpResponse) {
        List<String> taskIds = mvrpResponse.getRoutes().stream()
                .flatMap(it -> it.getRoute().stream())
                .filter(it -> it.getNode().getType() == RouteNodeNode.TypeEnum.LOCATION)
                .map(it -> it.getNode().getLocation().getId())
                .collect(Collectors.toList());

        HashSet<Object> items = new HashSet<>();
        items.addAll(routingRequest.getItems());

        routingRequest.getItems().addAll(
                taskIds.stream()
                        .map(it -> RoutingRequestItemTestBuilder.builder()
                                .taskId(it)
                                .type(RoutingRequestItemType.CLIENT)
                                .build().get()
                        ).collect(Collectors.toList())
        );
    }

    private MvrpResponse readResponse(String filename) throws Exception {
        return RoutingCommonTestUtils.mapFromResource(filename, MvrpResponse.class);
    }

    private RoutingRequest readRequest(String filename) throws Exception {
        return RoutingCommonTestUtils.mapFromResource(filename, RoutingRequest.class);
    }

}
