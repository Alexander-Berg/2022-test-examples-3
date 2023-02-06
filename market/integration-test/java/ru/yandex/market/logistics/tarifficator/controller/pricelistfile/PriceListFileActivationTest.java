package ru.yandex.market.logistics.tarifficator.controller.pricelistfile;

import javax.annotation.Nonnull;

import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.tarifficator.base.AbstractPriceListFileActivationTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class PriceListFileActivationTest extends AbstractPriceListFileActivationTest {
    @Nonnull
    @Override
    protected ResultActions performPriceListFileActivation() throws Exception {
        return mockMvc.perform(post("/price-list/files/1/activate"));
    }

    @Nonnull
    @Override
    protected ResultMatcher successResult() {
        return TestUtils.jsonContent(
            "controller/price-list-files/response/activate-manual-response.json",
            "updatedAt"
        );
    }
}
