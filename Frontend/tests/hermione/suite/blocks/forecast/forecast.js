const selectors = require('../../../page-objects');

describe('Блоки', function() {
    describe('Главная страница', function() {
        describe('Детальный прогноз', function() {
            it.langs.full();
            it('Обычный', async function() {
                const hideElements = [
                    selectors.index.MonthLink,
                    selectors.index.MonthLinkSecond,
                    selectors.index.ForecastWidget
                ];

                await this.browser
                    .ywOpenPage('moscow', {
                        lang: this.lang,
                        query: {
                            usemock: 'all_wind_directions',
                        },
                    })
                    // ждем monthlink, это надежнее, чем ждать forecast
                    .ywWaitForVisible(selectors.index.MonthLink, 8000)
                    .ywHideCamerasAndNews({ hideAllCamera: true })
                    .assertView('Forecast2', selectors.index.Forecast, { hideElements });
            });
        });
    });
});
