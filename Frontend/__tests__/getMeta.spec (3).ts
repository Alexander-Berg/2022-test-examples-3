import { layoutData } from '@yandex-turbo/applications/beru.ru/__mocks__/layoutData';
import { ISku } from '@yandex-turbo/applications/beru.ru/interfaces';
import { getImage } from '../getImage';
import { getMeta } from '../getMeta';
const mockMetaTitle = jest.fn();
const mockMetaDescription = jest.fn();
const mockCanonicalUrl = jest.fn();

jest.mock('../getMetaTitle', () => ({
    // eslint-disable-next-line
    getMetaTitle: (rawTitle: any, productTypes: any) => mockMetaTitle(rawTitle, productTypes),
}));

jest.mock('../getMetaDescription', () => ({
    // eslint-disable-next-line
    getMetaDescription: (rawTitle: any, productTypes: any) => mockMetaDescription(rawTitle, productTypes),
}));

jest.mock('../getCanonicalUrl', () => ({
    getCanonicalUrl: (skuId: string, slug: string) => mockCanonicalUrl(skuId, slug),
}));

jest.mock('../getImage', () => ({
    getImage: jest.fn(),
}));

describe('getMeta', () => {
    const expectedOgMeta = {
        title: 'Купить книгу kek по' +
            ' низкой цене с доставкой из маркетплейса Беру',
        description: 'Выберите книгу kek в интернет-магазине' +
            ' по отзывам, характеристикам, ценам и стоимости доставки по России.' +
            ' Купите книгу kek на маркетплейсе Беру выгодно!',
        url: 'https://beru.ru/product/smartfon-apple-iphone-x-64gb-seryi-kosmos-mqac2ru-a/100210864686',
        site_name: 'Яндекс.Маркет',
        image: 'path/to/image.png',
        type: 'website',
    };
    const expectedSeo = {
        ograph: expectedOgMeta,
        description: expectedOgMeta.description,
        url: expectedOgMeta.url,
        title: expectedOgMeta.title,
    };

    beforeEach(() => {
        mockMetaTitle.mockImplementation(() => expectedOgMeta.title);
        mockMetaDescription.mockImplementation(() => expectedOgMeta.description);
        mockCanonicalUrl.mockImplementation(() => expectedOgMeta.url);
        (getImage as ReturnType<typeof jest.fn>).mockReturnValue(expectedOgMeta.image);
    });

    it('Должен вернуть тайтл, описание, и каноникал для соответствующего типа', () => {
        expect(getMeta(layoutData.result as ISku, layoutData.knownThumbnails)).toEqual(expectedSeo);
        expect(mockMetaTitle).toHaveBeenCalledWith('title', {
            nominative: 'книга',
            accusative: 'книгу',
            prepositional: 'книге',
            genitive: 'книги',
        });
        expect(mockMetaDescription).toHaveBeenCalledWith('title', {
            nominative: 'книга',
            accusative: 'книгу',
            prepositional: 'книге',
            genitive: 'книги',
        });
        expect(mockCanonicalUrl).toHaveBeenCalledWith(
            (layoutData.result as ISku).id,
            (layoutData.result as ISku).slug
        );
    });

    it('seo описание должно строится даже при отсутствии поля product', () => {
        expect(getMeta({ ...layoutData.result, product: undefined } as ISku, layoutData.knownThumbnails)).toEqual(expectedSeo);
    });
});
