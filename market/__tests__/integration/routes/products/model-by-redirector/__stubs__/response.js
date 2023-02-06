/* Stubs */
const offer = require('../../__stubs__/offer.stub');
const opinion = require('../../__stubs__/opinion.stub');
const settings = require('../../__stubs__/settings.stub');
const model = require('../../__stubs__/model.stub');

const offers = Array(9).fill(offer);
const opinions = Array(10).fill(opinion);

const RESPONSE = expect.objectContaining({
    model,
    offers,

    bucketInfo: expect.any(Object),
    shopInfo: {
        shopName: expect.any(String),
        url: expect.any(String),
    },
    opinions,

    searchInfo: {
        originalQuery: '2 ТБ Жесткий диск Seagate BarraCuda [ST2000DM008]',
        filteredQuery: '2 ТБ Жесткий диск Seagate BarraCuda [ST2000DM008]',
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
    },
    pricebar: {
        constants: expect.any(Object),
    },
    tabs: {
        constants: expect.any(Object),
    },
    userRegion: expect.any(String),
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
    opinionsInfo: {
        constants: expect.any(Object),
    },
    specifications: expect.any(Array),
});

module.exports = RESPONSE;
