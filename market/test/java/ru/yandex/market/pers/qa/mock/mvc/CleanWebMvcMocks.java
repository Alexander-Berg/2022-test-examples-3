package ru.yandex.market.pers.qa.mock.mvc;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Service
public class CleanWebMvcMocks extends AbstractMvcMocks {

    public String callback(String body, ResultMatcher resultMatcher)
        throws Exception {

        return invokeAndRetrieveResponse(
            post("/clean/web/verdict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .accept(MediaType.APPLICATION_JSON),
            resultMatcher);
    }

    public String callback(String body)
        throws Exception {

        return invokeAndRetrieveResponse(
            post("/clean/web/verdict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .accept(MediaType.APPLICATION_JSON),
            status().is2xxSuccessful());
    }

}
