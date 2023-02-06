const selectors = require('../../page-objects');

describe('Страницы', function() {
    describe('Главная', function() {
        it('Скелетон (абсорбация)', async function() {
            const hideElements = [selectors.tech.BaobabButton];

            await this.browser
                .ywOpenPage('moscow', {
                    lang: this.lang,
                    query: {
                        usemock: 'turboapp-skeleton',
                        showmethehamster: { show_skeleton: 1 },
                    },
                })
                .ywStopSkeletonAnimation()
                .assertView('MainPage_skeleton', selectors.fullPage, { hideElements });
        });
    });
});
