var sinon = require('sinon'),
    Vow = require('vow');

module.exports = function(chai) {
    var assert = chai.assert;

    assert.callsNextWith = function(fn, fnArgs, nextArgs) {
        var next = sinon.spy();

        fnArgs.push(next);
        fn.apply(null, fnArgs);

        assert.isTrue(next.calledOnce, 'next() should be called once');

        if (nextArgs) {
            assert.isTrue(next.calledWith.apply(next, nextArgs), 'next() should be called with arguments');
        }
    };

    assert.logsTerror = function(TerrorClass, CODE, fn) {
        var args = Array.prototype.slice.call(arguments, 3),
            result;

        TerrorClass.prototype.log = sinon.spy(TerrorClass.prototype.log);

        function assertLog() {
            assert.isTrue(TerrorClass.prototype.log.thisValues.some(function(terrorInstance) {
                /**
                 * codeName for Terror@0.4
                 * code for Terror@1.3
                 */
                return (terrorInstance.codeName || terrorInstance.code) === CODE;
            }));

            TerrorClass.prototype.log.reset();
        }

        try {
            result = fn.apply(null, args);
        }
        catch (e) {}
        finally {
            if (Vow.isPromise(result)) {
                return result.always(assertLog);
            } else {
                assertLog();
            }
        }
    };
};
