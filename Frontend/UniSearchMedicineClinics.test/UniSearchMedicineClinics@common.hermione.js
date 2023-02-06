'use strict';
const PO = require('./UniSearchMedicineClinics.page-object/index@common');

specs({
    feature: 'Универсальный колдунщик поиска врачей',
}, function() {
    describe('Блоки', function() {
        describe('Список клиник', function() {
            const serpParams = {
                text: 'foreverdata',
                foreverdata: 2132929412,
                data_filter: 'unisearch/medicine',
            };
            it('Внешний вид', async function() {
                await this.browser.yaOpenSerp(serpParams, PO.UniSearchMedicineClinics());

                await this.browser.assertView('plain', PO.UniSearchMedicineClinics());
            });
        });
    });
});
