const RESPONSE = expect.objectContaining({
    name: expect.any(String),
    reviewsCount: expect.any(Number),
    rating: expect.any(Number),
    prices: expect.objectContaining({
        max: expect.any(Number),
        min: expect.any(Number),
        avg: expect.any(Number),
        curCode: expect.any(String),
        curName: expect.any(String),
        currency: expect.any(String),
    }),
    offersCount: expect.any(Number),
    mainPhoto: expect.objectContaining({
        width: expect.any(Number),
        height: expect.any(Number),
        url: expect.any(String),
    }),
    photo: expect.any(String),
    isNew: expect.any(Boolean),

    urls: expect.objectContaining({
        model: expect.any(String),
        modelPicture: expect.any(String),
        offers: expect.any(String),
        map: expect.any(String),
        price: expect.any(String),
        reviews: expect.any(String),
        reviewsBadge: expect.any(String),
        footerAveragePrice: expect.any(String),
    }),
    links: {
        constants: expect.any(Object),
    },
    info: {
        constants: expect.any(Object),
    },
});

module.exports = RESPONSE;
