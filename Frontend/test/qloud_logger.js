/* global describe, it */

let assert = require('chai').assert;

describe('QloudLogger', function() {
    let qloudLogger = require('../app/lib/qloud_logger');

    describe('prepareQloudData', function() {
        it('should make a proper structure', function() {
            let message = 'This is message';
            let stack = 'This is stack trace';
            let fields = { foo: 'bar' };
            let expected = {
                msg: '[pid:' + process.pid + '] ' + message,
                stackTrace: stack,
                '@fields': {
                    pid: process.pid,
                    foo: 'bar',
                },
            };

            let res = qloudLogger.prepareQloudData(message, stack, fields);

            assert.strictEqual(res, JSON.stringify(expected));
        });
    });
});
