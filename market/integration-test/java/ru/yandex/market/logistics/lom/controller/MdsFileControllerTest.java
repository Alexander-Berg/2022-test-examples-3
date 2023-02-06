package ru.yandex.market.logistics.lom.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
class MdsFileControllerTest extends AbstractContextualTest {

    private static final String TEST_URL = "http://localhost:8080/lom-doc-test-1.xml";
    private static final byte[] CONTENT = "test-content".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MdsS3Client mdsS3Client;

    @BeforeEach
    void init() throws MalformedURLException {
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(TEST_URL));
    }

    @Test
    @DisplayName("Загрузка файла")
    @DatabaseSetup(value = "/controller/mds/before/upload_file.xml")
    @ExpectedDatabase(value = "/controller/mds/after/upload_file.xml", assertionMode = NON_STRICT)
    void uploadFile() throws Exception {

        mockMvc.perform(
            multipart("/files")
                .file(createMultipartFile())
                .param("type", "ORDER_LABEL")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/mds/response/upload_file_success.json"));

        ResourceLocation resourceLocation = ResourceLocation.create("lom-doc-test", "1");

        ArgumentCaptor<StreamContentProvider> captor = ArgumentCaptor.forClass(StreamContentProvider.class);
        verify(mdsS3Client).upload(eq(resourceLocation), captor.capture());
        verify(mdsS3Client).getUrl(resourceLocation);
        verifyNoMoreInteractions(mdsS3Client);

        byte[] content = IOUtils.toByteArray(captor.getValue().getInputStream());
        softly.assertThat(content).isEqualTo(CONTENT);
    }

    @Test
    @DisplayName("Загрузка файла неизвестного типа")
    void uploadFileOfUnknownType() throws Exception {
        mockMvc.perform(
            multipart("/files")
                .file(createMultipartFile())
                .param("type", "UNKNOWN_TYPE")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/mds/response/upload_file_unknown_type.json"));
    }

    @Test
    @DisplayName("Загрузка некорректного файла")
    void uploadInvalidFile() throws Exception {
        MockMultipartFile file = spy(createMultipartFile());
        when(file.getInputStream()).thenThrow(IOException.class);

        mockMvc.perform(
            multipart("/files")
                .file(file)
                .param("type", "ORDER_LABEL")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/mds/response/upload_file_invalid_file.json"));
    }

    @Test
    @DisplayName("Получение информации о MDS файле")
    @DatabaseSetup(value = "/controller/mds/before/get_mds_file.xml")
    void getMdsFile() throws Exception {
        mockMvc.perform(get("/files/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/mds/response/get_mds_file_success.json"));
    }

    @Test
    @DisplayName("Получение информации о несуществующем MDS файле")
    void getMdsFileThatDoesNotExist() throws Exception {
        mockMvc.perform(get("/files/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/mds/response/get_mds_file_not_found.json"));
    }

    @Test
    @DisplayName("Получение информации об удалённом MDS файле")
    @DatabaseSetup(value = "/controller/mds/before/get_deleted_mds_file.xml")
    void getDeletedMdsFile() throws Exception {
        mockMvc.perform(get("/files/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/mds/response/get_mds_file_not_found.json"));
    }

    @Test
    @DisplayName("Удаление MDS файла")
    @DatabaseSetup(value = "/controller/mds/before/delete_mds_file.xml")
    @ExpectedDatabase(value = "/controller/mds/after/delete_mds_file.xml", assertionMode = NON_STRICT)
    void deleteMdsFile() throws Exception {
        mockMvc.perform(delete("/files/1"))
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Test
    @DisplayName("Удаление уже удалённого MDS файла")
    @DatabaseSetup(value = "/controller/mds/before/get_deleted_mds_file.xml")
    @ExpectedDatabase(value = "/controller/mds/before/get_deleted_mds_file.xml", assertionMode = NON_STRICT)
    void deleteDeletedMdsFile() throws Exception {
        mockMvc.perform(delete("/files/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/mds/response/get_mds_file_not_found.json"));
    }

    @Test
    @DisplayName("Удаление несуществующего MDS файла")
    void deleteMdsFileThatDoesNotExist() throws Exception {
        mockMvc.perform(delete("/files/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/mds/response/delete_mds_file_not_found.json"));
    }

    @DisplayName("Скачать MDS файл")
    @MethodSource("downloadFileArguments")
    @DatabaseSetup(value = "/controller/mds/before/get_mds_file.xml")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    void downloadFile(String path) throws Exception {
        mockMdsS3ClientDownload();
        mockMvc.perform(get(path, 1L))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string("Content-Disposition", "attachment;filename=\"original-filename.pdf\""))
            .andExpect(content().bytes(CONTENT));
    }

    @DisplayName("Скачать MDS файл с русскими буквами в названии")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("downloadFileArguments")
    @DatabaseSetup(value = "/controller/mds/before/get_mds_file_with_russian_letters.xml")
    void downloadFileWithRussianLetters(String path) throws Exception {
        mockMdsS3ClientDownload();
        mockMvc.perform(get(path, 1L))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(header().string(
                "Content-Disposition",
                "attachment;filename=\"%D0%98%D0%9C%D0%AF_%D0%A4%D0%90%D0%99%D0%9B%D0%90_label_1\""
            ))
            .andExpect(content().bytes(CONTENT));
    }

    @DisplayName("Скачать несуществующий MDS файл")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("downloadFileArguments")
    void downloadFileThatDoesNotExist(String path) throws Exception {
        mockMdsS3ClientDownload();
        mockMvc.perform(get(path, 1L))
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/mds/response/get_mds_file_not_found.json"));
    }

    @Nonnull
    private static Stream<Arguments> downloadFileArguments() {
        return Stream.of(
            Arguments.of("/files/{mdsFileId}/download"),
            Arguments.of("/admin/files/{mdsFileId}/download")
        );
    }

    private void mockMdsS3ClientDownload() {
        Answer<Void> answer = invocation -> {
            StreamCopyContentConsumer<OutputStream> argument = invocation.getArgument(1);
            argument.consume(new ByteArrayInputStream(CONTENT));
            return null;
        };
        when(mdsS3Client.download(eq(ResourceLocation.create("lom-doc-test", "1")), any())).thenAnswer(answer);
    }

    @Nonnull
    private MockMultipartFile createMultipartFile() {
        return new MockMultipartFile(
            "file",
            "original-filename.pdf",
            "application/pdf",
            CONTENT
        );
    }
}
