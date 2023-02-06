import {assert} from 'chai';
import {book} from 'suites/hotels';
import moment from 'moment';

import {TEST_SUCCESS_PROMOCODE} from 'helpers/constants/hotels/promocodes';
import {MINUTE} from 'helpers/constants/dates';

import dateFormats from 'helpers/utilities/date/formats';
import {maleGuests} from 'helpers/project/hotels/data/adultGuests';
import {contacts} from 'helpers/project/hotels/data/contacts';
import TestOrderHotels from 'helpers/project/account/pages/OrderPage/TestOrderHotels';
import TestApp from 'helpers/project/TestApp';
import getTestOfferParams from 'helpers/project/hotels/app/TestHotelsBookApp/utilities/getTestOfferParams';

describe(book.name, () => {
    hermione.config.testTimeout(4 * MINUTE);
    it('Успешное использование промокода', async function () {
        const testOfferParams = getTestOfferParams({
            checkinDate: moment().add(1, 'days').format(dateFormats.ROBOT),
            checkoutDate: moment().add(2, 'days').format(dateFormats.ROBOT),
            priceAmount: 6000,
        });

        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {hotelsBookApp} = app;

        const {hotelsBookPage, hotelsHappyPage, hotelsPaymentPage} =
            hotelsBookApp;

        const hotelPagePromoCodes = hotelsBookPage.priceInfo.promoCodes;

        await hotelsBookPage.goToPage(testOfferParams);
        await hotelsBookPage.bookStatusProvider.isOfferFetched();

        await hotelsBookApp.paymentTestContextHelper.setPaymentTestContext();

        await hotelPagePromoCodes.checkBox.scrollIntoView();
        await hotelPagePromoCodes.testInitialPromoCodesState();
        await hotelPagePromoCodes.checkBox.click();
        await hotelPagePromoCodes.testActivePromoCodesState();

        const originalPriceAmount =
            await hotelsBookPage.priceInfo.totalPrice.getPriceValue();

        await hotelPagePromoCodes.applyPromoCode(TEST_SUCCESS_PROMOCODE.code);

        const discountAmount =
            await hotelPagePromoCodes.discountPrice.getPriceValue();
        const discountedAmount =
            await hotelsBookPage.priceInfo.totalPrice.getPriceValue();

        assert.equal(
            await hotelPagePromoCodes.discountPrice.getText(),
            '− 200 ₽',
            'В корзинке должна быть указана скидка по промокоду -200₽',
        );
        assert(
            await hotelPagePromoCodes.input.isDisabled(),
            'Поле ввода промокода должно быть задизейблено после применения промокода',
        );
        assert(
            !(await hotelPagePromoCodes.button.isVisible()),
            'Кнопка применения промокода должна скрыта после применения промокода',
        );
        assert(
            await hotelPagePromoCodes.resetLinkButton.isVisible(),
            'Кнопка отмены промокода должна быть отображена после применения промокода',
        );
        assert.equal(
            originalPriceAmount - discountAmount,
            discountedAmount,
            'Цена после примения промокода должна быть равной цене до применения скидки минус размер скидки',
        );

        await hotelsBookApp.paymentTestContextHelper.setPaymentTestContext();

        await hotelsBookPage.bookForm.fillForm(maleGuests, contacts);
        await hotelsBookPage.bookForm.submit();

        await hotelsPaymentPage.waitUntilLoaded();

        await hotelsHappyPage.waitUntilLoaded();
        await hotelsHappyPage.orderActions.detailsLink.click();

        const orderPage = new TestOrderHotels(this.browser);

        await orderPage.loader.waitUntilLoaded();

        assert.equal(
            discountedAmount,
            await orderPage.mainInfo.orderHotelsPrice.totalPrice.totalPrice.getPriceValue(),
            'Цена на HappyPage должна совпадать с ценой на странице бронирования с примененым промокодом',
        );

        const {receiptsAndDocs} = orderPage.mainInfo.orderHotelsPrice;

        await receiptsAndDocs.openDetails();

        // - assert: В корзинке есть информация о примененном промокоде "Скидка по промокоду SUCCESS −200₽"

        const firstPromoCode =
            await receiptsAndDocs.detailsModal.details.promoCodes.first();
        const promoCode = await firstPromoCode.additional.getText();
        const promoCodeDiscount = await firstPromoCode.price.getPriceValue();

        // TODO: Убрать условие после || `happyPagePromoCode === 'AUTOTEST_PROMOCODE_2OO'` как только бэк перестанет присылать на HappyPage нормализованный промокод
        assert(
            promoCode === TEST_SUCCESS_PROMOCODE.code ||
                promoCode === 'AUTOTEST_PROMOCODE_2OO',
            'Текст промокода на HappyPage должен совпадать с введенных на странице бронирования промокодом',
        );
        assert.equal(
            promoCodeDiscount,
            discountAmount,
            'Размер скидки должен совпадать с указанным на странице бронирования',
        );
    });
});
