package ru.yandex.market.ff.controller.api;


import java.io.IOException;
import java.net.URL;
import java.util.function.Function;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.enums.FileExtension;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LimitControllerTest extends MvcIntegrationTest {

    private static final String VALID_FILE_PATH = "service/limit/files";
    private static final String ERR_FILE_PATH = "service/limit/files_err";
    private static final String URL = "http://test.yandex.ru";

    @BeforeEach
    public void initMdsS3() throws Exception {
        when(mdsS3Client.getUrl(ArgumentMatchers.any())).thenReturn(new URL(URL));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits/before.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits/after-supply.xml",
        assertionMode = NON_STRICT_UNORDERED)
    public void uploadSupplyLimitsFile() throws Exception {
        MockMultipartFile file = getValidFile("quotas_add.xlsx");
        uploadSupply(file)
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits/before-with-destination.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits/after-supply-with-destination.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void uploadSupplyLimitsFileWithDestination() throws Exception {
        MockMultipartFile file = getValidFile("quotas_add_with_destination.xlsx");
        uploadSupply(file)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits/before-same-dates.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits/after-supply-with-destination-same-dates.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void uploadSupplyLimitsFileWithDestinationForSameDates() throws Exception {
        MockMultipartFile file = getValidFile("quotas_add_with_destination_same_dates.xlsx");
        uploadSupply(file)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits/before-with-destination-nulls.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits/after-supply-with-destination-nulls.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void uploadSupplyLimitsFileWithDestinationWithNulls() throws Exception {

        MockMultipartFile file = getValidFile("quotas_add_with_destination_nulls.xlsx");
        uploadSupply(file)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits/before-with-destination-stop.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits/after-supply-with-destination-stop.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void uploadSupplyLimitsFileWithDestinationWithStop() throws Exception {
        MockMultipartFile file = getValidFile("quotas_add_with_destination_stop.xlsx");
        uploadSupply(file)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits/before.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits/after-withdraw.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void uploadWithdrawLimitsFile() throws Exception {
        MockMultipartFile file = getValidFile("quotas_add.xlsx");
        uploadWithdraw(file)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits-with-movement/before.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits-with-movement/after-supply.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void uploadSupplyWithMovementLimitsFile() throws Exception {
        MockMultipartFile file = getValidFile("quotas_with_movement_add.xlsx");
        uploadSupply(file)
                .andExpect(status().isOk())
                .andReturn();
    }


    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits-with-movement/before.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits-with-movement/after-withdraw.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void uploadWithdrawWithMovementLimitsFile() throws Exception {
        MockMultipartFile file = getValidFile("quotas_with_movement_add.xlsx");
        uploadWithdraw(file)
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits/before.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits/before.xml", assertionMode = NON_STRICT)
    public void uploadSupplyLimitsFileWithHeaderErrors() throws Exception {
        assertCorrectHeaderErrors(this::uploadSupply);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits/before.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits/before.xml", assertionMode = NON_STRICT)
    public void uploadWithdrawLimitsFileWithHeaderErrors() throws Exception {
        assertCorrectHeaderErrors(this::uploadWithdraw);
    }

    private void assertCorrectHeaderErrors(@Nonnull Function<MockMultipartFile, ResultActions> uploader)
            throws Exception {
        assertHeaderErrors("quotas_format_error_3p.xlsx", "{\"message\":\"Supply limit validation error\"," +
                        "\"type\":\"INVALID_DOCUMENT_CONTENT\",\"errorInfo\":{\"errors\":[{\"column\":3," +
                        "\"error\":{\"message\":\"Названия или порядок полей в загруженном файле не соответствуют " +
                        "шаблону: 'Квота по штукам'\"}},{\"column\":4,\"error\":{\"message\":\"Названия или порядок " +
                        "полей в загруженном файле не соответствуют шаблону: 'Квота по паллетам'\"}}]}}",
                uploader);
        assertHeaderErrors("quotas_format_error_1p.xlsx", "{\"message\":\"Supply limit validation error\"," +
                        "\"type\":\"INVALID_DOCUMENT_CONTENT\",\"errorInfo\":{\"errors\":[{\"column\":6," +
                        "\"error\":{\"message\":\"Названия или порядок полей в загруженном файле не соответствуют " +
                        "шаблону: 'Квота по штукам'\"}},{\"column\":7,\"error\":{\"message\":\"Названия или порядок " +
                        "полей в загруженном файле не соответствуют шаблону: 'Квота по паллетам'\"}}]}}",
                uploader);
        assertHeaderErrors("quotas_format_error_movement.xlsx", "{\"message\":\"Supply limit validation error\"," +
                        "\"type\":\"INVALID_DOCUMENT_CONTENT\",\"errorInfo\":{\"errors\":[{\"column\":9," +
                        "\"error\":{\"message\":\"Названия или порядок полей в загруженном файле не соответствуют " +
                        "шаблону: 'Квота по штукам'\"}},{\"column\":10,\"error\":{\"message\":\"Названия или порядок " +
                        "полей в загруженном файле не соответствуют шаблону: 'Квота по паллетам'\"}}]}}",
                uploader);
    }

    private void assertHeaderErrors(@Nonnull String fileName,
                                    @Nonnull String expectedResult,
                                    @Nonnull Function<MockMultipartFile, ResultActions> uploader) throws Exception {
        MockMultipartFile file = getFileWithErrors(fileName);
        MvcResult mvcResult = uploader.apply(file)
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn();

        assertions.assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo(expectedResult);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits/before.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits/before.xml", assertionMode = NON_STRICT)
    public void uploadSupplyLimitsFileWithFormatErrors() throws Exception {
        assertCorrectFormatErrors(this::uploadSupply);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits/before.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits/before.xml", assertionMode = NON_STRICT)
    public void uploadWithdrawLimitsFileWithFormatErrors() throws Exception {
        assertCorrectFormatErrors(this::uploadWithdraw);
    }

    private void assertCorrectFormatErrors(@Nonnull Function<MockMultipartFile, ResultActions> uploader)
            throws Exception {
        assertFormatErrors("quotas_format_error_stop.xlsx", "{\"message\":\"Supply limit validation error\"," +
                        "\"type\":\"INVALID_DOCUMENT_CONTENT\",\"errorInfo\":{\"errors\":[{\"row\":3,\"column\":4," +
                        "\"error\":{\"message\":\"Здесь могут быть только цифры\"}},{\"row\":3,\"column\":7," +
                        "\"error\":{\"message\":\"Здесь могут быть только цифры\"}},{\"row\":3,\"column\":10," +
                        "\"error\":{\"message\":\"Здесь могут быть только цифры\"}},{\"row\":4,\"column\":1," +
                        "\"error\":{\"message\":\"Здесь могут быть только цифры\"}},{\"row\":5,\"column\":2," +
                        "\"error\":{\"message\":\"Дата должна быть указана в формате дд.мм.гггг\"}},{\"row\":6," +
                        "\"column\":3,\"error\":{\"message\":\"Здесь могут быть только цифры\"}},{\"row\":6," +
                        "\"column\":6,\"error\":{\"message\":\"Здесь могут быть только цифры\"}},{\"row\":6," +
                        "\"column\":9,\"error\":{\"message\":\"Здесь могут быть только цифры\"}}]}}",
                uploader);
       }

    private void assertFormatErrors(@Nonnull String fileName,
                                    @Nonnull String expectedResult,
                                    @Nonnull Function<MockMultipartFile, ResultActions> uploader) throws Exception {
        MockMultipartFile file = getFileWithErrors(fileName);
        MvcResult mvcResult = uploader.apply(file)
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
            .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
            equalTo(expectedResult));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits/before-exceeded-supply.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits/before-exceeded-supply.xml",
        assertionMode = NON_STRICT)
    public void uploadSupplyLimitsFileWithLimitExceededIsOk() throws Exception {
        assertQuotasExceededIsOk("quotas_exceeded_1p.xlsx", this::uploadSupply);
        assertQuotasExceededIsOk("quotas_exceeded_3p.xlsx", this::uploadSupply);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits/before-exceeded-withdraw.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits/before-exceeded-withdraw.xml",
            assertionMode = NON_STRICT)
    public void uploadWithdrawLimitsFileWithLimitExceededIsOk() throws Exception {
        assertQuotasExceededIsOk("quotas_exceeded_1p.xlsx", this::uploadWithdraw);
        assertQuotasExceededIsOk("quotas_exceeded_3p.xlsx", this::uploadWithdraw);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits-with-movement/before-exceeded-supply.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits-with-movement/before-exceeded-supply.xml",
            assertionMode = NON_STRICT)
    public void uploadMovementSupplyLimitsFileWithLimitExceededIsOk() throws Exception {
        assertQuotasExceededIsOk("quotas_exceeded_movement.xlsx", this::uploadMovementSupply);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:controller/upload-limits-with-movement/before-exceeded-withdraw.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/upload-limits-with-movement/before-exceeded-withdraw.xml",
            assertionMode = NON_STRICT)
    public void uploadMovementWithdrawLimitsFileWithLimitExceededIsOk() throws Exception {
        assertQuotasExceededIsOk("quotas_exceeded_movement.xlsx", this::uploadMovementWithdraw);
    }

    private void assertQuotasExceededIsOk(@Nonnull String fileName,
                                      @Nonnull Function<MockMultipartFile, ResultActions> uploader) throws Exception {
        MockMultipartFile file = getFileWithErrors(fileName);
        uploader.apply(file)
            .andExpect(status().isOk())
                .andDo(print())
            .andReturn();

    }

    @Test
    @DatabaseSetup("classpath:controller/upload-limits/before.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-limits/before.xml", assertionMode = NON_STRICT)
    public void uploadSupplyLimitsWrongFileType() throws Exception {
        assertCorrectUploadWithWrongFileType(this::uploadSupply);
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-limits/before.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-limits/before.xml", assertionMode = NON_STRICT)
    public void uploadWithdrawLimitsWrongFileType() throws Exception {
        assertCorrectUploadWithWrongFileType(this::uploadWithdraw);
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-limits-with-movement/before.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-limits-with-movement/before.xml", assertionMode = NON_STRICT)
    public void uploadMovementSupplyLimitsWrongFileType() throws Exception {
        assertCorrectUploadWithWrongFileType(this::uploadMovementSupply);
    }

    @Test
    @DatabaseSetup("classpath:controller/upload-limits-with-movement/before.xml")
    @ExpectedDatabase(value = "classpath:controller/upload-limits-with-movement/before.xml", assertionMode = NON_STRICT)
    public void uploadMovementWithdrawLimitsWrongFileType() throws Exception {
        assertCorrectUploadWithWrongFileType(this::uploadMovementWithdraw);
    }

    private void assertCorrectUploadWithWrongFileType(@Nonnull Function<MockMultipartFile, ResultActions> uploader)
            throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "wrong_file",
                "application/msword", getSystemResourceAsStream("wrong_file.doc"));

        MvcResult mvcResult = uploader.apply(file)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Unknown content type application/msword\"," +
                        "\"type\":\"INVALID_FILE_FORMAT\"}"));
    }

    private MockMultipartFile getFileWithErrors(String name) throws IOException {
        return getFile(ERR_FILE_PATH, name);
    }

    private MockMultipartFile getValidFile(String name) throws IOException {
        return getFile(VALID_FILE_PATH, name);
    }

    private MockMultipartFile getFile(String path, String name) throws IOException {
        final String resourceFileName = String.format("%s/%s", path, name);
        return new MockMultipartFile("file", name, FileExtension.XLSX.getMimeType(),
            getSystemResourceAsStream(resourceFileName));
    }

    private ResultActions uploadSupply(MockMultipartFile file) {
        return upload(file, "/supply-limits/upload-limits-file");
    }

    private ResultActions uploadWithdraw(MockMultipartFile file) {
        return upload(file, "/supply-limits/upload-withdraw-limits-file");
    }

    private ResultActions uploadMovementSupply(MockMultipartFile file) {
        return upload(file, "/supply-limits/upload-limits-file");
    }

    private ResultActions uploadMovementWithdraw(MockMultipartFile file) {
        return upload(file, "/supply-limits/upload-withdraw-limits-file");
    }

    private ResultActions upload(MockMultipartFile file, String url) {
        MockHttpServletRequestBuilder upload = multipart(url)
                .file(file);

        try {
            return mockMvc.perform(upload);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
