const selectors = require('../../../page-objects').index;

describe('Блоки', function() {
    describe('Главная страница', function() {
        it.langs.full();
        it('Ссылка на месяц', async function() {
            await this.browser
                .ywOpenPage('moscow', {
                    lang: this.lang,
                    query: {
                        usemock: 'turboapp_moscow',
                    },
                })
                .ywWaitForVisible(selectors.MonthLink, 5000)
                .ywHideCamerasAndNews()
                .pause(50)
                .assertView('MonthLink', selectors.MonthLink)
                .assertView('MonthLink_second', selectors.MonthLinkSecond);
        });
    });
});
