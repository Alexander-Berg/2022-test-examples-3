const selectors = require('../../page-objects');

describe('Страницы', function() {
    describe('Регион', function() {
        it('Страница (абсорбация)', async function() {
            const hideElements = [selectors.footer, selectors.meteum, selectors.tech.BaobabButton];

            const ignoreElements = [selectors.region.Row, selectors.region.Letter];

            const invisibleElements = [selectors.region.Backward];

            await this.browser
                .ywOpenPage('region/84', {
                    query: {
                        usemock: 'turboapp-region',
                    },
                })
                .refresh()
                .ywWaitForVisible(selectors.regionContainer, 10000)
                .assertView('regionFullPage', selectors.fullPage, { hideElements, ignoreElements, invisibleElements });
        });

        it('Скелетон региона (абсорбация)', async function() {
            const hideElements = [selectors.tech.BaobabButton];

            await this.browser
                .ywOpenPage('region/123', {
                    query: {
                        usemock: 'turboapp-skeleton',
                        /*jshint camelcase: false */
                        showmethehamster: { show_skeleton: 1 },
                    },
                })
                .ywStopSkeletonAnimation()
                .assertView('regionSkeleton', selectors.fullPage, { hideElements });
        });
    });
});
