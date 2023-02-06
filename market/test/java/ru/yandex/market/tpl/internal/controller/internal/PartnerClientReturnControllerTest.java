package ru.yandex.market.tpl.internal.controller.internal;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnQueryService;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnReopenValidator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnService;
import ru.yandex.market.tpl.core.domain.partner.clientreturn.PartnerClientReturnService;
import ru.yandex.market.tpl.core.service.company.PartnerCompanyRoleService;
import ru.yandex.market.tpl.core.service.partnerka.PartnerkaCommandService;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;
import ru.yandex.market.tpl.internal.controller.partner.PartnerClientReturnController;
import ru.yandex.market.tpl.internal.service.report.SequenceBarcodeReportService;
import ru.yandex.market.tpl.internal.service.report.barcodes.BarcodeType;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebLayerTest(PartnerClientReturnController.class)
public class PartnerClientReturnControllerTest extends BaseShallowTest {

    @MockBean
    private ClientReturnService clientReturnService;
    @MockBean
    private SequenceBarcodeReportService sequenceBarcodeReportService;
    @MockBean
    private ClientReturnQueryService clientReturnQueryService;
    @MockBean
    private PartnerkaCommandService partnerkaCommandService;
    @MockBean
    private PartnerCompanyRoleService partnerCompanyRoleService;
    @MockBean
    private PartnerClientReturnService partnerClientReturnService;
    @MockBean
    private ClientReturnReopenValidator clientReturnReopenValidator;

    @SneakyThrows
    @Test
    public void generateBarcodesWithDefaultBarcodeType() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("countBarcodes", "1");

        mockMvc.perform(get("/internal/orders/return/barcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(params)
                )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @EnumSource(BarcodeType.class)
    @ParameterizedTest
    public void generateBarcodesWithBarcodeType(BarcodeType barcodeType) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("countBarcodes", "1");
        params.add("barcodeType", barcodeType.name());

        mockMvc.perform(get("/internal/orders/return/barcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(params)
                )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    public void generateBarcodesWithWrongBarcodeType() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("countBarcodes", "1");
        params.add("barcodeType", "FAKE_TEST");

        mockMvc.perform(get("/internal/orders/return/barcodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .params(params)
                )
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof MethodArgumentTypeMismatchException));
    }

}
