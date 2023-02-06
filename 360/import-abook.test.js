'use strict';

const s = require('serializr');
const schema = require('./import-abook.js');
const deserialize = s.deserialize.bind(s, schema);

test('returns import status for json', () => {
    const data = { rec_cnt: 13 };
    const result = deserialize(data);

    expect(result).toEqual({
        status: 'ok',
        result: { saved: 13 }
    });
});

test('returns import status for no count', () => {
    const data = {};
    const result = deserialize(data);

    expect(result).toEqual({
        status: 'ok',
        result: { saved: 0 }
    });
});
