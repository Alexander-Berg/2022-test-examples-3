package ru.yandex.market.logistics.tarifficator.controller.withdrawtariff;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.tarifficator.base.AbstractUploadWithdrawPriceListFileTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@DisplayName("Загрузка файла заборного прайс-листа")
@ParametersAreNonnullByDefault
class WithdrawPriceListFileUploadTest extends AbstractUploadWithdrawPriceListFileTest {

    @Nonnull
    @Override
    protected ResultActions mockMvcPerform(MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/withdraw-price-list/files/tariff/1").file(file));
    }

    @Nonnull
    @Override
    protected ResultMatcher successResult() {
        return TestUtils.jsonContent(
            "controller/withdraw/price-list-files/response/upload_success_response.json",
            "createdAt",
            "updatedAt"
        );
    }

    @Nonnull
    @Override
    protected String getRequestParamName() {
        return "file";
    }
}
