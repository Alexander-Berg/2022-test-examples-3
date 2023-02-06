const promoMiddleware = require('./promo');
const {
    createRequest,
    createResponse,
    createNext,
    createAffId,
    createClid,
} = require('./../utils/create');

const FAKE_URL = 'FAKE_URL';
const FAKE_HOSTNAME_CHROME = 'chrome.google.com';
const FAKE_HOSTNAME_MOZILLA = 'addons.mozilla.org';

const promo = Object.freeze({
    INSTALL: 'promo_install',
    DETAILS: 'promo_details',
    BODY: 'promo_body',
});

describe('Promo middleware', () => {
    test(`sets status 302 and 'Location' header if promoTarget is ${promo.INSTALL} from Chrome store`, () => {
        const req = createRequest({
            extractedData: {
                url: FAKE_URL,
                hostname: FAKE_HOSTNAME_CHROME,
                target: promo.INSTALL,
                clid: createClid(),
                affId: createAffId(),
            },
        });

        const res = createResponse({
            setHeader: jest.fn().mockName('setHeader'),
            end: jest.fn(),
        });

        const next = createNext();

        promoMiddleware(req, res, next);

        expect(next).not.toHaveBeenCalled();
        expect(res.setHeader).toHaveBeenCalledTimes(1);
        expect(res.setHeader).toMatchInlineSnapshot(`
            [MockFunction setHeader] {
              "calls": Array [
                Array [
                  "Location",
                  "FAKE_URL",
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
        expect(res.end).toHaveBeenCalled();
        expect(res.end).toHaveBeenCalledTimes(1);
        expect(res.statusCode).toBe(302);
    });

    test(`sets status 302 and 'Location' header if promoTarget is ${promo.INSTALL} from Mozilla store`, () => {
        const req = createRequest({
            extractedData: {
                url: FAKE_URL,
                hostname: FAKE_HOSTNAME_MOZILLA,
                target: promo.INSTALL,
                clid: createClid(),
                affId: createAffId(),
            },
        });

        const res = createResponse({
            setHeader: jest.fn().mockName('setHeader'),
            end: jest.fn(),
        });

        const next = createNext();

        promoMiddleware(req, res, next);

        expect(next).not.toHaveBeenCalled();
        expect(res.setHeader).toHaveBeenCalledTimes(1);
        expect(res.setHeader).toMatchInlineSnapshot(`
          [MockFunction setHeader] {
            "calls": Array [
              Array [
                "Location",
                "FAKE_URL",
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
        expect(res.end).toHaveBeenCalled();
        expect(res.end).toHaveBeenCalledTimes(1);
        expect(res.statusCode).toBe(302);
    });

    test(`sets status 302 and 'Location' header if promoTarget is ${promo.DETAILS} from Chrome store`, () => {
        const req = createRequest({
            extractedData: {
                url: FAKE_URL,
                hostname: FAKE_HOSTNAME_CHROME,
                target: promo.DETAILS,
                clid: createClid(),
                affId: createAffId(),
            },
        });

        const res = createResponse({
            setHeader: jest.fn().mockName('setHeader'),
            end: jest.fn(),
        });

        const next = createNext();

        promoMiddleware(req, res, next);

        expect(next).not.toHaveBeenCalled();
        expect(res.setHeader).toHaveBeenCalledTimes(1);
        expect(res.setHeader).toMatchInlineSnapshot(`
        [MockFunction setHeader] {
          "calls": Array [
            Array [
              "Location",
              "FAKE_URL",
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
        expect(res.end).toHaveBeenCalled();
        expect(res.end).toHaveBeenCalledTimes(1);
        expect(res.statusCode).toBe(302);
    });

    test(`sets status 302 and 'Location' header if promoTarget is ${promo.BODY} from Mozilla store`, () => {
        const req = createRequest({
            extractedData: {
                url: FAKE_URL,
                hostname: FAKE_HOSTNAME_MOZILLA,
                target: promo.BODY,
                clid: createClid(),
                affId: createAffId(),
            },
        });

        const res = createResponse({
            setHeader: jest.fn().mockName('setHeader'),
            end: jest.fn(),
        });

        const next = createNext();

        promoMiddleware(req, res, next);

        expect(next).not.toHaveBeenCalled();
        expect(res.setHeader).toHaveBeenCalledTimes(1);
        expect(res.setHeader).toMatchInlineSnapshot(`
      [MockFunction setHeader] {
        "calls": Array [
          Array [
            "Location",
            "FAKE_URL",
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
        expect(res.end).toHaveBeenCalled();
        expect(res.end).toHaveBeenCalledTimes(1);
        expect(res.statusCode).toBe(302);
    });

    test(`sets status 302 and 'Location' header if promoTarget is ${promo.DETAILS} from Chrome store`, () => {
        const req = createRequest({
            extractedData: {
                url: FAKE_URL,
                hostname: FAKE_HOSTNAME_CHROME,
                target: promo.DETAILS,
                clid: createClid(),
                affId: createAffId(),
            },
        });

        const res = createResponse({
            setHeader: jest.fn().mockName('setHeader'),
            end: jest.fn(),
        });

        const next = createNext();

        promoMiddleware(req, res, next);

        expect(next).not.toHaveBeenCalled();
        expect(res.setHeader).toHaveBeenCalledTimes(1);
        expect(res.setHeader).toMatchInlineSnapshot(`
      [MockFunction setHeader] {
        "calls": Array [
          Array [
            "Location",
            "FAKE_URL",
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
        expect(res.end).toHaveBeenCalled();
        expect(res.end).toHaveBeenCalledTimes(1);
        expect(res.statusCode).toBe(302);
    });

    test(`returns nothing when promo target is unknown`, () => {
        const req = createRequest({
            extractedData: {
                url: FAKE_URL,
                hostname: FAKE_HOSTNAME_MOZILLA,
                target: 'FAKE_TARGET',
                clid: createClid(),
                affId: createAffId(),
            },
        });

        const res = createResponse({
            setHeader: jest.fn().mockName('setHeader'),
            end: jest.fn(),
        });

        const next = createNext();

        promoMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
        expect(res.setHeader).not.toHaveBeenCalled();
        expect(res.end).not.toHaveBeenCalled();
    });

    test(`returns nothing when web store is unknown`, () => {
        const req = createRequest({
            extractedData: {
                url: FAKE_URL,
                hostname: 'FAKE_WEB_STORE',
                target: promo.INSTALL,
                clid: createClid(),
                affId: createAffId(),
            },
        });

        const res = createResponse({
            setHeader: jest.fn().mockName('setHeader'),
            end: jest.fn(),
        });

        const next = createNext();

        promoMiddleware(req, res, next);

        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
        expect(res.setHeader).not.toHaveBeenCalled();
        expect(res.end).not.toHaveBeenCalled();
    });
});
