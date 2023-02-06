'use strict';

const PO = require('./ArchGlobalRegistry.page-object');

specs('Глобальный реестр', function() {
    it('Только SSR', async function() {
        await this.browser.yaOpenSerp(
            { foreverdata: '2305985620' },
            PO.archGlobalRegistry(),
        );

        await this.browser.assertView('plain', PO.archGlobalRegistry());
    });

    it('С гидрацией на клиенте', async function() {
        await this.browser.yaOpenSerp(
            { foreverdata: '281781522' },
            PO.archGlobalRegistry(),
        );

        await this.browser.assertView('plain', PO.archGlobalRegistry());
    });
});
