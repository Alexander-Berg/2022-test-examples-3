import { getAnalytics } from '..';

describe('getAnalytics', () => {
    it('Параметры для КМ', () => {
        const params = {
            entityId: 12345,
            pageName: 'product',
        };
        const actual = getAnalytics(params)[0];

        expect(actual.params).toEqual({
            isTurbo: true,
            productId: params.entityId,
        });
    });

    it('Параметры для КО', () => {
        const params = {
            entityId: 'uN124Hu445',
            pageName: 'offer',
        };
        const actual = getAnalytics(params)[0];

        expect(actual.params).toEqual({
            isTurbo: true,
            offerId: params.entityId,
        });
    });

    it('Параметры для непонятной страницы', () => {
        const params = {
            entityId: 'uN124Hu445',
            pageName: 'unknown',
        };
        const actual = getAnalytics(params)[0];

        expect(actual.params).toEqual({
            isTurbo: true,
            entityId: params.entityId,
        });
    });
});
