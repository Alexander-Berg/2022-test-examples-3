describeBlock('adapter-social-snippet__stats-parse', function(block) {
    var count = {
            some: 'some',
            oneThou: 'oneThou',
            thou: 'thou',
            million: 'million'
        },
        handler = function() {
            return count;
        };

    it('should return field count.million', function() {
        assert.equal(block(2e6, handler), count.million);
    });

    it('should return field count.oneThou', function() {
        assert.equal(block(1e3, handler), count.oneThou);
    });

    it('should return field count.thou', function() {
        assert.equal(block(2e3, handler), count.thou);
    });

    it('should return field count.some', function() {
        assert.equal(block(5, handler), count.some);
    });
});
