package ru.yandex.market.logistics.tarifficator.admin.pricelistfile;

import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.base.AbstractPriceListFileProcessingRetryTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("Перезапуск разбора файла прайс-листа через админку")
class PriceListFileProcessingRetryTest extends AbstractPriceListFileProcessingRetryTest {
    @Override
    protected ResultActions retryPriceListFileProcessing(long id) throws Exception {
        return mockMvc.perform(post("/admin/price-list-files/retry")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"id\":" + id + "}")
        );
    }
}
