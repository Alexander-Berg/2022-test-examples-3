package ru.yandex.market.pers.grade.mock;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Service
public class CleanWebMvcMocks extends ru.yandex.market.pers.test.common.AbstractMvcMocks {

    public String successfulCallback(String body) {
        return callback(body, status().is2xxSuccessful());
    }

    public String callback(String body, ResultMatcher expected) {
        return invokeAndRetrieveResponse(
            post("/api/grade/clean/web/verdict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .accept(MediaType.APPLICATION_JSON),
            expected);
    }

}
