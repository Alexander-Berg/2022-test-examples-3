const selectors = require('../../../page-objects');

describe('Блоки', function() {
    it.langs.full();
    it('Футер', async function() {
        await this.browser
            .ywOpenPage('moscow', {
                lang: this.lang,
                query: {
                    usemock: this.lang.tld !== 'ru' ? `turboapp_moscow_${this.lang.tld}` : 'turboapp_moscow',
                },
            })
            .ywInitLazyLoading()
            .ywHideCamerasAndNews()
            // смотрим на историческую карточку, потому что у футера одинаковые классы как для скелетона,
            // так и для обычного состояния.
            .ywWaitForVisible(selectors.index.HistoryCard, 5000)
            // ставим overflow: hidden, потому что не смотря на плагин и все задержки
            // именно в футере пролезает скролл на страницу
            .execute(() => {
                document.querySelector('html').style.overflow = 'hidden';
            })
            .pause(10)
            .assertView('footer', selectors.footer, { invisibleElements: selectors.tech.BaobabButton });
    });
});
