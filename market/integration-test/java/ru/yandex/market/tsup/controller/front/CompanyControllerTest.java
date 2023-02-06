package ru.yandex.market.tsup.controller.front;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
import ru.yandex.mj.generated.client.carrier.api.CompanyApiClient;
import ru.yandex.mj.generated.client.carrier.model.CompanyDto;
import ru.yandex.mj.generated.client.carrier.model.PageOfCompanies;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CompanyControllerTest extends AbstractContextualTest {
    @Autowired
    private CompanyApiClient companyApiClient;

    @BeforeEach
    void beforeEach() {
        ExecuteCall<PageOfCompanies, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(companies()));

        Mockito.when(
            companyApiClient.internalCompaniesGet(
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
    void getCompanies() {
        mockMvc.perform(get("/companies")
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
            .andExpect(jsonPath("$.data[0].name").value("Компания 1"))
            .andExpect(jsonPath("$.data[0].deliveryServiceId").value(12L))
            .andExpect(jsonPath("$.data[1].id").value(2L))
            .andExpect(jsonPath("$.data[1].name").value("Компания 2"));
    }

    @NotNull
    private PageOfCompanies companies() {
        return new PageOfCompanies()
            .totalPages(1)
            .totalElements(2L)
            .size(10)
            .number(0)
            .content(List.of(
                new CompanyDto().id(1L).name("Компания 1").deliveryServiceId(12L),
                new CompanyDto().id(2L).name("Компания 2")
            ));
    }
}
