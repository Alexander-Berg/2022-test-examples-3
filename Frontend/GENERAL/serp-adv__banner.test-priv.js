describeBlock('serp-adv__banner', function(block) {
    var context;

    stubBlocks('Util', 'bemhtml-apply');

    beforeEach(function() {
        context = {
            reportData: {
                ...stubData('experiments', 'counters', 'direct', 'device'),
                config: { staticOrigin: {} },
                banner: {
                    data: {
                        media_anticontext: [1],
                        media_context: [{ flash: true }]
                    }
                }
            },

            // GlobalContext
            prefsBanners: true,
            device: { hasFlash: true }
        };
    });

    describe('with flash banner', function() {
        var result;

        beforeEach(function() {
            result = block(context);
        });

        it('should return BEMJSON', function() {
            assert.isObject(result);
        });

        it('should return object', function() {
            assert.isObject(result.content);
        });

        it('should be js params', function() {
            assert(result.js);
        });
    });
});
