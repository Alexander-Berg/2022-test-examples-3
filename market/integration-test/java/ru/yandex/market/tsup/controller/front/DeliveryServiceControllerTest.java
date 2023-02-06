package ru.yandex.market.tsup.controller.front;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.mj.generated.client.carrier.api.DeliveryServiceApiClient;
import ru.yandex.mj.generated.client.carrier.api.UserApiClient;
import ru.yandex.mj.generated.client.carrier.model.DeliveryServiceSuggest;
import ru.yandex.mj.generated.client.carrier.model.PageOfDeliveryServiceSuggest;
import ru.yandex.mj.generated.client.carrier.model.PageOfUserDto;
import ru.yandex.mj.generated.client.carrier.model.UserDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DeliveryServiceControllerTest extends AbstractContextualTest {

    @Autowired
    private DeliveryServiceApiClient deliveryServiceApiClient;

    @Autowired
    private UserApiClient userApiClient;

    @BeforeEach
    void beforeEach() {
        ExecuteCall<PageOfDeliveryServiceSuggest, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(
                call.schedule()
        ).thenReturn(CompletableFuture.completedFuture(
                makeDeliveryServiceResult()
        ));

        Mockito.when(
                deliveryServiceApiClient.internalDeliveryServicesSuggestGet(
                        null,
                        null,
                        null,
                        0,
                        300,
                        "id,DESC"
                )
        ).thenReturn(call);

        ExecuteCall<PageOfUserDto, RetryStrategy> getUsersResult = makeDriversResult();
        Mockito.when(
                userApiClient.internalUsersGet(
                        null,
                        null,
                        null,
                        1L,
                        null,
                        null,
                        null,
                        0,
                        20,
                        "name,ASC"
                )
        ).thenReturn(getUsersResult);
    }

    @NotNull
    private PageOfDeliveryServiceSuggest makeDeliveryServiceResult() {
        List<DeliveryServiceSuggest> content = IntStream.rangeClosed(1, 10)
                .mapToObj(this::makeSuggest)
                .collect(Collectors.toList());

        PageOfDeliveryServiceSuggest pageOfDeliveryServiceSuggest = new PageOfDeliveryServiceSuggest();
        pageOfDeliveryServiceSuggest.setContent(content);
        pageOfDeliveryServiceSuggest.setNumber(0);
        pageOfDeliveryServiceSuggest.setSize(content.size());
        pageOfDeliveryServiceSuggest.setTotalPages(1);
        pageOfDeliveryServiceSuggest.setTotalElements((long) content.size());
        return pageOfDeliveryServiceSuggest;
    }

    private ExecuteCall<PageOfUserDto, RetryStrategy> makeDriversResult() {
        ExecuteCall<PageOfUserDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        PageOfUserDto pageOfUserDto = new PageOfUserDto();
        pageOfUserDto.setNumber(1);
        pageOfUserDto.setSize(10);
        pageOfUserDto.setTotalPages(2);
        pageOfUserDto.setTotalElements(20L);
        pageOfUserDto.setContent(List.of(
                new UserDto()
                        .name("Петр Петрович"),
                new UserDto()
                        .name("Семен Семенович")
        ));

        Mockito.when(call.schedule())
                .thenReturn(CompletableFuture.completedFuture(pageOfUserDto));
        return call;
    }

    @NotNull
    private DeliveryServiceSuggest makeSuggest(int idx) {
        DeliveryServiceSuggest suggest = new DeliveryServiceSuggest();
        suggest.setId((long) idx);
        suggest.setName("ds " + idx);
        return suggest;
    }

    @SneakyThrows
    @Test
    void shouldGetDeliveryServices() {
        mockMvc.perform(get("/delivery-services")
                        .param("pageSize", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").value(Matchers.hasSize(10)));
    }

    @SneakyThrows
    @Test
    void shouldGetDeliveryService() {
        mockMvc.perform(get("/delivery-services/{id}", 1))
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldGetDeliveryServiceDrivers() {
        mockMvc.perform(get("/delivery-services/{id}/drivers", 1))
                .andExpect(status().isOk());
    }

}
