import {
    YandexCheckoutCartItemImage,
    YandexCheckoutCartItem,
    YandexCheckoutCartDetail,
    YandexCheckoutShippingOption,
    YandexCheckoutPickupOption,
    YandexCheckoutTimeOption,
    YandexCheckoutDatetimeOption,
    YandexCheckoutTotalAmountDetail,
    YandexCheckoutTotalAmount,
    YandexCheckoutPayerDetail,
    YandexCheckoutPaymentOption,
    YandexCheckoutOrder,
    YandexCheckoutValidationErrors,
    YandexCheckoutOrderErrors,
    YandexCheckoutDetails,
    YandexCheckoutDatetimeOptionState,
    YandexCheckoutOrderState,
    YandexCheckoutState,
    YandexCheckoutPrice,
} from '@yandex-int/tap-checkout-types';

type DeepPartial<T> = {
    [P in keyof T]?: T[P] extends (infer U)[]
        ? DeepPartial<U>[]
        : DeepPartial<T[P]>
};

export enum Currency {
    Rub = 'RUB',
}

export type CheckoutPrice = Partial<YandexCheckoutPrice>;

export type CheckoutCartItemImage = DeepPartial<YandexCheckoutCartItemImage>;

export type CheckoutCartItem = DeepPartial<YandexCheckoutCartItem>;

export type CheckoutCartDetail = DeepPartial<YandexCheckoutCartDetail>;

export type CheckoutShippingOption = DeepPartial<YandexCheckoutShippingOption>;

export type CheckoutPickupOption = DeepPartial<YandexCheckoutPickupOption>;

export type CheckoutTimeOption = DeepPartial<YandexCheckoutTimeOption>;

export type CheckoutDatetimeOption = DeepPartial<YandexCheckoutDatetimeOption>;

export type CheckoutTotalAmountDetail = DeepPartial<YandexCheckoutTotalAmountDetail>;

export type CheckoutTotalAmount = DeepPartial<YandexCheckoutTotalAmount>;

export type CheckoutPayerDetail = Partial<YandexCheckoutPayerDetail>;

export type CheckoutPaymentOption = Partial<YandexCheckoutPaymentOption>;

export type CheckoutOrder = DeepPartial<YandexCheckoutOrder>;

export type CheckoutValidationErrors = DeepPartial<YandexCheckoutValidationErrors>;

export type CheckoutOrderErrors = DeepPartial<YandexCheckoutOrderErrors>;

export type CheckoutDetailsUpdate = DeepPartial<YandexCheckoutDetails>;

export type CheckoutDatetimeOptionState = DeepPartial<YandexCheckoutDatetimeOptionState>;

export type CheckoutOrderState = DeepPartial<YandexCheckoutOrderState>;

export type CheckoutState = DeepPartial<YandexCheckoutState>;
