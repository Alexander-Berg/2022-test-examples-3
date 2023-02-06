/* Stubs */
const cbirOffer = require('../../__stubs__/search-offer.stub');
const settings = require('../../__stubs__/settings.stub');

const offers = Array(5).fill(cbirOffer);

const RESPONSE = expect.objectContaining({
    offers,
    bucketInfo: expect.any(Object),
    shopInfo: {
        shopName: expect.any(String),
        url: expect.any(String),
    },

    searchInfo: {
        originalQuery: 'Francesco Marconi 84520mg carmen nero',
        filteredQuery: 'Francesco Marconi 84520mg carmen nero',
        convertedPrice: {
            value: expect.any(Number),
            currencyCode: 'RUR',
        },
        offersCount: expect.any(Number),
        urls: expect.any(Object),
        category: {
            id: expect.any(Number),
            name: expect.any(String),
            categoryOffersCount: expect.any(Number),
            categorySimilarItemsPhrase: expect.any(String),
            categoryItemsPhrase: expect.any(String),
            categoryFoundPhrase: expect.any(String),
        },
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
    },

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

module.exports = RESPONSE;
