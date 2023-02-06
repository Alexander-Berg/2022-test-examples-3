/**
 * Various utils for testing
 */

function createRequest(overrides = {}) {
    const req = { ...overrides };
    return req;
}

function createResponse(overrides = {}) {
    const res = { ...overrides };
    return res;
}

function createNext() {
    return jest.fn().mockName('next');
}

module.exports = {
    createRequest,
    createResponse,
    createNext,
};
