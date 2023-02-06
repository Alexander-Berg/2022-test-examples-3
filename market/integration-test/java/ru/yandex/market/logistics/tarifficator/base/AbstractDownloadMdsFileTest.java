package ru.yandex.market.logistics.tarifficator.base;

import java.io.InputStream;
import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractDownloadMdsFileTest extends AbstractContextualTest {
    @Autowired
    private MdsS3Client mdsS3Client;

    @Test
    @DisplayName("Скачивание файла")
    @DatabaseSetup("/controller/mds/before.xml")
    void fileDownload() throws Exception {
        mockMdsS3Client("controller/revisions/response/dataset_1.xml");

        downloadFile(1)
            .andExpect(status().isOk())
            .andExpect(header().string(
                "Content-Type",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
            .andExpect(header().string("Content-Disposition", "attachment;filename=\"originalFileName\""))
            .andExpect(content().xml(extractFileContent("controller/revisions/response/dataset_1.xml")));
    }

    @Test
    @DisplayName("Скачивание файла с русскими буквами в названии")
    @DatabaseSetup("/controller/mds/before_with_russian_letters.xml")
    void fileDownloadWithRussianLettersInName() throws Exception {
        mockMdsS3Client("controller/revisions/response/dataset_1.xml");

        downloadFile(1)
            .andExpect(status().isOk())
            .andExpect(header().string(
                "Content-Type",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
            .andExpect(header().string(
                "Content-Disposition",
                "attachment;filename=\"%D0%98%D0%9C%D0%AF_%D0%A4%D0%90%D0%99%D0%9B%D0%90_dataset_1\""
            ))
            .andExpect(content().xml(extractFileContent("controller/revisions/response/dataset_1.xml")));
    }

    @Test
    @DisplayName("Скачивание несуществующего файла")
    void fileNotFoundDownload() throws Exception {
        downloadFile(1)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [MDS_FILE] with ids [[1]]"));
    }

    private void mockMdsS3Client(String responseFilePath) {
        doAnswer(invocation -> {
            StreamCopyContentConsumer consumer = invocation.getArgument(1);
            InputStream is = Objects.requireNonNull(getSystemResourceAsStream(responseFilePath));
            consumer.consume(is);
            return null;
        }).when(mdsS3Client).download(any(), any());
    }

    protected abstract ResultActions downloadFile(long id) throws Exception;
}
