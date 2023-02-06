'use strict';

const PO = require('./OrgFeatures.page-object')('touch-phone');

specs({
    feature: 'Одна организация',
    type: 'Фичи организации',
}, function() {
    it('Общие проверки', async function() {
        await this.browser.yaOpenSerp({
            text: 'кафе парус якорная',
            data_filter: 'companies',
            exp_flags: 'GEO_1org_about_old',
        }, PO.companiesComposite());

        await this.browser.click(PO.companiesComposite.tabsMenu.about());
        await this.browser.yaWaitForVisible(PO.bcardSideBlock(), 15000, 'Не показался сайдблок');

        await this.browser.yaWaitForVisible(
            PO.bcardSideBlock.OrgFeatures(),
            'Не показался список фичей организации',
        );

        await this.browser.yaScrollOverlay(PO.bcardSideBlock.OrgFeatures());
        await this.browser.assertView('plain', PO.bcardSideBlock.OrgFeatures());

        await this.browser.yaCheckBaobabCounter(PO.bcardSideBlock.OrgFeatures.short.toggle(), {
            path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/about/side-block-bcard/composite/features/more[@behaviour@type="dynamic"]',
        });

        await this.browser.assertView('expanded', PO.bcardSideBlock.OrgFeatures.full());
    });

    it('Длинные особенности обрезаются с многоточием', async function() {
        await this.browser.yaOpenSerp({
            text: 'спб почта россии 191055',
            data_filter: 'companies',
            exp_flags: 'GEO_1org_about_old',
        }, PO.OrgFeatures());

        await this.browser.assertView('long-features-cut-with-ellipsis-collapsed', PO.OrgFeatures());

        await this.browser.yaCheckBaobabCounter(PO.OrgFeatures.short.toggle(), {
            path: '/$page/$main/$result/composite/features/more[@behaviour@type="dynamic"]',
        });

        await this.browser.assertView('long-features-cut-with-ellipsis-expanded', PO.OrgFeatures.full());
    });

    it('Длинные особенности обрезаются по разделителю', async function() {
        await this.browser.yaOpenSerp({
            text: 'ILab на конюшенной',
            data_filter: 'companies',
            exp_flags: 'GEO_1org_about_old',
        }, PO.OrgFeatures());

        await this.browser.assertView('long-features-cut-collapsed', PO.OrgFeatures());

        await this.browser.yaCheckBaobabCounter(PO.OrgFeatures.short.toggle(), {
            path: '/$page/$main/$result/composite/features/more[@behaviour@type="dynamic"]',
        });

        await this.browser.assertView('long-features-cut-expanded', PO.OrgFeatures.full());
    });
});
