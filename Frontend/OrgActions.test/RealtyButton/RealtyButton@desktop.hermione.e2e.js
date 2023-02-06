'use strict';

const PO = require('../OrgActions.page-object').desktop;

const fallbackUrl = '/search/?text=жк бунинские луга&lr=213';

specs({
    feature: 'Колдунщик 1орг',
    type: 'Кнопка телефона недвижимости',
}, function() {
    it('Проверка попапа', async function() {
        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result[@wizard_name="companies" and @subtype="company"]/composite/tabs/about/contacts/realty/phone',
            PO.oneOrg.buttons.realty(),
            fallbackUrl,
        );
    });
});
