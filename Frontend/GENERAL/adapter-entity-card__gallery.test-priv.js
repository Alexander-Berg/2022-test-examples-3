describeBlock('adapter-entity-card__gallery-dataset', function(block) {
    let context;
    let snippet;

    stubBlocks([
        'adapter-entity-card__gallery-main-thumb',
        'adapter-entity-card__gallery-viewer2',
        'adapter-entity-card__video'
    ]);

    beforeEach(function() {
        context = {
            expFlags: {}
        };
        snippet = {
            base_info: {
                image: {}
            }
        };
    });

    it('should return gallery data with video-thumb is undefined', function() {
        blocks['adapter-entity-card__video'].returns(undefined);

        assert.isDefined(block(context, snippet));
    });
});
