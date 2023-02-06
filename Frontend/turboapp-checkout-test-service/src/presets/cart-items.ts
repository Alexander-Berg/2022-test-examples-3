import { CheckoutCartItem, Currency } from '../types/checkout-api';

export const fullInfoItem: CheckoutCartItem = {
    title: 'Смартфон Apple iPhone',
    amount: { value: 12990 * 100, currency: Currency.Rub },
    count: 1,
    image: [
        {
            url: 'https://yastat.net/s3/tap/checkout/images/image_32.jpg',
            width: 32,
            height: 32,
        },
    ],
};

export const withoutImageItem: CheckoutCartItem = {
    title: 'Смартфон Apple iPhone',
    amount: { value: 12990 * 100, currency: Currency.Rub },
    count: 1,
};

export const withoutAmountItem: CheckoutCartItem = {
    title: 'Смартфон Apple iPhone',
    amount: { value: 0, currency: Currency.Rub },
    count: 1,
    image: [
        {
            url: 'https://yastat.net/s3/tap/checkout/images/image_32.jpg',
            width: 32,
            height: 32,
        },
    ],
};

export const multiplyImageItem: CheckoutCartItem = {
    title: 'Смартфон Apple iPhone',
    amount: { value: 12990 * 100, currency: Currency.Rub },
    count: 1,
    image: [
        {
            url: 'https://yastat.net/s3/tap/checkout/images/image_128.jpg',
            width: 128,
            height: 128,
        },
        {
            url: 'https://yastat.net/s3/tap/checkout/images/image_64.jpg',
            width: 64,
            height: 64,
        },
        {
            url: 'https://yastat.net/s3/tap/checkout/images/image_32.jpg',
            width: 32,
            height: 32,
        },
    ],
};
