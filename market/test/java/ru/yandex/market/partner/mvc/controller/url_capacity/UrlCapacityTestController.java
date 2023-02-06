package ru.yandex.market.partner.mvc.controller.url_capacity;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ru.yandex.market.mbi.util.url_capacity.UrlCapacityLimitingInterceptor;

/**
 * Тестовый контроллер, поднимающийся только в тестовом контексте для теста UrlCapacityLimitingInterceptor
 * @see UrlCapacityLimitingInterceptor
 */
@Controller
@RequestMapping("/url-capacity-test")
public class UrlCapacityTestController {

    @RequestMapping("/test-method")
    public void testMethod() {

    }

    @RequestMapping("/test-method-2")
    public void testMethod2() {

    }


    @RequestMapping("/test-method-with-id/{partnerId}")
    public void testMethodWithId(Long id) {

    }

    @RequestMapping("/test-method-with-exception")
    public void testMethodWithException() {
        throw new RuntimeException("Some exception to check interceptor");
    }

    @RequestMapping("/test-async-method")
    public CompletableFuture<String> testAsyncMethod() {
        return CompletableFuture.supplyAsync(() -> "Test string");
    }
}
