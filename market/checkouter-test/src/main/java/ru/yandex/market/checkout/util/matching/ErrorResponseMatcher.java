package ru.yandex.market.checkout.util.matching;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author : poluektov
 * date: 16.08.17.
 */
public class ErrorResponseMatcher implements ResultMatcher {

    private final int status;
    private final String code;
    private final String message;

    ErrorResponseMatcher(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public void match(MvcResult result) throws Exception {
        status().is(status).match(result);
        jsonPath("$.message").value(equalTo(message)).match(result);
        jsonPath("$.code").value(equalTo(code)).match(result);
    }
}
