import {
    EBusesBookOutcome,
    EBusesConfirmOutcome,
    EBusesRefundInfoOutcome,
    EBusesRefundOutcome,
} from './models';

/**
 * @url https://api.travel-balancer-test.yandex.net/api/test_context/v1/bus_token
 * @url https://api-prod.travel-hotels.yandex.net/api/test_context/v1/bus_token
 */
export interface IRequest {
    /**
     * Результат создания бронирования
     */
    bookOutcome: EBusesBookOutcome;

    /**
     * Результат подтверждения бронирования
     */
    confirmOutcome: EBusesConfirmOutcome;

    /**
     * Результат получения стоимости к возврату
     */
    refundInfoOutcome: EBusesRefundInfoOutcome;

    /**
     * Результат проведения возврата
     */
    refundOutcome: EBusesRefundOutcome;

    /**
     * Секунд до автоотмены брони по таймауту
     */
    expireAfterSeconds?: number;
}
