package ru.yandex.market.tsup.controller.front;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RouteDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RoutePointDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RoutePointPairDto;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tpl.common.data_provider.meta.FrontHttpRequestMeta;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.util.TransportDtoUtils;
import ru.yandex.market.tsup.util.UserDtoUtils;
import ru.yandex.mj.generated.client.carrier.api.TransportApiClient;
import ru.yandex.mj.generated.client.carrier.api.UserApiClient;
import ru.yandex.mj.generated.client.carrier.model.CompanyDto;
import ru.yandex.mj.generated.client.carrier.model.PageOfTransportDto;
import ru.yandex.mj.generated.client.carrier.model.PageOfUserDto;
import ru.yandex.mj.generated.client.carrier.model.TransportDto;
import ru.yandex.mj.generated.client.carrier.model.TransportSource;
import ru.yandex.mj.generated.client.carrier.model.UserDto;
import ru.yandex.mj.generated.client.carrier.model.UserSourceDto;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.tsup.util.TransportDtoUtils.transportDto;

@SuppressWarnings("LineLength")
public class PipelineControllerTest extends AbstractContextualTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestableClock clock;

    @Autowired
    private UserApiClient userApiClient;

    @Autowired
    private TransportApiClient transportApiClient;

    @Autowired
    private TransportManagerClient transportManagerClient;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-01-01T12:00:00.00Z"), ZoneId.of("Europe/Moscow"));
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/after/create_by_configs.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testPost() throws Exception {

        mockMvc.perform(post("/pipelines")
                .contentType(MediaType.APPLICATION_JSON)
                .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                .content(extractFileContent("fixture/pipeline/request/create_simple_pipe.json")))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonContent("fixture/pipeline/response/pipeline_id.json"));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/pipeline/before/finished_pipeline.xml",
        connection = "dbUnitDatabaseConnection"
    )
    void testPipelineInfo() throws Exception {
        mockMvc.perform(get("/pipelines/1")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/pipeline/response/info.json", true));

    }

    @Test
    @DatabaseSetup(
        value = "/repository/pipeline/before/finished_pipeline.xml",
        connection = "dbUnitDatabaseConnection"
    )
    void testPipelineResult() throws Exception {
        mockMvc.perform(get("/pipelines/1/result")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(IntegrationTestUtils.jsonContent("fixture/pipeline/response/result.json", true));

    }

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/after/create_quick_trip.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createQuickTrip() throws Exception {
        ExecuteCall<PageOfUserDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        PageOfUserDto result = UserDtoUtils.page();
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(result));

        Mockito.when(
            userApiClient.internalUsersGet(
                null,
                "+79771234567",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "id,DESC"
            )
        ).thenReturn(call);

        mockExistingTransport();

        mockMvc.perform(post("/pipelines")
                .contentType(MediaType.APPLICATION_JSON)
                .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                .content(extractFileContent("fixture/pipeline/request/create_quick_trip.json")))
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/after/create_quick_trip_return.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createQuickTripReturn() throws Exception {
        Mockito.when(transportManagerClient.searchRouteById(1L)).thenReturn(
            RouteDto.builder().pointPairs(
                List.of(
                    new RoutePointPairDto(
                        RoutePointDto.builder().partnerId(1L).build(),
                        RoutePointDto.builder().partnerId(3L).build()
                    ),
                    new RoutePointPairDto(
                        RoutePointDto.builder().partnerId(2L).build(),
                        RoutePointDto.builder().partnerId(3L).build()
                    )
                )).build()
        );
        Mockito.when(lmsClient.searchPartners(SearchPartnerFilter.builder().setIds(Set.of(1L, 2L, 3L)).build()))
            .thenReturn(List.of(
                PartnerResponse.newBuilder().id(1L).partnerType(PartnerType.SORTING_CENTER).build(),
                PartnerResponse.newBuilder().id(2L).partnerType(PartnerType.SORTING_CENTER).build(),
                PartnerResponse.newBuilder().id(3L).partnerType(PartnerType.FULFILLMENT).build()
            ));
        ExecuteCall<PageOfUserDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        PageOfUserDto result = UserDtoUtils.page();
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(result));

        Mockito.when(
            userApiClient.internalUsersGet(
                null,
                "+79771234567",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "id,DESC"
            )
        ).thenReturn(call);

        mockExistingTransport();

        mockMvc.perform(post("/pipelines")
                .contentType(MediaType.APPLICATION_JSON)
                .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                .content(extractFileContent("fixture/pipeline/request/create_quick_trip_return.json")))
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/after/create_quick_trip_with_merged_points_times.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createQuickTripWithMergedPointsTimes() throws Exception {
        ExecuteCall<PageOfUserDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        PageOfUserDto result = UserDtoUtils.page();
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(result));

        Mockito.when(
                userApiClient.internalUsersGet(
                        null,
                        "+79771234567",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "id,DESC"
                )
        ).thenReturn(call);

        mockExistingTransport();

        mockMvc.perform(post("/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                        .content(extractFileContent("fixture/pipeline/request/create_quick_trip_with_merged_points_times.json")))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/after/create_quick_trip_for_duty.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createQuickTripWithDuty() throws Exception {
        ExecuteCall<PageOfUserDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        PageOfUserDto result = UserDtoUtils.page();
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(result));

        Mockito.when(
            userApiClient.internalUsersGet(
                null,
                "+79771234567",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "id,DESC"
            )
        ).thenReturn(call);

        mockExistingTransport();

        mockMvc.perform(post("/pipelines")
                .contentType(MediaType.APPLICATION_JSON)
                .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                .content(extractFileContent("fixture/pipeline/request/create_quick_trip_for_duty.json")))
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/after/create_quick_trip_for_duty_with_merged_points_times.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createQuickTripWithDutyWithMergedPointsTimes() throws Exception {
        ExecuteCall<PageOfUserDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        PageOfUserDto result = UserDtoUtils.page();
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(result));

        Mockito.when(
                userApiClient.internalUsersGet(
                        null,
                        "+79771234567",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "id,DESC"
                )
        ).thenReturn(call);

        mockExistingTransport();

        mockMvc.perform(post("/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                        .content(extractFileContent("fixture/pipeline/request/create_quick_trip_for_duty_with_merged_points_times.json")))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/after/creation_failed.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateQuickTripCourierConstraintViolation() throws Exception {

        ExecuteCall<PageOfUserDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        PageOfUserDto result = UserDtoUtils.page(
            new UserDto()
                .id(1L)
                .name("Василий Васильев Васильевич")
                .firstName("Василий")
                .lastName("Васильев")
                .patronymic("Васильевич")
                .companies(List.of(new CompanyDto().id(1L).name("company")))
                .phone("+79771234567")
                .source(UserSourceDto.CARRIER)
        );
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(result));

        Mockito.when(
            userApiClient.internalUsersGet(
                null,
                "+79771234567",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "id,DESC"
            )
        ).thenReturn(call);

        mockExistingTransport();

        mockMvc.perform(post("/pipelines")
                .contentType(MediaType.APPLICATION_JSON)
                .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                .content(extractFileContent("fixture/pipeline/request/create_quick_trip.json")))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError())
            .andExpect(content().string(containsString("Курьер с указанным номером телефона уже существует")));

    }

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/after/creation_failed.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateQuickTripTransportConstraintViolation() throws Exception {

        ExecuteCall<PageOfTransportDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(new PageOfTransportDto()
            .totalPages(1)
            .totalElements(2L)
            .size(10)
            .number(0)
            .content(List.of(
                new TransportDto()
                    .id(1L)
                    .name("Машинка раз")
                    .brand("КАМАЗ")
                    .model("5490-S5")
                    .company(new CompanyDto().id(1L).name("company"))
                    .number("BT320X")
                    .trailerNumber("123")
                    .source(TransportSource.CARRIER)
            ))));

        Mockito.when(
            transportApiClient.internalTransportGet(
                null,
                "BM456K",
                4L,
                null,
                null,
                null,
                "id,DESC"
            )
        ).thenReturn(call);

        mockMvc.perform(post("/pipelines")
                .contentType(MediaType.APPLICATION_JSON)
                .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                .content(extractFileContent("fixture/pipeline/request/create_quick_trip_with_transport.json")))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError())
            .andExpect(content().string(containsString("Транспорт для указанной компании с данным номером уже " +
                "существует")));

    }

    @Test
    @ExpectedDatabase(
        value = "/repository/pipeline/after/creation_failed.xml",
        connection = "dbUnitDatabaseConnection",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateQuickTripTransportConstraintViolationWithMergedPointsTimes() throws Exception {

        ExecuteCall<PageOfTransportDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(new PageOfTransportDto()
                .totalPages(1)
                .totalElements(2L)
                .size(10)
                .number(0)
                .content(List.of(
                        new TransportDto()
                                .id(1L)
                                .name("Машинка раз")
                                .brand("КАМАЗ")
                                .model("5490-S5")
                                .company(new CompanyDto().id(1L).name("company"))
                                .number("BT320X")
                                .trailerNumber("123")
                                .source(TransportSource.CARRIER)
                ))));

        Mockito.when(
                transportApiClient.internalTransportGet(
                        null,
                        "BM456K",
                        4L,
                        null,
                        null,
                        null,
                        "id,DESC"
                )
        ).thenReturn(call);

        mockMvc.perform(post("/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                        .content(extractFileContent("fixture/pipeline/request/create_quick_trip_with_transport_with_merged_points_times.json")))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                .andExpect(content().string(containsString("Транспорт для указанной компании с данным номером уже " +
                        "существует")));

    }

    @Test
    @ExpectedDatabase(
            value = "/repository/pipeline/after/creation_failed.xml",
            connection = "dbUnitDatabaseConnection",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateQuickTransportConstraintPalletsCapacity() throws Exception {
        ExecuteCall<PageOfTransportDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(new PageOfTransportDto()
                .totalPages(1)
                .totalElements(2L)
                .size(10)
                .number(0)
                .content(List.of(
                        new TransportDto()
                                .id(1L)
                                .name("Машинка раз")
                                .brand("КАМАЗ")
                                .model("5490-S5")
                                .company(new CompanyDto().id(1L).name("company"))
                                .number("BT320X")
                                .trailerNumber("123")
                                .source(TransportSource.CARRIER)
                ))));

        Mockito.when(
                transportApiClient.internalTransportGet(
                        null,
                        "BM456K",
                        4L,
                        null,
                        null,
                        null,
                        "id,DESC"
                )
        ).thenReturn(call);

        mockExistingTransport(4);

        mockMvc.perform(post("/pipelines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                        .content(extractFileContent("fixture/pipeline/request/create_quick_trip.json")))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                .andExpect(content().string(containsString("Недостаточная вместимость транспорта")));

    }


        private void mockExistingTransport() {
        mockExistingTransport(null);
    }

    private void mockExistingTransport(Integer palletsCapacity) {
        ExecuteCall<PageOfTransportDto, RetryStrategy> transportPageCall = Mockito.mock(ExecuteCall.class);
        PageOfTransportDto transportResult = TransportDtoUtils.page(
                transportDto(1L, "asdasd", palletsCapacity)
        );
        Mockito.when(transportPageCall.schedule())
                .thenReturn(CompletableFuture.completedFuture(transportResult));

        Mockito.when(
                transportApiClient.internalTransportGet(
                        1L,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "id,DESC"
                )
        ).thenReturn(transportPageCall);
    }
}
