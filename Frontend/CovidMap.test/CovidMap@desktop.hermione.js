'use strict';

const PO = require('./CovidMap.page-object').desktop;

specs('Карта коронавируса', function() {
    it('Основные проверки', async function() {
        await this.browser.yaOpenSerp(
            {
                text: 'коронавирус карта россии',
                data_filter: 'special/event',
            },
            PO.covidMap(),
        );

        await this.browser.yaCheckBaobabServerCounter({
            path: '/$page/$top/$result[@wizard_name="special/event" and @subtype="virus_map"]',
        });

        await this.browser.yaShouldBeVisible(PO.covidMap());
    });
});
