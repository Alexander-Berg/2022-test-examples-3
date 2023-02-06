import {
    ECheckAvailabilityBeforeBookingOutcome,
    ECheckAvailabilityOnRedirOutcome,
    EConfirmationOutcome,
    EMqEventOutcome,
    ETokenizationOutcome,
} from 'server/api/AviaBookingApi/types/IAviaTestContextTokenApiParams';

type TCodeEnums =
    | ECheckAvailabilityBeforeBookingOutcome
    | ECheckAvailabilityOnRedirOutcome
    | EConfirmationOutcome
    | EMqEventOutcome
    | ETokenizationOutcome;

const optionDescriptions: PartialRecord<TCodeEnums, string> = {
    [ECheckAvailabilityOnRedirOutcome.SUCCESS]: 'Успешная проверка',
    [ECheckAvailabilityOnRedirOutcome.PRICE_CHANGED]:
        'Цена изменилась в момент перехода',
    [ECheckAvailabilityOnRedirOutcome.NOT_AVAILABLE]:
        'Вариант более не доступен',

    [ECheckAvailabilityBeforeBookingOutcome.SUCCESS]: 'Успешная проверка',
    [ECheckAvailabilityBeforeBookingOutcome.PRICE_CHANGED]:
        'Цена изменилась перед попыткой создания заказа',

    [EConfirmationOutcome.SUCCESS]: 'Успешно создали и подтвердили заказ',
    [EConfirmationOutcome.PAYMENT_FAILED]: 'Оплата не удалась',
    [EConfirmationOutcome.VARIANT_NOT_AVIALABLE]:
        'Вариант оказался недоступен в момент создания и подтверждения заказа',
    [EConfirmationOutcome.PRICE_CHANGED]:
        'Цена изменилась в момент создания и подтверждения заказа',

    [EMqEventOutcome.NO_EVENT]: 'Без события от аэрофлота',
    [EMqEventOutcome.SUCCESS]:
        'События об успешности заказа (в нем будут билеты)',
    [EMqEventOutcome.NOT_PAID]: 'Заказ не оплачен',

    [ETokenizationOutcome.SUCCESS]: 'Успешная токенизация карты',
    [ETokenizationOutcome.FAILURE]: 'Токенизация не удалась',
};

export default function getOptionDescription(code: TCodeEnums): string {
    return optionDescriptions[code] ?? '';
}
