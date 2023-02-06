package ru.yandex.market.delivery.tracker.admin;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.delivery.tracker.AbstractContextualTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AdminTrackerControllerMassOperationTest extends AbstractContextualTest {

    private static final byte[] TEST_FILE_CONTENT = "Some test".getBytes(StandardCharsets.UTF_8);
    private static final String FILE_URL = "http://localhost:8080/file.xlsx";

    @Autowired
    private MdsS3Client mdsS3Client;

    @BeforeEach
    void setUp() throws Exception {
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(FILE_URL));
    }

    @Test
    @ExpectedDatabase(
        value = "/database/expected/admin_created_mass_add_checkpoints_task.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void uploadFileSuccess() throws Exception {
        uploadFile(createMockMultipartFile()).andExpect(status().isOk());
    }

    @Test
    void readFileFailure() throws Exception {
        IOException e = new IOException("Read file failure");
        MockMultipartFile mockMultipartFile = spy(createMockMultipartFile());
        doThrow(e).when(mockMultipartFile).getInputStream();
        uploadFile(mockMultipartFile).andExpect(status().isBadRequest());
    }

    @Test
    void uploadFileFailure() throws Exception {
        IllegalArgumentException e = new IllegalArgumentException("Upload file failure");
        doThrow(e).when(mdsS3Client).upload(any(), any());
        uploadFile(createMockMultipartFile()).andExpect(status().isInternalServerError());
    }

    private MockMultipartFile createMockMultipartFile() {
        return new MockMultipartFile(
            "request",
            "file.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            TEST_FILE_CONTENT
        );
    }

    private ResultActions uploadFile(MockMultipartFile mockMultipartFile) throws Exception {
        return mockMvc.perform(multipart("/admin/track/checkpoints/mass-create-checkpoints").file(mockMultipartFile));
    }
}
