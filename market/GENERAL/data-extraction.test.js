const dataExtractionMiddleware = require('./data-extraction');
const {
    createRequest,
    createResponse,
    createNext,
    createAffId,
    createClid,
} = require('./../utils/create');

describe('Data extraction middleware', () => {
    test(`throws an error when 'req.query.url' has unknown path`, () => {
        const FAKE_URL = 'FAKE_URL';
        const req = createRequest({
            query: {
                url: FAKE_URL,
                hostname: 'FAKE_HOSTNAME',
                target: 'FAKE_TARGET',
                clid: createClid(),
                aff_id: createAffId(),
            },
        });

        const res = createResponse({
            end: jest.fn(),
        });

        const next = createNext();

        expect(() => {
            dataExtractionMiddleware(req, res, next);
        }).toThrowError(`${FAKE_URL} has unknown path`);
        expect(res.end).toHaveBeenCalled();
        expect(next).not.toHaveBeenCalled();
    });

    test(`adds 'extractedData' object to 'req`, () => {
        const req = createRequest({
            query: {
                url: 'https://example.com/fakepath',
                hostname: 'FAKE_HOSTNAME',
                target: 'FAKE_TARGET',
                clid: createClid(),
                aff_id: createAffId(),
            },
        });

        const res = createResponse({
            end: jest.fn(),
        });

        const next = createNext();

        dataExtractionMiddleware(req, res, next);

        expect(res.end).not.toHaveBeenCalled();
        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
        expect(req.extractedData).toMatchInlineSnapshot(`
            Object {
              "affId": "FAKE_AFF_ID",
              "clid": "FAKE_CLID",
              "hostname": "example.com",
              "parsedUrl": Url {
                "auth": null,
                "hash": null,
                "host": "example.com",
                "hostname": "example.com",
                "href": "https://example.com/fakepath",
                "path": "/fakepath",
                "pathname": "/fakepath",
                "port": null,
                "protocol": "https:",
                "query": Object {},
                "search": "",
                "slashes": true,
              },
              "target": "FAKE_TARGET",
              "url": "https://example.com/fakepath",
            }
        `);
    });

    test(`does not call 'res.end' when we can not properly parse url from query`, () => {
        const req = createRequest({
            query: {
                hostname: 'FAKE_HOSTNAME',
                target: 'FAKE_TARGET',
                clid: createClid(),
                aff_id: createAffId(),
            },
        });

        const res = createResponse({
            end: jest.fn(),
        });

        const next = createNext();

        dataExtractionMiddleware(req, res, next);

        expect(res.end).not.toHaveBeenCalled();
        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
        expect(req.extractedData).toMatchInlineSnapshot(`
Object {
  "affId": "FAKE_AFF_ID",
  "clid": "FAKE_CLID",
  "hostname": undefined,
  "parsedUrl": undefined,
  "target": "FAKE_TARGET",
  "url": undefined,
}
`);
    });
});
