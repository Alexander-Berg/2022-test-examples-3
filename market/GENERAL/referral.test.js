const referralMiddleware = require('./referral');
const { createRequest, createResponse, createNext, createClid } = require('./../utils/create');

const FAKE_USER_AGENT =
    'Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 Mobile/15E148 Safari/604.1';

const FAKE_CLID = 12345;

describe('referralMiddleware', () => {
    test(`returns status 302 and sets location header when 'target' is 'referral' and 'clid' exist`, () => {
        const req = createRequest({
            headers: {
                'user-agent': FAKE_USER_AGENT,
            },
            extractedData: {
                target: 'referral',
                clid: createClid(FAKE_CLID),
            },
        });
        const res = createResponse({
            setHeader: jest.fn().mockName('setHeader'),
            end: jest.fn(),
        });
        const next = createNext();

        referralMiddleware(req, res, next);

        expect(next).not.toHaveBeenCalled();
        expect(res.end).toHaveBeenCalled();
        expect(res.end).toHaveBeenCalledTimes(1);
        expect(res.statusCode).toBe(302);
        expect(res.setHeader).toHaveBeenCalledTimes(1);
        expect(res.setHeader).toMatchInlineSnapshot(`
            [MockFunction setHeader] {
              "calls": Array [
                Array [
                  "Location",
                  "https://sovetnik.yandex.ru?aff_id=1550&clid=12345",
                ],
              ],
              "results": Array [
                Object {
                  "type": "return",
                  "value": undefined,
                },
              ],
            }
        `);
    });

    test('clid should be a number', () => {
        const req = createRequest({
            headers: {
                'user-agent': FAKE_USER_AGENT,
            },
            extractedData: {
                target: 'referral',
                clid: createClid(),
            },
        });
        const res = createResponse({
            setHeader: jest.fn().mockName('setHeader'),
            end: jest.fn(),
        });
        const next = createNext();

        referralMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
        expect(res.setHeader).not.toHaveBeenCalled();
        expect(res.end).not.toHaveBeenCalled();
    });

    test('clid should be a finite number', () => {
        const req = createRequest({
            headers: {
                'user-agent': FAKE_USER_AGENT,
            },
            extractedData: {
                target: 'referral',
                clid: createClid(Infinity),
            },
        });
        const res = createResponse({
            setHeader: jest.fn().mockName('setHeader'),
            end: jest.fn(),
        });
        const next = createNext();

        referralMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });

    test(`returns nothing if 'target' does not exist`, () => {
        const req = createRequest({
            headers: {
                'user-agent': FAKE_USER_AGENT,
            },
            extractedData: {
                clid: createClid(FAKE_CLID),
            },
        });
        const res = createResponse({
            setHeader: jest.fn().mockName('setHeader'),
            end: jest.fn(),
        });
        const next = createNext();

        referralMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
        expect(res.setHeader).not.toHaveBeenCalled();
        expect(res.end).not.toHaveBeenCalled();
    });

    test(`returns nothing if 'clid' does not exist`, () => {
        const req = createRequest({
            headers: {
                'user-agent': FAKE_USER_AGENT,
            },
            extractedData: {
                target: 'referral',
            },
        });
        const res = createResponse({
            setHeader: jest.fn().mockName('setHeader'),
            end: jest.fn(),
        });
        const next = createNext();

        referralMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
        expect(res.setHeader).not.toHaveBeenCalled();
        expect(res.end).not.toHaveBeenCalled();
    });
});
