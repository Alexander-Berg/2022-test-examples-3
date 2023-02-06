'use strict';

const PO = require('./OrgAbout.page-object').desktop;

specs({
    feature: 'Одна организация',
    experiment: 'Блок о компании',
}, function() {
    it('Внешний вид', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кафе пушкин',
            exp_flags: 'GEO_1org_about=1',
            data_filter: 'companies',
        }, PO.oneOrg.OrgAbout());

        await browser.assertView('plain', PO.oneOrg.OrgAbout());

        await browser.yaCheckBaobabCounter(PO.oneOrg.OrgAbout.more(), {
            path: '$page/$parallel/$result/composite/tabs/about/org-about/more',
        });

        await browser.yaWaitForHidden(PO.oneOrg.OrgAbout.more());

        await browser.assertView('expand', PO.oneOrg.OrgAbout());
    });
});
