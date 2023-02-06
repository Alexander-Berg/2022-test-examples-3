import {
    EPaymentFailureResponseCode,
    EPaymentOutcome,
} from 'server/api/OrdersAPI/types/TGetPaymentTestContextTokenParams';

type TCodeEnums = EPaymentOutcome | EPaymentFailureResponseCode;

const optionDescriptions: PartialRecord<TCodeEnums, string> = {
    [EPaymentOutcome.PO_SUCCESS]: 'Успешная оплата',
    [EPaymentOutcome.PO_FAILURE]: 'Неуспешная оплата',

    [EPaymentFailureResponseCode.AUTHORIZATION_REJECT]:
        'Банк отклонил транзакцию, за подробностями надо обращаться в банк',
    [EPaymentFailureResponseCode.BLACKLISTED]:
        'Сработало антифрод правило/алгоритм на стороне процессинга',
    [EPaymentFailureResponseCode.EXPIRED_CARD]: 'Карта просрочена',
    [EPaymentFailureResponseCode.EXT_ACTION_REQUIRED]:
        'При оплате Яндекс-кошельками - пользователь не прошёл доп. идентификацию и пытается оплатить выше 15000 руб. или прошёл, но ограничение не снято в нашем терминале',
    [EPaymentFailureResponseCode.FAIL_3DS]:
        'Платеж не авторизован, т.к. пользователь не прошел проверку 3D Secure',
    [EPaymentFailureResponseCode.INVALID_PROCESSING_REQUEST]:
        'Ошибка в данных, переданных в платежную систему',
    [EPaymentFailureResponseCode.INVALID_XRF_TOKEN]:
        'Не были получены яндексовые Cookie',
    [EPaymentFailureResponseCode.LIMIT_EXCEEDED]:
        'Достигнуты лимиты по операциям с картой',
    [EPaymentFailureResponseCode.NOT_ENOUGH_FUNDS]:
        'На карте недостаточно средств',
    [EPaymentFailureResponseCode.PAYMENT_GATEWAY_TECHNICAL_ERROR]:
        'Техническая ошибка на стороне платежного шлюза',
    [EPaymentFailureResponseCode.PAYMENT_TIMEOUT]:
        'Платеж не авторизован, т.к. пользователь не ввел данные за отведенный срок',
    [EPaymentFailureResponseCode.PROMOCODE_ALREADY_USED]:
        'Спец. статус для многоразовых промокодов - пользователь уже воспользовался данным промокодом',
    [EPaymentFailureResponseCode.RESTRICTED_CARD]:
        'Карта недействительна (украдена, утеряна и т.п.), транзакцию не нужно повторять!',
    [EPaymentFailureResponseCode.TIMEOUT_NO_SUCCESS]:
        'Не было получено ответа от платежной системы за отведенный лимит времени',
    [EPaymentFailureResponseCode.TRANSACTION_NOT_PERMITTED]:
        'Операция недоступна для данной карты (установлены ограничения пользователем или банком)',
    [EPaymentFailureResponseCode.UNKNOWN_ERROR]: 'Не классифицированная ошибка',
    [EPaymentFailureResponseCode.USER_CANCELLED]:
        'Пользователь отказался от платежа',
    [EPaymentFailureResponseCode.WRONG_FISCAL_DATA]:
        'Неверно заполнены данные, необходимые для выдачи чека',
    [EPaymentFailureResponseCode.OPERATION_CANCELLED]:
        'Операция отменена на стороне партнера и далее',
};

export default function getOptionDescription(code: TCodeEnums): string {
    return optionDescriptions[code] ?? '';
}
