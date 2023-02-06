const RESPONSE = expect.objectContaining({
    name: expect.any(String),
    price: expect.objectContaining({
        value: expect.any(Number),
        currencyCode: expect.any(String),
        currencyName: expect.any(String),
    }),
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
    offerSurfaceUrl: expect.any(String),
});

module.exports = RESPONSE;
