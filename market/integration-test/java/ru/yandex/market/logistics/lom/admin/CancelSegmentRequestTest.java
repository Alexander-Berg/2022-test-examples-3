package ru.yandex.market.logistics.lom.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil;
import ru.yandex.market.logistics.lom.filter.AdminCancelSegmentSearchFilter;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_AND_SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.utils.TestUtils.toHttpHeaders;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/controller/admin/cancel/prepare.xml")
public class CancelSegmentRequestTest extends AbstractContextualTest {

    @Autowired
    private TvmClientApi tvmClientApi;

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск заявок на отмену сегментов")
    void search(String displayName, AdminCancelSegmentSearchFilter filter, String responsePath) throws Exception {
        mockMvc.perform(get("/admin/cancel-segment").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminCancelSegmentSearchFilter(),
                "controller/admin/cancel/segment_all.json"
            ),
            Arguments.of(
                "По идентификатору заявки на отмену заказа",
                new AdminCancelSegmentSearchFilter().setCancelOrderId(1L),
                "controller/admin/cancel/segment_id_1_2.json"
            )
        );
    }

    @Test
    @DisplayName("Получение деталей заявки на отмену сегмента")
    void getOne() throws Exception {
        mockMvc.perform(get("/admin/cancel-segment/2"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/cancel/detail_segment_2.json"));
    }

    @Test
    @DisplayName("Заявка на отмену сегмента не найдена")
    void notFound() throws Exception {
        mockMvc.perform(get("/admin/cancel-segment/42"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [CANCELLATION_SEGMENT_REQUEST] with id [42]"));
    }

    @Test
    @DisplayName("Форма подтверждения заявки")
    void confirmNew() throws Exception {
        mockMvc.perform(get("/admin/cancel-segment/confirmation/new?parentId=2"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/cancel/new_confirm_segment_2.json"));
    }

    @Test
    @DisplayName("Подтверждение заявки на отмену сегмента")
    @ExpectedDatabase(
        value = "/controller/admin/cancel/confirm_segment_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void confirm() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        mockMvc.perform(
            post("/admin/cancel-segment/confirmation")
                .headers(toHttpHeaders(USER_AND_SERVICE_HEADERS))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\": 2, \"reason\": \"cancel reason\"}")
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Подтверждение заявки на отмену сегмента без указания причины")
    @ExpectedDatabase(
        value = "/controller/admin/cancel/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void confirmEmptyReason() throws Exception {
        mockMvc.perform(
            post("/admin/cancel-segment/confirmation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requestId\": 2, \"reason\": \"\"}")
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].field").value("reason"))
            .andExpect(jsonPath("errors[0].defaultMessage").value("must not be empty"));
    }

}
