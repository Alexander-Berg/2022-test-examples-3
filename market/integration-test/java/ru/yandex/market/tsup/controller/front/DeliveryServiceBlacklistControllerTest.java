package ru.yandex.market.tsup.controller.front;

import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.controller.dto.CourierBlackListDto;
import ru.yandex.market.tsup.controller.dto.CourierBlackListReasonDto;
import ru.yandex.mj.generated.client.carrier.api.UserApiClient;
import ru.yandex.mj.generated.client.carrier.model.UserDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DeliveryServiceBlacklistControllerTest extends AbstractContextualTest {

    @Autowired
    private UserApiClient userApiClient;

    @BeforeEach
    void setUp() {
        UserDto userDto = new UserDto();
        userDto.setId(1L);
        userDto.setName("Иван Иванов");
        userDto.setLastName("Иванов");
        userDto.setFirstName("Иван");

        ExecuteCall<UserDto, RetryStrategy> executeCall = Mockito.mock(ExecuteCall.class);
        Mockito.when(executeCall.schedule()).thenReturn(CompletableFuture.completedFuture(userDto));
        Mockito.when(userApiClient.internalUsersUserIdMarkBlacklistedPost(Mockito.anyLong(), Mockito.any()))
                .thenReturn(executeCall);
        Mockito.when(userApiClient.internalUsersUserIdUnmarkBlacklistedPost(Mockito.anyLong()))
                .thenReturn(executeCall);
    }

    @SneakyThrows
    @Test
    void shouldBlacklistUser() {
        mockMvc.perform(post("/delivery-services/{dsId}/drivers/{driverId}/mark-blacklisted", 1, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Иван Иванов"))
                .andExpect(jsonPath("$.lastName").value("Иванов"))
                .andExpect(jsonPath("$.firstName").value("Иван"));

    }

    @SneakyThrows
    @Test
    void shouldBlacklistUserWithReason() {
        mockMvc.perform(post("/delivery-services/{dsId}/drivers/{driverId}/mark-blacklisted", 1, 2)
                .content(objectMapper.writeValueAsString(
                    new CourierBlackListDto(CourierBlackListReasonDto.NOT_USING_APP)
                ))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Иван Иванов"))
            .andExpect(jsonPath("$.lastName").value("Иванов"))
            .andExpect(jsonPath("$.firstName").value("Иван"));

    }

    @SneakyThrows
    @Test
    void shouldUnmarkBlacklistedUser() {
        mockMvc.perform(post("/delivery-services/{dsId}/drivers/{driverId}/unmark-blacklisted", 1, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Иван Иванов"))
                .andExpect(jsonPath("$.lastName").value("Иванов"))
                .andExpect(jsonPath("$.firstName").value("Иван"));
    }

    @SneakyThrows
    @Test
    void shouldGetAllReasons() {
        mockMvc.perform(get("/delivery-services/blackListReason"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").value(Matchers.hasSize(12)))
            .andExpect(jsonPath("$[*]").value(Matchers.containsInAnyOrder(
                CourierBlackListReasonDto.NOT_USING_APP.name(),
                CourierBlackListReasonDto.NO_SMARTPHONE.name(),
                CourierBlackListReasonDto.SET_STATUSES_ALL_AT_ONCE.name(),
                CourierBlackListReasonDto.ERROR_WITH_APP_WITHOUT_TICKET.name(),
                CourierBlackListReasonDto.CARGO_SECURING_ABSENCE.name(),
                CourierBlackListReasonDto.UNLOADING_LATE.name(),
                CourierBlackListReasonDto.LOADING_LATE.name(),
                CourierBlackListReasonDto.DRIVER_INCOMPETENCE.name(),
                CourierBlackListReasonDto.UNAPPROVED_DECISION_MAKING.name(),
                CourierBlackListReasonDto.DISINFORMATION.name(),
                CourierBlackListReasonDto.STAMP_DAMAGE.name(),
                CourierBlackListReasonDto.OTHER.name()
                )
            ));
    }
}
