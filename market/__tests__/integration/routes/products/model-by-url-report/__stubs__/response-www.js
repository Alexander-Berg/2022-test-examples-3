/* Stubs */
const offer = require('../../__stubs__/offer.stub');
const settings = require('../../__stubs__/settings.stub');
const model = require('../../__stubs__/model.stub');

const offers = Array(17).fill(offer);

const Response = expect.objectContaining({
    model,
    offers,
    bucketInfo: expect.any(Object),
    shopInfo: {
        shopName: expect.any(String),
        url: expect.any(String),
    },
    searchInfo: expect.objectContaining({
        originalQuery: 'Блок питания AEROCOOL KCAS PLUS 500, 500Вт, 120мм, черный, retail [kcas-500 plus]',
        filteredQuery: 'Блок питания AEROCOOL KCAS PLUS 500, 500Вт, 120мм, черный, retail [kcas-500 plus]',
        convertedPrice: {
            value: expect.any(Number),
            currencyCode: 'RUR',
        },
        offersCount: expect.any(Number),
        urls: expect.any(Object),
        categories: [
            {
                id: expect.any(Number),
                name: expect.any(String),
                rank: expect.any(Number),
                url: expect.any(String),
            },
            {
                id: expect.any(Number),
                name: expect.any(String),
                rank: expect.any(Number),
                url: expect.any(String),
            },
            {
                id: expect.any(Number),
                name: expect.any(String),
                rank: expect.any(Number),
                url: expect.any(String),
            },
        ],
    }),
    userRegion: expect.any(String),
    pricebar: {
        constants: expect.any(Object),
    },
    tabs: {
        constants: expect.any(Object),
    },
    settings,
    info: {
        constants: expect.any(Object),
    },
    footer: {
        constants: expect.any(Object),
    },
    feedback: {
        constants: expect.any(Object),
    },
});

module.exports = Response;
