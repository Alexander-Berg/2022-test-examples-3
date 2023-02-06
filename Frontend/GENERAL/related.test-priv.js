describeBlock('related__item-mods', function(block) {
    it('should return undefined by default', function() {
        assert.equal(block(), undefined);
    });

    it('should return vertical-related-query for searchapp', function() {
        RequestCtx.GlobalContext.isSearchApp = true;

        assert.deepEqual(block(), {
            block: 'link',
            mods: { target: 'vertical-related-query' }
        });
    });

    it('should return vertical-related-query for direct page in YaBro', function() {
        RequestCtx.GlobalContext.isDirectPage = true;
        RequestCtx.GlobalContext.isYandexSearchBrowser = true;

        assert.deepEqual(block(), {
            block: 'link',
            mods: { target: 'vertical-related-query' }
        });
    });
});
