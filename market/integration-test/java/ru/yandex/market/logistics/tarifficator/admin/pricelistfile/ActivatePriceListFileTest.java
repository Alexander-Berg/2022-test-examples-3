package ru.yandex.market.logistics.tarifficator.admin.pricelistfile;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.tarifficator.base.AbstractPriceListFileActivationTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@DisplayName("Активация файла прайс-листа через админку")
class ActivatePriceListFileTest extends AbstractPriceListFileActivationTest {
    @Nonnull
    @Override
    protected ResultActions performPriceListFileActivation() throws Exception {
        return mockMvc.perform(
            post("/admin/price-list-files/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1}")
        );
    }

    @Nonnull
    @Override
    protected ResultMatcher successResult() {
        return TestUtils.noContent();
    }
}
