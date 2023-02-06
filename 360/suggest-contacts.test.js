'use strict';

const s = require('@ps-int/mail-lib').helpers.serializr;
const suggestContactsSchema = require('./suggest-contacts.js');
const deserialize = s.deserialize.bind(s, suggestContactsSchema);

describe('suggestContactsSchema', () => {
    it('extracts name and email', () => {
        const result = deserialize({
            contacts: [
                { name: 'n', email: 'e', other: 'd' },
                { name: 'a', email: 'b', oops: '2' }
            ]
        });
        expect(result).toEqual([
            { name: 'n', email: 'e' },
            { name: 'a', email: 'b' }
        ]);
    });

    it('ok without contacts', () => {
        const result = deserialize({});
        expect(result).toEqual([]);
    });
});
