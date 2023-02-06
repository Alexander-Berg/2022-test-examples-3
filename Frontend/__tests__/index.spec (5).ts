import { IOffer, IProduct, ISku } from '@yandex-turbo/applications/beru.ru/interfaces';
import { isSku, getTitle, getProduct } from '..';

describe('Entity: Sku', () => {
    describe('ф-ия isSku', () => {
        it('должна верно опредялять что на входе передан ску', () => {
            expect(isSku(<ISku>{ entity: 'sku' })).toBe(true);
            expect(isSku(<IProduct>{ entity: 'product' })).toBe(false);
            expect(isSku(<IOffer>{ entity: 'offer' })).toBe(false);
        });
    });

    describe('ф-ия getTitle', () => {
        it('должна возвращать заголовок sku', () => {
            expect(getTitle(<ISku>{ titles: { raw: 'Title' } })).toEqual('Title');
        });
    });

    describe('ф-ия getProduct', () => {
        it('должна возвращать продукт из sku', () => {
            const product = <IProduct>{ entity: 'product' };

            expect(getProduct(<ISku>{ titles: { raw: 'Title' }, product })).toEqual({ entity: 'product' });
        });
    });
});
