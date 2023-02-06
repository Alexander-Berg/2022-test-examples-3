import { CheckoutValidationErrors } from '../types/checkout-api';

export const mainErrors: CheckoutValidationErrors = {
    error: 'Товары на складе закончились',
};

export const payerErrors: CheckoutValidationErrors = {
    payerDetails: {
        name: 'Имя введено неверно',
        phone: 'Формат неверен',
    },
};

export const orderErrors: CheckoutValidationErrors = {
    orders: [
        {
            id: 'order-1',
            shippingAddress: {
                address: 'Не удалось найти адрес',
            },
        },
    ],
};
