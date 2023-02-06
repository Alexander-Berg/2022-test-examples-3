'use strict';

const s = require('serializr');
const schema = require('./get-pdd-info.js');
const deserialize = s.deserialize.bind(s, schema);

test('returns pdd-info', () => {
    const data = {
        'name': 'fake.ignored',
        'pop_enabled': 'true',
        'imap_enabled': 'false',
        'custom-hide-calendar-tab': 'yes',
        'logo': 'logo',
        'custom-news': 'news&news',
        'custom-help': 'help',
        'custom-direct-num': '123'
    };
    const result = deserialize(data, null, { pddDomain: 'fake.example' });
    expect(result).toEqual({
        'pdd-domain': 'fake.example',
        'pdd-pop-enabled': true,
        'pdd-imap-enabled': false,
        'pdd-hide-calendar': true,
        'pdd-logo': 'logo',
        'pdd-news': 'news&amp;news',
        'pdd-custom-help': 'help',
        'pdd-direct': '123'
    });
});
