package ru.yandex.market.logistics.nesu.base;

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.nesu.dto.deliveryoptions.DeliveryOptionsFilter;

import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск вариантов доставки")
public abstract class AbstractTvmAuthDeliveryOptionsSearchTest extends AbstractDeliveryOptionsSearchTest {

    @Test
    @DisplayName("Не указан сендер")
    void noSender() throws Exception {
        search(defaultFilter(), null)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "senderId",
                "Failed to convert value of type 'null' to required type 'long'",
                "senderIdHolder",
                "typeMismatch"
            )));
    }

    @Nonnull
    @Override
    protected ResultActions search(@Nonnull Consumer<DeliveryOptionsFilter> filterAdjuster, @Nullable Long senderId)
        throws Exception {
        DeliveryOptionsFilter filter = new DeliveryOptionsFilter();
        filterAdjuster.accept(filter);
        MockHttpServletRequestBuilder requestBuilder = request(HttpMethod.PUT, uri(), filter)
            .param("userId", "100")
            .param("shopId", "100");
        if (senderId != null) {
            requestBuilder.param("senderId", String.valueOf(senderId));
        }
        return mockMvc.perform(requestBuilder);
    }

    @Nonnull
    protected abstract String uri();
}
