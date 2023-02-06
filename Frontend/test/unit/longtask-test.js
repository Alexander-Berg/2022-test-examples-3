const assert = require('assert');

const moduleName = '../../src/inline/longtask';

function reloadModule() {
    delete require.cache[require.resolve(moduleName)];
    require(moduleName);
}

describe('RUM long task observer', function() {
    beforeEach(function() {
        // mock required browser environment
        global.window = global;
        window.Ya = { Rum: {} };

        window.PerformanceObserver = function(callback) {
            this.callback = callback;
        };
        window.PerformanceObserver.prototype = {
            observe: function() {
            }
        };
        window.PerformanceLongTaskTiming = true;

        reloadModule();
    });

    afterEach(function() {
        ['Ya', 'PerformanceObserver', 'PerformanceLongTaskTiming'].forEach(name => delete window[name]);
        delete global.window;
        delete global.document;
    });

    it('should not be created in unsupported environment', function() {
        window.PerformanceLongTaskTiming = false;
        delete Ya.Rum._tti;
        reloadModule();
        assert.strictEqual(Ya.Rum._tti, undefined);
    });

    it('should be created in supported environment', function() {
        assert.ok(Ya.Rum._tti);
    });

    it('should limit amount of tracked events', function() {
        assert.equal(Ya.Rum._tti.events.length, 0);

        for (var i = 0; i < 310; i++) {
            Ya.Rum._tti.observer.callback({
                i: i,
                getEntries: function() {
                    return [{ i: this.i }];
                }
            });
        }
        assert.equal(Ya.Rum._tti.events.length, 300);
        assert.deepEqual(Ya.Rum._tti.events[0], { i: 10 });
        assert.deepEqual(Ya.Rum._tti.events[99], { i: 109 });
    });
});
