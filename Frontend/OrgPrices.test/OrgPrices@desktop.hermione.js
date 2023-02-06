'use strict';

const POSimilar = require('../../SimilarCompanies/SimilarCompanies.test/SimilarCompanies.page-object/index@desktop');
const PO = require('./OrgPrices.page-object').desktop;

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

    it('Внешний вид', async function() {
        await this.browser.assertView('plain', PO.oneOrg.orgPrices());
    });

    describe('На выдаче', function() {
        it('Клик в заголовок открывает попап', async function() {
            const { browser } = this;

            const requestUrl = await this.browser.yaGetLastAjaxDataFast('"request_intent":"one-org-prices"', {
                field: 'url',
            }, () => this.browser.yaCheckBaobabCounter(PO.oneOrg.orgPrices.title(), {
                path: '/$page/$parallel/$result/composite/tabs/about/prices/title',
            }));

            assert.isNotNull(requestUrl, 'В запросе отсутствует request_intent');

            await browser.yaWaitForVisible(PO.popup.oneOrg.tabPrices());
        });

        it('Клик в ссылку "Смотреть меню полностью" открывает попап', async function() {
            const { browser } = this;

            const requestUrl = await this.browser.yaGetLastAjaxDataFast('"request_intent":"one-org-prices"', {
                field: 'url',
            }, () => this.browser.yaCheckBaobabCounter(PO.oneOrg.orgPrices.more(), {
                path: '/$page/$parallel/$result/composite/tabs/about/prices/more',
            }));

            assert.isNotNull(requestUrl, 'В запросе отсутствует request_intent');

            await browser.yaWaitForVisible(PO.popup.oneOrg.tabPrices());
        });
    });

    describe('В попапе', function() {
        it('Клик в заголовок открывает таб "Меню"', async function() {
            const { browser } = this;

            await browser.click(POSimilar.oneOrg.similarCompanies.scroller.thirdItem());
            await browser.yaWaitForVisible(PO.popup.oneOrg(), 'попап с 1орг не открылся');
            await browser.yaWaitForVisible(PO.popup.oneOrg.orgPrices());

            await browser.yaCheckBaobabCounter(PO.popup.oneOrg.orgPrices.title(), {
                path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/about/prices/title',
            });
            await browser.yaWaitForVisible(PO.popup.oneOrg.tabPrices());
        });

        it('Клик в ссылку "Смотреть меню полностью" открывает таб "Меню"', async function() {
            const { browser } = this;

            await browser.click(POSimilar.oneOrg.similarCompanies.scroller.thirdItem());
            await browser.yaWaitForVisible(PO.popup.oneOrg(), 'попап с 1орг не открылся');
            await browser.yaWaitForVisible(PO.popup.oneOrg.orgPrices());

            await browser.yaCheckBaobabCounter(PO.popup.oneOrg.orgPrices.more(), {
                path: '/$page/$parallel/$result/modal-popup/modal-content-loader/content/company/tabs/about/prices/more',
            });
            await browser.yaWaitForVisible(PO.popup.oneOrg.tabPrices());
        });
    });

    it('Валюта', async function() {
        const PO = this.PO;

        await this.browser.yaOpenSerp({
            text: 'здравия брест',
            data_filter: 'companies',
        }, PO.oneOrg());

        await this.browser.yaShouldBeVisible(PO.oneOrg.tabsPanes.about.prices(), 'Нет блока цен в карточке');
        await this.browser.assertView('org-prices-belarus', PO.oneOrg.tabsPanes.about.prices());
    });
});
