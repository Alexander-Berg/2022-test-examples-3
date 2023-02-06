const assert = require('assert');

const setIntervalWithWait = require('worker/setIntervalWithWait');

describe('setIntervalWithWait', () => {
    it('should call and wait while callback is finished', () => {
        const intervalId = setIntervalWithWait(intervalCallback, 100, 'test');
        let callbackCalls = 0;
        let awaitCalls = 0;

        return new Promise(resolve => (
            setTimeout(() => {
                assert.strictEqual(awaitCalls, 3);
                assert.strictEqual(callbackCalls, awaitCalls);
                resolve(true);
            }, 1000)
        ));

        function intervalCallback() {
            if (callbackCalls === 3) {
                return clearInterval(intervalId);
            }

            callbackCalls++;

            return new Promise(resolve => {
                setTimeout(() => {
                    awaitCalls++;
                    resolve(true);
                }, 200);
            });
        }
    });

    it('should continue to call callback after an error', () => {
        const intervalId = setIntervalWithWait(intervalCallback, 100, 'test');
        let callbackCalls = 0;
        let awaitCalls = 0;

        return new Promise(resolve => (
            setTimeout(() => {
                assert.strictEqual(awaitCalls, 3);
                assert.strictEqual(callbackCalls, 4);
                resolve(true);
            }, 1000)
        ));

        function intervalCallback() {
            if (callbackCalls === 3) {
                callbackCalls++;

                throw new Error('test error');
            }

            if (callbackCalls === 4) {
                return clearInterval(intervalId);
            }

            callbackCalls++;

            return new Promise(resolve => {
                setTimeout(() => {
                    awaitCalls++;
                    resolve(true);
                }, 200);
            });
        }
    });
});
