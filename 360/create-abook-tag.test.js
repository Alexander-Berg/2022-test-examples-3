'use strict';

const s = require('serializr');
const schema = require('./create-abook-tag.js');
const deserialize = s.deserialize.bind(s, schema);

test('retunds tagId as string', () => {
    const result = deserialize({ tag_id: 42, revision: 0 });
    expect(result).toEqual({ tagId: '42' });
});
