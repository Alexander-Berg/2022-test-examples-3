package ru.yandex.market.logistics.nesu.api.order;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.api.model.order.ApiOrderSearchFilter;
import ru.yandex.market.logistics.nesu.base.order.AbstractSearchOrdersTest;
import ru.yandex.market.logistics.nesu.dto.filter.AbstractOrderSearchFilter;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск заказов в Open API")
@DatabaseSetup("/controller/order/get/data.xml")
class ApiSearchOrdersTest extends AbstractSearchOrdersTest {

    @Autowired
    private BlackboxService blackboxService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MbiApiClient mbiApiClient;

    private ApiAuthHolder authHolder;

    @BeforeEach
    void setupAuth() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);

        authHolder.mockAccess(mbiApiClient, 1L);
    }

    @Test
    @DisplayName("Недоступный магазин")
    void inaccessibleShop() throws Exception {
        authHolder.mockNoAccess(mbiApiClient, 1L);

        search("controller/order/search/request/all.json")
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/order/search/response/no_access.json"));
    }

    @Test
    @DisplayName("Отсутствуют сендеры")
    void noSenderIds() throws Exception {
        search("controller/order/search/request/empty.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "senderIds",
                "must not be null",
                "apiOrderSearchFilter",
                "NotNull"
            )));
    }

    @Test
    @DisplayName("Слишком большой офсет")
    void bigOffset() throws Exception {
        MultiValueMap<String, String> sortingParam = new LinkedMultiValueMap<>();
        sortingParam.put("page", List.of(String.valueOf(Integer.MAX_VALUE)));
        sortingParam.put("size", List.of("10"));

        search("controller/order/search/request/all.json", sortingParam)
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/order/search/response/big_offset.json"));
    }

    @Nonnull
    @Override
    protected String orderSearchObjectName() {
        return "apiOrderSearchFilter";
    }

    @Override
    @Nonnull
    protected ResultActions search(String requestPath, MultiValueMap<String, String> params) throws Exception {
        return mockMvc.perform(
            put("/api/orders/search")
                .headers(authHolder.authHeaders())
                .params(params)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestPath))
        );
    }

    @Override
    @Nonnull
    protected ResultActions search(Consumer<AbstractOrderSearchFilter> filterAdjuster) throws Exception {
        ApiOrderSearchFilter filter = new ApiOrderSearchFilter();
        filter.setSenderIds(Set.of(11L, 12L));
        filterAdjuster.accept(filter);
        return mockMvc.perform(
            request(HttpMethod.PUT, "/api/orders/search", filter)
                .headers(authHolder.authHeaders())
        );
    }
}
