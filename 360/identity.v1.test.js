'use strict';

const s = require('serializr');
const identitySchema = require('./identity.v1.js');
const deserialize = s.deserialize.bind(s, identitySchema);

test('returns the same data', () => {
    const data = { x: 1 };
    const result = deserialize(data);
    expect(result).toEqual(data);
});
