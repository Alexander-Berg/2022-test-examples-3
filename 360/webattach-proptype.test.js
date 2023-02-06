'use strict';

const s = require('serializr');
const webattachProptype = require('./webattach-proptype.js');

const response = {
    1.1: 'TEST_SID',
    1.2: 'TEST_SID',
    0: 'FULL_MESSAGE_SID',
    all: 'ALL_ATTACHES_SID'
};

test('single webattach', () => {
    const schema = webattachProptype('TEST_ALIAS', [ 'name' ]);
    const deserialize = s.deserialize.bind(s, schema, response, null);
    const result = deserialize({ webattach: 'http://webattach', attachmentId: '1.1' });
    expect(result).toEqual({
        TEST_ALIAS: 'http://webattach?sid=TEST_SID'
    });
});

test('with filename', () => {
    const schema = webattachProptype('TEST_ALIAS', [ 'name' ]);
    const deserialize = s.deserialize.bind(s, schema, response, null);
    const result = deserialize({
        webattach: 'http://webattach',
        attachmentId: 'all',
        name: 'TEST_NAME.zip'
    });
    expect(result).toEqual({
        TEST_ALIAS: 'http://webattach?name=TEST_NAME.zip&sid=ALL_ATTACHES_SID'
    });
});

test('message-source', () => {
    const schema = webattachProptype('TEST_ALIAS', [ 'name' ]);
    const deserialize = s.deserialize.bind(s, schema, response, null);
    const result = deserialize({
        webattach: 'http://webattach',
        attachmentId: '0'
    });
    expect(result).toEqual({
        TEST_ALIAS: 'http://webattach?sid=FULL_MESSAGE_SID'
    });
});
