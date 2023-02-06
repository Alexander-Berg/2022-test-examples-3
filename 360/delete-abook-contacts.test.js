'use strict';

const s = require('serializr');
const schema = require('./delete-abook-contacts.js');

test('returns contact ids', () => {
    const args = { contactIds: [ '42', '13' ] };
    const result = s.deserialize(schema, {}, null, args);
    expect(result).toEqual({ contactIds: [ '42', '13' ] });
});
