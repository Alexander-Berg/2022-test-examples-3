'use strict';

const s = require('serializr');
const schema = require('./abook-tags.js');
const deserialize = s.deserialize.bind(s, schema);

test('return tags', () => {
    const data = {
        tag_id: 7,
        name: 'test',
        contacts_count: 13,
        type: 'user',
        skip: 'me'
    };
    const result = deserialize({ tags: [ data ] });

    expect(result).toEqual({
        tags: [ {
            id: '7',
            name: 'test',
            contactsCount: 13,
            type: 'user'
        } ]
    });
});
