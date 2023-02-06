'use strict';
const PO = require('./UniSearchMedicineOffers.page-object/index@common');

specs({
    feature: 'Универсальный колдунщик поиска врачей',
}, function() {
    describe('Блоки', function() {
        describe('Список оферов', function() {
            const serpParams = {
                text: 'foreverdata',
                foreverdata: 585070554,
                data_filter: 'unisearch/medicine',
            };
            it('Внешний вид', async function() {
                await this.browser.yaOpenSerp(serpParams, PO.UniSearchMedicineOffers());

                await this.browser.assertView('plain', PO.UniSearchMedicineOffers());
            });
        });
    });
});
