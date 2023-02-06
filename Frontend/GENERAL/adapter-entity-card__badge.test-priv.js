describeBlock('adapter-entity-card__badge-gallery', function(block) {
    var context, snpData;

    stubBlocks([
        'adapter-entity-card__gallery-is-present',
        'adapter-entity-card__gallery-dataset'
    ]);

    beforeEach(function() {
        context = {};
        snpData = {};
        blocks['adapter-entity-card__gallery-is-present'].returns(true);
        blocks['adapter-entity-card__gallery-dataset'].returns({ main: [{}], items: [{}] });
    });

    describe('should return undefined', function() {
        it('if items is empty', function() {
            blocks['adapter-entity-card__gallery-dataset'].returns({ main: [{}], items: [] });

            assert.isUndefined(block(context, snpData));
        });

        it('if gallery is not present', function() {
            blocks['adapter-entity-card__gallery-is-present'].returns(false);

            assert.isUndefined(block(context, snpData));
        });

        it('if gallery dataset is falsy', function() {
            blocks['adapter-entity-card__gallery-dataset'].returns(null);

            assert.isUndefined(block(context, snpData));
        });
    });

    it('should return gallery with removed main', function() {
        blocks['adapter-entity-card__gallery-dataset'].returns({ main: [{}], items: [{}] });

        assert.deepEqual(block(context, snpData), { items: [{}] });
    });
});
