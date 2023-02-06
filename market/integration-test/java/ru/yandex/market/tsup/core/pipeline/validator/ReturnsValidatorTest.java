package ru.yandex.market.tsup.core.pipeline.validator;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.delivery.transport_manager.client.TransportManagerClient;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RouteDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RoutePointDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.RoutePointPairDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleDto;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.tpl.common.data_provider.meta.FrontHttpRequestMeta;
import ru.yandex.market.tsup.AbstractContextualTest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class ReturnsValidatorTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private TransportManagerClient transportManagerClient;

    @BeforeEach
    void init() {
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

        Mockito.when(transportManagerClient.findOrCreateRouteSchedule(Mockito.any())).thenReturn(
            RouteScheduleDto.builder()
                .id(1L).build()
        );
    }

    @Test
    void testInvalidInbound() throws Exception {
        Mockito.when(lmsClient.searchPartners(SearchPartnerFilter.builder().setIds(Set.of(1L, 2L, 3L)).build()))
            .thenReturn(List.of(
                PartnerResponse.newBuilder().id(1L).partnerType(PartnerType.SORTING_CENTER).build(),
                PartnerResponse.newBuilder().id(2L).partnerType(PartnerType.SORTING_CENTER).build(),
                PartnerResponse.newBuilder().id(3L).partnerType(PartnerType.DISTRIBUTION_CENTER).build()
            ));

        mockMvc.perform(post("/routes/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                .content(extractFileContent("fixture/pipeline/request/create_returns.json")))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError())
            .andExpect(content().string(containsString(
                "Точки приёмки должны являться фулфиллмент центрами"
            )));
    }

    @Test
    void testInvalidOutbound() throws Exception {
        Mockito.when(lmsClient.searchPartners(SearchPartnerFilter.builder().setIds(Set.of(1L, 2L, 3L)).build()))
            .thenReturn(List.of(
                PartnerResponse.newBuilder().id(1L).partnerType(PartnerType.SORTING_CENTER).build(),
                PartnerResponse.newBuilder().id(2L).partnerType(PartnerType.FULFILLMENT).build(),
                PartnerResponse.newBuilder().id(3L).partnerType(PartnerType.SORTING_CENTER).build()
            ));

        mockMvc.perform(post("/routes/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                .content(extractFileContent("fixture/pipeline/request/create_returns.json")))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError())
            .andExpect(content().string(containsString(
                "Точки отгрузки должны являться сортировочными центрами"
            )));
    }

    @Test
    @DatabaseSetup("/repository/permission/schedule_creation.xml")
    void testOk() throws Exception {
        Mockito.when(lmsClient.searchPartners(SearchPartnerFilter.builder().setIds(Set.of(1L, 2L, 3L)).build()))
            .thenReturn(List.of(
                PartnerResponse.newBuilder().id(1L).partnerType(PartnerType.SORTING_CENTER).build(),
                PartnerResponse.newBuilder().id(2L).partnerType(PartnerType.SORTING_CENTER).build(),
                PartnerResponse.newBuilder().id(3L).partnerType(PartnerType.FULFILLMENT).build()
            ));

        mockMvc.perform(post("/routes/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .header(FrontHttpRequestMeta.YANDEX_LOGIN_HEADER, "staff-login")
                .content(extractFileContent("fixture/pipeline/request/create_returns.json")))
            .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
