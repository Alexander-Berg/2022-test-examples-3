package ru.yandex.market.logistics.management.controller.lgw.callback;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.management.AbstractContextualTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("/data/controller/lgwCallback/prepare_data.xml")
@ParametersAreNonnullByDefault
class PutReferenceWarehousesControllerTest extends AbstractContextualTest {
    @Test
    @ExpectedDatabase(
        value = "/data/controller/lgwCallback/multiple_marks_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void multipleWarehousesMarksAsCreatedSuccessfully() throws Exception {
        mockMvc.perform(putReference(
            "/lgw_callback/put_reference_warehouses_success",
            "{ \"partnerId\": 1, \"warehouseIds\": [1, 2] }"
        ))
            .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/lgwCallback/one_marks_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void oneWarehouseMarksAsErrorSuccessfully() throws Exception {
        mockMvc.perform(putReference(
            "/lgw_callback/put_reference_warehouses_error",
            "{ \"partnerId\": 1, \"warehouseIds\": [1] }"
        ))
            .andExpect(status().isOk());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/lgwCallback/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void couldNotChangeStatusForNotExistingWarehouseCreatingRecord() throws Exception {
        mockMvc.perform(putReference(
            "/lgw_callback/put_reference_warehouses_error",
            "{ \"partnerId\": 1, \"warehouseIds\": [1, 5] }"
        ))
            .andExpect(status().isNotFound());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/lgwCallback/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void putReferenceWarehousesCallbackWithInvalidBody() throws Exception {
        mockMvc.perform(putReference("/lgw_callback/put_reference_warehouses_error", "{ \"partnerId\": 1 }"))
            .andExpect(status().is4xxClientError());
    }

    @Nonnull
    private MockHttpServletRequestBuilder putReference(String url, String content) {
        return MockMvcRequestBuilders.put(url).contentType(MediaType.APPLICATION_JSON).content(content);
    }
}
