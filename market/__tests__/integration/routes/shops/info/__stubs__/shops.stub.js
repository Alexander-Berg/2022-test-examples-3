const RESPONSE = {
    region: {
        id: expect.any(Number),
        name: expect.any(String),
        type: expect.any(String),
        childCount: expect.any(Number),
        country: {
            id: expect.any(Number),
            name: expect.any(String),
            type: expect.any(String),
            childCount: expect.any(Number),
        },
    },
    id: expect.any(Number),
    name: expect.any(String),
    domain: 'pleer.ru',
    registered: expect.any(String),
    type: expect.any(String),
    opinionUrl: expect.any(String),
    outlets: expect.any(Array),
    context: {
        region: {
            id: expect.any(Number),
            name: expect.any(String),
            type: expect.any(String),
            childCount: expect.any(Number),
            country: expect.any(Object),
        },
        currency: {
            id: expect.any(String),
            name: expect.any(String),
        },
        id: expect.any(String),
        time: expect.any(String),
        marketUrl: expect.any(String),
    },
};
module.exports = RESPONSE;
