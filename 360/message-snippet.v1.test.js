'use strict';

const s = require('serializr');
const messageSnippetSchema = require('./message-snippet.v1.js');
const deserialize = s.deserialize.bind(s, messageSnippetSchema);

test('works', () => {
    const data = {
        text: 'text',
        text_html: 'html',
        foo: 'bar'
    };
    const result = deserialize(data);

    expect(result).toEqual({
        text: 'text',
        html: 'html'
    });
});
