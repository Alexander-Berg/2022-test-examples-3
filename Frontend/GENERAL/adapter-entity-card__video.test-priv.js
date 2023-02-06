describeBlock('adapter-entity-card__video-thumb-player', function(block) {
    let context;
    let snippet;

    stubBlocks([
        'i-entity-helpers__thumb-player-logo',
        'i-format-duration',
        'adapter-entity-legal__counter',
        'i-entity-helpers__get-subscription'
    ]);

    beforeEach(function() {
        context = { expFlags: {} };
        snippet = {};
    });

    it('should exist TV-series video-thumb if not defined seasons', function() {
        snippet = {
            data: {
                rich_info: {
                    vh_meta: {
                        content_groups: [{
                            content_type: 'TV_SERIES'
                        }]
                    }
                }
            }
        };
        const thumb = {};

        assert.isDefined(block(context, snippet.data, thumb));
    });
});
