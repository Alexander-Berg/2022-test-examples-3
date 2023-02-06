// Перенести на уровень deskpad, как только БК докатит новые данные на touch-pad SERP-55800
describeBlock('serp-adv__all', function(block) {
    var context, result;

    beforeEach(function() {
        context = {
            tld: 'ru',
            query: { text: 'test' },
            reportData: stubData('counters', 'direct', 'experiments', 'cgi')
        };
        context.log = context.reportData.log;

        _.set(context, 'reportData.banner.data.stat.0.direct_showall', 10);
    });

    every(['com.tr', 'com'], 'should return undefined for .com and com.tr domain', function(tld) {
        context.tld = tld;

        result = block(context);

        assert.isUndefined(result);
    });

    every(['ru', 'ua', 'kz', 'by'], 'should return BEMJSON for KUBR', function(tld) {
        context.tld = tld;

        result = block(context);

        assert.isObject(result);
    });

    it('should return undefined if no adv count in data', function() {
        _.set(context, 'reportData.banner.data.stat.0.direct_showall', 0);

        result = block(context);

        assert.isUndefined(result);
    });
});
