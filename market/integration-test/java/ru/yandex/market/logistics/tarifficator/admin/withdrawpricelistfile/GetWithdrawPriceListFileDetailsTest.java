package ru.yandex.market.logistics.tarifficator.admin.withdrawpricelistfile;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение деталей файла заборного прайс-листа через админку")
@DatabaseSetup("/controller/admin/withdrawtariffs/db/before/search_prepare.xml")
@DatabaseSetup("/controller/admin/withdrawpricelistfiles/db/before/search_prepare.xml")
class GetWithdrawPriceListFileDetailsTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получить детальную информацию о файле заборного прайс-листа")
    void getPriceListFileDetailInfo() throws Exception {
        mockMvc.perform(get("/admin/withdraw-price-list-files/1"))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/withdrawpricelistfiles/response/id_1_details.json"));
    }

    @Test
    @DisplayName("Получить детальную информацию о файле заборного прайс-листа — файла не существует")
    void getPriceListFileDetailInfoNotFound() throws Exception {
        mockMvc.perform(get("/admin/withdraw-price-list-files/4"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WITHDRAW_PRICE_LIST_FILE] with ids [[4]]"));
    }

    @Test
    @DisplayName("Получить детальную информацию о файле активированного заборного прайс-листа")
    void getPriceListFileDetailInfoActivated() throws Exception {
        mockMvc.perform(get("/admin/withdraw-price-list-files/2"))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/withdrawpricelistfiles/response/id_2_details.json"));
    }
}
