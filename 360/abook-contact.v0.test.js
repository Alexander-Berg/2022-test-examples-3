'use strict';

jest.mock('@yandex-int/duffman', () => ({
    helpers: {
        email: {
            ref: (e) => `ref-${e}`
        }
    }
}));

const s = require('serializr');
const abookContactSchema = require('./abook-contact.v0.js');
const deserialize = s.deserialize.bind(s, abookContactSchema);

const Validator = require('../lib/validator/response-validator.js');
const validator = new Validator(require.resolve('./abook-contact.v0.yaml'));
const validateFn = (result) => () => validator.call(result);

test('returns contact for collie', () => {
    const contact = {
        contact_id: 13,
        tag_ids: [ 1, 2 ],
        vcard: {
            names: [ { last: 'n' } ],
            telephone_numbers: [ { telephone_number: '+0' } ],
            events: [ {
                type: [ 'birthday', 'wtf' ],
                day: 1,
                month: 10,
                year: 2010
            }, {
                type: [ 'unknown' ]
            } ]
        },
        emails: [ { id: 1, value: 'e', tags: [ 2 ] } ]
    };
    const result = deserialize(contact);
    expect(validateFn(result)).not.toThrow();
    expect(result).toMatchSnapshot();
});

test('returns contact for collie (noname)', () => {
    const contact = {
        contact_id: 13,
        tag_ids: [],
        vcard: {
            names: [ ],
            events: [ {
                type: [ 'birthday' ],
                year: 1999
            } ],
            directory_entries: [ {
                entry_id: 1130000039137066,
                type: [ 'user' ]
            } ],
            organizations: [ {
                title: 'Руководитель разработки абука',
                department: 'Отдельный отдел',
                summary: 'Котики'
            } ]
        },
        emails: [ ]
    };
    const result = deserialize(contact);
    expect(validateFn(result)).not.toThrow();
    expect(result).toMatchSnapshot();
});

test('returns contact for collie (no birthdate)', () => {
    const contact = {
        contact_id: 13,
        tag_ids: [],
        vcard: {
            names: [ ],
            events: [ {
                type: [ 'anniversary' ],
                year: 1999
            } ]
        },
        emails: [ ]
    };
    const result = deserialize(contact);
    expect(validateFn(result)).not.toThrow();
    expect(result).toMatchSnapshot();
});

test('returns contact for collie (no email tags)', () => {
    const contact = {
        contact_id: 13,
        tag_ids: [],
        vcard: {
            names: [ ]
        },
        emails: [ { id: 1, value: 'e' } ]
    };
    const result = deserialize(contact);
    expect(validateFn(result)).not.toThrow();
    expect(result).toMatchSnapshot();
});

test('returns contact for collie (shared contact)', () => {
    const contact = {
        contact_id: 13,
        contact_owner_user_type: 'connect_organization',
        shared: true,
        tag_ids: [],
        vcard: {
            names: [ ]
        },
        emails: [ ]
    };
    const result = deserialize(contact);
    expect(validateFn(result)).not.toThrow();
    expect(result).toMatchSnapshot();
});

test('returns contact for aceventura', () => {
    const contact = {
        contact_id: 13,
        tag_ids: [],
        vcard: {
            names: [ { } ],
            events: [ {
                type: [ 'birthday', 'wtf' ],
                day: 1,
                month: 10
            }, {
                type: [ 'unknown' ]
            } ]
        },
        emails: [ { id: 1, value: 'e', tags: [ 2 ] } ]
    };
    const result = deserialize(contact);
    expect(validateFn(result)).not.toThrow();
    expect(result).toMatchSnapshot();
});

test('coverage', () => {
    const contact = {
        contact_id: 13,
        tag_ids: [],
        vcard: {
            names: [ ],
            organizations: [ ]
        },
        emails: [ ]
    };
    const result = deserialize(contact);
    expect(validateFn(result)).not.toThrow();
    expect(result).toMatchSnapshot();
});

test('coverage 2', () => {
    const contact = {
        contact_id: 13,
        tag_ids: [],
        vcard: {
            names: [ ],
            organizations: [ ],
            directory_entries: [ ]
        },
        emails: [ ]
    };
    const result = deserialize(contact);
    expect(validateFn(result)).not.toThrow();
    expect(result).toMatchSnapshot();
});
