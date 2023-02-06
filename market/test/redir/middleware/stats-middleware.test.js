const statsMiddleware = require('./stats-middleware');
const { createRequest, createResponse, createNext } = require('./../utils/create');

jest.mock('./../utils/crypto');
const { decrypt } = require('./../utils/crypto');

decrypt.mockImplementation((cookie) => cookie);

beforeEach(() => {
    jest.clearAllMocks();
});

describe('statsMiddleware', () => {
    test(`adds 'stats' object to 'req'`, () => {
        const req = createRequest({
            cookies: {
                'svt-s': JSON.stringify({
                    bar: {
                        clicks: 5,
                        shows: 5,
                        closes: 0,
                    },
                }),
            },
        });
        const res = createResponse();
        const next = createNext();

        statsMiddleware(req, res, next);

        expect(req.stats).toMatchInlineSnapshot(`
            Object {
              "bar": Object {
                "clicks": 5,
                "closes": 0,
                "shows": 5,
              },
            }
        `);
        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });

    test(`adds default 'stats' object to 'req' if we can't parse 'svt-s' cookie`, () => {
        const req = createRequest({
            cookies: {
                'svt-s': '',
            },
        });
        const res = createResponse();
        const next = createNext();

        statsMiddleware(req, res, next);

        expect(req.stats).toMatchInlineSnapshot(`
Object {
  "bar": Object {
    "clicks": 0,
    "closes": 0,
    "shows": 0,
  },
}
`);
        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });
});
