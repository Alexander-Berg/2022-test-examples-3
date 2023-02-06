describeBlock('rum__yabrowser-custom-marks', function(block) {
    stubBlocks('RequestCtx');

    it('should return client code for Android devices with supported version of YaBro', function() {
        RequestCtx.GlobalContext.device = { OSFamily: 'Android', BrowserVersionRaw: '19.7.0.1234' };
        assert.ok(block());

        RequestCtx.GlobalContext.device = { OSFamily: 'Android', BrowserVersionRaw: '19.10.0.1234' };
        assert.ok(block());

        RequestCtx.GlobalContext.device = { OSFamily: 'Android', BrowserVersionRaw: '20.1.0.1234' };
        assert.ok(block());
    });

    it('should not return client code for non-Android devices', function() {
        RequestCtx.GlobalContext.device = { OSFamily: 'iOS' };
        assert.notOk(block());
    });

    it('should not return client code for Android devices with unsupported version of YaBro', function() {
        RequestCtx.GlobalContext.device = { OSFamily: 'Android', BrowserVersionRaw: '18.10.0.1234' };
        assert.notOk(block());

        RequestCtx.GlobalContext.device = { OSFamily: 'Android', BrowserVersionRaw: '19.9.0.1234' };
        assert.notOk(block());
    });
});
