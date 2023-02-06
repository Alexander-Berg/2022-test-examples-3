/* Stubs */
const offer = require('../../__stubs__/search-offer.stub');
const settings = require('../../__stubs__/settings.stub');

const offers = Array(2).fill(offer);

const RESPONSE = expect.objectContaining({
    offers,

    bucketInfo: expect.any(Object),
    shopInfo: {
        shopName: expect.any(String),
        url: expect.any(String),
    },

    searchInfo: {
        originalQuery: 'Гэри Джанни: Corpus Monstrum',
        filteredQuery: 'Гэри Джанни: Corpus Monstrum',
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
        category: expect.any(Object),
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
