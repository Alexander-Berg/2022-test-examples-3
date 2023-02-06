'use strict';

const s = require('serializr');
const schema = require('./get-abook-emails-by-tags.js');
const deserialize = s.deserialize.bind(s, schema);

const data = {
    emails: [ {
        tag_id: 1,
        count: 1,
        emails: [ {
            email_id: 2,
            contact_id: 3,
            email: 'a@a.a'
        } ]
    }, {
        tag_id: 4,
        count: 2,
        emails: [ {
            email_id: 2,
            contact_id: 3,
            email: 'a@a.a'
        }, {
            email_id: 5,
            contact_id: 4,
            email: 'b@b.b'
        } ]
    } ]
};

test('returns all', () => {
    const result = deserialize(data);
    expect(result).toEqual({
        emailsByTag: [ {
            id: '1',
            count: 1,
            emails: [ {
                id: '2',
                contactId: '3',
                email: 'a@a.a'
            } ]
        }, {
            id: '4',
            count: 2,
            emails: [ {
                id: '2',
                contactId: '3',
                email: 'a@a.a'
            }, {
                id: '5',
                contactId: '4',
                email: 'b@b.b'
            } ]
        } ]
    });
});

test('returns only count', () => {
    const result = deserialize(data, null, { onlyCount: true });
    expect(result).toEqual({
        emailsByTag: [ {
            id: '1',
            count: 1
        }, {
            id: '4',
            count: 2
        } ]
    });
});
