package ru.yandex.market.delivery.transport_manager.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.delivery.transport_manager.exception.ResourceNotFoundException;
import ru.yandex.market.delivery.transport_manager.model.enums.ResourceType;
import ru.yandex.market.delivery.transport_manager.service.checker.exception.TransportationCouldNotBeCheckedException;

@RestController
public class ExceptionController {
    @GetMapping("/exception/transportationCouldNotBeCheckedException")
    public void transportationCouldNotBeCheckedException() {
        throw new TransportationCouldNotBeCheckedException(1L, "Reason");
    }

    @GetMapping("/exception/httpMessageNotReadableException")
    public void httpMessageNotReadableException() {
        final MockHttpInputMessage msg = new MockHttpInputMessage("abcd".getBytes(StandardCharsets.UTF_8));
        throw new HttpMessageNotReadableException("Message", msg);
    }

    @GetMapping("/exception/resourceNotFoundException")
    public void resourceNotFoundException() {
        throw new ResourceNotFoundException(ResourceType.TRANSPORTATION, 1L);
    }

    @GetMapping("/exception/bindException")
    public void bindException() throws NoSuchMethodException, BindException {
        final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "abc");
        bindingResult.addError(new ObjectError("abc", "Some error"));
        throw new BindException(
            bindingResult
        );
    }
}
