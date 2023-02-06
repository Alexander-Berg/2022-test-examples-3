const responseMiddleware = require('./response-middleware');
const { createRequest, createResponse, createNext } = require('./../utils/create');

jest.mock('./../utils/crypto');
const { encrypt } = require('./../utils/crypto');

beforeEach(() => {
    jest.clearAllMocks();
});

describe('responseMiddleware', () => {
    test('throws an error if request from unknown host', () => {
        const FAKE_URL = 'FAKE_URL';
        const req = createRequest({
            extractedData: {
                url: FAKE_URL,
                hostname: 'FAKE_HOSTNAME',
            },
        });
        const res = createResponse({
            end: jest.fn().mockName('end'),
        });
        const next = createNext();

        expect(() => {
            responseMiddleware(req, res, next);
        }).toThrowError(`${FAKE_URL} is unknown url`);
        expect(res.end).toHaveBeenCalled();
        expect(res.end).toHaveBeenCalledTimes(1);
    });

    test(`sets 'svt-s' cookie if request came from yandex`, () => {
        const req = createRequest({
            query: {},
            extractedData: {
                url: 'FAKE_URL',
                hostname: 'yandex.ru',
            },
        });
        const res = createResponse({
            end: jest.fn().mockName('end'),
            status: jest.fn().mockName('status'),
            location: jest.fn().mockName('location'),
        });
        const next = createNext();
        encrypt.mockImplementation(() => 'fake_cipher_string');

        responseMiddleware(req, res, next);

        expect(res.end).toHaveBeenCalled();
        expect(res.end).toHaveBeenCalledTimes(1);
        expect(res.cookie).toHaveBeenCalled();
        expect(res.cookie).toHaveBeenCalledTimes(1);
        expect(res.cookie.mock.calls[0]).toMatchInlineSnapshot(`
            Array [
              "svt-s",
              "fake_cipher_string",
              Object {
                "httpOnly": true,
                "maxAge": 315360000000,
                "secure": true,
              },
            ]
        `);
    });

    test(`sets 'svt-s' cookie if request came from auto.ru`, () => {
        const req = createRequest({
            query: {},
            extractedData: {
                url: 'FAKE_URL',
                hostname: 'auto.ru',
            },
        });
        const res = createResponse({
            end: jest.fn().mockName('end'),
            status: jest.fn().mockName('status'),
            location: jest.fn().mockName('location'),
        });
        const next = createNext();
        encrypt.mockImplementation(() => 'fake_cipher_string');

        responseMiddleware(req, res, next);

        expect(res.end).toHaveBeenCalled();
        expect(res.end).toHaveBeenCalledTimes(1);
        expect(res.cookie).toHaveBeenCalled();
        expect(res.cookie).toHaveBeenCalledTimes(1);
        expect(res.cookie.mock.calls[0]).toMatchInlineSnapshot(`
            Array [
              "svt-s",
              "fake_cipher_string",
              Object {
                "httpOnly": true,
                "maxAge": 315360000000,
                "secure": true,
              },
            ]
        `);
    });

    test(`updates 'stats' object if 'query' type is 'market' and request is not from a button`, () => {
        const req = createRequest({
            stats: {
                bar: {
                    clicks: 7,
                    shows: 10,
                    closes: 0,
                },
            },
            query: {
                type: 'market',
                from_button: false,
            },
            extractedData: {
                url: 'FAKE_URL',
                hostname: 'auto.ru',
            },
        });
        const res = createResponse({
            end: jest.fn().mockName('end'),
            status: jest.fn().mockName('status'),
            location: jest.fn().mockName('location'),
        });
        const next = createNext();
        encrypt.mockImplementation(() => 'fake_cipher_string');

        responseMiddleware(req, res, next);

        expect(res.end).toHaveBeenCalled();
        expect(res.end).toHaveBeenCalledTimes(1);
        expect(res.cookie).toHaveBeenCalled();
        expect(res.cookie).toHaveBeenCalledTimes(1);
        expect(res.cookie.mock.calls[0]).toMatchInlineSnapshot(`
Array [
  "svt-s",
  "fake_cipher_string",
  Object {
    "httpOnly": true,
    "maxAge": 315360000000,
    "secure": true,
  },
]
`);
        expect(req.stats).toMatchInlineSnapshot(`
Object {
  "bar": Object {
    "clicks": 8,
    "closes": 0,
    "shows": 10,
  },
}
`);
    });

    test(`does not update 'stats' object if 'query' type is 'market' and request is from a button`, () => {
        const fakeStats = {
            bar: {
                clicks: 7,
                shows: 10,
                closes: 0,
            },
        };

        const req = createRequest({
            stats: fakeStats,
            query: {
                type: 'market',
                from_button: true,
            },
            extractedData: {
                url: 'FAKE_URL',
                hostname: 'yandex.ru',
            },
        });
        const res = createResponse({
            end: jest.fn().mockName('end'),
            status: jest.fn().mockName('status'),
            location: jest.fn().mockName('location'),
        });
        const next = createNext();
        encrypt.mockImplementation(() => 'fake_cipher_string');

        responseMiddleware(req, res, next);

        expect(res.end).toHaveBeenCalled();
        expect(res.end).toHaveBeenCalledTimes(1);
        expect(res.cookie).toHaveBeenCalled();
        expect(res.cookie).toHaveBeenCalledTimes(1);
        expect(req.stats).toEqual(fakeStats);
        expect(res.cookie.mock.calls[0]).toMatchInlineSnapshot(`
Array [
  "svt-s",
  "fake_cipher_string",
  Object {
    "httpOnly": true,
    "maxAge": 315360000000,
    "secure": true,
  },
]
`);
    });
});
