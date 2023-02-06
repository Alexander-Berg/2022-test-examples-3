'use strict';

const { MarketOffersWizard } = require('../../../../Market.test/Market.page-object');

specs({
    feature: 'Маркет. Офферный колдунщик',
    type: 'Вытесненный справа',
}, () => {
    it('Проверка viewType в поле data-subtype', async function() {
        await this.browser.yaOpenSerp({
            foreverdata: 1778479012,
        }, MarketOffersWizard());

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/$main/$result[@wizard_name="market_constr" and @subtype="market_offers_wizard_center_incut"]',
        });
    });
});
