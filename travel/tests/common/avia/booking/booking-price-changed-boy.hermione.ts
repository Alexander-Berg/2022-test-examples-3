import {assert} from 'chai';
import moment from 'moment';
import {random} from 'lodash';

import {AviaCreateOrderPage} from 'helpers/project/avia/pages';
import dateFormats from 'helpers/utilities/date/formats';
import {phoneNumber} from 'helpers/project/common/phoneNumber';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import {ADULT_MAN_WITH_PASSPORT} from 'helpers/project/avia/pages/CreateOrderPage/passengers';
import {AviaBookingPriceChangedErrorModal} from 'helpers/project/avia/pages/CreateOrderPage/components/ErrorModal';
import {TestAviaHappyPage} from 'helpers/project/avia/pages/TestAviaHappyPage/TestAviaHappyPage';
import {AccountOrderPage} from 'helpers/project/avia/pages/AccountOrderPage/AccountOrderPage';
import {delay} from 'helpers/project/common/delay';

describe('ТК Авиа', function () {
    it('Изменение цены при переходе на бой', async function () {
        const date = moment().add(1, 'month').add(random(1, 10), 'day');
        const app = new TestAviaApp(this.browser, {
            checkAvailabilityOnRedirOutcome: 'CAOR_PRICE_CHANGED',
            checkAvailabilityBeforeBookingOutcome: 'CAO_SUCCESS',
            tokenizationOutcome: 'TO_SUCCESS',
            confirmationOutcome: 'CO_SUCCESS',
            mqEventOutcome: 'MEO_NO_EVENT',
        });

        const searchPage = await app.goToSearchPage({
            from: {name: 'Москва', id: 'c213'},
            to: {name: 'Сочи', id: 'c239'},
            startDate: date.format(dateFormats.ROBOT),
            travellers: {
                adults: 1,
                children: 0,
                infants: 0,
            },
            klass: 'economy',
            filters: 'pt=aeroflot&c=0,26',
        });

        await app.moveToBooking(searchPage);

        const createOrderPage = new AviaCreateOrderPage(this.browser);

        await createOrderPage.waitPageReadyForInteraction();

        // Получили модал
        const priceChangedError = new AviaBookingPriceChangedErrorModal(
            this.browser,
        );

        // Его видно
        assert.isTrue(
            await priceChangedError.isVisible(20000),
            'Ошибка о изменении цены не была показана',
        );

        // Цену запомнили
        const changedPrice = await priceChangedError.getNewPrice();

        // Закрыли мордал
        await priceChangedError.close();

        // ждём анимацию закрытия модала
        await delay(1000);

        // Нашли новую цену, нашли старую цену, сравнили (разница в 10 рублей)
        const changedPriceFromOrder =
            await createOrderPage.priceInfo.getPrice();

        assert.equal(
            changedPriceFromOrder,
            changedPrice,
            'Итоговая цена на странице заказа отличается от цены в модале',
        );

        const oldPriceFromOrder = await createOrderPage.priceInfo.getOldPrice();

        const oldPriceNumber = parseInt(
            oldPriceFromOrder.replace(/[^0-9]/i, ''),
        );
        const newPriceNumber = parseInt(
            changedPriceFromOrder.replace(/[^0-9]/i, ''),
        );

        assert.equal(
            oldPriceNumber + 10,
            newPriceNumber,
            'Новая цена отличается на сумму, отличную от 10',
        );

        // Сообщений об аэрофлоте есть
        const alertMessage = await createOrderPage.priceInfo.getAlertMessage();

        assert.equal(
            alertMessage,
            'Цена изменилась после уточнения предложения у Авиакомпании.',
            'Сообщение об изменении цены отличается от необходимого',
        );

        // Заполнили форму
        await createOrderPage.fillBookingForm([ADULT_MAN_WITH_PASSPORT], {
            phone: phoneNumber,
            email: 'test@test.ru',
        });
        // Сабмитнули
        await createOrderPage.goToPayment();

        const happyPage = new TestAviaHappyPage(this.browser);

        await happyPage.waitForPageLoading();

        assert.isTrue(
            await happyPage.isOrderSuccessful(),
            'Бронирование завершилось неудачно',
        );

        await happyPage.orderActions.detailsLink.click();

        const accountOrderPage = new AccountOrderPage(this.browser);

        await accountOrderPage.waitForPageLoading();

        const finalPrice = await accountOrderPage.getOrderPrice();

        assert.equal(
            finalPrice,
            changedPrice,
            'Финальная цена отличается от цены пооказанной в модале с ошибкой',
        );
    });
});
