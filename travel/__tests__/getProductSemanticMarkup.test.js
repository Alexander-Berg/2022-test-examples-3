jest.dontMock('../../format/formatPrice');

const gatherMinPriceData = jest.fn();

jest.setMock('../../segments/gatherMinPriceData', gatherMinPriceData);

const getProductSemanticMarkup = require.requireActual(
    '../getProductSemanticMarkup',
).default;

const title = 'product title';
const segments = [];

describe('getProductSemanticMarkup', () => {
    it('если нет минимальных цен - вернет null', () => {
        gatherMinPriceData.mockReturnValueOnce(null);
        expect(getProductSemanticMarkup(title, segments)).toBeNull();
    });

    it('если есть минимальные цены - вернет специальную разметку', () => {
        gatherMinPriceData.mockReturnValueOnce({
            price: {
                value: 1200,
                currency: 'RUB',
            },
        });
        expect(getProductSemanticMarkup(title, segments)).toEqual({
            '@context': 'http://schema.org/',
            '@type': 'Product',
            offers: {
                '@type': 'AggregateOffer',
                lowPrice: '1200',
                priceCurrency: 'RUB',
            },
            name: 'product title',
        });
    });
});
