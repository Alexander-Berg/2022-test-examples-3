'use strict';

const PO = require('./OrgAbout.page-object').touchPhone;

specs({
    feature: 'Одна организация',
    type: 'Блок о компании',
}, function() {
    it('На выдаче', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'салон красоты колибри богданович',
            data_filter: 'companies',
        }, PO.oneOrg.OrgAbout());

        await browser.assertView('plain', PO.oneOrg.OrgAbout());

        await browser.yaCheckBaobabCounter(PO.oneOrg.OrgAbout.more(), {
            path: '/$page/$main/$result/composite/org-about/more',
        });

        await browser.yaWaitForHidden(PO.oneOrg.OrgAbout.more());

        await browser.assertView('expand', PO.oneOrg.OrgAbout());
    });

    it('В оверлее', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'кафе пушкин',
            data_filter: 'companies',
        }, PO.oneOrg());
        await browser.yaOpenOverlayAjax(
            () => browser.click(PO.oneOrg.tabsMenu.about()),
            PO.overlayOneOrg(),
            'Сайдблок с карточкой организации не появился',
        );

        await browser.yaScroll(PO.overlayOneOrg.OrgAbout());
        await browser.assertView('plain', PO.overlayOneOrg.OrgAbout());

        await browser.yaCheckBaobabCounter(PO.overlayOneOrg.OrgAbout.more(), {
            path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/about/side-block-bcard/composite/org-about/more',
        });

        await browser.yaWaitForHidden(PO.overlayOneOrg.OrgAbout.more());

        await browser.assertView('expand', PO.overlayOneOrg.OrgAbout());
    });

    it('Организация с длинным описанием в оверлее', async function() {
        const { browser } = this;

        await browser.yaOpenSerp({
            text: 'fservice москва',
            data_filter: 'companies',
        }, PO.oneOrg());
        await browser.yaOpenOverlayAjax(
            () => browser.click(PO.oneOrg.tabsMenu.about()),
            PO.overlayOneOrg(),
            'Сайдблок с карточкой организации не появился',
        );

        await browser.yaScroll(PO.overlayOneOrg.OrgAbout());
        await browser.assertView('plain', PO.overlayOneOrg.OrgAbout());

        await browser.yaCheckBaobabCounter(PO.overlayOneOrg.OrgAbout.more(), {
            path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/about/side-block-bcard/composite/org-about/more',
        });

        await browser.yaWaitForHidden(PO.overlayOneOrg.OrgAbout.more());

        await browser.assertView('expand', PO.overlayOneOrg.OrgAbout());
    });
});
