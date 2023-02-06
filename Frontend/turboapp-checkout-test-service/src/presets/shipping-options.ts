import { Currency, CheckoutShippingOption } from '../types/checkout-api';

export const expressShippingOption: CheckoutShippingOption = {
    id: 'express-delivery',
    label: 'Курьер, срочно',
    datetimeEstimate: 'В течение дня',
    amount: {
        currency: Currency.Rub,
        value: 500 * 100,
    },
};
export const deliveryShippingOption: CheckoutShippingOption = {
    id: 'delivery',
    label: 'Курьер',
    datetimeEstimate: 'В течение 3-5 дней',
    amount: {
        currency: Currency.Rub,
        value: 0,
    },
};
export const pickupShippingOption: CheckoutShippingOption = {
    id: 'pickup',
    label: 'Самовывоз',
    amount: {
        currency: Currency.Rub,
        value: 0,
    },
};
export const postShippingOption: CheckoutShippingOption = {
    id: 'shipping',
    label: 'Почта России',
    amount: {
        currency: Currency.Rub,
        value: 1000 * 100,
    },
};
