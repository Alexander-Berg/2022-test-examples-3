import { cloneDeep } from 'lodash';

import { IProduct } from '~/types';
import { ECurrencyAvailable } from '~/components/Cost/Cost.types';
import { getItemsChanges } from '../utils';

describe('cart/changes', () => {
    describe('getItemsChanges()', () => {
        const products: Dictionary<IProduct> = {
            '1': {
                id: '1',
                name: 'Портативное цифровое пианино Yamaha P-45B',
                href: '/turbo?stub=productpage/product-1-server-for-screens.json',
                meta: 'CiAHMb',
                price: { currencyId: ECurrencyAvailable.RUR, value: 32990 },
                isAvailable: true,
                thumb: '//avatars.mds.yandex.net/get-turbo/1868007/rth14dcb6d529ea4e80c1c260ca3f881956/',
            },
            '2': {
                id: '2',
                name: 'Укулеле Veston KUS 15GR Veston KUS 15GR Veston KUS 15GR Veston KUS 15GR',
                href: '/turbo?stub=productpage/product-2-server-for-screens.json',
                meta: 'CiBWek',
                price: { currencyId: ECurrencyAvailable.RUR, value: 2360 },
                isAvailable: false,
                thumb: '//avatars.mds.yandex.net/get-turbo/1960623/rtha02eb4e5245b5dc4daa1c771613d6b87/',
            },
            '3': {
                id: '3',
                name: 'Гитарный преамп Orange Bax Bangeetar (BLK)',
                href: '/turbo?stub=productpage/product-3-server-for-screens.json',
                meta: 'CiA66+',
                price: { currencyId: ECurrencyAvailable.RUR, value: 30990 },
                isAvailable: true,
            },
        };

        it('Должен определять изменение доступного количества товара', () => {
            const changes = getItemsChanges(
                [{ id: '1', count: 1 }, { id: '2', count: 2 }],
                products,
                [{ id: '1', count: 1 }, { id: '2', count: 1 }],
                products,
            );
            expect(changes).toStrictEqual({
                title: 'Увы, этот товар уже разобрали',
                changes: [{
                    id: '2',
                    count: 1,
                }],
            });
        });

        it('Должен определять отсутствие', () => {
            const changes = getItemsChanges(
                [{ id: '1', count: 1 }, { id: '2', count: 2 }, { id: '3', count: 3 }],
                products,
                [{ id: '1', count: 1 }, { id: '2', count: 2 }],
                products,
            );
            expect(changes).toStrictEqual({
                title: 'Увы, этот товар уже разобрали',
                changes: [{
                    id: '3',
                    count: 0,
                }],
            });
        });

        it('Должен определять изменение количества нескольких товаров', () => {
            const changes = getItemsChanges(
                [{ id: '1', count: 1 }, { id: '2', count: 2 }, { id: '3', count: 3 }],
                products,
                [{ id: '1', count: 1 }, { id: '2', count: 1 }],
                products,
            );
            expect(changes).toStrictEqual({
                title: 'Увы, эти товары уже разобрали',
                changes: [{
                    id: '2',
                    count: 1,
                }, {
                    id: '3',
                    count: 0,
                }],
            });
        });

        it('Должен определять изменение цены по промокоду', () => {
            const promoCode = {
                code: 'TESTCODE',
            };
            const changes = getItemsChanges(
                [{ id: '1', count: 1 }, { id: '2', count: 2 }],
                products,
                [{ id: '1', count: 1 }, { id: '2', count: 2, discountedPrice: { value: 2500 } }],
                products,
                promoCode
            );
            expect(changes).toStrictEqual({
                title: 'Условия промокода TESTCODE изменились',
                changes: [{
                    id: '2',
                    discountedPrice: { value: 2500 },
                }],
            });
        });

        it('Должен определять изменение цены нескольких товаров по промокоду', () => {
            const promoCode = {
                code: 'TESTCODE',
            };
            const changes = getItemsChanges(
                [{ id: '1', count: 1 }, { id: '2', count: 2 }],
                products,
                [{ id: '1', count: 1, discountedPrice: { value: 100 } }, { id: '2', count: 2, discountedPrice: { value: 2500 } }],
                products,
                promoCode
            );
            expect(changes).toStrictEqual({
                title: 'Условия промокода TESTCODE изменились',
                changes: [{
                    id: '1',
                    discountedPrice: { value: 100 },
                }, {
                    id: '2',
                    discountedPrice: { value: 2500 },
                }],
            });
        });

        it('Должен определять изменение цены товара', () => {
            const nextProducts = cloneDeep(products);
            nextProducts['1'].price.value = 35000;

            const changes = getItemsChanges(
                [{ id: '1', count: 1 }, { id: '2', count: 2 }],
                products,
                [{ id: '1', count: 1 }, { id: '2', count: 2 }],
                nextProducts,
            );
            expect(changes).toStrictEqual({
                title: 'Цена изменилась',
                changes: [{
                    id: '1',
                    price: {
                        currencyId: 'RUR',
                        value: 35000,
                    },
                }],
            });
        });

        it('Должен определять изменение количества и цены товара одновременно', () => {
            const nextProducts = cloneDeep(products);
            nextProducts['2'].price.value = 2500;

            const changes = getItemsChanges(
                [{ id: '1', count: 1 }, { id: '2', count: 2 }],
                products,
                [{ id: '1', count: 1 }, { id: '2', count: 1 }],
                nextProducts,
            );
            expect(changes).toStrictEqual({
                title: 'Упс, что-то изменилось',
                changes: [{
                    id: '2',
                    count: 1,
                    price: {
                        currencyId: 'RUR',
                        value: 2500,
                    },
                }],
            });
        });

        it('Должен определять изменение количества одного товара и цены другого', () => {
            const nextProducts = cloneDeep(products);
            nextProducts['1'].price.value = 35000;

            const changes = getItemsChanges(
                [{ id: '1', count: 1 }, { id: '2', count: 2 }],
                products,
                [{ id: '1', count: 1 }, { id: '2', count: 1 }],
                nextProducts,
            );
            expect(changes).toStrictEqual({
                title: 'Упс, что-то изменилось',
                changes: [{
                    id: '1',
                    price: {
                        currencyId: 'RUR',
                        value: 35000,
                    },
                }, {
                    id: '2',
                    count: 1,
                }],
            });
        });

        it('Должен определять изменение цены одного товара, количества и цены по промокоду другого', () => {
            const nextProducts = cloneDeep(products);
            nextProducts['1'].price.value = 99000;

            const changes = getItemsChanges(
                [{ id: '1', count: 1 }, { id: '2', count: 2, discountedPrice: { value: 70000 } }],
                products,
                [{ id: '1', count: 1 }, { id: '2', count: 1 }],
                nextProducts,
            );
            expect(changes).toStrictEqual({
                title: 'Упс, что-то изменилось',
                changes: [{
                    id: '1',
                    price: {
                        currencyId: 'RUR',
                        value: 99000,
                    },
                }, {
                    id: '2',
                    count: 1,
                    discountedPrice: undefined,
                }],
            });
        });
    });
});
