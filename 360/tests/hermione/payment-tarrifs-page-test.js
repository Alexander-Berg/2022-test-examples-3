const page = require('../../../hermione/pages/payment-and-tarrifs');
const common = require('../../../hermione/pages/admin');
const app = require('../../../hermione/pages/common').app;

const { gotoOrganization } = require('./helpers/common');

const PRODUCTS_PAGE_ROUTE = '/products/plans';

async function gotoProductsPage(bro, indexOrg = 0) {
    await gotoOrganization(bro, false, indexOrg);
    const broUrl = await bro.getUrl();
    const url = new URL(broUrl);

    await bro.url(`/products?${url.searchParams.toString()}`);

    await bro.yaWaitForVisible(
        page.root(),
        'Не отобразилась страница оплаты и тарифов'
    );
}

hermione.only.in('chrome-desktop');
describe('Оплата и тарфифы', function () {
    describe('Физики', function () {
        it('Оплата и тарифы для физика-плательщика ', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-test-032',
                'pass-yndx-sarah-test-032'
            );
            await gotoProductsPage(bro, 1);
            await bro.assertView('ph-products-and-payments-payer', page.root());
        });

        it('Оплата и тарифы для физика-НЕплательщика ', async function () {
            const bro = this.browser;
            await bro.yaLoginFast(
                'yndx-sarah-test-033',
                'pass-yndx-sarah-test-033'
            );
            await gotoProductsPage(bro);
            await bro.assertView(
                'ph-products-and-payments-not-payer',
                page.root()
            );
        });
    });

    describe('Переключение тарифов', function () {
        it('Денег хватит на 3 месяца тарифа - просто переключаем', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-payflow-1', 'testiwan');
            await gotoOrganization(bro);

            await bro.url(PRODUCTS_PAGE_ROUTE);
            await bro.yaWaitForVisible(
                page.productList(),
                'Не отобразился список тарифов'
            );
            await bro.click(page.productList.chooseProductButtonByClass(1));
            await bro.yaWaitForVisible(page.changeTariffDialog());
            await bro.yaWaitForVisible(page.submitProductButton());
            await bro.click(page.submitProductButton());
            await bro.yaWaitForHidden(page.changeTariffDialog());
        });

        // it('Денег не хватит на 3 месяца тарифа - окно пополнения после переключения', async function () {
        //     const bro = this.browser;
        //     await bro.yaLoginFast('yndx-payflow-2', 'testiwan');
        //     await gotoOrganization(bro);
        // });

        it('Денег 0 - сразу окно пополнения баланса', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-payflow-3', 'testiwan');
            await gotoOrganization(bro);

            await bro.url(PRODUCTS_PAGE_ROUTE);
            await bro.yaWaitForVisible(
                common.topUpPanelRoot.topUpPanelCloseButton()
            );
            await bro.click(common.topUpPanelRoot.topUpPanelCloseButton());
            await bro.yaWaitForVisible(page.productsPageLink());
            await bro.click(page.productsPageLink());

            await bro.yaWaitForVisible(
                page.productList(),
                'Не отобразился список тарифов'
            );
            await bro.click(page.productList.chooseProductButtonByClass(1));

            await bro.yaWaitForVisible(common.topUpPanelRoot());
            await bro.pause(500); // анимация попапа
            await bro.yaAssertView(
                'change-tariff-top-up-zero-balance',
                common.topUpPanelRoot(),
                { hideElements: [app()] }
            );
        });

        it('Должник - подставляется сумма долга, подсветка красным если ввели меньше долга', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-payflow-4', 'testiwan');
            await gotoOrganization(bro);

            await bro.url(PRODUCTS_PAGE_ROUTE);
            await bro.yaWaitForVisible(
                common.topUpPanelRoot.topUpPanelCloseButton()
            );
            await bro.click(common.topUpPanelRoot.topUpPanelCloseButton());
            await bro.yaWaitForVisible(page.productsPageLink());
            await bro.click(page.productsPageLink());

            await bro.yaWaitForVisible(
                page.productList(),
                'Не отобразился список тарифов'
            );
            await bro.click(page.productList.chooseProductButtonByClass(1));

            await bro.yaWaitForVisible(common.topUpPanelRoot());
            await bro.pause(500); // анимация попапа
            await bro.yaAssertView(
                'change-tariff-top-up-has-debt',
                common.topUpPanelRoot(),
                { hideElements: [app()] }
            );
        });
    });
});
