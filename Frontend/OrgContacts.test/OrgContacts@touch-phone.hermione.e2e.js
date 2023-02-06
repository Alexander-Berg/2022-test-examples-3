'use-strict';

const PO = require('./OrgContacts.page-object')('touch-phone');

specs({
    feature: 'Одна организация',
    experiment: 'Контакты',
}, function() {
    it('Наличие элементов', async function() {
        const fallbackUrl = '/search/touch?text=кафе пушкин';

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result/composite/contacts/sites/site[@wizard_name="companies" and @subtype="company"]',
            PO.OrgContacts(),
            fallbackUrl,
        );
    });
});
