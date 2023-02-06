package ru.yandex.market.tsup.controller.front;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.model.EntityType;
import ru.yandex.market.tsup.service.data_provider.entity.moving_partner.MovingPartnerProvider;
import ru.yandex.mj.generated.client.carrier.api.TransportApiClient;
import ru.yandex.mj.generated.client.carrier.model.CompanyDto;
import ru.yandex.mj.generated.client.carrier.model.PageOfTransportDto;
import ru.yandex.mj.generated.client.carrier.model.TransportDto;
import ru.yandex.mj.generated.client.carrier.model.TransportSource;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SuggestTest extends AbstractContextualTest {

    @Autowired
    private MovingPartnerProvider movingPartnerProvider;

    @MockBean
    private TransportApiClient transportApiClient;

    @SneakyThrows
    @Test
    void movingPartnerCreate() {
        int expected = movingPartnerProvider.provideAll().size();

        mockMvc.perform(get("/filter/suggest")
                .param("entityType", EntityType.MOVING_PARTNER.name())
                .header("action", FrontAction.CREATE.name()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.suggest").isArray())
            .andExpect(jsonPath("$.suggest", hasSize(expected)));
    }

    @SneakyThrows
    @Test
    void movingPartnerGetWithFilter() {
        mockMvc.perform(get("/filter/suggest")
                        .param("entityType", EntityType.MOVING_PARTNER.name())
                        .param("substring", "Велес")
                        .header("action", FrontAction.GET.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggest").isArray())
                .andExpect(jsonPath("$.suggest", hasSize(1)));
    }

    @SneakyThrows
    @Test
    @DatabaseSetup("/repository/suggest/suggest.xml")
    void movingPartnerGetWithFilterWithFuzzySearch() {
        mockMvc.perform(get("/filter/suggest")
                .param("entityType", EntityType.MOVING_PARTNER.name())
                .param("substring", "Велес")
                .header("action", FrontAction.GET.name()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.suggest").isArray())
            .andExpect(jsonPath("$.suggest", hasSize(10)))
            .andExpect(jsonPath("$.suggest[0].name", equalTo("ВелесТорг")));
    }

    @SneakyThrows
    @Test
    void movementTransportSuggest() {
        ExecuteCall<PageOfTransportDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(call.schedule()).thenReturn(
            CompletableFuture.completedFuture(page(
                List.of(
                    new TransportDto()
                        .id(1L)
                        .number("MH891K")
                        .name("Машинка 1")
                        .brand("Тесла")
                        .model("Тесла")
                        .source(TransportSource.CARRIER)
                        .company(
                            new CompanyDto()
                                .id(11L)
                                .deliveryServiceId(12L)
                                .name("%)")
                        ),
                    new TransportDto()
                        .id(2L)
                        .number("AB891C")
                        .name("Машинка 2")
                        .brand("Тесла")
                        .model("S")
                        .source(TransportSource.CARRIER)
                        .company(
                            new CompanyDto()
                                .id(13L)
                                .deliveryServiceId(14L)
                                .name("%(")
                        ),
                    new TransportDto()
                        .id(3L)
                        .number("AB891D")
                        .name("Машинка 2")
                        .model("S")
                        .source(TransportSource.CARRIER)
                        .company(
                            new CompanyDto()
                                .id(13L)
                                .deliveryServiceId(14L)
                                .name("%(")
                        ),
                    new TransportDto()
                        .id(4L)
                        .number("AB891E")
                        .name("Машинка 2")
                        .brand("Тесла")
                        .source(TransportSource.CARRIER)
                        .company(
                            new CompanyDto()
                                .id(13L)
                                .deliveryServiceId(14L)
                                .name("%(")
                        ),
                    new TransportDto()
                        .id(5L)
                        .number("AB891F")
                        .name("Машинка 2")
                        .source(TransportSource.CARRIER)
                        .company(
                            new CompanyDto()
                                .id(13L)
                                .deliveryServiceId(14L)
                                .name("%(")
                        )
                )
            )));

        Mockito.when(transportApiClient.internalTransportGet(
                        Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(),
                        Mockito.any(), Mockito.any(),
                        Mockito.any()
                ))
                .thenReturn(call);

        mockMvc.perform(get("/filter/suggest")
                        .param("entityType", EntityType.MOVEMENT_TRANSPORT.name())
                        .param("substring", "891")
                        .header("action", FrontAction.GET.getAction()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggest").isArray())
                .andExpect(jsonPath("$.suggest", hasSize(5)))
                .andExpect(jsonPath("$.suggest[0].externalId").value(1L))
                .andExpect(jsonPath("$.suggest[0].name").value("MH891K марка/модель: Тесла"))
                .andExpect(jsonPath("$.suggest[1].name").value("AB891C марка/модель: Тесла/S"))
                .andExpect(jsonPath("$.suggest[2].name").value("AB891D модель: S"))
                .andExpect(jsonPath("$.suggest[3].name").value("AB891E марка: Тесла"))
                .andExpect(jsonPath("$.suggest[4].name").value("AB891F"));
    }

    private static PageOfTransportDto page(List<TransportDto> dtos) {
        PageOfTransportDto page = new PageOfTransportDto();
        page.setContent(dtos);
        page.setTotalElements((long) dtos.size());
        page.setTotalPages(0);
        page.setNumber(0);
        page.setSize(20);
        return page;
    }

}
