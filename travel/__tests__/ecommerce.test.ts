import {ECommerceActionType} from 'utilities/metrika/types/ecommerce';

import {eCommercePush} from '../ecommerce';

describe('eCommercePush', () => {
    let originalDataLayer: any[];

    beforeEach(() => {
        originalDataLayer = window.dataLayer || [];
        window.dataLayer = [];
    });

    afterEach(() => {
        window.dataLayer = originalDataLayer;
    });

    it('Должен добавить новую запись в window.dataLayer', () => {
        const products = [
            {
                id: 'product-id',
                name: 'product-name',
            },
        ];

        eCommercePush(ECommerceActionType.DETAIL, products);

        expect(window.dataLayer).toEqual([
            {
                ecommerce: {
                    currencyCode: 'RUB',
                    detail: {products},
                },
            },
        ]);
    });

    it('Должен добавить новую запись в window.dataLayer к существующей', () => {
        const products = [
            {
                id: 'product-id',
                name: 'product-name',
            },
        ];

        eCommercePush(ECommerceActionType.DETAIL, products);
        eCommercePush(ECommerceActionType.ADD, products);

        expect(window.dataLayer).toEqual([
            {
                ecommerce: {
                    currencyCode: 'RUB',
                    detail: {products},
                },
            },
            {
                ecommerce: {
                    currencyCode: 'RUB',
                    add: {products},
                },
            },
        ]);
    });

    it('Должен добавить новую запись с actionField в window.dataLayer', () => {
        const products = [
            {
                id: 'product-id',
                name: 'product-name',
            },
        ];
        const actionField = {id: 'order-id'};

        eCommercePush(ECommerceActionType.PURCHASE, products, actionField);

        expect(window.dataLayer).toEqual([
            {
                ecommerce: {
                    currencyCode: 'RUB',
                    purchase: {products, actionField},
                },
            },
        ]);
    });
});
