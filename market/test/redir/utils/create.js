/**
 * Various utils for testing
 */

function createRequest(overrides = {}) {
    const req = { body: {}, cookies: {}, ...overrides };
    return req;
}

function createResponse(overrides = {}) {
    const res = {
        cookie: jest.fn().mockName('clearCookie'),
        clearCookie: jest.fn().mockName('clearCookie'),
        ...overrides,
    };
    return res;
}

function createNext() {
    return jest.fn().mockName('next');
}

function createAffId(affId = 'FAKE_AFF_ID') {
    return affId;
}

function createClid(clid = 'FAKE_CLID') {
    return clid;
}

module.exports = {
    createRequest,
    createResponse,
    createNext,
    createAffId,
    createClid,
};
