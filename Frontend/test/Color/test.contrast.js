var Color = require('../../Color/Color').Color,
    assert = require('assert');

describe('with contrast helpers', function() {
    describe('with grayscale', function() {
        var grayscale = Color.grayscale;

        it('should return correct grayscale value for black', function() {
            assert.equal(grayscale([0, 0, 0]), 0);
        });

        it('should return correct grayscale value for white', function() {
            assert.equal(grayscale([255, 255, 255]), 255);
        });

        it('should return correct grayscale value for red', function() {
            assert.equal(grayscale([255, 0, 0]), 76.245);
        });

        it('should return correct grayscale value for green', function() {
            assert.equal(grayscale([0, 255, 0]), 149.685);
        });

        it('should return correct grayscale value for blue', function() {
            assert.equal(grayscale([0, 0, 255]), 29.07);
        });
    });

    describe('with getRgbContrast', function() {
        var rgbContrast = Color.getRgbContrast;

        it('should return correct rgb contrast between black and white', function() {
            assert.equal(rgbContrast([0, 0, 0], [255, 255, 255]), 1);
        });

        it('should return correct rgb contrast between white and red', function() {
            assert.equal(rgbContrast([255, 255, 255], [255, 0, 0]), 0.701);
        });

        it('should return correct rgb contrast between white and green', function() {
            assert.equal(rgbContrast([255, 255, 255], [0, 255, 0]), 0.413);
        });

        it('should return correct rgb contrast between white and blue', function() {
            assert.equal(rgbContrast([255, 255, 255], [0, 0, 255]), 0.886);
        });

        it('should return correct rgb contrast between black and red', function() {
            assert.equal(rgbContrast([0, 0, 0], [255, 0, 0]), 0.299);
        });

        it('should return correct rgb contrast between black and green', function() {
            assert.equal(rgbContrast([0, 0, 0], [0, 255, 0]), 0.587);
        });

        it('should return correct rgb contrast between black and blue', function() {
            assert.equal(rgbContrast([0, 0, 0], [0, 0, 255]), 0.114);
        });
    });

    describe('with contrast', function() {
        var contrast = Color.contrast;

        it('should return correct contrast color for black', function() {
            assert.deepEqual(contrast([0, 0, 0]), [0, 0, 50]);
        });

        it('should return correct contrast color for white', function() {
            assert.deepEqual(contrast([0, 0, 100]), [0, 0, 50]);
        });

        it('should return correct contrast color for red', function() {
            assert.deepEqual(contrast([0, 100, 100]), [0, 50, 0]);
        });

        it('should return correct contrast color for green', function() {
            assert.deepEqual(contrast([120, 100, 100]), [120, 100, 50]);
        });

        it('should return correct contrast color for blue', function() {
            assert.deepEqual(contrast([240, 100, 100]), [240, 50, 0]);
        });
    });

    describe('with getRelativeLuminance', function() {
        var getRelativeLuminance = Color.getRelativeLuminance;

        it('should return correct relative luminance value for black', function() {
            assert.equal(getRelativeLuminance([0, 0, 0]), 0);
        });

        it('should return correct relative luminance value for white', function() {
            assert.equal(getRelativeLuminance([255, 255, 255]), 255);
        });

        it('should return correct relative luminance value for red', function() {
            assert.equal(getRelativeLuminance([255, 0, 0]), 54.213);
        });

        it('should return correct relative luminance value for green', function() {
            assert.equal(getRelativeLuminance([0, 255, 0]), 182.376);
        });

        it('should return correct relative luminance value for blue', function() {
            assert.equal(getRelativeLuminance([0, 0, 255]), 18.411);
        });
    });
});
