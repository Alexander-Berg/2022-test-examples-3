package ru.yandex.market.checkout.checkouter.web;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;

@RestController
public class ErrorHandlingTestController {

    @RequestMapping("/test/npe")
    public String throwNpe() {
        throw new NullPointerException();
    }

    @PostMapping(value = "/test/postOnly")
    public String postOnly() {
        return "";
    }

    @RequestMapping("/test/badRequest")
    public String throwIllegalArgumentException() {
        throw new IllegalArgumentException("illegal argument exception");
    }

    @RequestMapping("/test/numberFormatException/{id}")
    public String numberFormatException(@PathVariable String id) {
        return "" + Integer.parseInt(id);
    }

    @RequestMapping("/test/conversionNotSupportedException/{id}")
    public String conversionNotSupportedException(@PathVariable long id) {
        return "" + id;
    }

    @RequestMapping("/test/typeMismatchException")
    public String typeMismatchException() {
        throw new TypeMismatchException("asdasd", Integer.class);
    }

    @RequestMapping("/test/missingRequiredParameter")
    public String missingRequiredParameter(@RequestParam(value = "requiured", required = true) String required) {
        return "";
    }

    @RequestMapping("/test/errorCodeException/{code}/{message}/{status}")
    public String errorCodeException(@PathVariable String code, @PathVariable String message,
                                     @PathVariable int status) {
        throw new ErrorCodeException(code, message, status);
    }

    @RequestMapping("/test/orderNotFoundException")
    public String orderNotFoundException() {
        throw new OrderNotFoundException(1337L);
    }

    @RequestMapping("/test/orderStatusNotAllowedException")
    public String orderStatusNotAllowedException() {
        throw OrderStatusNotAllowedException.notAllowed(1337L, OrderStatus.PROCESSING, OrderStatus.DELIVERED);
    }

    @RequestMapping("/test/httpMessageNotReadableException")
    public String httpMessageNotReadableException() {
        throw new HttpMessageNotReadableException("asdasd");
    }
}
