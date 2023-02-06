describeBlock('adapter-companies__urls', function(block) {
    let context;
    let snippet;
    let state;
    let response;
    let request;
    let org;

    stubBlocks('i-get-user-coords');

    beforeEach(function() {
        context = {
            device: {},
            expFlags: {},
            query: { text: 'query_from_context' },
            pageUrl: RequestCtx.url('https://yandex.ru/search/')
        };
        response = {
            InternalResponseInfo: {
                context: 'context'
            }
        };
        request = {
            request: 'query_from_response_metadata_object'
        };
        snippet = {
            data: {
                GeoMetaSearchData: {
                    properties: {
                        ResponseMetaData: {
                            SearchResponse: response,
                            SearchRequest: request
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
        org = {};

        blocks['i-get-user-coords'].returns([]);
    });

    describe('organisation landing url', function() {
        it('should build correct seo url for organisation landing', function() {
            const result = block(context, snippet, state, org).org;
            assert.equal(result, 'https://yandex.ru/maps/org/seoname/1/?source=wizbiz_new_map_single');
        });

        it('should build correct url for organisation landing', function() {
            state.seoname = null;
            const result = block(context, snippet, state, org).org;
            assert.include(result, 'sctx=context');
            assert.include(result, 'text=query_from_response_metadata_object');
            assert.include(result, 'll=1.000000%2C2.000000');
            assert.include(result, 'oid=1');
            assert.include(result, 'ol=biz');
        });

        it('should take query from context if request is empty', function() {
            state.seoname = null;
            request.request = false;

            const result = block(context, snippet, state, org).org;
            assert.include(result, 'text=query_from_context');
        });

        it('should return nothing if oid is not specified', function() {
            state.oid = null;

            var result = block(context, snippet, state, org).org;
            assert.notOk(result);
        });

        it('should not have sctx param in url if sctx is empty', function() {
            response.InternalResponseInfo.context = null;

            var result = block(context, snippet, state, org).org;
            assert.notInclude(result, 'sctx=');
        });
    });

    describe('site url', function() {
        it('should not change site urls with https://', function() {
            state.rawSiteUrl = 'https://url/';

            var result = block(context, snippet, state, org).site;
            assert.equal(result, 'https://url/');
        });

        it('should not change site urls with http://', function() {
            var result = block(context, snippet, state, org).site;
            assert.equal(result, 'http://url/');
        });

        it('should add protocol for site urls with //', function() {
            state.rawSiteUrl = '//url/';

            var result = block(context, snippet, state, org).site;
            assert.equal(result, 'http://url/');
        });

        it('should add protocol for site urls without protocol', function() {
            state.rawSiteUrl = 'url/';

            var result = block(context, snippet, state, org).site;
            assert.equal(result, 'http://url/');
        });
    });

    describe('url for phone number', function() {
        const phonesStubsWithPlusSign = [
            [{ formatted: '+71233212222' }],
            [{ formatted: '+7 123 3212222' }],
            [{ formatted: '+7 (123) 3212222' }],
            [{ formatted: '+7 (123) 321-22-22' }],
            [{ formatted: '+7 (123) 321 22 22' }],
            [{ formatted: '+7(123)321 22 22' }],
            [{ formatted: '+71233212222' }],
            [{ formatted: '+7 123 3212222' }],
            [{ formatted: '+7 (123) 3212222' }],
            [{ formatted: '+7 (123) 321-22-22' }],
            [{ formatted: '+7 (123) 321 22 22' }],
            [{ formatted: '+7(123)321 22 22' }]
        ];
        const phonesStubsWithoutPlusSign = [
            [{ formatted: '81233212222' }],
            [{ formatted: '8 123 3212222' }],
            [{ formatted: '8 (123) 3212222' }],
            [{ formatted: '8 (123) 321-22-22' }],
            [{ formatted: '8 (123) 321 22 22' }],
            [{ formatted: '8(123)321 22 22' }],
            [{ formatted: '81233212222' }],
            [{ formatted: '8 123 3212222' }],
            [{ formatted: '8 (123) 3212222' }],
            [{ formatted: '8 (123) 321-22-22' }],
            [{ formatted: '8 (123) 321 22 22' }],
            [{ formatted: '8(123)321 22 22' }]
        ];

        phonesStubsWithPlusSign.forEach(function(phone) {
            it('should make correct url from phone: ' + phone[0].formatted, function() {
                state.phones = phone;
                assert.equal(block(context, snippet, state, org).phone, 'tel:+71233212222');
            });
        });

        phonesStubsWithoutPlusSign.forEach(function(phone) {
            it('should make correct url from phone: ' + phone[0].formatted, function() {
                state.phones = phone;
                assert.equal(block(context, snippet, state, org).phone, 'tel:81233212222');
            });
        });
    });

    describe('route url', function() {
        it('should build correct url for route', function() {
            var result = block(context, snippet, state, org).route;
            assert.include(result, 'rtt=auto');
            assert.include(result, 'rtext=~2.000000%2C1.000000');
        });
    });

    describe('gallery url', function() {
        it('should return nothing if oid is not specified', function() {
            state.oid = null;

            var result = block(context, snippet, state, org).gallery;
            assert.notOk(result);
        });

        it('should build correct url for gallery', function() {
            var result = block(context, snippet, state, org).gallery;
            assert.include(result, 'sctx=context');
            assert.include(result, 'text=query_from_response_metadata_object');
            assert.include(result, 'll=1.000000%2C2.000000');
            assert.include(result, 'photos%5Bbusiness%5D=1');
            assert.include(result, 'oid=1');
            assert.include(result, 'ol=biz');
        });
    });

    describe('url for reviews', function() {
        it('should return nothing if oid is not specified', function() {
            state.oid = null;

            var result = block(context, snippet, state, org).reviews;
            assert.notOk(result);
        });

        it('should return nothing if seoname is not specified', function() {
            state.seoname = null;

            var result = block(context, snippet, state, org).reviews;
            assert.notOk(result);
        });

        it('should build correct url for reviews', function() {
            var result = block(context, snippet, state, org).reviews;
            assert.equal(result, 'https://yandex.ru/maps/org/seoname/1/?reviews');
        });
    });
});
