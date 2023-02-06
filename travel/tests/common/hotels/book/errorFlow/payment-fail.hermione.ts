import {assert} from 'chai';
import {book} from 'suites/hotels';

import {MOCK_PAYMENT_URL} from 'helpers/constants/testContext';
import {MINUTE} from 'helpers/constants/dates';

import {EPaymentFailureResponseCode} from 'helpers/project/common/api/types/EPaymentFailureResponseCode';

import TestOrderHotels from 'helpers/project/account/pages/OrderPage/TestOrderHotels';
import TestApp from 'helpers/project/TestApp';

/* Constants */
const {name: suiteName} = book;

describe(suiteName, () => {
    hermione.config.testTimeout(6 * MINUTE);
    it('Ошибка оплаты - повторная оплата', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {hotelsBookApp} = app;
        const {hotelsPaymentPage, hotelsHappyPage} = hotelsBookApp;

        await hotelsBookApp.paymentTestContextHelper.setStartPaymentTestContext(
            {
                minUserActionDelay:
                    hotelsBookApp.paymentTestContextHelper.getProgressiveDelay(
                        10,
                        this.retryCount,
                    ),
                paymentOutcome: 'PO_FAILURE',
                paymentFailureResponseCode:
                    EPaymentFailureResponseCode.UNKNOWN_ERROR,
                paymentFailureResponseDescription: 'Неиспользуемый текст',
                paymentUrl: MOCK_PAYMENT_URL,
            },
        );

        const {discountedPriceAmount} = await hotelsBookApp.bookWithPromoCode();

        await hotelsPaymentPage.hotelName.waitForVisible(MINUTE);

        // в новой форме траста цена только в трасте, а его замокали
        // assert.equal(
        //     discountedPriceAmount,
        //     await hotelsPaymentPage.price.getPriceValue(),
        //     'Цена на странице оплаты должна совпадать с ценой на странице бронирования с примененым промокодом',
        // );

        await hotelsPaymentPage.errorModal.waitForVisible(MINUTE);
        await hotelsPaymentPage.errorModal.testModalContent({
            title: 'Ошибка оплаты',
            text: 'Проверьте данные карты, пополните баланс или используйте другую карту.',
            secondaryActionText: 'Отмена',
            primaryActionText: 'Попробовать ещё раз',
            priceValue: discountedPriceAmount,
        });

        await hotelsBookApp.paymentTestContextHelper.setStartPaymentTestContext(
            {
                minUserActionDelay: 'PT10S',
                paymentOutcome: 'PO_SUCCESS',
                paymentUrl: MOCK_PAYMENT_URL,
            },
        );

        await hotelsPaymentPage.errorModal.primaryActionButton.click();
        await hotelsPaymentPage.errorModal.waitForHidden();
        await hotelsPaymentPage.waitUntilLoaded();

        await hotelsHappyPage.waitUntilLoaded();

        await hotelsHappyPage.orderActions.detailsLink.click();

        const orderPage = new TestOrderHotels(this.browser);

        await orderPage.loader.waitUntilLoaded();

        assert.equal(
            discountedPriceAmount,
            await orderPage.mainInfo.orderHotelsPrice.totalPrice.totalPrice.getPriceValue(),
            'Цена на HappyPage должна совпадать с ценой на странице бронирования с примененым промокодом',
        );
    });

    hermione.config.testTimeout(4 * MINUTE);
    it('Ошибка оплаты - отмена заказа', async function () {
        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {hotelsBookApp} = app;
        const {hotelsBookPage, hotelsPaymentPage} = hotelsBookApp;

        await hotelsBookApp.paymentTestContextHelper.setStartPaymentTestContext(
            {
                minUserActionDelay:
                    hotelsBookApp.paymentTestContextHelper.getProgressiveDelay(
                        10,
                        this.retryCount,
                    ),
                paymentOutcome: 'PO_FAILURE',
                paymentFailureResponseCode:
                    EPaymentFailureResponseCode.UNKNOWN_ERROR,
                paymentFailureResponseDescription: 'Неиспользуемый текст',
                paymentUrl: MOCK_PAYMENT_URL,
            },
        );

        const {discountedPriceAmount} = await hotelsBookApp.bookWithPromoCode();

        await hotelsPaymentPage.hotelName.waitForVisible(MINUTE);

        // в новой форме траста цена только в трасте, а его замокали
        // assert.equal(
        //     discountedPriceAmount,
        //     await hotelsPaymentPage.price.getPriceValue(),
        //     'Цена на странице оплаты должна совпадать с ценой на странице бронирования с примененым промокодом',
        // );

        await hotelsPaymentPage.errorModal.waitForVisible(MINUTE);
        await hotelsPaymentPage.errorModal.testModalContent({
            title: 'Ошибка оплаты',
            text: 'Проверьте данные карты, пополните баланс или используйте другую карту.',
            secondaryActionText: 'Отмена',
            primaryActionText: 'Попробовать ещё раз',
            priceValue: discountedPriceAmount,
        });

        await hotelsPaymentPage.errorModal.secondaryActionButton.click();
        await hotelsPaymentPage.errorModal.waitForHidden();

        assert(
            await hotelsBookPage.bookStatusProvider.isOfferFetched(),
            'Должна открыться страница бронирования отеля',
        );
        await hotelsBookPage.priceInfo.promoCodes.testInitialPromoCodesState();
        assert.isFalse(
            await hotelsBookPage.priceInfo.promoCodes.discountPrice.isVisible(),
            'В корзинке не должна быть указана скидка по промокоду',
        );
    });
});
