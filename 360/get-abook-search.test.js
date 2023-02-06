'use strict';

jest.mock('../../../../schemas/abook-contact.v0.js', () => ({
    factory: ({ json }) => ({
        mock: true,
        ...json
    }),
    props: {}
}));

const s = require('serializr');
const schema = require('./get-abook-search.js');
const deserialize = s.deserialize.bind(s, schema);

test('returns contact', () => {
    const data = {
        contacts: [ { contact_id: 13 } ],
        total: 5
    };
    const result = deserialize(data, null, { offset: 1, limit: 1 });
    expect(result).toEqual({
        count: 1,
        contact: [ { mock: true, contact_id: 13 } ],
        pager: {
            'skipped': 1,
            'items-per-page': 1,
            'items-count': 5
        },
        $collie: true
    });
});
