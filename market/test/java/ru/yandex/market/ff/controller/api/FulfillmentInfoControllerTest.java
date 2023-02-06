package ru.yandex.market.ff.controller.api;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.legalInfo.LegalInfoResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FulfillmentInfoControllerTest extends MvcIntegrationTest {

    @Test
    @DatabaseSetup("classpath:controller/fulfillment-info/before-sync.xml")
    @ExpectedDatabase(value = "classpath:controller/fulfillment-info/after-sync.xml",
            assertionMode = NON_STRICT
    )
    void syncWarehouse() throws Exception {
        List<PartnerResponse> partnerResponses = Collections.singletonList(
                PartnerResponse.newBuilder().id(145L).name("service1455").status(PartnerStatus.ACTIVE)
                        .partnerType(PartnerType.FULFILLMENT).marketId(11L).build()
        );

        when(lmsClient.searchPartners(eq(SearchPartnerFilter.builder()
                .setIds(Set.of(145L))
                .build())))
                .thenReturn(partnerResponses);

        when(lmsClient.getLogisticsPoints(
                refEq(LogisticsPointFilter.newBuilder()
                        .partnerIds(Collections.singleton(145L))
                        .type(PointType.WAREHOUSE)
                        .build())))
                .thenReturn(Collections.singletonList(LogisticsPointResponse.newBuilder()
                        .address(Address.newBuilder()
                                .addressString("А это ваш адрес")
                                .build())
                        .build()));

        when(lmsClient.getPartnerLegalInfo(eq(145L))).thenReturn(Optional.of(
                new LegalInfoResponse(1L, 145L, "ООО \"Ваш поставщик 1\"", 1L, "url", "ООО", "", "",
                        Address.newBuilder().addressString("А это ваш юридический адрес").build(),
                        Address.newBuilder().addressString("А это ваш почтовый адрес").build(),
                        "", "", "", "")
        ));

        mockMvc.perform(put("/fulfillments/145/sync"))
                .andExpect(status().isOk());
    }
}
