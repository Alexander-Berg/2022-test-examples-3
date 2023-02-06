const selectors = require('../../page-objects');
describe('Страницы', function() {
    describe('Месяц', function() {
        it('Страница', async function() {
            // делаем выборку всех детей
            const invisibleElements = [
                selectors.details.Title + ' *',
                selectors.details.Tabs + ' *',
                selectors.month.Calendar + ' *',
                selectors.month.MonthDiagramm + ' *',
                selectors.month.CitiesList + ' *',
            ];
            const hideElements = [
                selectors.header.Common,
                selectors.footer,
                selectors.meteum,
                selectors.tech.BaobabButton
            ];

            await this.browser
                .ywOpenPage('month/april', {
                    query: {
                        usemock: `turboapp-month${this.lang.tld !== 'ru' ? `-${this.lang.tld}` : ''}`,
                    },
                })
                .ywWaitForVisible(selectors.monthScreen, 10000)
                .ywDisguiseAllMonthBlocks()
                .assertView('MonthPage', selectors.fullPage, { invisibleElements, hideElements });
        });

        it('Скелетон', async function() {
            const hideElements = [selectors.tech.BaobabButton];

            await this.browser
                .ywOpenPage('month/april', {
                    lang: this.lang,
                    query: {
                        usemock: 'turboapp-skeleton',
                        /*jshint camelcase: false */
                        showmethehamster: { show_skeleton: 1 },
                    },
                })
                .ywStopSkeletonAnimation()
                .assertView('MonthPage_skeleton', selectors.fullPage, { hideElements });
        });
    });
});
