describeBlock('adapter-snip-videothumb__video-thumb-domain', function(block) {
    var context;

    beforeEach(function() {
        context = {};
    });

    it('should return ru for ru domain', function() {
        context.tld = 'ru';
        assert.strictEqual(block(context), 'ru');
    });

    it('should return kz for kz domain', function() {
        context.tld = 'kz';
        assert.strictEqual(block(context), 'kz');
    });

    it('should return ua for ua domain', function() {
        context.tld = 'ua';
        assert.strictEqual(block(context), 'ua');
    });

    it('should return by for by domain', function() {
        context.tld = 'by';
        assert.strictEqual(block(context), 'by');
    });

    it('should return tr for com.tr domain', function() {
        context.tld = 'com.tr';
        assert.strictEqual(block(context), 'tr');
    });

    it('should return com for com domain', function() {
        context.tld = 'com';
        assert.strictEqual(block(context), 'com');
    });

    it('should return com for com.am domain', function() {
        context.tld = 'com.am';
        assert.strictEqual(block(context), 'com');
    });

    it('should return com for fr domain', function() {
        context.tld = 'fr';
        assert.strictEqual(block(context), 'com');
    });
});

describeBlock('adapter-snip-videothumb', function(block) {
    var context, snippet;

    stubBlocks('adapter-snip-videothumb__text');
    stubBlocks('adapter-snip-videothumb__video-thumb');

    beforeEach(function() {
        snippet = {};
    });

    it('should not return videothumb if there are not thid and thmb_href', function() {
        assert.isUndefined(block(context, snippet));
    });

    it('should return videothumb if there is thid', function() {
        snippet.thid = '21bb03';
        assert.isObject(block(context, snippet));
    });

    it('should return videothumb if there is thmb_href', function() {
        snippet.thid = 'https://avatars.mds';
        assert.isObject(block(context, snippet));
    });
});

describeBlock('adapter-snip-videothumb__url', function(block) {
    let snippet;
    let doc = { url: 'source' };

    it('should return link to video source', function() {
        snippet = { original_url: 'source' };
        assert.strictEqual(block(undefined, snippet), 'source');
    });

    it('should return doc.url if there is no url in snippet', function() {
        snippet = {};
        assert.strictEqual(block(undefined, snippet, doc), 'source');
    });
});
