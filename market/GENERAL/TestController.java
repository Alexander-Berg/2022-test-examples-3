package ru.yandex.market.ir.ui.controllers;

import lombok.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/api/hello-world")
    public HelloWorld helloWorld() {
        return new HelloWorld("Hello world");
    }

    @Value
    public static class HelloWorld {
        private String message;
    }
}
