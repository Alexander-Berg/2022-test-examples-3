const selectors = require('../../../page-objects').region;

describe('Блоки', function() {
    describe('Страница региона', function() {
        it.langs.full();
        it('Строка тайтла', async function() {
            await this.browser
                .ywOpenPage('region/225', {
                    lang: this.lang,
                    query: {
                        usemock: `turboapp-region-small${this.lang.tld !== 'ru' ? `-${this.lang.tld}` : ''}`,
                    },
                })
                .ywWaitForVisible(selectors.Backward, 5000)
                .ywDeleteLoader()
                .assertView('regionBackward', selectors.Backward);
        });
    });
});
