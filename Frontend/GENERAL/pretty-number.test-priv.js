describeBlock('pretty-number', function(block) {
    it('should return self value if number < 1000', function() {
        assert.equal(block(999), '999');
    });

    it('should return thousands count if number >= 1000', function() {
        assert.equal(block(12945), '12 тыс.');
    });

    it('should return millions count if number >= 1 000 000', function() {
        assert.equal(block(12945678), '12 млн');
    });

    it('should return billions count if number >= 1 000 000 000', function() {
        assert.equal(block(42945678900), '42 млрд');
    });
});
