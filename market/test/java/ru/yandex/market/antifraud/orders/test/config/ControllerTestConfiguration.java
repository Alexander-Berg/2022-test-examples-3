package ru.yandex.market.antifraud.orders.test.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import ru.yandex.market.antifraud.orders.config.ExceptionHandlingControllerAdvice;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

/**
 * @author dzvyagin
 */
@Configuration
public class ControllerTestConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return AntifraudJsonUtil.OBJECT_MAPPER;
    }

    @Bean
    public ResponseEntityExceptionHandler exceptionHandler() {
        return new ExceptionHandlingControllerAdvice();
    }
}
