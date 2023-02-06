'use strict';

const { MarketOffersWizard } = require('../../../Market.test/Market.page-object');

specs({
    feature: 'Маркет. Офферный колдунщик в центре',
    type: 'Дебрендированный офферный колдунщик',
}, function() {
    it('Наличие элементов', async function() {
        const { browser } = this;
        const fallbackUrl = '/search/touch?text=купить диван';

        await browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result/MarketOffersWizard/debranding-title',
            MarketOffersWizard(),
            fallbackUrl,
        );

        await browser.yaShouldBeVisible(MarketOffersWizard.debrandingTitle());
        await browser.yaShouldBeVisible(MarketOffersWizard.firstProductCard());
    });
});
