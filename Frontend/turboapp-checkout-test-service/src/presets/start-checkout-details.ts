import { getDefaultTestPaymentFormUrl } from '../lib/url-builder';
import { CheckoutDetailsUpdate, Currency } from '../types/checkout-api';

const startCheckoutDetails: CheckoutDetailsUpdate = {
    total: {
        amount: {
            value: 15990 * 100,
            currency: Currency.Rub,
        },
        details: [
            { label: 'Товар', amount: { value: 15990 * 100, currency: Currency.Rub } },
            { label: 'Доставка', amount: { value: 500 * 100, currency: Currency.Rub } },
            { label: 'Скидка MasterCard', amount: { value: -500 * 100, currency: Currency.Rub } },
            { label: 'Скидка по промокоду', amount: { value: -200 * 100, currency: Currency.Rub } },
        ],
    },
    payerDetails: [{ type: 'name', require: true }, { type: 'phone' }, { type: 'email', require: true }],
    paymentOptions: [
        { type: 'offline-cash' },
        { type: 'offline-card' },
        { type: 'yandex-payments' },
        { type: 'online-external-payments', data: { paymentFormUrl: getDefaultTestPaymentFormUrl() } },
    ],
    requestOrderComment: true,
    orders: [
        {
            id: 'order-1',
            requestCity: true,
            requestShippingAddress: true,
            cartItems: [
                {
                    title: 'Смартфон Samsung Galaxy A51 64GB',
                    amount: { value: 12990 * 100, currency: Currency.Rub },
                    count: 1,
                    image: [
                        {
                            url: 'https://yastat.net/s3/tap/checkout/images/image-1.png',
                            width: 88,
                            height: 88,
                        },
                    ],
                },
                {
                    title: 'Чехол для смартфона Samsung Galaxy A51 - синий',
                    amount: { value: 1000 * 100, currency: Currency.Rub },
                    count: 1,
                    image: [
                        {
                            url: 'https://yastat.net/s3/tap/checkout/images/image-2.png',
                            width: 88,
                            height: 88,
                        },
                    ],
                },
                {
                    title: 'Чехол для смартфона Samsung Galaxy A51 - прозрачный',
                    amount: { value: 1000 * 100, currency: Currency.Rub },
                    count: 2,
                    image: [
                        {
                            url: 'https://yastat.net/s3/tap/checkout/images/image-3.png',
                            width: 88,
                            height: 88,
                        },
                    ],
                },
                {
                    title: 'Чехол для смартфона Samsung Galaxy A51 - желтый',
                    amount: { value: 1000 * 100, currency: Currency.Rub },
                    count: 2,
                    image: [
                        {
                            url: 'https://yastat.net/s3/tap/checkout/images/image-4.png',
                            width: 88,
                            height: 88,
                        },
                    ],
                },
            ],
            cartDetails: [
                { label: 'Товаров 5', amount: { value: 17490 * 100, currency: Currency.Rub } },
                { label: 'Скидка по акциям', amount: { value: -500 * 100, currency: Currency.Rub } },
                { label: 'Вес заказа', value: '0.4 кг' },
            ],
            shippingOptions: [
                {
                    id: 'express-delivery',
                    label: 'Курьер, срочно',
                    datetimeEstimate: 'В течение дня',
                    amount: {
                        currency: Currency.Rub,
                        value: 500 * 100,
                    },
                },
                {
                    id: 'delivery',
                    label: 'Курьер',
                    datetimeEstimate: 'В течение 3-5 дней',
                    amount: {
                        currency: Currency.Rub,
                        value: 0,
                    },
                    selected: true,
                },
                {
                    id: 'pickup',
                    label: 'Самовывоз',
                    amount: {
                        currency: Currency.Rub,
                        value: 0,
                    },
                },
                {
                    id: 'shipping',
                    label: 'Почта России',
                    datetimeEstimate: 'Через 3-4 дня',
                    amount: {
                        currency: Currency.Rub,
                        value: 0,
                    },
                },
            ],
            datetimeOptions: ['2020-09-10', '2020-09-11', '2020-09-12'].map(dateISO => ({
                id: dateISO,
                date: dateISO,
                timeOptions: ['с 9:00 до 10:00', 'с 13:00 до 14:00', 'с 17:00 до 18:00', 'с 20:00 до 21:00'].map(
                    (label, index, { length }) => ({
                        id: label,
                        label,
                        amount: {
                            value: (index + 1 === length && 1000 * 100) || (index === 0 && -500 * 100) || 0,
                            currency: Currency.Rub,
                        },
                    })
                ),
            })),
        },
    ],
};

export default startCheckoutDetails;
