describeBlock('adapter-recommendation-label', function(block) {
    const context = {};
    const snippet = {};

    it('should not crash', function() {
        assert.equal(block(context, snippet), undefined);
    });
});
