package ru.yandex.market.tpl.internal.controller.internal;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.partner.ScDamagedOrder;
import ru.yandex.market.tpl.api.model.order.partner.ScDamagedOrderList;
import ru.yandex.market.tpl.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.core.service.company.PartnerCompanyRoleService;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;
import ru.yandex.market.tpl.internal.service.SortingCenterInternalService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author valter
 */
@WebLayerTest(SortingCenterController.class)
class SortingCenterControllerTest extends BaseShallowTest {

    @MockBean
    private SortingCenterInternalService sortingCenterService;
    @MockBean
    private CompanyRepository companyRepository;
    @MockBean
    private PartnerCompanyRoleService partnerCompanyRoleService;

    @Test
    void getDamagedOrdersByIdsList() throws Exception {
        doReturn(new ScDamagedOrderList(List.of(new ScDamagedOrder("1", true))))
                .when(sortingCenterService).getDamagedOrders(eq(List.of("1")));
        mockMvc.perform(get("/internal/sc/damagedOrders?oid=1"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"orders\":[{ " +
                        "  \"externalId\": \"1\", " +
                        "  \"damaged\": true " +
                        "}]}"
                ));
    }

    @Test
    void getDamagedOrdersByInterval() throws Exception {
        var now = Instant.ofEpochMilli(0L);
        doReturn(new ScDamagedOrderList(List.of(new ScDamagedOrder("1", true))))
                .when(sortingCenterService).getDamagedOrders(eq(now), eq(now));
        mockMvc.perform(
                get("/internal/sc/damagedOrders" +
                        "?damagedFrom=1970-01-01T00:00:00Z&damagedTo=1970-01-01T00:00:00Z")
        )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\"orders\":[{ " +
                        "  \"externalId\": \"1\", " +
                        "  \"damaged\": true " +
                        "}]}"
                ));
    }

}
