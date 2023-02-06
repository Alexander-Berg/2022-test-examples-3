'use strict';

jest.mock('../../../../schemas/abook-contact.v0.js', () => ({
    factory: ({ json }) => ({
        mock: true,
        ...json
    }),
    props: {}
}));

const s = require('serializr');
const schema = require('./abook-contacts.js');
const deserialize = s.deserialize.bind(s, schema);

test('returns contacts and pager', () => {
    const data = {
        contacts: [ { a: 1 } ],
        pager: { limit: 5, offset: 10, total: 18 }
    };
    const result = deserialize(data);
    expect(result).toEqual({
        count: 1,
        contact: [ { a: 1, mock: true } ],
        pager: {
            'skipped': 10,
            'items-per-page': 5,
            'items-count': 18
        },
        $collie: true
    });
});
