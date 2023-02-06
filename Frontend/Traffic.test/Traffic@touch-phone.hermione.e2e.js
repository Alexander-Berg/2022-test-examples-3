'use strict';

const PO = require('./Traffic.page-object')('touch-phone');

const fallbackUrl = '/search/touch?text=пробки в москве';

hermione.only.notIn('searchapp-phone');
specs('Колдунщик пробок', function() {
    it('Наличие колдунщика', async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result[@wizard_name="traffic"]/map',
            PO.traffic(),
            fallbackUrl,
        );
        await this.browser.yaWaitForVisible(PO.traffic.map(), 'Не показалась карта в колдунщике пробок');
    });
});
