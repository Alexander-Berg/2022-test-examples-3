import { IAppState } from 'applications/ecom-tap/types';
import { INavigationScreenProps } from '../../../../components/Navigation';

import { currentProductSelector } from '../selectors';

// @ts-ignore мокаем часть стейта
const state: IAppState = {
    entities: {
        products: {
            '111': {
                href: '/turbo?text=https://awesome.com/product/1',
                id: '111',
                name: '',
                price: {
                    value: 123,
                },
                thumb: '',
            },
            '222': {
                href: '/turbo/s/awesome.com/product/2',
                id: '222',
                name: '',
                price: {
                    value: 123,
                },
                thumb: '',
            },
            '333': {
                href: '/turbo?text=https://awesome.com/product/3',
                id: '333',
                name: '',
                price: {
                    value: 123,
                },
                thumb: '',
            },
        },
        categories: {},
        offers: {},
        favorites: {},
    },
};

describe('productPage selectors', () => {
    describe('Выбор текущего продукта', () => {
        it('Возвращает продукт по его id в URL', () => {
            const props: INavigationScreenProps = {
                location: {
                    hash: '',
                    pathname: '/turbo',
                    search: '?text=https://awesome.com/product/1&product_id=111',
                    state: {},
                },
                match: {
                    isExact: true,
                    params: {

                    },
                    path: '/turbo',
                    url: '/turbo',
                    query: {
                        product_id: '111',
                        text: 'https://awesome.com/product/1',
                    },
                },
            };
            const product = currentProductSelector(state, props);

            expect(product).toEqual(state.entities.products['111']);
        });

        it('Возвращает продукт по cgi-параметру text', () => {
            const props: INavigationScreenProps = {
                location: {
                    hash: '',
                    pathname: '/turbo',
                    search: '?text=https://awesome.com/product/3',
                    state: {},
                },
                match: {
                    isExact: true,
                    params: {},
                    path: '/turbo',
                    url: '/turbo',
                    query: {
                        text: 'https://awesome.com/product/3',
                    },
                },
            };
            const product = currentProductSelector(state, props);

            expect(product).toEqual(state.entities.products['333']);
        });

        it('Возвращает продукт по текущему пути', () => {
            const props: INavigationScreenProps = {
                location: {
                    hash: '',
                    pathname: '/turbo/s/awesome.com/product/2',
                    search: '',
                    state: {},
                },
                match: {
                    isExact: true,
                    params: {

                    },
                    path: '/turbo/s/awesome.com/product/2',
                    url: '/turbo/s/awesome.com/product/2',
                    query: {},
                },
            };
            const product = currentProductSelector(state, props);

            expect(product).toEqual(state.entities.products['222']);
        });

        it('Возвращает undefined, если продукт не найден', () => {
            const props: INavigationScreenProps = {
                location: {
                    hash: '',
                    pathname: '/turbo/s/awesome.com/product/42',
                    search: '',
                    state: {},
                },
                match: {
                    isExact: true,
                    params: {},
                    path: '/turbo/s/awesome.com/product/42',
                    url: '/turbo/s/awesome.com/product/42',
                    query: {},
                },
            };
            const product = currentProductSelector(state, props);

            expect(product).toBeUndefined();
        });
    });
});
