'use strict';

const getBodySnippet = require('./get-body-snippet.js');

test('работает без фактов', () => {
    expect(() => {
        getBodySnippet({});
    }).not.toThrow();
});

test('без фактов возвращает пустую строку', () => {
    expect(getBodySnippet({})).toEqual('');
});

test('работает', () => {
    expect(getBodySnippet({
        body: [
            {
                facts: '{"snippet":{"text": "test"}}'
            }
        ]
    })).toMatchSnapshot();
});
