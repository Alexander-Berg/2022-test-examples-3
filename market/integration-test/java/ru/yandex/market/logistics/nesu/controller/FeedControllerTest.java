package ru.yandex.market.logistics.nesu.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.enums.FileExtension;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

// todo: Распространить dbQueue.threadCountPerQueue=0 на весь проект https://st.yandex-team.ru/DELIVERY-16574
@TestPropertySource(properties = "dbQueue.processingDisabled=false")
class FeedControllerTest extends AbstractContextualTest {

    private static final String TEST_URL = "http://localhost:8080/feed_document_1.xml";

    @Autowired
    private MdsS3Client mdsS3Client;

    @BeforeEach
    void init() throws MalformedURLException {
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(TEST_URL));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DisplayName("Успешно загрузить фид")
    @DatabaseSetup("/controller/feed/before/before_feed_upload.xml")
    @ExpectedDatabase(value = "/controller/feed/after/after_feed_upload.xml", assertionMode = NON_STRICT)
    void uploadFeed() throws Exception {
        try (InputStream fileStream = getSystemResourceAsStream("controller/feed/feed_file.xml")) {
            MockMultipartFile file = mockMultipartFile(fileStream, FileExtension.XML.getMimeTypes().get(0));
            createFeed(file)
                .andExpect(status().isMethodNotAllowed())
                .andExpect(errorMessage("Method is no longer allowed"));
        }
    }

    @Test
    @DisplayName("Загрузить файл фида, удалить его и обработать задачу из очереди")
    @DatabaseSetup("/controller/feed/before/before_feed_upload.xml")
    void processDeletedFeed() throws Exception {
        try (InputStream fileStream = getSystemResourceAsStream("controller/feed/feed_file.xml")) {
            MockMultipartFile file = mockMultipartFile(fileStream, FileExtension.XML.getMimeTypes().get(0));
            createFeed(file)
                .andExpect(status().isMethodNotAllowed())
                .andExpect(errorMessage("Method is no longer allowed"));
        }
    }

    @Test
    @DisplayName("Получить все фиды сендера")
    @DatabaseSetup("/controller/feed/before/before_get_feeds.xml")
    void getFeeds() throws Exception {
        getSenderFeeds(1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/feed/get_feeds_response.json"));
    }

    @Test
    @DisplayName("Получить фиды несуществующего сендера")
    @DatabaseSetup("/controller/feed/before/before_get_feeds.xml")
    void getFeedsOfNonExistingShop() throws Exception {
        getSenderFeeds(2L)
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/feed/sender_not_found_response.json"));
    }

    @Test
    @DisplayName("Получить фид по идентификатору")
    @DatabaseSetup("/controller/feed/before/before_get_feeds.xml")
    void getFeed() throws Exception {
        getFeedByIdAndSenderId(1L, 1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/feed/get_feed_response.json"));
    }

    @Test
    @DisplayName("Получить несуществующий фид")
    @DatabaseSetup("/controller/feed/before/before_get_feeds.xml")
    void getNonExistingFeed() throws Exception {
        getFeedByIdAndSenderId(4L, 1L)
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/feed/feed_not_found_response.json"));
    }

    @Test
    @DisplayName("Получить фид другого магазина")
    @DatabaseSetup("/controller/feed/before/before_get_feeds.xml")
    void getFeedWrongSender() throws Exception {
        getFeedByIdAndSenderId(1L, 2L)
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/feed/feed_wrong_sender_response.json"));
    }

    @Test
    @DisplayName("Удалить несуществующий фид")
    @DatabaseSetup("/controller/feed/before/before_delete_feed.xml")
    @ExpectedDatabase(value = "/controller/feed/before/before_delete_feed.xml", assertionMode = NON_STRICT)
    void deleteNonExistingFeed() throws Exception {
        deleteFeedByIdAndSenderId(4L, 1L)
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/feed/feed_not_found_response.json"));
    }

    @Test
    @DisplayName("Удалить фид другого магазина")
    @DatabaseSetup("/controller/feed/before/before_delete_feed.xml")
    @ExpectedDatabase(value = "/controller/feed/before/before_delete_feed.xml", assertionMode = NON_STRICT)
    void deleteFeedWrongSender() throws Exception {
        deleteFeedByIdAndSenderId(1L, 2L)
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/feed/feed_wrong_sender_response.json"));
    }

    @Test
    @DisplayName("Удалить фид")
    @DatabaseSetup("/controller/feed/before/before_delete_feed.xml")
    @ExpectedDatabase(value = "/controller/feed/after/after_delete_feed.xml", assertionMode = NON_STRICT)
    void deleteFeed() throws Exception {
        deleteFeedByIdAndSenderId(1L, 1L)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Получить все фиды магазина")
    @DatabaseSetup("/controller/feed/before/before_get_all_feeds.xml")
    void getAllFeeds() throws Exception {
        mockMvc.perform(get("/back-office/feeds/all")
            .param("userId", "1")
            .param("shopId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/feed/get_all_feeds_response.json"));
    }

    @Test
    @DisplayName("Получить все фиды не существующего магазина")
    @DatabaseSetup("/controller/feed/before/before_get_all_feeds.xml")
    void getAllFeedsOfNotExistingShop() throws Exception {
        mockMvc.perform(get("/back-office/feeds/all")
            .param("userId", "1")
            .param("shopId", "123")
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/feed/shop_not_found_response.json"));
    }

    @Nonnull
    private MockMultipartFile mockMultipartFile(InputStream inputStream, String contentType) throws IOException {
        return new MockMultipartFile(
            "file",
            "file",
            contentType,
            inputStream
        );
    }

    @Nonnull
    private ResourceLocation resourceLocation() {
        return ResourceLocation.create("nesu", "feed_document_1.xml");
    }

    @Nonnull
    private ResultActions deleteFeedByIdAndSenderId(long feedId, long senderId) throws Exception {
        return mockMvc.perform(delete("/back-office/feeds/" + feedId)
            .param("userId", "1")
            .param("shopId", "1")
            .param("senderId", String.valueOf(senderId))
        );
    }

    @Nonnull
    private ResultActions getFeedByIdAndSenderId(long feedId, long senderId) throws Exception {
        return mockMvc.perform(get("/back-office/feeds/" + feedId)
            .param("userId", "1")
            .param("shopId", "1")
            .param("senderId", String.valueOf(senderId))
        );
    }

    @Nonnull
    private ResultActions getSenderFeeds(long senderId) throws Exception {
        return mockMvc.perform(get("/back-office/feeds")
            .param("userId", "1")
            .param("shopId", "1")
            .param("senderId", String.valueOf(senderId))
        );
    }

    @Nonnull
    private ResultActions createFeed(MockMultipartFile file) throws Exception {
        return mockMvc.perform(
            multipart("/back-office/feeds")
                .file(file)
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
        );
    }
}
