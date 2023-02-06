var Color = require('../../Color/Color').Color,
    assert = require('assert');

describe('with hex helpers', function() {
    describe('with normalizeHex', function() {
        it('should normalize fce to ffccee', function() {
            assert.equal(Color.normalizeHex('fce'), 'ffccee');
        });

        it('should normalize FCE to ffccee', function() {
            assert.equal(Color.normalizeHex('fce'), 'ffccee');
        });

        it('should normalize FFCCEE to ffccee', function() {
            assert.equal(Color.normalizeHex('FFCCEE'), 'ffccee');
        });

        it('should not normalize ffccee', function() {
            assert.equal(Color.normalizeHex('ffccee'), 'ffccee');
        });
    });

    describe('with isHex', function() {
        it('should check fce to be hex', function() {
            assert.equal(Color.isHex('fce'), true);
        });

        it('should check ffccee to be hex', function() {
            assert.equal(Color.isHex('ffccee'), true);
        });

        it('should check fccee not to be hex', function() {
            assert.equal(Color.isHex('fccee'), false);
        });

        it('should check fccee not to be hex', function() {
            assert.equal(Color.isHex('ffcceg'), false);
        });
    });

    describe('with isHexDifferent', function() {
        it('should consider ffccee and fce equal', function() {
            assert.equal(Color.isHexDifferent('ffccee', 'fce'), false);
        });

        it('should consider ffccee and ffccee equal', function() {
            assert.equal(Color.isHexDifferent('ffccee', 'ffccee'), false);
        });

        it('should consider ffccee and ffccef different', function() {
            assert.equal(Color.isHexDifferent('ffccee', 'ffccef'), true);
        });

        it('should consider ffccee and fcf different', function() {
            assert.equal(Color.isHexDifferent('ffccee', 'fcf'), true);
        });
    });
});
