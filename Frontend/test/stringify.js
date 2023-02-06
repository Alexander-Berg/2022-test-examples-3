/* eslint-env mocha */
'use strict';

const assert = require('assert');

const stringify = require('../stringify');

describe('YandexLogger. Stringify', () => {
    it('should serialize to json', () => {
        let a = {
            data: 'foo',
            number: 2,
            date: new Date(1000),
            error: new Error(),
        };

        assert.strictEqual(stringify(a), '{"data":"foo","number":2,"date":"1970-01-01T00:00:01.000Z","error":{}}');
    });

    it('should serialize circular json', () => {
        let a = {};
        a.a = a;
        a.b = { a };

        assert.strictEqual(stringify(a), '{"a":"[Circular ~]","b":{"a":"[Circular ~]"}}');
    });
});
