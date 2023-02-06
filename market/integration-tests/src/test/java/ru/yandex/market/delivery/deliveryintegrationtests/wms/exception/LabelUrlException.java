package ru.yandex.market.delivery.deliveryintegrationtests.wms.exception;

public class LabelUrlException extends RuntimeException {
    private static final String MESSAGE = "Can't get labelUrl for order with id ";

    public LabelUrlException(long orderId, Throwable cause) {
        super(MESSAGE + orderId, cause);
    }
}
