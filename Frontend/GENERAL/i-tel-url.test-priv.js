describeBlock('i-tel-url', function(block) {
    let context;

    beforeEach(function() {
        context = { device: {} };
    });

    it('should correct build phone number url', function() {
        assert.strictEqual(block(context, '88123284161'), 'tel:88123284161');
        assert.strictEqual(block(context, '+7 812 328-41-61'), 'tel:%2B78123284161');
        assert.strictEqual(block(context, '+7-812-328-41-61'), 'tel:%2B78123284161');
    });

    it('should not cut commas that can be used for pauses', function() {
        assert.strictEqual(block(context, '+375-33-123-45-67,12'), 'tel:%2B375331234567%2C12');
        assert.strictEqual(block(context, '+375-33-123-45-67,ext.12'), 'tel:%2B375331234567%2C12');
        assert.strictEqual(block(context, '+375-33-123-45-67,12,21'), 'tel:%2B375331234567%2C12%2C21');
    });

    it('should do not encode phone number for UC Browser', function() {
        context.device.BrowserName = 'UCBrowser';

        assert.strictEqual(block(context, '+7 812 328-41-61'), 'tel:+78123284161');
        assert.strictEqual(block(context, '+7 812 328-41-61, 12'), 'tel:+78123284161,12');
    });

    every([null, 0, 100, {}], 'should return undefined if phone number is not string or falsy', function(val) {
        assert.isUndefined(block(context, val));
    });
});
