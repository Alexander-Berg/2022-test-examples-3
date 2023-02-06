'use strict';

const { MarketCarousel } = require('../../../Market.test/Market.page-object');
const fallbacks = [
    '/search/?text=купить+холодильник',
    '/search/?text=купить+телевизор',
    '/search/?text=купить+принтер',
    '/search/?text=купить+пылесос',
];

specs({
    feature: 'Товарная галерея',
}, () => {
    it('Проверка наличия', async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$top/$result/marketCarousel',
            MarketCarousel(),
            fallbacks,
        );
    });
});
