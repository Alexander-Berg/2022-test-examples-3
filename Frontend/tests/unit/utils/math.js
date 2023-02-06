const { round } = require('../../../src/client/components/utils/math');

describe('utils/math', () => {
    describe('round', () => {
        it('should return a Number', () => {
            assert(typeof round(1.25, 1) === 'number');
        });

        it('should round properly', function() {
            assert.equal(round(1.25, 1), 1.2);
            assert.equal(round(1.24, 1), 1.2);
            assert.equal(round(1.26, 1), 1.3);
            assert.equal(round(1.35, 1), 1.4);
            assert.equal(round(1.34, 1), 1.3);
            assert.equal(round(1.36, 1), 1.4);
            assert.equal(round(0, 1), 0);

            assert.equal(round(1.2, 1), 1.2);
            assert.equal(round(1.4, 2), 1.4);
            assert.equal(round(1.5, 2), 1.5);

            assert.equal(round(1.4, 0), 1);
            assert.equal(round(1.5, 0), 2);
            assert.equal(round(2.5, 0), 2);
            assert.equal(round(1.5, 0), 2);
            assert.equal(round(1.6, 0), 2);
            assert.equal(round(1.3, 0), 1);

            assert.equal(round(55.25, 0), 55);
            assert.equal(round(44.75, 0), 45);

            assert.equal(round(54.5, 0), 54);
            assert.equal(round(45.5, 0), 46);
        });
    });
});
