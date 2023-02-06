import { getMeta } from '../getMeta';

describe('getMeta', () => {
    const expectedOgMeta = {
        title: 'Фильтры - Яндекс.Маркет',
        image: '//yastatic.net/market-export/_/i/marketplace/opengraph-image-1024x512.png',
        site_name: 'Яндекс.Маркет',
        type: 'website',
    };
    const expectedSeo = {
        ograph: expectedOgMeta,
        title: expectedOgMeta.title,
    };

    it('возвращает корректные meta описания для seo', () => {
        expect(getMeta()).toEqual(expectedSeo);
    });
});
