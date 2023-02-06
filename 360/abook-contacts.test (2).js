'use strict';

jest.mock('../../../../schemas/abook-contact.v2.js', () => ({
    factory: ({ json }) => ({
        mock: true,
        ...json
    }),
    props: {}
}));

const s = require('serializr');
const schema = require('./abook-contacts.js');
const deserialize = s.deserialize.bind(s, schema);

test('returns contacts', () => {
    const data = {
        contacts: [ { a: 1 } ]
    };
    const result = deserialize(data);
    expect(result).toEqual({
        contacts: [ { a: 1, mock: true } ]
    });
});
