package ru.yandex.market.api.partner.controllers.util.request;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class OrderControllerHelper {

    private final String urlBasePrefix;

    public OrderControllerHelper(String urlBasePrefix) {
        this.urlBasePrefix = urlBasePrefix;
    }

    @Nullable
    public String getOrderRequest(@Nonnull OrderRequestBuilder requestBuilder) {
        requestBuilder.url("/campaigns/{campaignId}/orders/{orderId}.{format}");
        requestBuilder.urlBasePrefix(urlBasePrefix);
        final ResponseEntity<String> response;
        if (requestBuilder.hasUid()) {
            response = FunctionalTestHelper.makeRequest(
                    requestBuilder.build(),
                    HttpMethod.GET,
                    String.class,
                    requestBuilder.getUid()
            );
        } else {
            response = FunctionalTestHelper.makeRequest(
                    requestBuilder.build(),
                    HttpMethod.GET,
                    String.class
            );
        }
        assertStatusOk(response);
        return response.getBody();
    }

    @Nullable
    public String getOrdersRequest(@Nonnull OrderRequestBuilder requestBuilder) {
        requestBuilder.url("/campaigns/{campaignId}/orders");
        requestBuilder.urlBasePrefix(urlBasePrefix);
        final ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                requestBuilder.build(),
                HttpMethod.GET,
                Format.JSON
        );
        assertStatusOk(response);
        return response.getBody();
    }

    @Nullable
    public String updateOrderStatusRequest(
            @Nonnull OrderRequestBuilder requestBuilder,
            @Nonnull OrderSubstatus substatus
    ) {
        requestBuilder.url("/campaigns/{campaignId}/orders/{orderId}/status");
        requestBuilder.urlBasePrefix(urlBasePrefix);

        final Map<String, Object> substitutions = new HashMap<>();
        substitutions.put("substatus", substatus);
        substitutions.put("status", substatus.getStatus());


        final ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                requestBuilder.build(),
                HttpMethod.PUT,
                Format.JSON,
                new StrSubstitutor(substitutions, "${", "}")
                        .replace("{\"order\": {\"status\": \"${status}\", \"substatus\": \"${substatus}\"}}")
        );
        assertStatusOk(response);
        return response.getBody();
    }

    private void assertStatusOk(ResponseEntity<?> responseEntity) {
        assertThat(responseEntity.getStatusCode(), equalTo(HttpStatus.OK));
    }
}
