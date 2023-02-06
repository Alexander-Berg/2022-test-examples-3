'use strict';
const assert = require('assert');
const urequire = require('require-uncached');
const yasmkit = urequire('../');

describe('config', function() {
    it('works', function() {
        assert(yasmkit.config('') === undefined);
        assert.strictEqual(yasmkit.config('test', 'value'), 'value');
        assert.strictEqual(yasmkit.config('test'), 'value');
    });
});
