/* global describe, it, beforeEach, afterEach */

var assert = require('chai').assert,
    sinon = require('sinon');

describe('ErrorLogger', function() {
    var app = require('../app'),
        qloudLogger = require('../app/lib/qloud_logger'),
        errorLogger = app.ErrorLogger,
        supportedMethods = [ 'log', 'error', 'warn', 'info' ];

    describe('debugLogger', function() {
        var originals = {
            log: null,
            error: null,
            warn: null,
            info: null
        };

        beforeEach(function() {
            Object.keys(originals).forEach(function(method) {
                originals[method] = console[method];
                console[method] = sinon.spy(console, method);
            });
        });

        afterEach(function() {
            Object.keys(originals).forEach(function(method) {
                console[method] = originals[method];
            });
        });

        it('should return a function', function() {
            assert.strictEqual(typeof errorLogger.debugLogger(), 'function');
        });

        it('should call the console method we passed as level', function() {
            var errorMessage = 'error message';
            var debugLogger = errorLogger.debugLogger();

            supportedMethods.forEach(function(method) {
                debugLogger(errorMessage, method);

                assert.strictEqual(console[method].getCall(0).args[1], errorMessage);
            });
        });

        it('should use console.error by default', function() {
            var errorMessage = 'error message';
            var debugLogger = errorLogger.debugLogger();

            debugLogger(errorMessage);

            assert.strictEqual(console.error.getCall(0).args[1], errorMessage);
        });
    });

    describe('qloudLogger', function() {
        it('should return a function', function() {
            assert.strictEqual(typeof errorLogger.qloudLogger(), 'function');
        });

        it('should call passed console method', function() {
            var originalConsoleError = console.error;
            console.error = sinon.spy(console, 'error');

            var logger = errorLogger.qloudLogger(console.error);

            logger.call({});

            assert.isTrue(console.error.calledOnce);

            console.error = originalConsoleError;
        });

        it('should wrap data with prepareQloudData', function() {
            var logger = errorLogger.qloudLogger(console.error),
                result = 'Yes it’s call prepareQloudData',
                toString = 'toString',
                stack = 'errorStack',
                fields = {},
                errorMock = {
                    toString: sinon.stub().returns(toString),
                    stack: stack,
                    data: fields
                };

            var originalPrepareQloudData = qloudLogger.prepareQloudData;

            qloudLogger.prepareQloudData = sinon.stub().returns(result);

            logger.call(errorMock);

            assert.deepEqual(qloudLogger.prepareQloudData.getCall(0).args, [ toString, stack, fields ]);

            qloudLogger.prepareQloudData = originalPrepareQloudData;
        });
    });
});
