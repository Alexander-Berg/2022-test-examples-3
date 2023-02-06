'use strict';

const PO = require('./OrgPrices.page-object').touchPhone;

specs({
    feature: 'Одна организация',
    type: 'Врезка с ценами',
}, function() {
    beforeEach(async function() {
        await this.browser.yaOpenSerp({
            text: 'duo pizza and wine екатеринбург',
            data_filter: 'companies',
        }, PO.oneOrg());
    });

    describe('На выдаче', function() {
        it('Внешний вид', async function() {
            await this.browser.assertView('plain', PO.oneOrg.orgPrices());
        });

        it('Клик в заголовок открывает оверлей', async function() {
            const { browser } = this;

            const requestUrl = await this.browser.yaGetLastAjaxDataFast('"request_intent":"one-org-prices"', {
                field: 'url',
            }, () => this.browser.yaCheckBaobabCounter(PO.oneOrg.orgPrices.title(), {
                path: '/$page/$main/$result/composite/prices/title',
            }));

            assert.isNotNull(requestUrl, 'В запросе отсутствует request_intent');

            await browser.yaWaitForVisible(PO.overlayOneOrg.tabPrices());
        });

        it('Клик в ссылку "Смотреть меню полностью" открывает оверлей', async function() {
            const { browser } = this;

            const requestUrl = await this.browser.yaGetLastAjaxDataFast('"request_intent":"one-org-prices"', {
                field: 'url',
            }, () => this.browser.yaCheckBaobabCounter(PO.oneOrg.orgPrices.more(), {
                path: '/$page/$main/$result/composite/prices/more',
            }));

            assert.isNotNull(requestUrl, 'В запросе отсутствует request_intent');

            await browser.yaWaitForVisible(PO.overlayOneOrg.tabPrices());
        });
    });

    describe('В оверлее', function() {
        it('Внешний вид', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.tabsMenu.about());
            await browser.yaWaitForVisible(PO.overlayOneOrg.orgPrices());
            await browser.yaScrollOverlay(PO.overlayOneOrg.orgPrices());
            await browser.assertView('overlay', PO.overlayOneOrg.orgPrices());
        });

        it('Клик в заголовок открывает таб "Меню"', async function() {
            const { browser } = this;

            await browser.click(PO.oneOrg.tabsMenu.about());
            await browser.yaWaitForVisible(PO.overlayOneOrg.orgPrices());
            await browser.yaScrollOverlay(PO.overlayOneOrg.orgPrices());
            await browser.yaCheckBaobabCounter(PO.overlayOneOrg.orgPrices.title(), {
                path: '/$subresult/overlay-wrapper/content/tabs/controls/tabs/about/side-block-bcard/composite/prices/title',
            });
            await browser.yaWaitForVisible(PO.overlayOneOrg.tabPrices());
        });
    });
});
