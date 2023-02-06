const selectors = require('../../../page-objects');

describe('Блоки', function() {
    describe('Главная страница', function() {
        const { index } = selectors;

        it.langs.only('ru');
        hermione.only.in('chromeMobile');
        it('График 10ти дней закрыт', async function() {
            await this.browser
                .ywOpenPage('moscow', {
                    lang: this.lang,
                    query: {
                        usemock: 'horz_10_days',
                    },
                })
                .ywWaitForVisible(index.ForecastWidget, 5000)
                .pause(500)
                .assertView('forecast-chart-closed', index.ForecastWidget);
        });

        it.langs.full();
        it('График 10ти дней', async function() {
            await this.browser
                .ywOpenPage('moscow', {
                    lang: this.lang,
                    query: {
                        usemock: 'horz_10_days',
                    },
                })
                .ywWaitForVisible(index.ForecastWidget, 5000)
                .click(index.ForecastWidgetBtnOpen)
                .ywWaitForVisible(index.ForecastWidgetOpened, 5000)
                .pause(500)
                .assertView('forecast-chart', index.ForecastWidget);
        });
    });
});
