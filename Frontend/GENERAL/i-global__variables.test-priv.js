describeBlock('i-global__parse-ajax', function(block) {
    it('should return undefined if empty data given', function() {
        assert.isUndefined(block());
    });

    it('should return undefined if invalid data given', function() {
        assert.isUndefined(block('test'));
    });

    it('should return number value if stringified number given', function() {
        assert.equal(block('1'), 1);
    });

    it('should return object if valid JSON given', function() {
        var result = block(JSON.stringify({ text: 'test' }));

        assert.isObject(result);
        assert.equal(result.text, 'test');
    });
});

describeBlock('i-global__variables', block => {
    let data;
    let glob;

    beforeEach(() => {
        glob = stubGlobal('RequestCtx');
        data = { reqdata: { headers: {}, prefs: {} }, cgidata: { args: {} }, expFlags: {}, config: {} };
        RequestCtx.GlobalContext.cgi.host = () => {};
        RequestCtx.GlobalContext.expFlags = stubData('experiments');
    });

    afterEach(() => {
        glob.restore();
    });

    it('shouldn\'t be a pre-render', () => {
        block(data);
        assert.equal(data.isPrerender, false);
    });

    it('should be a pre-render with browser header Yandex-Preload=prerender', () => {
        data.reqdata.headers['Yandex-Preload'] = 'prerender';
        block(data);
        assert.equal(data.isPrerender, true);
    });

    it('should be a pre-render with browser header yandex-preload=prerender', () => {
        data.reqdata.headers['yandex-preload'] = 'prerender';
        block(data);
        assert.equal(data.isPrerender, true);
    });

    it('should be a pre-render with cgi parameter prefetch=1', () => {
        data.cgidata.args.prefetch = 1;
        block(data);
        assert.equal(data.isPrerender, true);
    });
});
