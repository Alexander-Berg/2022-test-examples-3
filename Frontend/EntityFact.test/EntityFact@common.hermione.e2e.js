'use strict';

const PO = require('../EntityFact.page-object/');

hermione.only.notIn('searchapp-phone');
specs({
    feature: 'Факт',
    type: 'Объектный',
}, () => {
    it('Проверка наличия', async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result[@wizard_name="entity-fact"]',
            PO.EntityFact(),
            '/search/?text=сколько лет зюганову',
        );
    });
});
