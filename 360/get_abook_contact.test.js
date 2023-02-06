'use strict';

const method = require('./get_abook_contact.js');

const vcard = {
    names: [ { first: 'John' } ],
    emails: [],
    instant_messengers: [],
    social_profiles: [],
    telephone_numbers: [],
    notes: [ 'notes' ],
    organizations: [],
    description: '',
    any: 'other',
    events: [ { as: 'is' } ]
};

let core;
let mockRequest;

beforeEach(() => {
    mockRequest = jest.fn();

    core = {
        config: {},
        params: {},
        request: mockRequest
    };
});

test('отвечаем 400, если нет параметров', async () => {
    expect.assertions(2);

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(400);
        expect(err.message).toInclude('invalid params schema');
    }
});

test('happy path', async () => {
    core.params.uuid = 'deadbeef42';
    core.params.email = 'login@example.com';
    mockRequest.mockResolvedValueOnce({ id: 1, list_id: 2, vcard });

    const res = await method(core);

    expect(mockRequest.mock.calls).toMatchSnapshot();
    expect(res).toMatchSnapshot();
});

test('фильтрация для шареных контактов', async () => {
    core.params.uuid = 'deadbeef42';
    core.params.email = 'login@example.com';
    const vcard = {
        names: [ { first: 'John' } ],
        emails: [ { email: 'login@example.com' } ],
        instant_messengers: [],
        social_profiles: [],
        telephone_numbers: [],
        notes: [ 'notes' ],
        organizations: [],
        description: '',
        any: 'other',
        events: [ { as: 'is' } ]
    };
    mockRequest.mockResolvedValueOnce({ id: 1, list_id: 2, shared: true, vcard });

    const res = await method(core);

    expect(mockRequest.mock.calls).toMatchSnapshot();
    expect(res).toMatchSnapshot();
});

test('happy path for internal login', async () => {
    core.config.IS_CORP = true;
    core.params.uuid = 'deadbeef42';
    core.params.email = 'login@example.com';
    mockRequest.mockResolvedValueOnce('internal');
    mockRequest.mockResolvedValueOnce({ id: 1, list_id: 2, shared: true, vcard });

    const res = await method(core);

    expect(mockRequest.mock.calls).toMatchSnapshot();
    expect(res).toMatchSnapshot();
});

test('happy path for external login', async () => {
    core.config.IS_CORP = true;
    core.params.uuid = 'deadbeef42';
    core.params.email = 'login@example.com';
    mockRequest.mockResolvedValueOnce({ affiliation: 'external' });
    mockRequest.mockResolvedValueOnce({ id: 1, list_id: 2, shared: true, vcard });

    const res = await method(core);

    expect(mockRequest.mock.calls).toMatchSnapshot();
    expect(res).toMatchSnapshot();
});

test('happy path for pdd', async () => {
    core.params.uuid = 'deadbeef42';
    core.params.email = 'login@example.com';
    mockRequest.mockResolvedValueOnce({ id: 1, list_id: 2, shared: true, vcard });

    const res = await method(core);

    expect(mockRequest.mock.calls).toMatchSnapshot();
    expect(res).toMatchSnapshot();
});

test('sad path', async () => {
    expect.assertions(1);

    core.params.uuid = 'deadbeef42';
    core.params.email = 'login@example.com';
    mockRequest.mockRejectedValue({});

    try {
        await method(core);
    } catch (err) {
        expect(err.code).toBe(400);
    }
});
