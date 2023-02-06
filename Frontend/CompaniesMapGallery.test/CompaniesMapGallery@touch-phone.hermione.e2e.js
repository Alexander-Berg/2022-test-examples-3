'use strict';

const PO = require('./CompaniesMapGallery.page-object').touch;

specs({
    feature: 'Одна организация',
    type: 'Галерея с фото и картой',
}, function() {
    it('Наличие элементов', async function() {
        const fallbackUrl = '/search/touch?text=кафе пушкин';

        await this.browser.yaOpenUrlByFeatureCounter(
            '/$page/$main/$result/composite/companies-map-gallery/photo-tiles/scroller/item[@wizard_name="companies" and @subtype="company"]',
            PO.oneOrg.companiesMapGallery(),
            fallbackUrl,
        );
    });
});
