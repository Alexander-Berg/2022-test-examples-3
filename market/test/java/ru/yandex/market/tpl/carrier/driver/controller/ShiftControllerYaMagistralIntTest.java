package ru.yandex.market.tpl.carrier.driver.controller;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.company_property.CompanyProperty;
import ru.yandex.market.tpl.carrier.core.domain.company_property.CompanyPropertyRepository;
import ru.yandex.market.tpl.carrier.core.domain.company_property.CompanyPropertyType;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user_properties.UserProperty;
import ru.yandex.market.tpl.carrier.core.domain.user_properties.UserPropertyRepository;
import ru.yandex.market.tpl.carrier.core.domain.user_properties.UserPropertyType;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.api.model.routepoint.RoutePointAddressDto;
import ru.yandex.market.tpl.carrier.driver.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.carrier.driver.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.carrier.driver.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.carrier.driver.api.model.shift.UserShiftDto;
import ru.yandex.market.tpl.carrier.driver.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.carrier.driver.api.model.task.CollectDropshipTakePhotoDto;
import ru.yandex.market.tpl.carrier.driver.api.model.task.CollectDropshipTaskDto;
import ru.yandex.market.tpl.carrier.driver.api.model.task.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.carrier.driver.api.model.task.OrderReturnTaskDto;
import ru.yandex.market.tpl.carrier.driver.api.model.task.OrderReturnTaskStatus;
import ru.yandex.market.tpl.carrier.driver.api.model.task.PhotoRequirementType;
import ru.yandex.market.tpl.carrier.driver.api.model.task.TaskDto;
import ru.yandex.market.tpl.carrier.driver.api.model.task.TaskType;
import ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.mj.generated.client.yandex_magistral.api.YandexMagistralApiClient;
import ru.yandex.mj.generated.client.yandex_magistral.model.FlightListDto;
import ru.yandex.mj.generated.client.yandex_magistral.model.GetFlightsListByDriverResponse;
import ru.yandex.mj.generated.client.yandex_magistral.model.GetTokenDto;
import ru.yandex.mj.generated.client.yandex_magistral.model.GetTransportDto;
import ru.yandex.mj.generated.client.yandex_magistral.model.GetTransportResponse;
import ru.yandex.mj.generated.client.yandex_magistral.model.RouteDto;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.carrier.core.domain.user.UserUtil.TAXI_ID;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class ShiftControllerYaMagistralIntTest extends BaseDriverApiIntTest {

    private final TestUserHelper testUserHelper;
    private final CompanyPropertyRepository companyPropertyRepository;
    private final UserPropertyRepository userPropertyRepository;

    private final YandexMagistralApiClient yandexMagistralApiClient;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private final ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.USER_DRAFT_ENABLED, true);

        ExecuteCall<GetTokenDto, RetryStrategy> getTokenCall = Mockito.mock(ExecuteCall.class);
        Mockito.when(getTokenCall.schedule())
                        .thenReturn(CompletableFuture.completedFuture(
                                new GetTokenDto()
                                        .isSuccess(true)
                                        .message("TOKEN")
                        ));

        Mockito.when(yandexMagistralApiClient.accountGetTokenPost(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(getTokenCall);

        ExecuteCall<GetFlightsListByDriverResponse, RetryStrategy> getFlightsCall = Mockito.mock(ExecuteCall.class);
        Mockito.when(getFlightsCall.schedule())
                        .thenReturn(CompletableFuture.completedFuture(
                                new GetFlightsListByDriverResponse()
                                        .isSuccess(true)
                                        .entity(List.of(
                                                new FlightListDto()
                                                        .id("asdasd")
                                                        .startDate("15.06.2022")
                                                        .startTime("09:00")
                                                        .endDate("15.06.2022")
                                                        .endTime("18:00")
                                                        .addRouteItem(
                                                                new RouteDto()
                                                                        .coordinates(List.of(
                                                                                "50.0", "50.0"
                                                                        ))
                                                                        .address("address")
                                                                        .type(0)
                                                                        .date("15.06.2022 09:00")
                                                                        .deliveriesId(List.of("id1"))
                                                        )
                                                        .addRouteItem(
                                                                new RouteDto()
                                                                        .coordinates(List.of(
                                                                                "50.0", "50.0"
                                                                        ))
                                                                        .address("address")
                                                                        .type(1)
                                                                        .date("15.06.2022 18:00")
                                                                        .deliveriesId(List.of("id1"))
                                                        )
                                        ))
                        ));
        ExecuteCall<GetFlightsListByDriverResponse, RetryStrategy> getOnlineFlights = Mockito.mock(ExecuteCall.class);
        Mockito.when(getOnlineFlights.schedule())
                        .thenReturn(CompletableFuture.completedFuture(
                                new GetFlightsListByDriverResponse()
                                        .isSuccess(true)
                                        .entity(List.of(
                                                new FlightListDto()
                                                        .id("defdef")

                                                        .addRouteItem(
                                                                new RouteDto()
                                                                        .title("title1")
                                                                        .coordinates(List.of(
                                                                                "50.0", "50.0"
                                                                        ))
                                                                        .address("address1")
                                                                        .type(0)
                                                                        .date("15.06.2022 09:00")
                                                                        .deliveriesId(List.of("id1"))
                                                        )
                                                        .addRouteItem(
                                                                new RouteDto()
                                                                        .title("title2")
                                                                        .coordinates(List.of(
                                                                                "50.0", "50.0"
                                                                        ))
                                                                        .address("address2")
                                                                        .type(1)
                                                                        .date("15.06.2022 18:00")
                                                                        .deliveriesId(List.of("id1"))
                                                        )
                                        ))
                        ));
        ExecuteCall<GetTransportResponse, RetryStrategy> getTransportDataCall = Mockito.mock(ExecuteCall.class);
        Mockito.when(getTransportDataCall.schedule())
                        .thenReturn(CompletableFuture.completedFuture(
                                new GetTransportResponse()
                                        .isSuccess(true)
                                        .entity(new GetTransportDto()
                                                .startDate("15.06.2022")
                                                .startTime("09:00")
                                                .endDate("15.06.2022")
                                                .endTime("18:00")
                                        )
                        ));


        Mockito.when(yandexMagistralApiClient.flightsGetListByDriverPost("Bearer TOKEN"))
                .thenReturn(getFlightsCall);
        Mockito.when(yandexMagistralApiClient.onlineFlightsGetListByDriverPost("Bearer TOKEN"))
                .thenReturn(getOnlineFlights);
        Mockito.when(yandexMagistralApiClient.flightsGetTransportDataPost("Bearer TOKEN", "defdef"))
                .thenReturn(getTransportDataCall);

        var company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        var user = testUserHelper.findOrCreateUser(TAXI_ID, UID);
        CompanyProperty<Boolean> companyProperty = new CompanyProperty<>();
        companyProperty.init(company, CompanyPropertyType.CompanyPropertyName.IS_YANDEX_MAGISTRAL, Boolean.toString(true));
        companyPropertyRepository.save(companyProperty);

        UserProperty<String> userProperty = new UserProperty<>();
        userProperty.init(user, UserPropertyType.UserPropertyName.YANDEX_MAGISTRAL_PASSWORD, "password");
        userPropertyRepository.save(userProperty);
    }


    @SneakyThrows
    @Test
    void shouldGetShiftsFromYandexMagistral() {
        String getShiftsResponse = mockMvc.perform(
                        MockMvcRequestBuilders.get(ApiParams.BASE_PATH + "/shifts")
                                .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                                .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<UserShiftDto> userShifts = objectMapper.readValue(getShiftsResponse, new TypeReference<List<UserShiftDto>>() {});
        Assertions.assertThat(userShifts).hasSize(2);

        UserShiftDto notActive = userShifts.stream().filter(us -> us.getIdString().equals("asdasd")).findFirst().get();

        UserShiftDto active = userShifts.stream().filter(us -> us.getIdString().equals("defdef")).findFirst().get();

        Assertions.assertThat(active).extracting(UserShiftDto::getStatus).isEqualTo(UserShiftStatus.ON_TASK);
        Assertions.assertThat(active).extracting(UserShiftDto::isActive).isEqualTo(true);
        Assertions.assertThat(active).extracting(UserShiftDto::getStartDate).isEqualTo(
                ZonedDateTime.of(
                        2022, 6, 15, 9, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID
                ).toInstant()
        );
        Assertions.assertThat(active).extracting(UserShiftDto::getEndDate).isEqualTo(
                ZonedDateTime.of(
                        2022, 6, 15, 18, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID
                ).toInstant()
        );
        Assertions.assertThat(active).extracting(UserShiftDto::getCurrentRoutePointIdString).isEqualTo("cd_id1");
        Assertions.assertThat(active).extracting(UserShiftDto::getRunIdString).isEqualTo("defdef");
        Assertions.assertThat(active).extracting(UserShiftDto::getCollectNumber).isEqualTo(1L);
        Assertions.assertThat(active).extracting(UserShiftDto::getReturnNumber).isEqualTo(1L);
        Assertions.assertThat(active).extracting(UserShiftDto::getFirstRoutePointName).isEqualTo("title1");
        Assertions.assertThat(active).extracting(UserShiftDto::getLastRoutePointName).isEqualTo("title2");

        Mockito.verify(yandexMagistralApiClient, Mockito.atLeastOnce()).accountGetTokenPost(Mockito.anyString(), Mockito.anyString());
    }

    @SneakyThrows
    @Test
    void shouldReturnUserShiftById() {
        String getRoutePointsResponse = mockMvc.perform(
                        MockMvcRequestBuilders.get(ApiParams.BASE_PATH + "/shifts/{id}", "defdef")
                                .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                                .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(Matchers.hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<RoutePointDto> routePoints = objectMapper.readValue(getRoutePointsResponse, new TypeReference<List<RoutePointDto>>() {});
        Assertions.assertThat(routePoints).hasSize(2);
        
        RoutePointDto collectPoint = routePoints.stream().filter(p -> p.getType() == RoutePointType.COLLECT_DROPSHIP).findFirst().get();
        checkCollectPoint(collectPoint);

        RoutePointDto returnPoint = routePoints.stream().filter(p -> p.getType() == RoutePointType.ORDER_RETURN).findFirst().get();
        checkReturnPoint(returnPoint);
    }

    @SneakyThrows
    @Test
    void shouldReturnRoutePointById() {
        String getRoutePointsResponse = mockMvc.perform(
                        MockMvcRequestBuilders.get(ApiParams.BASE_PATH + "/route-points/{id}", "cd_id1")
                                .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                                .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();


        RoutePointDto collectPoint = objectMapper.readValue(getRoutePointsResponse, RoutePointDto.class);
        checkCollectPoint(collectPoint);
    }

    @SneakyThrows
    @Test
    void shouldReturnRoutePointById2() {
        String getRoutePointsResponse = mockMvc.perform(
                        MockMvcRequestBuilders.get(ApiParams.BASE_PATH + "/route-points/{id}", "or_id1")
                                .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                                .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();


        RoutePointDto collectPoint = objectMapper.readValue(getRoutePointsResponse, RoutePointDto.class);
        checkReturnPoint(collectPoint);
    }

    private void checkCollectPoint(RoutePointDto collectPoint) {
        Assertions.assertThat(collectPoint).extracting(RoutePointDto::getIdString).isEqualTo("cd_id1");
        Assertions.assertThat(collectPoint).extracting(RoutePointDto::getStatus).isEqualTo(RoutePointStatus.IN_PROGRESS);
        Assertions.assertThat(collectPoint)
                .extracting(RoutePointDto::getAddress)
                .extracting(RoutePointAddressDto::getAddressString)
                .isEqualTo("address1");
        Assertions.assertThat(collectPoint).extracting(RoutePointDto::getExpectedDate).isEqualTo(
                ZonedDateTime.of(
                        2022, 6, 15, 9, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID
                ).toInstant()
        );
        List<TaskDto> tasks = collectPoint.getTasks();
        Assertions.assertThat(tasks).hasSize(1);
        CollectDropshipTaskDto task = (CollectDropshipTaskDto) tasks.get(0);
        Assertions.assertThat(task).extracting(CollectDropshipTaskDto::getIdString).isEqualTo("cd_id1");
        Assertions.assertThat(task).extracting(CollectDropshipTaskDto::getType).isEqualTo(TaskType.COLLECT_DROPSHIP);
        Assertions.assertThat(task).extracting(CollectDropshipTaskDto::getFinishedAt).isNull();
        Assertions.assertThat(task).extracting(CollectDropshipTaskDto::getStatus).isEqualTo(CollectDropshipTaskStatus.IN_PROGRESS);
        Assertions.assertThat(task)
                .extracting(CollectDropshipTaskDto::getTakePhoto)
                .extracting(CollectDropshipTakePhotoDto::getPhotoRequired)
                .isEqualTo(PhotoRequirementType.OPTIONAL);
    }

    private void checkReturnPoint(RoutePointDto returnPoint) {
        Assertions.assertThat(returnPoint).extracting(RoutePointDto::getIdString).isEqualTo("or_id1");
        Assertions.assertThat(returnPoint).extracting(RoutePointDto::getStatus).isEqualTo(RoutePointStatus.NOT_STARTED);
        Assertions.assertThat(returnPoint).extracting(RoutePointDto::getAddress)
                .extracting(RoutePointAddressDto::getAddressString)
                .isEqualTo("address2");
        Assertions.assertThat(returnPoint).extracting(RoutePointDto::getExpectedDate).isEqualTo(
                ZonedDateTime.of(
                        2022, 6, 15, 18, 0, 0, 0, DateTimeUtil.DEFAULT_ZONE_ID
                ).toInstant()
        );
        List<TaskDto> returnTasks = returnPoint.getTasks();
        Assertions.assertThat(returnTasks).hasSize(1);
        OrderReturnTaskDto returnTask = (OrderReturnTaskDto) returnTasks.get(0);
        Assertions.assertThat(returnTask).extracting(OrderReturnTaskDto::getIdString).isEqualTo("or_id1");
        Assertions.assertThat(returnTask).extracting(OrderReturnTaskDto::getType).isEqualTo(TaskType.ORDER_RETURN);
        Assertions.assertThat(returnTask).extracting(OrderReturnTaskDto::getFinishedAt).isNull();
        Assertions.assertThat(returnTask).extracting(OrderReturnTaskDto::getStatus).isEqualTo(OrderReturnTaskStatus.NOT_STARTED);
        Assertions.assertThat(returnTask)
                .extracting(OrderReturnTaskDto::getTakePhoto)
                .extracting(CollectDropshipTakePhotoDto::getPhotoRequired)
                .isEqualTo(PhotoRequirementType.OPTIONAL);
    }

}
