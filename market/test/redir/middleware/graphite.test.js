const graphiteMiddleware = require('./graphite');
const statsd = require('./../utils/statsd');
const { createRequest, createResponse, createNext, createClid } = require('./../utils/create');

jest.mock('./../utils/statsd');

beforeEach(() => {
    jest.clearAllMocks();
});

describe('graphiteMiddleware', () => {
    test(`invokes 'statsd.increment' method`, () => {
        const req = createRequest({
            query: {
                target: 'FAKE_TARGET',
                clid: createClid(),
            },
        });
        const res = createResponse();
        const next = createNext();

        graphiteMiddleware(req, res, next);

        expect(statsd.increment.mock.calls[0]).toMatchInlineSnapshot(`
            Array [
              "suggest_script.FAKE_CLID.sitebar.pricebar.click.FAKE_TARGET",
              1,
            ]
        `);
        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });

    test(`returns nothing if 'target' does not exist`, () => {
        const req = createRequest({
            query: {
                clid: createClid(),
            },
        });
        const res = createResponse();
        const next = createNext();

        graphiteMiddleware(req, res, next);

        expect(statsd.increment).not.toHaveBeenCalled();
        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });

    test(`returns nothing if 'clid' does not exist`, () => {
        const req = createRequest({
            query: {
                target: 'FAKE_TARGET',
            },
        });
        const res = createResponse();
        const next = createNext();

        graphiteMiddleware(req, res, next);

        expect(statsd.increment).not.toHaveBeenCalled();
        expect(next).toHaveBeenCalled();
        expect(next).toHaveBeenCalledTimes(1);
    });
});
