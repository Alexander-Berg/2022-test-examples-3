import { ICartItem } from '@yandex-turbo/components/CartItem/CartItem';
import {
    addItemToCart,
    updateItemCount,
    removeItem,
    updateState,
} from '../actions';
import { cartReducer } from '../reducer';

describe('cartReducer', () => {
    const initState = {
        items: [],
        meta: null,
        etag: 0,
        summary: {
            count: 0,
            cost: 0,
            costWithDelivery: 0,
        },
    };
    const items: ICartItem[] = [
        {
            product: {
                id: '1',
                description: '',
                price: {
                    current: 1000,
                },
                thumb: {
                    src: 'src',
                    height: 10,
                    width: 10,
                },
                meta: 'meta_product',
            },
            count: 1,
            meta: 'meta_item',
        },
        {
            product: {
                id: '2',
                description: '',
                price: {
                    current: 2000,
                },
                thumb: {
                    src: 'src',
                    height: 10,
                    width: 10,
                },
                meta: 'meta_product',
            },
            count: 42,
            meta: 'meta_item',
        },
    ];

    it('должен инитится дефолтным стейтом', () => {
        expect(cartReducer(undefined, { type: '@INIT' })).toEqual(initState);
    });

    it('должен вернуть прежний объект', () => {
        expect(cartReducer(initState, { type: '@RANDOM' })).toBe(initState);
    });

    it('должен возвращать новый объект', () => {
        expect(cartReducer(initState, removeItem('random'))).not.toBe(
            initState
        );
    });

    it('должен перезаписать состояние (updateState)', () => {
        const state = cartReducer(
            { items: [], meta: null, etag: 0, summary: { cost: 0, count: 0, costWithDelivery: 0 } },
            updateState({ items, meta: 'ok', etag: 1 })
        );

        expect(state).toEqual({
            items,
            meta: 'ok',
            etag: 1,
            summary: { count: 43, cost: 85000, currencyId: undefined, costWithDelivery: 85000 },
        });
        expect(
            cartReducer(state, updateState({ items: [], meta: null, etag: 0 }))
        ).toEqual(initState);
    });

    it('должен добавлять но товар (addItemToCart)', () => {
        let state = cartReducer(initState, addItemToCart(items[0]));

        expect(state, 'Не добавился первый товар').toEqual({
            ...initState,
            items: [items[0]],
            summary: { count: 1, cost: 1000, currencyId: undefined, costWithDelivery: 1000 },
            meta: null,
        });

        state = cartReducer(state, addItemToCart(items[0]));

        expect(
            state,
            'Кол-во товара не увеличилось'
        ).toEqual({
            ...state,
            items: [
                {
                    ...items[0],
                    count: 2,
                },
            ],
            summary: { count: 2, cost: 2000, currencyId: undefined, costWithDelivery: 2000 },
            meta: null,
            etag: 0,
        });

        expect(
            cartReducer(state, addItemToCart(items[1])),
            'Еще один товар не добавился в корзину'
        ).toEqual({
            ...state,
            items: [
                {
                    ...items[0],
                    count: 2,
                },
                {
                    ...items[1],
                    count: 1,
                },
            ],
            meta: null,
            summary: { count: 3, cost: 4000, currencyId: undefined, costWithDelivery: 4000 },
            etag: 0,
        });
    });

    it('должен устанавливать кол-во товара (updateItemCount)', () => {
        expect(
            cartReducer(
                { items, meta: 'meta_item', etag: 0, summary: { count: 0, cost: 0, costWithDelivery: 0 } },
                updateItemCount('1', 13)
            ),
            'Кол-во товара не изменилось'
        ).toEqual({
            items: [
                {
                    ...items[0],
                    count: 13,
                },
                items[1],
            ],
            meta: 'meta_item',
            summary: { count: 55, cost: 97000, currencyId: undefined, costWithDelivery: 97000 },
            etag: 0,
        });
    });

    it('должен удалять товар из корзины (removeItem)', () => {
        expect(
            cartReducer({ items, etag: 0, summary: { count: 0, cost: 0, costWithDelivery: 0 } }, removeItem('2')),
            'Товар не удален'
        ).toEqual({
            items: [items[0]],
            summary: { count: 1, cost: 1000, currencyId: undefined, costWithDelivery: 1000 },
            etag: 0,
        });

        expect(
            cartReducer({ items, etag: 0, summary: { count: 0, cost: 0, costWithDelivery: 0 } }, removeItem('2-random')),
            'Товары не должны меняться'
        ).toEqual({
            items,
            summary: { count: 43, cost: 85000, currencyId: undefined, costWithDelivery: 85000 },
            etag: 0,
        });
    });
});
