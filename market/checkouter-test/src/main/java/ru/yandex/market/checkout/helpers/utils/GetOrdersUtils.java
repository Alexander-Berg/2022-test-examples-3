package ru.yandex.market.checkout.helpers.utils;

import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matcher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.checkouter.order.Order;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author mmetlov
 */
public final class GetOrdersUtils {

    private GetOrdersUtils() {
    }

    public static <T> ParameterizedRequest<T> parameterizedGetRequest(String urlPattern) {
        return param -> MockMvcRequestBuilders.get(urlPattern, String.valueOf(param));
    }

    public static <T> ParameterizedRequest<Collection<T>> parameterizedGetRequest(String url, String param) {
        return values -> {
            MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(url);
            for (T value : values) {
                builder.param(param, String.valueOf(value));
            }
            return builder;
        };
    }

    public static <T> ParameterizedRequest<T> parameterizedPostRequest(String url, String contentPattern) {
        return param -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String valueAsString = mapper.writeValueAsString(param);
                return MockMvcRequestBuilders.post(url)
                        .content(String.format(contentPattern, valueAsString))
                        .contentType(MediaType.APPLICATION_JSON_UTF8);

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };
    }

    public static ResultMatcher resultMatcherOrderNotFound(Long orderId) throws Exception {
        return result -> {
            status().is(404).match(result);
            jsonPath("$.status").value(404).match(result);
            jsonPath("$.code").value("ORDER_NOT_FOUND").match(result);
            jsonPath("$.message").value("Order not found: " + orderId).match(result);
        };
    }

    public static ResultMatcher resultMatcherOrdersNotFound() throws Exception {
        return result -> {
            status().is(200).match(result);
            jsonPath("$.orders").isEmpty().match(result);
            jsonPath("$.pager.total").value(0).match(result);
        };
    }

    public static ResultMatcher resultMatcherCount(int count) throws Exception {
        return result -> {
            status().is(200).match(result);
            jsonPath("$.value").value(count).match(result);
        };
    }

    public static ResultMatcher resultMatcherBoolean(boolean exist) throws Exception {
        return result -> {
            status().is(200).match(result);
            jsonPath("$.value").value(exist).match(result);
        };
    }

    public static ResultMatcher resultMatcherOrders(Order... orders) {
        return result -> {
            status().isOk().match(result);
            jsonPath("$.orders[*]",
                    containsInAnyOrder(
                            Arrays.stream(orders)
                                    .map(Order::getId)
                                    .map(id -> hasEntry("id", id.intValue()))
                                    .toArray(Matcher[]::new)
                    )).match(result);
        };
    }

    @FunctionalInterface
    public interface ParameterizedRequest<T> {

        MockHttpServletRequestBuilder build(T parameter);
    }
}
