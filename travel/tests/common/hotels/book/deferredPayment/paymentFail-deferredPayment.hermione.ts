import {assert} from 'chai';
import {book} from 'suites/hotels';

import {MINUTE} from 'helpers/constants/dates';
import {MOCK_PAYMENT_URL} from 'helpers/constants/testContext';

import {EPaymentFailureResponseCode} from 'helpers/project/common/api/types/EPaymentFailureResponseCode';

import Account from 'helpers/project/common/passport/Account';
import TestOrderHotels from 'helpers/project/account/pages/OrderPage/TestOrderHotels';
import TestHotelsStartDeferredPaymentPage from 'helpers/project/hotels/pages/TestHotelsStartDeferredPaymentPage/TestHotelsStartDeferredPaymentPage';
import {successPrepayFlow} from './utilities/successPrepayFlow';
import {TestHotelsBookApp} from 'helpers/project/hotels/app/TestHotelsBookApp/TestHotelsBookApp';

const {name: suiteName} = book;

describe(suiteName, () => {
    hermione.config.testTimeout(5 * MINUTE);
    it('Обработка ошибки при доплате брони', async function () {
        const account = new Account();
        const {account: createdAccount} = await account.getOrCreate();

        const app = new TestHotelsBookApp(this.browser);

        const {hotelsHappyPage, hotelsPaymentPage} = app;

        const orderPage = new TestOrderHotels(this.browser);
        const startDeferredPaymentPage = new TestHotelsStartDeferredPaymentPage(
            this.browser,
        );

        const orderPageData = await successPrepayFlow(
            this.browser,
            this.retryCount,
            createdAccount,
            orderPage,
        );

        const {paymentEndsAtDeferredPayment, deferredFullPriceAfterApplyPromo} =
            orderPageData;

        await app.paymentTestContextHelper.setStartPaymentTestContext({
            minUserActionDelay:
                app.paymentTestContextHelper.getProgressiveDelay(
                    10,
                    this.retryCount,
                ),
            paymentOutcome: 'PO_FAILURE',
            paymentFailureResponseCode:
                EPaymentFailureResponseCode.UNKNOWN_ERROR,
            paymentFailureResponseDescription: 'Неиспользуемый текст',
            paymentUrl: MOCK_PAYMENT_URL,
        });

        /* Start nextPayment */
        await orderPage.deferredPayment.nextPaymentLink.click();

        await this.browser.switchToNextTab();

        await startDeferredPaymentPage.waitForPageLoading();

        /* FailPayment */
        await hotelsPaymentPage.hotelName.waitForVisible(MINUTE);
        await hotelsPaymentPage.errorModal.waitForVisible(MINUTE);
        await hotelsPaymentPage.errorModal.testModalContent({
            title: 'Ошибка оплаты',
            text: 'Проверьте данные карты, пополните баланс или используйте другую карту.',
            secondaryActionText: 'Отмена',
            primaryActionText: 'Попробовать ещё раз',
            priceValue: deferredFullPriceAfterApplyPromo,
        });

        await hotelsPaymentPage.errorModal.secondaryActionButton.click();
        await hotelsPaymentPage.errorModal.waitForHidden();

        /* OrderPage */
        await orderPage.loader.waitUntilLoaded();

        assert.equal(
            deferredFullPriceAfterApplyPromo,
            await orderPage.deferredPayment.nextPaymentPrice.getPriceValue(),
            'Цена доплаты на странице бронирования и на странице заказа должна совпадать',
        );

        assert.equal(
            paymentEndsAtDeferredPayment,
            await orderPage.deferredPayment.paymentEndsAt.getText(),
            'Крайняя дата доплаты на странице бронирования и на странице заказа должна совпадать',
        );

        assert.isTrue(
            await orderPage.deferredPayment.nextPaymentLink.isVisible(),
            'Должна отображаться ссылка на доплату',
        );

        /* Repeat nextPayment */
        await orderPage.deferredPayment.nextPaymentLink.click();

        await this.browser.switchToNextTab();

        await startDeferredPaymentPage.waitForPageLoading();

        /* Repeat failPayment */
        await hotelsPaymentPage.hotelName.waitForVisible(MINUTE);

        await hotelsPaymentPage.errorModal.waitForVisible(MINUTE);

        await hotelsPaymentPage.errorModal.testModalContent({
            title: 'Ошибка оплаты',
            text: 'Проверьте данные карты, пополните баланс или используйте другую карту.',
            secondaryActionText: 'Отмена',
            primaryActionText: 'Попробовать ещё раз',
            priceValue: deferredFullPriceAfterApplyPromo,
        });

        await app.paymentTestContextHelper.setStartPaymentTestContext({
            minUserActionDelay: 'PT10S',
            paymentOutcome: 'PO_SUCCESS',
            paymentUrl: MOCK_PAYMENT_URL,
        });

        await hotelsPaymentPage.errorModal.primaryActionButton.click();
        await hotelsPaymentPage.errorModal.waitForHidden();
        await hotelsPaymentPage.iframe.waitForVisible(MINUTE);

        /* HappyPage */
        await hotelsHappyPage.loader.waitUntilLoaded();

        assert.equal(
            hotelsHappyPage.isTouch
                ? 'Отель оплачен\nполностью!'
                : 'Отель оплачен полностью!',
            await hotelsHappyPage.successText.title.getText(),
            'В заголовке страницы HappyPage должно быть указано: "Отель оплачен полностью"',
        );

        assert.isFalse(
            await hotelsHappyPage.nextPaymentLink.isVisible(),
            'На странице HappyPage не должно быть ссылки на старт доплаты',
        );
    });
});
