const selectors = require('../../../page-objects').month;

describe('Блоки', function() {
    describe('Страница месяца', function() {
        describe('Диаграмма', function() {
            it.langs.full();
            it('Диаграмма', async function() {
                await this.browser
                    .ywOpenPage('month/april?lat=54.7574942101484&lon=38.87098261901856', {
                        lang: this.lang,
                        query: {
                            usemock: `turboapp-month${this.lang.tld !== 'ru' ? `-${this.lang.tld}` : ''}`,
                        },
                    })
                    .ywWaitForVisible(selectors.MonthDiagramm, 5000)
                    .ywRemoveShadowFromContainer(selectors.MonthDiagramm)
                    .ywHideCamerasAndNews()
                    .assertView('monthDiagramm', selectors.MonthDiagramm);
            });
            it.langs.full();
            it('Диаграмма нулевая температура воды', async function() {
                await this.browser
                    .ywOpenPage('month/april?lat=54.7574942101484&lon=38.87098261901856', {
                        lang: this.lang,
                        query: {
                            usemock: 'month_zero_water_temp',
                        },
                    })
                    .ywWaitForVisible(selectors.MonthDiagramm, 5000)
                    .ywRemoveShadowFromContainer(selectors.MonthDiagramm)
                    .ywHideCamerasAndNews()
                    .assertView('monthDiagrammZeroTemp', selectors.MonthDiagramm);
            });
            it.langs.full();
            it('Диаграмма нулевые осадки', async function() {
                await this.browser
                    .ywOpenPage('month/april?lat=54.7574942101484&lon=38.87098261901856', {
                        lang: this.lang,
                        query: {
                            usemock: 'month_no_prec',
                        },
                    })
                    .ywWaitForVisible(selectors.MonthDiagramm, 5000)
                    .ywRemoveShadowFromContainer(selectors.MonthDiagramm)
                    .ywHideCamerasAndNews()
                    .assertView('monthDiagrammZeroPrec', selectors.MonthDiagramm);
            });
            it.langs.full();
            it('Диаграмма нулевое кол-во дней с переменной обл.', async function() {
                await this.browser
                    .ywOpenPage('month/april?lat=54.7574942101484&lon=38.87098261901856', {
                        lang: this.lang,
                        query: {
                            usemock: 'month_zero_overcast_days',
                        },
                    })
                    .ywWaitForVisible(selectors.MonthDiagramm, 5000)
                    .ywRemoveShadowFromContainer(selectors.MonthDiagramm)
                    .ywHideCamerasAndNews()
                    .assertView('monthDiagrammZeroOvercast', selectors.MonthDiagramm);
            });
        });
    });
});
