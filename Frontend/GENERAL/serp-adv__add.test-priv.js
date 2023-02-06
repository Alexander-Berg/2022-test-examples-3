describeBlock('serp-adv__add-to-not-show', function(block) {
    var data, result;

    stubBlocks('RequestCtx');

    beforeEach(function() {
        data = stubData('direct', 'navi', 'counters');

        data.app_host = {
            word_stat: {
                data: 42
            }
        };
        data.searchdata = { numdocs: 50 };
        data.search = {
            text: { word_count: 123 },
            is_porno_query: 0
        };
    });

    it('should return false for optimistic case ("good" data set in `beforeEach` above)', function() {
        result = block(data);

        assert.isFalse(result);
    });

    it('should return true for .com and .eu domains', function() {
        ['com', 'eu'].forEach(function(tld) {
            RequestCtx.GlobalContext.tld = tld;
            result = block(data);
            assert.isTrue(result);
        });
    });

    every(['ru', 'ua', 'kz', 'by', 'com.tr'], 'should return false for KUBR and Turkish domains', function(tld) {
        RequestCtx.GlobalContext.tld = tld;
        RequestCtx.GlobalContext.isComTr = tld === 'com.tr';

        result = block(data);

        assert.isFalse(result);
    });
});

describeBlock('serp-adv__add', function(block) {
    var context, result;

    beforeEach(function() {
        sinon.stub(blocks, 'serp-adv__add-to-not-show').returns(false);
        context = { reportData: stubData('counters', 'direct', 'i18n', 'prefs', 'region', 'cgi') };
        context.log = context.reportData.log;
    });

    afterEach(function() {
        blocks['serp-adv__add-to-not-show'].restore();
    });

    it('should return BEMJSON', function() {
        result = block(context);

        assert.isObject(result.content);
    });

    it('should return undefined if some condition to not show is truthy', function() {
        blocks['serp-adv__add-to-not-show'].returns(true);

        result = block(context);

        assert.isUndefined(result);
    });
});
