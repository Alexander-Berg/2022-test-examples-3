import {assert} from 'chai';
import {book} from 'suites/hotels';
import moment from 'moment';

import {MINUTE, SECOND} from 'helpers/constants/dates';

import {testFailedPromoCodes} from './utilities/testFailedPromoCodes';
import dateFormats from 'helpers/utilities/date/formats';
import TestApp from 'helpers/project/TestApp';
import getTestOfferParams from 'helpers/project/hotels/app/TestHotelsBookApp/utilities/getTestOfferParams';

describe(book.name, () => {
    it('Неуспешное применение промокода', async function () {
        const testOfferParams = getTestOfferParams({
            originalId: 100,
            checkinDate: moment().add(1, 'days').format(dateFormats.ROBOT),
            checkoutDate: moment().add(2, 'days').format(dateFormats.ROBOT),
            priceAmount: 6000,
        });

        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        const {
            hotelsBookApp: {hotelsBookPage},
        } = app;

        // - do: Перейти на страницу бронирования для любого отеля, на любые даты.
        await hotelsBookPage.goToPage(testOfferParams);
        // - assert: Открылась страница бронирования.
        await hotelsBookPage.bookStatusProvider.isOfferFetched(30 * SECOND);

        await hotelsBookPage.priceInfo.promoCodes.checkBox.scrollIntoView();
        // - assert: В корзинке есть галка "У меня есть промокод", по умолчанию выключена. Поле инпут для промокода и кнопка "Применить" не показывается.
        await hotelsBookPage.priceInfo.promoCodes.testInitialPromoCodesState();
        // - do: Включить галку "У меня есть промокод"
        await hotelsBookPage.priceInfo.promoCodes.checkBox.click();
        // - assert: Появилось Поле инпут для промокода и кнопка "Применить"
        // - assert: Кнопка по умолчанию неактивна
        await hotelsBookPage.priceInfo.promoCodes.testActivePromoCodesState();

        // - do: В инпут написать промокод "EXPIRED", нажать применить
        // - assert: Появился тултип "Истек срок действия промокода"
        await hotelsBookPage.priceInfo.promoCodes.testTooltipError(
            'RANDOM_FAIL_PROMOCODE',
            'Такого промокода не существует',
        );

        const isDisabledInputWithError =
            await hotelsBookPage.priceInfo.promoCodes.input.isDisabled();
        const isDisabledButtonWithError =
            await hotelsBookPage.priceInfo.promoCodes.button.isDisabled();

        assert(
            !isDisabledInputWithError,
            'Поле ввода промокода не должно быть задизейблено после появлении ошибки',
        );
        assert(
            !isDisabledButtonWithError,
            'Кнопка примения промокода не должна быть задизейблена после появлении ошибки',
        );

        // - do: В инпут написать промокод "NOT_APPLICABLE", нажать применить
        await hotelsBookPage.priceInfo.promoCodes.testTooltipError(
            'EXPIRED',
            'Срок действия промокода истёк',
        );
        // - assert: Появился тултип "Промокод не подходит для этого заказа по условиям использования"
        await hotelsBookPage.priceInfo.promoCodes.testTooltipError(
            'NOT_APPLICABLE',
            'Промокод не подходит для этого заказа по условиям использования',
        );
    });

    hermione.config.testTimeout(6 * MINUTE);
    it('Промокод отклонен на этапе бронирования', async function () {
        const testOfferParams = getTestOfferParams({
            originalId: 100,
            checkinDate: moment().add(1, 'days').format(dateFormats.ROBOT),
            checkoutDate: moment().add(2, 'days').format(dateFormats.ROBOT),
            priceAmount: 6000,
        });

        const app = new TestApp(this.browser);

        await app.loginRandomAccount();

        await testFailedPromoCodes(this.browser, testOfferParams, {
            promoCode: 'SUCCESS_ALREADY_APPLIED',
            errorText:
                'Этот промокод уже был использован. Вы можете оплатить заказ без промокода или использовать другой.',
        });

        await testFailedPromoCodes(this.browser, testOfferParams, {
            promoCode: 'SUCCESS_NOT_APPLICABLE',
            errorText:
                'Промокод не подходит для этого заказа по условиям использования. Вы можете оплатить заказ без промокода или использовать другой.',
        });
    });
});
