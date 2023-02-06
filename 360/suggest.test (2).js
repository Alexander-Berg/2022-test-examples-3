'use strict';

const createEntity = require('./../../build/fetchers/suggest/suggest').default;
const ApiError = require('../../../../routes/helpers/api-error');
const suggestMock = require('../../../../test/mock/search-suggest.json');

const Ajv = require('ajv');
const ajv = new Ajv();
async function validateSchema(searchSuggestSchema, schemaToCheck) {
    try {
        return await ajv.validate(searchSuggestSchema, schemaToCheck);
    } catch (error) {
        throw new Error(`SCHEMA INVALID ${error.errors.map((e) => e.message)}`);
    }
}

let core;
let mockService;

beforeEach(() => {
    mockService = jest.fn();
    core = {
        params: {
            uuid: 'deadbeef42',
            client: 'iphone',
            client_version: '10.0.3'
        },
        res: {
            set: jest.fn()
        },
        service: () => mockService
    };
});

test('adds side=mobile to parameters of abook-suggest', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({}, {}).then(() => {
        expect(mockService.mock.calls[0][1]).toContainEntry([ 'side', 'mobile' ]);
    });
});

test('adds highlight=1 to parameters of abook-suggest', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({}, {}).then(() => {
        expect(mockService.mock.calls[0][1]).toContainEntry([ 'highlight', 1 ]);
    });
});

test('doesn\'t pass fid if undefined', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({}, {}).then(() => {
        expect(Object.keys(mockService.mock.calls[0][1])).not.toContain('fid');
    });
});

test('adds fid if specified', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({ fid: 42 }, {}).then(() => {
        expect(mockService.mock.calls[0][1].fid).toEqual(42);
    });
});

test('passes uuid as reqid if undefined', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({}, {}).then(() => {
        expect(mockService.mock.calls[0][1].reqid).toEqual('deadbeef42');
    });
});

test('passes reqid if specified', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({}, { reqid: '424242' }).then(() => {
        expect(mockService.mock.calls[0][1].reqid).toEqual('424242');
    });
});

test('limit is 10 by default if not passed', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({}, {}).then(() => {
        expect(mockService.mock.calls[0][1]).toContainEntry([ 'limit', 10 ]);
    });
});

test('uses limit from params if specified', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({}, { limit: 42 }).then(() => {
        expect(mockService.mock.calls[0][1]).toContainEntry([ 'limit', 42 ]);
    });
});

test('uses text from params if specified', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({ text: '42' }, {}).then(() => {
        expect(mockService.mock.calls[0][1]).toContainEntry([ 'request', '42' ]);
    });
});

test('sets types = subject,contact if text is passed in params', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({ text: '42' }, {}).then(() => {
        expect(mockService.mock.calls[0][1]).toContainEntry([ 'types', 'subject,contact' ]);
    });
});

test('sets types = history if text is not passed', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({}, {}).then(() => {
        expect(mockService.mock.calls[0][1]).toContainEntry([ 'types', 'history' ]);
    });
});

test('adds status to parameters if it\'s passed', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({}, { status: 1234 }).then(() => {
        expect(mockService.mock.calls[0][1]).toContainEntry([ 'status', 1234 ]);
    });
});

test('responses with Error Code 500 if the service dies', () => {
    mockService.mockRejectedValueOnce({ message: 'foo' });
    return createEntity(core).get({}, {}).then(
        () => Promise.reject('MUST REJECT'),
        (err) => {
            expect(err).toBeInstanceOf(ApiError);
            expect(err.code).toEqual(500);
            expect(err.message).toEqual('foo');
        }
    );
});

describe('happy path', () => {
    beforeEach(() => {
        mockService.mockResolvedValueOnce({ body: suggestMock });
    });

    it('calls api/async/mail/suggest', () => {
        return createEntity(core).get({}, {}).then(() => {
            expect(mockService.mock.calls[0][0]).toEqual('/api/async/mail/suggest');
        });
    });

    it('responses with some number of items from the backend', () => {
        return createEntity(core).get({}, {}).then((res) => {
            expect(res[0].items).toHaveLength(7);
        });
    });

    it('responses with a response satisfying _helpers/schema.json', () => {
        const schema = Object.assign({ $async: true }, require('./../_test_helpers/suggest-schema.json'));
        return createEntity(core).get({}, {})
            .then((result) => validateSchema(schema, result[0].items))
            .then((result) => expect(result).toMatchSnapshot());
    });
});

test('sends twoSteps = 0 to the service by default', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({}, {}).then(() => {
        const opts = mockService.mock.calls[0][1];
        expect(opts.twoSteps).toEqual(0);
    });
});

test('doesn\'t pass timeout by default', () => {
    mockService.mockResolvedValueOnce({ body: [] });
    return createEntity(core).get({}, { timeout: 42 }).then(() => {
        const opts = mockService.mock.calls[0][1];
        expect(opts.twoSteps).toEqual(0);
        expect(opts.timeout).toBeUndefined();
    });
});

describe('twoSteps mode', () => {
    beforeEach(() => {
        mockService.mockResolvedValueOnce({ headers: {}, body: [] });
    });

    it('adds twoSteps to the service request', () => {
        return createEntity(core).get({}, { twoSteps: 1 }).then(() => {
            const opts = mockService.mock.calls[0][1];
            expect(opts.twoSteps).toEqual(1);
        });
    });

    it('returns with the status, if the one was received from the service', () => {
        mockService.mockReset()
            .mockResolvedValueOnce({ headers: { status: '1234' }, body: [] });
        return createEntity(core).get({}, { twoSteps: 1 }).then(() => {
            expect(core.res.set).toHaveBeenCalledWith('msearch-status', '1234');
        });
    });

    it('passes timeout to the service', () => {
        return createEntity(core).get({}, { twoSteps: 1, timeout: 42 }).then(() => {
            const opts = mockService.mock.calls[0][1];
            expect(opts.timeout).toEqual(42);
        });
    });
});
