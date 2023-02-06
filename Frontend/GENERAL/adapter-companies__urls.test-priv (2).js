describeBlock('adapter-companies__urls', function(block) {
    let context;
    let snippet;
    let state;
    let org;
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
            coordinates: [1, 2]
        };
        org = {};

        service = sinon.stub(RequestCtx.Service, 'service').returns({ root: 'maps.yandex.ru' });
        blocks['i-get-user-coords'].returns([]);
    });

    afterEach(function() {
        service.restore();
    });

    describe('url for reviews', function() {
        it('should return nothing if oid is not specified', function() {
            state.oid = null;

            const result = block(context, snippet, state, org).reviews;

            assert.notOk(result);
        });

        it('should return nothing if seoname is not specified', function() {
            state.seoname = null;

            const result = block(context, snippet, state, org).reviews;

            assert.notOk(result);
        });
    });
});
