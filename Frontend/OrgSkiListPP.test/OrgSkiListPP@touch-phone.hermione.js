'use strict';

const PO = require('./OrgSkiListPP.page-object/index@touch-phone');

specs({
    feature: 'Одна организация',
    type: 'Горнолыжный курорт сценарий туриста',
}, function() {
    hermione.also.in('iphone-dark');
    h.it('Расхлопы и ссылки – внешний вид', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '3974341951',
        }, PO.OrgSkiListPP());

        await this.browser.assertView('OrgSkiListPP', PO.OrgSkiListPP());
    });

    h.it('Расхлопы и ссылки – расхлопы раскрываются по клику', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '3974341951',
        }, PO.OrgSkiListPP());

        await this.browser.click(PO.OrgSkiListPPItem1());
        await this.browser.click(PO.OrgSkiListPPItem3());
        await this.browser.click(PO.OrgSkiListPPItem4());
        await this.browser.yaWaitForVisible(PO.OrgSkiListPPItem4.CollapserContent(), 'врезка не раскрылась');
        await this.browser.assertView('opened', PO.OrgSkiListPP());
    });

    h.it('Расхлопы и ссылки – счётчики', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '3974341951',
            data_filter: 'companies',
        }, PO.OrgSkiListPP());

        await this.browser.yaCheckBaobabCounter(PO.OrgSkiListPPItem1.CollapserLabel(), {
            path: '/$page/$main/$result/composite/ski-collapser[@type="routes"]/label[@behaviour@type="dynamic"]',
        });
    });

    h.it('Расхлопы и ссылки – номер телефона', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '3320167860',
        }, PO.OrgSkiListPP());

        await this.browser.click(PO.OrgSkiListPPItem4());

        await this.browser.yaCheckBaobabCounter(PO.OrgSkiListPP.PhoneMore(), {
            path: '/$page/$main/$result/composite/ski-collapser/ski-features/key-value/phones/more',
        }, 'Не сработал счетчик расхлопа телефона');

        await this.browser.assertView('extended-phone', PO.OrgSkiListPP());
    });

    h.it('Расхлопы и ссылки – ссылка "Цены на ски-пасс"', async function() {
        await this.browser.yaOpenSerp({
            text: 'foreverdata',
            foreverdata: '3974341951',
            data_filter: 'companies',
        }, PO.OrgSkiListPP());

        await this.browser.click(PO.OrgSkiListPPItem3());

        await this.browser.yaCheckBaobabCounter(PO.OrgSkiListPP.BuyButton(), {
            path: '/$page/$main/$result/composite/ski-collapser/ski-pass/buy-link',
        });
    });
});
