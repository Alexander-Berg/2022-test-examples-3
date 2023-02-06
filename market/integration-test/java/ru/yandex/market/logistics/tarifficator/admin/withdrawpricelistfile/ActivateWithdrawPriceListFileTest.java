package ru.yandex.market.logistics.tarifficator.admin.withdrawpricelistfile;

import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.base.AbstractActivateWithdrawPriceListFileTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("Активировать файл заборного прайс-листа через админку")
public class ActivateWithdrawPriceListFileTest extends AbstractActivateWithdrawPriceListFileTest {
    @Override
    protected ResultActions activateFile(long priceListFileId) throws Exception {
        return mockMvc.perform(
            post("/admin/withdraw-price-list-files/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + priceListFileId + "}")
        );
    }
}
