package ru.yandex.market.delivery.deliveryintegrationtests.wms.exception;

public class TrackCodeException extends RuntimeException {
    private static final String MESSAGE = "Can't get tackCode for order with id ";

    public TrackCodeException(long orderId, Throwable cause) {
        super(MESSAGE + orderId, cause);
    }
}
