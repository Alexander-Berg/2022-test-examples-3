package ru.yandex.market.pharmatestshop.domain.order.paymentmethod;

/**
 * Способ оплаты заказа.
 */
public enum PaymentMethod {

    //"paymentType": "PREPAID"
    YANDEX,// — банковской картой.
    APPLE_PAY,// — Apple Pay.
    GOOGLE_PAY,// — Google Pay.
    CREDIT,// — в кредит.
    EXTERNAL_CERTIFICATE,// — подарочным сертификатом (например, из приложения «Сбербанк Онлайн»).

    //"paymentType": "POSTPAID"
    CARD_ON_DELIVERY,// — банковской картой.
    CASH_ON_DELIVERY,// — наличными. (Значение по умолчанию)
}
