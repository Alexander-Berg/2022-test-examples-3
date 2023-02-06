package ru.yandex.market.sc.internal.controller.manual;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ManualPrintControllerTest {

    private final MockMvc mockMvc;
    private final TestFactory testFactory;

    @BeforeEach
    void init() {
        testFactory.storedSortingCenter(1);
    }

    @Test
    @SneakyThrows
    void printerZplPrinterLabel() {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/manual/print/printerLabel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sortingCenterId\": 1,\"printerName\": \"zebra1\"}")
        ).andExpect(status().isOk());
    }

    @Test
    @SneakyThrows
    void printerZplPrinterLabelInvalidRequests() {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/manual/print/printerLabel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"printerName\": \"zebra1\"}")
        ).andExpect(status().is4xxClientError());
        mockMvc.perform(
                MockMvcRequestBuilders.post("/manual/print/printerLabel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sortingCenterId\": 1}")
        ).andExpect(status().is4xxClientError());
        mockMvc.perform(MockMvcRequestBuilders.post("/manual/print/printerLabel"))
                .andExpect(status().is4xxClientError());
    }

}
