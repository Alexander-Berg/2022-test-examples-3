'use strict';

const TrafficPO = require('./Traffic.page-object');
let PO;

const fallbackUrl = '/search?text=пробки в москве';

specs('Колдунщик пробок', function() {
    beforeEach(function() {
        PO = TrafficPO(this.currentPlatform);

        return this.browser;
    });

    it('Проверяем показ колдунщика', async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result[@wizard_name="traffic"]/point',
            PO.traffic(),
            fallbackUrl,
        );
        await this.browser.yaWaitForVisible(PO.traffic.map(), 'Не показалась карта в колдунщике пробок');

        await this.browser.yaWaitForVisible(
            PO.traffic.map.gotomap(),
            'Не найдена кнопка "На большую карту" в колдунщике пробок',
        );
    });
});
