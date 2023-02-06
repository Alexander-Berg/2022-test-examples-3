package ru.yandex.market.logistics.tarifficator.admin.pricelistfile;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получить детальную информацию о файле прайс-листа через админку")
@DatabaseSetup("/controller/tariffs/db/search_prepare.xml")
@DatabaseSetup("/controller/admin/pricelistfiles/before/search_prepare.xml")
class GetPriceListFileDetailsTest extends AbstractContextualTest {

    @Test
    @DisplayName("Получить детальную информацию о файле прайс-листа")
    void getPriceListFilerDetailInfo() throws Exception {
        mockMvc.perform(get("/admin/price-list-files/1"))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/pricelistfiles/response/id_1_details.json"));
    }

    @Test
    @DisplayName("Получить детальную информацию о файле прайс-листа — файла не существует")
    void getPriceListFilerDetailInfoNotFound() throws Exception {
        mockMvc.perform(get("/admin/price-list-files/4"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PRICE_LIST_FILE] with ids [[4]]"));
    }

    @Test
    @DisplayName("Получить детальную информацию о файле активированного прайс-листа")
    void getPriceListFilerDetailInfoActivated() throws Exception {
        mockMvc.perform(get("/admin/price-list-files/6"))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/pricelistfiles/response/id_6_details.json"));
    }
}
