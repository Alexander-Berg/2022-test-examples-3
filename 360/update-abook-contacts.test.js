'use strict';

const s = require('serializr');
const schema = require('./update-abook-contacts.js');

test('returns contact id', () => {
    const args = {
        updatedContacts: [
            { contactId: '42' },
            { contactId: '13' }
        ]
    };
    const result = s.deserialize(schema, {}, null, args);
    expect(result).toEqual({ contactIds: [ '42', '13' ] });
});
