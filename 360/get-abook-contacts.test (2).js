'use strict';

jest.mock('../../../lib/method/base-method.js');

const mockSuper = require('../../../lib/method/base-method.js').prototype;
const GetContacts = require('./get-abook-contacts.js');

let getContacts;
let contacts;

beforeEach(() => {
    getContacts = new GetContacts();
    contacts = [];
    mockSuper.calls.exec.mockResolvedValueOnce({ contacts });
});

test('get contacts from collie', async () => {
    contacts.push({ contact_id: 42 });
    const params = {
        limit: 10,
        offset: 0,
        sort: 'alpha'
    };

    const result = await getContacts.fetch({}, params);

    expect(mockSuper.calls.exec.mock.calls).toMatchSnapshot();
    expect(result).toMatchSnapshot();
});

test('get contacts from aceventura (last_usage)', async () => {
    contacts.push({ contact_id: 42 });
    const params = {
        limit: 10,
        offset: 0,
        sort: 'last_usage'
    };

    const result = await getContacts.fetch({}, params);

    expect(mockSuper.calls.exec.mock.calls).toMatchSnapshot();
    expect(result).toMatchSnapshot();
});

test('get contacts from aceventura (usage_frequency)', async () => {
    contacts.push({ contact_id: 42 });
    const params = {
        limit: 10,
        offset: 0,
        sort: 'usage_frequency'
    };

    const result = await getContacts.fetch({}, params);

    expect(mockSuper.calls.exec.mock.calls).toMatchSnapshot();
    expect(result).toMatchSnapshot();
});

test('get contacts from aceventura (search)', async () => {
    contacts.push({ contact_id: 42 });
    const params = {
        limit: 10,
        offset: 0,
        query: 'search'
    };

    const result = await getContacts.fetch({}, params);

    expect(mockSuper.calls.exec.mock.calls).toMatchSnapshot();
    expect(result).toMatchSnapshot();
});
