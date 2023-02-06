'use strict';

jest.mock('@yandex-int/duffman', () => ({
    helpers: {
        email: { ref: (email) => `ref-${email}` }
    }
}));

const s = require('serializr');
const abookSuggestSchema = require('./abook-suggest.js');
const deserialize = s.deserialize.bind(s, abookSuggestSchema);

const defaultVcard = {
    names: [
        { first: 'first' },
        { prefix: 'mr', last: 'last', middle: '' }
    ],
    emails: [
        { email: 'email1@yandex.ru' },
        { email: 'email2@yandex.ru' }
    ],
    telephone_numbers: [
        { telephone_number: '+700' },
        { telephone_number: '+100' }
    ]
};

const defaultEmails = [
    { id: 1, value: 'email1@yandex.ru', last_usage: 0 },
    { id: 2, value: 'email2@yandex.ru', last_usage: 12345 }
];

test('returns contacts', () => {
    const data = {
        contacts: [ {
            contact_id: 42,
            emails: defaultEmails,
            vcard: defaultVcard
        }, {
            contact_id: 43,
            emails: [],
            vcard: defaultVcard
        } ],
        tags: []
    };
    const result = deserialize(data);

    expect(result).toMatchSnapshot();
});

test('returns contact with email only', () => {
    const data = {
        contacts: [ {
            contact_id: 42,
            emails: [ { value: 'a@a.ru' } ],
            vcard: {
                names: [],
                emails: [ { email: 'a@a.ru' } ]
            }
        } ],
        tags: []
    };
    const result = deserialize(data);

    expect(result).toMatchSnapshot();
});

test('returns contact without email', () => {
    const vcard = {
        names: defaultVcard.names.slice(0, 1),
        emails: [],
        telephone_numbers: defaultVcard.telephone_numbers.slice(0, 1)
    };
    const data = {
        contacts: [ {
            contact_id: 42,
            vcard: vcard
        } ],
        tags: []
    };
    const result = deserialize(data);

    expect(result).toMatchSnapshot();
});

test('returns contact with organization', () => {
    const data = {
        contacts: [ {
            contact_id: 42,
            emails: defaultEmails.slice(1, 2),
            vcard: {
                ...defaultVcard,
                organizations: [
                    {
                        title: 'Умилять',
                        department: 'Котики'
                    }
                ]
            }
        } ],
        tags: []
    };
    const result = deserialize(data);

    expect(result).toMatchSnapshot();
});

test('returns contact with last organization', () => {
    const data = {
        contacts: [ {
            contact_id: 42,
            emails: defaultEmails.slice(1, 2),
            vcard: {
                ...defaultVcard,
                organizations: [
                    {
                        title: 'Умилять',
                        department: 'Котики'
                    },
                    {
                        title: 'Мяукать',
                        department: 'Котики'
                    }
                ]
            }
        } ],
        tags: []
    };
    const result = deserialize(data);

    expect(result).toMatchSnapshot();
});

test('returns contact without organizations', () => {
    const data = {
        contacts: [ {
            contact_id: 42,
            emails: defaultEmails.slice(1, 2),
            vcard: {
                ...defaultVcard,
                organizations: []
            }
        } ],
        tags: []
    };
    const result = deserialize(data);

    expect(result).toMatchSnapshot();
});

test('returns groups', () => {
    const data = {
        contacts: [],
        tags: [ {
            tag_id: 13,
            name: 'Super Group',
            contacts: [ {
                contact_id: 42,
                emails: defaultEmails,
                vcard: defaultVcard
            } ]
        } ]
    };
    const result = deserialize(data);

    expect(result).toMatchSnapshot();
});
