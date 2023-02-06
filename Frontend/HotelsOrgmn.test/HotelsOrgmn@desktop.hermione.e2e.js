'use strict';

const PO = require('./HotelsOrgmn.page-object')('desktop');

specs({
    feature: 'Hotels / Колдунщик многих организаций',
    type: 'Стандартный вид',
}, () => {
    it('Должен присутствовать на выдаче', async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result[@wizard_name="companies" and @subtype="travel_map"]/hotels-orgmn',
            PO.hotelsOrgmn(),
            '/search/touch?text=отели в лазаревском',
            {
                // выключаем карусель
                rearr: 'scheme_Local/OrgWizard/MinCarouselSize=1000',
            },
        );
    });
});
