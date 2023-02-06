var Color = require('../../Color/Color').Color,
    assert = require('assert');

describe('with hsl helpers', function() {
    describe('with darken helpers', function() {
        describe('with darkenHslRaw', function() {
            it('should decrease lightness', function() {
                assert.deepEqual(Color.darkenHslRaw([255, 90, 15], 0.1), [255, 90, 5]);
            });

            it('should decrease lightness and not check for negative result', function() {
                assert.deepEqual(Color.darkenHslRaw([255, 90, 15], 0.2), [255, 90, -5]);
            });
        });

        describe('with darkenHsl', function() {
            it('should decrease lightness and convert negative value to zero', function() {
                assert.deepEqual(Color.darkenHsl([255, 90, 15], 0.2), [255, 90, 0]);
            });
        });

        describe('with darkenHslWithMirror', function() {
            // Больше тестов в ./test.mirror.js
            it('should decrease lightness and mirror negative value', function() {
                assert.deepEqual(Color.darkenHslWithMirror([255, 90, 15], 0.2, 0.1), [255, 90, 15]);
            });
        });
    });

    describe('with lighten helpers', function() {
        describe('with lightenHslRaw', function() {
            it('should increase lightness', function() {
                assert.deepEqual(Color.lightenHslRaw([255, 90, 85], 0.1), [255, 90, 95]);
            });

            it('should increase lightness and not check for maximum overflow', function() {
                assert.deepEqual(Color.lightenHslRaw([255, 90, 85], 0.2), [255, 90, 105]);
            });
        });

        describe('with lightenHsl', function() {
            it('should increase lightness and convert value to maximum if it overflows', function() {
                assert.deepEqual(Color.lightenHsl([255, 90, 85], 0.2), [255, 90, 100]);
            });
        });

        describe('with lightenHslWithMirror', function() {
            it('should increase lightness and mirror value if it overflows', function() {
                // Больше тестов в ./test.mirror.js
                assert.deepEqual(Color.lightenHslWithMirror([255, 90, 85], 0.2, 0.1), [255, 90, 85]);
            });
        });
    });
});
