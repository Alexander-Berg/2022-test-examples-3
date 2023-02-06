var Color = require('../../Color/Color').Color,
    converter = Color.converter,
    getConverterTo = Color.getConverterTo,
    assert = require('assert'),
    _ = require('lodash');

describe('with converter', function() {
    describe('with rgb', function() {
        it('should convert rgb to hsl', function() {
            assert.deepEqual(
                converter.rgb2hsl([105, 17, 170]),
                [275, 82, 37]
            );
        });

        it('should convert rgb to hsv', function() {
            assert.deepEqual(
                converter.rgb2hsv([105, 17, 170]),
                [275, 90, 67]
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
                [107, 17, 172]
            );
        });

        it('should convert hsl to hsv', function() {
            assert.deepEqual(
                converter.hsl2hsv([275, 82, 37]),
                [275, 90, 67]
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
                [107, 17, 171]
            );
        });

        it('should convert hsv to hsl', function() {
            assert.deepEqual(
                converter.hsv2hsl([275, 90, 67]),
                [275, 82, 37]
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
                [275, 82, 37]
            );
        });

        it('should convert hex to hsv', function() {
            assert.deepEqual(
                converter.hex2hsv('6911aa'),
                [275, 90, 67]
            );
        });
    });
});

describe('with getConverterTo', function() {
    describe('with rgb color', function() {
        var getToRgbConverter = _.partial(getConverterTo, 'rgb');

        it('should retreive hsl to rgb converter', function() {
            assert.equal(getToRgbConverter({ isHSL: true }), converter.hsl2rgb);
        });

        it('should retreive hsv to rgb converter', function() {
            assert.equal(getToRgbConverter({ isHSV: true }), converter.hsv2rgb);
        });

        it('should retreive hex to rgb converter', function() {
            assert.equal(getToRgbConverter({ isHEX: true }), converter.hex2rgb);
        });
    });

    describe('with hsl color', function() {
        var getToHslConverter = _.partial(getConverterTo, 'hsl');

        it('should retreive rgb to hsl converter', function() {
            assert.equal(getToHslConverter({ isRGB: true }), converter.rgb2hsl);
        });

        it('should retreive hsv to hsl converter', function() {
            assert.equal(getToHslConverter({ isHSV: true }), converter.hsv2hsl);
        });

        it('should retreive hex to hsl converter', function() {
            assert.equal(getToHslConverter({ isHEX: true }), converter.hex2hsl);
        });
    });

    describe('with hsv color', function() {
        var getToHsvConverter = _.partial(getConverterTo, 'hsv');

        it('should retreive rgb to hsv converter', function() {
            assert.equal(getToHsvConverter({ isRGB: true }), converter.rgb2hsv);
        });

        it('should retreive hsl to hsv converter', function() {
            assert.equal(getToHsvConverter({ isHSL: true }), converter.hsl2hsv);
        });

        it('should retreive hex to hsv converter', function() {
            assert.equal(getToHsvConverter({ isHEX: true }), converter.hex2hsv);
        });
    });

    describe('with hex color', function() {
        var getToHexConverter = _.partial(getConverterTo, 'hex');

        it('should retreive rgb to hex converter', function() {
            assert.equal(getToHexConverter({ isRGB: true }), converter.rgb2hex);
        });

        it('should retreive hsl to hex converter', function() {
            assert.equal(getToHexConverter({ isHSL: true }), converter.hsl2hex);
        });

        it('should retreive hsv to hex converter', function() {
            assert.equal(getToHexConverter({ isHSV: true }), converter.hsv2hex);
        });
    });
});
