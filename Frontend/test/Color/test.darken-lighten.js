var Color = require('../../Color/Color').Color,
    assert = require('assert');

describe('with darken', function() {
    describe('with darken', function() {
        it('should decrease hsl lightness of hsv value and convert negative value to zero', function() {
            assert.deepEqual(Color.darken([255, 95, 15], 0.1), [255, 0, 0]);
        });
    });

    describe('with darkenWithMirror', function() {
        // Больше тестов в ./test.mirror.js
        it('should decrease hsl lightness of hsv value and mirror negative value', function() {
            assert.deepEqual(Color.darkenWithMirror([255, 95, 15], 0.1, 0.1), [255, 95, 34]);
        });
    });
});

describe('with lighten', function() {
    describe('with lighten', function() {
        it('should increase hsl lightness of hsv value and convert value to maximum if it overflows', function() {
            assert.deepEqual(Color.lighten([255, 5, 95], 0.1), [255, 0, 100]);
        });
    });

    describe('with lightenWithMirror', function() {
        it('should increase hsl lightness of hsv value and mirror value if it overflows', function() {
            // Больше тестов в ./test.mirror.js
            assert.deepEqual(Color.lightenWithMirror([255, 5, 95], 0.1, 0.1), [255, 12, 88]);
        });
    });
});
