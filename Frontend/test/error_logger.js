/* global describe, it, beforeEach, afterEach */

/* eslint-disable no-console */

let assert = require('chai').assert;
let sinon = require('sinon');

describe('ErrorLogger', function() {
    let app = require('../app');
    let qloudLogger = require('../app/lib/qloud_logger');
    let errorLogger = app.ErrorLogger;
    let supportedMethods = ['log', 'error', 'warn', 'info'];

    describe('debugLogger', function() {
        let originals = {
            log: null,
            error: null,
            warn: null,
            info: null,
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
            let errorMessage = 'error message';
            let debugLogger = errorLogger.debugLogger();

            supportedMethods.forEach(function(method) {
                debugLogger(errorMessage, method);

                assert.strictEqual(console[method].getCall(0).args[1], errorMessage);
            });
        });

        it('should use console.error by default', function() {
            let errorMessage = 'error message';
            let debugLogger = errorLogger.debugLogger();

            debugLogger(errorMessage);

            assert.strictEqual(console.error.getCall(0).args[1], errorMessage);
        });
    });

    describe('qloudLogger', function() {
        it('should return a function', function() {
            assert.strictEqual(typeof errorLogger.qloudLogger(), 'function');
        });

        it('should call passed console method', function() {
            let originalConsoleError = console.error;
            console.error = sinon.spy(console, 'error');

            let logger = errorLogger.qloudLogger(console.error);

            logger.call({});

            assert.isTrue(console.error.calledOnce);

            console.error = originalConsoleError;
        });

        it('should wrap data with prepareQloudData', function() {
            let logger = errorLogger.qloudLogger(console.error);
            let result = 'Yes it’s call prepareQloudData';
            let toString = 'toString';
            let stack = 'errorStack';
            let fields = {};
            let errorMock = {
                toString: sinon.stub().returns(toString),
                stack: stack,
                data: fields,
            };

            let originalPrepareQloudData = qloudLogger.prepareQloudData;

            qloudLogger.prepareQloudData = sinon.stub().returns(result);

            logger.call(errorMock);

            assert.deepEqual(qloudLogger.prepareQloudData.getCall(0).args, [toString, stack, fields]);

            qloudLogger.prepareQloudData = originalPrepareQloudData;
        });
    });
});
