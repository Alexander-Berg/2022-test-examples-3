package ru.yandex.travel.api.factory;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    public static final String TEST_ENDPOINT = "/test-endpoint";

    @Data
    public static class TestRequest {

        private int inputField;

    }

    @Data
    public static class TestResponse {

        private int outputField;
    }

    @RequestMapping(value = TEST_ENDPOINT, method = RequestMethod.POST)
    public TestResponse getResponse(@RequestBody TestRequest testRequest) {
        TestResponse response = new TestResponse();
        response.setOutputField(testRequest.getInputField());
        return response;
    }

}
