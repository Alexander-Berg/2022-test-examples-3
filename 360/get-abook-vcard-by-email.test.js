'use strict';

jest.mock('../../../../schemas/vcard.v0.js', () => ({
    factory: ({ json }) => ({
        mock: true,
        ...json
    }),
    props: {}
}));

const s = require('serializr');
const schema = require('./get-abook-vcard-by-email.js');
const deserialize = s.deserialize.bind(s, schema);

test('returns contact for email', () => {
    const contact = {
        contact_id: 42,
        list_id: 43,
        vcard: { foo: 'bar' }
    };
    const result = deserialize({ e: contact }, null, { email: 'e' });
    expect(result).toEqual({
        shared: false,
        id: 42,
        list_id: 43,
        vcard: {
            mock: true,
            foo: 'bar'
        }
    });
});

test('returns empty object for no email', () => {
    const result = deserialize({}, null, { email: 'e' });
    expect(result).toEqual({});
});
