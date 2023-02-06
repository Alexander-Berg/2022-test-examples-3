var Color = require('../../Color/Color').Color,
    converter = Color.preciseConverter,
    assert = require('assert');

describe('with precise converter', function() {
    describe('with rgb', function() {
        it('should convert rgb to hsl', function() {
            assert.deepEqual(
                converter.rgb2hsl([105, 17, 170]),
                [274.5098, 81.8182, 36.6667]
            );
        });

        it('should convert rgb to hsv', function() {
            assert.deepEqual(
                converter.rgb2hsv([105, 17, 170]),
                [274.5098, 90, 66.6667]
            );
        });

        it('should convert rgb to hex', function() {
            assert.equal(
                converter.rgb2hex([105, 17, 170]),
                '6911aa'
            );
        });
    });

    describe('with hsl', function() {
        it('should convert hsl to rgb', function() {
            assert.deepEqual(
                converter.hsl2rgb([275, 82, 37]),
                [107.2445, 16.983, 171.717]
            );
        });

        it('should convert hsl to hsv', function() {
            assert.deepEqual(
                converter.hsl2hsv([275, 82, 37]),
                [275, 90.1099, 67.34]
            );
        });

        it('should convert hsl to hex', function() {
            assert.equal(
                converter.hsl2hex([275, 82, 37]),
                '6b11ac'
            );
        });
    });

    describe('with hsv', function() {
        it('should convert hsv to rgb', function() {
            assert.deepEqual(
                converter.hsv2rgb([275, 90, 67]),
                [106.7812, 17.085, 170.85]
            );
        });

        it('should convert hsv to hsl', function() {
            assert.deepEqual(
                converter.hsv2hsl([275, 90, 67]),
                [275, 81.8182, 36.85]
            );
        });

        it('should convert hsv to hex', function() {
            assert.equal(
                converter.hsv2hex([275, 90, 67]),
                '6b11ab'
            );
        });
    });

    describe('with hex', function() {
        it('should convert hex to rgb', function() {
            assert.deepEqual(
                converter.hex2rgb('6911aa'),
                [105, 17, 170]
            );
        });

        it('should convert hex to hsl', function() {
            assert.deepEqual(
                converter.hex2hsl('6911aa'),
                [274.5098, 81.8182, 36.6667]
            );
        });

        it('should convert hex to hsv', function() {
            assert.deepEqual(
                converter.hex2hsv('6911aa'),
                [274.5098, 90, 66.6667]
            );
        });
    });
});
