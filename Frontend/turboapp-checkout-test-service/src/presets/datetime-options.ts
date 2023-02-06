import clone from 'clone-deep';

import { CheckoutTimeOption, Currency, CheckoutDatetimeOption } from '../types/checkout-api';

export const timeOptions: CheckoutTimeOption[] = [
    { id: '9.00 - 12.00', label: '9.00 - 12.00', amount: { value: 400 * 100, currency: Currency.Rub } },
    { id: '12.00 - 15.00', label: '12.00 - 15.00', amount: { value: -200 * 100, currency: Currency.Rub } },
    { id: '15.00 - 18.00', label: '15.00 - 18.00', amount: { value: 0, currency: Currency.Rub } },
    { id: '18.00 - 22.00', label: '18.00 - 22.00', amount: { value: 300 * 100, currency: Currency.Rub } },
];

export const timeOption: CheckoutTimeOption = {
    id: '12.00 - 15.00',
    label: '12.00 - 15.00',
    amount: { value: 0, currency: Currency.Rub },
};

export const withAmountTimeOption: CheckoutTimeOption = {
    id: '9.00 - 12.00',
    label: '9.00 - 12.00',
    amount: { value: 400 * 100, currency: Currency.Rub },
};

const todayISO = new Date().toISOString().split('T')[0];

export const withoutTimeOption: CheckoutDatetimeOption = {
    id: 'today',
    date: todayISO,
};

export const fullDateTimeOption: CheckoutDatetimeOption = {
    id: 'today',
    date: todayISO,
    timeOptions: clone(timeOptions),
};
