/* Stubs */
const offer = require('../../__stubs__/offer.stub');
const opinion = require('../../__stubs__/opinion.stub');
const settings = require('../../__stubs__/settings.stub');
const model = require('../../__stubs__/model.stub');

const offers = Array(17).fill(offer);
const opinions = Array(10).fill(opinion);

const RESPONSE = expect.objectContaining({
    offers,
    settings,
    opinions,
    model,
    bucketInfo: expect.any(Object),
    shopInfo: {
        shopName: expect.any(String),
        url: expect.any(String),
    },
    pricebar: {
        constants: expect.any(Object),
    },
    tabs: {
        constants: expect.any(Object),
    },
    info: {
        constants: expect.any(Object),
    },
    footer: {
        constants: expect.any(Object),
    },
    feedback: {
        constants: expect.any(Object),
    },

    opinionsInfo: {
        constants: expect.any(Object),
    },
    searchInfo: expect.objectContaining({
        originalQuery: 'Яндекс.Станция',
        filteredQuery: '',
        convertedPrice: {
            value: expect.any(Number),
            currencyCode: 'RUR',
        },
        offersCount: expect.any(Number),
        urls: expect.any(Object),
    }),
});

module.exports = RESPONSE;
