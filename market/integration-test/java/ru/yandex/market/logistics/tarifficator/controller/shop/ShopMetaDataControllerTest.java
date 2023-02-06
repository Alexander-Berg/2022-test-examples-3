package ru.yandex.market.logistics.tarifficator.controller.shop;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
class ShopMetaDataControllerTest extends AbstractContextualTest {

    @Test
    @DisplayName("Сохранение новых метаданных магазина")
    @DatabaseSetup("/controller/shop/shop-meta-data/db/saveShopMetaData.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/shop-meta-data/db/saveNewShopMetaData.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void saveNewShopMetaData() throws Exception {
        long shopId = 775L;

        saveShopMetaData(shopId, "controller/shop/shop-meta-data/json/saveNewShopMeta.request.json")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/shop-meta-data/json/saveNewShopMeta.response.json"));
    }

    @Test
    @DisplayName("Обновление метаданных для уже существующего магазина")
    @DatabaseSetup("/controller/shop/shop-meta-data/db/saveShopMetaData.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/shop-meta-data/db/saveExistingShopMetaData.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void saveExistingShopMetaData() throws Exception {
        long shopId = 774L;

        saveShopMetaData(shopId, "controller/shop/shop-meta-data/json/updateShopMeta.request.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Проверка на валидность метаданных")
    void shopMetaRequestIsInvalidNoCurrency() throws Exception {
        long shopId = 774L;
        String expectedErrorMessage =
            "Field: 'currency', message: 'must not be null'";

        saveShopMetaData(
            shopId,
            "controller/shop/shop-meta-data/json/shopMetaRequestIsInvalidNoCurrency.request.json"
        )
            .andExpect(status().is4xxClientError())
            .andExpect(
                errorMessage(String.format("Following validation errors occurred:\n%s", expectedErrorMessage))
            );
    }

    @Test
    @DisplayName("Проверка на валидность метаданных")
    void shopMetaRequestIsInvalidNoRegion() throws Exception {
        long shopId = 774L;
        String expectedErrorMessage =
            "Field: 'localRegion', message: 'must not be null'";

        saveShopMetaData(
            shopId,
            "controller/shop/shop-meta-data/json/shopMetaRequestIsInvalidNoRegion.request.json"
        )
            .andExpect(status().is4xxClientError())
            .andExpect(
                errorMessage(String.format("Following validation errors occurred:\n%s", expectedErrorMessage))
            );
    }

    @Test
    @DisplayName("Обновление тарифов")
    @DatabaseSetup("/controller/shop/shop-meta-data/db/updateTariffs.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/shop-meta-data/db/updateTariffs.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateTariffs() throws Exception {
        long shopId = 774L;

        saveShopMetaData(shopId, "controller/shop/shop-meta-data/json/updateTariffs.request.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Сохранение локального региона доставки магазина")
    @DatabaseSetup("/controller/shop/shop-meta-data/db/saveLocalRegion.before.xml")
    @ExpectedDatabase(
            value = "/controller/shop/shop-meta-data/db/saveLocalRegion.after.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void saveLocalRegion() throws Exception {
        long shopId = 774L;

        saveLocalRegion(shopId, "controller/shop/shop-meta-data/json/saveLocalRegion.request.json")
                .andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions saveShopMetaData(long shopId, String requestFilePath) throws Exception {
        return saveShopMetaDataWithContent(shopId, extractFileContent(requestFilePath));
    }

    @Nonnull
    private ResultActions saveShopMetaDataWithContent(long shopId, String requestJson) throws Exception {
        return mockMvc.perform(
            post("/v2/shops/" + shopId + "/meta?_user_id=100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        );
    }

    @Nonnull
    private ResultActions saveLocalRegion(long shopId, String requestFilePath) throws Exception {
        return saveLocalRegionWithContent(shopId, extractFileContent(requestFilePath));
    }

    @Nonnull
    private ResultActions saveLocalRegionWithContent(long shopId, String requestJson) throws Exception {
        return mockMvc.perform(
                put("/v2/shops/" + shopId + "/local-region?_user_id=100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)
        );
    }
}
