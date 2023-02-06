const trackReferralMiddleware = require('./track-referral-data');
const { createRequest, createResponse, createNext } = require('./../utils/create');

jest.mock('./../utils/crypto');
const { encrypt } = require('./../utils/crypto');

beforeEach(() => {
    jest.clearAllMocks();
});

describe('trackReferralMiddleware', () => {
    test(`throws an error if 'req' or 'req.query' is missing`, () => {
        const req = createRequest();
        const res = createResponse();
        const next = createNext();

        expect(() => {
            trackReferralMiddleware(req, res, next);
        }).toThrowError('req or req.query is missing');
        expect(res.cookie).not.toHaveBeenCalled();
        expect(next).not.toHaveBeenCalled();
    });

    test(`does nothing when 'req.query' does not contain 'transaction_id' or 'offer_direct_domain'`, () => {
        const req = createRequest({
            query: {},
        });
        const res = createResponse();
        const next = createNext();

        trackReferralMiddleware(req, res, next);

        expect(res.cookie).not.toHaveBeenCalled();
        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });

    test(`sets cookie when 'transaction_id' and 'offer_direct_domain' exist`, () => {
        const req = createRequest({
            query: {
                transaction_id: 'FAKE_TRANSCATION_ID',
                offer_direct_domain: 'FAKE_OFFER_DIRECT_DOMAIN',
            },
        });
        const res = createResponse({
            cookie: jest.fn().mockName('cookie'),
        });
        const next = createNext();

        encrypt.mockImplementation(() => 'fake_cipher_string');

        trackReferralMiddleware(req, res, next);

        expect(res.cookie).toHaveBeenCalled();
        expect(res.cookie).toHaveBeenCalledTimes(1);
        expect(res.cookie.mock.calls).toMatchInlineSnapshot(`
            Array [
              Array [
                "fake_cipher_string",
                "FAKE_TRANSCATION_ID",
                Object {
                  "httpOnly": true,
                  "maxAge": 3600000,
                  "secure": true,
                },
              ],
            ]
        `);
        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });
});
