import { getMeta, TParams } from '../getMeta';

describe('getMeta', () => {
    let options: TParams;
    const expectedOgMeta = {
        title: 'Купить Телефоны по низким ценам в интернет-магазинах - Яндекс.Маркет',
        description: 'Телефоны - купить в Яндекс.Маркете. Выбор товаров из категории Телефоны по характеристикам, описанию и отзывам с удобной доставкой.',
        url: 'https://pokupki.market.yandex.ru/catalog/telefon/123/list',
        site_name: 'Яндекс.Маркет',
        image: '//yastatic.net/market-export/_/i/marketplace/opengraph-image-1024x512.png',
        type: 'website',
    };
    const expectedSeo = {
        ograph: expectedOgMeta,
        description: expectedOgMeta.description,
        title: expectedOgMeta.title,
        url: expectedOgMeta.url,
    };

    beforeEach(() => {
        options = {
            nid: 123,
            slug: 'telefon',
            name: 'Телефоны',
        };
    });

    it('возвращает корректные meta описания для seo, если не переданы options.params', () => {
        expect(getMeta(options)).toEqual(expectedSeo);
    });

    it('возвращает корректные meta описания для seo, если переданы options.params', () => {
        options.params = { page: 1 };

        expect(getMeta(options)).toMatchObject({
            ograph: expect.objectContaining({
                title: expectedSeo.title,
                url: expectedSeo.url,
            }),
            title: expectedSeo.title,
            url: expectedSeo.url,
        });

        options.params.page = 2;

        expect(getMeta(options)).toMatchObject({
            ograph: expect.objectContaining({
                title: expectedSeo.title + ' - страница 2',
                url: expectedSeo.url + '?page=2',
            }),
            title: expectedSeo.title + ' - страница 2',
            url: expectedSeo.url + '?page=2',
        });
    });
});
