describeBlock('i-global__add-search-props', function(block) {
    stubBlocks('RequestCtx');
    beforeEach(function() {
        RequestCtx.Baobab = {
            Helpers: {
                saveServerTechCounter: function(log, name, data) {
                    const attrs = log.baobabTree.tree && log.baobabTree.tree.attrs;
                    attrs.tech[name] = data;
                }
            }
        };
    });

    it('should return undefined', () => {
        const data = {};
        assert.isUndefined(block(data));
    });

    it('should return undefined', () => {
        const data = {
            entry: 'pre-search'
        };
        assert.isUndefined(block(data));
    });

    it('should return undefined', () => {
        const data = {
            WEB: []
        };
        assert.isUndefined(block(data));
    });

    it('should return undefined', () => {
        const data = {
            WEB: [
                {
                    properties: {}
                }]
        };
        assert.isUndefined(block(data));
    });

    it('should return modificated object with field', () => {
        const data = {
            search_props: {
                WEB: [
                    {
                        properties: {
                            'FreshDetector.PrsSourceRatioFromQuickRt': '0'
                        }
                    }]
            },
            log: {
                baobabTree: {
                    tree: {
                        attrs: {
                            tech: {}
                        }
                    }
                }
            }
        };
        block(data);
        assert.equal(data.log.baobabTree.tree.attrs.tech.scraper['FreshDetector.PrsSourceRatioFromQuickRt'], '0');
    });
});
