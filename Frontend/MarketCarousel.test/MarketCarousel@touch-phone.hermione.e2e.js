'use strict';

const { MarketCarousel } = require('../../../Market.test/Market.page-object');
const fallbacks = [
    '/search/touch?text=купить+холодильник',
    '/search/touch?text=купить+телевизор',
    '/search/touch?text=купить+принтер',
    '/search/touch?text=купить+пылесос',
];

specs({
    feature: 'Товарная галерея',
}, () => {
    it('Проверка наличия', async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result/marketCarousel',
            MarketCarousel(),
            fallbacks,
        );
    });
});
