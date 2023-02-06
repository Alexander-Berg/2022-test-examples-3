'use strict';

const RESPONSE = expect.objectContaining({
    id: expect.any(Number),
    date: expect.any(String),
    grade: expect.any(Number),
    state: expect.any(String),
    agreeCount: expect.any(Number),
    disagreeCount: expect.any(Number),
    author: expect.objectContaining({
        visibility: expect.any(String),
    }),
    recommend: expect.any(Boolean),
    usageTime: expect.any(String),
    verifiedBuyer: expect.any(Boolean),
    model: expect.objectContaining({
        id: expect.any(Number),
    }),
    facts: expect.anything(),
    agreeCountText: expect.any(String),
    disagreeCountText: expect.any(String),
    constants: expect.any(Object),
});

module.exports = RESPONSE;
