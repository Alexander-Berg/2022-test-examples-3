const selectors = require('../../../page-objects').index;

describe('Блоки', function() {
    describe('Главная страница', function() {
        describe('Детальный прогноз', function() {
            it.langs.full();
            it('В абсорбации', async function() {
                await this.browser
                    .ywOpenPage('moscow', {
                        lang: this.lang,
                        query: {
                            usemock: 'turboapp_moscow',
                        },
                    })
                    .ywWaitForVisible(selectors.Forecast, 5000)
                    .ywDeleteAdvs()
                    .ywHideCamerasAndNews()
                    .assertView('Forecast', selectors.Forecast);
            });
        });
    });
});
