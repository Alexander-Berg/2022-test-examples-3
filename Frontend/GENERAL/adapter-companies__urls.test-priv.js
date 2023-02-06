describeBlock('adapter-companies__urls', function(block) {
    let context;
    let snippet;
    let state;
    let response;
    let service;

    stubBlocks('i-get-user-coords');

    beforeEach(function() {
        context = {
            device: {},
            expFlags: {},
            reportData: {
                isSearchApp: false
            },
            query: { xmlEscaped: 'query_from_context' },
            pageUrl: RequestCtx.url('https://yandex.ru/search/')
        };
        response = {
            request: 'query_from_response_object',
            InternalResponseInfo: {
                context: 'context'
            }
        };
        snippet = {
            data: {
                GeoMetaSearchData: {
                    properties: {
                        ResponseMetaData: {
                            SearchResponse: response
                        }
                    }
                }
            }
        };
        state = {
            oid: 1,
            type: 'business',
            rawSiteUrl: 'http://url/',
            seoname: 'seoname',
            phones: [],
            rtext: '~2.000000,1.000000',
            coordinates: [1, 2]
        };

        service = sinon.stub(RequestCtx.Service, 'service').returns({ root: 'maps.yandex.ru' });
        blocks['i-get-user-coords'].returns([]);
    });

    afterEach(function() {
        service.restore();
    });

    describe('urls if travel source', function() {
        let org;

        beforeEach(function() {
            org = {};
        });

        it('should get correct route urls', function() {
            const result = block(context, snippet, state, org).route;
            assert.include(result, '&rtext=~2.000000%2C1.000000&ruri=~ymapsbm1%3A%2F%2Forg%3Foid%3D1&rtt=auto');
        });
    });
});

describeBlock('adapter-companies__travel-hotel-link', function(block) {
    const reqid = 'test-reqid-123';
    const travelHost = 'travel.yandex.ru';
    let context;
    let state;

    beforeEach(function() {
        context = {
            reportData: {
                reqdata: {
                    reqid
                }
            }
        };

        state = {
            oid: 1
        };
    });

    describe('hotel url', function() {
        const tlds = ['ru', 'kz', 'ua', 'by', 'com.tr', 'com', 'com.am', 'fr'];

        tlds.forEach(tld => {
            it(`should return .ru for ${tld} domain`, function() {
                context.tld = tld;
                const result = block(context, { oid: state.oid });
                assert.include(result, travelHost);
            });
        });

        it('should pass serpReqId', function() {
            const result = block(context, { oid: state.oid });
            assert.include(result, `serpReqId=${reqid}`);
        });

        it('should pass hotelPermalink', function() {
            const result = block(context, { oid: state.oid });
            assert.include(result, `hotelPermalink=${state.oid}`);
        });
    });
});

describeBlock('adapter-companies__yandex-travel-url', function(block) {
    const reqid = 'test-reqid-123';
    const travelHost = 'travel.yandex.ru';
    let context;

    beforeEach(function() {
        context = {
            reportData: {
                reqdata: {
                    reqid
                }
            }
        };
    });

    describe('travel url', function() {
        const tlds = ['ru', 'kz', 'ua', 'by', 'com.tr', 'com', 'com.am', 'fr'];

        tlds.forEach(tld => {
            it(`should return .ru for ${tld} domain`, function() {
                context.tld = tld;
                const result = block(context, { query: 'query' }).url();
                assert.include(result, travelHost);
            });
        });

        it('should pass serpReqId', function() {
            const result = block(context, { query: 'query' }).url();
            assert.include(result, `serpReqId=${reqid}`);
        });

        it('should pass searchText', function() {
            const query = 'query';
            const result = block(context, { query }).url();
            assert.include(result, `searchText=${query}`);
        });
    });
});

describeBlock('adapter-companies__serp-org-url', function(block) {
    let context;
    let state;
    let similarItem;

    beforeEach(function() {
        context = stubData('experiments');
        context.pageUrl = RequestCtx.url('https://yandex.ru/search/?text=some_search_text&lr=2&oid=111');
        state = { yandexTravelInfo: {} };
        similarItem = {
            logId: 'similar_item_oid',
            name: 'item_name',
            address: 'item_address'
        };
    });

    it('should contain similar company oid', function() {
        let result = block(context, state, similarItem);
        assert.include(result, 'similar_item_oid');
    });
});
