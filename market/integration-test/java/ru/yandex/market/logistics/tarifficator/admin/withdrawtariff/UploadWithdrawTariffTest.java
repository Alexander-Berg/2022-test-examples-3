package ru.yandex.market.logistics.tarifficator.admin.withdrawtariff;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.tarifficator.base.AbstractUploadWithdrawPriceListFileTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

@DisplayName("Загрузка файла заборного прайс-листа через админку")
@ParametersAreNonnullByDefault
class UploadWithdrawTariffTest extends AbstractUploadWithdrawPriceListFileTest {
    @Nonnull
    @Override
    protected ResultActions mockMvcPerform(MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/admin/withdraw-tariffs/upload/1").file(file));
    }

    @Nonnull
    @Override
    protected ResultMatcher successResult() {
        return TestUtils.noContent();
    }

    @Nonnull
    @Override
    protected String getRequestParamName() {
        return "request";
    }
}
