package ru.yandex.market.replenishment.autoorder.api;

import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WithMockLogin
public class AssortmentControllerTest extends ControllerTest {

    @Test
    @DbUnitDataSet(before = "AssortmentPackageTypes.before.csv",
            after = "AssortmentPackageTypes.after.csv")
    public void testUploadExcel() throws Exception {
        update(
                "assortment_package_type_test.xlsx",
                "/assortment/package-type/excel").andExpect(status().isOk());
    }

    @Test
    @DbUnitDataSet(before = "Assortment_InvalidMsku.before.csv",
            after = "Assortment_NotUpdated.after.csv")
    public void testUploadExcel_invalidMsku() throws Exception {
        update(
                "assortment_package_type_test.xlsx",
                "/assortment/package-type/excel")
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("MSKU не существуют: 234234"));
    }

    @Test
    @DbUnitDataSet(before = "AssortmentPackageTypes_InvalidRsId.before.csv",
            after = "Assortment_NotUpdated.after.csv")
    public void testUploadExcel_invalidRsID() throws Exception {
        update(
                "assortment_package_type_test.xlsx",
                "/assortment/package-type/excel")
                .andExpect(status().isIAmATeapot())
                .andExpect(jsonPath("$.message")
                        .value("Поставщиков с указанными rsIds не существует: 000123, 000321"));
    }

    private ResultActions update(String excelFilename, String apiUrl) throws Exception {
        byte[] bytes = getClass().getResourceAsStream(excelFilename).readAllBytes();
        MockMultipartFile file = new MockMultipartFile(
                "files", excelFilename,"application/vnd.ms-excel", bytes);
        MockMultipartHttpServletRequestBuilder builder =
                MockMvcRequestBuilders.multipart(apiUrl);
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });
        return mockMvc.perform(builder
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE));
    }
}
