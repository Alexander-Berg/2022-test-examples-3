describeBlock('adapter-entity-card__is-last', function(block) {
    var context, docsRight;

    beforeEach(function() {
        context = { reportData: { searchdata: { docs_right: [] } } };
    });

    it('should return true if doc is last media card', function() {
        docsRight = [
            { id: 1, markers: { MediaCard: '1' } },
            { id: 2, markers: { MediaCard: '1' } },
            { id: 3, markers: {} }
        ];
        context.reportData.searchdata.docs_right = docsRight;

        assert.isTrue(block(context, docsRight[1]));
    });

    it('should return false if doc is NOT last media card', function() {
        docsRight = [
            { id: 1, markers: { MediaCard: '1' } },
            { id: 2, markers: { MediaCard: '1' } },
            { id: 3, markers: { MediaCard: '1' } }
        ];
        context.reportData.searchdata.docs_right = docsRight;

        assert.isFalse(block(context, docsRight[1]));
    });

    it('should return true if doc is last "entity_search"', function() {
        docsRight = [
            { id: 1, markers: {} },
            { id: 2, markers: {}, snippets: { full: { template: 'entity_search' } } },
            { id: 3, markers: {} }
        ];
        context.reportData.searchdata.docs_right = docsRight;

        assert.isTrue(block(context, docsRight[1]));
    });

    it('should return false if doc is NOT last "entity_search"', function() {
        docsRight = [
            { id: 1, markers: {} },
            { id: 2, markers: {}, snippets: { full: { type: 'entity_search' } } },
            { id: 3, markers: {}, snippets: { full: { type: 'entity_search' } } }
        ];
        context.reportData.searchdata.docs_right = docsRight;

        assert.isFalse(block(context, docsRight[1]));
    });

    it('should return true, because MediaCard marker checked first', function() {
        docsRight = [
            { id: 1, markers: { MediaCard: '1' } },
            { id: 2, markers: { MediaCard: '1' }, snippets: { full: { type: 'entity_search' } } },
            { id: 3, markers: {}, snippets: { full: { type: 'entity_search' } } }
        ];
        context.reportData.searchdata.docs_right = docsRight;

        assert.isTrue(block(context, docsRight[1]));
    });
});
