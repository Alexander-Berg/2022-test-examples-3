'use strict';

const PO = require('./CompaniesFooter.page-object')('common');

specs({
    feature: 'Колдунщик 1Орг',
    type: 'Футер',
}, function() {
    it('На выдаче', async function() {
        await this.browser.yaOpenSerp({
            text: 'кафе пушкин',
            data_filter: 'companies',
        }, PO.oneOrg());

        await this.browser.assertView('serp', PO.oneOrg.footer());
    });
});
