package ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions;

public class RetryableTestException extends RuntimeException {
    public RetryableTestException(String message) {
        super(message);
    }
}
