/* Stubs */
const offer = require('../../__stubs__/search-offer.stub');
const settings = require('../../__stubs__/settings.stub');

const offers = Array(31).fill(offer);

const RESPONSE = expect.objectContaining({
    offers,
    rules: ['second-script'],
    bucketInfo: expect.any(Object),
    searchInfo: expect.objectContaining({
        originalQuery: expect.any(String),
        filteredQuery: expect.any(String),
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

module.exports = RESPONSE;
