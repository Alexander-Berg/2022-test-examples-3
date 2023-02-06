'use strict';

const {
    serializeNewContacts,
    serializeUpdatedContacts,
    serializeUpdatedTag
} = require('./functions.js');

test('serializeNewContacts', () => {
    const contact = {
        name: { first: 'f', last: 'l' },
        emails: [ 'e1', 'e2' ],
        phones: [ '+1', '+2' ],
        description: 'd',
        birthdate: { day: 5, month: 6, year: 7 },
        tagIds: [ '13' ]
    };
    const result = serializeNewContacts({ params: { newContacts: [ contact ] } });
    expect(result).toEqual([ {
        vcard: {
            names: [ { first: 'f', last: 'l' } ],
            emails: [ { email: 'e1' }, { email: 'e2' } ],
            telephone_numbers: [ { telephone_number: '+1' }, { telephone_number: '+2' } ],
            description: 'd',
            events: [ { type: [ 'birthday' ], day: 5, month: 6, year: 7 } ]
        },
        tag_ids: [ 13 ]
    } ]);
});

test('serializeNewContacts almost empty', () => {
    const contact = {
        emails: [ 'e1' ]
    };
    const result = serializeNewContacts({ params: { newContacts: [ contact ] } });
    expect(result).toEqual([ {
        vcard: {
            emails: [ { email: 'e1' } ]
        }
    } ]);
});

test('serializeUpdatedContacts', () => {
    const contact = {
        contactId: '5',
        name: { suffix: 's' },
        description: ''
    };
    const result = serializeUpdatedContacts({ params: { updatedContacts: [ contact ] } });
    expect(result).toEqual([ {
        contact_id: 5,
        vcard: {
            names: [ { suffix: 's' } ],
            description: ''
        }
    } ]);
});

test('serializeUpdatedTag', () => {
    const params = {
        name: 'tag name',
        removeContactIds: [ '1', '5' ],
        addEmailIds: [ '2.2', '3.15' ]
    };
    const result = serializeUpdatedTag({ params });
    expect(result).toEqual({
        name: 'tag name',
        remove_contact_ids: [ 1, 5 ],
        add_contact_email_ids: [
            { contact_id: 2, email_id: 2 },
            { contact_id: 3, email_id: 15 }
        ]
    });
});
