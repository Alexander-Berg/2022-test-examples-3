'use strict';

const s = require('serializr');
const schema = require('./translate-text.js');
const deserialize = s.deserialize.bind(s, schema);

test('returns text translation', () => {
    const result = deserialize({
        code: 200,
        lang: 'ru-en',
        text: [
            'Hello, world.'
        ]
    });

    expect(result).toEqual({
        translation: {
            lang: 'ru-en',
            text: 'Hello, world.'
        }
    });
});
