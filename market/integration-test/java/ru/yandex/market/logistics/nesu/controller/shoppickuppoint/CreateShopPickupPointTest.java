package ru.yandex.market.logistics.nesu.controller.shoppickuppoint;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.logistics.nesu.jobs.producer.CreateShopPickupPointProducer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Создание ПВЗ магазина и его тарифа")
@DatabaseSetup("/controller/shop-pickup-points/before/shop.xml")
@ExpectedDatabase(
    value = "/controller/shop-pickup-points/after/shop_pickup_point_id_mapping_empty.xml",
    assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
)
class CreateShopPickupPointTest extends AbstractCreateOrUpdateShopPickupPointTest {

    @Autowired
    private CreateShopPickupPointProducer createShopPickupPointProducer;

    @BeforeEach
    void setUp() {
        doNothing().when(createShopPickupPointProducer).produceTask(anyLong());
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(createShopPickupPointProducer);
    }

    @Test
    @DisplayName("Успешное создание")
    void createSuccess() throws Exception {
        when(geoSearchClient.find(LONGITUDE + " " + LATITUDE)).thenReturn(List.of(geoObject(888), geoObject(213)));
        mockMvc.perform(requestBuilder(
                200,
                "controller/shop-pickup-points/request/create-shop-pickup-point-meta.json"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop-pickup-points/response/create-shop-pickup-point-meta.json"));
        verify(geoSearchClient).find(LONGITUDE + " " + LATITUDE);
        verify(createShopPickupPointProducer).produceTask(1);
    }

    @Test
    @DisplayName("Успешное создание с перевернутыми координатами")
    void createWithFlippedCoordinatesSuccess() throws Exception {
        when(geoSearchClient.find(LATITUDE + " " + LONGITUDE)).thenReturn(List.of());
        when(geoSearchClient.find(LONGITUDE + " " + LATITUDE)).thenReturn(List.of(geoObject(213)));
        mockMvc.perform(requestBuilder(
                200,
                "controller/shop-pickup-points/request/create-shop-pickup-point-meta-flipped-gps.json"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/shop-pickup-points/response/create-shop-pickup-point-meta.json"
            ));
        verify(geoSearchClient).find(LATITUDE + " " + LONGITUDE);
        verify(geoSearchClient).find(LONGITUDE + " " + LATITUDE);
        verify(createShopPickupPointProducer).produceTask(1);
    }

    @Test
    @DisplayName("Успешное создание с обновлением адресных полей")
    void createAndEnrichAddressSuccess() throws Exception {
        try (var ignored = mockUnifierClient()) {
            mockMvc.perform(requestBuilder(
                    200,
                    "controller/shop-pickup-points/request/create-and-enrich-address.json"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/shop-pickup-points/response/create-shop-pickup-point-meta.json"));
        }
        verify(geoSearchClient).find("-1.0 1.0");
        verify(geoSearchClient).find("1.0 -1.0");
        verify(createShopPickupPointProducer).produceTask(1);
    }

    @Test
    @DisplayName("Повторный запрос на создание")
    @DatabaseSetup("/controller/shop-pickup-points/before/shop_pickup_point.xml")
    void createAlreadyExistingEntity() throws Exception {
        mockMvc.perform(requestBuilder(
                200,
                "controller/shop-pickup-points/request/shop-pickup-point-meta-already-exists.json"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/shop-pickup-points/response/shop-pickup-point-meta-already-exists.json"
            ));
    }

    @Nonnull
    private MockHttpServletRequestBuilder requestBuilder(long shopId, String requestBodyFilePath) {
        return MockMvcRequestBuilders.request(
                HttpMethod.POST,
                String.format("/internal/shop/%d/pickup-point-meta", shopId)
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent(requestBodyFilePath));
    }

    @Nonnull
    @Override
    protected MockHttpServletRequestBuilder requestBuilder(long shopId) throws Exception {
        return request(HttpMethod.POST, String.format("/internal/shop/%d/pickup-point-meta", shopId), defaultMeta());
    }
}
