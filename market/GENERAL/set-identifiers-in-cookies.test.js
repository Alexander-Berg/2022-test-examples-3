const setIdentifiersInCookies = require('./set-identifiers-in-cookies');
const { createResponse, createAffId, createClid } = require('./../../utils/create');

beforeEach(() => {
    jest.clearAllMocks();
});

describe('setIdentifiersInCookies', () => {
    test(`sets cookies when 'res', 'aff_id', and 'clid' exist`, () => {
        const res = createResponse();
        const affId = createAffId();
        const clid = createClid();

        setIdentifiersInCookies(res, affId, clid);

        expect(res.cookie).toHaveBeenCalledTimes(2);
        expect(res.cookie.mock.calls).toMatchInlineSnapshot(`
            Array [
              Array [
                "yandex.statistics.clid.21",
                "FAKE_CLID",
                Object {
                  "domain": "market.yandex.ru",
                  "httpOnly": true,
                  "maxAge": 1800000,
                  "path": "/",
                  "sameSite": "Lax",
                  "secure": true,
                },
              ],
              Array [
                "sovetnik.aff_id",
                "FAKE_AFF_ID",
                Object {
                  "domain": "market.yandex.ru",
                  "httpOnly": true,
                  "maxAge": 1800000,
                  "path": "/",
                  "sameSite": "Lax",
                  "secure": true,
                },
              ],
            ]
        `);
    });

    test(`sets cookies when 'res', 'aff_id', 'clid' exist, and 'timeKey' is 'day'`, () => {
        const res = createResponse();
        const affId = createAffId();
        const clid = createClid();
        const timeKey = 'day';

        setIdentifiersInCookies(res, affId, clid, timeKey);

        expect(res.cookie).toHaveBeenCalledTimes(2);
        expect(res.cookie.mock.calls).toMatchInlineSnapshot(`
            Array [
              Array [
                "yandex.statistics.clid.21",
                "FAKE_CLID",
                Object {
                  "domain": "market.yandex.ru",
                  "httpOnly": true,
                  "maxAge": 86400000,
                  "path": "/",
                  "sameSite": "Lax",
                  "secure": true,
                },
              ],
              Array [
                "sovetnik.aff_id",
                "FAKE_AFF_ID",
                Object {
                  "domain": "market.yandex.ru",
                  "httpOnly": true,
                  "maxAge": 86400000,
                  "path": "/",
                  "sameSite": "Lax",
                  "secure": true,
                },
              ],
            ]
        `);
    });

    test(`sets cookies when 'res', 'aff_id', 'clid' exist, and 'timeKey' is '3 days'`, () => {
        const res = createResponse();
        const affId = createAffId();
        const clid = createClid();
        const timeKey = '3 days';

        setIdentifiersInCookies(res, affId, clid, timeKey);

        expect(res.cookie).toHaveBeenCalledTimes(2);
        expect(res.cookie.mock.calls).toMatchInlineSnapshot(`
            Array [
              Array [
                "yandex.statistics.clid.21",
                "FAKE_CLID",
                Object {
                  "domain": "market.yandex.ru",
                  "httpOnly": true,
                  "maxAge": 259200000,
                  "path": "/",
                  "sameSite": "Lax",
                  "secure": true,
                },
              ],
              Array [
                "sovetnik.aff_id",
                "FAKE_AFF_ID",
                Object {
                  "domain": "market.yandex.ru",
                  "httpOnly": true,
                  "maxAge": 259200000,
                  "path": "/",
                  "sameSite": "Lax",
                  "secure": true,
                },
              ],
            ]
        `);
    });

    test(`sets cookies when 'res', 'aff_id', 'clid' exist, and 'timeKey' is '7 days'`, () => {
        const res = createResponse();
        const affId = createAffId();
        const clid = createClid();
        const timeKey = '7 days';

        setIdentifiersInCookies(res, affId, clid, timeKey);

        expect(res.cookie).toHaveBeenCalledTimes(2);
        expect(res.cookie.mock.calls).toMatchInlineSnapshot(`
            Array [
              Array [
                "yandex.statistics.clid.21",
                "FAKE_CLID",
                Object {
                  "domain": "market.yandex.ru",
                  "httpOnly": true,
                  "maxAge": 604800000,
                  "path": "/",
                  "sameSite": "Lax",
                  "secure": true,
                },
              ],
              Array [
                "sovetnik.aff_id",
                "FAKE_AFF_ID",
                Object {
                  "domain": "market.yandex.ru",
                  "httpOnly": true,
                  "maxAge": 604800000,
                  "path": "/",
                  "sameSite": "Lax",
                  "secure": true,
                },
              ],
            ]
        `);
    });

    test(`sets cookies when 'res', 'aff_id', 'clid' exist, and 'timeKey' is '30 days'`, () => {
        const res = createResponse();
        const affId = createAffId();
        const clid = createClid();
        const timeKey = '30 days';

        setIdentifiersInCookies(res, affId, clid, timeKey);

        expect(res.cookie).toHaveBeenCalledTimes(2);
        expect(res.cookie.mock.calls).toMatchInlineSnapshot(`
Array [
  Array [
    "yandex.statistics.clid.21",
    "FAKE_CLID",
    Object {
      "domain": "market.yandex.ru",
      "httpOnly": true,
      "maxAge": 2592000000,
      "path": "/",
      "sameSite": "Lax",
      "secure": true,
    },
  ],
  Array [
    "sovetnik.aff_id",
    "FAKE_AFF_ID",
    Object {
      "domain": "market.yandex.ru",
      "httpOnly": true,
      "maxAge": 2592000000,
      "path": "/",
      "sameSite": "Lax",
      "secure": true,
    },
  ],
]
`);
    });

    test(`sets no cookie when 'aff_id' is not defined`, () => {
        const res = createResponse();
        const affId = createAffId(null);
        const clid = createClid();

        setIdentifiersInCookies(res, affId, clid);

        expect(res.cookie).not.toHaveBeenCalled();
    });

    test(`sets no cookie when 'clid' is not defined`, () => {
        const res = createResponse();
        const affId = createAffId();
        const clid = createClid(null);

        setIdentifiersInCookies(res, affId, clid);

        expect(res.cookie).not.toHaveBeenCalled();
    });

    test(`sets no cookie when 'timeKey' is unknown`, () => {
        const res = createResponse();
        const affId = createAffId();
        const clid = createClid(null);

        setIdentifiersInCookies(res, affId, clid, 'FAKE_TIME_KEY');

        expect(res.cookie).not.toHaveBeenCalled();
    });
});
