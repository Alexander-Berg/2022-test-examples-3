package ru.yandex.market.logistics.nesu.controller.shoppickuppoint;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
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

import ru.yandex.market.logistics.nesu.jobs.producer.UpdateShopPickupPointProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.UpdateShopPickupPointTariffProducer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Обновление ПВЗ магазина и его тарифа")
@DatabaseSetup("/controller/shop-pickup-points/before/shop.xml")
@DatabaseSetup("/controller/shop-pickup-points/before/shop_pickup_point.xml")
@ExpectedDatabase(
    value = "/controller/shop-pickup-points/after/shop_pickup_point_id_mapping_empty.xml",
    assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
)
class UpdateShopPickupPointTest extends AbstractCreateOrUpdateShopPickupPointTest {

    @Autowired
    private UpdateShopPickupPointProducer updateShopPickupPointProducer;

    @Autowired
    private UpdateShopPickupPointTariffProducer updateShopPickupPointTariffProducer;

    @BeforeEach
    void setUp() {
        doNothing().when(updateShopPickupPointProducer).produceTask(anyLong());
        doNothing().when(updateShopPickupPointTariffProducer).produceTask(anyLong());
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(updateShopPickupPointProducer);
        verifyNoMoreInteractions(updateShopPickupPointTariffProducer);
    }

    @Test
    @DisplayName("Успешное обновление")
    @ExpectedDatabase(
        value = "/controller/shop-pickup-points/after/updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateSuccess() throws Exception {
        when(geoSearchClient.find(LONGITUDE + " " + LATITUDE)).thenReturn(List.of(geoObject(888), geoObject(213)));
        mockMvc.perform(requestBuilder(
                200,
                800,
                "controller/shop-pickup-points/request/update-shop-pickup-point-meta.json"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop-pickup-points/response/update-shop-pickup-point-meta.json"));
        verify(geoSearchClient).find(LONGITUDE + " " + LATITUDE);
        verify(updateShopPickupPointProducer).produceTask(800);
        verify(updateShopPickupPointTariffProducer).produceTask(800);
    }

    @Test
    @DisplayName("Успешное обновление с обогащением адресных полей")
    void updateAndEnrichAddressSuccess() throws Exception {
        try (var ignored = mockUnifierClient()) {
            mockMvc.perform(requestBuilder(
                    200,
                    800,
                    "controller/shop-pickup-points/request/update-and-enrich-address.json"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonContent("controller/shop-pickup-points/response/update-shop-pickup-point-meta.json"));
        }
        verify(geoSearchClient).find("-1.0 1.0");
        verify(geoSearchClient).find("1.0 -1.0");
        verify(updateShopPickupPointProducer).produceTask(800);
        verify(updateShopPickupPointTariffProducer).produceTask(800);
    }

    @Test
    @DisplayName("Успешное обновление с перевернутыми координатами")
    @ExpectedDatabase(
        value = "/controller/shop-pickup-points/after/updated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateWithFlippedCoordinatesSuccess() throws Exception {
        when(geoSearchClient.find(LATITUDE + " " + LONGITUDE)).thenReturn(List.of(geoObject(888)));
        when(geoSearchClient.find(LONGITUDE + " " + LATITUDE)).thenReturn(List.of(geoObject(888), geoObject(213)));
        mockMvc.perform(requestBuilder(
                200,
                800,
                "controller/shop-pickup-points/request/update-shop-pickup-point-meta-flipped-gps.json"
            ))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/shop-pickup-points/response/update-shop-pickup-point-meta.json"
            ));
        verify(geoSearchClient).find(LATITUDE + " " + LONGITUDE);
        verify(geoSearchClient).find(LONGITUDE + " " + LATITUDE);
        verify(updateShopPickupPointProducer).produceTask(800);
        verify(updateShopPickupPointTariffProducer).produceTask(800);
    }

    @Test
    @DisplayName("Нет записи по идентификатору")
    void noShopPickupPointMeta() throws Exception {
        mockMvc.perform(requestBuilder(200, 0))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_PICKUP_POINT_META] with ids [0]"));
    }

    @Test
    @DisplayName("Удален")
    @DatabaseSetup(
        value = "/controller/shop-pickup-points/before/deleted.xml",
        type = DatabaseOperation.REFRESH
    )
    void deleted() throws Exception {
        mockMvc.perform(requestBuilder(200, 800))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_PICKUP_POINT_META] with ids [800]"));
    }

    @Test
    @DisplayName("Не принадлежит магазину")
    @DatabaseSetup(
        value = "/controller/shop-pickup-points/before/does_not_belong_to_shop.xml",
        type = DatabaseOperation.REFRESH
    )
    void doesNotBelongToShop() throws Exception {
        mockMvc.perform(requestBuilder(200, 800))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_PICKUP_POINT_META] with ids [800]"));
    }

    @Nonnull
    private MockHttpServletRequestBuilder requestBuilder(
        long shopId,
        long shopPickupPointMetaId,
        String requestBodyFilePath
    ) {
        return MockMvcRequestBuilders.request(
                HttpMethod.PUT,
                String.format("/internal/shop/%d/pickup-point-meta/%d", shopId, shopPickupPointMetaId)
            )
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent(requestBodyFilePath));
    }

    @Nonnull
    @Override
    protected MockHttpServletRequestBuilder requestBuilder(long shopId) throws Exception {
        return requestBuilder(shopId, 800);
    }

    @Nonnull
    private MockHttpServletRequestBuilder requestBuilder(long shopId, long shopPickupPointMetaId) throws Exception {
        return request(
            HttpMethod.PUT,
            String.format("/internal/shop/%d/pickup-point-meta/%s", shopId, shopPickupPointMetaId),
            defaultMeta()
        );
    }
}
