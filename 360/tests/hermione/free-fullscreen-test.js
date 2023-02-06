const { assert } = require('chai');
const adminPage = require('../../../hermione/pages/admin');
const { gotoOrganization } = require('./helpers/common');

const PAYMENT_AND_PRODUCT_PAGE_ROUTE = '/products';
const PRODUCTS_PAGE_ROUTE = '/products/plans';

hermione.only.in('chrome-desktop');

describe('free fuyllscreen', function () {
    describe('out of money', function () {
        it('assert top up panel view and close', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-fullscreen2-test', 'testiwan');
            await gotoOrganization(bro);
            await bro.url('/users');
            await bro.yaWaitForVisible(
                adminPage.topUpPanelRoot(),
                'Не отобразился фуллскрин бесплатников'
            );

            await bro.yaAssertView(
                'free-fullscreen-top-up-panel-root',
                adminPage.freeFullscreen()
            );

            await bro.click(adminPage.topUpPanelRoot.topUpPanelCloseButton());

            const address = await bro.getUrl();
            assert.equal(
                new URL(address).pathname,
                PAYMENT_AND_PRODUCT_PAGE_ROUTE
            );
        });

        it('assert time buttons and prices', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-fullscreen2-test', 'testiwan');
            await gotoOrganization(bro);
            await bro.url(PAYMENT_AND_PRODUCT_PAGE_ROUTE);
            await bro.yaWaitForVisible(
                adminPage.freeFullscreen(),
                'Не отобразился фуллскрин бесплатников'
            );

            const usersCount = 1;
            const pricePerMonth = 249;
            const monthsAmount = [1, 3, 6, 12];

            for (const term of monthsAmount) {
                await bro.click(
                    adminPage.topUpPanelRoot[`timeButton${term}`]()
                );
                const priceVal = await bro
                    .$(adminPage.topUpPanelRoot.priceInput())
                    .getValue();
                assert.equal(priceVal, usersCount * pricePerMonth * term);
            }
        });

        it('open and close trust frame', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-fullscreen2-test', 'testiwan');
            await gotoOrganization(bro);
            await bro.url('/domains');
            await bro.yaWaitForVisible(
                adminPage.freeFullscreen(),
                'Не отобразился фуллскрин бесплатников'
            );

            await bro.click(adminPage.topUpPanelRoot.payButton());

            await bro.yaWaitForVisible(adminPage.paymentRoot.trustFrame());
            await bro.click(adminPage.paymentRoot.closeButton());

            const address = await bro.getUrl();
            assert.equal(new URL(address).pathname, '/products');
        });
    });

    describe('turned tariff off', function () {
        it('assert renew tariff view', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-fullscreen-test', 'testiwan');
            await gotoOrganization(bro);
            await bro.url('/domains');
            await bro.yaWaitForVisible(
                adminPage.freeFullscreen(),
                'Не отобразился фуллскрин бесплатников'
            );

            await bro.yaAssertView(
                'free-fullscreen-renew-tariff-root',
                adminPage.freeFullscreen()
            );

            await bro.click(adminPage.renewTariff.closeButton());

            const address = await bro.getUrl();
            assert.equal(
                new URL(address).pathname,
                PAYMENT_AND_PRODUCT_PAGE_ROUTE
            );
        });

        it('check all tariffs link', async function () {
            const bro = this.browser;
            await bro.yaLoginFast('yndx-fullscreen-test', 'testiwan');
            await gotoOrganization(bro);
            await bro.url('/domains');
            await bro.yaWaitForVisible(
                adminPage.freeFullscreen(),
                'Не отобразился фуллскрин бесплатников'
            );

            await bro.click(adminPage.renewTariff.allTariffsLink());

            const address = await bro.getUrl();
            assert.equal(new URL(address).pathname, PRODUCTS_PAGE_ROUTE);
        });
    });
});
