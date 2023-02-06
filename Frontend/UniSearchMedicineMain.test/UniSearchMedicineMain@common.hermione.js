'use strict';
const PO = require('./UniSearchMedicineMain.page-object/index');

specs({
    feature: 'Универсальный колдунщик поиска врачей',
}, function() {
    describe('Блоки', function() {
        describe('Описание врача', function() {
            const serpParams = {
                text: 'foreverdata',
                foreverdata: 3143097452,
                data_filter: 'unisearch/medicine',
            };

            it('Внешний вид', async function() {
                await this.browser.yaOpenSerp(serpParams, PO.Main());

                await this.browser.assertView('plain', PO.Main());
                await this.browser.click(PO.Main.DescriptionMore());
                await this.browser.assertView('expanded', PO.Main());

                await this.browser.yaCheckBaobabCounter(() => {}, {
                    path: '/$page/$main/$result/extended-text/more',
                    attrs: {
                        behaviour: {
                            type: 'dynamic',
                        },
                    },
                });
            });

            it('Проверка ссылок', async function() {
                await this.browser.yaOpenSerp(serpParams, PO.Main());
                await this.browser.yaCheckBaobabCounter(PO.Main.Source.Link(), {
                    path: '/$page/$main/$result/source_meta',
                    attrs: {
                        text: 'НаПоправку',
                        url: 'https://napopravku.ru/moskva/doctor-profile/edelev-dmitriy-arkadevich/',
                    },
                });
            });
        });
    });
});
