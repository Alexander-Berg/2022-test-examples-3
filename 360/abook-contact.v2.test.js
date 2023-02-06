'use strict';

const s = require('serializr');
const abookContactSchema = require('./abook-contact.v2.js');
const deserialize = s.deserialize.bind(s, abookContactSchema);

const Validator = require('../lib/validator/response-validator.js');
const validator = new Validator(require.resolve('./abook-contact.v2.yaml'));
const validateFn = (result) => () => validator.call(result);

test('returns contact for collie', () => {
    const contact = {
        contact_id: 13,
        list_id: 14,
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

test('returns contact for collie (no email tags)', () => {
    const contact = {
        contact_id: 13,
        list_id: 14,
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
        list_id: 14,
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
        list_id: 14,
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
