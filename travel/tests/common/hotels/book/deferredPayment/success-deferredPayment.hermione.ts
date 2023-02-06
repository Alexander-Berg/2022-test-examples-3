import {assert} from 'chai';
import {book} from 'suites/hotels';
import moment from 'moment';

import {MINUTE} from 'helpers/constants/dates';
import {MOCK_PAYMENT_URL} from 'helpers/constants/testContext';
import {TEST_SUCCESS_PROMOCODE} from 'helpers/constants/hotels/promocodes';

import TestOrderHotels from 'helpers/project/account/pages/OrderPage/TestOrderHotels';
import {
    maleGuests,
    femaleGuests,
} from 'helpers/project/hotels/data/adultGuests';
import {contacts} from 'helpers/project/hotels/data/contacts';
import dateFormats from 'helpers/utilities/date/formats';
import TestApp from 'helpers/project/TestApp';
import getTestOfferParams from 'helpers/project/hotels/app/TestHotelsBookApp/utilities/getTestOfferParams';

describe(book.name, () => {
    hermione.config.testTimeout(5 * MINUTE);
    it('Бронь в рассрочку (с промокодом)', async function () {
        const testOfferParams = getTestOfferParams({
            originalId: 100,
            occupancy: '2',
            checkinDate: moment()
                .add(1, 'month')
                .add(5, 'days')
                .format(dateFormats.ROBOT),
            checkoutDate: moment()
                .add(1, 'month')
                .add(6, 'days')
                .format(dateFormats.ROBOT),
            priceAmount: 6000,
        });

        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {hotelsBookApp} = app;
        const {hotelsBookPage, hotelsPaymentPage, hotelsHappyPage} =
            hotelsBookApp;

        const orderPage = new TestOrderHotels(this.browser);
        const hotelPagePromoCodes = hotelsBookPage.priceInfo.promoCodes;
        const deferredPayment = hotelsBookPage.priceInfo.deferredPayment;

        /* Fetch offer */
        await hotelsBookPage.goToPage(testOfferParams);

        await hotelsBookPage.bookStatusProvider.isOfferFetched();

        /* Deferred payment */
        const originalDeferredFullPrice = await deferredPayment.getFullPrice();

        await deferredPayment.tryApplyFullPayment();

        assert.equal(
            await deferredPayment.postpayRadioboxLabel.getText(),
            'Оплатить позже',
            'Чекбокс рассрочки должен содержать текст "Оплатить позже"',
        );

        const originalTotalPrice =
            await hotelsBookPage.priceInfo.totalPrice.getPriceValue();

        assert.equal(
            originalTotalPrice,
            originalDeferredFullPrice,
            'Цена в чекбоксе "полная оплата" не совпадает с итоговой ценой "к оплате"',
        );

        await deferredPayment.testHasPaymentEndsAtLabel();
        await deferredPayment.testAvailabilityAllCheckbox();
        await deferredPayment.applyDeferredPayment();

        const paymentEndsAtDeferredPayment =
            await deferredPayment.paymentEndsAt.getText();
        const totalPriceAfterApplyDeferredPayment =
            await hotelsBookPage.priceInfo.totalPrice.getPriceValue();

        assert.isTrue(
            await deferredPayment.checkActiveDeferredPaymentCheckbox(),
            'Чекбокс "Оплатить позже" должен быть активирован',
        );

        /* Apply promoCode */
        await hotelPagePromoCodes.checkBox.scrollIntoView();
        await hotelPagePromoCodes.testInitialPromoCodesState();
        await hotelPagePromoCodes.checkBox.click();
        await hotelPagePromoCodes.testActivePromoCodesState();
        await hotelPagePromoCodes.applyPromoCode(TEST_SUCCESS_PROMOCODE.code);
        await hotelPagePromoCodes.discountPrice.waitForVisible();

        const deferredFullPriceAfterApplyPromo =
            await deferredPayment.getFullPrice();
        const totalPriceAfterApplyApplyPromo =
            await hotelsBookPage.priceInfo.totalPrice.getPriceValue();

        assert(
            await hotelPagePromoCodes.input.isDisabled(),
            'Поле ввода промокода должно быть задизейблено после применения промокода',
        );

        assert.equal(
            totalPriceAfterApplyDeferredPayment,
            totalPriceAfterApplyApplyPromo,
            'Итоговая цена не должна измениться (меняется доплата по рассрочке)',
        );

        assert.equal(
            originalDeferredFullPrice,
            deferredFullPriceAfterApplyPromo + TEST_SUCCESS_PROMOCODE.amount,
            `В доплате по рассрочке цена для оплаты уменьшилась на сумму промокода (200р). (${TEST_SUCCESS_PROMOCODE.amount})`,
        );

        /* Fill form and submit */
        await hotelsBookPage.bookForm.fillForm(
            [...maleGuests, ...femaleGuests],
            contacts,
        );

        await hotelsBookApp.paymentTestContextHelper.setStartPaymentTestContext(
            {
                minUserActionDelay:
                    hotelsBookApp.paymentTestContextHelper.getProgressiveDelay(
                        10,
                        this.retryCount,
                    ),
                paymentOutcome: 'PO_SUCCESS',
                paymentUrl: MOCK_PAYMENT_URL,
            },
        );

        await hotelsBookPage.bookForm.submit();

        /* HappyPage */
        await hotelsHappyPage.loader.waitUntilLoaded();

        assert.equal(
            hotelsHappyPage.isTouch
                ? 'Отель\nзабронирован!'
                : 'Отель забронирован!',
            await hotelsHappyPage.successText.title.getText(),
            'В заголовке страницы HappyPage должно быть указано: "Отель забронирован"',
        );

        assert.equal(
            await hotelsHappyPage.nextPaymentPrice.getPriceValue(),
            originalTotalPrice - TEST_SUCCESS_PROMOCODE.amount,
            'Сумма доплаты и дата с временем на HappyPage совпадает с суммой и датами на бронировании',
        );

        assert.include(
            await hotelsHappyPage.paymentEndsAt.getText(),
            paymentEndsAtDeferredPayment,
            'Сумма доплаты и дата с временем на HappyPage совпадает с суммой и датами на бронировании',
        );

        assert.isTrue(
            await hotelsHappyPage.nextPaymentLink.isVisible(),
            'На странице HappyPage должна быть ссылка на старт доплаты',
        );

        await hotelsHappyPage.orderActions.detailsLink.click();

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

        // await this.browser.pause(DELAY_BEFORE_CHECK_FISCAL_RECEIPTS);
        // await this.browser.refresh();
        // await orderPage.loader.waitUntilLoaded();

        // const fiscalReceipts = await orderPage.fiscalReceipts.map(
        //     async item => {
        //         return await item.getTextQA();
        //     },
        // );
        //
        // assert.equal(
        //     'Фискальный чек',
        //     fiscalReceipts[0],
        //     'На странице заказа должен отображаться один фискальный чек',
        // );

        /* Start nextPayment */
        await hotelsBookApp.paymentTestContextHelper.setStartPaymentTestContext(
            {
                minUserActionDelay:
                    hotelsBookApp.paymentTestContextHelper.getProgressiveDelay(
                        10,
                        this.retryCount,
                    ),
                paymentOutcome: 'PO_SUCCESS',
                paymentUrl: MOCK_PAYMENT_URL,
            },
        );

        const link =
            await orderPage.deferredPayment.nextPaymentLink.getAttribute(
                'href',
            );

        await orderPage.deferredPayment.nextPaymentLink.click();

        if (link) {
            try {
                await this.browser.switchWindow(link);
            } catch {
                await this.browser.switchToNextTab();
            }
        } else {
            throw new Error('У элемента ссылки нет аттрибута href');
        }

        /* Payment */
        await hotelsPaymentPage.hotelName.waitForVisible(2 * MINUTE);

        // в новой форме траста цена только в трасте, а его замокали
        // assert.equal(
        //     deferredPostpayPriceAfterApplyPromo,
        //     await hotelsPaymentPage.price.getPriceValue(),
        //     'Цена доплаты на этапе бронирования должна совпадать с ценой на этапе оплаты',
        // );

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

        await hotelsHappyPage.orderActions.detailsLink.click();

        /* OrderPage */
        await orderPage.loader.waitUntilLoaded();

        assert.isFalse(
            await orderPage.deferredPayment.nextPaymentLink.isVisible(),
            'На странице заказа не должно быть ссылки на старт доплаты',
        );

        // await this.browser.pause(DELAY_BEFORE_CHECK_FISCAL_RECEIPTS);
        // await this.browser.refresh();
        // await orderPage.loader.waitUntilLoaded();

        // const fiscalReceiptsWithSecondPayment = await orderPage.fiscalReceipts.map(
        //     async item => {
        //         return await item.getTextQA();
        //     },
        // );
        //
        // assert.equal(
        //     'Чек на 1 платеж',
        //     fiscalReceiptsWithSecondPayment[0],
        //     'На странице заказа должен отображаться чек на предоплату',
        // );
        //
        // assert.equal(
        //     'Чек на 2 платеж',
        //     fiscalReceiptsWithSecondPayment[1],
        //     'На странице заказа должен отображаться чек на доплату',
        // );
    });
});
