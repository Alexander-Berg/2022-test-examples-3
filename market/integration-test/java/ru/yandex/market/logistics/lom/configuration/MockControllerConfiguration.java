package ru.yandex.market.logistics.lom.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Configuration
public class MockControllerConfiguration {

    @RestController
    @RequestMapping("/test")
    @SuppressWarnings("unused")
    // параметр TestDto используется для проверки логирования
    static class TestController {

        @PostMapping("/logging")
        TestDto method1(@RequestBody TestDto testDto) {
            return new TestDto("response");
        }

        @PostMapping("/not-logging")
        TestDto method2(@RequestBody TestDto testDto) {
            return new TestDto("response");
        }

        @PostMapping("/not-logging-this-too/submethod")
        TestDto method3(@RequestBody TestDto testDto) {
            return new TestDto("response");
        }
    }

    @Data
    @NoArgsConstructor
    static class TestDto {
        String body;

        @JsonCreator
        TestDto(String body) {
            this.body = body;
        }
    }
}
