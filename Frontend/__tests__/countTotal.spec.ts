import { ECurrencyAvailable } from '~/types';
import { countTotal } from '../countTotal';

describe('countTotal', () => {
    const initialItems = [
        {
            product: {
                price: {
                    value: 999,
                },
            },
            count: 3,
        },
        {
            product: {
                price: {
                    value: 499,
                },
            },
            count: 5,
        },
    ];

    it('Корректно вычисляет полную сумму', () => {
        expect(countTotal(initialItems)).toStrictEqual({
            count: 8,
            promoCodeDiscount: {
                currencyId: undefined,
                value: 0,
            },
            shopDiscount: {
                currencyId: undefined,
                value: 0,
            },
            total: {
                currencyId: undefined,
                value: 5492,
            },
        });
    });

    it('Корректно вычисляет полную сумму до скидки', () => {
        expect(countTotal([
            ...initialItems,
            {
                product: {
                    price: {
                        value: 199,
                        oldValue: 299,
                    },
                },
                count: 2,
            },
        ])).toStrictEqual({
            count: 10,
            promoCodeDiscount: {
                currencyId: undefined,
                value: 0,
            },
            shopDiscount: {
                currencyId: undefined,
                value: 200,
            },
            total: {
                currencyId: undefined,
                value: 5890,
                oldValue: 6090,
            },
        });
    });

    it('Корректно устанавливает валюту', () => {
        expect(countTotal([
            {
                product: {
                    price: {
                        value: 199,
                        currencyId: ECurrencyAvailable.UAH,
                    },
                },
                count: 1,
            },
            ...initialItems,
            {
                product: {
                    price: {
                        value: 299,
                        currencyId: ECurrencyAvailable.RUR,
                    },
                },
                count: 2,
            },
        ])).toStrictEqual({
            count: 11,
            promoCodeDiscount: {
                currencyId: ECurrencyAvailable.UAH,
                value: 0,
            },
            shopDiscount: {
                currencyId: ECurrencyAvailable.UAH,
                value: 0,
            },
            total: {
                currencyId: ECurrencyAvailable.UAH,
                value: 6289,
            },
        });
    });
});
