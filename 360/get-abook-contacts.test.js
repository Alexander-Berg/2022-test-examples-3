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

test('get contact by contactId', async () => {
    contacts.push({ contact_id: 42 });
    const params = { contactId: '42' };

    const result = await getContacts.fetch(1, params);
    expect(result).toEqual({
        contacts: [ { contact_id: 42 } ],
        pager: { total: 1 }
    });
});

test('get contact by contactId (no contact)', async () => {
    const params = { contactId: '42' };

    const result = await getContacts.fetch(1, params);
    expect(result).toEqual({
        contacts: [ ],
        pager: { total: 0 }
    });
});

test('calculate pager if result is less than limit', async () => {
    contacts.push(1, 2, 3, 4);
    const params = { limit: 5, offset: 10 };

    const result = await getContacts.fetch(1, params);
    expect(result).toEqual({
        contacts: [ 1, 2, 3, 4 ],
        pager: { offset: 10, limit: 5, total: 14 }
    });
});

test('get total from contacts count', async () => {
    contacts.push(1, 2, 3, 4, 5);
    const params = { limit: 5, offset: 10 };
    mockSuper.calls.exec.mockResolvedValueOnce({ total: 100500 });

    const result = await getContacts.fetch(1, params);
    expect(result).toEqual({
        contacts: [ 1, 2, 3, 4, 5 ],
        pager: { limit: 5, offset: 10, total: 100500 }
    });
});

test('get total from shared list', async () => {
    contacts.push(1, 2, 3, 4, 5);
    const params = { limit: 5, offset: 10, listId: 123 };
    mockSuper.calls.exec.mockResolvedValueOnce({
        count: 42
    });

    const result = await getContacts.fetch(1, params);
    expect(result).toEqual({
        contacts: [ 1, 2, 3, 4, 5 ],
        pager: { limit: 5, offset: 10, total: 42 }
    });
});

test('get total from tags', async () => {
    const params = { limit: 5, offset: 10, tagId: '13' };
    mockSuper.calls.exec.mockResolvedValueOnce({
        tags: [ { tag_id: 13, contacts_count: 3 } ]
    });

    const result = await getContacts.fetch(1, params);
    expect(result).toEqual({
        contacts: [ ],
        pager: { limit: 5, offset: 10, total: 3 }
    });
});

test('get total from tags for non-existent tag', async () => {
    const params = { limit: 5, offset: 10, tagId: '13' };
    mockSuper.calls.exec.mockResolvedValueOnce({
        tags: [ { tag_id: 42, contacts_count: 3 } ]
    });

    const result = await getContacts.fetch(1, params);
    expect(result).toEqual({
        contacts: [ ],
        pager: { limit: 5, offset: 10, total: 0 }
    });
});
