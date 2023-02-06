const selectors = require('../../../page-objects');

describe('Промка горнолыжки', function() {
    hermione.only.in('chromeMobile');
    it('выглядит корректно и скрывается', async function() {
        await this.browser
            .ywOpenPage('moscow', {
                query: {
                    showmethehamster: {
                        spa_ski: 0,
                        spa_ski_promo: 1
                    },
                    usemock: 'prisma',
                },
            })
            .ywWaitForVisible(selectors.index.SkiPromoCard, 5000)
            .assertView('SkiPromoCard', selectors.index.SkiPromoCard)
            .pause(5000)
            .ywOpenPage('moscow', {
                query: {
                    showmethehamster: {
                        spa_ski_promo: 1
                    },
                    usemock: 'prisma',
                },
            })
            .ywWaitForVisible(selectors.index.HourlyContainer, 5000)
            .ywWaitForVisible(selectors.index.SkiPromoCard, 0, 'Промка не скрылась', true);
    });
});
