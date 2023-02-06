const RESPONSE = expect.objectContaining({
    id: expect.any(Number),
    date: expect.any(String),
    grade: expect.any(Number),
    agreeCount: expect.any(Number),
    disagreeCount: expect.any(Number),
    author: expect.objectContaining({
        visibility: expect.any(String),
        uid: expect.any(Number),
    }),
    recommend: expect.any(Boolean),
    usageTime: expect.any(String),
    verifiedBuyer: expect.any(Boolean),
    model: expect.objectContaining({
        id: expect.any(Number),
    }),
    agreeCountText: expect.any(String),
    disagreeCountText: expect.any(String),
    pros: expect.any(String),
    cons: expect.any(String),
    constants: expect.any(Object),
    text: expect.any(String),
});

module.exports = RESPONSE;
