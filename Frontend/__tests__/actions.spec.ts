import { ECurrencyAvailable } from '~/types';
import { IItemResponse } from '~/api/fetchProducts';
import { processItems } from '../actions';

describe('Cart actions', () => {
    describe('processItems', () => {
        let items: IItemResponse[];

        beforeEach(() => {
            items = [
                {
                    count: 1,
                    product: {
                        id: 'test',
                        description: 'Test product',
                        href: 'https://yandex.ru/turbo',
                        meta: 'CiDnkhYzLm2kx8',
                        available: true,
                        thumb: {
                            src: 'https://avatars.mds.yandex.net/get-turbo',
                            height: 480,
                            block: 'image',
                            width: 480,
                        },
                        price: {
                            current: '999',
                            old: '1000',
                            currencyId: 'RUR',
                        },
                    },
                },
            ];
        });

        it('Возвращает корректный объект цены', () => {
            expect(processItems(items).products.test.price).toStrictEqual({
                value: 999,
                oldValue: 1000,
                currencyId: ECurrencyAvailable.RUR,
            });
        });

        it('Корректно обрабатывает отсутствие старой цены', () => {
            delete items[0].product.price.old;

            expect(processItems(items).products.test.price.oldValue).toBeUndefined();
        });
    });
});
