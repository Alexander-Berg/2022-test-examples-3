import { Currency, CheckoutCartDetail } from '../types/checkout-api';

export const productsAmount: CheckoutCartDetail = {
    label: 'Товары',
    amount: { value: 15990 * 100, currency: Currency.Rub },
};
export const discount: CheckoutCartDetail = {
    label: 'Скидка по акциям',
    amount: { value: -500 * 100, currency: Currency.Rub },
};
export const checkoutWeight: CheckoutCartDetail = { label: 'Вес заказа', value: '0.4 кг' };
