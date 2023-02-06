'use strict';
const assert = require('assert');
const debug = require('../lib/debug');
const config = require('../lib/config').getSetConfig;

describe('debug', function() {
    it('throwDebug()', function() {
        assert.doesNotThrow(() => {
            debug.throwDebug('lol');
        });
        config('debug', true);
        assert.throws(() => {
            debug.throwDebug('lol');
        });
    });
    after(function() {
        config('debug', false);
    });
});
