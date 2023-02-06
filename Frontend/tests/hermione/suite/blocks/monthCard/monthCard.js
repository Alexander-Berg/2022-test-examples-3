const selectors = require('../../../page-objects').index;

describe('Блоки', function() {
    describe('Главная страница', function() {
        it.langs.full();
        it('Карточка месяца', async function() {
            await this.browser
                .ywOpenPage('moscow', {
                    lang: this.lang,
                    query: {
                        usemock: 'climate_card',
                    },
                })
                .ywWaitForVisible(selectors.MonthCard, 5000)
                .ywHideCamerasAndNews()
                .assertView('MonthCard', selectors.MonthCard);
        });
    });
});
