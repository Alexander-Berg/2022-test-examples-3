package ru.yandex.market.mbi.open.api.client;

import java.io.File;
import java.net.URL;
import java.util.Objects;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.open.api.client.model.DefaultFeedResponse;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerRequest;
import ru.yandex.market.mbi.open.api.client.model.ReplicatePartnerResponse;
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MbiOpenApiClientTest {
    private static WireMockServer wm;

    private final MbiOpenApiClient client = new MbiOpenApiClient.Builder().baseUrl("http://localhost:9000").build();

    @BeforeAll
    public static void setup() {
        wm = new WireMockServer(options().port(9000).withRootDirectory(Objects.requireNonNull(getClassPathFile(
                "wiremock")).getAbsolutePath()));
        wm.start();
    }

    @AfterAll
    public static void tearDown() {
        wm.shutdown();
    }

    @DisplayName("Запрос выполнился с ошибкой. Сервер вернул код 400")
    @Test
    public void get400ErrorTest() {
        MbiOpenApiClientResponseException thrown = assertThrows(MbiOpenApiClientResponseException.class,
                () -> client.registerPartnerNesu(10L, 101L, "sklad", "110"));
        assertEquals(400, thrown.getHttpErrorCode());
        assertEquals("Bad Request", thrown.getMessage());
        assertNotNull(thrown.getApiError());
        assertEquals("You are wrong", thrown.getApiError().getMessage());
    }

    @DisplayName("Запрос выполнился с ошибкой. Сервер вернул код 500")
    @Test
    public void get500ErrorTest() {
        MbiOpenApiClientResponseException thrown = assertThrows(MbiOpenApiClientResponseException.class,
                () -> client.registerPartnerNesu(10L, 100L, "sklad", "1000"));
        assertEquals(500, thrown.getHttpErrorCode());
        assertEquals("Server Error", thrown.getMessage());
        assertNotNull(thrown.getApiError());
        assertEquals("You are wrong", thrown.getApiError().getMessage());
    }

    @DisplayName("Создание дефолтного фида")
    @Test
    public void createDefaultFeed() {
        DefaultFeedResponse response = client.requestPartnerFeedDefault(10L, 200L);
        assertEquals(200, response.getPartnerId());
        assertEquals(1, response.getFeedId());
    }

    @DisplayName("репликация dbs партнера")
    @Test
    public void replicateDbsPartner() {
        ReplicatePartnerResponse response = client.replicateDbsPartner(10L,
                new ReplicatePartnerRequest()
                        .partnerDonorId(101L)
                        .regionId(213L)
                        .warehouseName("Склад")
        );
        assertEquals(200, response.getPartnerId());
    }

    @DisplayName("репликация партнера")
    @Test
    public void replicatePartner() {
        ReplicatePartnerResponse response = client.replicatePartner(
                10L,
                new ReplicatePartnerRequest().partnerDonorId(101L)
        );
        assertEquals(200, response.getPartnerId());
    }

    /**
     * Получение файла из classpath
     *
     * @param path путь к файлу
     */
    private static File getClassPathFile(String path) {
        ClassLoader classLoader = MbiOpenApiClientTest.class.getClassLoader();
        URL url = classLoader.getResource(path);
        if (url == null) {
            return null;
        } else {
            return new File(url.getFile());
        }

    }

}
