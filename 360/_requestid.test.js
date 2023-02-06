'use strict';

const middleware = require('./requestid.js');
const res = {};
const next = () => {};

test('uses x-request-id header', () => {
    const req = {
        headers: {
            'x-request-id': 'test'
        }
    };
    middleware(req, res, next);
    expect(req.requestId).toBe('test');
});

test('generates fake requestId', () => {
    const req = { headers: { } };
    middleware(req, res, next);
    const { requestId } = req;
    expect(requestId).toMatch(/^\d+Duffman\d+$/);
    expect(requestId).toHaveLength(32);
});
