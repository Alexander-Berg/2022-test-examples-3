package ru.yandex.market.tsup.controller.front;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.util.UserDtoUtils;
import ru.yandex.mj.generated.client.carrier.api.UserApiClient;
import ru.yandex.mj.generated.client.carrier.model.CompanyDto;
import ru.yandex.mj.generated.client.carrier.model.PageOfUserDto;
import ru.yandex.mj.generated.client.carrier.model.PersonalDataDto;
import ru.yandex.mj.generated.client.carrier.model.UserDto;
import ru.yandex.mj.generated.client.carrier.model.UserSourceDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CourierControllerTest extends AbstractContextualTest {
    @Autowired
    private UserApiClient userApiClient;

    @BeforeEach
    void beforeEach() {
        ExecuteCall<PageOfUserDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        PageOfUserDto result = UserDtoUtils.page(
                new UserDto()
                        .id(1L)
                        .name("Василий Васильев Васильевич")
                        .firstName("Василий")
                        .lastName("Васильев")
                        .patronymic("Васильевич")
                        .companies(List.of(new CompanyDto().id(1L).name("company")))
                        .phone("+79234567821")
                        .source(UserSourceDto.CARRIER),
                new UserDto()
                        .id(2L)
                        .name("Татьяна Иванова")
                        .source(UserSourceDto.LOGISTICS_COORDINATOR)
        );
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(result));

        Mockito.when(
            userApiClient.internalUsersGet(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                10,
                "id,DESC"
            )
        ).thenReturn(call);
    }

    @SneakyThrows
    @Test
    void getCouriers() {
        mockMvc.perform(get("/couriers")
                .param("pageSize", "10")
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data").value(Matchers.hasSize(2)))
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.pageSize").value(10))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.data[0].id").value(1L))
            .andExpect(jsonPath("$.data[0].name").value("Василий Васильев Васильевич"))
            .andExpect(jsonPath("$.data[0].firstName").value("Василий"))
            .andExpect(jsonPath("$.data[0].lastName").value("Васильев"))
            .andExpect(jsonPath("$.data[0].patronymic").value("Васильевич"))
            .andExpect(jsonPath("$.data[0].phone").value("+79234567821"))
            .andExpect(jsonPath("$.data[0].source").value("CARRIER"))
            .andExpect(jsonPath("$.data[0].companies[0].id").value(1))
            .andExpect(jsonPath("$.data[0].companies[0].name").value("company"))
            .andExpect(jsonPath("$.data[1].id").value(2L))
            .andExpect(jsonPath("$.data[1].name").value("Татьяна Иванова"))
            .andExpect(jsonPath("$.data[1].source").value("LOGISTICS_COORDINATOR"));
    }

    @SneakyThrows
    @Test
    void getPersonalData() {
        var personalData = new PersonalDataDto()
                .id(101L)
                .dsmId("dsmId")
                .lastName("lastName")
                .firstName("firstName")
                .patronymic("patronymic")
                .phone("88002353535");

        ExecuteCall<PersonalDataDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(personalData));
        Mockito.when(userApiClient.internalUsersUserIdPersonalGet(personalData.getId())).thenReturn(call);

        mockMvc.perform(get("/couriers/{courierId}", personalData.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(personalData)));
    }
}
