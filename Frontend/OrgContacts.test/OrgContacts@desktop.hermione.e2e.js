'use-strict';

const PO = require('./OrgContacts.page-object')('desktop');

specs({
    feature: 'Одна организация',
    experiment: 'Контакты',
}, function() {
    it('Наличие элементов', async function() {
        const fallbackUrl = '/search?text=кафе пушкин';

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$parallel/$result/composite/tabs/about/contacts/site[@wizard_name="companies" and @subtype="company"]',
            PO.OrgContacts(),
            fallbackUrl,
        );
    });
});
