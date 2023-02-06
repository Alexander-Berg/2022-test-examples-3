package ru.yandex.market.tpl.internal.controller.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;
import ru.yandex.market.tpl.internal.service.report.barcodes.BarcodePrinter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebLayerTest(BarcodeController.class)
public class BarcodeControllerTest extends BaseShallowTest {

    @MockBean
    private BarcodePrinter barcodePrinter;
    @Autowired
    private ObjectMapper objectMapper;

    @SneakyThrows
    @Test
    public void printBarcodes() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("barcodes", "Barcode1");
        params.add("barcodes", "Barcode2");
        mockMvc.perform(get("/internal/barcodes/print")
                .params(params)
        )
                .andExpect(status().isOk());
    }
}
