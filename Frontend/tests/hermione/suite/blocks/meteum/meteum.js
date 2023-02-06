const selectors = require('../../../page-objects');

describe('Блоки', function() {
    it.langs.full();
    it('Метеум', async function() {
        await this.browser
            .ywOpenPage('moscow', {
                lang: this.lang,
                query: {
                    usemock: this.lang.tld !== 'ru' ? `turboapp_moscow_${this.lang.tld}` : 'turboapp_moscow',
                },
            })
            .ywInitLazyLoading()
            .ywHideCamerasAndNews()
            .assertView('meteum', selectors.meteum, {
                hideElements: [selectors.footer]
            });
    });
});
