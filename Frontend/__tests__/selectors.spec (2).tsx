import { getState, getItems, getItem, getItemCount, getIsLoadging, getSummaryCount, getSummary } from '../selectors';

const item = {
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
    meta: 'meta',
};
const stateWithItems = {
    cart: {
        etag: 0,
        items: [
            item,
            {
                ...item,
                product: {
                    ...item.product,
                    id: '2',
                },
                count: 10,
            },
        ],
        summary: { count: 11, cost: 11000, costWithDelivery: 11000 },
    },
};

describe('cart selectors', () => {
    describe('getState', () => {
        it('должен вернуть содержание state', () => {
            expect(
                getState({
                    cart: {
                        items: [],
                        meta: 'FOO',
                        etag: 0,
                        summary: { count: 0, cost: 0, costWithDelivery: 0 },
                    },
                }),
                'вернул не cart'
            ).toEqual({
                items: [],
                meta: 'FOO',
                etag: 0,
                summary: { count: 0, cost: 0, costWithDelivery: 0 },
            });
        });

        it('должен вернуть initialState при пустом state', () => {
            expect(getState({}), 'вернулся не initState').toEqual({
                items: [],
                meta: null,
                etag: 0,
                summary: { count: 0, cost: 0, costWithDelivery: 0 },
            });
        });
    });

    describe('getSummary', () => {
        it('должен вернуть summary', () => {
            expect(
                getSummary({
                    cart: {
                        items: [],
                        meta: 'FOO',
                        etag: 0,
                        summary: { count: 42, cost: 4212, currencyId: 'RUR', costWithDelivery: 4212 },
                    },
                }),
                'вернул не summary'
            ).toEqual({ count: 42, cost: 4212, currencyId: 'RUR', costWithDelivery: 4212 });
        });

        it('должен вернуть initialState', () => {
            expect(
                getSummary({}),
                'вернул не initial summary'
            ).toEqual({ count: 0, cost: 0, costWithDelivery: 0 });
        });
    });

    describe('getSummaryCount', () => {
        it('должен вернуть itemCount', () => {
            expect(
                getSummaryCount({
                    cart: {
                        items: [],
                        meta: 'FOO',
                        etag: 0,
                        summary: { count: 42, cost: 4212, currencyId: 'RUR', costWithDelivery: 4212 },
                    },
                }),
                'вернул не кол-во товаров'
            ).toBe(42);
        });

        it('должен вернуть initialState', () => {
            expect(
                getSummaryCount({}),
                'вернул не initial count'
            ).toBe(0);
        });
    });

    describe('getItems', () => {
        it('должен вернуть пустой массив', () => {
            expect(
                getItems({}),
                'вернул не initial items'
            ).toEqual([]);
        });

        it('должен вернуть массив items', () => {
            expect(
                getItems(stateWithItems),
                'вернул не items'
            ).toEqual([
                item,
                { ...item, product: { ...item.product, id: '2' }, count: 10 },
            ]);
        });
    });

    describe('getItem', () => {
        it('должен вернуть undefined', () => {
            expect(
                getItem(stateWithItems, '123'),
                'вернул не undefined'
            ).toBe(undefined);

            expect(
                getItem(stateWithItems, { ...item, product: { ...item.product, id: '22' } }),
                'вернул не undefined'
            ).toBe(undefined);
        });

        it('должен вернуть item', () => {
            expect(
                getItem(stateWithItems, '2'),
                'вернул не item'
            ).toEqual({ ...item, product: { ...item.product, id: '2' }, count: 10 });

            expect(
                getItem(stateWithItems, { ...item, product: { ...item.product, id: '2' } }),
                'вернул не item'
            ).toEqual({ ...item, product: { ...item.product, id: '2' }, count: 10 });
        });
    });

    describe('getItemCount', () => {
        it('должен вернуть 0', () => {
            expect(
                getItemCount(stateWithItems, '123'),
                'вернул не 0'
            ).toBe(0);

            expect(
                getItemCount(stateWithItems, { ...item, product: { ...item.product, id: '22' } }),
                'вернул не 0'
            ).toBe(0);
        });

        it('должен вернуть item', () => {
            expect(
                getItemCount(stateWithItems, '2'),
                'вернул не item'
            ).toEqual(10);

            expect(
                getItemCount(stateWithItems, { ...item, product: { ...item.product, id: '2' } }),
                'вернул не item'
            ).toEqual(10);
        });
    });

    describe('getIsLoadging', () => {
        it('должен вернуть unefined', () => {
            expect(
                getIsLoadging(stateWithItems),
                'вернул не undefined'
            ).toBe(undefined);
        });

        it('должен вернуть false', () => {
            expect(
                getIsLoadging({ cart: { ...stateWithItems.cart, loading: false } }),
                'вернул не false'
            ).toBe(false);
        });

        it('должен вернуть true', () => {
            expect(
                getIsLoadging({ cart: { ...stateWithItems.cart, loading: true } }),
                'вернул не true'
            ).toBe(true);
        });
    });
});
