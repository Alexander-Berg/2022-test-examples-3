const assert = require('assert');

const moduleName = '../../src/inline/interface';

function reloadModule() {
    delete require.cache[require.resolve(moduleName)];
    require(moduleName);
}

function assertDefTimesCount(expectedCount, message) {
    assert.strictEqual(Ya.Rum._defTimes.length, expectedCount, message);
}

describe('RUM interface', function() {
    beforeEach(function() {
        // mock required browser environment
        global.window = global;
        window.performance = { timing: { navigationStart: Date.now() - 100 } };
        window.requestAnimationFrame = function(callback) {
            setTimeout(callback, 1);
        };

        // noinspection JSUnusedGlobalSymbols
        global.document = {
            addEventListener: function() {
            }
        };

        reloadModule();
        Ya.Rum._settings.sendFirstRaf = true;
    });

    afterEach(function() {
        ['Ya', 'performance', 'requestAnimationFrame'].forEach(name => delete window[name]);
        delete global.window;
        delete global.document;
    });

    it('should not throw in Ya.Rum.getTime', function() {
        Ya.Rum.getTime();
    });

    it('should not throw in Ya.Rum.mark', function() {
        Ya.Rum.mark();
    });

    it('should not throw in Ya.Rum.sendTimeMark', function() {
        Ya.Rum.sendTimeMark();
    });

    it('should not throw in Ya.Rum.sendDelta', function() {
        Ya.Rum.sendDelta();
    });

    it('should send rAF metrics when document is visible', function(done) {
        Ya.Rum.sendRaf(85);

        setTimeout(function() {
            assertDefTimesCount(2, 'should add two time marks');

            const RAF_MIN = 100;
            const RAF_MAX = 200;

            const rafFirst = Ya.Rum._defTimes[0];
            assert.strictEqual(rafFirst[0], '2616.85.205');
            assert(rafFirst[1] >= RAF_MIN && rafFirst[1] <= RAF_MAX,
                `raf_metric.p0.first time should be between ${RAF_MIN} and ${RAF_MAX} ms but is ${rafFirst[1]} ms`);

            const rafLast = Ya.Rum._defTimes[1];
            assert.strictEqual(rafLast[0], '2616.85.1928');
            assert(rafLast[1] >= rafFirst[1] && rafLast[1] <= RAF_MAX,
                `raf_metric.p0.last time should be between raf.p0.first and ${RAF_MAX} ms but is ${rafLast[1]} ms`);

            done();
        }, 20);
    });

    it('should send only last rAF metrics if disabled', function(done) {
        Ya.Rum._settings.sendFirstRaf = false;

        Ya.Rum.sendRaf(85);

        setTimeout(function() {
            assertDefTimesCount(1, 'should add only one time mark');

            const rafLast = Ya.Rum._defTimes[0];
            assert.strictEqual(rafLast[0], '2616.85.1928');

            done();
        }, 20);
    });

    it('should not send rAF metrics when document is initially hidden', function(done) {
        Ya.Rum.vsStart = 'hidden';
        Ya.Rum.sendRaf(85);

        setTimeout(function() {
            assertDefTimesCount(0, 'should not add time marks');
            done();
        }, 20);
    });

    it('should send rAF metrics when "forcePaintTimeSending" setting is enabled', function(done) {
        Ya.Rum.vsStart = 'hidden';
        Ya.Rum._settings.forcePaintTimeSending = 1;
        Ya.Rum.sendRaf(85);

        setTimeout(function() {
            assertDefTimesCount(2, 'should add two time marks');
            done();
        }, 20);
    });

    it('should not send rAF metrics when document becomes hidden after first rAF', function(done) {
        const visibilityChanged = [false, true, true];
        Ya.Rum.isVisibilityChanged = function() {
            return visibilityChanged.shift();
        };
        Ya.Rum.sendRaf(85);

        setTimeout(function() {
            assertDefTimesCount(0, 'should not add time marks');
            done();
        }, 20);
    });

    it('should send only raf_metric.p0.first when document becomes hidden after second rAF', function(done) {
        const visibilityChanged = [false, false, true];
        Ya.Rum.isVisibilityChanged = function() {
            return visibilityChanged.shift();
        };
        Ya.Rum.sendRaf(85);

        setTimeout(function() {
            assertDefTimesCount(1, 'should add time mark for raf_metric.p0.first');

            const rafFirst = Ya.Rum._defTimes[0];
            assert.strictEqual(rafFirst[0], '2616.85.205');
            assert(rafFirst[1] > 100 && rafFirst[1] < 200,
                `raf_metric.p0.first time should be between 100 and 200 ms but is ${rafFirst[1]} ms`);

            done();
        }, 20);
    });

    it('should register a mark listener for specified counterId', function() {
        const counterId = '123';
        const cb = _ => {};

        Ya.Rum.on(counterId, cb);

        assert(Array.isArray(Ya.Rum._markListeners[counterId]));
        assert.strictEqual(Ya.Rum._markListeners[counterId][0], cb);
    });
});
