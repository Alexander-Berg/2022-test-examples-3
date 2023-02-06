'use strict';

const s = require('serializr');
const schema = require('./get-translation-langs.js');
const deserialize = s.deserialize.bind(s, schema);

test('returns array of langs', () => {
    const result = deserialize({
        dirs: [ 'az-ru', 'be-bg', 'be-cs', 'be-de' ],
        langs: {
            af: 'Африкаанс',
            az: 'Азербайджанский',
            en: 'Английский'
        }
    });

    expect(result).toEqual({
        translationLangs: [
            { lang: 'af', name: 'Африкаанс' },
            { lang: 'az', name: 'Азербайджанский' },
            { lang: 'en', name: 'Английский' }
        ]
    });
});
