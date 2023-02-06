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

            it('Ссылки на перезапрос с 1Орг', async function() {
                await this.browser.yaOpenSerp(serpParams, PO.UniSearchMedicineClinics());
                await this.browser.yaIndexify(PO.UniSearchMedicineClinics.Clinic());
                await this.browser.yaCheckBaobabCounter(
                    PO.UniSearchMedicineClinics.ClinicFirst.Name.Link(),
                    {
                        path: '/$page/$main/$result/unisearch-clinic/link',
                        attrs: {
                            name: 'Клиника факультетской хирургии имени Н.Н. Бурденко Университетской клинической больницы №1',
                            url: '?text=%D0%9A%D0%BB%D0%B8%D0%BD%D0%B8%D0%BA%D0%B0+%D1%84%D0%B0%D0%BA%D1%83%D0%BB%D1%8C%D1%82%D0%B5%D1%82%D1%81%D0%BA%D0%BE%D0%B9+%D1%85%D0%B8%D1%80%D1%83%D1%80%D0%B3%D0%B8%D0%B8+%D0%B8%D0%BC%D0%B5%D0%BD%D0%B8+%D0%9D.%D0%9D.+%D0%91%D1%83%D1%80%D0%B4%D0%B5%D0%BD%D0%BA%D0%BE+%D0%A3%D0%BD%D0%B8%D0%B2%D0%B5%D1%80%D1%81%D0%B8%D1%82%D0%B5%D1%82%D1%81%D0%BA%D0%BE%D0%B9+%D0%BA%D0%BB%D0%B8%D0%BD%D0%B8%D1%87%D0%B5%D1%81%D0%BA%D0%BE%D0%B9+%D0%B1%D0%BE%D0%BB%D1%8C%D0%BD%D0%B8%D1%86%D1%8B+%E2%84%961&oid=b:161203215287&serp-reload-from=unisearch/medicine',
                        },
                    },
                );
            });
        });
    });
});
