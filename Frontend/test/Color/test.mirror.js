var Color = require('../../Color/Color').Color,
    mirror = Color.mirror,
    assert = require('assert');

describe('with mirror', function() {
    it('should not mirror numbers between zero and maximum', function() {
        assert.equal(mirror(4, 100, 0.1), 4);
    });

    describe('with positive numbers', function() {
        describe('with step 10', function() {
            it('should mirror number 104 to 84', function() {
                assert.equal(mirror(104, 100, 0.1), 84);
            });

            it('should mirror number 114 to 74', function() {
                assert.equal(mirror(114, 100, 0.1), 74);
            });

            it('should mirror number 124 to 64', function() {
                assert.equal(mirror(124, 100, 0.1), 64);
            });

            it('should mirror number 110 to 90', function() {
                assert.equal(mirror(110, 100, 0.1), 90);
            });

            it('should mirror number 120 to 80', function() {
                assert.equal(mirror(120, 100, 0.1), 80);
            });

            it('should mirror number 130 to 70', function() {
                assert.equal(mirror(130, 100, 0.1), 70);
            });
        });

        describe('with step 20', function() {
            it('should mirror number 104 to 64', function() {
                assert.equal(mirror(104, 100, 0.2), 64);
            });

            it('should mirror number 124 to 44', function() {
                assert.equal(mirror(124, 100, 0.2), 44);
            });

            it('should mirror number 144 to 24', function() {
                assert.equal(mirror(144, 100, 0.2), 24);
            });

            it('should mirror number 114 to 74', function() {
                assert.equal(mirror(114, 100, 0.2), 74);
            });

            it('should mirror number 134 to 54', function() {
                assert.equal(mirror(134, 100, 0.2), 54);
            });

            it('should mirror number 154 to 34', function() {
                assert.equal(mirror(154, 100, 0.2), 34);
            });

            it('should mirror number 110 to 70', function() {
                assert.equal(mirror(110, 100, 0.2), 70);
            });

            it('should mirror number 130 to 50', function() {
                assert.equal(mirror(130, 100, 0.2), 50);
            });

            it('should mirror number 150 to 30', function() {
                assert.equal(mirror(150, 100, 0.2), 30);
            });

            it('should mirror number 120 to 80', function() {
                assert.equal(mirror(120, 100, 0.2), 80);
            });

            it('should mirror number 140 to 60', function() {
                assert.equal(mirror(140, 100, 0.2), 60);
            });

            it('should mirror number 160 to 40', function() {
                assert.equal(mirror(160, 100, 0.2), 40);
            });
        });
    });

    describe('with negative numbers', function() {
        describe('with step 10', function() {
            it('should mirror number -6 to 14', function() {
                assert.equal(mirror(-6, 100, 0.1), 14);
            });

            it('should mirror number -16 to 24', function() {
                assert.equal(mirror(-16, 100, 0.1), 24);
            });

            it('should mirror number -26 to 34', function() {
                assert.equal(mirror(-26, 100, 0.1), 34);
            });

            it('should mirror number -10 to 10', function() {
                assert.equal(mirror(-10, 100, 0.1), 10);
            });

            it('should mirror number -20 to 20', function() {
                assert.equal(mirror(-20, 100, 0.1), 20);
            });

            it('should mirror number -30 to 30', function() {
                assert.equal(mirror(-30, 100, 0.1), 30);
            });
        });

        describe('with step 20', function() {
            it('should mirror number -6 to 34', function() {
                assert.equal(mirror(-6, 100, 0.2), 34);
            });

            it('should mirror number -26 to 54', function() {
                assert.equal(mirror(-26, 100, 0.2), 54);
            });

            it('should mirror number -46 to 74', function() {
                assert.equal(mirror(-46, 100, 0.2), 74);
            });

            it('should mirror number -16 to 24', function() {
                assert.equal(mirror(-16, 100, 0.2), 24);
            });

            it('should mirror number -36 to 44', function() {
                assert.equal(mirror(-36, 100, 0.2), 44);
            });

            it('should mirror number -56 to 64', function() {
                assert.equal(mirror(-56, 100, 0.2), 64);
            });

            it('should mirror number -20 to 20', function() {
                assert.equal(mirror(-20, 100, 0.2), 20);
            });

            it('should mirror number -60 to 20', function() {
                assert.equal(mirror(-40, 100, 0.2), 40);
            });

            it('should mirror number -60 to 20', function() {
                assert.equal(mirror(-60, 100, 0.2), 60);
            });

            it('should mirror number -10 to 30', function() {
                assert.equal(mirror(-10, 100, 0.2), 30);
            });

            it('should mirror number -30 to 30', function() {
                assert.equal(mirror(-30, 100, 0.2), 50);
            });

            it('should mirror number -50 to 30', function() {
                assert.equal(mirror(-50, 100, 0.2), 70);
            });
        });
    });
});
