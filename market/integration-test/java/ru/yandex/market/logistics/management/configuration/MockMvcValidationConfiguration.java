package ru.yandex.market.logistics.management.configuration;

import java.net.BindException;

import javax.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Support for testing errors https://github.com/spring-projects/spring-boot/issues/5574
 */
@ControllerAdvice
@RequiredArgsConstructor
public class MockMvcValidationConfiguration {

    private final BasicErrorController errorController;

    @ExceptionHandler(value = {MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity defaultErrorHandler(HttpServletRequest request, Exception ex) {
        request.setAttribute("javax.servlet.error.request_uri", request.getPathInfo());
        request.setAttribute("javax.servlet.error.status_code", 400);
        return errorController.error(request);
    }
}
