const disableMiddleware = require('./disable');

function createRequest(overrides = {}) {
    const req = { body: {}, cookies: {}, ...overrides };
    return req;
}

function createResponse(overrides = {}) {
    const res = {
        clearCookie: jest.fn().mockName('clearCookie'),
        ...overrides,
    };
    return res;
}

function createNext() {
    return jest.fn().mockName('next');
}

describe('disable middleware', () => {
    test(`removes 'svt-disabled' and 'svt-times_of_closings' cookies if 'req' contains them`, () => {
        const req = createResponse({
            cookies: {
                'svt-disabled': 'COOKIE_VALUE_1',
                'svt-times_of_closings': 'COOKIE_VALUE_2',
            },
        });
        const res = createResponse();
        const next = createNext();

        disableMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
        expect(res.clearCookie).toHaveBeenCalledTimes(2);
        expect(res.clearCookie).toMatchInlineSnapshot(`
            [MockFunction clearCookie] {
              "calls": Array [
                Array [
                  "svt-disabled",
                  Object {
                    "path": "/",
                  },
                ],
                Array [
                  "svt-times_of_closings",
                  Object {
                    "path": "/",
                  },
                ],
              ],
              "results": Array [
                Object {
                  "type": "return",
                  "value": undefined,
                },
                Object {
                  "type": "return",
                  "value": undefined,
                },
              ],
            }
        `);
    });

    test(`does not remove 'svt-disabled' and 'svt-times_of_closings' cookies if 'req' does not contain them`, () => {
        const req = createRequest();
        const res = createResponse();
        const next = createNext();

        disableMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
        expect(res.clearCookie).not.toHaveBeenCalled();
    });
});
