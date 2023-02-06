package ru.yandex.market.logistics.tarifficator.controller.pricelistfile;

import org.junit.jupiter.api.DisplayName;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.base.AbstractPriceListFileProcessingRetryTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("Перезапуск разбора файла прайс-листа")
public class PriceListFileProcessingRetryTest extends AbstractPriceListFileProcessingRetryTest {
    @Override
    protected ResultActions retryPriceListFileProcessing(long id) throws Exception {
        return mockMvc.perform(post("/price-list/files/" + id + "/retry"));
    }
}
