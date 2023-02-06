package ru.yandex.market.tpl.carrier.driver.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import javax.transaction.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.registry.RegistryRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.carrier.driver.api.model.task.LocationDto;
import ru.yandex.market.tpl.carrier.driver.api.model.task.TaskDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Sql("classpath:mockPartner/defaultDeliveryServices.sql")
@RequiredArgsConstructor(onConstructor_=@Autowired)
public class RoutePointControllerTest extends BaseDriverApiIntTest {

    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TestUserHelper testUserHelper;
    private final RunHelper runHelper;
    private final RunGenerator runGenerator;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final ObjectMapper objectMapper;

    private final UserShiftRepository userShiftRepository;
    private final RegistryRepository registryRepository;

    private UserShift userShift;
    private OrderWarehouse orderWarehouseFrom;

    private final Instant inboundArrivalTime = Instant.parse("2021-01-01T13:00:00.00Z");
    private final Instant deliveryIntervalTo = Instant.parse("2021-01-01T14:00:00.00Z");

    @BeforeEach
    void setUp() {
        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        var user = testUserHelper.findOrCreateUser(1L);
        var transport = testUserHelper.findOrCreateTransport();
        orderWarehouseFrom = orderWarehouseGenerator.generateWarehouse();

        var run = runGenerator.generate(RunGenerator.RunGenerateParam.builder()
                .runDate(LocalDate.now())
                .externalId("runId")
                .campaignId(company.getCampaignId())
                .deliveryServiceId(123L)
                .item(RunGenerator.RunItemGenerateParam.builder()
                        .movement(MovementCommand.Create.builder()
                                .orderWarehouse(orderWarehouseFrom)
                                .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse())
                                .inboundArrivalTime(inboundArrivalTime)
                                .deliveryIntervalTo(deliveryIntervalTo)
                                .build())
                        .orderNumber(1)
                        .build())
                .build());

        userShift = runHelper.assignUserAndTransport(run, user, transport);
        testUserHelper.openShift(user, userShift.getId());
    }

    @SneakyThrows
    @Test
    @Transactional
    void shouldArriveAtRoutePointCorrectly() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.CREATE_REGISTRY_ON_POINTS, true);

        RoutePoint routePoint = userShift.streamRoutePoints()
                .findFirst()
                .orElseThrow();

        var response = mockMvc.perform(
                post("/api/route-points/{id}/arrive", routePoint.getId())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(LocationDto.builder()
                        .latitude(orderWarehouseFrom.getAddress().getLatitude())
                        .longitude(orderWarehouseFrom.getAddress().getLongitude())
                        .wrongLocationComment(null)
                        .build()
                ))
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var routePointDto = objectMapper.readValue(response, RoutePointDto.class);
        var registry = registryRepository.findByTaskId(
                routePointDto.getTasks().stream()
                        .findFirst()
                        .map(TaskDto::getId)
                        .orElseThrow()
        );
        Assertions.assertThat(registry.get().getSortables()).isEmpty();

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());

        Assertions.assertThat(userShift.getCurrentRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        Assertions.assertThat(userShift.getLastRoutePoint().getExpectedDateTime()).isEqualTo(inboundArrivalTime);
    }

    @SneakyThrows
    @Test
    @Transactional
    void shouldArriveAtRoutePointCorrectlyWithWrongCoordinate() {
        RoutePoint routePoint = userShift.streamRoutePoints()
                .findFirst()
                .orElseThrow();

        mockMvc.perform(
                        post("/api/route-points/{id}/arrive", routePoint.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(objectMapper.writeValueAsString(LocationDto.builder()
                                        .latitude(orderWarehouseFrom.getAddress().getLatitude().add(BigDecimal.TEN))
                                        .longitude(orderWarehouseFrom.getAddress().getLongitude())
                                        .wrongLocationComment("wrong comment")
                                        .build()
                                ))
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                )
                .andExpect(status().isOk());


        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());

        Assertions.assertThat(userShift.getCurrentRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        Assertions.assertThat(userShift.getLastRoutePoint().getExpectedDateTime()).isEqualTo(inboundArrivalTime);
    }

    @SneakyThrows
    @Test
    @Transactional
    void shouldNotBeAbleToArriveWithWrongCommentIfItIsEnabled() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.USER_CANNOT_SKIP_DISTANCE_CHECK_WITH_WRONG_COMMENT_ENABLED, true);

        RoutePoint routePoint = userShift.streamRoutePoints()
                .findFirst()
                .orElseThrow();

        mockMvc.perform(
                        post("/api/route-points/{id}/arrive", routePoint.getId())
                                .contentType(MediaType.APPLICATION_JSON_UTF8)
                                .content(objectMapper.writeValueAsString(LocationDto.builder()
                                        .latitude(orderWarehouseFrom.getAddress().getLatitude().add(BigDecimal.TEN))
                                        .longitude(orderWarehouseFrom.getAddress().getLongitude())
                                        .wrongLocationComment("Я на точке, мамой клянусь")
                                        .build()
                                ))
                                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceToRoutePoint.nearby").value(false));


        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());

        Assertions.assertThat(userShift.getCurrentRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
    }

    @SneakyThrows
    @Test
    void shouldReturnQrCodePayload() {
        RoutePoint first = userShift.streamRoutePoints().findFirst().orElseThrow();

        Map<String, String> expected = Map.of(
                "type", "VEHICLE_NUM",
                "value", TestUserHelper.DEFAULT_TRANSPORT_NUMBER
        );
        mockMvc.perform(
                get("/api/route-points/{id}", first.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(jsonPath("$.tasks[0].qrCodePayload").exists())
                .andExpect(jsonPath("$.tasks[0].qrCodePayload").value(
                        new CustomTypeSafeMatcher<String>(objectMapper.writeValueAsString(expected)) {
                            @SneakyThrows
                            @Override
                            protected boolean matchesSafely(String item) {
                                Map<String, Object> jsonNode = objectMapper.readValue(item, new TypeReference<Map<String, String>>() {});

                                return expected.equals(jsonNode);
                            }
                        }
                ));
    }
}
