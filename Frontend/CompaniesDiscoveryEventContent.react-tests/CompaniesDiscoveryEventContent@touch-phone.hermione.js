'use strict';

const PO = require('../CompaniesDiscoveryEventContent.test/CompaniesDiscoveryEventContent.page-object')('touch-phone');

const componentName = 'CompaniesDiscoveryEventContent';

specs({
    feature: 'Компоненты: CompaniesDiscoveryEventContent',
}, function() {
    it('Внешний вид', async function() {
        await this.browser.yaOpenComponent(
            `serp-components/${componentName}@touch-phone`,
            'Внешний вид',
        );
        await this.browser.assertView('plain', PO.CompaniesDiscoveryEventContent());
    });
});
