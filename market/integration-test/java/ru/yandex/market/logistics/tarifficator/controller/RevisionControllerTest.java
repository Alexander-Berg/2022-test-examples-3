package ru.yandex.market.logistics.tarifficator.controller;

import java.io.InputStream;
import java.util.Objects;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/tags/tags.xml")
@DisplayName("Интеграционный тест контроллера RevisionController")
class RevisionControllerTest extends AbstractContextualTest {

    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @AfterEach
    void afterEach() {
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DisplayName("Получение последнего поколения")
    @DatabaseSetup("/tariffs/courier_without_active_price_lists_1.xml")
    @DatabaseSetup(value = "/tariffs/courier_300.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/tariffs/post_200.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/tariffs/pick_up_100.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/tags/relations/add-market-delivery-to-tariff-100.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/revisions/revision_with_tariffs_100_200_300.xml", type = DatabaseOperation.INSERT)
    void getLast() throws Exception {
        mockMvc.perform(
            get("/revisions/last")
        )
            .andExpect(status().isOk())
            .andExpect(content().json(
                extractFileContent("controller/revisions/response/last_success.json"))
            );
    }

    @Test
    @DisplayName("Получение пустого поколения")
    @DatabaseSetup("/controller/revisions/db/before/empty.xml")
    void getLastEmpty() throws Exception {
        mockMvc.perform(
            get("/revisions/last")
        )
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage(
                "Revision with id [1] and hash [6255c3603b8lk1d401d9d83757ed433b] "
                    + "failed validation due to its items set is empty"
            ));
    }

    @Test
    @DisplayName("Ни одно поколение еще не построено")
    void getLastNotExist() throws Exception {
        mockMvc.perform(
            get("/revisions/last")
        )
            .andExpect(status().is4xxClientError())
            .andExpect(errorMessage("Failed to find [REVISION] with ids [[1]]"));
    }

    @Test
    @DisplayName("Успешное получение выгрузки для версии тарифа")
    @DatabaseSetup("/controller/revisions/db/before/last_success.xml")
    void getDataset() throws Exception {
        mockMdsS3Client("controller/revisions/response/dataset_1.xml");

        mockMvc.perform(
            get("/revisions/tariff/1/6244c2603b8ca1d401d9d50757ed488b")
        )
            .andExpect(status().isOk())
            .andExpect(content().xml(
                extractFileContent("controller/revisions/response/dataset_1.xml"))
            );

        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Успешное получение непубличной выгрузки себестоимостей для версии тарифа")
    @DatabaseSetup("/tariffs/courier_without_active_price_lists_1.xml")
    @DatabaseSetup(value = "/tariffs/courier_300.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/tariffs/post_200.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/tariffs/pick_up_100.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/tags/relations/add-market-delivery-to-tariff-100.xml", type = DatabaseOperation.INSERT)
    @DatabaseSetup(value = "/revisions/revision_with_tariffs_100_200_300.xml", type = DatabaseOperation.INSERT)
    void getNonPublicDataset() throws Exception {
        mockMdsS3Client("controller/revisions/response/dataset_100000000100.xml");

        mockMvc.perform(
            get("/revisions/tariff/100000000100/6244c2603b8ca1d401d9d50757ed488b")
        )
            .andExpect(status().isOk())
            .andExpect(content().xml(
                extractFileContent("controller/revisions/response/dataset_100000000100.xml"))
            );

        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Не удалось скачать файл выгрузки из MDS хранилища")
    @DatabaseSetup("/controller/revisions/db/before/last_success.xml")
    void getDatasetCannotDownload() throws Exception {
        doThrow(new MdsS3Exception("Cannot download file")).when(mdsS3Client).download(any(), any());

        mockMvc.perform(
            get("/revisions/tariff/1/6244c2603b8ca1d401d9d50757ed488b")
        )
            .andExpect(status().is5xxServerError())
            .andExpect(errorMessage("Cannot download revision item file 1002"));

        verifyMdsS3Client();
    }

    @Test
    @DisplayName("Выгрузка не найдена")
    void notFoundDataset() throws Exception {
        mockMvc.perform(
            get("/revisions/tariff/1/6244c2603b8ca1d401d9d50757ed488b")
        )
            .andExpect(status().is4xxClientError())
            .andExpect(errorMessage(
                "Failed to find [DATASET_FILE] with tariffId [1] and hash [6244c2603b8ca1d401d9d50757ed488b]"
            ));
    }

    @Test
    @SneakyThrows
    @DisplayName("Получение файла если было несколько ревизий с одинаковым хэшем")
    @DatabaseSetup("/controller/revisions/db/before/last_success.xml")
    @DatabaseSetup(
        value = "/controller/revisions/db/before/additional_revision.xml",
        type = DatabaseOperation.REFRESH
    )
    void getDatasetForMultipleRevisionWithEqualHash() {
        mockMdsS3Client("controller/revisions/response/dataset_1.xml");

        mockMvc.perform(
            get("/revisions/tariff/1/6244c2603b8ca1d401d9d50757ed488b")
        )
            .andExpect(status().isOk())
            .andExpect(content().xml(
                extractFileContent("controller/revisions/response/dataset_1.xml"))
            );

        verifyMdsS3Client();
    }

    private void mockMdsS3Client(String responseFilePath) {
        doAnswer(invocation -> {
            StreamCopyContentConsumer consumer = invocation.getArgument(1);
            InputStream is = Objects.requireNonNull(getSystemResourceAsStream(responseFilePath));
            consumer.consume(is);
            return null;
        }).when(mdsS3Client).download(any(), any());
    }

    private void verifyMdsS3Client() {
        verify(mdsS3Client).download(
            eq(createResourceLocation()),
            any(StreamCopyContentConsumer.class)
        );
    }

    private ResourceLocation createResourceLocation() {
        return resourceLocationFactory.createLocation("dataset_1.xml");
    }
}
