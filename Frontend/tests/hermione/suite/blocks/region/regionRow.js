const selectors = require('../../../page-objects').region;

describe('Блоки', function() {
    describe('Страница региона', function() {
        it.langs.full();
        it('Строка со ссылкой на локацию', async function() {
            await this.browser
                .ywOpenPage('region/10064', {
                    lang: this.lang,
                    query: {
                        usemock: `turboapp-region-small${this.lang.tld !== 'ru' ? `-${this.lang.tld}` : ''}`,
                    },
                })
                .ywWaitForVisible(selectors.Row, 5000)
                .assertView('regionRow', selectors.Row);
        });
    });
});
