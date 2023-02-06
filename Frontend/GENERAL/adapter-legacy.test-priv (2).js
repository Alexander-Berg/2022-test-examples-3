describeBlock('adapter-legacy__extralinks', function(block) {
    let glob;

    beforeEach(() => {
        glob = stubGlobal('RequestCtx');
    });

    afterEach(() => {
        glob.restore();
    });

    it('should not add "copy" variant', function() {
        const doc = {
            signed_saved_copy_url: 'blah-blah-blah',
            mime_is_convertible: true
        };
        const data = { expFlags: {} };
        const actual = block(data, doc);

        assert.isUndefined(actual);
    });

    it('should add "copy" variant', function() {
        const mockUrl = 'www.example.com';
        const doc = {
            signed_saved_copy_url: mockUrl
        };
        const data = { expFlags: {} };

        const actual = block(data, doc);

        assert.lengthOf(actual, 1);
        assert.propertyVal(actual[0], 'url', mockUrl);
        assert.propertyVal(actual[0], 'variant', 'copy');
        assert.nestedPropertyVal(actual[0], 'counter.logNodeData.name', 'safedcopy');
    });

    it('should not add "more" variant when doc has no host', function() {
        const doc = {
            noMoreFromSite: false
        };
        const data = { expFlags: {} };
        const actual = block(data, doc);

        assert.isUndefined(actual);
    });

    it('should not add "more" variant when we have site host but already searching on site', function() {
        const siteHost = 'www.example.com';
        RequestCtx.GlobalContext.query = { text: `site:${siteHost} pizza` };

        const doc = {
            noMoreFromSite: false,
            host: siteHost
        };
        const data = { query: { text: `site:${siteHost} pizza` }, expFlags: {} };
        const actual = block(data, doc);

        assert.isUndefined(actual);
    });

    it('should add "more" variant on incorrect punycode input', function() {
        RequestCtx.GlobalContext.query = { text: 'testquery' };
        const doc = {
            noMoreFromSite: false,
            host: 'xn--222.apkcafe.ru'
        };

        const data = { query: { text: 'testquery' }, expFlags: {}, reqdata: {} };
        const actual = block(data, doc);

        assert.lengthOf(actual, 1);
        assert.propertyVal(actual[0], 'variant', 'more');
    });

    it('should add "more" variant', function() {
        RequestCtx.GlobalContext.query = { text: 'pizza' };
        const doc = {
            noMoreFromSite: false,
            host: 'www.example.com'
        };

        const data = { query: { text: 'pizza' }, expFlags: {}, reqdata: {} };
        const actual = block(data, doc);

        assert.lengthOf(actual, 1);
        assert.propertyVal(actual[0], 'variant', 'more');
        assert.propertyVal(actual[0], 'url', '');
        assert.propertyVal(actual[0], 'target', '_self');
        assert.nestedPropertyVal(actual[0], 'counter.redir', false);
        assert.nestedPropertyVal(actual[0], 'counter.token', 'sitemore');
    });

    it('should add "feedback" variant', function() {
        const doc = {};
        const data = {
            expFlags: {
                abuse_report: true
            }
        };

        const actual = block(data, doc);

        assert.lengthOf(actual, 1);
        assert.propertyVal(actual[0], 'variant', 'feedback');
        assert.nestedPropertyVal(actual[0], 'counter.logNodeData.name', 'abuse');
        assert.nestedPropertyVal(actual[0], 'counter.logNodeData.attrs.behaviour.type', 'dynamic');
    });

    it('should return two variants', function() {
        RequestCtx.GlobalContext.query = { text: 'pizza' };
        const doc = {
            signed_saved_copy_url: 'blah-blah-blah',
            noMoreFromSite: false,
            host: 'www.example.com'
        };
        const data = { query: { text: 'pizza' }, expFlags: {}, reqdata: {} };

        const actual = block(data, doc);

        assert.lengthOf(actual, 2);
        assert.propertyVal(actual[0], 'variant', 'copy');
        assert.propertyVal(actual[1], 'variant', 'more');
    });

    it('should return three variants', function() {
        RequestCtx.GlobalContext.query = { text: 'pizza' };
        const doc = {
            signed_saved_copy_url: 'blah-blah-blah',
            noMoreFromSite: false,
            host: 'www.example.com'
        };
        const data = {
            query: { text: 'pizza' },
            expFlags: {
                abuse_report: true
            },
            reqdata: {}
        };

        const actual = block(data, doc);

        assert.lengthOf(actual, 3);
        assert.propertyVal(actual[0], 'variant', 'copy');
        assert.propertyVal(actual[1], 'variant', 'more');
        assert.propertyVal(actual[2], 'variant', 'feedback');
    });

    it('should return undefined by default', function() {
        const doc = {};
        const data = { expFlags: {} };

        const actual = block(data, doc);

        assert.isUndefined(actual);
    });
});
