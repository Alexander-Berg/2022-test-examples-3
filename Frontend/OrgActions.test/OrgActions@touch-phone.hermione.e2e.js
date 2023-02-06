'use strict';

const PO = require('./OrgActions.page-object').touchPhone;

specs({
    feature: 'Одна организация',
    experiment: 'Блок кнопок',
}, function() {
    it('Наличие элементов', async function() {
        const fallbackUrl = '/search/touch?text=кафе пушкин';

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result/composite/org-actions/scroller/contacts[@wizard_name="companies" and @subtype="company"]',
            PO.oneOrg.OrgActions(),
            fallbackUrl,
        );
    });
});
