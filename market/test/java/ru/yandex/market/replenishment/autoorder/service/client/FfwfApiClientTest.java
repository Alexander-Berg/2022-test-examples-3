package ru.yandex.market.replenishment.autoorder.service.client;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.config.security.tvm.TvmService;
import ru.yandex.market.replenishment.autoorder.exception.BadRequestException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
public class FfwfApiClientTest extends FunctionalTest {

    private FfwfApiClient ffwfApiClient;

    @Value("${ffw-api.tvm-service-id}")
    private int tvmServiceId;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TvmService tvmService;

    private MockWebServer mockWebServer;

    @Before
    public void setUp() {
        this.mockWebServer = new MockWebServer();
        ffwfApiClient = new FfwfApiClient(
            tvmServiceId,
            mockWebServer.url("/").toString(),
            objectMapper,
            tvmService);
    }

    @Test
    public void testOK() {
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .throttleBody(64, 5, TimeUnit.MILLISECONDS)
                .setBody("{\"totalPages\":1,\"pageNumber\":0,\"totalElements\":2," +
                        "\"requestItems\":[{\"article\":\"131\",\"barcodes\":[\"1234567\",\"sasda121\"],\"count\":1," +
                        "\"holds\":{},\"boxCount\":1,\"boxCountSummary\":[],\"comment\":\"comment\"}," +
                        "{\"article\":\"77771\",\"barcodes\":[\"12345678\"],\"count\":2,\"holds\":{},\"boxCount\":1," +
                        "\"boxCountSummary\":[]}],\"totalCount\":3,\"size\":2}")
                .setResponseCode(200));
        Set<String> sskus = ffwfApiClient.getShadowSupplySskus(100500100L);
        assertThat(sskus, hasSize(2));
        assertThat(sskus, hasItems("131", "77771"));
    }

    @Test
    public void testOK_twoPages() {
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .throttleBody(64, 5, TimeUnit.MILLISECONDS)
                .setBody("{\"totalPages\":2,\"pageNumber\":0,\"totalElements\":3," +
                        "\"requestItems\":[{\"article\":\"131\",\"barcodes\":[\"1234567\",\"sasda121\"],\"count\":1," +
                        "\"holds\":{},\"boxCount\":1,\"boxCountSummary\":[],\"comment\":\"comment\"}," +
                        "{\"article\":\"77771\",\"barcodes\":[\"12345678\"],\"count\":2,\"holds\":{},\"boxCount\":1," +
                        "\"boxCountSummary\":[]}],\"totalCount\":3,\"size\":2}")
                .setResponseCode(200));

        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .throttleBody(64, 5, TimeUnit.MILLISECONDS)
                .setBody("{\"totalPages\":2,\"pageNumber\":1,\"totalElements\":3," +
                        "\"requestItems\":[{\"article\":\"321\",\"barcodes\":[\"123456789\",\"sasda121\"],\"count\":1," +
                        "\"holds\":{},\"boxCount\":1,\"boxCountSummary\":[],\"comment\":\"comment\"}]," +
                        "\"totalCount\":1,\"size\":1}")
                .setResponseCode(200));

        Set<String> sskus = ffwfApiClient.getShadowSupplySskus(100500100L);
        assertThat(sskus, hasSize(3));
        assertThat(sskus, hasItems("131", "77771", "321"));
    }

    @Test
    public void testNotFound() {
        mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .throttleBody(64, 5, TimeUnit.MILLISECONDS)
                .setBody("{\"message\":\"Failed to find [REQUEST] with id [100500100]\",\"resourceType\":\"REQUEST\"," +
                        "\"identifier\":\"100500100\"}")
                .setResponseCode(404));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> ffwfApiClient.getShadowSupplySskus(100500100L)
        );

        assertEquals("Error getting items of shadow supply with ID 100500100", exception.getMessage());
    }
}
