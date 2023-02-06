/* global describe, it */

var assert = require('chai').assert;

describe('QloudLogger', function() {
    var qloudLogger = require('../app/lib/qloud_logger');

    describe('prepareQloudData', function() {
        it('should make a proper structure', function() {
            var message = 'This is message',
                stack = 'This is stack trace',
                fields = { foo: 'bar' },
                expected = {
                    msg: '[pid:' + process.pid + '] ' + message,
                    stackTrace: stack,
                    '@fields': {
                        pid: process.pid,
                        foo: 'bar'
                    }
                };

            var res = qloudLogger.prepareQloudData(message, stack, fields);

            assert.strictEqual(res, JSON.stringify(expected));
        });
    });
});
