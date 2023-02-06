import { IOffer, IProduct, ISku } from '@yandex-turbo/applications/beru.ru/interfaces';
import { isProduct, getStats } from '..';

describe('Entity: Product', () => {
    describe('ф-ия isProduct', () => {
        it('должна верно опредялять что на входе передан продукт', () => {
            expect(isProduct(<IProduct>{ entity: 'product' })).toBe(true);
            expect(isProduct(<IOffer>{ entity: 'offer' })).toBe(false);
            expect(isProduct(<ISku>{ entity: 'sku' })).toBe(false);
        });
    });

    describe('ф-ия getStats', () => {
        it('должна возвращать информацию о отзывах и рейтинге товара', () => {
            expect(getStats(<IProduct>{ opinions: 33, ratingCount: 22, rating: 10 })).toEqual({
                opinions: 33,
                ratingCount: 22,
                rating: 10,
            });
        });
    });
});
