import {assert} from 'chai';
import {random} from 'lodash';
import moment from 'moment';

import {phoneNumber} from 'helpers/project/common/phoneNumber';
import {TestAviaApp} from 'helpers/project/avia/app/TestAviaApp';
import {ADULT_MAN_WITH_PASSPORT} from 'helpers/project/avia/pages/CreateOrderPage/passengers';
import {AviaBookingPriceChangedErrorModal} from 'helpers/project/avia/pages/CreateOrderPage/components/ErrorModal';
import dateFormats from 'helpers/utilities/date/formats';

describe('ТК Авиа', () => {
    it('Изменение цены при оплате', async function () {
        const date = moment().add(1, 'month').add(random(1, 10), 'day');
        const app = new TestAviaApp(this.browser, {
            checkAvailabilityOnRedirOutcome: 'CAOR_SUCCESS',
            checkAvailabilityBeforeBookingOutcome: 'CAO_PRICE_CHANGED',
            tokenizationOutcome: 'TO_SUCCESS',
            confirmationOutcome: 'CO_SUCCESS',
            mqEventOutcome: 'MEO_SUCCESS',
        });

        const {price: originPrice} = await app.book(
            {
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
            },
            {
                passengers: [ADULT_MAN_WITH_PASSPORT],
                contacts: {
                    phone: phoneNumber,
                    email: 'test@test.ru',
                },
            },
        );

        const priceChangedError = new AviaBookingPriceChangedErrorModal(
            this.browser,
        );

        assert.isTrue(
            await priceChangedError.isVisible(20000),
            'Ошибка о изменении цены не была показана',
        );

        const changedPrice = await priceChangedError.getNewPrice();

        assert.isTrue(changedPrice !== originPrice, 'Цена не изменилась');

        await priceChangedError.primaryActionButton.click();

        const {happyPage, accountOrderPage} = app;

        await happyPage.waitForPageLoading();

        assert.isTrue(
            await happyPage.isOrderSuccessful(),
            'Бронирование завершилось неудачно',
        );

        await happyPage.forwardToDetailedPage();

        const finalPrice = await accountOrderPage.getOrderPrice();

        assert.equal(
            finalPrice,
            changedPrice,
            'Финальная цена отличается от цены пооказанной в модале с ошибкой',
        );

        /* TODO: оживить поосле фикса бекенда https://st.yandex-team.ru/TRAVELBACK-1365
        const passengerPrice = await resultPage.getOrderAdultPrice();

        assert.equal(
            finalPrice,
            passengerPrice,
            'Финальная и детализированная цена не совпадают',
        );
         */
    });
});
