const selectors = require('../../../page-objects');

describe('Блоки', function() {
    describe('Шапка', function() {
        const { header } = selectors;

        it.langs.full();
        it('Серая шапка', async function() {
            await this.browser
                .ywOpenPage('moscow/details/today', {
                    lang: this.lang,
                    query: {
                        usemock: this.lang.tld !== 'ru' ? `turboapp_moscow_${this.lang.tld}` : 'turboapp_moscow',
                    },
                })
                .ywWaitForVisible(header.Gray, 5000)
                .assertView('grayHeader', header.Gray, {
                    hideElements: header.SearchIcon
                });
        });
    });
});
