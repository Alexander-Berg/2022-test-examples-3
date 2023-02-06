import {
    isRealOffer,
    getSkuId,
    getRating,
    getRatingCount,
    getOpinions,
    getStats,
    isOffer,
    getCPALink,
    getDirectLink,
    getOfferId,
    getShowPlaceId,
} from '..';
import { IOffer, IProduct, ISku } from '@yandex-turbo/applications/beru.ru/interfaces';

describe('Entity: Offer', () => {
    describe('ф-ия isRealOffer', () => {
        it('должна возвращать true, если это валидный офер', () => {
            expect(isRealOffer(<IOffer>{ cpa: 'real' })).toBe(true);
        });

        it('должна возвращать false если офер не валидный', () => {
            expect(isRealOffer(<IOffer>{ cpa: 'unknown' })).toBe(false);
        });
    });

    describe('ф-ия getSkuId', () => {
        it('должна возвращать skuId из поля marketSku', () => {
            expect(getSkuId(<IOffer>{ marketSku: '123', sku: '333' })).toBe('123');
        });

        it('должна возвращать skuId из поля sku, если нет marketSku', () => {
            expect(getSkuId(<IOffer>{ sku: '333' })).toBe('333');
        });
    });

    describe('ф-ия getRating', () => {
        it('должна возвращать усредненное значение рейтинга', () => {
            expect(getRating(<IOffer>{ productInfo: { rating: 10 } })).toBe(10);
        });
    });

    describe('ф-ия getRatingCount', () => {
        it('должна возвращать общее кол-во рейтинга', () => {
            expect(getRatingCount(<IOffer>{ productInfo: { ratingCount: 22 } })).toBe(22);
        });
    });

    describe('ф-ия getOpinions', () => {
        it('должна возвращать кол-во отзывов', () => {
            expect(getOpinions(<IOffer>{ productInfo: { opinions: 33 } })).toBe(33);
        });
    });

    describe('ф-ия getStats', () => {
        it('должна возвращать информацию о отзывах и рейтинге товара', () => {
            expect(getStats(<IOffer>{ productInfo: { opinions: 33, ratingCount: 22, rating: 10 } })).toEqual({
                opinions: 33,
                ratingCount: 22,
                rating: 10,
            });
        });
    });

    describe('ф-ия isOffer', () => {
        it('должна верно опредялять что на входе передан офер', () => {
            expect(isOffer(<IOffer>{ entity: 'offer' })).toBe(true);
            expect(isOffer(<IProduct>{ entity: 'product' })).toBe(false);
            expect(isOffer(<ISku>{ entity: 'sku' })).toBe(false);
        });
    });

    describe('ф-ия getCPALink', () => {
        it('должна возвращать CPA ссылку', () => {
            expect(getCPALink(<IOffer>{ urls: { encryptedTurboBundle: '/test' } })).toBe('/test');
        });
    });

    describe('ф-ия getDirectLink', () => {
        it('должна возвращать прямую ссылку', () => {
            expect(getDirectLink(<IOffer>{ urls: { directTurboBundle: 'https://test.test/path' } })).toBe('https://test.test/path');
        });
    });

    describe('ф-ия getOfferId', () => {
        it('должна возвращать идентификатор офера', () => {
            expect(getOfferId(<IOffer>{ wareId: '123' })).toBe('123');
        });
    });

    describe('ф-ия getShowPlaceId', () => {
        it('должна возвращать идентификатор места показа', () => {
            expect(getShowPlaceId(<IOffer>{ feeShow: '123' })).toBe('123');
        });
    });
});
