package ru.yandex.market.logistics.nesu.admin.logistic_point_availability;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.nesu.utils.TvmClientApiTestUtil;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.TvmClientApiTestUtil.mockTvmClientApiUserTicket;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тест на создание задачи на массовое обновление доступностей")
@DatabaseSetup("/repository/logistic-point-availability/before/prepare_data.xml")
class LogisticPointAvailabilityMultipleUpdateTest extends BaseLogisticPointAvailabilityTest {

    private static final String FILE_NAME = "fileName";

    private static final byte[] CONTENT = new byte[5];

    @Autowired
    private MdsS3Client mdsS3Client;

    @SpyBean
    private TvmClientApi tvmClientApi;

    @BeforeEach
    void setup() {
        mockTvmClientApiUserTicket(tvmClientApi, 1234L);
    }

    @Test
    @DisplayName("Успех обработки файла на обновления")
    @ExpectedDatabase(
        value = "/controller/admin/logistic-point-availability/multiple_update/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() throws Exception {
        postMultipartFile()
            .andExpect(status().isOk())
            .andExpect(content().string("1"));

        verify(mdsS3Client).upload(
            eq(ResourceLocation.create("nesu", "FILE_PROCESSING_TASK_1_fileName")),
            any(StreamContentProvider.class)
        );
    }

    @Test
    @DisplayName("Ошибка MDS'a")
    @ExpectedDatabase(
        value = "/controller/admin/logistic-point-availability/multiple_update/empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failMds() throws Exception {
        doThrow(new IllegalArgumentException("oops"))
            .when(mdsS3Client).upload(any(ResourceLocation.class), any(StreamContentProvider.class));

        postMultipartFile()
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Invalid byte stream or MDS client error"));

        verify(mdsS3Client).upload(
            eq(ResourceLocation.create("nesu", "FILE_PROCESSING_TASK_1_fileName")),
            any(StreamContentProvider.class)
        );
    }

    @Nonnull
    private ResultActions postMultipartFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "request",
            FILE_NAME,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            CONTENT
        );
        return mockMvc.perform(
            multipart("/admin/logistic-point-availability/update-logistic-point-availabilities")
                .file(file)
                .headers(TvmClientApiTestUtil.USER_HEADERS)
        );
    }
}
