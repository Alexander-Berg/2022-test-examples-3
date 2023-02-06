describeBlock('misspell_type_region', function(block) {
    let data;
    let wzrd;
    let result;

    stubBlocks('RequestCtx');

    beforeEach(function() {
        data = stubData('cgi', 'counters', 'region', 'experiments', 'i-log', 'user-time');
        data.wizplaces = [];
        wzrd = {
            type: 'region',
            empty_request_region: true,
            current_region: {
                tld: 'foo',
                name: {
                    ru: {
                        nominative: 'Иннополис',
                        locative: 'Иннополисе',
                        genitive: 'Иннополиса'
                    }
                }
            },
            counter_prefix: '/reask/results_for_region'
        };
    });

    it('replaces tld with yandex_tld', function() {
        RequestCtx.GlobalContext.cgi.hostname.returns('example.com.tr');
        block(data, wzrd);
        assert.calledWith(RequestCtx.GlobalContext.cgi.setHostname, 'example.foo');
    });

    it('returns correct message when region is unknown', function() {
        result = _(block(data, wzrd).content).find({ elem: 'message' }).content;
        assert.equal(result, 'При поиске не учитывается регион.');
    });
});
