package ru.yandex.market.logistics.lom.controller.order;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.lom.utils.TestUtils.toHttpHeaders;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@ParametersAreNonnullByDefault
public final class OrderTestUtil {

    private OrderTestUtil() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    static ResultActions createOrderWithRawRequest(MockMvc mockMvc, String request) throws Exception {
        return mockMvc.perform(post("/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request));
    }

    @Nonnull
    private static ResultActions createOrderWithRawRequest(
        MockMvc mockMvc,
        String request,
        HttpHeaders httpHeaders
    ) throws Exception {
        return mockMvc.perform(post("/orders")
            .headers(httpHeaders)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request));
    }

    @Nonnull
    static ResultActions createOrder(MockMvc mockMvc, String requestPath) throws Exception {
        return createOrderWithRawRequest(mockMvc, extractFileContent(requestPath));
    }

    @Nonnull
    static ResultActions createOrder(
        MockMvc mockMvc,
        String requestPath,
        Map<String, List<String>> headers
    ) throws Exception {
        return createOrderWithRawRequest(
            mockMvc,
            extractFileContent(requestPath),
            toHttpHeaders(headers)
        );
    }

    @Nonnull
    static ResultActions updateOrder(MockMvc mockMvc, long orderId, String requestPath) throws Exception {
        return mockMvc.perform(put("/orders/" + orderId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent(requestPath)));
    }

    @Nonnull
    static ResultActions updateOrder(
        MockMvc mockMvc,
        long orderId,
        String requestPath,
        Map<String, List<String>> headers
    ) throws Exception {
        return mockMvc.perform(put("/orders/" + orderId)
            .headers(toHttpHeaders(headers))
            .contentType(MediaType.APPLICATION_JSON)
            .content(extractFileContent(requestPath)));
    }

    @Nonnull
    public static ResultActions commitOrder(MockMvc mockMvc, long orderId) throws Exception {
        return mockMvc.perform(post("/orders/" + orderId + "/commit"));
    }

    @Nonnull
    static ResultActions commitOrder(
        MockMvc mockMvc,
        long orderId,
        Map<String, List<String>> headers
    ) throws Exception {
        return mockMvc.perform(post("/orders/" + orderId + "/commit").headers(toHttpHeaders(headers)));
    }

    @Nonnull
    static ResultActions getOrder(MockMvc mockMvc, long orderId) throws Exception {
        return mockMvc.perform(get("/orders/" + orderId));
    }

    @Nonnull
    static ResultActions cancelOrder(MockMvc mockMvc, long orderId) throws Exception {
        return mockMvc.perform(delete("/orders/" + orderId));
    }

    @Nonnull
    static ResultActions cancelOrder(MockMvc mockMvc, long orderId, String requestPath) throws Exception {
        return mockMvc.perform(
            post("/orders/" + orderId + "/cancel")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestPath))
        );
    }

    @Nonnull
    static ResultActions cancelOrder(
        MockMvc mockMvc,
        long orderId,
        Map<String, List<String>> headers
    ) throws Exception {
        return mockMvc.perform(delete("/orders/" + orderId).headers(toHttpHeaders(headers)));
    }

    @Nonnull
    public static ResultActions asyncOrderCreate(MockMvc mockMvc, String url, String requestPath) throws Exception {
        return mockMvc.perform(
            put("/orders/processing/" + url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestPath))
        );
    }

    @Nonnull
    public static List<String> getSenderEmails(JdbcTemplate jdbcTemplate, long orderId) {
        return jdbcTemplate.queryForList(
            "SELECT unnest(sender_emails) FROM orders WHERE id = ?",
            new Object[]{orderId},
            String.class
        );
    }

    @Nonnull
    static ResultActions rebindOrder(MockMvc mockMvc, String requestPath) throws Exception {
        return mockMvc.perform(
            put("/orders/rebind-order")
                .content(extractFileContent(requestPath))
                .contentType(MediaType.APPLICATION_JSON)
        );
    }
}
