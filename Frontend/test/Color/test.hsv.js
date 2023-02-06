var Color = require('../../Color/Color').Color,
    assert = require('assert');

describe('with hsv helpers', function() {
    describe('with darken helpers', function() {
        describe('with darkenHsvRaw', function() {
            it('should decrease value', function() {
                assert.deepEqual(Color.darkenHsvRaw([255, 90, 15], 0.1), [255, 90, 5]);
            });

            it('should decrease value and not check for negative result', function() {
                assert.deepEqual(Color.darkenHsvRaw([255, 90, 15], 0.2), [255, 90, -5]);
            });
        });

        describe('with darkenHsv', function() {
            it('should decrease value and convert negative value to zero', function() {
                assert.deepEqual(Color.darkenHsv([255, 90, 15], 0.2), [255, 90, 0]);
            });
        });

        describe('with darkenHsvWithMirror', function() {
            // Больше тестов в ./test.mirror.js
            it('should decrease value and mirror negative value', function() {
                assert.deepEqual(Color.darkenHsvWithMirror([255, 90, 15], 0.2, 0.1), [255, 90, 15]);
            });
        });
    });

    describe('with lighten helpers', function() {
        describe('with lightenHsvRaw', function() {
            it('should decrease saturation', function() {
                assert.deepEqual(Color.lightenHsvRaw([255, 15, 90], 0.1), [255, 5, 90]);
            });

            it('should decrease saturation and not check for negative result', function() {
                assert.deepEqual(Color.lightenHsvRaw([255, 15, 90], 0.2), [255, -5, 90]);
            });
        });

        describe('with lightenHsv', function() {
            it('should decrease saturation and convert value to maximum if it overflows', function() {
                assert.deepEqual(Color.lightenHsv([255, 15, 90], 0.2), [255, 0, 90]);
            });
        });

        describe('with lightenHsvWithMirror', function() {
            it('should decrease saturation and mirror negative', function() {
            // Больше тестов в ./test.mirror.js
                assert.deepEqual(Color.lightenHsvWithMirror([255, 15, 90], 0.2, 0.1), [255, 15, 90]);
            });
        });
    });
});
