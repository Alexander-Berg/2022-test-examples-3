import {assert} from 'chai';
import {book} from 'suites/hotels';

import {MINUTE} from 'helpers/constants/dates';
import {MOCK_PAYMENT_URL} from 'helpers/constants/testContext';

import {successPrepayFlow} from './utilities/successPrepayFlow';
import TestOrderHotels from 'helpers/project/account/pages/OrderPage/TestOrderHotels';
import TestHotelsStartDeferredPaymentPage from 'helpers/project/hotels/pages/TestHotelsStartDeferredPaymentPage/TestHotelsStartDeferredPaymentPage';
import Account from 'helpers/project/common/passport/Account';
import {testOrderPage} from './utilities/testOrderPage';
import {TestHotelsBookApp} from 'helpers/project/hotels/app/TestHotelsBookApp/TestHotelsBookApp';

const {name: suiteName} = book;

describe(suiteName, () => {
    hermione.config.testTimeout(6 * MINUTE);
    it('Возврат брони в рассрочку', async function () {
        const account = new Account();
        const {account: createdAccount} = await account.getOrCreate();
        const orderPage = new TestOrderHotels(this.browser);
        const cancelledOrderPage = new TestOrderHotels(this.browser);

        /* SuccessPrepayFlow */
        const orderPageData = await successPrepayFlow(
            this.browser,
            this.retryCount,
            createdAccount,
            orderPage,
        );

        await testOrderPage(orderPage, orderPageData);

        /* OrderPage */
        await orderPage.hotelActions.actions.cancelButton.click();
        await orderPage.cancelOrderModal.isVisible();
        await orderPage.cancelOrderModal.buttonSubmitClick();

        /* CancelledOrderPage */
        await cancelledOrderPage.loader.waitUntilLoaded();

        // Проработать чеки с бэкендом
    });

    hermione.config.testTimeout(5 * MINUTE);
    it('Возврат брони после доплаты', async function () {
        const account = new Account();
        const {account: createdAccount} = await account.getOrCreate();

        const app = new TestHotelsBookApp(this.browser);

        const {hotelsHappyPage} = app;

        const orderPage = new TestOrderHotels(this.browser);
        const startDeferredPaymentPage = new TestHotelsStartDeferredPaymentPage(
            this.browser,
        );
        const cancelledOrderPage = new TestOrderHotels(this.browser);

        /* SuccessPrepayFlow */
        const orderPageData = await successPrepayFlow(
            this.browser,
            this.retryCount,
            createdAccount,
            orderPage,
        );

        await testOrderPage(orderPage, orderPageData);

        /* Start nextPayment */
        await app.paymentTestContextHelper.setStartPaymentTestContext({
            minUserActionDelay:
                app.paymentTestContextHelper.getProgressiveDelay(
                    10,
                    this.retryCount,
                ),
            paymentOutcome: 'PO_SUCCESS',
            paymentUrl: MOCK_PAYMENT_URL,
        });

        await orderPage.deferredPayment.nextPaymentLink.click();

        await this.browser.switchToNextTab();

        await startDeferredPaymentPage.waitForPageLoading();

        /* HappyPage */
        await hotelsHappyPage.loader.waitUntilLoaded();
        await hotelsHappyPage.orderActions.detailsLink.waitForVisible();
        await hotelsHappyPage.orderActions.detailsLink.click();

        /* OrderPage */
        await orderPage.loader.waitUntilLoaded();

        assert.isFalse(
            await orderPage.deferredPayment.nextPaymentLink.isVisible(),
            'На странице заказа не должно быть ссылки на старт доплаты',
        );

        await orderPage.hotelActions.actions.cancelButton.click();
        await orderPage.cancelOrderModal.isVisible();
        await orderPage.cancelOrderModal.buttonSubmitClick();

        /* CancelledOrderPage */
        await cancelledOrderPage.loader.waitUntilLoaded();

        // Проработать чеки с бэкендом
    });
});
