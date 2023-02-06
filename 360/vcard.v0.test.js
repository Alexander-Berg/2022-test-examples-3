'use strict';

const s = require('serializr');
const vcardSchema = require('./vcard.v0.js');
const deserialize = s.deserialize.bind(s, vcardSchema);

test('returns vcard data', () => {
    const data = {
        names: [
            {
                first: 'John',
                middle: 'S.',
                last: 'Doe',
                suffix: 'jr',
                prefix: 'Mr.'
            }
        ],
        emails: [
            {
                email: 'foo@example.com',
                type: [ 'type' ],
                label: 'label'
            }
        ],
        instant_messengers: [
            {
                protocol: 'skype',
                type: [ 'type' ],
                service_id: 'service_id',
                service_type: 'service_type'
            }
        ],
        social_profiles: [
            {
                profile: 'facebook.com/example',
                type: [ 'type' ],
                label: 'facebook'
            }
        ],
        telephone_numbers: [
            {
                telephone_number: '(123) 456-7890',
                additional: '333',
                type: [ 'type' ],
                label: 'label'
            }
        ],
        vcard_uids: [ 'vcard_uid' ],
        directory_entries: [
            {
                entry_id: 'entry_id',
                type: [ 'type' ]
            }
        ],
        notes: [ 'note', 'note2' ],
        description: 'description',
        events: [
            {
                year: '2019',
                month: '11',
                day: '11',
                type: [ 'type' ],
                label: 'label'
            }
        ],
        photos: [
            {
                uri: 'https://example.com/photo.jpg',
                storage: 'storage',
                type: [ 'type' ]
            }
        ],
        addresses: [
            {
                extended: 'extended',
                post_office_box: '0',
                street: 'Red Square, 1',
                city: 'Moscow',
                region: 'Moscow',
                postal_code: '111000',
                country: 'Russia',
                label: 'label',
                type: [ 'type' ]
            }
        ],
        organizations: [
            {
                company: 'company',
                title: 'title',
                department: 'department',
                type: [ 'type' ]
            }
        ],
        nicknames: [ 'nickname', 'nick' ],
        websites: [
            {
                url: 'https://example.com',
                label: 'label',
                type: [ 'type' ]
            }
        ]
    };
    const result = deserialize(data);
    expect(result).toEqual(data);
});
