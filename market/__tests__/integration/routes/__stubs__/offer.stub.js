'use strict';

const RESPONSE = expect.objectContaining({
    name: expect.any(String),
    price: expect.objectContaining({
        value: expect.any(Number),
        currencyCode: expect.any(String),
        currencyName: expect.any(String),
    }),
    category: expect.objectContaining({
        id: expect.any(Number),
        name: expect.any(String),
        fullName: expect.any(String),
        type: expect.any(String),
        childCount: expect.any(Number),
        advertisingModel: expect.any(String),
        viewType: expect.any(String),
    }),
    target: expect.any(String),
    shopInfo: expect.objectContaining({
        name: expect.any(String),
        id: expect.any(Number),
        rating: expect.any(Number),
        gradeTotal: expect.any(Number),
        url: expect.any(String),
    }),
    delivery: expect.any(Object),
    source: expect.any(String),
    id: expect.any(String),
    url: expect.any(String),
    buttonUrl: expect.any(String),
    constants: expect.any(Object),
});

module.exports = RESPONSE;
