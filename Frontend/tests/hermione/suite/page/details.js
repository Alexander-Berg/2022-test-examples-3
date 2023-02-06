const selectors = require('../../page-objects');

describe('Страницы', function() {
    describe('Детальный прогноз', function() {
        it('Страница', async function() {
            // делаем выборку всех детей
            const invisibleElements = [
                selectors.details.Title + ' *',
                selectors.details.Tabs + ' *',
                selectors.details.DetailsTemp + ' *',
                selectors.details.DetailsWind + ' *',
                selectors.details.DetailsHumidity + ' *',
                selectors.details.DetailsPressure + ' *',
                selectors.details.DetailsSun + ' *',
                selectors.details.DetailsOther + ' *',
            ];
            const hideElements = [
                selectors.header.Common,
                selectors.footer,
                selectors.meteum,
                selectors.tech.BaobabButton,
            ];

            await this.browser
                .ywOpenPage('details/today', {
                    query: {
                        usemock: `turboapp_moscow${this.lang.tld !== 'ru' ? `_${this.lang.tld}` : ''}`,
                    },
                })
                .ywWaitForVisible(selectors.detailsScreen, 10000)
                .ywDisguiseAllDetailsBlocks()
                .assertView('detailsFullPage', selectors.fullPage, { invisibleElements, hideElements });
        });

        it('Скелетон', async function() {
            const hideElements = [selectors.tech.BaobabButton];

            await this.browser
                .ywOpenPage('moscow/details', {
                    lang: this.lang,
                    query: {
                        usemock: 'turboapp-skeleton',
                        /*jshint camelcase: false */
                        showmethehamster: { show_skeleton: 1 },
                    },
                })
                .ywStopSkeletonAnimation()
                .assertView('detailsPageSkeleton', selectors.fullPage, { hideElements });
        });
    });
});
