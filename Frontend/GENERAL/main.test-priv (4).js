describeBlock('main__app-mark-stateful-nav', function(block) {
    let glob;

    beforeEach(function() {
        glob = stubGlobal('RequestCtx');
        RequestCtx.GlobalContext = {
            expFlags: {},
            isSearchApp: true,
            device: {
                BrowserName: 'YandexSearch'
            }
        };
    });

    afterEach(function() {
        glob.restore();
    });

    it('should return answer for full data', function() {
        const data = {
            latent_docs: {
                stateful_data: {
                    tagName: 'tagName',
                    themeTitle: 'themeTitle',
                    themeId: '4321',
                    urlsList: ['someurl.ru', 'someurl.org']
                }
            }
        };

        const result = block(data);

        assert.ok(Object.keys(result).length);
    });

    it('should return undefined when urlsList is undefined', function() {
        const data = {
            latent_docs: {
                stateful_data: {
                    tagName: 'tagName',
                    themeId: '4321'
                }
            }
        };

        assert.isUndefined(block(data));
    });

    it('should return undefined for empty urlsList', function() {
        const data = {
            latent_docs: {
                stateful_data: {
                    tagName: 'tagName',
                    themeId: '4321',
                    urlsList: []
                }
            }
        };

        assert.isUndefined(block(data));
    });

    it('should return undefined without stateful', function() {
        const data = {};

        assert.isUndefined(block(data));
    });
});
