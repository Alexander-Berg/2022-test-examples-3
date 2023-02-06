'use strict';

jest.mock('../../../../schemas/abook-contact.v0.js', () => ({
    factory: ({ json }) => ({
        mock: true,
        ...json
    }),
    props: {}
}));

const s = require('serializr');
const schema = require('./get-abook-search-by-email.js');
const deserialize = s.deserialize.bind(s, schema);

test('returns contact for email', () => {
    const contact = {
        contact_id: 13
    };
    const result = deserialize({ e: contact }, null, { email: 'e' });
    expect(result).toEqual({
        mock: true,
        contact_id: 13
    });
});

test('returns empty object for no email', () => {
    const result = deserialize({}, null, { email: 'e' });
    expect(result).toEqual({});
});
