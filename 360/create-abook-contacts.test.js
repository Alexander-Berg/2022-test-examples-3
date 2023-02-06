'use strict';

const s = require('serializr');
const schema = require('./create-abook-contacts.js');
const deserialize = s.deserialize.bind(s, schema);

test('cast contact ids to string', () => {
    const result = deserialize({ contact_ids: [ 13, 42 ] });
    expect(result).toEqual({ contactIds: [ '13', '42' ] });
});
