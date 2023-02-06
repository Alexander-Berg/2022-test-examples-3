package ru.yandex.market.checkout.util.matching;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class OrderFailureResultMatcher implements ResultMatcher {

    private final String error;
    private String errorDetails;

    public OrderFailureResultMatcher(String error) {
        this.error = error;
    }

    public OrderFailureResultMatcher(String error, String errorDetails) {
        this.error = error;
        this.errorDetails = errorDetails;
    }

    @Override
    public void match(MvcResult result) throws Exception {
        status().isOk().match(result);
        jsonPath("$.orderFailures[0].error").value(equalTo(error)).match(result);
        if (errorDetails != null) {
            jsonPath("$.orderFailures[0].errorDetails").value(equalTo(errorDetails)).match(result);
        }
    }
}
